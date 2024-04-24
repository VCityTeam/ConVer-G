package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVariable;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.Aggregator;

import java.util.List;

public abstract class AbstractAggregator<E extends Aggregator> {
    private E aggr;
    private Var var;

    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr the source Jena aggregator
     */
    protected AbstractAggregator(E aggr, Var var) {
        this.aggr = aggr;
        this.var = var;
    }

    public E getAggregator() {
        return aggr;
    }

    public Var getVar() {
        return var;
    }

    public abstract String toSQLString(List<SQLVariable> sqlVariables);
}
