package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.AbstractAggregator;
import org.apache.jena.sparql.ARQNotImplemented;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggMedianDistinct;

public class MedianDistinct extends AbstractAggregator<AggMedianDistinct> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr     the source Jena aggregator
     * @param variable the variable associated to the aggregator
     */
    public MedianDistinct(AggMedianDistinct aggr, Var variable) {
        super(aggr, variable, true);
    }

    @Override
    public String toSQLString() {
        throw new ARQNotImplemented("Median distinct is not implemented");
    }
}
