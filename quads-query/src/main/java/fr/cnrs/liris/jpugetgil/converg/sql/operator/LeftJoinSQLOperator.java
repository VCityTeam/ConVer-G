package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.Expression;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLClause;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLUtils;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;

import java.util.stream.Collectors;

public class LeftJoinSQLOperator extends JoinSQLOperator {

    private final OpLeftJoin opLeftJoin;
    private final String LEFT_TABLE_NAME = "left_table";
    private final String RIGHT_TABLE_NAME = "right_table";

    public LeftJoinSQLOperator(
            OpLeftJoin opLeftJoin,
            SQLQuery leftSQLQuery,
            SQLQuery rightSQLQuery
    ) {
        super(leftSQLQuery, rightSQLQuery);
        this.mergedMapOccurrences = SQLUtils.mergeMapOccurrencesLeftJoin(
                leftQuery.getContext().sparqlVarOccurrences(),
                rightQuery.getContext().sparqlVarOccurrences()
        );
        this.opLeftJoin = opLeftJoin;
    }

    /**
     * @return the select part of the Join SQL Operator
     */
    @Override
    protected String buildSelect() {
        return "SELECT " + mergedMapOccurrences
                .keySet()
                .stream()
                .map((node) -> SQLUtils.generateLeftJoinNodeProjectionByListSPARQLOccurrences(
                        leftQuery.getContext().sparqlVarOccurrences().get(node),
                        rightQuery.getContext().sparqlVarOccurrences().get(node)
                ))
                .collect(Collectors.joining(", ")) + "\n";
    }

    private String buildDifferenceGroupBy() {
        return "GROUP BY " + leftQuery.getContext().sparqlVarOccurrences()
                .keySet()
                .stream()
                .map((node) -> SQLUtils.getMaxSPARQLOccurrence(leftQuery.getContext().sparqlVarOccurrences().get(node)))
                .map(sparqlOccurrence -> sparqlOccurrence.getSqlVariable().getSelect(LEFT_TABLE_NAME))
                .collect(Collectors.joining(", "));
    }

    /**
     * @return the select part of the Join SQL Operator
     */
    protected String buildDifferenceSelect() {
        return "SELECT " + mergedMapOccurrences
                .keySet()
                .stream()
                .map((node) -> SQLUtils.generateDifferenceLeftJoinNodeProjectionByListSPARQLOccurrences(
                        leftQuery.getContext().sparqlVarOccurrences().get(node),
                        rightQuery.getContext().sparqlVarOccurrences().get(node)
                ))
                .collect(Collectors.joining(", ")) + "\n";
    }

    /**
     * @return the from part of the Join SQL Operator
     */
    @Override
    protected String buildFrom() {
        SQLClause.SQLClauseBuilder sqlJoinClauseBuilder = new SQLClause.SQLClauseBuilder();

        if (commonVariables.isEmpty()) {
            sqlJoinClauseBuilder.and("1 = 1");
        } else {
            commonVariables.forEach(sqlVariablePair -> sqlJoinClauseBuilder.and(
                    sqlVariablePair.getLeft().joinLeftJoin(
                            sqlVariablePair.getRight(),
                            LEFT_TABLE_NAME,
                            RIGHT_TABLE_NAME
                    )
            ));
        }

        return " FROM " + LEFT_TABLE_NAME + " LEFT JOIN " + RIGHT_TABLE_NAME + " ON " + sqlJoinClauseBuilder.build().clause;
    }

    /**
     * @return the from difference part of the Join SQL Operator
     */
    protected String buildDifferenceFrom() {
        SQLClause.SQLClauseBuilder sqlJoinClauseBuilder = new SQLClause.SQLClauseBuilder();

        if (commonVariables.isEmpty()) {
            sqlJoinClauseBuilder.and("1 = 1");
        } else {
            commonVariables.forEach(sqlVariablePair -> sqlJoinClauseBuilder.and(
                    sqlVariablePair.getLeft().joinLeftJoin(
                            sqlVariablePair.getRight(),
                            LEFT_TABLE_NAME,
                            RIGHT_TABLE_NAME
                    )
            ));
        }

        return " FROM " + LEFT_TABLE_NAME + " JOIN " + RIGHT_TABLE_NAME + " ON " + sqlJoinClauseBuilder.build().clause;
    }

    /**
     * @return the where part of the Join SQL Operator
     */
    @Override
    protected String buildWhere() {
        if (opLeftJoin.getExprs() == null) {
            return "";
        }

        return opLeftJoin.getExprs().getList().stream()
                .map(Expression::fromJenaExpr)
                .map(Expression::toSQLString)
                .collect(Collectors.joining(" AND "));
    }

    /**
     * @return then new SQLQuery containing the join of the two subqueries
     */
    @Override
    public SQLQuery buildSQLQuery() {
        joinSubQueries();

        // similar or right part not existing
        String selectSimOrNotExist = buildSelect();
        String fromSimOrNotExist = buildFrom();
        String whereSimOrNotExist = buildWhere();

        String sql = "WITH " + LEFT_TABLE_NAME + " AS (" + leftQuery.getSql() + "),\n" +
                RIGHT_TABLE_NAME + " AS (" + rightQuery.getSql() + ")\n" +
                selectSimOrNotExist + fromSimOrNotExist + whereSimOrNotExist;

        if (leftQuery.getContext().condensedMode()) {
            String differenceSelect = buildDifferenceSelect();
            String differenceFrom = buildDifferenceFrom();
            String groupByDifferences = buildDifferenceGroupBy();

            sql = sql + "\n UNION \n "
                    + differenceSelect + differenceFrom + whereSimOrNotExist + "\n" + groupByDifferences;
        }

        return new SQLQuery(
                sql,
                new SQLContext(
                        mergedMapOccurrences,
                        leftQuery.getContext().condensedMode(),
                        null,
                        null
                ));
    }
}
