package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sql.*;
import org.apache.jena.graph.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IdentifySQLOperator extends SQLOperator {
    private final SQLQuery sqlQuery;
    private final SQLVariable identifiedVariable;
    private final String IDENTIFY_TABLE_NAME = "flatten_table";

    public IdentifySQLOperator(SQLQuery sqlQuery, SQLVariable identifiedVariable) {
        this.sqlQuery = sqlQuery;
        this.identifiedVariable = identifiedVariable;
    }

    @Override
    public SQLQuery buildSQLQuery() {
        String select = buildSelect();
        String join = buildFrom();
        SQLContext newContext = identifyContext();

        return new SQLQuery(
                "SELECT " + select + " FROM (" + sqlQuery.getSql() + ") " + IDENTIFY_TABLE_NAME + " " + join,
                newContext
        );
    }

    @Override
    protected String buildSelect() {
        Map<Node, List<SPARQLOccurrence>> sparqlVarOccurrences = this.sqlQuery
                .getContext()
                .sparqlVarOccurrences();

        return sparqlVarOccurrences.keySet()
                .stream()
                .map(node -> {
                    SQLVariable maxVar = SQLUtils.getMaxSQLVariableByOccurrences(sparqlVarOccurrences.get(node));

                    if (maxVar.getSqlVarName().equals(identifiedVariable.getSqlVarName()) && maxVar.getSqlVarType() == SQLVarType.ID) {
                        return maxVar.getSelectIdentifyVariable();
                    } else {
                        return maxVar.getSelect(IDENTIFY_TABLE_NAME);
                    }
                })
                .collect(Collectors.joining(", "));
    }


    @Override
    protected String buildFrom() {
        Map<Node, List<SPARQLOccurrence>> sparqlVarOccurrences = this.sqlQuery.getContext().sparqlVarOccurrences();
        return sparqlVarOccurrences.keySet()
                .stream()
                .filter(node -> node.getName().equals(identifiedVariable.getSqlVarName()))
                .map(node -> sparqlVarOccurrences.get(node).stream()
                        .map(sparqlOccurrence -> sparqlOccurrence.getSqlVariable()
                                .fromIdentifiedVariable(IDENTIFY_TABLE_NAME))
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

    /**
     * Identify the variable in the context of the SQL query
     *
     * @return the new context where the variable is identified
     */
    private SQLContext identifyContext() {
        Map<Node, List<SPARQLOccurrence>> newSPARQLOccurrences = new HashMap<>();

        sqlQuery.getContext().sparqlVarOccurrences().forEach((node, occurrences) -> {
            if (node.getName().equals(identifiedVariable.getSqlVarName())) {
                List<SPARQLOccurrence> sparqlOccurrences = new ArrayList<>();
                occurrences.forEach(occurrence -> {
                    if (occurrence.getSqlVariable().getSqlVarType() == SQLVarType.ID) {
                        newSPARQLOccurrences
                                .computeIfAbsent(node, k -> sparqlOccurrences)
                                .add(new SPARQLOccurrence(
                                        occurrence.getType(),
                                        occurrence.getPosition(),
                                        occurrence.getContextType(),
                                        new SQLVariable(SQLVarType.VALUE, occurrence.getSqlVariable().getSqlVarName())
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
}
