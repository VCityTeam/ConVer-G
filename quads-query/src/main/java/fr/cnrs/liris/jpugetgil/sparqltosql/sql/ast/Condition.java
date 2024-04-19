package fr.cnrs.liris.jpugetgil.sparqltosql.sql.ast;

public interface Condition {
    /**
     * Renders this condition as a SQL String
     * @return the generated SQL string
     */
    String asSQL();
}
