package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sql.*;
import fr.cnrs.liris.jpugetgil.converg.utils.Pair;
import org.apache.jena.graph.Node;

import java.util.List;
import java.util.Map;

public class UnionSQLOperator extends SQLOperator {

    SQLQuery leftQuery;

    SQLQuery rightQuery;

    Map<Node, List<SPARQLOccurrence>> mergedMapOccurrences;


    public UnionSQLOperator(SQLQuery leftQuery, SQLQuery rightQuery) {
        this.leftQuery = leftQuery;
        this.rightQuery = rightQuery;
    }

    /**
     * @return
     */
    @Override
    protected String buildWhere() {
        return "";
    }

    /**
     * @return
     */
    @Override
    protected String buildFrom() {
        String UNION_TABLE_NAME = "union_table";

        return " FROM (" + leftQuery.getSql() + " UNION " +
                rightQuery.getSql() + ") " + UNION_TABLE_NAME;
    }

    /**
     * @return
     */
    @Override
    protected String buildSelect() {
        return "SELECT * ";
    }

    /**
     * @return
     */
    @Override
    public SQLQuery buildSQLQuery() {
        unionSubQueries();

        String select = buildSelect();
        String from = buildFrom();
        String where = buildWhere();

        return new SQLQuery(
                select + from + where,
                new SQLContext(
                        mergedMapOccurrences,
                        leftQuery.getContext().condensedMode()
                ));
    }

    private void unionSubQueries() {
        SQLUtils.getAllCondensedVariables(leftQuery.getContext().sparqlVarOccurrences())
                .forEach(sqlVariable -> leftQuery = new FlattenSQLOperator(leftQuery, sqlVariable).buildSQLQuery());

        SQLUtils.getAllCondensedVariables(rightQuery.getContext().sparqlVarOccurrences())
                .forEach(sqlVariable -> rightQuery = new FlattenSQLOperator(rightQuery, sqlVariable).buildSQLQuery());
    }
}
