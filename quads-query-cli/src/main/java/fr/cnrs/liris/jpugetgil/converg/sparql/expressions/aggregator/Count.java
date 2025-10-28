package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.AbstractAggregator;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggCount;

public class Count extends AbstractAggregator<AggCount> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr     the source Jena aggregator
     * @param variable the variable associated to the aggregator
     */
    public Count(AggCount aggr, Var variable) {
        super(aggr, variable, false);
    }

    @Override
    public String toSQLString() {
        String varName = "agg" + getVariable().getVarName().replace(".", "");
        return this.getAggregator().getName() + "(*) AS " + varName;
    }

    @Override
    public String toSQLString(OpGroup opGroup, String alias) {
        String varName = "agg" + getVariable().getVarName().replace(".", "");
        return "SUM(bit_count(bs$" + alias + ")) AS " + varName;
    }
}
