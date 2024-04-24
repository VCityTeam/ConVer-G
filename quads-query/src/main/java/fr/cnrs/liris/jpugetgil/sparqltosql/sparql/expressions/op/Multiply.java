package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.op;

import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVariable;
import org.apache.jena.sparql.expr.E_Multiply;

import java.util.List;

public class Multiply extends AbstractFunction<E_Multiply> {
    public Multiply(E_Multiply expr) {
        super(expr, true);
    }

    @Override
    public String toSQLString(List<SQLVariable> variables) {
        return this.args[0].toSQLString(variables) + getJenaExpr().getOpName() + this.args[1].toSQLString(variables);
    }

    @Override
    public String toSQLString() {
        return this.args[0].toSQLString() + getJenaExpr().getOpName() + this.args[1].toSQLString();
    }
}
