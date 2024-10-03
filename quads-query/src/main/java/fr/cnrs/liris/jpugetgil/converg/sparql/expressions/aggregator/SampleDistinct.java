package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.AbstractAggregator;
import org.apache.jena.sparql.ARQNotImplemented;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggSampleDistinct;

public class SampleDistinct extends AbstractAggregator<AggSampleDistinct> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr     the source Jena aggregator
     * @param variable the variable associated to the aggregator
     */
    public SampleDistinct(AggSampleDistinct aggr, Var variable) {
        super(aggr, variable, true);
    }

    @Override
    public String toSQLString() {
        throw new ARQNotImplemented("Sample distinct aggregation is not implemented");
    }
}
