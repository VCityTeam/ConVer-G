package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVariable;
import org.apache.jena.sparql.expr.aggregate.Aggregator;

import java.util.List;

public abstract class AbstractAggregator<E extends Aggregator> {
    private E aggr;

    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr the source Jena aggregator
     */
    protected AbstractAggregator(E aggr) {
        this.aggr = aggr;
    }

    public E getAggregator() {
        return aggr;
    }

    public abstract String toSQLString(List<SQLVariable> sqlVariables);
}
