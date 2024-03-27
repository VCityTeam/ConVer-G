package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

public class NotEqualToOperator implements ComparisonOperator {
    @Override
    public String getComparisonOperatorName() {
        return "NotEqualTo";
    }

    @Override
    public String getComparisonOperatorSymbol() {
        return "<>";
    }
}
