package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.constants;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractExpression;
import org.apache.jena.sparql.expr.NodeValue;

public abstract class Constant<E extends NodeValue> extends AbstractExpression<E> {
    protected Constant(E expr) {
        super(expr);
    }
}
