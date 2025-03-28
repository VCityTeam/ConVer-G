package fr.cnrs.liris.jpugetgil.converg.sparql.expressions.op;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.AbstractExpression;
import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.Expression;
import fr.cnrs.liris.jpugetgil.converg.sparql.transformer.FilterConfiguration;
import org.apache.jena.sparql.expr.ExprFunction;

public abstract class AbstractFunction<E extends ExprFunction> extends AbstractExpression<E> {
    protected final Expression[] args;
    protected final boolean requiresValue;

    protected AbstractFunction(E expr, boolean requiresValue) {
        super(expr);
        args = expr.getArgs().stream().map(Expression::fromJenaExpr).toArray(Expression[]::new);
        this.requiresValue = requiresValue;
    }

    @Override
    public boolean requiresValue() {
        return requiresValue;
    }

    @Override
    public void updateFilterConfiguration(FilterConfiguration configuration, boolean unusedRequiresValue) {
        for (Expression e : args) {
            e.updateFilterConfiguration(configuration, this.requiresValue);
        }
    }
}
