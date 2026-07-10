package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.entailment.OpGraphVersionIntersection;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLUtils;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVarType;
import org.apache.jena.graph.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL operator for {@link OpGraphVersionIntersection}.
 * <p>
 * The sub-query binds two graph coordinates: the data graph (variable or
 * versioned graph URI) and the rewriter-allocated schema graph variable. This
 * operator keeps the rows whose version sets intersect and discards the schema
 * graph columns:
 * <ul>
 *   <li>condensed mode: {@code bit_count(bs$data & bs$schema) <> 0}, and the data
 *       graph's version set is rebound to the intersection. When the data graph is
 *       a concrete versioned graph URI, the schema version set must instead contain
 *       that graph's version index.</li>
 *   <li>flat mode: both graph coordinates are versioned named graph ids; they must
 *       belong to the same {@code index_version} (resolved through the
 *       {@code versioned_named_graph} table).</li>
 * </ul>
 */
public class GraphVersionIntersectionSQLOperator extends SQLOperator {

    private static final String INTERSECTION_TABLE_NAME = "gvi_table";

    private final OpGraphVersionIntersection opGVI;
    private final SQLQuery subQuery;

    public GraphVersionIntersectionSQLOperator(OpGraphVersionIntersection opGVI, SQLQuery subQuery) {
        this.opGVI = opGVI;
        this.subQuery = subQuery;
    }

    @Override
    public SQLQuery buildSQLQuery() {
        String select = "SELECT " + buildSelect() + "\n";
        String from = "FROM " + buildFrom() + "\n";
        String where = buildWhere();

        String sql = where.isEmpty() ? select + from : select + from + "WHERE " + where;

        return new SQLQuery(sql, buildContext());
    }

    /**
     * Project every sub-query variable except the schema graph one; the data
     * graph's version set becomes the intersection of both version sets.
     */
    @Override
    protected String buildSelect() {
        Map<Node, List<SPARQLOccurrence>> occurrences = subQuery.getContext().sparqlVarOccurrences();
        Node dataGraph = opGVI.getDataGraphNode();

        List<String> columns = new ArrayList<>();
        occurrences.forEach((node, nodeOccurrences) -> {
            if (node.equals(opGVI.getSchemaGraphVar())) {
                return;
            }
            SPARQLOccurrence maxOccurrence = SQLUtils.getMaxSPARQLOccurrence(nodeOccurrences);
            if (node.equals(dataGraph)
                    && maxOccurrence.getSqlVariable().getSqlVarType() == SQLVarType.CONDENSED) {
                columns.add("(" + INTERSECTION_TABLE_NAME + ".bs$" + node.getName()
                        + " & " + INTERSECTION_TABLE_NAME + ".bs$" + opGVI.getSchemaGraphVar().getName()
                        + ") AS bs$" + node.getName()
                        + ", " + INTERSECTION_TABLE_NAME + ".ng$" + node.getName());
            } else {
                columns.add(maxOccurrence.getSqlVariable().getSelect(INTERSECTION_TABLE_NAME));
            }
        });

        if (columns.isEmpty()) {
            columns.add("1");
        }
        return String.join(", ", columns);
    }

    @Override
    protected String buildFrom() {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(subQuery.getSql()).append(") ").append(INTERSECTION_TABLE_NAME);

        if (!isSchemaGraphCondensed()) {
            // Flat mode: resolve both versioned named graph ids to their version index
            sb.append("\nJOIN versioned_named_graph vng_schema ON vng_schema.id_versioned_named_graph = ")
                    .append(INTERSECTION_TABLE_NAME).append(".v$").append(opGVI.getSchemaGraphVar().getName());
            if (opGVI.getDataGraphNode().isVariable()) {
                sb.append("\nJOIN versioned_named_graph vng_data ON vng_data.id_versioned_named_graph = ")
                        .append(INTERSECTION_TABLE_NAME).append(".v$").append(opGVI.getDataGraphNode().getName());
            }
        }
        return sb.toString();
    }

    @Override
    protected String buildWhere() {
        Node dataGraph = opGVI.getDataGraphNode();
        String schemaGraphName = opGVI.getSchemaGraphVar().getName();

        if (isSchemaGraphCondensed()) {
            if (dataGraph.isVariable()) {
                return "bit_count(" + INTERSECTION_TABLE_NAME + ".bs$" + dataGraph.getName()
                        + " & " + INTERSECTION_TABLE_NAME + ".bs$" + schemaGraphName + ") <> 0";
            }
            // Concrete versioned graph URI: the schema premise must exist in that version
            return "get_bit(" + INTERSECTION_TABLE_NAME + ".bs$" + schemaGraphName + ", "
                    + versionIndexLookup(dataGraph.getURI()) + " - 1) = 1";
        }

        if (dataGraph.isVariable()) {
            return "vng_data.index_version = vng_schema.index_version";
        }
        return "vng_schema.index_version = " + versionIndexLookup(dataGraph.getURI());
    }

    /**
     * The schema graph variable comes out of a quad pattern or transitive closure:
     * CONDENSED (ng$/bs$) in condensed mode, ID (versioned named graph id) in flat mode.
     */
    private boolean isSchemaGraphCondensed() {
        List<SPARQLOccurrence> schemaOccurrences =
                subQuery.getContext().sparqlVarOccurrences().get(opGVI.getSchemaGraphVar());
        if (schemaOccurrences == null) {
            throw new IllegalStateException(
                    "Schema graph variable " + opGVI.getSchemaGraphVar()
                            + " is not bound by the sub-query of OpGraphVersionIntersection");
        }
        return SQLUtils.getMaxSPARQLOccurrence(schemaOccurrences)
                .getSqlVariable().getSqlVarType() == SQLVarType.CONDENSED;
    }

    private SQLContext buildContext() {
        Map<Node, List<SPARQLOccurrence>> newOccurrences =
                new HashMap<>(subQuery.getContext().sparqlVarOccurrences());
        newOccurrences.remove(opGVI.getSchemaGraphVar());
        return subQuery.getContext().copyWithNewVarOccurrences(newOccurrences);
    }

    private String versionIndexLookup(String uri) {
        return """
                (SELECT vng.index_version
                 FROM versioned_named_graph vng JOIN resource_or_literal rl ON
                 vng.id_versioned_named_graph = rl.id_resource_or_literal
                 WHERE rl.name = '""" + uri + "')";
    }
}
