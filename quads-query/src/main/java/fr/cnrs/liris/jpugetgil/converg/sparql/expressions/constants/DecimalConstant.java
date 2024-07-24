package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.constants;

import org.apache.jena.sparql.expr.nodevalue.NodeValueDecimal;

public class DecimalConstant extends Constant<NodeValueDecimal> {
    public DecimalConstant(NodeValueDecimal nodeValueDecimal) {
        super(nodeValueDecimal);
    }
}
