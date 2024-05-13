package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.Aggregator;

public abstract class AbstractAggregator<E extends Aggregator> {
    private final E aggr;
    private final Var variable;

    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr the source Jena aggregator
     */
    protected AbstractAggregator(E aggr, Var variable) {
        this.aggr = aggr;
        this.variable = variable;
    }

    public E getAggregator() {
        return aggr;
    }

    public Var getVariable() {
        return variable;
    }

    public abstract String toSQLString();
}
