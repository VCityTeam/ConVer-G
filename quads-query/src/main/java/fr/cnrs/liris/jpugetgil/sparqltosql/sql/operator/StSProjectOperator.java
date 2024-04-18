package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVariable;
import org.apache.jena.sparql.algebra.op.OpProject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StSProjectOperator extends StSOperator {
    private final OpProject op;

    private final SQLQuery sqlQuery;

    public StSProjectOperator(OpProject op, SQLQuery sqlQuery) {
        this.op = op;
        this.sqlQuery = sqlQuery;
        this.sqlVariables = sqlQuery.getContext().sqlVariables();
    }

    @Override
    public SQLQuery buildSQLQuery() {
        List<SQLVariable> sqlVariables = new ArrayList<>();
        for (SQLVariable sqlVariable : this.sqlVariables) {
            if (sqlVariable.getSqlVarType() == SQLVarType.AGGREGATED ||
                    op.getVars().stream()
                            .anyMatch(variable -> variable.getName().equals(sqlVariable.getSqlVarName()))) {
                sqlVariables.add(sqlVariable);
            }
        }

        SQLContext sqlContext = new SQLContext(
                sqlQuery.getContext().graph(),
                sqlQuery.getContext().sparqlVarOccurrences(),
                sqlVariables
        );

        SQLQuery newSQLQuery = new SQLQuery(sqlQuery.getSql(), sqlContext);

        return new SQLQuery(
                getSqlProjectionsQuery(newSQLQuery) + " FROM (" + sqlQuery.getSql() + ") project_table",
                sqlContext
        );
    }

    /**
     * Get the SELECT clause of the SQL query with the resource or literal table
     *
     * @param sqlQuery the SQL query
     * @return the SELECT projections of the SQL query
     */
    private static String getSqlProjectionsQuery(SQLQuery sqlQuery) {
        return "SELECT " + sqlQuery.getContext().sqlVariables().stream()
                .map(sqlVariable -> switch (sqlVariable.getSqlVarType()) {
                    case DATA:
                        yield "project_table.v$" + sqlVariable.getSqlVarName();
                    case BIT_STRING:
                        yield "project_table.bs$" + sqlVariable.getSqlVarName();
                    case GRAPH_NAME:
                        yield "project_table.ng$" + sqlVariable.getSqlVarName();
                    case VERSIONED_NAMED_GRAPH:
                        yield "project_table.vng$" + sqlVariable.getSqlVarName();
                    case AGGREGATED:
                        yield "project_table." + sqlVariable.getSqlVarName();
                })
                .collect(Collectors.joining(", "));
    }
}
