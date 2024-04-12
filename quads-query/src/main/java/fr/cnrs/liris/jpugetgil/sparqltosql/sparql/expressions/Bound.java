package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import org.apache.jena.sparql.expr.E_Bound;

public class Bound extends AbstractExpression<E_Bound> {
    public Bound(E_Bound expr) {
        super(expr);
    }
}
