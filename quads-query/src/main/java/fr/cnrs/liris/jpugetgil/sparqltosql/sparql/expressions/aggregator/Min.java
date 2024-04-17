package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractAggregator;
import org.apache.jena.sparql.expr.aggregate.AggMin;

public class Min extends AbstractAggregator<AggMin> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr the source Jena aggregator
     */
    public Min(AggMin aggr) {
        super(aggr);
    }

    @Override
    public String toSQLString() {
        throw new IllegalStateException("Not implemented yet");
    }
}
