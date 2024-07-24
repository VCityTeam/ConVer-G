package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_NumAbs;

public class NumAbs extends AbstractFunction<E_NumAbs> {
    public NumAbs(E_NumAbs expr) {
        super(expr, true);
    }
}
