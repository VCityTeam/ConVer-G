package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.constants;

import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;

public class NodeConstant extends Constant<NodeValueNode> {
    public NodeConstant(NodeValueNode expr) {
        super(expr);
    }
}
