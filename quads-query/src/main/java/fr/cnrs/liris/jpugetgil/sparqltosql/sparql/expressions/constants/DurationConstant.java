package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.constants;

import org.apache.jena.sparql.expr.nodevalue.NodeValueDuration;

public class DurationConstant extends Constant<NodeValueDuration> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public DurationConstant(NodeValueDuration expr) {
        super(expr);
    }
}
