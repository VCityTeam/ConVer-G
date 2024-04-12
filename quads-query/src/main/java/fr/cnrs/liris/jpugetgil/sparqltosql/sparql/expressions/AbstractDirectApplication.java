package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import org.apache.jena.sparql.expr.ExprFunction;

import java.util.List;

public class AbstractDirectApplication<E extends ExprFunction> extends AbstractExpression<E> {
    protected String sqlFunction;
    protected List<Expression> args;

    public AbstractDirectApplication(E expr, String sqlFunction) {
        super(expr);
        args = expr.getArgs().stream().map(Expression::fromJenaExpr).toList();
        this.sqlFunction = sqlFunction;
    }
}
