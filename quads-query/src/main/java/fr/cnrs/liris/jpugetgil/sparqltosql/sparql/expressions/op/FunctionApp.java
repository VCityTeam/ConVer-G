package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractExpression;
import org.apache.jena.sparql.expr.E_Function;

public class FunctionApp extends AbstractExpression<E_Function> {
    public FunctionApp(E_Function ef) {
        super(ef);
    }
}
