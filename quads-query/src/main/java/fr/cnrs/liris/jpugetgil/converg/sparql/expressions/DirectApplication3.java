package fr.cnrs.liris.jpugetgil.converg.sparql.expressions;

import org.apache.jena.sparql.expr.ExprFunction3;

public class DirectApplication3 extends AbstractDirectApplication<ExprFunction3> {
    public DirectApplication3(ExprFunction3 expr, boolean requiresValue, String sqlFunction) {
        super(expr, requiresValue, sqlFunction);
    }
}
