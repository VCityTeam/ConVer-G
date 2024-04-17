package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.SPARQLPositionType;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.*;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.comparison.EqualToOperator;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.comparison.NotEqualToOperator;
import org.apache.jena.graph.*;
import org.apache.jena.sparql.algebra.op.OpBGP;

import java.util.List;
import java.util.stream.Collectors;

public class StSBGPOperator extends StSOperator {
    private final OpBGP op;

    private final SQLContext context;

    public StSBGPOperator(OpBGP op, SQLContext context) {
        this.op = op;
        this.context = context;
    }

    @Override
    public SQLQuery buildSQLQuery() {
        if (this.context.graph() instanceof Node_Variable) {
            String select = generateSelect();
            String tables = generateFromTables(false);
            return getSqlProjectionsQuery(select, tables, false);
        } else if (this.context.graph() instanceof Node_URI) {
            String select = generateSelect();
            String tables = generateFromTables(false);
            return getSqlProjectionsQuery(select, tables, false);
        } else {
            String select = generateSelectWorkspace();
            String tables = generateFromTables(true);
            return getSqlProjectionsQuery(select, tables, true);
        }
    }

    /**
     * Generate the SELECT clause of the SQL query
     *
     * @return the SELECT clause of the SQL query
     */
    private String generateSelect() {
        if (context.graph() instanceof Node_Variable) {
            this.sqlVariables.add(new SQLVariable(SQLVarType.BIT_STRING, context.graph().getName()));
            return intersectionValidity() + " as bs$" + context.graph().getName() + ", " + getSelectVariables();
        } else {
            return getSelectVariables();
        }
    }

    /**
     * Generate the SELECT clause of the SQL query in a workspace context
     *
     * @return the SELECT clause of the SQL query
     */
    private String generateSelectWorkspace() {
        return Streams.mapWithIndex(context.sparqlVarOccurrences().keySet().stream()
                        .filter(Node_Variable.class::isInstance), (node, index) -> {
                    this.sqlVariables.add(new SQLVariable(SQLVarType.DATA, node.getName()));

                    return "t" + context.sparqlVarOccurrences().get(node).getFirst().getPosition() +
                            "." + getColumnByOccurrence(context.sparqlVarOccurrences().get(node).getFirst()) +
                            " as v$" + node.getName();
                })
                .collect(Collectors.joining(", \n"));
    }

    /**
     * Generate the FROM clause of the SQL query
     *
     * @param isWorkspace true if the query is in a workspace context, false otherwise
     * @return the FROM clause of the SQL query
     */
    private String generateFromTables(boolean isWorkspace) {
        return Streams.mapWithIndex(op.getPattern().getList().stream(), (triple, index) -> {
            if (isWorkspace) {
                return ("workspace t" + index);
            } else {
                return ("versioned_quad t" + index);
            }
        }).collect(Collectors.joining(", "));
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
                this.sqlVariables.add(new SQLVariable(SQLVarType.GRAPH_NAME, node.getName()));

                return (
                        "t" + context.sparqlVarOccurrences().get(node).getFirst().getPosition() +
                                ".id_named_graph as ng$" + node.getName()
                );
            }

            this.sqlVariables.add(new SQLVariable(SQLVarType.DATA, node.getName()));
            return (
                    "t" + context.sparqlVarOccurrences().get(node).getFirst().getPosition() + "." +
                            getColumnByOccurrence(context.sparqlVarOccurrences().get(node).getFirst()) +
                            " as v$" + node.getName()
            );
        }).collect(Collectors.joining(", \n"));
    }

    /**
     * Return the column name of the SQL query according to the occurrence type
     *
     * @param sparqlOccurrence the occurrence of the Node
     * @return the column name of the versioned quad table
     */
    private String getColumnByOccurrence(SPARQLOccurrence sparqlOccurrence) {
        return switch (sparqlOccurrence.getType()) {
            case SUBJECT -> "id_subject";
            case PROPERTY -> "id_property";
            case OBJECT -> "id_object";
            default -> throw new IllegalArgumentException();
        };
    }

    /**
     * Get the SQL query
     *
     * @param select      the SELECT clause of the SQL query
     * @param tables      the FROM clause of the SQL query
     * @param isWorkspace true if the query is in a workspace context, false otherwise
     * @return the SQL query
     */
    private SQLQuery getSqlProjectionsQuery(String select, String tables, boolean isWorkspace) {
        String query = "SELECT " + select + " FROM " + tables;

        String where = isWorkspace ? generateWhereWorkspace() : generateWhere();
        if (!where.isEmpty()) {
            query += " WHERE " + where;
        }

        SQLContext newContext = context.setSQLVariables(sqlVariables);

        return new SQLQuery(query, newContext);
    }

    /**
     * Generate the WHERE clause of the SQL query in a workspace context
     *
     * @return the WHERE clause of the SQL query
     */
    private String generateWhereWorkspace() {
        SQLClause.SQLClauseBuilder sqlClauseBuilder = new SQLClause.SQLClauseBuilder();
        List<Triple> triples = op.getPattern().getList();

        for (int i = 0; i < triples.size(); i++) {
            sqlClauseBuilder.and(buildFiltersOnIds(triples, i));
        }

        return sqlClauseBuilder.build().clause;
    }

    /**
     * Generate the WHERE clause of the SQL query with a graph variable
     *
     * @return the WHERE clause of the SQL query
     */
    private String generateWhere() {
        SQLClause.SQLClauseBuilder sqlClauseBuilder = new SQLClause.SQLClauseBuilder();
        String idSelect = "";
        List<Triple> triples = op.getPattern().getList();

        if (context.graph() instanceof Node_Variable) {
            sqlClauseBuilder = sqlClauseBuilder.and(
                    new NotEqualToOperator()
                            .buildComparisonOperatorSQL(
                                    "bit_count" + intersectionValidity(),
                                    "0"
                            )
            );
        }

        for (int i = 0; i < triples.size(); i++) {
            switch (context.graph()) {
                case Node_Variable ignored -> {
                    // where
                    if (i < triples.size() - 1) {
                        sqlClauseBuilder = sqlClauseBuilder.and(
                                new EqualToOperator()
                                        .buildComparisonOperatorSQL(
                                                "t" + i + ".id_named_graph",
                                                "t" + (i + 1) + ".id_named_graph")
                        );
                    }
                }
                case Node_URI nodeUri -> {
                    // where
                    sqlClauseBuilder = sqlClauseBuilder.and(
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
                                                                SELECT vng.index_version
                                                                FROM versioned_named_graph vng JOIN resource_or_literal rl ON 
                                                                vng.id_versioned_named_graph = rl.id_resource_or_literal
                                                                WHERE rl.name = '""" + nodeUri.getURI() + "')"
                                                    + ")",
                                            "1"
                                    )
                    );
                }
                default -> throw new IllegalStateException("Unexpected value: " + context.graph());
            }

            sqlClauseBuilder.and(buildFiltersOnIds(triples, i));
        }

        return sqlClauseBuilder.and(idSelect).build().clause;
    }

    /**
     * Build the filters on the IDs of the triple
     *
     * @param triples the list of triples
     * @param i       the index of the current triple
     * @return the filters on the IDs of the triple
     */
    private String buildFiltersOnIds(List<Triple> triples, int i) {
        SQLClause.SQLClauseBuilder sqlClauseBuilder = new SQLClause.SQLClauseBuilder();
        Node subject = triples.get(i).getSubject();
        Node predicate = triples.get(i).getPredicate();
        Node object = triples.get(i).getObject();

        if (subject instanceof Node_URI) {
            sqlClauseBuilder.and(
                    new EqualToOperator()
                            .buildComparisonOperatorSQL(
                                    "t" + i + ".id_subject",
                                    """
                                            (
                                                SELECT id_resource_or_literal
                                                FROM resource_or_literal
                                                WHERE name = '""" + subject.getURI() + "')"
                            )
            );
        }
        if (predicate instanceof Node_URI) {
            sqlClauseBuilder.and(
                    new EqualToOperator()
                            .buildComparisonOperatorSQL(
                                    "t" + i + ".id_property",
                                    """
                                            (
                                                SELECT id_resource_or_literal
                                                FROM resource_or_literal
                                                WHERE name = '""" + predicate.getURI() + "')"
                            )
            );
        }
        if (object instanceof Node_URI) {
            sqlClauseBuilder.and(
                    new EqualToOperator()
                            .buildComparisonOperatorSQL(
                                    "t" + i + ".id_object",
                                    """
                                            (
                                                SELECT id_resource_or_literal
                                                FROM resource_or_literal
                                                WHERE name = '""" + object.getURI() + "')"
                            )
            );
        } else if (object instanceof Node_Literal) {
            sqlClauseBuilder.and(
                    new EqualToOperator()
                            .buildComparisonOperatorSQL(
                                    "t" + i + ".id_object",
                                    """
                                            (
                                                SELECT id_resource_or_literal
                                                FROM resource_or_literal
                                                WHERE name = '""" + object.getLiteralLexicalForm() + "' AND type IS NOT NULL)"
                            )
            );
        }

        return sqlClauseBuilder.build().clause;
    }

    private String intersectionValidity() {
        return "(" + Streams.mapWithIndex(op.getPattern().getList().stream(), (triple, index) ->
                "t" + index + ".validity"
        ).collect(Collectors.joining(" & ")) + ")";
    }
}
