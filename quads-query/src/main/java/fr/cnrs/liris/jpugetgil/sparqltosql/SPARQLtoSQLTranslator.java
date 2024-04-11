package fr.cnrs.liris.jpugetgil.sparqltosql;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;
import fr.cnrs.liris.jpugetgil.sparqltosql.dao.ResourceOrLiteral;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLClause;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.context.StSBGPOperator;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.context.StSGraphOperator;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator.EqualToOperator;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator.NotEqualToOperator;
import org.apache.jena.graph.*;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is used to translate a SPARQL query into a SQL query
 */
public class SPARQLtoSQLTranslator {

    private static final Logger log = LoggerFactory.getLogger(SPARQLtoSQLTranslator.class);

    private final Session session;

    private final Map<String, Integer> uriToIdMap = new HashMap<>();

    public SPARQLtoSQLTranslator(SessionFactory sessionFactory) {
        this.session = sessionFactory.openSession();
    }

    /**
     * Translate the SPARQL query into a SQL query
     *
     * @return the SQL query
     */
    public String translate(Query query) {
        Op op = Algebra.compile(query);

        SQLQuery qu = buildSPARQLContext(op);

        SQLContext sqlContext = new SQLContext(
                qu.getContext().graph(),
                qu.getContext().varOccurrences(),
                "indexes_table",
                qu.getContext().tableIndex() == null ? 0 : qu.getContext().tableIndex() + 1
        );

        // Join with resource or literal table for each variable
        String select = "SELECT " + getSelectVariablesResourceOrLiteral(sqlContext);
        String from = " FROM (" + qu.getSql() + ") " + sqlContext.tableName() + sqlContext.tableIndex();
        String join = getJoinVariablesResourceOrLiteral(sqlContext);

        SQLQuery finalQuery = new SQLQuery(
                select + from + join,
                sqlContext
        );

        log.info(finalQuery.getSql());

        return finalQuery.getSql();
    }

    private SQLQuery buildSPARQLContext(Op op) {
        return buildSPARQLContext(op, new SQLContext(null, new HashMap<>(), null, null));
    }

    private SQLQuery buildSPARQLContext(Op op, SQLContext context) {

        return switch (op) {
            case OpJoin opJoin -> {
                SQLQuery leftSQLQuery = buildSPARQLContext(opJoin.getLeft(), context);
                SQLContext leftContext = leftSQLQuery.getContext()
                        .setTableName("left_table")
                        .setTableIndex(leftSQLQuery.getContext().tableIndex() == null ? 0 : leftSQLQuery.getContext().tableIndex() + 1);
                leftSQLQuery.setContext(leftContext);

                SQLQuery rightSQLQuery = buildSPARQLContext(opJoin.getRight(), context);
                SQLContext rightContext = rightSQLQuery.getContext()
                        .setTableName("right_table")
                        .setTableIndex(rightSQLQuery.getContext().tableIndex() == null ? 0 : rightSQLQuery.getContext().tableIndex() + 1);
                rightSQLQuery.setContext(rightContext);

                yield buildSQLQueryJoin(leftSQLQuery, rightSQLQuery);
            }
            case OpLeftJoin opLeftJoin -> {
                // Jointure avec un/des var qui sont dans un optional
                buildSPARQLContext(opLeftJoin.getLeft(), context);
                buildSPARQLContext(opLeftJoin.getRight(), context);
                yield null;
            }
            case OpUnion opUnion -> {
                SQLQuery leftQuery = buildSPARQLContext(opUnion.getLeft(), context);
                SQLQuery rightQuery = buildSPARQLContext(opUnion.getRight(), context);

                Node graph = leftQuery.getContext().graph() != null ?
                        leftQuery.getContext().graph() : rightQuery.getContext().graph();
                Map<Node, List<Occurrence>> varOccurrences = mergeMapOccurrences(
                        leftQuery.getContext().varOccurrences(),
                        rightQuery.getContext().varOccurrences()
                );
                SQLContext sqlContext = new SQLContext(
                        graph,
                        varOccurrences,
                        "union_table",
                        leftQuery.getContext().tableIndex() == null ? 0 : leftQuery.getContext().tableIndex() + 1
                );

                yield new SQLQuery(
                        "SELECT * FROM (" + leftQuery.getSql() + ") UNION (" + rightQuery.getSql() + ")" +
                                sqlContext.tableName() + sqlContext.tableIndex(),
                        leftQuery.getContext()
                );
            }
            case OpProject opProject -> {
                SQLQuery sqlQuery = buildSPARQLContext(opProject.getSubOp(), context);

                Map<Node, List<Occurrence>> varOccurrences = new HashMap<>();
                for (Var var : opProject.getVars()) {
                    Occurrence occ = sqlQuery.getContext().varOccurrences().get(var).stream()
                            .findFirst()
                            .orElse(null);

                    if (sqlQuery.getContext().varOccurrences().get(var).stream()
                            .anyMatch(occurrence -> occurrence.getType() == SPARQLPositionType.GRAPH_NAME)) {
                        varOccurrences.put(var, Collections.singletonList(new Occurrence(SPARQLPositionType.GRAPH_NAME, 0, occ.getContextType())));
                    } else {
                        varOccurrences.put(var, Collections.singletonList(new Occurrence(occ.getType(), 0, occ.getContextType())));
                    }
                }

                SQLContext sqlContext = new SQLContext(
                        sqlQuery.getContext().graph(),
                        varOccurrences,
                        "project_table",
                        sqlQuery.getContext().tableIndex() == null ? 0 : sqlQuery.getContext().tableIndex() + 1
                );

                yield new SQLQuery(
                        getSqlProjectionsQuery(opProject.getVars(), sqlQuery) + " FROM (" + sqlQuery.getSql() + ") " +
                                sqlQuery.getContext().tableName() + sqlQuery.getContext().tableIndex(),
                        sqlContext
                );
            }
            case OpTable opTable -> {
//                opTable.getTable().toString();
                yield null;
            }
            case OpQuadPattern opQuadPattern -> {
                opQuadPattern.getGraphNode().getURI();
                yield null;
            }
            case OpExtend opExtend -> {
                VarExprList varExprList = opExtend.getVarExprList();
                List<Var> vars = varExprList.getVars();
                Map<Var, Expr> exprMap = varExprList.getExprs();
                yield buildSPARQLContext(opExtend.getSubOp(), context);
            }
            case OpDistinct opDistinct -> {
                SQLQuery sqlQuery = buildSPARQLContext(opDistinct.getSubOp(), context);
                SQLContext sqlContext = new SQLContext(
                        sqlQuery.getContext().graph(),
                        sqlQuery.getContext().varOccurrences(),
                        "distinct_table",
                        sqlQuery.getContext().tableIndex() == null ? 0 : sqlQuery.getContext().tableIndex() + 1
                );

                yield new SQLQuery(
                        "SELECT DISTINCT * FROM (" + sqlQuery.getSql() +
                                ") " + sqlContext.tableName() + sqlContext.tableIndex(),
                        sqlContext
                );
            }
            case OpFilter opFilter -> {
                yield buildSPARQLContext(opFilter.getSubOp(), null);
            }
            case OpOrder opOrder -> {
                yield buildSPARQLContext(opOrder.getSubOp(), null);
            }
            case OpGroup opGroup -> {
                SQLQuery sqlQuery = buildSPARQLContext(opGroup.getSubOp(), context);
                SQLContext sqlContext = new SQLContext(
                        sqlQuery.getContext().graph(),
                        sqlQuery.getContext().varOccurrences(),
                        "group_table",
                        sqlQuery.getContext().tableIndex() == null ? 0 : sqlQuery.getContext().tableIndex() + 1
                );

                // TODO : GROUP BY
                VarExprList exprList = opGroup.getGroupVars();
                List<Var> vars = exprList.getVars();
                Map<Var, Expr> exprVar = exprList.getExprs();
                String groupBy = vars.stream()
                        .map(var -> {
                            if (sqlQuery.getContext().varOccurrences().get(var).stream()
                                    .anyMatch(occurrence -> occurrence.getType() == SPARQLPositionType.GRAPH_NAME)) {
                                return sqlContext.tableName() + sqlContext.tableIndex() +
                                        ".ng$" + var.getName();
                            } else {
                                return sqlContext.tableName() + sqlContext.tableIndex() +
                                        ".v$" + var.getName();
                            }
                        })
                        .collect(Collectors.joining(", "));

                // FIXME : projections
                yield new SQLQuery(
                        "SELECT * FROM (" + sqlQuery.getSql() +
                                ") GROUP BY (" + groupBy + ")",
                        sqlContext
                );
            }
            case OpSlice opSlice -> {
                yield buildSPARQLContext(opSlice.getSubOp(), null);
            }
            case OpTopN opTopN -> {
                yield buildSPARQLContext(opTopN.getSubOp(), null);
            }
            case OpPath opPath -> {
                yield null;
            }
            case OpLabel opLabel -> {
                yield buildSPARQLContext(opLabel.getSubOp(), null);
            }
            case OpNull opNull -> {
                yield buildSPARQLContext(null, context);
            }
            case OpList opList -> {
                yield buildSPARQLContext(opList.getSubOp(), null);
            }
            case OpBGP opBGP -> {
                SQLContext cont = addURIsToContext(opBGP, context);
                addURIsToMap(opBGP);
                getURIsIds();

                yield new StSBGPOperator(opBGP, cont, uriToIdMap, session)
                        .buildSQLQuery();
            }
            case OpGraph opGraph -> {
                if (opGraph.getNode() instanceof Node_URI) {
                    uriToIdMap.put(op.getName(), null);
                }
                if (opGraph.getNode() instanceof Node_Literal) {
                    uriToIdMap.put(opGraph.getNode().getLiteralLexicalForm(), null);
                }

                Map<Node, List<Occurrence>> newVarOccurrences = new HashMap<>(context.varOccurrences());

                Integer count = newVarOccurrences
                        .keySet()
                        .stream()
                        .filter(
                                node -> newVarOccurrences.get(node).stream()
                                        .anyMatch(occurrence -> occurrence.getType() == SPARQLPositionType.GRAPH_NAME)
                        )
                        .map(node -> 1)
                        .reduce(0, Integer::sum, Integer::sum);

                newVarOccurrences
                        .computeIfAbsent(opGraph.getNode(), k -> new ArrayList<>())
                        .add(new Occurrence(SPARQLPositionType.GRAPH_NAME, count, ContextType.DATASET));

                SQLContext cont = context.setGraph(opGraph.getNode(), "sq")
                        .setVarOccurrences(newVarOccurrences)
                        .setTableIndex(count);

                if (opGraph.getSubOp() instanceof OpBGP) {
                    SQLQuery sqlQuery = buildSPARQLContext(opGraph.getSubOp(), cont);
                    yield new StSGraphOperator(sqlQuery)
                            .buildSQLQuery();
                } else {
                    // TODO : OpTable
                    yield null;
                }
            }
            case OpTriple opTriple -> {
//                context.getTriples().add(opTriple.getTriple());
//                return context.buildSQL();
                yield null;
            }
            default -> throw new IllegalArgumentException("TODO: Unknown operator " + op.getClass().getName());
        };
    }

    /**
     * Get the SELECT clause of the SQL query with the resource or literal table
     *
     * @param variables the variables of the SQL query
     * @param sqlQuery  the SQL query
     * @return the SELECT projections of the SQL query
     */
    private static String getSqlProjectionsQuery(List<Var> variables, SQLQuery sqlQuery) {
        Map<Node, List<Occurrence>> varOccurrences = sqlQuery.getContext().varOccurrences();
        return "SELECT " + variables.stream()
                .map((node) -> {
                    if (varOccurrences.get(node).stream().anyMatch(occurrence -> occurrence.getType() == SPARQLPositionType.GRAPH_NAME)) {
                        return sqlQuery.getContext().tableName() + sqlQuery.getContext().tableIndex() +
                                ".ng$" + node.getName() + ", " + sqlQuery.getContext().tableName() + sqlQuery.getContext().tableIndex() +
                                ".bs$" + node.getName();
                    } else {
                        return sqlQuery.getContext().tableName() + sqlQuery.getContext().tableIndex() +
                                ".v$" + node.getName();
                    }
                })
                .collect(Collectors.joining(", "));
    }

    /**
     * Collect the occurrences of the variables in the BGP
     *
     * @param opBGP   the current BGP
     * @param context the current SQL context
     * @return the modified SQL context
     */
    private SQLContext addURIsToContext(OpBGP opBGP, SQLContext context) {
        Map<Node, List<Occurrence>> newVarOccurrences = new HashMap<>(context.varOccurrences());
        ContextType contextType = context.graph() == null ? ContextType.WORKSPACE : ContextType.DATASET;

        for (int i = 0; i < opBGP.getPattern().getList().size(); i++) {
            Triple triple = opBGP.getPattern().getList().get(i);
            Node subject = triple.getSubject();
            Node predicate = triple.getPredicate();
            Node object = triple.getObject();

            newVarOccurrences.computeIfAbsent(subject, k -> new ArrayList<>())
                    .add(new Occurrence(SPARQLPositionType.SUBJECT, i, contextType));
            newVarOccurrences.computeIfAbsent(predicate, k -> new ArrayList<>())
                    .add(new Occurrence(SPARQLPositionType.PROPERTY, i, contextType));
            newVarOccurrences.computeIfAbsent(object, k -> new ArrayList<>())
                    .add(new Occurrence(SPARQLPositionType.OBJECT, i, contextType));

            context = context.setVarOccurrences(newVarOccurrences);
        }

        return context;
    }

    /**
     * Add URI to the idMap and retrieve ids later
     *
     * @param opBGP the current BGP
     */
    private void addURIsToMap(OpBGP opBGP) {
        for (int i = 0; i < opBGP.getPattern().getList().size(); i++) {
            Triple triple = opBGP.getPattern().getList().get(i);
            Node subject = triple.getSubject();
            Node predicate = triple.getPredicate();
            Node object = triple.getObject();

            if (subject instanceof Node_URI) {
                uriToIdMap.put(subject.getURI(), null);
            }
            if (predicate instanceof Node_URI) {
                uriToIdMap.put(predicate.getURI(), null);
            }
            if (object instanceof Node_URI) {
                uriToIdMap.put(object.getURI(), null);
            }
            if (object instanceof Node_Literal) {
                uriToIdMap.put(object.getLiteralLexicalForm(), null);
            }
        }
    }


    /**
     * Build the SQL query with the join
     *
     * @param leftSQLQuery  the left SQL query
     * @param rightSQLQuery the right SQL query
     * @return the SQL query with the joined terms
     */
    private SQLQuery buildSQLQueryJoin(SQLQuery leftSQLQuery, SQLQuery rightSQLQuery) {
        List<Node> commonNodesWithoutGraph = new ArrayList<>();
        Node graphLeftVariable = null;
        Node graphRightVariable = null;
        Node graphVariable = null;

        // build commonNodesWithoutGraph
        leftSQLQuery.getContext().varOccurrences().keySet().forEach(node -> {
            if (rightSQLQuery.getContext().varOccurrences().containsKey(node)
                    && rightSQLQuery.getContext().varOccurrences().get(node).stream()
                    .anyMatch(occurrence -> occurrence.getType() == SPARQLPositionType.GRAPH_NAME)) {
                commonNodesWithoutGraph.add(node);
            }
        });

        // for loop rightSQLQuery varOccurrences
        for (Node node : rightSQLQuery.getContext().varOccurrences().keySet()) {
            if (node instanceof Node_Variable && rightSQLQuery.getContext().varOccurrences().get(node).stream()
                    .anyMatch(occurrence -> occurrence.getType() == SPARQLPositionType.GRAPH_NAME)
            ) {
                graphRightVariable = node;
                graphVariable = node;
                break;
            }
        }

        // for loop leftSQLQuery varOccurrences
        for (Node node : leftSQLQuery.getContext().varOccurrences().keySet()) {
            if (node instanceof Node_Variable && leftSQLQuery.getContext().varOccurrences().get(node).stream()
                    .anyMatch(occurrence -> occurrence.getType() == SPARQLPositionType.GRAPH_NAME)
            ) {
                graphLeftVariable = node;
                graphVariable = node;
                break;
            }
        }

        String select = buildSelectVariablesWithoutGraph(leftSQLQuery.getContext(), rightSQLQuery.getContext());

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

        commonNodesWithoutGraph.forEach(node -> sqlClauseBuilder.and(
                new EqualToOperator()
                        .buildComparisonOperatorSQL(
                                leftSQLQuery.getContext().tableName() + leftSQLQuery.getContext().tableIndex() +
                                        ".v$" + node.getName(),
                                rightSQLQuery.getContext().tableName() + rightSQLQuery.getContext().tableIndex() +
                                        ".v$" + node.getName()
                        )
        ));

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

        Map<Node, List<Occurrence>> mergedOccurrences = mergeMapOccurrences(
                leftSQLQuery.getContext().varOccurrences(),
                rightSQLQuery.getContext().varOccurrences()
        );

        SQLContext context = new SQLContext(
                graphVariable,
                mergedOccurrences,
                "join",
                leftSQLQuery.getContext().tableIndex() + 1
        );
        return new SQLQuery(sql, context);
    }

    private String buildSelectVariablesWithoutGraph(SQLContext leftContext, SQLContext rightContext) {
        Set<String> leftSelect = leftContext.varOccurrences().keySet().stream()
                .filter(node -> node instanceof Node_Variable)
                .filter(node -> leftContext.graph() == null || !node.equals(leftContext.graph()))
                .map(node ->
                        ".v$" + node.getName()
                )
                .collect(Collectors.toSet());

        Set<String> rightSelect = rightContext.varOccurrences().keySet().stream()
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

    private Map<Node, List<Occurrence>> mergeMapOccurrences(
            Map<Node, List<Occurrence>> leftMapOccurrences,
            Map<Node, List<Occurrence>> rightMapOccurrences
    ) {
        Map<Node, List<Occurrence>> mergedOccurrences = new HashMap<>(leftMapOccurrences);

        rightMapOccurrences.forEach((node, occurrences) ->
                mergedOccurrences.computeIfAbsent(node, k -> new ArrayList<>()).addAll(occurrences)
        );

        return mergedOccurrences;
    }

    /**
     * Get the SELECT clause of the SQL query with the resource or literal table
     *
     * @param context the context of the SPARQL query
     * @return the SELECT clause of the SQL query
     */
    private String getSelectVariablesResourceOrLiteral(SQLContext context) {
        return Streams.mapWithIndex(context.varOccurrences().keySet().stream()
                .filter(node -> node instanceof Node_Variable), (node, index) -> (
                "rl" + index + ".name as name$" + node.getName() + ", rl" + index + ".type as type$" + node.getName()
        )).collect(Collectors.joining(", "));
    }


    private String getJoinVariablesResourceOrLiteral(SQLContext context) {
        return Streams.mapWithIndex(context.varOccurrences().keySet().stream()
                .filter(node -> node instanceof Node_Variable), (node, index) -> {
            if (context.varOccurrences().get(node).getFirst().getType() == SPARQLPositionType.GRAPH_NAME) {
                return (
                        " JOIN versioned_named_graph vng" + index + " ON " + context.tableName() +
                                context.tableIndex() + ".ng$" + node.getName() + " = vng" + index +
                                ".id_named_graph AND get_bit(" + context.tableName() + context.tableIndex() +
                                ".bs$" + node.getName() + ", vng" + index + ".index_version) = 1 \n" +
                                " JOIN resource_or_literal rl" + index + " ON vng" + index + ".id_versioned_named_graph = rl" +
                                index + ".id_resource_or_literal"
                );
            }
            return (
                    " JOIN resource_or_literal rl" + index + " ON " +
                            context.tableName() + context.tableIndex() + ".v$" + node.getName() +
                            " = rl" + index + ".id_resource_or_literal"
            );
        }).collect(Collectors.joining(" \n"));
    }

    /**
     * Fills the map of URIs with their IDs
     */
    private void getURIsIds() {
        if (!uriToIdMap.isEmpty()) {
            Transaction tx = session.beginTransaction();
            List<ResourceOrLiteral> resourceOrLiterals =
                    session.createSelectionQuery("from ResourceOrLiteral where name in :names", ResourceOrLiteral.class)
                            .setParameter("names", uriToIdMap.keySet())
                            .getResultList();
            tx.commit();

            resourceOrLiterals.forEach(resourceOrLiteral -> uriToIdMap.put(resourceOrLiteral.getName(), resourceOrLiteral.getIdResourceOrLiteral()));
        }
    }
}
