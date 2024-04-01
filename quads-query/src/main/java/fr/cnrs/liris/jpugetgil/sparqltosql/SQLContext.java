package fr.cnrs.liris.jpugetgil.sparqltosql;

import org.apache.jena.graph.Node;

import java.util.List;
import java.util.Map;

public record SQLContext(Node graph, Map<Node, List<Occurrence>> varOccurrences, String tableName) {
    public SQLContext setGraph(Node newGraph, String tableName) {
        return new SQLContext(newGraph, varOccurrences, tableName);
    }

    public SQLContext setVarOccurrences(Map<Node, List<Occurrence>> newVarOccurrences) {
        return new SQLContext(graph, newVarOccurrences, tableName);
    }

    public SQLContext setTableName(String newTableName) {
        return new SQLContext(graph, varOccurrences, newTableName);
    }
}
