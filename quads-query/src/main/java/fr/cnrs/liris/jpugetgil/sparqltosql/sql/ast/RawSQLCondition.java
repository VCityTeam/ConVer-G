package fr.cnrs.liris.jpugetgil.sparqltosql.sql.ast;

public record RawSQLCondition(String sql) implements Condition {
    @Override
    public String asSQL() {
        return "(" + sql + ")";
    }
}
