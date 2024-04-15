package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.SPARQLPositionType;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLClause;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVariable;
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
        List<String> commonVariables = new ArrayList<>();
        List<Node> commonNodesWithoutGraph = new ArrayList<>();
        Node graphLeftVariable = null;
        Node graphRightVariable = null;
        Node graphVariable = null;

        leftSQLQuery.getContext().sqlVariables().forEach(leftSQLVar -> {
            if (rightSQLQuery.getContext().sqlVariables().stream()
                    .anyMatch(rightSQLVar -> rightSQLVar.getSqlVarName().equals(leftSQLVar.getSqlVarName()))) {
                commonVariables.add(leftSQLVar.getSqlVarName());
            }
        });

        // build commonNodesWithoutGraph
        leftSQLQuery.getContext().sparqlVarOccurrences().keySet().forEach(node -> {
            if (rightSQLQuery.getContext().sparqlVarOccurrences().containsKey(node)
                    && rightSQLQuery.getContext().sparqlVarOccurrences().get(node).stream()
                    .anyMatch(SPARQLOccurrence -> SPARQLOccurrence.getType() == SPARQLPositionType.GRAPH_NAME)) {
                commonNodesWithoutGraph.add(node);
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

        SQLClause.SQLClauseBuilder sqlClauseBuilder = new SQLClause.SQLClauseBuilder();

        commonNodesWithoutGraph.forEach(node -> sqlClauseBuilder.and(
                new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftSQLQuery.getContext().tableName() + leftSQLQuery.getContext().tableIndex() +
                                        ".v$" + node.getName(),
                                rightSQLQuery.getContext().tableName() + rightSQLQuery.getContext().tableIndex() +
                                        ".v$" + node.getName()
                        )
        ));

//        String select = buildSelectVariables(leftSQLQuery.getContext(), rightSQLQuery.getContext(), commonVariables);
        String select = buildSelectVariablesWithoutGraph(leftSQLQuery.getContext(), rightSQLQuery.getContext());
        if (graphRightVariable != null && graphLeftVariable != null) {
            select += ", (" + leftSQLQuery.getContext().tableName() + leftSQLQuery.getContext().tableIndex() +
                    ".ng$" + leftSQLQuery.getContext().graph().getName() + " & " +
                    rightSQLQuery.getContext().tableName() + rightSQLQuery.getContext().tableIndex() +
                    ".ng$" + rightSQLQuery.getContext().graph().getName() + ") as ng$" + graphVariable.getName();

            sqlClauseBuilder.and(
                    new NotEqualToOperator()
                            .buildComparisonOperatorSQL(
                                    "bit_count(" + leftSQLQuery.getContext().tableName() + leftSQLQuery.getContext().tableIndex()
                                            + ".bs$" + leftSQLQuery.getContext().graph().getName() +
                                            " & " + rightSQLQuery.getContext().tableName() +
                                            rightSQLQuery.getContext().tableIndex() + ".bs$" +
                                            rightSQLQuery.getContext().graph().getName() + ")",
                                    "0"
                            )
            );
        } else if (graphLeftVariable != null) {
            if (leftSQLQuery.getContext().graph() != null) {
                select += ", " + leftSQLQuery.getContext().tableName() + leftSQLQuery.getContext().tableIndex() +
                        ".ng$" + leftSQLQuery.getContext().graph().getName() + ", " +
                        leftSQLQuery.getContext().tableName() + leftSQLQuery.getContext().tableIndex() + ".bs$" +
                        leftSQLQuery.getContext().graph().getName();
            }
        } else if (graphRightVariable != null) {
            if (rightSQLQuery.getContext().graph() != null) {
                select += ", " + rightSQLQuery.getContext().tableName() + rightSQLQuery.getContext().tableIndex() +
                        ".ng$" + rightSQLQuery.getContext().graph().getName() + ", " +
                        rightSQLQuery.getContext().tableName() + rightSQLQuery.getContext().tableIndex() + ".bs$" +
                        rightSQLQuery.getContext().graph().getName();
            }
        }

        String sql = "SELECT " + select + " FROM (" + leftSQLQuery.getSql() + ") " +
                leftSQLQuery.getContext().tableName() + leftSQLQuery.getContext().tableIndex() +
                " JOIN (" + rightSQLQuery.getSql() + ") " + rightSQLQuery.getContext().tableName() +
                rightSQLQuery.getContext().tableIndex() + " ON " + sqlClauseBuilder.build().clause;

        Map<Node, List<SPARQLOccurrence>> mergedOccurrences = mergeMapOccurrences(
                leftSQLQuery.getContext().sparqlVarOccurrences(),
                rightSQLQuery.getContext().sparqlVarOccurrences()
        );

        this.sqlVariables = leftSQLQuery.getContext().sqlVariables();
        for (SQLVariable sqlVariable : rightSQLQuery.getContext().sqlVariables()) {
            if (!this.sqlVariables.contains(sqlVariable)) {
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

//    private String buildSelectVariables(SQLContext leftContext, SQLContext rightContext, List<String> commonVariables) {
//        for (String commonVariable : commonVariables) {
//            SQLVariable leftSQLVar = leftContext.sqlVariables().stream()
//                    .filter(sqlVariable -> sqlVariable.getSqlVarName().equals(commonVariable))
//                    .findFirst()
//                    .orElse(null);
//            SQLVariable rightSQLVar = rightContext.sqlVariables().stream()
//                    .filter(sqlVariable -> sqlVariable.getSqlVarName().equals(commonVariable))
//                    .findFirst()
//                    .orElse(null);
//            String common = buildSelectVariable(leftSQLVar, rightSQLVar);
//        }
//        return "";
//    }

//    private String buildSelectVariable(SQLVariable leftSQLVar, SQLVariable rightSQLVar) {
//        return switch (leftSQLVar.getSqlVarType()) {
//            case DATA -> {
//                yield switch (rightSQLVar.getSqlVarType()) {
//                    case DATA -> ".v$" + leftSQLVar.getSqlVarName();
//                    case VERSIONED_NAMED_GRAPH -> {
//                        yield null;
//                    }
//                    case BIT_STRING -> {
//                        yield null;
//                    }
//                    case GRAPH_NAME -> {
//                        yield null;
//                    }
//                };
//            }
//            case VERSIONED_NAMED_GRAPH -> {
//                yield switch (rightSQLVar.getSqlVarType()) {
//                    case DATA -> {
//                        yield null;
//                    }
//                    case VERSIONED_NAMED_GRAPH -> {
//                        yield null;
//                    }
//                    case BIT_STRING -> {
//                        yield null;
//                    }
//                    case GRAPH_NAME -> {
//                        yield null;
//                    }
//                };
//            }
//            case BIT_STRING -> {
//                yield switch (rightSQLVar.getSqlVarType()) {
//                    case DATA -> {
//                        yield null;
//                    }
//                    case VERSIONED_NAMED_GRAPH -> {
//                        yield null;
//                    }
//                    case BIT_STRING -> {
//                        yield "bit_count(" + leftSQLVar.getSqlVarName() + " & " + rightSQLVar.getSqlVarName() + ") as bs$" +
//                                leftSQLVar.getSqlVarName();
//                    }
//                    case GRAPH_NAME -> {
//                        yield null;
//                    }
//                };
//            }
//            case GRAPH_NAME -> {
//                yield switch (rightSQLVar.getSqlVarType()) {
//                    case DATA -> {
//                        yield null;
//                    }
//                    case VERSIONED_NAMED_GRAPH -> {
//                        yield null;
//                    }
//                    case BIT_STRING -> {
//                        yield null;
//                    }
//                    case GRAPH_NAME -> {
//                        yield null;
//                    }
//                };
//            };
//        }
//    }

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
