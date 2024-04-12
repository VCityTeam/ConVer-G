package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractExpression;
import org.apache.jena.sparql.expr.E_GreaterThan;

public class GreaterThan extends AbstractExpression<E_GreaterThan> {
    public GreaterThan(E_GreaterThan expr) {
        super(expr);
    }
}
