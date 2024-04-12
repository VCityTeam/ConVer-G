package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_Function;

public class FunctionApp extends AbstractFunction<E_Function> {
    public FunctionApp(E_Function ef) {
        super(ef, true);
    }
}
