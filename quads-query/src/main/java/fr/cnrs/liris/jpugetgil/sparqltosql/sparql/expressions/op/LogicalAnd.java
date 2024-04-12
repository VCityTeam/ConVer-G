package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_LogicalAnd;

public class LogicalAnd extends AbstractFunction<E_LogicalAnd> {
    public LogicalAnd(E_LogicalAnd expr) {
        super(expr, false);
    }
}
