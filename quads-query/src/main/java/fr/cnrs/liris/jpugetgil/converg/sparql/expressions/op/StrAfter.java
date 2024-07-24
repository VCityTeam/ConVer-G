package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_StrAfter;

public class StrAfter extends AbstractFunction<E_StrAfter> {
    public StrAfter(E_StrAfter expr) {
        super(expr, true);
    }
}
