package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractAggregator;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggModeDistinct;

public class ModeDistinct extends AbstractAggregator<AggModeDistinct> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr     the source Jena aggregator
     * @param variable the variable associated to the aggregator
     */
    public ModeDistinct(AggModeDistinct aggr, Var variable) {
        super(aggr, variable);
    }

    @Override
    public String toSQLString() {
        throw new IllegalStateException("Not implemented yet");
    }
}
