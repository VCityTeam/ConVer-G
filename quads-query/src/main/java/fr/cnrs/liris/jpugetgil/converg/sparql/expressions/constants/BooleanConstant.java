package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.constants;

import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;

public class BooleanConstant extends Constant<NodeValueBoolean> {
    public BooleanConstant(NodeValueBoolean expr) {
        super(expr);
    }
}
