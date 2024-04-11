package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import org.apache.jena.sparql.expr.nodevalue.NodeValueString;

public class StringConstant extends Constant {
    private final String value;

    public StringConstant(NodeValueString expr) {
        super(expr);
        value = expr.asString();
    }
}
