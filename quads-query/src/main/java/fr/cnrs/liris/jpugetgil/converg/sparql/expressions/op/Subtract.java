package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.op;

import org.apache.jena.sparql.expr.E_Subtract;

public class Subtract extends AbstractFunction<E_Subtract> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public Subtract(E_Subtract expr) {
        super(expr, true);
    }

    @Override
    public String toSQLString() {
        return "(" + args[0].toSQLString() + "::float" + getJenaExpr().getOpName() + args[1].toSQLString() + "::float)";
    }

    @Override
    public String toSQLStringAgg() {
        return "(" + args[0].toSQLStringAgg() + "::float" + getJenaExpr().getOpName() + args[1].toSQLStringAgg() + "::float)";
    }

    @Override
    public String toNameSQLString() {
        return "(" + args[0].toNameSQLString() + "::float" + getJenaExpr().getOpName() + args[1].toNameSQLString() + "::float)";
    }
}
