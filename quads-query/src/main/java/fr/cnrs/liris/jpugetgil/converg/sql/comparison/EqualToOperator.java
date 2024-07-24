package fr.cnrs.liris.jpugetgil.converg.sql.comparison;

public class EqualToOperator implements ComparisonOperator {
    @Override
    public String getComparisonOperatorName() {
        return "EqualTo";
    }

    @Override
    public String getComparisonOperatorSymbol() {
        return "=";
    }
}
