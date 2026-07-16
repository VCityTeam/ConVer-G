package fr.cnrs.liris.jpugetgil.converg.swrl;

import fr.cnrs.liris.jpugetgil.converg.entailment.EntailmentRule;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.SWRLRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SWRL reasoner: loads a SWRL rules ontology, verifies it with Openllet
 * ({@link SWRLVerifier}) and exposes the supported rules as
 * {@link EntailmentRule}s applied at query-rewriting time, alongside the RDFS
 * entailment rules.
 * <p>
 * The rules file is configured with the {@code SWRL_RULES} environment
 * variable (any ontology format OWLAPI can parse: RDF/XML, Turtle, OWL/XML,
 * functional syntax). An inconsistent ontology or an unreadable file is a
 * configuration error and fails loudly; individual rules the rewriter cannot
 * translate are skipped with a warning.
 */
public class SWRLReasoner {

    private static final Logger log = LoggerFactory.getLogger(SWRLReasoner.class);

    public static final String SWRL_RULES_ENV = "SWRL_RULES";

    private static final SWRLReasoner DISABLED =
            new SWRLReasoner(List.of(), new SWRLVerificationReport(true, List.of(), Map.of()));

    private final List<EntailmentRule> rules;
    private final SWRLVerificationReport report;

    private SWRLReasoner(List<EntailmentRule> rules, SWRLVerificationReport report) {
        this.rules = rules;
        this.report = report;
    }

    /**
     * @return the reasoner configured from the {@code SWRL_RULES} environment
     * variable, or a disabled reasoner when the variable is unset
     */
    public static SWRLReasoner fromEnv() {
        String path = System.getenv(SWRL_RULES_ENV);
        if (path == null || path.isBlank()) {
            return disabled();
        }
        return fromFile(path);
    }

    /**
     * @return a reasoner with no rules (SWRL reasoning disabled)
     */
    public static SWRLReasoner disabled() {
        return DISABLED;
    }

    /**
     * Loads and verifies the SWRL rules ontology at the given path.
     *
     * @throws IllegalStateException when the file is missing, unparsable or inconsistent
     */
    public static SWRLReasoner fromFile(String path) {
        File file = new File(path);
        if (!file.isFile()) {
            throw new IllegalStateException("SWRL rules file not found: " + path);
        }
        OWLOntology ontology;
        try {
            ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(file);
        } catch (OWLOntologyCreationException e) {
            throw new IllegalStateException("Could not parse SWRL rules file: " + path, e);
        }
        log.info("SWRL rules loaded from {}", path);
        return fromOntology(ontology);
    }

    /**
     * Verifies the given ontology with Openllet and converts its supported SWRL
     * rules for query rewriting.
     *
     * @throws IllegalStateException when the ontology is inconsistent
     */
    public static SWRLReasoner fromOntology(OWLOntology ontology) {
        SWRLVerificationReport report = new SWRLVerifier().verify(ontology);
        if (!report.consistent()) {
            throw new IllegalStateException(
                    "SWRL rules ontology is inconsistent (Openllet verification failed), refusing to reason with it");
        }
        report.rejectedRules().forEach((rule, reason) ->
                log.warn("SWRL rule skipped, {}: {}", reason, rule));

        List<EntailmentRule> rules = new ArrayList<>();
        int index = 0;
        for (SWRLRule rule : report.supportedRules()) {
            rules.addAll(SWRLEntailmentRule.fromRule(rule, ruleName(rule, index)));
            index++;
        }
        log.info("SWRL reasoner enabled: {} rule(s) verified and active, {} skipped",
                report.supportedRules().size(), report.rejectedRules().size());
        return new SWRLReasoner(List.copyOf(rules), report);
    }

    /**
     * @return true when at least one verified rule is active
     */
    public boolean isEnabled() {
        return !rules.isEmpty();
    }

    /**
     * @return the verified SWRL rules as entailment rules for the query rewriter
     */
    public List<EntailmentRule> getRules() {
        return rules;
    }

    /**
     * @return the Openllet verification report of the loaded ontology
     */
    public SWRLVerificationReport getReport() {
        return report;
    }

    /**
     * The rule's rdfs:label when present, otherwise a positional name.
     */
    private static String ruleName(SWRLRule rule, int index) {
        return rule.annotations()
                .filter(annotation -> annotation.getProperty().isLabel())
                .map(annotation -> annotation.getValue().asLiteral())
                .flatMap(Optional::stream)
                .map(literal -> "swrl:" + literal.getLiteral())
                .findFirst()
                .orElse("swrl#" + index);
    }
}
