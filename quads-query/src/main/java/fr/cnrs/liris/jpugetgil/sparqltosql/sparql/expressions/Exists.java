package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import org.apache.jena.sparql.expr.E_Exists;

public class Exists extends AbstractExpression<E_Exists> {
    public Exists(E_Exists exists) {
        super(exists);
    }
}
