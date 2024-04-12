package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractExpression;
import org.apache.jena.sparql.expr.E_NotEquals;

public class NotEquals extends AbstractExpression<E_NotEquals> {
    public NotEquals(E_NotEquals expr) {
        super(expr);
    }
}
