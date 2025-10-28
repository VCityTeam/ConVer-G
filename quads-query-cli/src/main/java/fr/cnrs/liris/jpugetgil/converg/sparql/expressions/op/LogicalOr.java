package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_LogicalOr;

public class LogicalOr extends AbstractFunction<E_LogicalOr> {
    public LogicalOr(E_LogicalOr expr) {
        super(expr, false);
    }

    @Override
    public String toSQLString() {
        return "(" + args[0].toSQLString() + " OR " + args[1].toSQLString() + ")";
    }

    @Override
    public String toSQLStringAgg() {
        return "(" + args[0].toSQLStringAgg() + " OR " + args[1].toSQLStringAgg() + ")";
    }

    @Override
    public String toNameSQLString() {
        return "(" + args[0].toNameSQLString() + " OR " + args[1].toNameSQLString() + ")";
    }
}
