package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.entailment.SchemaDriftDetector;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLContextType;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLPositionType;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVariable;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.op.OpQuadPattern;
import org.apache.jena.sparql.core.Quad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SQL operator for the {@code <urn:converg:schema-drift>} virtual graph.
 * <p>
 * Generates SQL that returns all RDFS/OWL schema triples (rdfs:subClassOf,
 * rdfs:subPropertyOf, rdfs:domain, rdfs:range) from the versioned_quad table
 * with their validity bitsets. This lets users query schema drift with standard
 * SPARQL patterns:
 * <pre>
 * SELECT ?s ?p ?o WHERE {
 *     GRAPH &lt;urn:converg:schema-drift&gt; { ?s ?p ?o }
 * }
 * </pre>
 * In condensed mode the graph variable (or a synthetic one) carries the validity
 * bitset, so schema triples can be filtered by version using normal SPARQL operators.
 */
public class SchemaDriftSQLOperator extends SQLOperator {

    private final OpQuadPattern opQuadPattern;
    private final SQLContext context;

    public SchemaDriftSQLOperator(OpQuadPattern opQuadPattern, SQLContext context) {
        this.opQuadPattern = opQuadPattern;
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
        List<Quad> quads = opQuadPattern.getPattern().getList();
        // Use the first quad to extract s/p/o nodes
        Quad quad = quads.getFirst();
        Node subject = quad.getSubject();
        Node predicate = quad.getPredicate();
        Node object = quad.getObject();

        List<String> selectCols = new ArrayList<>();
        if (subject.isVariable()) {
            selectCols.add("vq.id_subject AS v$" + subject.getName());
        }
        if (predicate.isVariable()) {
            selectCols.add("vq.id_predicate AS v$" + predicate.getName());
        }
        if (object.isVariable()) {
            selectCols.add("vq.id_object AS v$" + object.getName());
        }
        // Always include validity for drift analysis
        selectCols.add("vq.id_named_graph AS ng$__drift_g");
        selectCols.add("vq.validity AS bs$__drift_g");

        String predicateFilter = buildPredicateFilter();

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ").append(String.join(", ", selectCols)).append("\n");
        sb.append("FROM versioned_quad vq\n");
        sb.append("WHERE ").append(predicateFilter);

        // Apply filters for bound s/p/o
        appendBoundFilters(sb, subject, predicate, object);

        return sb.toString();
    }

    private String buildFlatSQL() {
        List<Quad> quads = opQuadPattern.getPattern().getList();
        Quad quad = quads.getFirst();
        Node subject = quad.getSubject();
        Node predicate = quad.getPredicate();
        Node object = quad.getObject();

        List<String> selectCols = new ArrayList<>();
        if (subject.isVariable()) {
            selectCols.add("vq.id_subject AS v$" + subject.getName());
        }
        if (predicate.isVariable()) {
            selectCols.add("vq.id_predicate AS v$" + predicate.getName());
        }
        if (object.isVariable()) {
            selectCols.add("vq.id_object AS v$" + object.getName());
        }
        selectCols.add("vq.id_versioned_named_graph AS v$__drift_g");

        String predicateFilter = buildPredicateFilter();

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ").append(String.join(", ", selectCols)).append("\n");
        sb.append("FROM versioned_quad_flat vq\n");
        sb.append("JOIN versioned_named_graph vng ON vq.id_versioned_named_graph = vng.id_versioned_named_graph\n");
        sb.append("WHERE ").append(predicateFilter);

        appendBoundFilters(sb, subject, predicate, object);

        return sb.toString();
    }

    private void appendBoundFilters(StringBuilder sb, Node subject, Node predicate, Node object) {
        if (subject.isURI()) {
            sb.append("\n  AND vq.id_subject = ")
                    .append(resourceLookup(subject.getURI()));
        }
        if (predicate.isURI()) {
            sb.append("\n  AND vq.id_predicate = ")
                    .append(resourceLookup(predicate.getURI()));
        }
        if (object.isURI()) {
            sb.append("\n  AND vq.id_object = ")
                    .append(resourceLookup(object.getURI()));
        }
    }

    private String buildPredicateFilter() {
        return "vq.id_predicate IN (" +
                SchemaDriftDetector.SCHEMA_PREDICATES.stream()
                        .map(uri -> "(SELECT id_resource_or_literal FROM resource_or_literal WHERE name = '" + uri + "')")
                        .collect(Collectors.joining(", ")) +
                ")";
    }

    private Map<Node, List<SPARQLOccurrence>> createVarOccurrences() {
        Map<Node, List<SPARQLOccurrence>> occurrences = new HashMap<>();
        List<Quad> quads = opQuadPattern.getPattern().getList();
        Quad quad = quads.getFirst();

        if (quad.getSubject().isVariable()) {
            occurrences.computeIfAbsent(quad.getSubject(), k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(
                            SPARQLPositionType.SUBJECT, 0,
                            SPARQLContextType.VERSIONED_DATA,
                            new SQLVariable(SQLVarType.ID, quad.getSubject().getName())));
        }
        if (quad.getPredicate().isVariable()) {
            occurrences.computeIfAbsent(quad.getPredicate(), k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(
                            SPARQLPositionType.PREDICATE, 0,
                            SPARQLContextType.VERSIONED_DATA,
                            new SQLVariable(SQLVarType.ID, quad.getPredicate().getName())));
        }
        if (quad.getObject().isVariable()) {
            occurrences.computeIfAbsent(quad.getObject(), k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(
                            SPARQLPositionType.OBJECT, 0,
                            SPARQLContextType.VERSIONED_DATA,
                            new SQLVariable(SQLVarType.ID, quad.getObject().getName())));
        }

        // Internal graph variable carrying the validity bitset
        Node driftGraphVar = org.apache.jena.sparql.core.Var.alloc("__drift_g");
        if (context.condensedMode()) {
            occurrences.computeIfAbsent(driftGraphVar, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(
                            SPARQLPositionType.GRAPH_NAME, 0,
                            SPARQLContextType.VERSIONED_DATA,
                            new SQLVariable(SQLVarType.CONDENSED, "__drift_g")));
        } else {
            occurrences.computeIfAbsent(driftGraphVar, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(
                            SPARQLPositionType.GRAPH_NAME, 0,
                            SPARQLContextType.VERSIONED_DATA,
                            new SQLVariable(SQLVarType.ID, "__drift_g")));
        }

        return occurrences;
    }

    private static String resourceLookup(String uri) {
        return "(SELECT id_resource_or_literal FROM resource_or_literal WHERE name = '" + uri + "')";
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