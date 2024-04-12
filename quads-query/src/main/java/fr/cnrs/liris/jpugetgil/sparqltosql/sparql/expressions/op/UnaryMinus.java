package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractExpression;
import org.apache.jena.sparql.expr.E_UnaryMinus;

public class UnaryMinus extends AbstractExpression<E_UnaryMinus> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public UnaryMinus(E_UnaryMinus expr) {
        super(expr);
    }
}
