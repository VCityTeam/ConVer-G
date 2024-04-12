package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.AbstractExpression;
import org.apache.jena.sparql.expr.E_LogicalNot;

public class LogicalNot extends AbstractExpression<E_LogicalNot> {
    public LogicalNot(E_LogicalNot expr) {
        super(expr);
    }
}
