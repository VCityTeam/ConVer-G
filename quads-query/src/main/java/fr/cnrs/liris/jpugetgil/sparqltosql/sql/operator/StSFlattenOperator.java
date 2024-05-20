package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StSFlattenOperator extends StSOperator {
    private final SQLQuery sqlQuery;

    public StSFlattenOperator(SQLQuery sqlQuery) {
        this.sqlQuery = sqlQuery;
        this.sqlVariables = sqlQuery.getContext().sqlVariables();
    }

    @Override
    public SQLQuery buildSQLQuery() {
        String select = getSelect();
        String join = flattenVariables();
        List<SQLVariable> sqlVariables = new ArrayList<>();

        for (SQLVariable sqlVariable : this.sqlVariables) {
            if (sqlVariable.getSqlVarType() == SQLVarType.DATA) {
                sqlVariables.add(sqlVariable);
            } else if (sqlVariable.getSqlVarType() == SQLVarType.GRAPH_NAME) {
                sqlVariables.add(new SQLVariable(SQLVarType.DATA, sqlVariable.getSqlVarName()));
            }
        }
        SQLContext context = this.sqlQuery.getContext().setSQLVariables(sqlVariables);

        return new SQLQuery(
                "SELECT " + select + " FROM (" + sqlQuery.getSql() + ") flatten_table " + join, context
        );
    }

    private String getSelect() {
        return this.sqlVariables.stream()
                .map(sqlVariable -> {
                    if (sqlVariable.getSqlVarType() == SQLVarType.GRAPH_NAME) {
                        return "vng.id_versioned_named_graph AS v$" + sqlVariable.getSqlVarName();
                    } else if (sqlVariable.getSqlVarType() == SQLVarType.BIT_STRING) {
                        return null;
                    } else {
                        return "flatten_table.v$" + sqlVariable.getSqlVarName();
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
    }

    /**
     * Get the join part of the query (flattening the graph variables)
     *
     * @return the flattened variables
     */
    private String flattenVariables() {
        return this.sqlQuery.getContext().sqlVariables().stream()
                .filter(sqlVariable -> sqlVariable.getSqlVarType() == SQLVarType.GRAPH_NAME)
                .map(sqlVariable -> " JOIN versioned_named_graph vng ON flatten_table.ng$" +
                        sqlVariable.getSqlVarName() + " = vng.id_named_graph AND get_bit(flatten_table.bs$" +
                        sqlVariable.getSqlVarName() + ", vng.index_version) = 1 \n")
                .collect(Collectors.joining(" \n"));
    }
}
