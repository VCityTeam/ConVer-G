package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractAggregator;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.Expression;
import org.apache.jena.sparql.expr.aggregate.AggCountVar;

import java.util.stream.Collectors;

public class CountVar extends AbstractAggregator<AggCountVar> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr the source Jena aggregator
     */
    public CountVar(AggCountVar aggr) {
        super(aggr);
    }

    @Override
    public String toSQLString() {
        AggCountVar aggCountVar = this.getAggregator();
        String expression = aggCountVar.getExprList().getList().stream()
                .map(expr -> Expression.fromJenaExpr(expr).toSQLString())
                .collect(Collectors.joining(", "));
        throw new IllegalStateException("Not implemented yet");
//        return this.getAggregator().getName() + "(" + expression + ")";
    }
}
