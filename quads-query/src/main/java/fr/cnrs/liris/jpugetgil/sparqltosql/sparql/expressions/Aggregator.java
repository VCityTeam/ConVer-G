package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.transformer.FilterConfiguration;
import org.apache.jena.sparql.expr.ExprAggregator;

public class Aggregator extends AbstractExpression<ExprAggregator> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public Aggregator(ExprAggregator expr) {
        super(expr);
    }

    @Override
    public void updateFilterConfiguration(FilterConfiguration configuration, boolean requiresValue) {
        throw new IllegalStateException("updateFilterConfiguration should not be called on an aggregate value");
    }
}
