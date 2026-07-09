package fr.cnrs.liris.jpugetgil.converg.entailment;

import org.apache.jena.sparql.core.Var;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Allocates fresh variable names prefixed with {@code __ent_} to avoid
 * collisions with user-defined SPARQL variables during entailment rewriting.
 */
public class FreshVariableAllocator {

    private static final String PREFIX = "__ent_";

    private final AtomicInteger counter = new AtomicInteger(0);

    public Var allocate(String hint) {
        return Var.alloc(PREFIX + hint + "_" + counter.getAndIncrement());
    }
}
