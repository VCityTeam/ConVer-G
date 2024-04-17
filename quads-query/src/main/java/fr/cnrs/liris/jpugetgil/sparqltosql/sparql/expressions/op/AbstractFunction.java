package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractExpression;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.Expression;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.transformer.ExpressionVariableClassification;
import org.apache.jena.sparql.expr.ExprFunction;

public abstract class AbstractFunction<E extends ExprFunction> extends AbstractExpression<E> {
    protected final Expression[] args;
    protected final boolean requiresValue;

    protected AbstractFunction(E expr, boolean requiresValue) {
        super(expr);
        args = expr.getArgs().stream().map(Expression::fromJenaExpr).toArray(nargs -> new Expression[nargs]);
        this.requiresValue = requiresValue;
    }

    @Override
    public void updateFilterConfiguration(ExpressionVariableClassification classification, boolean unusedRequiresValue) {
        for (Expression e : args) {
            e.updateFilterConfiguration(classification, this.requiresValue);
        }
    }
}
