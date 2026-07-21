package fr.vcity;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.core.Quad;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RDFDeltaTest {

    private static final String EX = "http://example.edu/";

    /**
     * Builds a model from (subject, predicate, object) triplets, each term interpreted as a URI under {@link #EX}.
     */
    private static Model modelOf(String... spo) {
        Model model = ModelFactory.createDefaultModel();
        for (int i = 0; i < spo.length; i += 3) {
            Resource subject = model.createResource(EX + spo[i]);
            Property predicate = model.createProperty(EX + spo[i + 1]);
            model.add(subject, predicate, model.createResource(EX + spo[i + 2]));
        }
        return model;
    }

    private static Quad quad(String g, String s, String p, String o) {
        return Quad.create(
                NodeFactory.createURI(EX + g),
                NodeFactory.createURI(EX + s),
                NodeFactory.createURI(EX + p),
                NodeFactory.createURI(EX + o));
    }

    /**
     * Builds a dataset from (graph, subject, predicate, object) 4-tuples.
     */
    private static Dataset datasetOf(String... gspo) {
        Dataset dataset = DatasetFactory.create();
        for (int i = 0; i < gspo.length; i += 4) {
            dataset.asDatasetGraph().add(quad(gspo[i], gspo[i + 1], gspo[i + 2], gspo[i + 3]));
        }
        return dataset;
    }

    @Test
    void additionsBetweenModelsContainsOnlyNewTriples() {
        Model oldModel = modelOf("Building1", "height", "10");
        Model newModel = modelOf("Building1", "height", "10", "Building3", "height", "15");

        Model additions = RDFDelta.additions(oldModel, newModel);

        assertEquals(1, additions.size());
        assertTrue(additions.contains(
                additions.createResource(EX + "Building3"),
                additions.createProperty(EX + "height"),
                additions.createResource(EX + "15")));
    }

    @Test
    void deletionsBetweenModelsContainsOnlyRemovedTriples() {
        Model oldModel = modelOf("Building1", "height", "10", "Building2", "height", "9");
        Model newModel = modelOf("Building1", "height", "10");

        Model deletions = RDFDelta.deletions(oldModel, newModel);

        assertEquals(1, deletions.size());
        assertTrue(deletions.contains(
                deletions.createResource(EX + "Building2"),
                deletions.createProperty(EX + "height"),
                deletions.createResource(EX + "9")));
    }

    @Test
    void identicalModelsProduceEmptyDelta() {
        Model a = modelOf("Building1", "height", "10");
        Model b = modelOf("Building1", "height", "10");

        assertTrue(RDFDelta.additions(a, b).isEmpty());
        assertTrue(RDFDelta.deletions(a, b).isEmpty());
    }

    @Test
    void datasetDeltaIsGraphAware() {
        // Same triple, but moved from GraphA to GraphB: one deletion and one addition.
        Dataset oldDataset = datasetOf("GraphA", "Building1", "height", "10");
        Dataset newDataset = datasetOf("GraphB", "Building1", "height", "10");

        Dataset additions = RDFDelta.additions(oldDataset, newDataset);
        Dataset deletions = RDFDelta.deletions(oldDataset, newDataset);

        assertEquals(1, additions.asDatasetGraph().stream().count());
        assertTrue(additions.asDatasetGraph().contains(quad("GraphB", "Building1", "height", "10")));

        assertEquals(1, deletions.asDatasetGraph().stream().count());
        assertTrue(deletions.asDatasetGraph().contains(quad("GraphA", "Building1", "height", "10")));
    }

    @Test
    void datasetDeltaIgnoresUnchangedQuads() {
        Dataset oldDataset = datasetOf(
                "GraphA", "Building1", "height", "10",
                "GraphA", "Building2", "height", "9");
        Dataset newDataset = datasetOf(
                "GraphA", "Building1", "height", "10",
                "GraphA", "Building3", "height", "15");

        Dataset additions = RDFDelta.additions(oldDataset, newDataset);
        Dataset deletions = RDFDelta.deletions(oldDataset, newDataset);

        assertEquals(1, additions.asDatasetGraph().stream().count());
        assertTrue(additions.asDatasetGraph().contains(quad("GraphA", "Building3", "height", "15")));

        assertEquals(1, deletions.asDatasetGraph().stream().count());
        assertTrue(deletions.asDatasetGraph().contains(quad("GraphA", "Building2", "height", "9")));
    }

    @Test
    void outputFilenamesUseNqExtensionForQuads() {
        String[] out = RDFDelta.getOutputFilenames("v1.trig", "v2.trig", "/data/", false);

        assertArrayEquals(
                new String[]{"/data/v1-v2.additions.nq", "/data/v1-v2.deletions.nq"},
                out);
    }

    @Test
    void outputFilenamesUseNtExtensionForTriples() {
        String[] out = RDFDelta.getOutputFilenames("a.ttl", "b.ttl", "/data/", true);

        assertArrayEquals(
                new String[]{"/data/a-b.additions.nt", "/data/a-b.deletions.nt"},
                out);
    }

    @Test
    void validateAndGetLangResolvesKnownExtensions() {
        assertEquals(Lang.TURTLE, RDFDelta.validateAndGetLang("/data/buildings.ttl"));
        assertEquals(Lang.NQUADS, RDFDelta.validateAndGetLang("/data/buildings.nq"));
        assertEquals(Lang.TRIG, RDFDelta.validateAndGetLang("/data/buildings.trig"));
    }
}
