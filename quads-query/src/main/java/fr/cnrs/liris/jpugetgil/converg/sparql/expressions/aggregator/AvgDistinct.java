package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.AbstractAggregator;
import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.Expression;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggAvgDistinct;

import java.util.List;
import java.util.stream.Collectors;

public class AvgDistinct extends AbstractAggregator<AggAvgDistinct> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr the source Jena aggregator
     * @param variable the variable associated to the aggregator
     */
    public AvgDistinct(AggAvgDistinct aggr, Var variable) {
        super(aggr, variable, true);
    }

    @Override
    public String toSQLString() {
        List<Expression> expressions = getExpressionList();

        String joinedExpression = expressions.stream()
                .map(expression -> expression.toSQLString() + "::float")
                .collect(Collectors.joining(""));

        String varName = "agg" + getVariable().getVarName().replace(".", "");
        return "AVG(DISTINCT " + joinedExpression + ") AS " + varName;
    }
}
