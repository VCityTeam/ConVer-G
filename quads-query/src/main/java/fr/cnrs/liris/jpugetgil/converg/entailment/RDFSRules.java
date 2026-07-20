package fr.cnrs.liris.jpugetgil.converg.entailment;

/**
 * RDFS vocabulary IRIs.
 * <p>
 * Inference itself is no longer performed by query rewriting; it is computed at query
 * time as a deductive closure over the data (see
 * {@code fr.cnrs.liris.jpugetgil.converg.inference.SaturationSqlBuilder}). These
 * constants remain the shared reference for the RDFS schema predicates, used by
 * {@link SchemaDriftDetector}.
 */
public final class RDFSRules {

    public static final String RDFS_SUBCLASS_OF = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
    public static final String RDFS_SUBPROPERTY_OF = "http://www.w3.org/2000/01/rdf-schema#subPropertyOf";
    public static final String RDFS_DOMAIN = "http://www.w3.org/2000/01/rdf-schema#domain";
    public static final String RDFS_RANGE = "http://www.w3.org/2000/01/rdf-schema#range";

    private RDFSRules() {
    }
}
