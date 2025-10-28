package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_UnaryMinus;

public class UnaryMinus extends AbstractFunction<E_UnaryMinus> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public UnaryMinus(E_UnaryMinus expr) {
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

    @Override
    public String toNameSQLString() {
        return "(" + args[0].toNameSQLString() + getJenaExpr().getOpName() + args[1].toNameSQLString() + ")";
    }
}
