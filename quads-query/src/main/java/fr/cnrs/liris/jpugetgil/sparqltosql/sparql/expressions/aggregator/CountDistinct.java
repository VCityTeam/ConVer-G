package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractAggregator;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVariable;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggCountDistinct;

import java.util.List;

public class CountDistinct extends AbstractAggregator<AggCountDistinct> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr the source Jena aggregator
     * @param variable the variable associated to the aggregator
     */
    public CountDistinct(AggCountDistinct aggr, Var variable) {
        super(aggr, variable);
    }

    @Override
    public String toSQLString(List<SQLVariable> sqlVariables) {
        throw new IllegalStateException("Not implemented yet");
    }
}
