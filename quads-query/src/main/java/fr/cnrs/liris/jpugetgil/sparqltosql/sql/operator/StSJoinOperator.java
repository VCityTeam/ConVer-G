package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.*;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.comparison.EqualToOperator;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.comparison.NotEqualToOperator;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_Variable;

import java.util.*;
import java.util.stream.Collectors;

public class StSJoinOperator extends StSOperator {
    private final SQLQuery leftSQLQuery;

    private final SQLQuery rightSQLQuery;

    public StSJoinOperator(SQLQuery leftSQLQuery, SQLQuery rightSQLQuery) {
        this.leftSQLQuery = leftSQLQuery;
        this.rightSQLQuery = rightSQLQuery;
    }

    @Override
    public SQLQuery buildSQLQuery() {
        SQLContext leftContext = leftSQLQuery.getContext()
                .setTableName("left_table")
                .setTableIndex(leftSQLQuery.getContext().tableIndex() == null ? 0 : leftSQLQuery.getContext().tableIndex() + 1);
        leftSQLQuery.setContext(leftContext);
        SQLContext rightContext = rightSQLQuery.getContext()
                .setTableName("right_table")
                .setTableIndex(rightSQLQuery.getContext().tableIndex() == null ? 0 : rightSQLQuery.getContext().tableIndex() + 1);
        rightSQLQuery.setContext(rightContext);

        List<SQLVariable> commonVariablesWithoutGraph = new ArrayList<>();
        Node graphLeftVariable = null;
        Node graphRightVariable = null;
        Node graphVariable = null;

        leftSQLQuery.getContext().sqlVariables().stream()
                .filter(sqlVariable -> sqlVariable.getSqlVarType() == SQLVarType.DATA)
                .forEach(leftSQLVar -> {
                    if (rightSQLQuery.getContext().sqlVariables().stream()
                            .filter(sqlVariable -> sqlVariable.getSqlVarType() == SQLVarType.DATA)
                            .anyMatch(rightSQLVar -> rightSQLVar.getSqlVarName().equals(leftSQLVar.getSqlVarName()))) {
                        commonVariablesWithoutGraph.add(leftSQLVar);
                    }
                });

        // for loop rightSQLQuery sparqlVarOccurrences
        if (rightSQLQuery.getContext().graph() instanceof Node_Variable) {
            graphRightVariable = rightSQLQuery.getContext().graph();
            graphVariable = rightSQLQuery.getContext().graph();
        }

        if (leftSQLQuery.getContext().graph() instanceof Node_Variable) {
            graphLeftVariable = leftSQLQuery.getContext().graph();
            graphVariable = leftSQLQuery.getContext().graph();
        }

        SQLClause.SQLClauseBuilder sqlJoinClauseBuilder = new SQLClause.SQLClauseBuilder();

        commonVariablesWithoutGraph.forEach(commonNodeWithoutGraph ->
                sqlJoinClauseBuilder.and(
                        Objects.requireNonNull(leftSQLQuery.getContext().sqlVariables().stream()
                                .filter(leftSQLVar ->
                                        leftSQLVar.getSqlVarName().equals(commonNodeWithoutGraph.getSqlVarName()) &&
                                                leftSQLVar.getSqlVarType() == commonNodeWithoutGraph.getSqlVarType()
                                ).findFirst()
                                .orElse(null)
                        ).join(
                                rightSQLQuery.getContext().sqlVariables().stream()
                                        .filter(rightSQLVar ->
                                                rightSQLVar.getSqlVarName().equals(commonNodeWithoutGraph.getSqlVarName()) &&
                                                        rightSQLVar.getSqlVarType() == commonNodeWithoutGraph.getSqlVarType()
                                        ).findFirst()
                                        .orElse(null),
                                leftSQLQuery.getContext().tableName() + leftSQLQuery.getContext().tableIndex(),
                                rightSQLQuery.getContext().tableName() + rightSQLQuery.getContext().tableIndex()
                        )
                )
        );

//        String select = buildSelectVariables(leftSQLQuery.getContext(), rightSQLQuery.getContext(), commonVariables);
        String select = buildSelectVariablesWithoutGraph(leftSQLQuery.getContext(), rightSQLQuery.getContext());
        if (graphLeftVariable != null && graphRightVariable != null && graphRightVariable.getName().equals(graphLeftVariable.getName())) {
            select += ", (" + leftSQLQuery.getContext().tableName() + leftSQLQuery.getContext().tableIndex() +
                    ".bs$" + leftSQLQuery.getContext().graph().getName() + " & " +
                    rightSQLQuery.getContext().tableName() + rightSQLQuery.getContext().tableIndex() +
                    ".bs$" + rightSQLQuery.getContext().graph().getName() + ") as bs$" + graphVariable.getName() + ", " +
                    leftSQLQuery.getContext().tableName() + leftSQLQuery.getContext().tableIndex() + ".ng$" + graphLeftVariable.getName();

            sqlJoinClauseBuilder.and(
                    new NotEqualToOperator()
                            .buildComparisonOperatorSQL(
                                    "bit_count(" + leftSQLQuery.getContext().tableName() + leftSQLQuery.getContext().tableIndex()
                                            + ".bs$" + leftSQLQuery.getContext().graph().getName() +
                                            " & " + rightSQLQuery.getContext().tableName() +
                                            rightSQLQuery.getContext().tableIndex() + ".bs$" +
                                            rightSQLQuery.getContext().graph().getName() + ")",
                                    "0"
                            )
            ).and(
                    new EqualToOperator()
                            .buildComparisonOperatorSQL(
                                    leftSQLQuery.getContext().tableName() + leftSQLQuery.getContext().tableIndex() +
                                            ".ng$" + leftSQLQuery.getContext().graph().getName(),
                                    rightSQLQuery.getContext().tableName() + rightSQLQuery.getContext().tableIndex() +
                                            ".ng$" + rightSQLQuery.getContext().graph().getName()
                            )
            );
        } else if (graphLeftVariable != null && graphRightVariable != null) {
            select += getContextSelectGraphVariable(leftSQLQuery.getContext()) +
                    getContextSelectGraphVariable(rightSQLQuery.getContext());
        } else if (graphLeftVariable != null) {
            select += getContextSelectGraphVariable(leftSQLQuery.getContext());
        } else if (graphRightVariable != null) {
            select += getContextSelectGraphVariable(rightSQLQuery.getContext());
        }

        String sql = "SELECT " + select + " FROM (" + leftSQLQuery.getSql() + ") " +
                leftSQLQuery.getContext().tableName() + leftSQLQuery.getContext().tableIndex() +
                " JOIN (" + rightSQLQuery.getSql() + ") " + rightSQLQuery.getContext().tableName() +
                rightSQLQuery.getContext().tableIndex() + " ON " + sqlJoinClauseBuilder.build().clause;

        Map<Node, List<SPARQLOccurrence>> mergedOccurrences = mergeMapOccurrences(
                leftSQLQuery.getContext().sparqlVarOccurrences(),
                rightSQLQuery.getContext().sparqlVarOccurrences()
        );

        this.sqlVariables = leftSQLQuery.getContext().sqlVariables();
        for (SQLVariable sqlVariable : rightSQLQuery.getContext().sqlVariables()) {
            if (this.sqlVariables.stream()
                    .noneMatch(sqlVar -> sqlVar.getSqlVarName().equals(sqlVariable.getSqlVarName()))
            ) {
                this.sqlVariables.add(sqlVariable);
            }
        }

        SQLContext context = new SQLContext(
                graphVariable,
                mergedOccurrences,
                "join",
                leftSQLQuery.getContext().tableIndex() + 1,
                this.sqlVariables
        );
        return new SQLQuery(sql, context);
    }

    private String getContextSelectGraphVariable(SQLContext context) {
        return ", " + context.tableName() + context.tableIndex() +
                ".ng$" + context.graph().getName() + ", " +
                context.tableName() + context.tableIndex() + ".bs$" +
                context.graph().getName();
    }

    private String buildSelectVariablesWithoutGraph(SQLContext leftContext, SQLContext rightContext) {
        Set<String> leftSelect = leftContext.sparqlVarOccurrences().keySet().stream()
                .filter(node -> node instanceof Node_Variable)
                .filter(node -> leftContext.graph() == null || !node.equals(leftContext.graph()))
                .map(node ->
                        ".v$" + node.getName()
                )
                .collect(Collectors.toSet());

        Set<String> rightSelect = rightContext.sparqlVarOccurrences().keySet().stream()
                .filter(node -> node instanceof Node_Variable)
                .filter(node -> rightContext.graph() == null || !node.equals(rightContext.graph()))
                .map(node ->
                        ".v$" + node.getName()
                )
                .collect(Collectors.toSet());

        Set<String> unionSelect = new HashSet<>(leftSelect);
        unionSelect.addAll(rightSelect);

        return unionSelect.stream().map(var -> {
            if (leftSelect.contains(var)) {
                return leftContext.tableName() + leftContext.tableIndex() + var;
            }
            return rightContext.tableName() + rightContext.tableIndex() + var;
        }).collect(Collectors.joining(", "));
    }

    private Map<Node, List<SPARQLOccurrence>> mergeMapOccurrences(
            Map<Node, List<SPARQLOccurrence>> leftMapOccurrences,
            Map<Node, List<SPARQLOccurrence>> rightMapOccurrences
    ) {
        Map<Node, List<SPARQLOccurrence>> mergedOccurrences = new HashMap<>(leftMapOccurrences);

        rightMapOccurrences.forEach((node, occurrences) ->
                mergedOccurrences.computeIfAbsent(node, k -> new ArrayList<>()).addAll(occurrences)
        );

        return mergedOccurrences;
    }
}
