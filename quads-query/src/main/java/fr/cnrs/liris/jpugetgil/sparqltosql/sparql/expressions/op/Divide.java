package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractExpression;
import org.apache.jena.sparql.expr.E_Divide;

public class Divide extends AbstractExpression<E_Divide> {
    public Divide(E_Divide expr) {
        super(expr);
    }
}
