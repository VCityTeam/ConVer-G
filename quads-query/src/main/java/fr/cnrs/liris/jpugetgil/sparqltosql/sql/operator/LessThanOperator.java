package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

public class LessThanOperator implements ComparisonOperator {
    @Override
    public String getComparisonOperatorName() {
        return "LessThan";
    }

    @Override
    public String getComparisonOperatorSymbol() {
        return "<";
    }
}
