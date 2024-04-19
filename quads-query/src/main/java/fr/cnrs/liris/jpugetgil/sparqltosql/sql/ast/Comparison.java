package fr.cnrs.liris.jpugetgil.sparqltosql.sql.ast;

import fr.cnrs.liris.jpugetgil.sparqltosql.sql.comparison.ComparisonOperator;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.comparison.EqualToOperator;

public class Comparison implements Condition {
    private ComparisonOperator op;
    private String left;
    private String right;

    public Comparison(String left, ComparisonOperator op, String right) {
        this.left = left;
        this.op = op;
        this.right = right;
    }

    @Override
    public String asSQL() {
        return left + " " + op.getComparisonOperatorSymbol() + " " + right;
    }

    public static Comparison eq (String left, String right) {
        return new Comparison(left, new EqualToOperator(), right);
    }
}
