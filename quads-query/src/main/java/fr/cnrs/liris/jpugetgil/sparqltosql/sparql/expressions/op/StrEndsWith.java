package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_StrEndsWith;

public class StrEndsWith extends AbstractFunction<E_StrEndsWith> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public StrEndsWith(E_StrEndsWith expr) {
        super(expr, true);
    }
}
