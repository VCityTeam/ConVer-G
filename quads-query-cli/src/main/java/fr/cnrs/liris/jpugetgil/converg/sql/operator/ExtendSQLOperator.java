package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLPositionType;
import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.Expression;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVariable;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.op.OpExtend;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
     * @return the SQL query of extend operator
     */
    @Override
    public SQLQuery buildSQLQuery() {
        addExtendedVariablesToContext();

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
        Map<Var, Expr> exprs = opExtend.getVarExprList().getExprs();

        return "*, " + exprs.keySet().stream()
                .map(variable -> {
                    String aggName = variable.getVarName().replace(".", "agg");
                    String valueExpr = Expression.fromJenaExpr(exprs.get(variable)).toSQLStringAgg();
                    // Project a native numeric sibling (num$) so a downstream numeric
                    // filter on this computed variable can use it like any stored
                    // literal. try_cast_numeric keeps non-numeric binds (e.g. CONCAT)
                    // safe by yielding NULL instead of failing the cast.
                    return valueExpr + " AS " + new SQLVariable(SQLVarType.VALUE, aggName).getSelect()
                            + ", try_cast_numeric((" + valueExpr + ")::text) AS num$" + aggName;
                })
                .collect(Collectors.joining(", "));
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

    private void addExtendedVariablesToContext() {
        SQLContext context = this.query.getContext();
        Map<Node, List<SPARQLOccurrence>> sparqlVarOccurrences = new HashMap<>(context.sparqlVarOccurrences());

        this.opExtend.getVarExprList().getExprs().forEach((var, expr) ->
                sparqlVarOccurrences.put(var, Collections.singletonList(
                        new SPARQLOccurrence(SPARQLPositionType.AGGREGATED, null, null,
                                new SQLVariable(SQLVarType.VALUE, var.getVarName().replace(".", "agg"))
                        ))));

        this.query.setContext(context.copyWithNewVarOccurrences(sparqlVarOccurrences));
    }
}
