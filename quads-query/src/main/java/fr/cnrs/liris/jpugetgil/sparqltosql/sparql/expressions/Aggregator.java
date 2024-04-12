package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

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
}
