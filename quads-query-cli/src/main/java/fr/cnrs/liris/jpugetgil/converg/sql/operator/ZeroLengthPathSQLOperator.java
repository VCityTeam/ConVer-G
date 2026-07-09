package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.path.OpZeroLengthPath;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLContextType;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLPositionType;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVariable;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Quad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL operator for the zero-length component of a {@code path?} property path:
 * binds the variable endpoint to the concrete term.
 * <ul>
 *   <li>default graph (metadata) or concrete graph: a single row binding the variable;</li>
 *   <li>graph variable, condensed: one row per named graph, valid in every version
 *       where the graph exists ({@code bit_or(validity)});</li>
 *   <li>graph variable, flat: one row per versioned named graph.</li>
 * </ul>
 * Note: the term must exist in the {@code resource_or_literal} dictionary,
 * otherwise the binding resolves to NULL and the row is dropped downstream.
 */
public class ZeroLengthPathSQLOperator extends SQLOperator {

    private final OpZeroLengthPath opZLP;
    private final SQLContext context;

    public ZeroLengthPathSQLOperator(OpZeroLengthPath opZLP, SQLContext context) {
        this.opZLP = opZLP;
        this.context = context;
    }

    @Override
    public SQLQuery buildSQLQuery() {
        SQLContext newContext = context.copyWithNewVarOccurrences(createVarOccurrences());

        Node graph = opZLP.getGraphNode();
        String termLookup = resourceLookup(opZLP.getTermNode().getURI());
        String varName = opZLP.getVarNode().getName();

        String sql;
        if (graph == Quad.defaultGraphNodeGenerated || graph.isURI()) {
            sql = "SELECT " + termLookup + " AS v$" + varName;
        } else if (context.condensedMode()) {
            sql = "SELECT vq.id_named_graph AS ng$" + graph.getName() +
                    ", bit_or(vq.validity) AS bs$" + graph.getName() +
                    ", " + termLookup + " AS v$" + varName + "\n" +
                    "FROM versioned_quad vq\n" +
                    "GROUP BY vq.id_named_graph";
        } else {
            sql = "SELECT DISTINCT vq.id_versioned_named_graph AS v$" + graph.getName() +
                    ", " + termLookup + " AS v$" + varName + "\n" +
                    "FROM versioned_quad_flat vq";
        }

        return new SQLQuery(sql, newContext);
    }

    private Map<Node, List<SPARQLOccurrence>> createVarOccurrences() {
        Map<Node, List<SPARQLOccurrence>> occurrences = new HashMap<>();
        Node graph = opZLP.getGraphNode();
        SPARQLContextType contextType = graph == Quad.defaultGraphNodeGenerated
                ? SPARQLContextType.METADATA
                : SPARQLContextType.VERSIONED_DATA;

        occurrences.computeIfAbsent(opZLP.getVarNode(), k -> new ArrayList<>())
                .add(new SPARQLOccurrence(
                        SPARQLPositionType.OBJECT,
                        0,
                        contextType,
                        new SQLVariable(SQLVarType.ID, opZLP.getVarNode().getName())
                ));

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

    private String resourceLookup(String uri) {
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
