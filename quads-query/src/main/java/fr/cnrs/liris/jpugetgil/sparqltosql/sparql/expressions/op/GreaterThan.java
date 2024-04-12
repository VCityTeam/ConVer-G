package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_GreaterThan;

public class GreaterThan extends AbstractFunction<E_GreaterThan> {
    public GreaterThan(E_GreaterThan expr) {
        super(expr, true);
    }
}
