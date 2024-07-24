package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_StrBefore;

public class StrBefore extends AbstractFunction<E_StrBefore> {
    public StrBefore(E_StrBefore expr) {
        super(expr, true);
    }
}
