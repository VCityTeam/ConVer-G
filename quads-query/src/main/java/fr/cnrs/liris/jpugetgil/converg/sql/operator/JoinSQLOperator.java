package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sql.*;
import fr.cnrs.liris.jpugetgil.converg.utils.Pair;
import org.apache.jena.graph.Node;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JoinSQLOperator extends SQLOperator {

    protected SQLQuery leftQuery;
    protected SQLQuery rightQuery;
    protected List<Pair<SQLVariable, SQLVariable>> commonVariables;
    protected Map<Node, List<SPARQLOccurrence>> mergedMapOccurrences;

    private final String LEFT_TABLE_NAME = "left_table";
    private final String RIGHT_TABLE_NAME = "right_table";

    public JoinSQLOperator(SQLQuery leftQuery, SQLQuery rightQuery) {
        this.leftQuery = leftQuery;
        this.rightQuery = rightQuery;
        this.commonVariables = SQLUtils.buildCommonsVariables(
                leftQuery.getContext().sparqlVarOccurrences(),
                rightQuery.getContext().sparqlVarOccurrences()
        );
        this.mergedMapOccurrences = SQLUtils.mergeMapOccurrences(
                leftQuery.getContext().sparqlVarOccurrences(),
                rightQuery.getContext().sparqlVarOccurrences()
        );
    }

    /**
     * @return the Where part of the Join SQL Operator
     */
    @Override
    protected String buildWhere() {
        return "";
    }

    /**
     * @return the From part of the Join SQL Operator
     */
    @Override
    protected String buildFrom() {
        SQLClause.SQLClauseBuilder sqlJoinClauseBuilder = new SQLClause.SQLClauseBuilder();

        if (commonVariables.isEmpty()) {
            sqlJoinClauseBuilder.and("1 = 1");
        } else {
            commonVariables.forEach(sqlVariablePair -> sqlJoinClauseBuilder.and(
                    sqlVariablePair.getLeft().joinJoin(
                            sqlVariablePair.getRight(),
                            LEFT_TABLE_NAME,
                            RIGHT_TABLE_NAME
                    )
            ));
        }

        return " FROM (" + leftQuery.getSql() + ") " + LEFT_TABLE_NAME + " JOIN (" +
                rightQuery.getSql() + ") " + RIGHT_TABLE_NAME + " ON " + sqlJoinClauseBuilder.build().clause;
    }

    /**
     * @return the select part of the Join SQL Operator
     */
    @Override
    protected String buildSelect() {
        return "SELECT " + mergedMapOccurrences
                .keySet()
                .stream()
                .map((node) -> SQLUtils.generateNodeProjectionByListSPARQLOccurrences(
                        leftQuery.getContext().sparqlVarOccurrences().get(node),
                        rightQuery.getContext().sparqlVarOccurrences().get(node)
                ))
                .collect(Collectors.joining(", ")) + "\n";
    }

    /**
     * @return then new SQLQuery containing the join of the two subqueries
     */
    @Override
    public SQLQuery buildSQLQuery() {
        joinSubQueries();

        String select = buildSelect();
        String from = buildFrom();
        String where = buildWhere();

        return new SQLQuery(
                select + from + where,
                new SQLContext(
                        mergedMapOccurrences,
                        leftQuery.getContext().condensedMode(),
                        null,
                        null
                ));
    }

    /**
     * @implNote flatten joined variable if they have two different representation
     */
    protected void joinSubQueries() {
        commonVariables.forEach(sqlVariablePair -> {
            if (sqlVariablePair.getLeft().getSqlVarType().isLower(sqlVariablePair.getRight().getSqlVarType())) {
                // Change the representation of the variable
                leftQuery = new FlattenSQLOperator(leftQuery, sqlVariablePair.getRight()).buildSQLQuery();
                sqlVariablePair.setLeft(sqlVariablePair.getRight());
            }
            if (sqlVariablePair.getLeft().getSqlVarType().isHigher(sqlVariablePair.getRight().getSqlVarType())) {
                // Change the representation of the variable
                rightQuery = new FlattenSQLOperator(rightQuery, sqlVariablePair.getRight()).buildSQLQuery();
                sqlVariablePair.setRight(sqlVariablePair.getLeft());
            }
        });
    }
}
