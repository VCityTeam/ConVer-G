package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractAggregator;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.Expression;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVariable;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggSumDistinct;

import java.util.List;
import java.util.stream.Collectors;

public class SumDistinct extends AbstractAggregator<AggSumDistinct> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr the source Jena aggregator
     * @param var the variable associated to the aggregator
     */
    public SumDistinct(AggSumDistinct aggr, Var var) {
        super(aggr, var);
    }

    @Override
    public String toSQLString(List<SQLVariable> sqlVariables) {
        List<Expression> expressions = this.getAggregator().getExprList().getList().stream()
                .map(Expression::fromJenaExpr)
                .toList();

        String joinedExpression = expressions.stream()
                .map(expression -> expression.toSQLString(sqlVariables))
                .collect(Collectors.joining(""));

        String varName = "agg" + getVar().getVarName().replace(".", "");
        return "SUM(DISTINCT " + joinedExpression + ") AS " + varName;
    }
}
