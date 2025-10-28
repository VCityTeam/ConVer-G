package fr.cnrs.liris.jpugetgil.converg.sparql.expressions;

import org.apache.jena.sparql.ARQNotImplemented;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.Aggregator;

import java.util.List;

public abstract class AbstractAggregator<E extends Aggregator> {
    private final E aggr;
    private final Var variable;
    private final boolean requiresValue;

    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr the source Jena aggregator
     */
    protected AbstractAggregator(E aggr, Var variable, boolean requiresValue) {
        this.aggr = aggr;
        this.variable = variable;
        this.requiresValue = requiresValue;
    }

    public E getAggregator() {
        return aggr;
    }

    public Var getVariable() {
        return variable;
    }

    public boolean getRequiresValue() {
        return this.requiresValue;
    }

    public abstract String toSQLString();

    protected String toSQLString(OpGroup opGroup, String alias) {
        throw new ARQNotImplemented("toSQLString not implemented for " + this.getClass().getSimpleName());
    }

    protected List<Expression> getExpressionList() {
        return this.getAggregator().getExprList().getList().stream()
                .map(Expression::fromJenaExpr)
                .toList();
    }
}
