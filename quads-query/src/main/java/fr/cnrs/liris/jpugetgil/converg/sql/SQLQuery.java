package fr.cnrs.liris.jpugetgil.converg.sql;

import org.apache.jena.sparql.algebra.op.OpSlice;

public class SQLQuery {

    private String sql;

    private final SQLContext context;

    private OpSlice opSlice;

    public SQLQuery(String sql, SQLContext context) {
        this.sql = sql;
        this.context = context;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public void setOpSlice(OpSlice opSlice) {
        this.opSlice = opSlice;
    }

    public SQLContext getContext() {
        return context;
    }

    public SQLQuery finalizeQuery() {

        if (this.opSlice != null) {
            insertLimit();
        }

        return new SQLQuery(
                this.sql,
                this.context
        );
    }

    private void insertLimit() {
        String select = "SELECT * ";
        String from = " FROM (" + this.sql + ") sl \n";
        String limit;
        if (opSlice.getStart() > 0) {
            limit = "LIMIT " + opSlice.getLength() + " OFFSET " + opSlice.getStart();
        } else {
            limit = "LIMIT " + opSlice.getLength();
        }
        this.sql = select + from + limit;
    }
}
