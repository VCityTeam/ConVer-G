package fr.cnrs.liris.jpugetgil.sparqltosql;

import org.apache.jena.graph.Node;

import java.util.Map;

public record SQLContext(Node graph, Map<String, Integer> URI_ids) {
    public SQLContext setGraph(Node new_graph) {
        return new SQLContext(new_graph, this.URI_ids);
    }

    public SQLContext setURI_ids(Map<String, Integer> new_URI_ids) {
        return new SQLContext(this.graph, new_URI_ids);
    }
}
