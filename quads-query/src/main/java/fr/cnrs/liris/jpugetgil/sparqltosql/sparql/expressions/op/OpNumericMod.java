package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_OpNumericMod;

public class OpNumericMod extends AbstractFunction<E_OpNumericMod> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public OpNumericMod(E_OpNumericMod expr) {
        super(expr, true);
    }
}
