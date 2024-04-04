package fr.cnrs.liris.jpugetgil.sparqltosql;

import org.apache.jena.graph.Node;

import java.util.List;
import java.util.Map;

/**
 * This class represents the context of a SQL query.
 * @param graph The graph of the context
 * @param varOccurrences The occurrences of the variables in the context
 * @param tableName The name of the table in the context
 * @param tableIndex The index of the table in the context
 */
public record SQLContext(Node graph, Map<Node, List<Occurrence>> varOccurrences, String tableName, Integer tableIndex) {
    public SQLContext setGraph(Node newGraph, String tableName) {
        return new SQLContext(newGraph, varOccurrences, tableName, tableIndex);
    }

    public SQLContext setVarOccurrences(Map<Node, List<Occurrence>> newVarOccurrences) {
        return new SQLContext(graph, newVarOccurrences, tableName, tableIndex);
    }

    public SQLContext setTableName(String newTableName) {
        return new SQLContext(graph, varOccurrences, newTableName, tableIndex);
    }

    public SQLContext setTableIndex(Integer newTableIndex) {
        return new SQLContext(graph, varOccurrences, tableName, newTableIndex);
    }
}
