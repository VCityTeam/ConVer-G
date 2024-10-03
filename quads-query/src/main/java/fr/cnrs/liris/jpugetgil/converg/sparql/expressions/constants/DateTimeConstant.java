package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.constants;

import org.apache.jena.sparql.expr.nodevalue.NodeValueDateTime;

public class DateTimeConstant extends Constant<NodeValueDateTime> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public DateTimeConstant(NodeValueDateTime expr) {
        super(expr);
    }
}
