package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_GreaterThanOrEqual;

public class GreaterThanOrEqual extends AbstractFunction<E_GreaterThanOrEqual> {
    public GreaterThanOrEqual(E_GreaterThanOrEqual expr) {
        super(expr, true);
    }
}
