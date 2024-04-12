package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractExpression;
import org.apache.jena.sparql.expr.E_Str;

public class Str extends AbstractExpression<E_Str> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public Str(E_Str expr) {
        super(expr);
    }
}
