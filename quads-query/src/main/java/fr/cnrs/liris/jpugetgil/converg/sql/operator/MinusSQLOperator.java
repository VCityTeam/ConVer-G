package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;

public class MinusSQLOperator extends SQLOperator {

    SQLQuery leftQuery;

    SQLQuery rightQuery;


    public MinusSQLOperator(SQLQuery leftQuery, SQLQuery rightQuery) {
        this.leftQuery = leftQuery;
        this.rightQuery = rightQuery;
    }

    /**
     * @return the Where part of the Join SQL Operator
     */
    @Override
    protected String buildWhere() {
        return "";
    }

    /**
     * @return the From part of the minus operator
     */
    @Override
    protected String buildFrom() {
        String LEFT_TABLE_NAME = "left_table";
        String RIGHT_TABLE_NAME = "right_table";

        return " FROM (" + this.leftQuery.getSql() + ") " + LEFT_TABLE_NAME + " EXCEPT \n" +
                " (SELECT * FROM (" + this.rightQuery.getSql() + ") " + RIGHT_TABLE_NAME + " )";
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
        String select = buildSelect();
        String from = buildFrom();
        String where = buildWhere();

        return new SQLQuery(
                select + from + where,
                leftQuery.getContext()
        );
    }
}
