package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.AbstractAggregator;
import org.apache.jena.sparql.ARQNotImplemented;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggNull;

public class Null extends AbstractAggregator<AggNull> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr     the source Jena aggregator
     * @param variable the variable associated to the aggregator
     */
    public Null(AggNull aggr, Var variable) {
        super(aggr, variable, false);
    }

    @Override
    public String toSQLString() {
        throw new ARQNotImplemented("Null aggregation is not implemented");
    }
}
