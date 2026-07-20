package fr.cnrs.liris.jpugetgil.converg.inference;

import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.SWRLArgument;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLClassAtom;
import org.semanticweb.owlapi.model.SWRLDataPropertyAtom;
import org.semanticweb.owlapi.model.SWRLIndividualArgument;
import org.semanticweb.owlapi.model.SWRLLiteralArgument;
import org.semanticweb.owlapi.model.SWRLObjectPropertyAtom;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.model.SWRLVariable;

import java.util.List;

/**
 * Converts a verified OWLAPI {@link SWRLRule} into {@link InferenceRule}s over quads,
 * one per head atom (all sharing the rule body). The atoms accepted here mirror those
 * accepted by {@code SWRLVerifier}; a class atom {@code C(x)} becomes
 * {@code (x, rdf:type, C)} and a property atom {@code p(x, y)} becomes {@code (x, p, y)}.
 */
public final class SwrlRuleTranslator {

    private SwrlRuleTranslator() {
    }

    /**
     * @param rule the verified SWRL rule
     * @param name a human-readable name used in logs
     * @return one {@link InferenceRule} per head atom
     */
    public static List<InferenceRule> translate(SWRLRule rule, String name) {
        List<RuleAtom> body = rule.getBody().stream()
                .map(SwrlRuleTranslator::toAtom)
                .toList();
        return rule.getHead().stream()
                .map(headAtom -> new InferenceRule(name, body, toAtom(headAtom)))
                .toList();
    }

    private static RuleAtom toAtom(SWRLAtom atom) {
        return switch (atom) {
            case SWRLClassAtom classAtom -> new RuleAtom(
                    toTerm(classAtom.getArgument()),
                    new Term.Iri(RuleAtom.RDF_TYPE),
                    new Term.Iri(classAtom.getPredicate().asOWLClass().getIRI().toString()));
            case SWRLObjectPropertyAtom propertyAtom -> new RuleAtom(
                    toTerm(propertyAtom.getFirstArgument()),
                    new Term.Iri(propertyAtom.getPredicate().asOWLObjectProperty().getIRI().toString()),
                    toTerm(propertyAtom.getSecondArgument()));
            case SWRLDataPropertyAtom propertyAtom -> new RuleAtom(
                    toTerm(propertyAtom.getFirstArgument()),
                    new Term.Iri(propertyAtom.getPredicate().asOWLDataProperty().getIRI().toString()),
                    toTerm(propertyAtom.getSecondArgument()));
            default -> throw new IllegalArgumentException(
                    "Unsupported SWRL atom (should have been rejected by the verifier): " + atom);
        };
    }

    private static Term toTerm(SWRLArgument argument) {
        return switch (argument) {
            case SWRLVariable variable -> new Term.Variable(variable.getIRI().getShortForm());
            case SWRLIndividualArgument individual -> new Term.Iri(
                    individual.getIndividual().asOWLNamedIndividual().getIRI().toString());
            case SWRLLiteralArgument literal -> toLiteral(literal.getLiteral());
            default -> throw new IllegalArgumentException(
                    "Unsupported SWRL argument (should have been rejected by the verifier): " + argument);
        };
    }

    private static Term toLiteral(OWLLiteral literal) {
        String datatype = literal.hasLang()
                ? "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString"
                : literal.getDatatype().getIRI().toString();
        return new Term.Literal(literal.getLiteral(), datatype);
    }
}
