package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sql.SQLClause;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLUtils;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVariable;
import fr.cnrs.liris.jpugetgil.converg.utils.Pair;

import java.util.List;

/**
 * SPARQL MINUS: keeps the left solutions that are not compatible with any right
 * solution on their shared variables (anti-join), translated as NOT EXISTS.
 * <p>
 * With no shared variable, MINUS removes nothing and the left query is returned
 * unchanged. A shared variable held in the CONDENSED representation (graph
 * variable in condensed mode) is flattened first so that the subtraction is
 * computed per versioned named graph, not per version set.
 */
public class MinusSQLOperator extends SQLOperator {

    private static final String LEFT_TABLE_NAME = "minus_left";
    private static final String RIGHT_TABLE_NAME = "minus_right";

    SQLQuery leftQuery;

    SQLQuery rightQuery;

    private final List<Pair<SQLVariable, SQLVariable>> commonVariables;

    public MinusSQLOperator(SQLQuery leftQuery, SQLQuery rightQuery) {
        this.leftQuery = leftQuery;
        this.rightQuery = rightQuery;
        this.commonVariables = SQLUtils.buildCommonsVariables(
                leftQuery.getContext().sparqlVarOccurrences(),
                rightQuery.getContext().sparqlVarOccurrences()
        );
    }

    /**
     * @return the Where part of the minus operator: an anti-join on the shared variables
     */
    @Override
    protected String buildWhere() {
        SQLClause.SQLClauseBuilder correlation = new SQLClause.SQLClauseBuilder();
        commonVariables.forEach(pair -> correlation.and(
                LEFT_TABLE_NAME + ".v$" + pair.getLeft().getSqlVarName() +
                        " = " + RIGHT_TABLE_NAME + ".v$" + pair.getRight().getSqlVarName()
        ));

        return " WHERE NOT EXISTS (SELECT 1 FROM (" + this.rightQuery.getSql() + ") " + RIGHT_TABLE_NAME +
                " WHERE " + correlation.build().clause + ")";
    }

    /**
     * @return the From part of the minus operator
     */
    @Override
    protected String buildFrom() {
        return " FROM (" + this.leftQuery.getSql() + ") " + LEFT_TABLE_NAME;
    }

    /**
     * @return the select part of the minus operator
     */
    @Override
    protected String buildSelect() {
        return "SELECT * ";
    }

    /**
     * @return then new SQLQuery containing the minus of the two subqueries
     */
    @Override
    public SQLQuery buildSQLQuery() {
        // MINUS with disjoint variable domains removes nothing
        if (commonVariables.isEmpty()) {
            return leftQuery;
        }

        flattenCondensedCommonVariables();

        String select = buildSelect();
        String from = buildFrom();
        String where = buildWhere();

        return new SQLQuery(
                select + from + where,
                leftQuery.getContext()
        );
    }

    /**
     * Flatten every shared variable held in CONDENSED representation on either
     * side, so the anti-join compares plain v$ columns version by version.
     */
    private void flattenCondensedCommonVariables() {
        commonVariables.forEach(pair -> {
            if (pair.getLeft().getSqlVarType() == SQLVarType.CONDENSED) {
                SQLVariable flattened = new SQLVariable(SQLVarType.ID, pair.getLeft().getSqlVarName());
                leftQuery = new FlattenSQLOperator(leftQuery, flattened).buildSQLQuery();
                pair.setLeft(flattened);
            }
            if (pair.getRight().getSqlVarType() == SQLVarType.CONDENSED) {
                SQLVariable flattened = new SQLVariable(SQLVarType.ID, pair.getRight().getSqlVarName());
                rightQuery = new FlattenSQLOperator(rightQuery, flattened).buildSQLQuery();
                pair.setRight(flattened);
            }
        });
    }
}
