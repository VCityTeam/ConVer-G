package fr.cnrs.liris.jpugetgil.converg.sparql.expressions;

import fr.cnrs.liris.jpugetgil.converg.sparql.transformer.FilterConfiguration;
import org.apache.jena.sparql.ARQNotImplemented;
import org.apache.jena.sparql.expr.E_Exists;

public class Exists extends AbstractExpression<E_Exists> {
    public Exists(E_Exists exists) {
        super(exists);
    }

    @Override
    public void updateFilterConfiguration(FilterConfiguration configuration, boolean requiresValue) {
        throw new ARQNotImplemented("Exists not implemented"); // TODO: Implement
    }
}
