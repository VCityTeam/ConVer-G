package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_LogicalOr;

public class LogicalOr extends AbstractFunction<E_LogicalOr> {
    public LogicalOr(E_LogicalOr expr) {
        super(expr, false);
    }
}
