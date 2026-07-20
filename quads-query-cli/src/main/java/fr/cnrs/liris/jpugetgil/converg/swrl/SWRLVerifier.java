package fr.cnrs.liris.jpugetgil.converg.swrl;

import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.SWRLArgument;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLClassAtom;
import org.semanticweb.owlapi.model.SWRLDataPropertyAtom;
import org.semanticweb.owlapi.model.SWRLIndividualArgument;
import org.semanticweb.owlapi.model.SWRLLiteralArgument;
import org.semanticweb.owlapi.model.SWRLObjectPropertyAtom;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.model.SWRLVariable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Verifies a SWRL rules ontology before it is used for query rewriting:
 * <ul>
 *   <li>the ontology (schema, rules and any individuals) must be consistent,
 *       checked with the Openllet reasoner,</li>
 *   <li>each rule may only use constructs the query rewriter can translate:
 *       named-class atoms, named object/data property atoms, and variable,
 *       named-individual or literal arguments. Builtin, sameAs/differentFrom
 *       and data range atoms are rejected, as are rules without a head or a
 *       body.</li>
 * </ul>
 * Rejected rules are reported with the reason so the caller can decide whether
 * to skip them or fail.
 */
public class SWRLVerifier {

    /**
     * Verify the given ontology and classify its SWRL rules.
     *
     * @param ontology the ontology holding the SWRL rules
     * @return the verification report
     */
    public SWRLVerificationReport verify(OWLOntology ontology) {
        boolean consistent = isConsistent(ontology);

        List<SWRLRule> supported = new ArrayList<>();
        Map<SWRLRule, String> rejected = new LinkedHashMap<>();
        ontology.axioms(AxiomType.SWRL_RULE).forEach(rule ->
                checkSupported(rule).ifPresentOrElse(
                        reason -> rejected.put(rule, reason),
                        () -> supported.add(rule)));

        return new SWRLVerificationReport(consistent, supported, rejected);
    }

    private boolean isConsistent(OWLOntology ontology) {
        OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);
        try {
            return reasoner.isConsistent();
        } finally {
            reasoner.dispose();
        }
    }

    /**
     * @return the rejection reason, or empty when the rule is supported
     */
    private Optional<String> checkSupported(SWRLRule rule) {
        if (rule.getHead().isEmpty()) {
            return Optional.of("rule has no head atom");
        }
        if (rule.getBody().isEmpty()) {
            return Optional.of("rule has no body atom (facts cannot be applied by query rewriting)");
        }
        return Stream.concat(rule.getHead().stream(), rule.getBody().stream())
                .map(this::checkAtom)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<String> checkAtom(SWRLAtom atom) {
        return switch (atom) {
            case SWRLClassAtom classAtom -> classAtom.getPredicate().isAnonymous()
                    ? Optional.of("class atom uses an anonymous class expression: " + atom)
                    : checkArguments(atom);
            case SWRLObjectPropertyAtom propertyAtom -> propertyAtom.getPredicate().isAnonymous()
                    ? Optional.of("object property atom uses an anonymous property expression: " + atom)
                    : checkArguments(atom);
            case SWRLDataPropertyAtom ignored -> checkArguments(atom);
            default -> Optional.of("unsupported atom type: " + atom);
        };
    }

    private Optional<String> checkArguments(SWRLAtom atom) {
        return atom.getAllArguments().stream()
                .map(this::checkArgument)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<String> checkArgument(SWRLArgument argument) {
        return switch (argument) {
            case SWRLVariable ignored -> Optional.empty();
            case SWRLIndividualArgument individual -> individual.getIndividual().isAnonymous()
                    ? Optional.of("anonymous individual argument: " + argument)
                    : Optional.empty();
            case SWRLLiteralArgument ignored -> Optional.empty();
            default -> Optional.of("unsupported argument type: " + argument);
        };
    }
}
