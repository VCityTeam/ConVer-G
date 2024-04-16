package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.SPARQLPositionType;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLQuery;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StSGroupOperator extends StSOperator {
    private final OpGroup op;

    private final SQLQuery sqlQuery;

    public StSGroupOperator(OpGroup op, SQLQuery sqlQuery) {
        this.op = op;
        this.sqlQuery = sqlQuery;
        this.sqlVariables = sqlQuery.getContext().sqlVariables();
    }

    @Override
    public SQLQuery buildSQLQuery() {
        // TODO : GROUP BY
        VarExprList exprList = op.getGroupVars();
        List<Var> vars = exprList.getVars();
        Map<Var, Expr> exprVar = exprList.getExprs();
        String groupBy = vars.stream()
                .map(variable -> {
                    if (sqlQuery.getContext().sparqlVarOccurrences().get(variable).stream()
                            .anyMatch(sparqlOccurrence -> sparqlOccurrence.getType() == SPARQLPositionType.GRAPH_NAME)) {
                        return "group_table.ng$" + variable.getName();
                    } else {
                        return "group_table.v$" + variable.getName();
                    }
                })
                .collect(Collectors.joining(", "));

        // FIXME : projections
        return new SQLQuery(
                "SELECT * FROM (" + sqlQuery.getSql() +
                        ") GROUP BY (" + groupBy + ")",
                sqlQuery.getContext()
        );
    }
}
