package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.entailment.OpTransitiveClosure;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLContextType;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLPositionType;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVariable;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.ARQException;
import org.apache.jena.sparql.core.Quad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL operator that generates a PostgreSQL recursive CTE for transitive property
 * closures (e.g., {@code rdfs:subClassOf+}, {@code ex:adjacentTo*}).
 * <p>
 * Three storage layouts are supported:
 * <ul>
 *   <li>condensed ({@code versioned_quad}): the version set is intersected at each
 *       step of the chain ({@code c.validity & t.validity}) so a node is only
 *       reachable in versions where ALL links of at least one chain exist. Rows
 *       reachable through several chains are collapsed with {@code bit_or}.</li>
 *   <li>flat ({@code versioned_quad_flat}): the chain stays within one versioned
 *       named graph; duplicates are removed with {@code DISTINCT}.</li>
 *   <li>metadata ({@code metadata}): used when the closure runs over the default
 *       graph (e.g., {@code prov:specializationOf+}).</li>
 * </ul>
 * The recursion is anchored on the concrete endpoint when there is one (forward
 * from a bound start, backward from a bound end) so that multi-hop chains towards
 * a bound end are found. With {@code minHops == 0} ({@code *} / zero-or-more), a
 * zero-length arm binding both endpoints to the concrete term is added; this
 * requires at least one concrete endpoint.
 */
public class TransitiveClosureSQLOperator extends SQLOperator {

    private final OpTransitiveClosure opTC;
    private final SQLContext context;
    private final boolean anchoredForward;

    public TransitiveClosureSQLOperator(OpTransitiveClosure opTC, SQLContext context) {
        this.opTC = opTC;
        this.context = context;
        // Anchor backward only when the end is the single concrete endpoint
        this.anchoredForward = opTC.getStartNode().isConcrete() || !opTC.getEndNode().isConcrete();

        if (opTC.getMinHops() == 0 && !opTC.getStartNode().isConcrete() && !opTC.getEndNode().isConcrete()) {
            throw new ARQException(
                    "Zero-or-more transitive closure requires at least one concrete endpoint (got "
                            + opTC.getStartNode() + " and " + opTC.getEndNode() + ")");
        }
    }

    @Override
    public SQLQuery buildSQLQuery() {
        Map<Node, List<SPARQLOccurrence>> varOccurrences = createVarOccurrences();
        SQLContext newContext = context.copyWithNewVarOccurrences(varOccurrences);

        String sql;
        if (isMetadataClosure()) {
            sql = buildMetadataSQL();
        } else if (context.condensedMode()) {
            sql = buildCondensedSQL();
        } else {
            sql = buildFlatSQL();
        }

        return new SQLQuery(sql, newContext);
    }

    private boolean isMetadataClosure() {
        return opTC.getGraphNode() == Quad.defaultGraphNodeGenerated;
    }

    private String buildCondensedSQL() {
        String predicateLookup = predicateSubquery();
        Node start = opTC.getStartNode();
        Node end = opTC.getEndNode();
        Node graph = opTC.getGraphNode();

        StringBuilder sb = new StringBuilder();

        // The validity is carried as text inside the CTE: PostgreSQL cannot
        // deduplicate a recursive UNION over bit varying (not hashable)
        sb.append("WITH RECURSIVE closure(id_start, id_end, validity, id_named_graph) AS (\n");

        // Base case: direct property assertions, anchored on the concrete endpoint
        sb.append("  SELECT t0.id_subject, t0.id_object, t0.validity::text, t0.id_named_graph\n");
        sb.append("  FROM versioned_quad t0\n");
        sb.append("  WHERE t0.id_predicate = ").append(predicateLookup).append("\n");
        if (anchoredForward && start.isURI()) {
            sb.append("    AND t0.id_subject = ").append(resourceLookup(start.getURI())).append("\n");
        } else if (!anchoredForward && end.isURI()) {
            sb.append("    AND t0.id_object = ").append(resourceLookup(end.getURI())).append("\n");
        }
        appendCondensedGraphFilters(sb, "t0", graph);

        // Zero-length arm for zero-or-more closures
        if (opTC.getMinHops() == 0) {
            String termLookup = resourceLookup(zeroLengthTerm().getURI());
            sb.append("  UNION\n");
            sb.append("  SELECT ").append(termLookup).append(", ").append(termLookup)
                    .append(", zv.validity::text, zv.id_named_graph\n");
            sb.append("  FROM (SELECT bit_or(z0.validity) AS validity, z0.id_named_graph\n");
            sb.append("        FROM versioned_quad z0\n");
            sb.append("        WHERE 1 = 1\n");
            appendCondensedGraphFilters(sb, "z0", graph);
            sb.append("        GROUP BY z0.id_named_graph) zv\n");
        }

        sb.append("  UNION\n");

        // Recursive step: intersect validity at each hop
        if (anchoredForward) {
            sb.append("  SELECT c.id_start, t1.id_object, (c.validity::varbit & t1.validity)::text, c.id_named_graph\n");
            sb.append("  FROM closure c\n");
            sb.append("  JOIN versioned_quad t1 ON c.id_end = t1.id_subject\n");
        } else {
            sb.append("  SELECT t1.id_subject, c.id_end, (c.validity::varbit & t1.validity)::text, c.id_named_graph\n");
            sb.append("  FROM closure c\n");
            sb.append("  JOIN versioned_quad t1 ON c.id_start = t1.id_object\n");
        }
        sb.append("    AND c.id_named_graph = t1.id_named_graph\n");
        sb.append("    AND t1.id_predicate = ").append(predicateLookup).append("\n");
        sb.append("  WHERE bit_count(c.validity::varbit & t1.validity) != 0\n");

        sb.append(")\n");

        // Collapse chains: a pair reachable through several chains is valid in the
        // union of the chains' version sets
        sb.append("SELECT ");
        List<String> selectCols = new ArrayList<>();
        if (start.isVariable()) {
            selectCols.add("closure.id_start AS v$" + start.getName());
        }
        if (end.isVariable()) {
            selectCols.add("closure.id_end AS v$" + end.getName());
        }
        if (graph.isVariable()) {
            selectCols.add("closure.id_named_graph AS ng$" + graph.getName());
            selectCols.add("closure.validity AS bs$" + graph.getName());
        }
        if (selectCols.isEmpty()) {
            selectCols.add("1");
        }
        sb.append(String.join(", ", selectCols)).append("\n");
        sb.append("FROM (SELECT id_start, id_end, id_named_graph, bit_or(validity::varbit) AS validity\n");
        sb.append("      FROM closure\n");
        sb.append("      WHERE bit_count(validity::varbit) != 0\n");
        if (anchoredForward && end.isURI()) {
            sb.append("        AND id_end = ").append(resourceLookup(end.getURI())).append("\n");
        }
        sb.append("      GROUP BY id_start, id_end, id_named_graph) closure");

        return sb.toString();
    }

    private String buildFlatSQL() {
        String predicateLookup = predicateSubquery();
        Node start = opTC.getStartNode();
        Node end = opTC.getEndNode();
        Node graph = opTC.getGraphNode();

        StringBuilder sb = new StringBuilder();

        sb.append("WITH RECURSIVE closure(id_start, id_end, id_versioned_named_graph) AS (\n");

        // Base case
        sb.append("  SELECT t0.id_subject, t0.id_object, t0.id_versioned_named_graph\n");
        sb.append("  FROM versioned_quad_flat t0\n");
        sb.append("  WHERE t0.id_predicate = ").append(predicateLookup).append("\n");
        if (anchoredForward && start.isURI()) {
            sb.append("    AND t0.id_subject = ").append(resourceLookup(start.getURI())).append("\n");
        } else if (!anchoredForward && end.isURI()) {
            sb.append("    AND t0.id_object = ").append(resourceLookup(end.getURI())).append("\n");
        }
        appendFlatGraphFilter(sb, "t0", graph);

        // Zero-length arm for zero-or-more closures
        if (opTC.getMinHops() == 0) {
            String termLookup = resourceLookup(zeroLengthTerm().getURI());
            sb.append("  UNION\n");
            sb.append("  SELECT ").append(termLookup).append(", ").append(termLookup)
                    .append(", z0.id_versioned_named_graph\n");
            sb.append("  FROM versioned_quad_flat z0\n");
            sb.append("  WHERE 1 = 1\n");
            appendFlatGraphFilter(sb, "z0", graph);
        }

        sb.append("  UNION\n");

        // Recursive step
        if (anchoredForward) {
            sb.append("  SELECT c.id_start, t1.id_object, c.id_versioned_named_graph\n");
            sb.append("  FROM closure c\n");
            sb.append("  JOIN versioned_quad_flat t1 ON c.id_end = t1.id_subject\n");
        } else {
            sb.append("  SELECT t1.id_subject, c.id_end, c.id_versioned_named_graph\n");
            sb.append("  FROM closure c\n");
            sb.append("  JOIN versioned_quad_flat t1 ON c.id_start = t1.id_object\n");
        }
        sb.append("    AND c.id_versioned_named_graph = t1.id_versioned_named_graph\n");
        sb.append("    AND t1.id_predicate = ").append(predicateLookup).append("\n");

        sb.append(")\n");

        sb.append("SELECT DISTINCT ");
        List<String> selectCols = new ArrayList<>();
        if (start.isVariable()) {
            selectCols.add("closure.id_start AS v$" + start.getName());
        }
        if (end.isVariable()) {
            selectCols.add("closure.id_end AS v$" + end.getName());
        }
        if (graph.isVariable()) {
            selectCols.add("closure.id_versioned_named_graph AS v$" + graph.getName());
        }
        if (selectCols.isEmpty()) {
            selectCols.add("1");
        }
        sb.append(String.join(", ", selectCols)).append("\n");
        sb.append("FROM closure");
        if (anchoredForward && end.isURI()) {
            sb.append("\nWHERE closure.id_end = ").append(resourceLookup(end.getURI()));
        }

        return sb.toString();
    }

    private String buildMetadataSQL() {
        String predicateLookup = predicateSubquery();
        Node start = opTC.getStartNode();
        Node end = opTC.getEndNode();

        StringBuilder sb = new StringBuilder();

        sb.append("WITH RECURSIVE closure(id_start, id_end) AS (\n");

        // Base case
        sb.append("  SELECT t0.id_subject, t0.id_object\n");
        sb.append("  FROM metadata t0\n");
        sb.append("  WHERE t0.id_predicate = ").append(predicateLookup).append("\n");
        if (anchoredForward && start.isURI()) {
            sb.append("    AND t0.id_subject = ").append(resourceLookup(start.getURI())).append("\n");
        } else if (!anchoredForward && end.isURI()) {
            sb.append("    AND t0.id_object = ").append(resourceLookup(end.getURI())).append("\n");
        }

        // Zero-length arm for zero-or-more closures
        if (opTC.getMinHops() == 0) {
            String termLookup = resourceLookup(zeroLengthTerm().getURI());
            sb.append("  UNION\n");
            sb.append("  SELECT ").append(termLookup).append(", ").append(termLookup).append("\n");
        }

        sb.append("  UNION\n");

        // Recursive step
        if (anchoredForward) {
            sb.append("  SELECT c.id_start, t1.id_object\n");
            sb.append("  FROM closure c\n");
            sb.append("  JOIN metadata t1 ON c.id_end = t1.id_subject\n");
        } else {
            sb.append("  SELECT t1.id_subject, c.id_end\n");
            sb.append("  FROM closure c\n");
            sb.append("  JOIN metadata t1 ON c.id_start = t1.id_object\n");
        }
        sb.append("    AND t1.id_predicate = ").append(predicateLookup).append("\n");

        sb.append(")\n");

        sb.append("SELECT DISTINCT ");
        List<String> selectCols = new ArrayList<>();
        if (start.isVariable()) {
            selectCols.add("closure.id_start AS v$" + start.getName());
        }
        if (end.isVariable()) {
            selectCols.add("closure.id_end AS v$" + end.getName());
        }
        if (selectCols.isEmpty()) {
            selectCols.add("1");
        }
        sb.append(String.join(", ", selectCols)).append("\n");
        sb.append("FROM closure");
        if (anchoredForward && end.isURI()) {
            sb.append("\nWHERE closure.id_end = ").append(resourceLookup(end.getURI()));
        }

        return sb.toString();
    }

    /**
     * The concrete endpoint used by the zero-length arm; validated in the constructor.
     */
    private Node zeroLengthTerm() {
        return opTC.getStartNode().isConcrete() ? opTC.getStartNode() : opTC.getEndNode();
    }

    /**
     * Restrict a condensed-mode arm to a concrete named graph and its version bit.
     */
    private void appendCondensedGraphFilters(StringBuilder sb, String alias, Node graph) {
        if (graph.isURI() && !isMetadataClosure()) {
            sb.append("    AND ").append(alias).append(".id_named_graph = ")
                    .append(namedGraphLookup(graph.getURI())).append("\n");
            sb.append("    AND get_bit(").append(alias).append(".validity, ")
                    .append(versionIndexLookup(graph.getURI())).append(" - 1) = 1\n");
        }
    }

    private void appendFlatGraphFilter(StringBuilder sb, String alias, Node graph) {
        if (graph.isURI() && !isMetadataClosure()) {
            sb.append("    AND ").append(alias).append(".id_versioned_named_graph = ")
                    .append("(SELECT rl.id_resource_or_literal FROM resource_or_literal rl WHERE rl.name = '")
                    .append(graph.getURI()).append("')\n");
        }
    }

    /**
     * Create variable occurrence map for the output of this operator.
     */
    private Map<Node, List<SPARQLOccurrence>> createVarOccurrences() {
        Map<Node, List<SPARQLOccurrence>> occurrences = new HashMap<>();
        Node start = opTC.getStartNode();
        Node end = opTC.getEndNode();
        Node graph = opTC.getGraphNode();
        SPARQLContextType contextType = isMetadataClosure()
                ? SPARQLContextType.METADATA
                : SPARQLContextType.VERSIONED_DATA;

        if (start.isVariable()) {
            occurrences.computeIfAbsent(start, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(
                            SPARQLPositionType.SUBJECT,
                            0,
                            contextType,
                            new SQLVariable(SQLVarType.ID, start.getName())
                    ));
        }

        if (end.isVariable()) {
            occurrences.computeIfAbsent(end, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(
                            SPARQLPositionType.OBJECT,
                            0,
                            contextType,
                            new SQLVariable(SQLVarType.ID, end.getName())
                    ));
        }

        if (graph.isVariable() && context.condensedMode()) {
            occurrences.computeIfAbsent(graph, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(
                            SPARQLPositionType.GRAPH_NAME,
                            0,
                            contextType,
                            new SQLVariable(SQLVarType.CONDENSED, graph.getName())
                    ));
        } else if (graph.isVariable()) {
            occurrences.computeIfAbsent(graph, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(
                            SPARQLPositionType.GRAPH_NAME,
                            0,
                            contextType,
                            new SQLVariable(SQLVarType.ID, graph.getName())
                    ));
        }

        return occurrences;
    }

    private String predicateSubquery() {
        return "(SELECT id_resource_or_literal FROM resource_or_literal WHERE name = '"
                + opTC.getPredicateURI() + "')";
    }

    private String resourceLookup(String uri) {
        return "(SELECT id_resource_or_literal FROM resource_or_literal WHERE name = '" + uri + "')";
    }

    private String namedGraphLookup(String uri) {
        return """
                (SELECT vng.id_named_graph
                 FROM versioned_named_graph vng JOIN resource_or_literal rl ON
                 vng.id_versioned_named_graph = rl.id_resource_or_literal
                 WHERE rl.name = '""" + uri + "')";
    }

    private String versionIndexLookup(String uri) {
        return """
                (SELECT vng.index_version
                 FROM versioned_named_graph vng JOIN resource_or_literal rl ON
                 vng.id_versioned_named_graph = rl.id_resource_or_literal
                 WHERE rl.name = '""" + uri + "')";
    }

    @Override
    protected String buildSelect() {
        return "";
    }

    @Override
    protected String buildFrom() {
        return "";
    }

    @Override
    protected String buildWhere() {
        return "";
    }
}
