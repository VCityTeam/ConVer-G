package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.aggregator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractAggregator;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.Expression;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVariable;
import org.apache.jena.sparql.expr.aggregate.AggGroupConcat;

import java.util.List;
import java.util.stream.Collectors;

public class GroupConcat extends AbstractAggregator<AggGroupConcat> {
    /**
     * Build an aggregator from a Jena aggregator.
     *
     * @param aggr the source Jena aggregator
     */
    public GroupConcat(AggGroupConcat aggr) {
        super(aggr);
    }

    @Override
    public String toSQLString(List<SQLVariable> sqlVariables) {
        List<Expression> expressions = this.getAggregator().getExprList().getList().stream()
                .map(Expression::fromJenaExpr)
                .toList();

        String joinedExpression = expressions.stream()
                .map(expression -> expression.toSQLString(sqlVariables))
                .collect(Collectors.joining(", "));

        expressions.forEach(expression -> sqlVariables.removeIf(
                sqlVariable -> sqlVariable.getSqlVarName().equals(expression.getJenaExpr().getVarName()))
        );

        sqlVariables.add(new SQLVariable(SQLVarType.AGGREGATED, joinedExpression));
        return "GROUP_CONCAT('" + joinedExpression + "' SEPARATOR '" +
                this.getAggregator().getSeparator() + "') AS " + joinedExpression;
    }
}
