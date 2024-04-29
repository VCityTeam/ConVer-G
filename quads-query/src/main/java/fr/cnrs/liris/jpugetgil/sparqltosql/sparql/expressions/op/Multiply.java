package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_Multiply;

public class Multiply extends AbstractFunction<E_Multiply> {
    public Multiply(E_Multiply expr) {
        super(expr, true);
    }

    @Override
    public String toSQLString() {
        return this.args[0].toSQLString() + getJenaExpr().getOpName() + this.args[1].toSQLString();
    }

    @Override
    public String toSQLStringAgg() {
        return "(" + args[0].toSQLStringAgg() + getJenaExpr().getOpName() + args[1].toSQLStringAgg() + ")";
    }
}
