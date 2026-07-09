package fr.cnrs.liris.jpugetgil.converg.entailment;

import org.apache.jena.graph.Node;

import java.util.List;

/**
 * Constants and utilities for schema drift detection across versions.
 * <p>
 * Schema drift is exposed as a queryable virtual graph in the SPARQL pipeline.
 * Querying {@code GRAPH <urn:converg:schema-drift> { ?s ?p ?o }} returns all
 * RDFS/OWL schema triples with their version sets, enabling users to detect
 * ontological changes across versions using standard SPARQL FILTERs.
 * <p>
 * The virtual graph is handled by {@code SchemaDriftSQLOperator} which generates
 * SQL filtering on schema predicates (rdfs:subClassOf, rdfs:domain, etc.).
 */
public final class SchemaDriftDetector {

    /**
     * The virtual graph URI that triggers schema drift detection when used
     * in a {@code GRAPH} pattern.
     */
    public static final String SCHEMA_DRIFT_GRAPH_URI = "urn:converg:schema-drift";

    /**
     * RDFS/OWL predicates that constitute the "schema" layer.
     */
    public static final List<String> SCHEMA_PREDICATES = List.of(
            RDFSRules.RDFS_SUBCLASS_OF,
            RDFSRules.RDFS_SUBPROPERTY_OF,
            RDFSRules.RDFS_DOMAIN,
            RDFSRules.RDFS_RANGE
    );

    private SchemaDriftDetector() {
    }

    /**
     * Check whether a graph node refers to the schema-drift virtual graph.
     */
    public static boolean isSchemaDriftGraph(Node graphNode) {
        return graphNode.isURI() && SCHEMA_DRIFT_GRAPH_URI.equals(graphNode.getURI());
    }
}
