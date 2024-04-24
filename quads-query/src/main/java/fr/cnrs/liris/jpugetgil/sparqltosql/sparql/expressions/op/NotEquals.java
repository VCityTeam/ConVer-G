package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_NotEquals;

public class NotEquals extends AbstractFunction<E_NotEquals> {
    public NotEquals(E_NotEquals expr) {
        super(expr, false);
    }

    @Override
    public String toSQLString() {
        return "(" + args[0].toSQLString() + getJenaExpr().getOpName() + args[1].toSQLString() + ")";
    }
}
