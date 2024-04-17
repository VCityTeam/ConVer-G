package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.transformer.ExpressionVariableClassification;
import org.apache.jena.sparql.expr.E_Exists;

public class Exists extends AbstractExpression<E_Exists> {
    public Exists(E_Exists exists) {
        super(exists);
    }

    @Override
    public void updateFilterConfiguration(ExpressionVariableClassification classification, boolean requiresValue) {
        throw new IllegalStateException("Not implemented yet"); // TODO: implement
    }
}
