package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.Expression;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLClause;
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

    /**
     * @return
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

        return " FROM (" + leftQuery.getSql() + ") " + LEFT_TABLE_NAME + " LEFT JOIN (" +
                rightQuery.getSql() + ") " + RIGHT_TABLE_NAME + " ON " + sqlJoinClauseBuilder.build().clause;
    }

    /**
     * @return
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
}
