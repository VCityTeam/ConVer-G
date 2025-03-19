package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_NotEquals;

public class NotEquals extends AbstractFunction<E_NotEquals> {
    public NotEquals(E_NotEquals expr) {
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

    @Override
    public String toNameSQLString() {
        return "(" + args[0].toNameSQLString() + getJenaExpr().getOpName() + args[1].toNameSQLString() + ")";
    }
}
