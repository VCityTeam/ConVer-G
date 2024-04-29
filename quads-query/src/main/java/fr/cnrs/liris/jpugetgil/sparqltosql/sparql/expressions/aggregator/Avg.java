package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractAggregator;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.Expression;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggAvg;

import java.util.List;
import java.util.stream.Collectors;

public class Avg extends AbstractAggregator<AggAvg> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr     the source Jena aggregator
     * @param variable the variable associated to the aggregator
     */
    public Avg(AggAvg aggr, Var variable) {
        super(aggr, variable);
    }

    @Override
    public String toSQLString() {
        List<Expression> expressions = this.getAggregator().getExprList().getList().stream()
                .map(Expression::fromJenaExpr)
                .toList();

        String joinedExpression = expressions.stream()
                .map(Expression::toSQLString)
                .collect(Collectors.joining(""));

        String varName = "agg" + getVariable().getVarName().replace(".", "");
        return this.getAggregator().getName() + "(" + joinedExpression + ") AS " + varName;
    }
}
