package fr.cnrs.liris.jpugetgil.sparqltosql.sql;

public class SQLQuery {

    private String sql;

    private SQLContext context;

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

    public SQLContext getContext() {
        return context;
    }

    public void setContext(SQLContext context) {
        this.context = context;
    }
}
