package fr.cnrs.liris.jpugetgil.sparqltosql.sql.context;

import fr.cnrs.liris.jpugetgil.sparqltosql.SQLContext;
import fr.cnrs.liris.jpugetgil.sparqltosql.SQLQuery;
import fr.cnrs.liris.jpugetgil.sparqltosql.SQLVarType;
import fr.cnrs.liris.jpugetgil.sparqltosql.SQLVariable;
import org.apache.jena.graph.Node_Variable;
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
                sqlVariables.add(new SQLVariable(SQLVarType.VERSIONED_NAMED_GRAPH, op.getNode().getName()));
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
                        throw new UnsupportedOperationException("Unsupported SQLVarType: " + sqlVariable.getSqlVarType());
                    }
                }).collect(Collectors.joining(", "));

        String from = " FROM versioned_named_graph";

        return select + from;
    }

    private String getStringQueryWithBGP() {
        String select = "SELECT " + context.sqlVariables().stream()
                .map(sqlVariable -> {
                    if (sqlVariable.getSqlVarType() == SQLVarType.BIT_STRING) {
                        return sqlQuery.getContext().tableName() + sqlQuery.getContext().tableIndex() + ".bs$" +
                                sqlQuery.getContext().graph().getName();
                    } else if (sqlVariable.getSqlVarType() == SQLVarType.GRAPH_NAME) {
                        return sqlQuery.getContext().tableName() + sqlQuery.getContext().tableIndex() + ".ng$" +
                                sqlQuery.getContext().graph().getName();
                    } else {
                        return sqlQuery.getContext().tableName() + sqlQuery.getContext().tableIndex() + ".v$" + sqlVariable.getSqlVarName();
                    }
                }).collect(Collectors.joining(", "));

        String from = " FROM (" + sqlQuery.getSql() + ") " + sqlQuery.getContext().tableName() + sqlQuery.getContext().tableIndex();

        return select + from;
    }
}
