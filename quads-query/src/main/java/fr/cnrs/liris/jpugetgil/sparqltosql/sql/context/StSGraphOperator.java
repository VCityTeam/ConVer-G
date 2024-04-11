package fr.cnrs.liris.jpugetgil.sparqltosql.sql.context;

import fr.cnrs.liris.jpugetgil.sparqltosql.SQLContext;
import fr.cnrs.liris.jpugetgil.sparqltosql.SQLQuery;
import org.apache.jena.graph.Node_Variable;

import java.util.stream.Collectors;

public class StSGraphOperator extends StSOperator {

    private final SQLContext context;

    private final SQLQuery sqlQuery;

    public StSGraphOperator(SQLQuery sqlQuery) {
        this.context = sqlQuery.getContext();
        this.sqlQuery = sqlQuery;
    }

    @Override
    public SQLQuery buildSQLQuery() {
        String select = "SELECT " + context.varOccurrences().keySet()
                .stream()
                .filter(node -> node instanceof Node_Variable)
                .filter(node -> !node.equals(sqlQuery.getContext().graph()))
                .map(node ->
                        sqlQuery.getContext().tableName() + sqlQuery.getContext().tableIndex() + ".v$" + node.getName()
                )
                .collect(Collectors.joining(", "));
        String from = " FROM (" + sqlQuery.getSql() + ") " + sqlQuery.getContext().tableName() + sqlQuery.getContext().tableIndex();

        if (sqlQuery.getContext().graph() instanceof Node_Variable) {
            select += ", " + sqlQuery.getContext().tableName() + sqlQuery.getContext().tableIndex() + ".ng$" +
                    sqlQuery.getContext().graph().getName() + ", "
                    + sqlQuery.getContext().tableName() + sqlQuery.getContext().tableIndex() + ".bs$" +
                    sqlQuery.getContext().graph().getName();
        }

        return new SQLQuery(
                select + from,
                sqlQuery.getContext()
        );
    }
}
