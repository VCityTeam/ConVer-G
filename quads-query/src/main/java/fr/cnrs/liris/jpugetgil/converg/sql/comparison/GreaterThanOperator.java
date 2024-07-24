package fr.cnrs.liris.jpugetgil.converg.sql.comparison;

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
