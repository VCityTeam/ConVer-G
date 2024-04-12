package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractExpression;
import org.apache.jena.sparql.expr.E_Equals;

public class Equals extends AbstractExpression<E_Equals> {
    public Equals(E_Equals expr) {
        super(expr);
    }
}
