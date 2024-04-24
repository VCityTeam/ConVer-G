package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.constants;

import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;

public class IntegerConstant extends Constant<NodeValueInteger> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public IntegerConstant(NodeValueInteger expr) {
        super(expr);
    }

    @Override
    public String toSQLString() {
        return getJenaExpr().toString();
    }

    @Override
    public String toSQLStringAgg() {
        return toSQLString();
    }
}
