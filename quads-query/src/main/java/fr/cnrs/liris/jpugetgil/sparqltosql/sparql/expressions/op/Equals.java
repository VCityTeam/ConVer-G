package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_Equals;

public class Equals extends AbstractFunction<E_Equals> {
    public Equals(E_Equals expr) {
        super(expr, false);
    }
}
