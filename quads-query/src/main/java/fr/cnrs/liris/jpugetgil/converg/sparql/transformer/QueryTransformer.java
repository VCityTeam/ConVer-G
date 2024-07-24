package fr.cnrs.liris.jpugetgil.converg.sparql.transformer;

import org.apache.jena.sparql.algebra.Op;

public interface QueryTransformer {
    Op getOp();
}
