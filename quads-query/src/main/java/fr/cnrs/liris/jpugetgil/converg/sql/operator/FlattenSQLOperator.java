package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sql.*;
import fr.cnrs.liris.jpugetgil.converg.utils.Pair;
import org.apache.jena.graph.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FlattenSQLOperator extends SQLOperator {
    private final SQLQuery sqlQuery;
    private final SQLVariable joinedSQLVariable;

    public FlattenSQLOperator(SQLQuery sqlQuery, SQLVariable joinedSQLVariable) {
        this.sqlQuery = sqlQuery;
        this.joinedSQLVariable = joinedSQLVariable;
    }

    @Override
    public SQLQuery buildSQLQuery() {
        String select = buildSelect();
        String join = buildFrom();
        SQLContext newContext = flattenContext();

        return new SQLQuery(
                "SELECT " + select + " FROM (" + sqlQuery.getSql() + ") flatten_table " + join,
                newContext
        );
    }

    private SQLContext flattenContext() {
        Map<Node, List<SPARQLOccurrence>> newSPARQLOccurrences = new HashMap<>();

        sqlQuery.getContext().sparqlVarOccurrences().forEach((node, occurrences) -> {
            if (node.getName().equals(joinedSQLVariable.getSqlVarName())) {
                List<SPARQLOccurrence> sparqlOccurrences = new ArrayList<>();
                occurrences.forEach(occurrence -> {
                    if (occurrence.getSqlVariable().getSqlVarType() == SQLVarType.CONDENSED) {
                        newSPARQLOccurrences
                                .computeIfAbsent(node, k -> sparqlOccurrences)
                                .add(new SPARQLOccurrence(
                                        occurrence.getType(),
                                        occurrence.getPosition(),
                                        occurrence.getContextType(),
                                        new SQLVariable(SQLVarType.ID, occurrence.getSqlVariable().getSqlVarName())
                                ));
                    } else {
                        newSPARQLOccurrences.computeIfAbsent(node, k -> sparqlOccurrences).add(occurrence);
                    }
                });
            } else {
                newSPARQLOccurrences.computeIfAbsent(node, k -> new ArrayList<>()).addAll(occurrences);
            }
        });
        return sqlQuery.getContext()
                .setVarOccurrences(newSPARQLOccurrences);
    }

    @Override
    protected String buildSelect() {
        Map<Node, List<SPARQLOccurrence>> sparqlVarOccurrences = this.sqlQuery
                .getContext()
                .sparqlVarOccurrences();

        return sparqlVarOccurrences.keySet()
                .stream()
                .map(node -> {
                    SQLVariable sqlVar = SQLUtils.getMaxSQLVariableByOccurrences(sparqlVarOccurrences.get(node));

                    if (sqlVar.getSqlVarType() == SQLVarType.CONDENSED && sqlVar.getSqlVarName().equals(joinedSQLVariable.getSqlVarName())) {
                        return sqlVar.joinProjections(this.joinedSQLVariable, "flatten_table", "vng");
                    } else {
                        return sqlVar.getSelect("flatten_table");
                    }
                })
                .collect(Collectors.joining(", "));
    }

    /**
     * Get the join part of the query (flattening the graph variables)
     *
     * @return the flattened variables
     */
    @Override
    protected String buildFrom() {
        Map<Node, List<SPARQLOccurrence>> sparqlVarOccurrences = this.sqlQuery.getContext().sparqlVarOccurrences();
        return sparqlVarOccurrences.keySet()
                .stream()
                .filter(node -> node.getName().equals(joinedSQLVariable.getSqlVarName()))
                .map(node -> sparqlVarOccurrences.get(node).stream()
                        .map(sparqlOccurrence -> sparqlOccurrence.getSqlVariable()
                                .joinJoin(joinedSQLVariable, "flatten_table", "vng"))
                        .collect(Collectors.joining(" \n")))
                .collect(Collectors.joining(" \n"));
    }

    /**
     * @return
     */
    @Override
    protected String buildWhere() {
        return "";
    }
}
