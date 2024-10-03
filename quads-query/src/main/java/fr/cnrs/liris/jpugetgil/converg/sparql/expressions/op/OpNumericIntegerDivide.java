package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_OpNumericIntegerDivide;

public class OpNumericIntegerDivide extends AbstractFunction<E_OpNumericIntegerDivide> {
    public OpNumericIntegerDivide(E_OpNumericIntegerDivide expr) {
        super(expr, true);
    }
}
