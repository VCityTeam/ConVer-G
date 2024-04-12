package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_Add;

public class Add extends AbstractFunction<E_Add> {
    public Add(E_Add expr) {
        super(expr, true);
    }
}
