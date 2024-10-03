package fr.cnrs.liris.jpugetgil.converg.sql.comparison;

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
