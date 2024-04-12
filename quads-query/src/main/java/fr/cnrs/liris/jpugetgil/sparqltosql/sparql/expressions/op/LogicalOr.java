package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractExpression;
import org.apache.jena.sparql.expr.E_LogicalOr;

public class LogicalOr extends AbstractExpression<E_LogicalOr> {
    public LogicalOr(E_LogicalOr expr) {
        super(expr);
    }
}
