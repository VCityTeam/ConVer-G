package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.constants;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractExpression;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.transformer.ExpressionVariableClassification;
import org.apache.jena.sparql.expr.NodeValue;

public abstract class Constant<E extends NodeValue> extends AbstractExpression<E> {
    protected Constant(E expr) {
        super(expr);
    }

    @Override
    public void updateFilterConfiguration(ExpressionVariableClassification classification, boolean requiresValue) {
        // nothing to do for constants
    }
}
