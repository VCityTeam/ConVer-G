package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.AbstractAggregator;
import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.Expression;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggMode;

import java.util.List;
import java.util.stream.Collectors;

public class Mode extends AbstractAggregator<AggMode> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr     the source Jena aggregator
     * @param variable the variable associated to the aggregator
     */
    public Mode(AggMode aggr, Var variable) {
        super(aggr, variable);
    }

    @Override
    public String toSQLString() {
        List<Expression> expressions = getExpressionList();

        String joinedExpression = expressions.stream()
                .map(Expression::toSQLString)
                .collect(Collectors.joining(""));

        String varName = "agg" + getVariable().getVarName().replace(".", "");
        return this.getAggregator().getName() + "() WITHIN GROUP (ORDER BY " + joinedExpression + ") AS " + varName;
    }
}
