package fr.cnrs.liris.jpugetgil.converg.entailment;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.core.Quad;

/**
 * Represents a single RDFS or OWL entailment rule that can expand a quad pattern
 * to include inferred triples. Each rule checks whether it applies to a given quad
 * and, if so, produces an Op subtree representing the inferred pattern.
 * <p>
 * The version set of the inferred pattern is determined by how the expansion's
 * Op nodes compose: {@code OpJoin} yields intersection (conjunctive premises),
 * while {@code OpUnion} yields union (disjunctive alternatives).
 */
public interface EntailmentRule {

    /**
     * @return a human-readable name for this rule (e.g., "rdfs9", "rdfs2")
     */
    String name();

    /**
     * Checks whether this rule can produce inferred triples from the given quad pattern.
     *
     * @param quad the quad pattern to check
     * @return true if this rule applies
     */
    boolean appliesTo(Quad quad);

    /**
     * Expands the given quad pattern into an Op subtree that, when joined or unioned
     * with the original pattern, produces the inferred triples.
     * <p>
     * The returned Op should be combined with the original quad via {@code OpUnion}
     * so that both explicit and inferred results are returned.
     *
     * @param quad      the quad pattern that triggered this rule
     * @param graphNode the graph node scope for the expansion
     * @param allocator allocator for generating fresh internal variable names
     * @return an Op subtree representing the inferred pattern
     */
    Op expand(Quad quad, Node graphNode, FreshVariableAllocator allocator);
}
