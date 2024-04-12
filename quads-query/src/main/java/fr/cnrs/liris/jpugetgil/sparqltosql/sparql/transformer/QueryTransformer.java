package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.transformer;

import org.apache.jena.sparql.algebra.Op;

public interface QueryTransformer {
    Op getOp();
}
