package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

public class LessThanEqualToOperator implements ComparisonOperator {
    @Override
    public String getComparisonOperatorName() {
        return "LessThanEqualTo";
    }

    @Override
    public String getComparisonOperatorSymbol() {
        return "<=";
    }
}
