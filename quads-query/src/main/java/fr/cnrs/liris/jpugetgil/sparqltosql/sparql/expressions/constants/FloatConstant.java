package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.constants;

import org.apache.jena.sparql.expr.nodevalue.NodeValueFloat;

public class FloatConstant extends Constant<NodeValueFloat> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public FloatConstant(NodeValueFloat expr) {
        super(expr);
    }
}
