package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_LessThanOrEqual;

public class LessThanOrEqual extends AbstractFunction<E_LessThanOrEqual> {
    public LessThanOrEqual(E_LessThanOrEqual expr) {
        super(expr, true);
    }
}
