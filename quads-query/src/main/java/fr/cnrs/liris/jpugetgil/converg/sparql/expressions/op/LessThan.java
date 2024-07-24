package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_LessThan;

public class LessThan extends AbstractFunction<E_LessThan> {
    public LessThan(E_LessThan expr) {
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
