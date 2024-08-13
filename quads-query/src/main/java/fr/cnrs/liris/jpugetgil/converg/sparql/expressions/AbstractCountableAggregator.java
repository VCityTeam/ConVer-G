package fr.cnrs.liris.jpugetgil.converg.sparql.expressions;

import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.Aggregator;

public abstract class AbstractCountableAggregator<E extends Aggregator> extends AbstractAggregator<E> {

    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr the source Jena aggregator
     */
    protected AbstractCountableAggregator(E aggr, Var variable) {
        super(aggr, variable);
    }
}
