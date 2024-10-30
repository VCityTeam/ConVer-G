package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVariable;
import org.apache.jena.graph.Node_Variable;
import org.apache.jena.sparql.ARQException;
import org.apache.jena.sparql.algebra.op.OpGraph;

import java.util.stream.Collectors;

public class StSGraphOperator extends StSOperator {
    private final OpGraph op;

    private final SQLContext context;

    private final SQLQuery sqlQuery;

    public StSGraphOperator(OpGraph op, SQLQuery sqlQuery) {
        this.op = op;
        this.context = sqlQuery.getContext();
        this.sqlQuery = sqlQuery;
        this.sqlVariables = sqlQuery.getContext().sqlVariables();
    }

    @Override
    public SQLQuery buildSQLQuery() {
        String query;
        if (this.sqlQuery.getSql() == null) {
            if (op.getNode() instanceof Node_Variable) {
                sqlVariables.add(new SQLVariable(SQLVarType.VERSIONED_NAMED_GRAPH, op.getNode().getName(), true));
            }
            query = getStringQueryTable();
        } else {
            query = getStringQueryWithBGP();
        }

        SQLContext newContext = context
                .setSQLVariables(sqlVariables);

        return new SQLQuery(
                query,
                newContext
        );
    }

    private String getStringQueryTable() {
        String select = "SELECT " + context.sqlVariables().stream()
                .map(sqlVariable -> {
                    if (sqlVariable.getSqlVarType() == SQLVarType.VERSIONED_NAMED_GRAPH) {
                        return "id_versioned_named_graph as vng$" + sqlQuery.getContext().graph().getName();
                    } else {
                        throw new ARQException("Unsupported SQLVarType: " + sqlVariable.getSqlVarType());
                    }
                }).collect(Collectors.joining(", "));

        String from = " FROM versioned_named_graph";

        return select + from;
    }

    private String getStringQueryWithBGP() {
        String select = "SELECT " + context.sqlVariables().stream()
                .map(sqlVariable -> {
                    if (sqlVariable.getSqlVarType() == SQLVarType.BIT_STRING) {
                        return "graph.bs$" + sqlQuery.getContext().graph().getName();
                    } else if (sqlVariable.getSqlVarType() == SQLVarType.GRAPH_NAME) {
                        return "graph.ng$" + sqlQuery.getContext().graph().getName();
                    } else {
                        return "graph.v$" + sqlVariable.getSqlVarName();
                    }
                }).collect(Collectors.joining(", "));

        String from = " FROM (" + sqlQuery.getSql() + ") graph";

        return select + from;
    }
}
