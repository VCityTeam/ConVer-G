package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_LogicalAnd;

public class LogicalAnd extends AbstractFunction<E_LogicalAnd> {
    public LogicalAnd(E_LogicalAnd expr) {
        super(expr, false);
    }

    @Override
    public String toSQLString() {
        return "(" + args[0].toSQLString() + " AND " + args[1].toSQLString() + ")";
    }

    @Override
    public String toSQLStringAgg() {
        return "(" + args[0].toSQLStringAgg() + " AND " + args[1].toSQLStringAgg() + ")";
    }

    @Override
    public String toNameSQLString() {
        return "(" + args[0].toNameSQLString() + " AND " + args[1].toNameSQLString() + ")";
    }
}
