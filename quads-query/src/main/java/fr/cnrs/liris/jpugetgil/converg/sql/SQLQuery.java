package fr.cnrs.liris.jpugetgil.converg.sql;

import com.google.common.collect.Streams;
import org.apache.jena.sparql.algebra.op.OpSlice;

import java.util.Objects;
import java.util.stream.Collectors;

public class SQLQuery {

    private String sql;

    private final SQLContext context;

    private OpSlice opSlice;

    public SQLQuery(String sql, SQLContext context) {
        this.sql = sql;
        this.context = context;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public SQLContext getContext() {
        return context;
    }

    public void setOpSlice(OpSlice opSlice) {
        this.opSlice = opSlice;
    }

    public SQLQuery finalizeQuery() {
        String select = "SELECT " + getSelectVariablesResourceOrLiteral();
        String from = " FROM (" + this.sql + ") indexes_table";
        String join = getJoinVariablesResourceOrLiteral();
        this.sql = select + from + join;

        if (this.opSlice != null) {
            insertLimit();
        }

         return new SQLQuery(
                this.sql,
                this.context
        );
    }

    private void insertLimit() {
        String select = "SELECT * ";
        String from = " FROM (" + this. sql + ") sl \n";
        String limit;
        if (opSlice.getStart() > 0) {
            limit = "LIMIT " + opSlice.getLength() + " OFFSET " + opSlice.getStart();
        } else {
            limit = "LIMIT " + opSlice.getLength();
        }
        this.sql = select + from + limit;
    }

    /**
     * Gets the SELECT clause of the SQL query with the resource or literal table
     *
     * @return the SELECT clause of the SQL query
     */
    private String getSelectVariablesResourceOrLiteral() {
        return Streams.mapWithIndex(this.context.sqlVariables().stream()
                .filter(sqlVariable -> sqlVariable.getSqlVarType() != SQLVarType.BIT_STRING), (sqlVariable, index) -> {
            if (sqlVariable.isValue()) {
                if (sqlVariable.getSqlVarType() == SQLVarType.AGGREGATED) {
                    return (
                            sqlVariable.getSqlVarName().replace(".", "agg") + " as name$" + sqlVariable.getSqlVarName().replace(".", "agg")
                    );
                } else {
                    return (
                           "v$" + sqlVariable.getSqlVarName() + " as " + sqlVariable.getSqlVarName()
                    );
                }
            } else {
                if (sqlVariable.getSqlVarType() == SQLVarType.AGGREGATED) {
                    return (
                            sqlVariable.getSqlVarName().replace(".", "agg") + " as name$" + sqlVariable.getSqlVarName().replace(".", "agg")
                    );
                } else {
                    return (
                            "rl" + index + ".name as name$" + sqlVariable.getSqlVarName() + ", rl" + index + ".type as type$" + sqlVariable.getSqlVarName()
                    );
                }
            }

        }).collect(Collectors.joining(", "));
    }

    /**
     * Gets the finalized query with the JOIN clause of the SQL query with the resource_or_literal/versioned_named_graph table
     *
     * @return the finalized SQL query
     */
    private String getJoinVariablesResourceOrLiteral() {
        return Streams.mapWithIndex(this.context.sqlVariables().stream()
                        .filter(sqlVariable -> sqlVariable.getSqlVarType() != SQLVarType.BIT_STRING), (sqlVariable, index) -> {
                    if (sqlVariable.isValue()) {
                        return null;
                    } else {
                        return switch (sqlVariable.getSqlVarType()) {
                            case DATA -> " JOIN resource_or_literal rl" + index + " ON indexes_table.v$" +
                                    sqlVariable.getSqlVarName() +
                                    " = rl" + index + ".id_resource_or_literal";
                            case GRAPH_NAME -> " JOIN versioned_named_graph vng" + index + " ON indexes_table.ng$" +
                                    sqlVariable.getSqlVarName() + " = vng" + index +
                                    ".id_named_graph AND get_bit(indexes_table.bs$" +
                                    sqlVariable.getSqlVarName() + ", vng" + index + ".index_version - 1) = 1 \n" +
                                    " JOIN resource_or_literal rl" + index + " ON vng" + index + ".id_versioned_named_graph = rl" +
                                    index + ".id_resource_or_literal";
                            case VERSIONED_NAMED_GRAPH -> " JOIN resource_or_literal rl" + index + " ON " +
                                    "indexes_table.vng$" + sqlVariable.getSqlVarName() +
                                    " = rl" + index + ".id_resource_or_literal";
                            default -> "";
                        };
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" \n"));
    }
}
