package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_Str;

public class Str extends AbstractFunction<E_Str> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public Str(E_Str expr) {
        super(expr, true);
    }
}
