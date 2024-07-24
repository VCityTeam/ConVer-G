package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_GreaterThan;

public class GreaterThan extends AbstractFunction<E_GreaterThan> {
    public GreaterThan(E_GreaterThan expr) {
        super(expr, true);
    }

    @Override
    public String toSQLString() {
        return "(" + args[0].toSQLString() + "::float" + getJenaExpr().getOpName() + args[1].toSQLString() + "::float)";
    }

    @Override
    public String toSQLStringAgg() {
        return "(" + args[0].toSQLStringAgg() + "::float" + getJenaExpr().getOpName() + args[1].toSQLStringAgg() + "::float)";
    }
}
