package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractExpression;
import org.apache.jena.sparql.expr.E_LessThanOrEqual;

public class LessThanOrEqual extends AbstractExpression<E_LessThanOrEqual> {
    public LessThanOrEqual(E_LessThanOrEqual expr) {
        super(expr);
    }
}
