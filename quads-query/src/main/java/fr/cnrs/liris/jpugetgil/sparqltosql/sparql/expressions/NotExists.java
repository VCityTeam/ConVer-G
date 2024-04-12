package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import org.apache.jena.sparql.expr.E_NotExists;

public class NotExists extends AbstractExpression<E_NotExists>{
    public NotExists(E_NotExists expr) {
        super(expr);
    }
}
