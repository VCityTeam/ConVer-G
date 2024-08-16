package fr.cnrs.liris.jpugetgil.converg.sparql.expressions;

import fr.cnrs.liris.jpugetgil.converg.sparql.transformer.FilterConfiguration;
import org.apache.jena.sparql.ARQNotImplemented;
import org.apache.jena.sparql.expr.E_NotExists;

public class NotExists extends AbstractExpression<E_NotExists> {
    public NotExists(E_NotExists expr) {
        super(expr);
    }

    @Override
    public void updateFilterConfiguration(FilterConfiguration configuration, boolean requiresValue) {
        throw new ARQNotImplemented("NotExists aggregation is not implemented");
    }
}
