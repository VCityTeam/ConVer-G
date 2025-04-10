package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.AbstractAggregator;
import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.Expression;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggCountVar;

import java.util.List;
import java.util.stream.Collectors;

public class CountVar extends AbstractAggregator<AggCountVar> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr     the source Jena aggregator
     * @param variable the variable associated to the aggregator
     */
    public CountVar(AggCountVar aggr, Var variable) {
        super(aggr, variable, false);
    }

    @Override
    public String toSQLString() {
        List<Expression> expressions = getExpressionList();

        String joinedExpression = expressions.stream()
                .map(Expression::toSQLString)
                .collect(Collectors.joining(""));

        String varName = "agg" + getVariable().getVarName().replace(".", "");
        return this.getAggregator().getName() + "(" + joinedExpression + ") AS " + varName;
    }

    @Override
    public String toSQLString(OpGroup opGroup, String alias) {
        String varName = "agg" + getVariable().getVarName().replace(".", "");
        return "SUM(bit_count(bs$" + alias + ")) AS " + varName;
    }
}
