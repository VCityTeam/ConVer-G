package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_GreaterThanOrEqual;

public class GreaterThanOrEqual extends AbstractFunction<E_GreaterThanOrEqual> {
    public GreaterThanOrEqual(E_GreaterThanOrEqual expr) {
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

    @Override
    public String toNameSQLString() {
        return "(" + args[0].toNameSQLString() + "::float" + getJenaExpr().getOpName() + args[1].toNameSQLString() + "::float)";
    }
}
