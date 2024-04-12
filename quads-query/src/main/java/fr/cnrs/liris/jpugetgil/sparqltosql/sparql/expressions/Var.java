package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import org.apache.jena.sparql.expr.ExprVar;

public class Var extends AbstractExpression<ExprVar> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public Var(ExprVar expr) {
        super(expr);
    }
}
