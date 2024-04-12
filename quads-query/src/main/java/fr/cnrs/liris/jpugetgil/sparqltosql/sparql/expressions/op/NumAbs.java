package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractExpression;
import org.apache.jena.sparql.expr.E_NumAbs;

public class NumAbs extends AbstractExpression<E_NumAbs> {
    public NumAbs(E_NumAbs expr) {
        super(expr);
    }
}
