package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractExpression;
import org.apache.jena.sparql.expr.E_Subtract;

public class Subtract extends AbstractExpression<E_Subtract> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public Subtract(E_Subtract expr) {
        super(expr);
    }
}
