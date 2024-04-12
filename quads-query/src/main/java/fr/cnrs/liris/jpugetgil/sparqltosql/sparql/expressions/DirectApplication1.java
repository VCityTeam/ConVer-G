package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import org.apache.jena.sparql.expr.ExprFunction1;

public class DirectApplication1 extends AbstractDirectApplication<ExprFunction1> {
    public DirectApplication1(ExprFunction1 expr, boolean requiresValue, String sqlFunction) {
        super(expr, requiresValue, sqlFunction);
    }
}
