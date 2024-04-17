package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLQuery;
import org.apache.jena.sparql.algebra.op.OpExtend;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;

import java.util.List;
import java.util.Map;

public class StSExtendOperator extends StSOperator {
    private final SQLQuery sqlQuery;

    private final OpExtend op;

    public StSExtendOperator(OpExtend op, SQLQuery sqlQuery) {
        this.sqlQuery = sqlQuery;
        this.op = op;
    }

    @Override
    public SQLQuery buildSQLQuery() {
        VarExprList varExprList = op.getVarExprList();
        List<Var> vars = varExprList.getVars();
        Map<Var, Expr> exprMap = varExprList.getExprs();

        throw new IllegalArgumentException("TODO: implement StSExtendOperator.buildSQLQuery()");
    }
}
