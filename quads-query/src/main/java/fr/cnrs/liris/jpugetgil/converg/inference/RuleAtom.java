package fr.cnrs.liris.jpugetgil.converg.inference;

/**
 * A single triple pattern (subject, predicate, object) in the body or head of an
 * {@link InferenceRule}. Predicates are always concrete: a class atom {@code C(x)}
 * is represented as {@code (x, rdf:type, C)} and a property atom {@code p(x, y)} as
 * {@code (x, p, y)}.
 */
public record RuleAtom(Term subject, Term predicate, Term object) {

    public static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

}
