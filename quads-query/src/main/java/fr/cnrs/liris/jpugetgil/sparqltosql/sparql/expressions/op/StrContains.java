package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_StrContains;

public class StrContains extends AbstractFunction<E_StrContains> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public StrContains(E_StrContains expr) {
        super(expr, true);
    }
}
