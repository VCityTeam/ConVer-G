package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.AbstractAggregator;
import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.Expression;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggGroupConcat;

import java.util.List;
import java.util.stream.Collectors;

public class GroupConcat extends AbstractAggregator<AggGroupConcat> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr     the source Jena aggregator
     * @param variable the variable associated to the aggregator
     */
    public GroupConcat(AggGroupConcat aggr, Var variable) {
        super(aggr, variable, true);
    }

    @Override
    public String toSQLString() {
        List<Expression> expressions = getExpressionList();

        String joinedExpression = expressions.stream()
                .map(Expression::toSQLString)
                .collect(Collectors.joining(""));

        String varName = "agg" + getVariable().getVarName().replace(".", "");
        if (this.getAggregator().getSeparator() == null) {
            return "STRING_AGG(" + joinedExpression + ", ' ') AS " + varName;
        } else {
            return "STRING_AGG(" + joinedExpression + ", '" + this.getAggregator().getSeparator() + "') AS " + varName;
        }
    }
}
