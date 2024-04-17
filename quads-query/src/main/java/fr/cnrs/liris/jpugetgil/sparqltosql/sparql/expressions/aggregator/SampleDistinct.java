package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractAggregator;
import org.apache.jena.sparql.expr.aggregate.AggSampleDistinct;

public class SampleDistinct extends AbstractAggregator<AggSampleDistinct> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr the source Jena aggregator
     */
    public SampleDistinct(AggSampleDistinct aggr) {
        super(aggr);
    }

    @Override
    public String toSQLString() {
        throw new IllegalStateException("Not implemented yet");
    }
}
