package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.op.OpDatasetNames;

import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLContextType;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLPositionType;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLClause;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVariable;
import fr.cnrs.liris.jpugetgil.converg.sql.comparison.EqualToOperator;

public class DatasetNamesSQLOperator extends SQLOperator {

    OpDatasetNames opDatasetNames;

    SQLContext context;

    Node graph;

    public DatasetNamesSQLOperator(OpDatasetNames opDatasetNames, SQLContext context) {
        this.opDatasetNames = opDatasetNames;
        this.context = context;
        this.graph = opDatasetNames.getGraphNode();
    }

    /**
     * @return the SQL query of the dataset names
     */
    @Override
    public SQLQuery buildSQLQuery() {
        context = context
                .copyWithNewVarOccurrences(createVarOccurrencesMap());

        String select = "SELECT " + buildSelect() + "\n";
        String from = "FROM " + buildFrom() + "\n";
        String where = buildWhere();

        String query = !where.isEmpty() ? select + from + "WHERE " + where : select + from;

        SQLQuery sqlQuery = new SQLQuery(query, context);

        return new SQLQuery(
                sqlQuery.getSql(),
                sqlQuery.getContext());
    }

    /**
     * @return the select part of a dataset names
     */
    @Override
    protected String buildSelect() {
        if (this.graph.isVariable()) {
            return "vq.id_subject, vq.id_predicate, vq.id_object, t.id_versioned_named_graph as v$" + this.graph.getName();
        } else {
            return "vq.id_subject, vq.id_predicate, vq.id_object, t.id_versioned_named_graph";
        }
    }

    /**
     * @return the from part of a dataset names
     */
    @Override
    protected String buildFrom() {
        return ("versioned_named_graph t JOIN versioned_quad vq ON t.id_named_graph = vq.id_named_graph AND get_bit(vq.validity, t.index_version - 1) = 1");
    }

    /**
     * @return the where part of a dataset names
     */
    @Override
    protected String buildWhere() {
        return (opDatasetNames.getGraphNode().isURI())
                ? generateWhere()
                : "";
    }

    /**
     * Generate the WHERE clause of the SQL query with a dataset name
     *
     * @return the WHERE clause of the SQL query
     */
    private String generateWhere() {
        SQLClause.SQLClauseBuilder sqlClauseBuilder = new SQLClause.SQLClauseBuilder();

        sqlClauseBuilder = sqlClauseBuilder.and(
                                new EqualToOperator().buildComparisonOperatorSQL(
                                        "t.id_versioned_named_graph",
                                        """
                                                (
                                                    SELECT rl.id_resource_or_literal
                                                    FROM resource_or_literal rl
                                                    WHERE rl.name = '""" + this.graph.getURI() + "')"));
        return sqlClauseBuilder.build().clause;
    }

    private Map<Node, List<SPARQLOccurrence>> createVarOccurrencesMap() {
        Map<Node, List<SPARQLOccurrence>> newVarOccurrences = new HashMap<>();
        SPARQLContextType sparqlContextType = SPARQLContextType.VERSIONED_DATA;

        if (graph.isVariable()) {
            newVarOccurrences.computeIfAbsent(graph, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(
                            SPARQLPositionType.GRAPH_NAME,
                            0, sparqlContextType,
                            new SQLVariable(SQLVarType.ID, graph.getName())));
        }

        return newVarOccurrences;
    }
}
