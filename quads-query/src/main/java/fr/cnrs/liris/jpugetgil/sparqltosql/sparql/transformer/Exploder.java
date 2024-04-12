package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.transformer;

import org.apache.jena.sparql.algebra.Op;

/**
 * Operator to join with the VersionedNamedGraph table on some graph variable
 * in order to switch to the flat representation for the given variable
 */
public record Exploder(String varName, QueryTransformer transformer) implements QueryTransformer {
    @Override
    public Op getOp() {
        return transformer.getOp();
    }
}
