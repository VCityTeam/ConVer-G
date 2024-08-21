package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.AbstractAggregator;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggCountDistinct;

public class CountDistinct extends AbstractAggregator<AggCountDistinct> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr     the source Jena aggregator
     * @param variable the variable associated to the aggregator
     */
    public CountDistinct(AggCountDistinct aggr, Var variable) {
        super(aggr, variable, false);
    }

    @Override
    public String toSQLString() {
        String varName = "agg" + getVariable().getVarName().replace(".", "");
        return this.getAggregator().getName() + "(DISTINCT *) AS " + varName;
    }
}
