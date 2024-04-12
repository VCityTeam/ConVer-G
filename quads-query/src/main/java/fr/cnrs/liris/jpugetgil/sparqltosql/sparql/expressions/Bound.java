package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op.AbstractFunction;
import org.apache.jena.sparql.expr.E_Bound;

public class Bound extends AbstractFunction<E_Bound> {
    public Bound(E_Bound expr) {
        super(expr, false);
    }
}
