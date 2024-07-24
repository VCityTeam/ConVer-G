package fr.cnrs.liris.jpugetgil.converg.sparql.expressions;

import org.apache.jena.sparql.expr.Expr;

public abstract class AbstractExpression<E extends Expr> implements Expression {
    private E expr;

    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    protected AbstractExpression(E expr) {
        this.expr = expr;
    }

    @Override
    public E getJenaExpr() {
        return expr;
    }
}
