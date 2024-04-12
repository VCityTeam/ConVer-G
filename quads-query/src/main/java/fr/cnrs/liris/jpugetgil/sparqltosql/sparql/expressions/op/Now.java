package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractExpression;
import org.apache.jena.sparql.expr.E_Now;

public class Now extends AbstractExpression<E_Now> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public Now(E_Now expr) {
        super(expr);
    }
}
