package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.AbstractAggregator;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggMedian;

public class Median extends AbstractAggregator<AggMedian> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr     the source Jena aggregator
     * @param variable the variable associated to the aggregator
     */
    public Median(AggMedian aggr, Var variable) {
        super(aggr, variable);
    }

    @Override
    public String toSQLString() {
        throw new IllegalStateException("Not implemented yet");
    }
}
