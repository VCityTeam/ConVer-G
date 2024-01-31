package fr.cnrs.liris.jpugetgil.sparqltosql;

import org.apache.jena.graph.Node;

import java.util.List;
import java.util.Map;

public record SQLContext(Node graph, Map<Node, List<Occurence>> varOccurrences) {
    public SQLContext setGraph(Node newGraph) {
        return new SQLContext(newGraph, varOccurrences);
    }

    public SQLContext setVarOccurrences(Map<Node, List<Occurence>> newVarOccurrences) {
        return new SQLContext(graph, newVarOccurrences);
    }
}
