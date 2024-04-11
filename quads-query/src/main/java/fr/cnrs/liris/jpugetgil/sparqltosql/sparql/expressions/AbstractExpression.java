package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import org.apache.jena.sparql.expr.Expr;

public abstract class AbstractExpression implements Expression {
    private Expr expr;

    /**
     * Builds an expression from a jena expr
     * @param expr the source jena expr
     */
    public AbstractExpression(Expr expr) {
        this.expr = expr;
    }

    @Override
    public Expr getJenaExpr() {
        return expr;
    }
}
