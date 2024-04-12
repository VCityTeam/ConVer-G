package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractExpression;
import org.apache.jena.sparql.expr.E_Add;

public class Add extends AbstractExpression<E_Add> {
    public Add(E_Add expr) {
        super(expr);
    }
}
