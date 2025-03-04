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

    Node graph;

    public QuadPatternSQLOperator(OpQuadPattern opQuadPattern, SQLContext context) {
        this.opQuadPattern = opQuadPattern;
        this.context = context;
        this.graph = opQuadPattern.getGraphNode();
    }

    /**
     * @return the SQL query of the quad pattern
     */
    @Override
    public SQLQuery buildSQLQuery() {
        context = context
                .setVarOccurrences(createVarOccurrencesMap());

        String select = "SELECT " + buildSelect() + "\n";
        String from = "FROM " + buildFrom() + "\n";
        String where = buildWhere();

        String query = !where.isEmpty() ? select + from + "WHERE " + where : select + from;

        return new SQLQuery(
                query,
                context
        );
    }

    /**
     * @return the select part of a Quad pattern
     */
    @Override
    protected String buildSelect() {
        if (this.graph.isURI() && this.graph == Quad.defaultGraphNodeGenerated) {
            return generateSelectMetadata();
        } else if (this.graph.isVariable()) {
            return SQLUtils.intersectionValidity(opQuadPattern.getBasicPattern().size()) +
                    " as bs$" + this.graph.getName() + ", " + getSelectVariables();
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
        return opQuadPattern.getGraphNode().isURI() && opQuadPattern.getGraphNode() == Quad.defaultGraphNodeGenerated ?
                generateWhereMetadata() : generateWhere();
    }

    /**
     * Generate the SELECT clause of the SQL query in a metadata context
     *
     * @return the SELECT clause of the SQL query
     */
    private String generateSelectMetadata() {
        return Streams.mapWithIndex(context.sparqlVarOccurrences().keySet().stream()
                        .filter(Node_Variable.class::isInstance), (node, index) ->
                        "t" + context.sparqlVarOccurrences().get(node).getFirst().getPosition() +
                                "." + SQLUtils.getColumnByOccurrence(context.sparqlVarOccurrences().get(node).getFirst().getType()) +
                                " as v$" + node.getName()
                )
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

                return (
                        "t" + context.sparqlVarOccurrences().get(node).getFirst().getPosition() +
                                ".id_named_graph as ng$" + node.getName()
                );
            }

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

        if (this.graph.isVariable()) {
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
            switch (this.graph) {
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
                default -> throw new ARQException("Unexpected value: " + this.graph);
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

    private Map<Node, List<SPARQLOccurrence>> createVarOccurrencesMap() {
        Map<Node, List<SPARQLOccurrence>> newVarOccurrences = new HashMap<>();
        SPARQLContextType sparqlContextType = opQuadPattern.getGraphNode() == Quad.defaultGraphNodeGenerated ?
                SPARQLContextType.METADATA : SPARQLContextType.VERSIONED_DATA;

        if (sparqlContextType == SPARQLContextType.VERSIONED_DATA) {
            newVarOccurrences.computeIfAbsent(opQuadPattern.getGraphNode(), k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(
                            SPARQLPositionType.GRAPH_NAME,
                            0, sparqlContextType,
                            new SQLVariable(SQLVarType.CONDENSED, opQuadPattern.getGraphNode().getName())
                    ));
        }

        for (int i = 0; i < opQuadPattern.getPattern().getList().size(); i++) {
            Quad quad = opQuadPattern.getPattern().getList().get(i);
            Node subject = quad.getSubject();
            Node predicate = quad.getPredicate();
            Node object = quad.getObject();

            if (subject.isVariable()) {
                newVarOccurrences.computeIfAbsent(subject, k -> new ArrayList<>())
                        .add(new SPARQLOccurrence(
                                SPARQLPositionType.SUBJECT,
                                i, sparqlContextType,
                                new SQLVariable(SQLVarType.ID, subject.getName())
                        ));
            }
            if (predicate.isVariable()) {
                newVarOccurrences.computeIfAbsent(predicate, k -> new ArrayList<>())
                        .add(new SPARQLOccurrence(
                                SPARQLPositionType.PREDICATE,
                                i, sparqlContextType,
                                new SQLVariable(SQLVarType.ID, predicate.getName())
                        ));
            }
            if (object.isVariable()) {
                newVarOccurrences.computeIfAbsent(object, k -> new ArrayList<>())
                        .add(new SPARQLOccurrence(
                                SPARQLPositionType.OBJECT,
                                i, sparqlContextType,
                                new SQLVariable(SQLVarType.ID, object.getName())
                        ));
            }
        }

        return newVarOccurrences;
    }
}
