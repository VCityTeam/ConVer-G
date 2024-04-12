package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractExpression;
import org.apache.jena.sparql.expr.E_LessThan;

public class LessThan extends AbstractExpression<E_LessThan> {
    public LessThan(E_LessThan expr) {
        super(expr);
    }
}
