package fr.cnrs.liris.jpugetgil.sparqltosql;

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
}
