package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.constants;

import org.apache.jena.sparql.expr.nodevalue.NodeValueDouble;

public class DoubleConstant extends Constant<NodeValueDouble> {
    public DoubleConstant(NodeValueDouble expr) {
        super(expr);
    }
}