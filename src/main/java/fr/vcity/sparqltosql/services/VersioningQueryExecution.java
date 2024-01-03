package fr.vcity.sparqltosql.services;

import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.util.Context;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class VersioningQueryExecution implements QueryExecution {
    private final Query query;

    public VersioningQueryExecution(Query query) {
        this.query = query;
    }

    @Override
    public void setInitialBinding(QuerySolution binding) {

    }

    @Override
    public void setInitialBinding(Binding binding) {

    }

    @Override
    public Dataset getDataset() {
        return null;
    }

    @Override
    public Context getContext() {
        return Context.emptyContext();
    }

    @Override
    public Query getQuery() {
        return query;
    }

    @Override
    public String getQueryString() {
        return query.toString();
    }

    @Override
    public ResultSet execSelect() {
        Var toto = Var.alloc("toto");
        Node totoVal = NodeFactory.createURI("http://toto.example.com");
        List<Var> vars = new ArrayList<>();
        vars.add(toto);
        Binding binding = Binding.builder().add(toto, totoVal).build();
        List<Binding> bindings = new ArrayList<>();
        bindings.add(binding);
        return ResultSetStream.create(vars, bindings.iterator());
    }

    @Override
    public Model execConstruct() {
        return null;
    }

    @Override
    public Model execConstruct(Model model) {
        return null;
    }

    @Override
    public Iterator<Triple> execConstructTriples() {
        return null;
    }

    @Override
    public Iterator<Quad> execConstructQuads() {
        return null;
    }

    @Override
    public Dataset execConstructDataset() {
        return null;
    }

    @Override
    public Dataset execConstructDataset(Dataset dataset) {
        return null;
    }

    @Override
    public Model execDescribe() {
        return null;
    }

    @Override
    public Model execDescribe(Model model) {
        return null;
    }

    @Override
    public Iterator<Triple> execDescribeTriples() {
        return null;
    }

    @Override
    public boolean execAsk() {
        return false;
    }

    @Override
    public JsonArray execJson() {
        return null;
    }

    @Override
    public Iterator<JsonObject> execJsonItems() {
        return null;
    }

    @Override
    public void abort() {

    }

    @Override
    public void close() {

    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public void setTimeout(long timeout, java.util.concurrent.TimeUnit timeoutUnits) {

    }

    @Override
    public void setTimeout(long timeout) {

    }

    @Override
    public void setTimeout(long timeout1, java.util.concurrent.TimeUnit timeUnit1, long timeout2, java.util.concurrent.TimeUnit timeUnit2) {

    }

    @Override
    public void setTimeout(long timeout1, long timeout2) {

    }

    @Override
    public long getTimeout1() {
        return 0;
    }

    @Override
    public long getTimeout2() {
        return 0;
    }
}

