package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import com.google.common.collect.Streams;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLContextType;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLPositionType;
import fr.cnrs.liris.jpugetgil.converg.sql.*;
import fr.cnrs.liris.jpugetgil.converg.sql.comparison.EqualToOperator;
import fr.cnrs.liris.jpugetgil.converg.sql.comparison.NotEqualToOperator;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Node_Variable;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.ARQException;
import org.apache.jena.sparql.algebra.op.OpQuadPattern;
import org.apache.jena.sparql.core.Quad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuadPatternSQLOperator extends SQLOperator {

    OpQuadPattern opQuadPattern;

    SQLContext context;

    public QuadPatternSQLOperator(OpQuadPattern opQuadPattern, SQLContext context) {
        this.opQuadPattern = opQuadPattern;
        this.context = context;
    }

    /**
     * @return the SQL query of the quad pattern
     */
    @Override
    public SQLQuery buildSQLQuery() {
        context = context
                .setGraph(opQuadPattern.getGraphNode())
                .setVarOccurrences(createVarOccurrencesMap());
        context = context
                .setSQLVariables(createSQLVariables());

        String select = "SELECT " + buildSelect() + "\n";
        String from = "FROM " + buildFrom() + "\n";
        String where = "WHERE " + buildWhere();

        return new SQLQuery(
                select + from + where,
                context
        );
    }

    /**
     * @return the select part of a Quad pattern
     */
    @Override
    protected String buildSelect() {
        if (context.graph() == Quad.defaultGraphNodeGenerated) {
            return generateSelectMetadata();
        } else if (context.graph() instanceof Node_Variable) {
            return SQLUtils.intersectionValidity(opQuadPattern.getBasicPattern().size()) +
                    " as bs$" + context.graph().getName() + ", " + getSelectVariables();
        } else {
            return getSelectVariables();
        }
    }

    /**
     * @return the from part of a Quad pattern
     */
    @Override
    protected String buildFrom() {
        return Streams.mapWithIndex(opQuadPattern.getPattern().getList().stream(), (quad, index) -> {
            if (quad.isDefaultGraphGenerated()) {
                return ("metadata t" + index);
            } else {
                return ("versioned_quad t" + index);
            }
        }).collect(Collectors.joining(", "));
    }

    /**
     * @return the where part of a Quad pattern
     */
    @Override
    protected String buildWhere() {
        return opQuadPattern.getGraphNode() == Quad.defaultGraphNodeGenerated ?
                generateWhereMetadata() : generateWhere();
    }

    /**
     * Generate the SELECT clause of the SQL query in a metadata context
     *
     * @return the SELECT clause of the SQL query
     */
    private String generateSelectMetadata() {
        return Streams.mapWithIndex(context.sparqlVarOccurrences().keySet().stream()
                        .filter(Node_Variable.class::isInstance), (node, index) -> {
//                    this.sqlVariables.add(new SQLVariable(SQLVarType.ID, node.getName()));

                    return "t" + context.sparqlVarOccurrences().get(node).getFirst().getPosition() +
                            "." + SQLUtils.getColumnByOccurrence(context.sparqlVarOccurrences().get(node).getFirst().getType()) +
                            " as v$" + node.getName();
                })
                .collect(Collectors.joining(", "));
    }

    /**
     * Get the SELECT clause of the SQL query
     *
     * @return the SELECT clause of the SQL query
     */
    private String getSelectVariables() {
        return Streams.mapWithIndex(context.sparqlVarOccurrences().keySet().stream()
                .filter(Node_Variable.class::isInstance), (node, index) -> {
            if (context.sparqlVarOccurrences().get(node).getFirst().getType() == SPARQLPositionType.GRAPH_NAME) {
//                this.sqlVariables.add(new SQLVariable(SQLVarType.CONDENSED, node.getName()));

                return (
                        "t" + context.sparqlVarOccurrences().get(node).getFirst().getPosition() +
                                ".id_named_graph as ng$" + node.getName()
                );
            }

//            this.sqlVariables.add(new SQLVariable(SQLVarType.ID, node.getName()));
            return (
                    "t" + context.sparqlVarOccurrences().get(node).getFirst().getPosition() + "." +
                            SQLUtils.getColumnByOccurrence(context.sparqlVarOccurrences().get(node).getFirst().getType()) +
                            " as v$" + node.getName()
            );
        }).collect(Collectors.joining(", "));
    }

    /**
     * Generate the WHERE clause of the SQL query with a graph variable
     *
     * @return the WHERE clause of the SQL query
     */
    private String generateWhere() {
        SQLClause.SQLClauseBuilder sqlClauseBuilder = new SQLClause.SQLClauseBuilder();
        List<Triple> triples = opQuadPattern.getBasicPattern().getList();

        if (context.graph() instanceof Node_Variable) {
            sqlClauseBuilder = sqlClauseBuilder.and(
                    new NotEqualToOperator()
                            .buildComparisonOperatorSQL(
                                    "bit_count" + SQLUtils.intersectionValidity(opQuadPattern.getBasicPattern().size()),
                                    "0"
                            )
            );
        }

        SQLUtils.getEqualitiesBGP(sqlClauseBuilder, context.sparqlVarOccurrences());

        for (int i = 0; i < triples.size(); i++) {
            switch (context.graph()) {
                case Node_Variable ignored -> {
                    if (i < triples.size() - 1) {
                        sqlClauseBuilder = sqlClauseBuilder.and(
                                new EqualToOperator()
                                        .buildComparisonOperatorSQL(
                                                "t" + i + ".id_named_graph",
                                                "t" + (i + 1) + ".id_named_graph")
                        );
                    }
                }
                case Node_URI nodeUri -> sqlClauseBuilder = sqlClauseBuilder.and(
                        new EqualToOperator().buildComparisonOperatorSQL(
                                "t" + i + ".id_named_graph",
                                """
                                        (
                                            SELECT vng.id_named_graph
                                            FROM versioned_named_graph vng JOIN resource_or_literal rl ON
                                            vng.id_versioned_named_graph = rl.id_resource_or_literal
                                            WHERE rl.name = '""" + nodeUri.getURI() + "')"
                        )
                ).and(
                        new EqualToOperator()
                                .buildComparisonOperatorSQL(
                                        "get_bit(t" + i + ".validity," +
                                                """
                                                        (
                                                            SELECT vng.index_version - 1
                                                            FROM versioned_named_graph vng JOIN resource_or_literal rl ON
                                                            vng.id_versioned_named_graph = rl.id_resource_or_literal
                                                            WHERE rl.name = '""" + nodeUri.getURI() + "')"
                                                + ")",
                                        "1"
                                )
                );
                default -> throw new ARQException("Unexpected value: " + context.graph());
            }

            sqlClauseBuilder.and(SQLUtils.buildFiltersOnIds(triples, i));
        }

        return sqlClauseBuilder.build().clause;
    }

    private String generateWhereMetadata() {
        SQLClause.SQLClauseBuilder sqlClauseBuilder = new SQLClause.SQLClauseBuilder();
        List<Triple> triples = opQuadPattern.getBasicPattern().getList();

        SQLUtils.getEqualitiesBGP(sqlClauseBuilder, context.sparqlVarOccurrences());

        for (int i = 0; i < triples.size(); i++) {
            sqlClauseBuilder.and(SQLUtils.buildFiltersOnIds(triples, i));
        }

        return sqlClauseBuilder.build().clause;
    }

    private List<SQLVariable> createSQLVariables() {
        List<SQLVariable> sqlVars = new ArrayList<>();
        context.sparqlVarOccurrences().keySet().stream()
                .filter(Node_Variable.class::isInstance)
                .forEach(node -> {
                    if (context.sparqlVarOccurrences().get(node).getFirst().getType() == SPARQLPositionType.GRAPH_NAME) {
                        sqlVars.add(new SQLVariable(SQLVarType.CONDENSED, node.getName()));
                    } else {
                        sqlVars.add(new SQLVariable(SQLVarType.ID, node.getName()));
                    }
                });
        return sqlVars;
    }

    private Map<Node, List<SPARQLOccurrence>> createVarOccurrencesMap() {
        Map<Node, List<SPARQLOccurrence>> newVarOccurrences = new HashMap<>();
        SPARQLContextType sparqlContextType = opQuadPattern.getGraphNode() == Quad.defaultGraphNodeGenerated ?
                SPARQLContextType.METADATA : SPARQLContextType.VERSIONED_DATA;

        newVarOccurrences.computeIfAbsent(opQuadPattern.getGraphNode(), k -> new ArrayList<>())
                .add(new SPARQLOccurrence(SPARQLPositionType.GRAPH_NAME, 0, sparqlContextType));

        for (int i = 0; i < opQuadPattern.getPattern().getList().size(); i++) {
            Quad quad = opQuadPattern.getPattern().getList().get(i);
            Node subject = quad.getSubject();
            Node predicate = quad.getPredicate();
            Node object = quad.getObject();

            newVarOccurrences.computeIfAbsent(subject, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(SPARQLPositionType.SUBJECT, i, sparqlContextType));
            newVarOccurrences.computeIfAbsent(predicate, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(SPARQLPositionType.PREDICATE, i, sparqlContextType));
            newVarOccurrences.computeIfAbsent(object, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(SPARQLPositionType.OBJECT, i, sparqlContextType));
        }

        return newVarOccurrences;
    }
}
