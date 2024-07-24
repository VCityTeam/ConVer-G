package fr.cnrs.liris.jpugetgil.converg.sparql.expressions;

import fr.cnrs.liris.jpugetgil.converg.sparql.transformer.FilterConfiguration;
import org.apache.jena.sparql.expr.ExprVar;

public class Var extends AbstractExpression<ExprVar> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public Var(ExprVar expr) {
        super(expr);
    }

    @Override
    public void updateFilterConfiguration(FilterConfiguration configuration, boolean requiresValue) {
        if (requiresValue) {
            configuration.addNeedLookupVariable(varName());
        }
    }

    /**
     * The variable's name
     *
     * @return the variable's name
     */
    public String varName() {
        return getJenaExpr().getVarName();
    }

    @Override
    public String toSQLString() {
        return "v$" + varName();
    }

    @Override
    public String toSQLStringAgg() {
        return varName().replace(".", "agg");
    }
}
