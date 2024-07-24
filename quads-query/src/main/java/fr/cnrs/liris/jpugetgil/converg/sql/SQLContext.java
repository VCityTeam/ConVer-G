package fr.cnrs.liris.jpugetgil.converg.sql;

import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import org.apache.jena.graph.Node;

import java.util.List;
import java.util.Map;

/**
 * This class represents the context of a SQL query.
 *
 * @param graph                The graph of the context
 * @param sparqlVarOccurrences The occurrences of the variables in the context
 * @param sqlVariables         The variables of the SQL query made by the context
 */
public record SQLContext(Node graph, Map<Node, List<SPARQLOccurrence>> sparqlVarOccurrences, List<SQLVariable> sqlVariables) {
    public SQLContext setGraph(Node newGraph) {
        return new SQLContext(newGraph, sparqlVarOccurrences, sqlVariables);
    }

    public SQLContext setVarOccurrences(Map<Node, List<SPARQLOccurrence>> newVarOccurrences) {
        return new SQLContext(graph, newVarOccurrences, sqlVariables);
    }

    public SQLContext setSQLVariables(List<SQLVariable> newSQLVariables) {
        return new SQLContext(graph, sparqlVarOccurrences, newSQLVariables);
    }
}
