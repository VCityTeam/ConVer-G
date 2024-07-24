package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_Equals;

public class Equals extends AbstractFunction<E_Equals> {
    public Equals(E_Equals expr) {
        super(expr, false);
    }

    @Override
    public String toSQLString() {
        return "(" + args[0].toSQLString() + getJenaExpr().getOpName() + args[1].toSQLString() + ")";
    }

    @Override
    public String toSQLStringAgg() {
        return "(" + args[0].toSQLStringAgg() + getJenaExpr().getOpName() + args[1].toSQLStringAgg() + ")";
    }
}
