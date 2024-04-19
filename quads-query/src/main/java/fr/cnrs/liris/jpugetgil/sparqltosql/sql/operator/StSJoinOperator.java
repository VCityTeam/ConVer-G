package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.*;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.comparison.EqualToOperator;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.comparison.NotEqualToOperator;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_Variable;
import org.apache.jena.sparql.algebra.op.OpJoin;

import java.sql.SQLType;
import java.util.*;
import java.util.stream.Collectors;

public class StSJoinOperator extends StSOperator {

    private final OpJoin opJoin;
    private final StSOperator left;
    private final StSOperator right;
    private Map<String, JoinVar> vars = new HashMap<>();

    public StSJoinOperator(SQLContext context, OpJoin opJoin) {
        super(context);
        this.opJoin = opJoin;
        left = fromSPARQL(context,opJoin.getLeft());
        right = fromSPARQL(context,opJoin.getRight());
        for(SQLVariable v : left.getSQLVariables()) {
            vars.
        }
    }



    private enum OccType { DATA, GRAPH, NONE; public OccType fromSQLType(SQLVarType type) {
        return switch (type) {
            case SQLVarType.BIT_STRING -> GRAPH;
            case SQLVarType.GRAPH_NAME -> GRAPH;
            case SQLVarType.VERSIONED_NAMED_GRAPH -> throw new IllegalStateException();
            case SQLVarType.DATA -> DATA;

        };
    }}
    private record JoinVar(String name, OccType left, OccType right) {
        public JoinVar setRight(OccType newRight) { return new JoinVar(name, left, newRight); }
    }

    @Override
    public SQLQuery buildSQLQuery() {
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
                                "left_table",
                                "right_table"
                        )
                )
        );

//        String select = buildSelectVariables(leftSQLQuery.getContext(), rightSQLQuery.getContext(), commonVariables);
        String select = buildSelectVariablesWithoutGraph(leftSQLQuery.getContext(), rightSQLQuery.getContext());
        if (
                graphLeftVariable != null && graphRightVariable != null &&
                        graphRightVariable.getName().equals(graphLeftVariable.getName())
        ) {
            select += ", (left_table.bs$" + leftSQLQuery.getContext().graph().getName() + " & right_table.bs$" +
                    rightSQLQuery.getContext().graph().getName() + ") as bs$" + graphVariable.getName() + ", left_table.ng$" +
                    graphLeftVariable.getName();

            sqlJoinClauseBuilder.and(
                    new NotEqualToOperator()
                            .buildComparisonOperatorSQL(
                                    "bit_count(left_table.bs$" + leftSQLQuery.getContext().graph().getName() +
                                            " & right_table.bs$" +
                                            rightSQLQuery.getContext().graph().getName() + ")",
                                    "0"
                            )
            ).and(
                    new EqualToOperator()
                            .buildComparisonOperatorSQL(
                                    "left_table.ng$" + leftSQLQuery.getContext().graph().getName(),
                                    "right_table.ng$" + rightSQLQuery.getContext().graph().getName()
                            )
            );
        } else if (graphLeftVariable != null && graphRightVariable != null) {
            select += getContextSelectGraphVariable(leftSQLQuery.getContext(), "left_table") +
                    getContextSelectGraphVariable(rightSQLQuery.getContext(), "right_table");
        } else if (graphLeftVariable != null) {
            select += getContextSelectGraphVariable(leftSQLQuery.getContext(), "left_table");
        } else if (graphRightVariable != null) {
            select += getContextSelectGraphVariable(rightSQLQuery.getContext(), "right_table");
        }

        String sql = "SELECT " + select + " FROM (" + leftSQLQuery.getSql() + ") left_table JOIN (" +
                rightSQLQuery.getSql() + ") right_table ON " + sqlJoinClauseBuilder.build().clause;

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
                this.sqlVariables
        );
        return new SQLQuery(sql, context);
    }

    private String getContextSelectGraphVariable(SQLContext context, String tableName) {
        return ", " + tableName +
                ".ng$" + context.graph().getName() + ", " +
                tableName + ".bs$" +
                context.graph().getName();
    }

    private String buildSelectVariablesWithoutGraph(SQLContext leftContext, SQLContext rightContext) {
        Set<String> leftSelect = leftContext.sparqlVarOccurrences().keySet().stream()
                .filter(Node_Variable.class::isInstance)
                .filter(node -> leftContext.graph() == null || !node.equals(leftContext.graph()))
                .map(node ->
                        ".v$" + node.getName()
                )
                .collect(Collectors.toSet());

        Set<String> rightSelect = rightContext.sparqlVarOccurrences().keySet().stream()
                .filter(Node_Variable.class::isInstance)
                .filter(node -> rightContext.graph() == null || !node.equals(rightContext.graph()))
                .map(node ->
                        ".v$" + node.getName()
                )
                .collect(Collectors.toSet());

        Set<String> unionSelect = new HashSet<>(leftSelect);
        unionSelect.addAll(rightSelect);

        return unionSelect.stream().map(unionS -> {
            if (leftSelect.contains(unionS)) {
                return "left_table" + unionS;
            }
            return "right_table" + unionS;
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
