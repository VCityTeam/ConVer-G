package fr.cnrs.liris.jpugetgil.converg.sql;

import com.google.common.collect.Streams;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLPositionType;
import fr.cnrs.liris.jpugetgil.converg.sql.comparison.EqualToOperator;
import fr.cnrs.liris.jpugetgil.converg.utils.Pair;
import org.apache.jena.graph.*;
import org.apache.jena.sparql.ARQNotImplemented;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SQLUtils {


    public static String intersectionValidity(Integer size) {
        return "(" + Streams.mapWithIndex(IntStream.range(1, 1 + size), (triple, index) ->
                "t" + index + ".validity"
        ).collect(Collectors.joining(" & ")) + ")";
    }

    /**
     * Return the column name of the SQL query according to the occurrence type
     *
     * @param sparqlPositionType the sparql position type
     * @return the column name
     */
    public static String getColumnByOccurrence(SPARQLPositionType sparqlPositionType) {
        return switch (sparqlPositionType) {
            case SUBJECT -> "id_subject";
            case PREDICATE -> "id_predicate";
            case OBJECT -> "id_object";
            default ->
                    throw new ARQNotImplemented("Unexpected value: " + sparqlPositionType + " in getColumnByOccurrence");
        };
    }

    /**
     * Build the filters on the IDs of the triple
     *
     * @param triples the list of triples
     * @param i       the index of the current triple
     * @return the filters on the IDs of the triple
     */
    public static String buildFiltersOnIds(List<Triple> triples, int i) {
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
                                    "t" + i + ".id_predicate",
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

    /**
     * Get the equalities between the variables in var occurrences
     *
     * @param sqlClauseBuilder     the SQL clause builder
     * @param sparqlVarOccurrences the var occurrences
     */
    public static void getEqualitiesBGP(SQLClause.SQLClauseBuilder sqlClauseBuilder, Map<Node, List<SPARQLOccurrence>> sparqlVarOccurrences) {
        for (Node node : sparqlVarOccurrences.keySet()) {
            if (node instanceof Node_Variable && sparqlVarOccurrences.get(node).size() > 1) {
                for (int i = 1; i < sparqlVarOccurrences.get(node).size(); i++) {
                    sqlClauseBuilder.and(
                            "t" + sparqlVarOccurrences.get(node).get(i - 1).getPosition() + "." +
                                    SQLUtils.getColumnByOccurrence(sparqlVarOccurrences.get(node).get(i - 1).getType()) +
                                    " = t" + sparqlVarOccurrences.get(node).get(i).getPosition() + "." +
                                    SQLUtils.getColumnByOccurrence(sparqlVarOccurrences.get(node).get(i).getType())
                    );
                }
            }
        }
    }

    /**
     * Build the common variables between two maps of occurrences
     *
     * @param leftNodeListMap  the left node list map
     * @param rightNodeListMap the right node list map
     * @return the list of common variables
     */
    public static List<Pair<SQLVariable, SQLVariable>> buildCommonsVariables(
            Map<Node, List<SPARQLOccurrence>> leftNodeListMap,
            Map<Node, List<SPARQLOccurrence>> rightNodeListMap
    ) {
        List<Pair<SQLVariable, SQLVariable>> sqlJoinedVars = new ArrayList<>();

        leftNodeListMap
                .forEach((leftNode, leftSPARQLOccurrence) -> {
                    if (rightNodeListMap.containsKey(leftNode)) {
                        List<SPARQLOccurrence> rightSPARQLOccurrence = rightNodeListMap.get(leftNode);

                        var rightMaxSPARQLOcc = rightSPARQLOccurrence
                                .stream()
                                .max(Comparator.comparingInt(o -> o.getSqlVariable().getSqlVarType().level))
                                .orElseThrow();
                        var leftMaxSPARQLOcc = leftSPARQLOccurrence
                                .stream()
                                .max(Comparator.comparingInt(o -> o.getSqlVariable().getSqlVarType().level))
                                .orElseThrow();

                        sqlJoinedVars.add(new Pair<>(leftMaxSPARQLOcc.getSqlVariable(), rightMaxSPARQLOcc.getSqlVariable()));
                    }
                });

        return sqlJoinedVars;
    }

    /**
     * Get all condensed variables from the map occurrences
     *
     * @param mapOccurrences the map occurrences
     * @return the list of condensed variables
     */
    public static List<SQLVariable> getAllCondensedVariables(Map<Node, List<SPARQLOccurrence>> mapOccurrences) {
        List<SQLVariable> condensedVariables = new ArrayList<>();

        mapOccurrences.forEach(((node, sparqlOccurrences) -> {
            for (SPARQLOccurrence sparqlOccurrence : sparqlOccurrences) {
                if (sparqlOccurrence.getSqlVariable().getSqlVarType() == SQLVarType.CONDENSED) {
                    condensedVariables.add(sparqlOccurrence.getSqlVariable());
                }
            }
        }));

        return condensedVariables;
    }

    /**
     * Merge two maps of occurrences
     *
     * @param leftMapOccurrences  the left map occurrences
     * @param rightMapOccurrences the right map occurrences
     * @return the merged map occurrences
     */
    public static Map<Node, List<SPARQLOccurrence>> mergeMapOccurrences(
            Map<Node, List<SPARQLOccurrence>> leftMapOccurrences,
            Map<Node, List<SPARQLOccurrence>> rightMapOccurrences
    ) {
        Map<Node, List<SPARQLOccurrence>> mergedOccurrences = new HashMap<>();

        leftMapOccurrences.forEach((node, occurrences) ->
                mergedOccurrences.computeIfAbsent(node, k -> new ArrayList<>()).addAll(occurrences)
        );

        rightMapOccurrences.forEach((node, occurrences) ->
                mergedOccurrences.computeIfAbsent(node, k -> new ArrayList<>()).addAll(occurrences)
        );

        return mergedOccurrences;
    }

    /**
     * Get the max (representation) SQL variable by occurrences
     *
     * @param sparqlOccurrences the sparql occurrences
     * @return the max representation SQL variable
     */
    public static SQLVariable getMaxSQLVariableByOccurrences(
            List<SPARQLOccurrence> sparqlOccurrences
    ) {
        return sparqlOccurrences.stream()
                .max(Comparator.comparingInt((SPARQLOccurrence so) -> so.getSqlVariable().getSqlVarType().level)
                ).orElseThrow().getSqlVariable();
    }

    /**
     * Generate the projection of the SQL query according to the occurrences
     *
     * @param leftSparqlOccurrences  the left sparql occurrences
     * @param rightSparqlOccurrences the right sparql occurrences
     * @return the projection of the SQL query
     */
    public static String generateNodeProjectionByListSPARQLOccurrences(
            List<SPARQLOccurrence> leftSparqlOccurrences,
            List<SPARQLOccurrence> rightSparqlOccurrences
    ) {

        boolean presentLeft = leftSparqlOccurrences != null;
        boolean presentRight = rightSparqlOccurrences != null;

        if (presentLeft && presentRight) {
            SQLVariable leftSQLVariable = SQLUtils.getMaxSQLVariableByOccurrences(leftSparqlOccurrences);
            SQLVariable rightSQLVariable = SQLUtils.getMaxSQLVariableByOccurrences(rightSparqlOccurrences);
            return leftSQLVariable.joinProjections(rightSQLVariable, "left_table", "right_table");
        } else if (presentLeft) {
            SQLVariable leftSQLVariable = SQLUtils.getMaxSQLVariableByOccurrences(leftSparqlOccurrences);

            return leftSQLVariable.getSelect("left_table");
        }

        assert rightSparqlOccurrences != null;
        SQLVariable rightSQLVariable = SQLUtils.getMaxSQLVariableByOccurrences(rightSparqlOccurrences);

        return rightSQLVariable.getSelect("right_table");
    }
}
