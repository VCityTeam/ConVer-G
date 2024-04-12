package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_LogicalNot;

public class LogicalNot extends AbstractFunction<E_LogicalNot> {
    public LogicalNot(E_LogicalNot expr) {
        super(expr, false);
    }
}
