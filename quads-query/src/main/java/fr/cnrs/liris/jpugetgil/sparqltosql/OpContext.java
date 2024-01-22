package fr.cnrs.liris.jpugetgil.sparqltosql;


import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;

import java.util.ArrayList;
import java.util.List;

public class OpContext {
    public OpContext() {
    }

    private List<Var> projections;

    private Node graph;

    private List<Triple> triples = new ArrayList<>();

    public List<Var> getProjections() {
        return projections;
    }

    public void setProjections(List<Var> projections) {
        this.projections = projections;
    }

    public Node getGraph() {
        return graph;
    }

    public void setGraph(Node graph) {
        this.graph = graph;
    }

    public List<Triple> getTriples() {
        return triples;
    }

    public void setTriples(List<Triple> triples) {
        this.triples = triples;
    }
}
