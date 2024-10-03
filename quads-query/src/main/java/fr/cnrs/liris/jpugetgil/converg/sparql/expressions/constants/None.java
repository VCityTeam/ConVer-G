package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.constants;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.AbstractExpression;
import fr.cnrs.liris.jpugetgil.converg.sparql.transformer.FilterConfiguration;
import org.apache.jena.sparql.expr.ExprNone;

public class None extends AbstractExpression<ExprNone> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public None(ExprNone expr) {
        super(expr);
    }

    @Override
    public void updateFilterConfiguration(FilterConfiguration configuration, boolean requiresValue) {
        // nothing to do for constants
    }
}
