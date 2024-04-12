package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op.AbstractFunction;
import org.apache.jena.sparql.expr.ExprFunction;

public abstract class AbstractDirectApplication<E extends ExprFunction> extends AbstractFunction<E> {
    protected String sqlFunction;

    protected AbstractDirectApplication(E expr, boolean requiresValue, String sqlFunction) {
        super(expr, requiresValue);
        this.sqlFunction = sqlFunction;
    }
}
