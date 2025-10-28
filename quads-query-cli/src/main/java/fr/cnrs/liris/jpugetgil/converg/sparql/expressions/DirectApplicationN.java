package fr.cnrs.liris.jpugetgil.converg.sparql.expressions;

import org.apache.jena.sparql.expr.ExprFunctionN;

public class DirectApplicationN extends AbstractDirectApplication<ExprFunctionN> {
    public DirectApplicationN(ExprFunctionN expr, boolean requiresValue, String sqlFunction) {
        super(expr, requiresValue, sqlFunction);
    }
}
