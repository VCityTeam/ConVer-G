package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_UnaryPlus;

public class UnaryPlus extends AbstractFunction<E_UnaryPlus> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public UnaryPlus(E_UnaryPlus expr) {
        super(expr, true);
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
