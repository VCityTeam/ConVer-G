package fr.cnrs.liris.jpugetgil.converg.sparql.expressions;

import org.apache.jena.sparql.ARQException;
import org.apache.jena.sparql.expr.Expr;

public class UnsupportedExpressionException extends ARQException {
    public final Expr expr;

    public UnsupportedExpressionException(Expr expr) {
        super("This expression type is not yet translated: " + expr.getClass().getName());
        this.expr = expr;
    }
}
