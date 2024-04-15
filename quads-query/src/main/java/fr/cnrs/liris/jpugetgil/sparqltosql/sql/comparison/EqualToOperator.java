package fr.cnrs.liris.jpugetgil.sparqltosql.sql.comparison;

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
