package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;

public class StSMinusOperator extends StSOperator {
    private final SQLQuery sqlQueryLeft;

    private final SQLQuery sqlQueryRight;


    public StSMinusOperator(SQLQuery sqlQueryLeft, SQLQuery sqlQueryRight) {
        this.sqlQueryLeft = sqlQueryLeft;
        this.sqlQueryRight = sqlQueryRight;
    }


    @Override
    public SQLQuery buildSQLQuery() {
        String select = "SELECT * ";
        String from = " FROM (" + this.sqlQueryLeft.getSql() + ") left_table EXCEPT \n";
        String where = " (SELECT * FROM (" + this.sqlQueryRight.getSql() + ") right_table )";

        return new SQLQuery(select + from + where, this.sqlQueryLeft.getContext());
    }
}
