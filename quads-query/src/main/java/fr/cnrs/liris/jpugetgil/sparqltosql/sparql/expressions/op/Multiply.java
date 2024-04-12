package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractExpression;
import org.apache.jena.sparql.expr.E_Multiply;

public class Multiply extends AbstractExpression<E_Multiply> {
    public Multiply(E_Multiply expr) {
        super(expr);
    }
}
