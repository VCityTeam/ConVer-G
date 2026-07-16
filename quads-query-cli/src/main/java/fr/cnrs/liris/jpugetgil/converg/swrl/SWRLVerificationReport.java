package fr.cnrs.liris.jpugetgil.converg.swrl;

import org.semanticweb.owlapi.model.SWRLRule;

import java.util.List;
import java.util.Map;

/**
 * Result of verifying a SWRL rules ontology with {@link SWRLVerifier}: whether
 * the ontology is consistent according to Openllet, which rules the query
 * rewriter supports, and which rules were rejected (mapped to the reason).
 */
public record SWRLVerificationReport(
        boolean consistent,
        List<SWRLRule> supportedRules,
        Map<SWRLRule, String> rejectedRules) {
}
