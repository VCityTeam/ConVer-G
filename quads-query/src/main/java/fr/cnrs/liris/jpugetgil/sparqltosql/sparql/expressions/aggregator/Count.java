package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractAggregator;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVariable;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggCount;

import java.util.List;

public class Count extends AbstractAggregator<AggCount> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr the source Jena aggregator
     * @param var the variable associated to the aggregator
     */
    public Count(AggCount aggr, Var var) {
        super(aggr, var);
    }

    @Override
    public String toSQLString(List<SQLVariable> sqlVariables) {
        throw new IllegalStateException("Not implemented yet");
    }
}
