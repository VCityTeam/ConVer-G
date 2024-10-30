package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.Expression;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVariable;
import org.apache.jena.sparql.algebra.op.OpExtend;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StSExtendOperator extends StSOperator {
    private final SQLQuery sqlQuery;

    private final OpExtend op;

    public StSExtendOperator(OpExtend op, SQLQuery sqlQuery) {
        this.sqlQuery = sqlQuery;
        this.op = op;
        this.sqlVariables = sqlQuery.getContext().sqlVariables();
    }

    @Override
    public SQLQuery buildSQLQuery() {
        VarExprList varExprList = op.getVarExprList();

        String select = "SELECT *, " + getSelectExtend(varExprList.getExprs());
        String from = " FROM (" + this.sqlQuery.getSql() + ") ext \n";

        List<SQLVariable> newSQLVariables = new ArrayList<>(this.sqlVariables);
        varExprList.getExprs().keySet()
                .forEach(variable -> newSQLVariables.add(new SQLVariable(SQLVarType.AGGREGATED, variable.getVarName(), false)));
        SQLContext newSQLContext = sqlQuery.getContext()
                .setSQLVariables(newSQLVariables);

        return new SQLQuery(
                select + from,
                newSQLContext
        );
    }

    private String getSelectExtend(Map<Var, Expr> exprs) {
        return exprs.keySet().stream()
                .map(variable -> Expression.fromJenaExpr(exprs.get(variable)).toSQLStringAgg() +
                        " AS " + variable.getVarName().replace(".", "agg")
                ).collect(Collectors.joining(", "));
    }
}
