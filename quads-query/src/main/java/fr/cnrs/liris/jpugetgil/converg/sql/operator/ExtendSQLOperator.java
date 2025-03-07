package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.Expression;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import org.apache.jena.sparql.algebra.op.OpExtend;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;

import java.util.Map;
import java.util.stream.Collectors;

public class ExtendSQLOperator extends SQLOperator {

    OpExtend opExtend;

    SQLQuery query;

    public ExtendSQLOperator(OpExtend opExtend, SQLQuery query) {
        this.opExtend = opExtend;
        this.query = query;
    }

    /**
     * @return the SQL query of the quad pattern
     */
    @Override
    public SQLQuery buildSQLQuery() {
        String select = "SELECT " + buildSelect() + "\n";
        String from = "FROM " + buildFrom() + "\n";
        String where = buildWhere();

        String query = !where.isEmpty() ? select + from + "WHERE " + where : select + from;

        return new SQLQuery(
                query,
                this.query.getContext()
        );
    }

    /**
     * @return the select part of extend operator
     */
    @Override
    protected String buildSelect() {
        VarExprList varExprList = opExtend.getVarExprList();
        return getSelectExtend(varExprList.getExprs());
    }

    /**
     * @return the from part of extend operator
     */
    @Override
    protected String buildFrom() {
        return " (" + this.query.getSql() + ") ext \n";
    }

    /**
     * @return the where part of extend operator
     */
    @Override
    protected String buildWhere() {
        return "";
    }


    /**
     * @return the select part of extend operator
     */
    private String getSelectExtend(Map<Var, Expr> exprs) {
        return exprs.keySet().stream()
                .map(variable -> Expression.fromJenaExpr(exprs.get(variable)).toSQLStringAgg() +
                        " AS " + variable.getVarName().replace(".", "agg")
                ).collect(Collectors.joining(", "));
    }
}
