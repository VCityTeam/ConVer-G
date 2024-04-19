package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.SPARQLContextType;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.SPARQLPositionType;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.*;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.ast.Comparison;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.ast.Conjunction;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.ast.RawSQLCondition;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.comparison.EqualToOperator;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.comparison.NotEqualToOperator;
import org.apache.jena.graph.*;
import org.apache.jena.sparql.algebra.op.OpBGP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StSBGPOperator extends StSOperator {
    private final OpBGP op;
    private List<SQLVariable> sqlVariables = null;
    private Map<Node, List<SPARQLOccurrence>> varOccurrences = new HashMap<>();
//    private final SQLContext context;

    public StSBGPOperator(SQLContext context, OpBGP op) {
        super(context);
        this.op = op;
        collectVariableOccurrences();
    }

    @Override
    public SQLQuery buildSQLQuery() {
        if (context.graph() instanceof Node_Variable) {
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

    @Override
    public List<SQLVariable> getSQLVariables() {
        if (sqlVariables == null) {
            sqlVariables = varOccurrences.keySet()
                    .stream()
                    .filter(Node_Variable.class::isInstance)
                    .map(node -> new SQLVariable(SQLVarType.DATA, node.getName()))
                    .collect(Collectors.toCollection(ArrayList::new));
            if (context.graph() instanceof Node_Variable) {
                sqlVariables.add(new SQLVariable(SQLVarType.BIT_STRING, context.graph().getName()));
                sqlVariables.add(new SQLVariable(SQLVarType.GRAPH_NAME, context.graph().getName()));
            }
        }
        return sqlVariables;
    }

    /**
     * Generates a string containing SQL attributes for representing the context graph
     *
     * @return a string containing context graph attributes
     */
    private String generateSQLGraphSelect() {
        // TODO: manage corner cases where the graph variable appears in the bgp triples
        assert context.graph() instanceof Node_Variable;
        return intersectionValidity() + " as bs$" + context.graph().getName() + ", t0.id_named_graph as ng$" +
                context.graph().getName();
    }

    /**
     * Generate SQL string for attributes representing the value (i.e. the node id) of SPARQL data variables
     *
     * @return the SQL string to use in SELECT statement
     */
    private String generateSQLDataSelect() {
        return varOccurrences.entrySet().stream().filter(e -> e.getValue() instanceof Node_Variable).map(e -> {
            var node = e.getKey();
            var occ = e.getValue().getFirst();
            return occ.asSQL("t") + " as v$" + node.getName();
        }).collect(Collectors.joining(", \n"));
    }

    /**
     * Generate the SELECT clause of the SQL query
     *
     * @return the SELECT clause of the SQL query
     */
    private String generateSelect() {
        if (context.graph() instanceof Node_Variable) {
            return generateSQLGraphSelect() + generateSQLDataSelect();
        } else {
            return generateSQLDataSelect();
        }
    }

    /**
     * Generate the SELECT clause of the SQL query in a workspace context
     *
     * @return the SELECT clause of the SQL query
     */
    private String generateSelectWorkspace() {
        return generateSQLDataSelect();
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
                return (Schema.WK_TABLE+" t" + index);
            } else {
                return (Schema.VQ_TABLE+" t" + index);
            }
        }).collect(Collectors.joining(", "));
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
        where = new Conjunction().and(new RawSQLCondition(where))
                .and(generateDataVariableJoinConditions())
                .asSQL();
        if (!where.isEmpty()) {
            query += " WHERE " + where;
        }
//        SQLContext newContext = context.setSQLVariables(sqlVariables);
        return new SQLQuery(query, getSQLVariables());
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
                    new NotEqualToOperator().buildComparisonOperatorSQL("bit_count" + intersectionValidity(), "0"));
        }

        for (int i = 0; i < triples.size(); i++) {
            switch (context.graph()) {
                case Node_Variable ignored -> {
                    // where
                    if (i < triples.size() - 1) {
                        sqlClauseBuilder = sqlClauseBuilder.and(
                                new EqualToOperator().buildComparisonOperatorSQL("t" + i + ".id_named_graph",
                                        "t" + (i + 1) + ".id_named_graph"));
                    }
                }
                case Node_URI nodeUri -> {
                    // where
                    sqlClauseBuilder = sqlClauseBuilder.and(
                                    new EqualToOperator().buildComparisonOperatorSQL("t" + i + ".id_named_graph", """
                                            (
                                                SELECT vng.id_named_graph
                                                FROM versioned_named_graph vng JOIN resource_or_literal rl ON 
                                                vng.id_versioned_named_graph = rl.id_resource_or_literal
                                                WHERE rl.name = '""" + nodeUri.getURI() + "')"))
                            .and(new EqualToOperator().buildComparisonOperatorSQL("get_bit(t" + i + ".validity," + """
                                    (
                                        SELECT vng.index_version
                                        FROM versioned_named_graph vng JOIN resource_or_literal rl ON 
                                        vng.id_versioned_named_graph = rl.id_resource_or_literal
                                        WHERE rl.name = '""" + nodeUri.getURI() + "')" + ")", "1"));
                }
                default -> throw new IllegalStateException("Unexpected value: " + context.graph());
            }

            sqlClauseBuilder.and(buildFiltersOnIds(triples, i));
        }

        return sqlClauseBuilder.and(idSelect).build().clause;
    }

    private Conjunction generateDataVariableJoinConditions() {
        Conjunction conj = new Conjunction();
        varOccurrences.entrySet().stream().filter(e -> e.getValue() instanceof Node_Variable).forEach(e -> {
            var node = e.getKey();
            var occs = e.getValue();
            var first = occs.getFirst();
            for (int i = 1; i < occs.size(); i++) {
                conj.and(new Comparison(first.asSQL("t"), new EqualToOperator(), occs.get(i).asSQL("t")));
            }
        });
        return conj;
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
            sqlClauseBuilder.and(new EqualToOperator().buildComparisonOperatorSQL("t" + i + ".id_subject", """
                    (
                        SELECT id_resource_or_literal
                        FROM resource_or_literal
                        WHERE name = '""" + subject.getURI() + "')"));
        }
        if (predicate instanceof Node_URI) {
            sqlClauseBuilder.and(new EqualToOperator().buildComparisonOperatorSQL("t" + i + ".id_property", """
                    (
                        SELECT id_resource_or_literal
                        FROM resource_or_literal
                        WHERE name = '""" + predicate.getURI() + "')"));
        }
        if (object instanceof Node_URI) {
            sqlClauseBuilder.and(new EqualToOperator().buildComparisonOperatorSQL("t" + i + ".id_object", """
                    (
                        SELECT id_resource_or_literal
                        FROM resource_or_literal
                        WHERE name = '""" + object.getURI() + "')"));
        } else if (object instanceof Node_Literal) {
            sqlClauseBuilder.and(new EqualToOperator().buildComparisonOperatorSQL("t" + i + ".id_object", """
                    (
                        SELECT id_resource_or_literal
                        FROM resource_or_literal
                        WHERE name = '""" + object.getLiteralLexicalForm() + "' AND type IS NOT NULL)"));
        }

        return sqlClauseBuilder.build().clause;
    }

    private String intersectionValidity() {
        return "(" +
                Streams.mapWithIndex(op.getPattern().getList().stream(), (triple, index) -> "t" + index + ".validity")
                        .collect(Collectors.joining(" & ")) + ")";
    }

    /**
     * Collect the occurrences of the variables in the BGP
     *
     * @return the modified SQL context
     */
    private void collectVariableOccurrences() {
        SPARQLContextType sparqlContextType =
                context.graph() == null ? SPARQLContextType.WORKSPACE : SPARQLContextType.DATASET;

        for (int i = 0; i < op.getPattern().getList().size(); i++) {
            Triple triple = op.getPattern().getList().get(i);
            Node subject = triple.getSubject();
            Node predicate = triple.getPredicate();
            Node object = triple.getObject();

            varOccurrences.computeIfAbsent(subject, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(SPARQLPositionType.SUBJECT, i, sparqlContextType));
            varOccurrences.computeIfAbsent(predicate, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(SPARQLPositionType.PROPERTY, i, sparqlContextType));
            varOccurrences.computeIfAbsent(object, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(SPARQLPositionType.OBJECT, i, sparqlContextType));
        }
    }
}
