package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_Multiply;

public class Multiply extends AbstractFunction<E_Multiply> {
    public Multiply(E_Multiply expr) {
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
