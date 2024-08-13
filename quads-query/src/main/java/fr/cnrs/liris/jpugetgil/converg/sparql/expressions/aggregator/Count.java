package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.AbstractCountableAggregator;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggCount;

public class Count extends AbstractCountableAggregator<AggCount> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr     the source Jena aggregator
     * @param variable the variable associated to the aggregator
     */
    public Count(AggCount aggr, Var variable) {
        super(aggr, variable);
    }

    @Override
    public String toSQLString() {
        String varName = "agg" + getVariable().getVarName().replace(".", "");
        return this.getAggregator().getName() + "(*) AS " + varName;
    }
}
