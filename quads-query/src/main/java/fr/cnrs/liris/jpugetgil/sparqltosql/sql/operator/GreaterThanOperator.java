package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

public class GreaterThanOperator implements ComparisonOperator {
    @Override
    public String getComparisonOperatorName() {
        return "GreaterThan";
    }

    @Override
    public String getComparisonOperatorSymbol() {
        return ">";
    }
}
