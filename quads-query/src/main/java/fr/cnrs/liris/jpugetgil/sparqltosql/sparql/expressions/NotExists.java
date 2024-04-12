package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.transformer.FilterConfiguration;
import org.apache.jena.sparql.expr.E_NotExists;

public class NotExists extends AbstractExpression<E_NotExists> {
    public NotExists(E_NotExists expr) {
        super(expr);
    }

    @Override
    public void updateFilterConfiguration(FilterConfiguration configuration, boolean requiresValue) {
        throw new IllegalStateException("Not implemented yet"); // TODO: implement
    }
}
