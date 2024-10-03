package fr.cnrs.liris.jpugetgil.converg.sparql.expressions;

import org.apache.jena.sparql.expr.ExprFunction2;

public class DirectApplication2 extends AbstractDirectApplication<ExprFunction2> {
    public DirectApplication2(ExprFunction2 expr, boolean requiresValue, String sqlFunction) {
        super(expr, requiresValue, sqlFunction);
    }
}
