package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractExpression;
import org.apache.jena.sparql.expr.E_LogicalAnd;

public class LogicalAnd extends AbstractExpression<E_LogicalAnd> {
    public LogicalAnd(E_LogicalAnd expr) {
        super(expr);
    }
}
