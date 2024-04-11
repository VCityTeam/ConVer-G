package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import org.apache.jena.sparql.expr.Expr;

public abstract class Constant extends AbstractExpression {
    public Constant(Expr expr) {
        super(expr);
    }
}
