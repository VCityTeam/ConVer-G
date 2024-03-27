package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

public class GreaterThanEqualToOperator implements ComparisonOperator {
    @Override
    public String getComparisonOperatorName() {
        return "GreaterThanEqualTo";
    }

    @Override
    public String getComparisonOperatorSymbol() {
        return ">=";
    }
}
