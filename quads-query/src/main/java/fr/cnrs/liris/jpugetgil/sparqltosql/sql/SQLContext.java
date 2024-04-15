package fr.cnrs.liris.jpugetgil.sparqltosql.sql;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.SPARQLOccurrence;
import org.apache.jena.graph.Node;

import java.util.List;
import java.util.Map;

/**
 * This class represents the context of a SQL query.
 *
 * @param graph                The graph of the context
 * @param sparqlVarOccurrences The occurrences of the variables in the context
 * @param tableName            The name of the table in the context
 * @param tableIndex           The index of the table in the context
 * @param sqlVariables         The variables of the SQL query made by the context
 */
public record SQLContext(Node graph, Map<Node, List<SPARQLOccurrence>> sparqlVarOccurrences, String tableName,
                         Integer tableIndex, List<SQLVariable> sqlVariables) {
    public SQLContext setGraph(Node newGraph, String tableName) {
        return new SQLContext(newGraph, sparqlVarOccurrences, tableName, tableIndex, sqlVariables);
    }

    public SQLContext setVarOccurrences(Map<Node, List<SPARQLOccurrence>> newVarOccurrences) {
        return new SQLContext(graph, newVarOccurrences, tableName, tableIndex, sqlVariables);
    }

    public SQLContext setTableName(String newTableName) {
        return new SQLContext(graph, sparqlVarOccurrences, newTableName, tableIndex, sqlVariables);
    }

    public SQLContext setTableIndex(Integer newTableIndex) {
        return new SQLContext(graph, sparqlVarOccurrences, tableName, newTableIndex, sqlVariables);
    }

    public SQLContext setSQLVariables(List<SQLVariable> newSQLVariables) {
        return new SQLContext(graph, sparqlVarOccurrences, tableName, tableIndex, newSQLVariables);
    }
}
