package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.constants;

import org.apache.jena.sparql.expr.nodevalue.NodeValueString;

public class StringConstant extends Constant<NodeValueString> {
    private final String value;

    public StringConstant(NodeValueString expr) {
        super(expr);
        value = expr.asString();
    }
}
