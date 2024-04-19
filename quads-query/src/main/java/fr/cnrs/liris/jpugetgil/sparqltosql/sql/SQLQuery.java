package fr.cnrs.liris.jpugetgil.sparqltosql.sql;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;

import java.util.List;
import java.util.stream.Collectors;

public class SQLQuery {

    private String sql;
    private final List<SQLVariable> sqlVariables;

//    private SQLContext context;

    public SQLQuery(String sql /*, SQLContext context */, List<SQLVariable> sqlVariables) {
        this.sql = sql;
//        this.context = context;
        this.sqlVariables = sqlVariables;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public List<SQLVariable> getSqlVariables() {
        return sqlVariables;
    }

    //    public SQLContext getContext() {
//        return context;
//    }

//    public void setContext(SQLContext context) {
//        this.context = context;
//    }

    public SQLQuery finalizeQuery() {
        String select = "SELECT " + getSelectVariablesResourceOrLiteral();
        String from = " FROM (" + this.sql + ") indexes_table";
        String join = getJoinVariablesResourceOrLiteral();

        return new SQLQuery(
                select + from + join,
//                this.context
                sqlVariables
        );
    }

    /**
     * Gets the SELECT clause of the SQL query with the resource or literal table
     *
     * @return the SELECT clause of the SQL query
     */
    private String getSelectVariablesResourceOrLiteral() {
        return Streams.mapWithIndex(sqlVariables.stream()
                .filter(sqlVariable -> sqlVariable.getSqlVarType() != SQLVarType.BIT_STRING), (sqlVariable, index) -> (
                "rl" + index + ".name as name$" + sqlVariable.getSqlVarName() + ", rl" + index + ".type as type$" + sqlVariable.getSqlVarName()
        )).collect(Collectors.joining(", "));
    }

    /**
     * Gets the finalized query with the JOIN clause of the SQL query with the resource_or_literal/versioned_named_graph table
     *
     * @return the finalized SQL query
     */
    private String getJoinVariablesResourceOrLiteral() {
        return Streams.mapWithIndex(sqlVariables.stream()
                .filter(sqlVariable -> sqlVariable.getSqlVarType() != SQLVarType.BIT_STRING), (sqlVariable, index) ->
                switch (sqlVariable.getSqlVarType()) {
                    case DATA:
                        yield " JOIN resource_or_literal rl" + index + " ON indexes_table.v$" +
                                sqlVariable.getSqlVarName() +
                                " = rl" + index + ".id_resource_or_literal";
                    case GRAPH_NAME:
                        yield " JOIN versioned_named_graph vng" + index + " ON indexes_table.ng$" +
                                sqlVariable.getSqlVarName() + " = vng" + index +
                                ".id_named_graph AND get_bit(indexes_table.bs$" +
                                sqlVariable.getSqlVarName() + ", vng" + index + ".index_version) = 1 \n" +
                                " JOIN resource_or_literal rl" + index + " ON vng" + index + ".id_versioned_named_graph = rl" +
                                index + ".id_resource_or_literal";
                    case VERSIONED_NAMED_GRAPH:
                        yield " JOIN resource_or_literal rl" + index + " ON " +
                                "indexes_table.vng$" + sqlVariable.getSqlVarName() +
                                " = rl" + index + ".id_resource_or_literal";
                    default:
                        yield "";
                }).collect(Collectors.joining(" \n"));
    }
}
