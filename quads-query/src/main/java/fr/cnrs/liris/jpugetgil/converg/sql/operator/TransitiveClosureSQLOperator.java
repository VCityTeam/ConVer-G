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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL operator that generates a PostgreSQL recursive CTE for transitive property
 * closures (e.g., {@code rdfs:subClassOf*}, {@code rdfs:subPropertyOf*}).
 * <p>
 * In condensed mode, the version set is intersected at each step of the chain:
 * {@code c.validity & t.validity}. This ensures that the transitive closure
 * only returns results valid in versions where ALL links in the chain exist.
 * <p>
 * The generated SQL has the form:
 * <pre>
 * WITH RECURSIVE closure AS (
 *     -- Base case: direct property assertions
 *     SELECT t0.id_subject, t0.id_object, t0.validity, t0.id_named_graph
 *     FROM versioned_quad t0
 *     WHERE t0.id_predicate = (SELECT id_resource_or_literal FROM resource_or_literal WHERE name = '...')
 *     UNION
 *     -- Recursive step: extend chain with version-set intersection
 *     SELECT c.id_subject, t1.id_object, c.validity &amp; t1.validity, c.id_named_graph
 *     FROM closure c
 *     JOIN versioned_quad t1 ON c.id_object = t1.id_subject
 *         AND c.id_named_graph = t1.id_named_graph
 *         AND t1.id_predicate = (SELECT ... WHERE name = '...')
 *     WHERE bit_count(c.validity &amp; t1.validity) != 0
 * )
 * SELECT ... FROM closure WHERE bit_count(validity) != 0
 * </pre>
 */
public class TransitiveClosureSQLOperator extends SQLOperator {

    private final OpTransitiveClosure opTC;
    private final SQLContext context;

    public TransitiveClosureSQLOperator(OpTransitiveClosure opTC, SQLContext context) {
        this.opTC = opTC;
        this.context = context;
    }

    @Override
    public SQLQuery buildSQLQuery() {
        Map<Node, List<SPARQLOccurrence>> varOccurrences = createVarOccurrences();
        SQLContext newContext = context.copyWithNewVarOccurrences(varOccurrences);

        String sql;
        if (context.condensedMode()) {
            sql = buildCondensedSQL();
        } else {
            sql = buildFlatSQL();
        }

        return new SQLQuery(sql, newContext);
    }

    private String buildCondensedSQL() {
        String predicateLookup = predicateSubquery();
        Node start = opTC.getStartNode();
        Node end = opTC.getEndNode();
        Node graph = opTC.getGraphNode();

        StringBuilder sb = new StringBuilder();

        // WITH RECURSIVE closure
        sb.append("WITH RECURSIVE closure(id_start, id_end, validity, id_named_graph) AS (\n");

        // Base case
        sb.append("  SELECT t0.id_subject, t0.id_object, t0.validity, t0.id_named_graph\n");
        sb.append("  FROM versioned_quad t0\n");
        sb.append("  WHERE t0.id_predicate = ").append(predicateLookup).append("\n");
        appendStartEndFilters(sb, true);

        sb.append("  UNION\n");

        // Recursive step: intersect validity at each hop
        sb.append("  SELECT c.id_start, t1.id_object, c.validity & t1.validity, c.id_named_graph\n");
        sb.append("  FROM closure c\n");
        sb.append("  JOIN versioned_quad t1 ON c.id_end = t1.id_subject\n");
        sb.append("    AND c.id_named_graph = t1.id_named_graph\n");
        sb.append("    AND t1.id_predicate = ").append(predicateLookup).append("\n");
        sb.append("  WHERE bit_count(c.validity & t1.validity) != 0\n");

        sb.append(")\n");

        // Final SELECT
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

        sb.append(String.join(", ", selectCols)).append("\n");
        sb.append("FROM closure\n");
        sb.append("WHERE bit_count(closure.validity) != 0");

        // If end is a concrete URI, filter in the final SELECT too
        if (end.isURI()) {
            sb.append("\n  AND closure.id_end = ").append(resourceLookup(end.getURI()));
        }

        return sb.toString();
    }

    private String buildFlatSQL() {
        String predicateLookup = predicateSubquery();
        Node start = opTC.getStartNode();
        Node end = opTC.getEndNode();
        Node graph = opTC.getGraphNode();

        StringBuilder sb = new StringBuilder();

        // WITH RECURSIVE closure
        sb.append("WITH RECURSIVE closure(id_start, id_end, id_versioned_named_graph) AS (\n");

        // Base case
        sb.append("  SELECT t0.id_subject, t0.id_object, t0.id_versioned_named_graph\n");
        sb.append("  FROM versioned_quad_flat t0\n");
        sb.append("  WHERE t0.id_predicate = ").append(predicateLookup).append("\n");
        appendStartEndFilters(sb, false);

        sb.append("  UNION\n");

        // Recursive step
        sb.append("  SELECT c.id_start, t1.id_object, c.id_versioned_named_graph\n");
        sb.append("  FROM closure c\n");
        sb.append("  JOIN versioned_quad_flat t1 ON c.id_end = t1.id_subject\n");
        sb.append("    AND c.id_versioned_named_graph = t1.id_versioned_named_graph\n");
        sb.append("    AND t1.id_predicate = ").append(predicateLookup).append("\n");

        sb.append(")\n");

        // Final SELECT
        sb.append("SELECT ");
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

        sb.append(String.join(", ", selectCols)).append("\n");
        sb.append("FROM closure");

        if (end.isURI()) {
            sb.append("\nWHERE closure.id_end = ").append(resourceLookup(end.getURI()));
        }

        return sb.toString();
    }

    /**
     * Append WHERE filters for bound start/end nodes in the base case.
     */
    private void appendStartEndFilters(StringBuilder sb, boolean isCondensed) {
        Node start = opTC.getStartNode();
        Node end = opTC.getEndNode();

        if (start.isURI()) {
            sb.append("    AND t0.id_subject = ")
                    .append(resourceLookup(start.getURI())).append("\n");
        }
        if (end.isURI()) {
            sb.append("    AND t0.id_object = ")
                    .append(resourceLookup(end.getURI())).append("\n");
        }

        // For condensed mode with a URI graph, filter on named graph and validity
        Node graph = opTC.getGraphNode();
        if (isCondensed && graph.isURI()) {
            sb.append("    AND t0.id_named_graph = ")
                    .append(namedGraphLookup(graph.getURI())).append("\n");
            sb.append("    AND get_bit(t0.validity, ")
                    .append(versionIndexLookup(graph.getURI())).append(" - 1) = 1\n");
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

        if (start.isVariable()) {
            occurrences.computeIfAbsent(start, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(
                            SPARQLPositionType.SUBJECT,
                            0,
                            SPARQLContextType.VERSIONED_DATA,
                            new SQLVariable(SQLVarType.ID, start.getName())
                    ));
        }

        if (end.isVariable()) {
            occurrences.computeIfAbsent(end, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(
                            SPARQLPositionType.OBJECT,
                            0,
                            SPARQLContextType.VERSIONED_DATA,
                            new SQLVariable(SQLVarType.ID, end.getName())
                    ));
        }

        if (graph.isVariable() && context.condensedMode()) {
            occurrences.computeIfAbsent(graph, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(
                            SPARQLPositionType.GRAPH_NAME,
                            0,
                            SPARQLContextType.VERSIONED_DATA,
                            new SQLVariable(SQLVarType.CONDENSED, graph.getName())
                    ));
        } else if (graph.isVariable()) {
            occurrences.computeIfAbsent(graph, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(
                            SPARQLPositionType.GRAPH_NAME,
                            0,
                            SPARQLContextType.VERSIONED_DATA,
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
