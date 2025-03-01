package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FlattenSQLOperator extends SQLOperator {
    private final SQLQuery sqlQuery;

    public FlattenSQLOperator(SQLQuery sqlQuery) {
        this.sqlQuery = sqlQuery;
        this.sqlVariables = sqlQuery.getContext().sqlVariables();
    }

    @Override
    public SQLQuery buildSQLQuery() {
        String select = buildSelect();
        String join = buildFrom();
        List<SQLVariable> sqlVariables = new ArrayList<>();

        for (SQLVariable sqlVariable : this.sqlVariables) {
            if (sqlVariable.getSqlVarType() == SQLVarType.ID) {
                sqlVariables.add(sqlVariable);
            } else if (sqlVariable.getSqlVarType() == SQLVarType.CONDENSED) {
                sqlVariables.add(new SQLVariable(SQLVarType.ID, sqlVariable.getSqlVarName()));
            }
        }
        SQLContext context = this.sqlQuery.getContext().setSQLVariables(sqlVariables);

        return new SQLQuery(
                "SELECT " + select + " FROM (" + sqlQuery.getSql() + ") flatten_table " + join, context
        );
    }

    @Override
    protected String buildSelect() {
        return this.sqlVariables.stream()
                .map(sqlVariable -> {
                    if (sqlVariable.getSqlVarType() == SQLVarType.CONDENSED) {
                        return "vng.id_versioned_named_graph AS v$" + sqlVariable.getSqlVarName();
                    } else {
                        return "flatten_table.v$" + sqlVariable.getSqlVarName();
                    }
                })
                .collect(Collectors.joining(", "));
    }

    /**
     * Get the join part of the query (flattening the graph variables)
     *
     * @return the flattened variables
     */
    @Override
    protected String buildFrom() {
        return this.sqlQuery.getContext().sqlVariables().stream()
                .filter(sqlVariable -> sqlVariable.getSqlVarType() == SQLVarType.CONDENSED)
                .map(sqlVariable -> " JOIN versioned_named_graph vng ON flatten_table.ng$" +
                        sqlVariable.getSqlVarName() + " = vng.id_named_graph AND get_bit(flatten_table.bs$" +
                        sqlVariable.getSqlVarName() + ", vng.index_version - 1) = 1 \n")
                .collect(Collectors.joining(" \n"));
    }

    /**
     * @return 
     */
    @Override
    protected String buildWhere() {
        return "";
    }
}
