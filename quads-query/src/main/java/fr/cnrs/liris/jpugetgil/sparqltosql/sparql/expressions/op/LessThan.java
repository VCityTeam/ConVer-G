package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_LessThan;

public class LessThan extends AbstractFunction<E_LessThan> {
    public LessThan(E_LessThan expr) {
        super(expr, true);
    }
}
