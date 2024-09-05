package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import com.google.common.collect.Streams;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVariable;
import org.apache.jena.sparql.core.Var;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class StSIdentifyOperator extends StSOperator {
    private final SQLContext context;

    private final SQLQuery sqlQuery;

    private final Set<Var> varsMentioned;

    public StSIdentifyOperator(SQLQuery sqlQuery, Set<Var> varsMentioned) {
        this.context = sqlQuery.getContext();
        this.sqlQuery = sqlQuery;
        this.sqlVariables = sqlQuery.getContext().sqlVariables();
        this.varsMentioned = varsMentioned;
    }

    @Override
    public SQLQuery buildSQLQuery() {
        String select = getSelect();
        String join = getJoin();
        List<SQLVariable> sqlVariables = new ArrayList<>();

        for (SQLVariable sqlVariable : sqlQuery.getContext().sqlVariables()) {
            if (
                    sqlVariable.getSqlVarType() == SQLVarType.DATA &&
                            !sqlVariable.isValue() && varsMentioned
                            .stream()
                            .anyMatch(varMentioned -> varMentioned.getVarName().equals(sqlVariable.getSqlVarName()))
            ) {
                sqlVariables.add(new SQLVariable(SQLVarType.DATA, sqlVariable.getSqlVarName(), true));
            } else {
                sqlVariables.add(sqlVariable);
            }
        }

        SQLContext context = this.sqlQuery.getContext().setSQLVariables(sqlVariables);

        return new SQLQuery(
                "SELECT " + select + " FROM (" + sqlQuery.getSql() + ") identify_table " + join, context
        );
    }

    /**
     * Get the select part of the SQL query
     *
     * @return the built select part
     */
    private String getSelect() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Streams.mapWithIndex(this.context.sqlVariables()
                        .stream()
                        .filter(sqlVariable -> varsMentioned
                                .stream()
                                .anyMatch(varMentioned -> varMentioned.getVarName().equals(sqlVariable.getSqlVarName()))
                        ), (sqlVariable, index) -> (
                        "rl" + index + ".name as " + getProjectionOfSQLVariable(sqlVariable)
                ))
                .collect(Collectors.joining(", ")));
        String otherVarsProjection = this.context.sqlVariables()
                .stream()
                .filter(sqlVariable -> varsMentioned
                        .stream()
                        .noneMatch(varMentioned -> varMentioned.getVarName().equals(sqlVariable.getSqlVarName()))
                )
                .map(this::getProjectionOfSQLVariable)
                .collect(Collectors.joining(", "));

        if (!otherVarsProjection.isBlank()) {
            stringBuilder.append(", ");
            stringBuilder.append(otherVarsProjection);
        }

        return stringBuilder.toString();
    }

    private String getProjectionOfSQLVariable(SQLVariable sqlVariable) {
        return switch (sqlVariable.getSqlVarType()) {
            case DATA -> "v$";
            case VERSIONED_NAMED_GRAPH -> "vng$";
            case BIT_STRING -> "bs$";
            case GRAPH_NAME -> "ng$";
            case AGGREGATED -> "";
        } + sqlVariable.getSqlVarName();
    }

    /**
     * Get the join part of the SQL query (i.e. the part that gets the variables values)
     *
     * @return the built join part
     */
    private String getJoin() {
        return Streams.mapWithIndex(this.context.sqlVariables().stream()
                        .filter(sqlVariable -> sqlVariable.getSqlVarType() != SQLVarType.BIT_STRING && varsMentioned
                                .stream()
                                .anyMatch(varMentioned -> varMentioned.getVarName().equals(sqlVariable.getSqlVarName()))
                        ), (sqlVariable, index) -> {
                    if (sqlVariable.isValue()) {
                        return null;
                    } else {
                        return switch (sqlVariable.getSqlVarType()) {
                            case DATA -> " JOIN resource_or_literal rl" + index + " ON identify_table.v$" +
                                    sqlVariable.getSqlVarName() +
                                    " = rl" + index + ".id_resource_or_literal";
                            case GRAPH_NAME -> " JOIN versioned_named_graph vng" + index + " ON identify_table.ng$" +
                                    sqlVariable.getSqlVarName() + " = vng" + index +
                                    ".id_named_graph AND get_bit(identify_table.bs$" +
                                    sqlVariable.getSqlVarName() + ", vng" + index + ".index_version - 1) = 1 \n" +
                                    " JOIN resource_or_literal rl" + index + " ON vng" + index + ".id_versioned_named_graph = rl" +
                                    index + ".id_resource_or_literal";
                            case VERSIONED_NAMED_GRAPH -> " JOIN resource_or_literal rl" + index + " ON " +
                                    "identify_table.vng$" + sqlVariable.getSqlVarName() +
                                    " = rl" + index + ".id_resource_or_literal";
                            default -> "";
                        };
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" \n"));
    }
}