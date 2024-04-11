package fr.cnrs.liris.jpugetgil.sparqltosql.sql.context;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;
import fr.cnrs.liris.jpugetgil.sparqltosql.Occurrence;
import fr.cnrs.liris.jpugetgil.sparqltosql.SPARQLPositionType;
import fr.cnrs.liris.jpugetgil.sparqltosql.SQLContext;
import fr.cnrs.liris.jpugetgil.sparqltosql.SQLQuery;
import fr.cnrs.liris.jpugetgil.sparqltosql.dao.VersionedNamedGraph;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLClause;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator.EqualToOperator;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator.NotEqualToOperator;
import org.apache.jena.graph.*;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StSBGPOperator extends StSOperator {
    private OpBGP op;

    private SQLContext context;

    private final Map<String, Integer> uriToIdMap;

    // remove later
    private final Session session;


    public StSBGPOperator(OpBGP op, SQLContext context, Map<String, Integer> uriToIdMap, Session session) {
        this.op = op;
        this.context = context;
        this.uriToIdMap = uriToIdMap;
        this.session = session;
    }

    @Override
    public SQLQuery buildSQLQuery() {
        if (this.context.graph() != null && this.context.graph() instanceof Node_Variable) {
            return buildContextBGPWithGraph();
        } else if (this.context.graph() != null && this.context.graph() instanceof Node_URI) {
            return buildContextBGPWithGraphURI();
        } else {
            return buildContextBGPWorkspace();
        }
    }

    /**
     * Build the SQL query in a graph context
     *
     * @return the SQL query
     */
    private SQLQuery buildContextBGPWithGraph() {
        String select = generateSelect();
        String tables = generateFromTables(false);
        return getSqlProjectionsQuery(select, tables, false);
    }

    /**
     * Build the SQL query in a URI graph context
     *
     * @return the built SQL query
     */
    private SQLQuery buildContextBGPWithGraphURI() {
        String select = generateSelect();
        String tables = generateFromTables(false);
        return getSqlProjectionsQuery(select, tables, false);
    }

    /**
     * Build the SQL query in a workspace context (no graph)
     *
     * @return the SQL query
     */
    private SQLQuery buildContextBGPWorkspace() {
        String select = generateSelectWorkspace();
        String tables = generateFromTables(true);
        return getSqlProjectionsQuery(select, tables, true);
    }

    /**
     * Generate the SELECT clause of the SQL query
     *
     * @return the SELECT clause of the SQL query
     */
    private String generateSelect() {
        if (context.graph() instanceof Node_Variable) {
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
        return Streams.mapWithIndex(context.varOccurrences().keySet().stream()
                .filter(node -> node instanceof Node_Variable), (node, index) ->
                "t" + context.varOccurrences().get(node).getFirst().getPosition() +
                        "." + getColumnByOccurrence(context.varOccurrences().get(node).getFirst()) +
                        " as v$" + node.getName()
        ).collect(Collectors.joining(", \n"));
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
        return Streams.mapWithIndex(context.varOccurrences().keySet().stream()
                .filter(node -> node instanceof Node_Variable), (node, index) -> {
            if (context.varOccurrences().get(node).getFirst().getType() == SPARQLPositionType.GRAPH_NAME) {
                return (
                        "t" + context.varOccurrences().get(node).getFirst().getPosition() +
                                ".id_named_graph as ng$" + node.getName()
                );
            }
            return (
                    "t" + context.varOccurrences().get(node).getFirst().getPosition() + "." +
                            getColumnByOccurrence(context.varOccurrences().get(node).getFirst()) +
                            " as v$" + node.getName()
            );
        }).collect(Collectors.joining(", \n"));
    }

    /**
     * Return the column name of the SQL query according to the occurrence type
     *
     * @param occurrence the occurrence of the Node
     * @return the column name of the versioned quad table
     */
    private String getColumnByOccurrence(Occurrence occurrence) {
        return switch (occurrence.getType()) {
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

        return new SQLQuery(query, context);
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
                    VersionedNamedGraph versionedNamedGraph = getAssociatedVNG(nodeUri.getURI());
                    sqlClauseBuilder = sqlClauseBuilder.and(
                            new EqualToOperator().buildComparisonOperatorSQL(
                                    "t" + i + ".id_named_graph",
                                    String.valueOf(versionedNamedGraph.getIdNamedGraph())
                            )
                    ).and(
                            new EqualToOperator()
                                    .buildComparisonOperatorSQL(
                                            "get_bit(t" + i + ".validity," + versionedNamedGraph.getIndex() + ")",
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
                                    String.valueOf(uriToIdMap.get(subject.getURI()))
                            )
            );
        }
        if (predicate instanceof Node_URI) {
            sqlClauseBuilder.and(
                    new EqualToOperator()
                            .buildComparisonOperatorSQL(
                                    "t" + i + ".id_property",
                                    String.valueOf(uriToIdMap.get(predicate.getURI()))
                            )
            );
        }
        if (object instanceof Node_URI) {
            sqlClauseBuilder.and(
                    new EqualToOperator().buildComparisonOperatorSQL(
                            "t" + i + ".id_object",
                            String.valueOf(uriToIdMap.get(object.getURI()))
                    )
            );
        } else if (object instanceof Node_Literal) {
            sqlClauseBuilder.and(
                    new EqualToOperator()
                            .buildComparisonOperatorSQL(
                                    "t" + i + ".id_object",
                                    String.valueOf(uriToIdMap.get(object.getLiteralLexicalForm()))
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

    private VersionedNamedGraph getAssociatedVNG(String uri) {
        Transaction tx = session.beginTransaction();
        VersionedNamedGraph versionedNamedGraph = session.createQuery(
                        "from VersionedNamedGraph vng join ResourceOrLiteral rl " +
                                "on vng.idVersionedNamedGraph = rl.idResourceOrLiteral where rl.name = :uri",
                        VersionedNamedGraph.class
                )
                .setParameter("uri", uri)
                .getSingleResult();
        tx.commit();

        return versionedNamedGraph;
    }
}
