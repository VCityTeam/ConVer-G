package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_Divide;

public class Divide extends AbstractFunction<E_Divide> {
    public Divide(E_Divide expr) {
        super(expr, true);
    }
}
