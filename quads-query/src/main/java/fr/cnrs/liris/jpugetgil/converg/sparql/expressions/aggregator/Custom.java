package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.AbstractAggregator;
import org.apache.jena.sparql.ARQNotImplemented;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggCustom;

public class Custom extends AbstractAggregator<AggCustom> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr     the source Jena aggregator
     * @param variable the variable associated to the aggregator
     */
    public Custom(AggCustom aggr, Var variable) {
        super(aggr, variable);
    }

    @Override
    public String toSQLString() {
        throw new ARQNotImplemented("This custom aggregator is not supported in SQL");
    }
}
