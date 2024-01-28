package fr.cnrs.liris.jpugetgil.sparqltosql;


import com.github.jsonldjava.shaded.com.google.common.collect.Streams;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    public String buildSQL() {
        String sql = "SELECT {{projections}} FROM {{table}} table \n";

        if (!getTriples().isEmpty()) {
            sql += Streams.mapWithIndex(getTriples().stream(), (triple, index) -> {
                String tripleSql = "";

                if (triple.getSubject().isURI()) {
                    tripleSql += "JOIN resource_or_literal rl" + (4 * index) + " ON table.id_subject = rl" + (4 * index) + ".id_resource_or_literal AND rl" + (4 * index) + ".name = '" + triple.getSubject().getURI() + "'\n";
                } else {
                    tripleSql += "JOIN resource_or_literal rl" + (4 * index) + " ON table.id_subject = rl" + (4 * index) + ".id_resource_or_literal\n";
                }

                if (triple.getPredicate().isURI()) {
                    tripleSql += "JOIN resource_or_literal rl" + (4 * index + 1) + " ON table.id_property = rl" + (4 * index + 1) + ".id_resource_or_literal AND rl" + (4 * index + 1) + ".name = '" + triple.getPredicate().getURI() + "'\n";
                } else {
                    tripleSql += "JOIN resource_or_literal rl" + (4 * index + 1) + " ON table.id_property = rl" + (4 * index + 1) + ".id_resource_or_literal\n";
                }

                if (triple.getObject().isURI()) {
                    tripleSql += "JOIN resource_or_literal rl" + (4 * index + 2) + " ON table.id_object = rl" + (4 * index + 2) + ".id_resource_or_literal AND rl" + (4 * index + 2) + ".name = '" + triple.getObject().getURI() + "'\n";

                } else {
                    tripleSql += "JOIN resource_or_literal rl" + (4 * index + 2) + " ON table.id_object = rl" + (4 * index + 2) + ".id_resource_or_literal\n";
                }

                if (getGraph() != null && getGraph().isURI()) {
                    tripleSql += "JOIN resource_or_literal rl" + (4 * index + 3) + " ON table.id_named_graph = rl" + (4 * index + 3) + ".id_resource_or_literal AND rl" + (4 * index + 3) + ".name = '" + getGraph().getURI() + "'\n";
                } else if (getGraph() != null) {
                    tripleSql += "JOIN resource_or_literal rl" + (4 * index + 3) + " ON table.id_named_graph = rl" + (4 * index + 3) + ".id_resource_or_literal\n";
                }

                return tripleSql;
            }).collect(Collectors.joining(" AND "));
        }

        if (getGraph() != null) {
            sql = sql.replace("{{table}}", "versioned_quad");
        } else {
            sql = sql.replace("{{table}}", "workspace");
        }

        if (getProjections().isEmpty()) {
            // FIXME : add all columns that are variables
            sql = sql.replace("{{projections}}", "*");
        } else {
            // FIXME : add all prefix to variables
            sql = sql.replace("{{projections}}", getProjections().stream().map(Var::getVarName).collect(Collectors.joining(", ")));
        }

        return sql;
    }
}
