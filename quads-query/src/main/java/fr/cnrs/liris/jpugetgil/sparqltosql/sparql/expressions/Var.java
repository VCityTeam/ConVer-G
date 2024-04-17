package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.transformer.FilterConfiguration;
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
        // FIXME : Detect if the variable is a graph name (bs/ng) or not ?
        return "$" + varName();
    }
}
