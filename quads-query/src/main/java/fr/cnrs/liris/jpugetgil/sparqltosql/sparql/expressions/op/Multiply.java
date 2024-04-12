package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_Multiply;

public class Multiply extends AbstractFunction<E_Multiply> {
    public Multiply(E_Multiply expr) {
        super(expr, true);
    }
}
