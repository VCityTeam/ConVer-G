package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

public interface ComparisonOperator {
    String getComparisonOperatorName();
    String getComparisonOperatorSymbol();
    default String buildComparisonOperatorSQL(String left, String right) {
        return left + " " + getComparisonOperatorSymbol() + " " + right;
    }
}
