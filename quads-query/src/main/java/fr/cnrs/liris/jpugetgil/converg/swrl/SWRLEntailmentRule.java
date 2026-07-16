package fr.cnrs.liris.jpugetgil.converg.swrl;

import fr.cnrs.liris.jpugetgil.converg.entailment.EntailmentRule;
import fr.cnrs.liris.jpugetgil.converg.entailment.FreshVariableAllocator;
import fr.cnrs.liris.jpugetgil.converg.entailment.RDFSRules;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpExtend;
import org.apache.jena.sparql.algebra.op.OpQuadPattern;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.NodeValue;
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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adapts a single SWRL rule head atom (with the full rule body) to the
 * {@link EntailmentRule} interface so SWRL rules are applied through the same
 * query-rewriting machinery as the RDFS entailment rules.
 * <p>
 * A quad pattern matching the head atom is expanded into the rule body: for a
 * rule {@code hasParent(?x, ?y) ^ hasBrother(?y, ?z) -> hasUncle(?x, ?z)}, the
 * pattern {@code ?a :hasUncle ?u} is unioned with
 * {@code ?a :hasParent ?__ent_y . ?__ent_y :hasBrother ?u}. Body atoms are
 * matched in the same graph scope as the triggering quad, so the versioned
 * semantics of a plain BGP apply: the inferred triple only holds in versions
 * where all body premises hold.
 * <p>
 * A rule with several head atoms is split into one {@code SWRLEntailmentRule}
 * per head atom by {@link #fromRule(SWRLRule, String)}.
 */
public class SWRLEntailmentRule implements EntailmentRule {

    private static final Node RDF_TYPE_NODE = NodeFactory.createURI(RDFSRules.RDF_TYPE);

    /**
     * A triple pattern of the rule where SWRL rule variables appear as Jena variables.
     */
    private record TripleTemplate(Node subject, Node predicate, Node object) {
    }

    private final String name;
    private final TripleTemplate head;
    private final List<TripleTemplate> body;

    private SWRLEntailmentRule(String name, TripleTemplate head, List<TripleTemplate> body) {
        this.name = name;
        this.head = head;
        this.body = body;
    }

    /**
     * Converts a verified OWLAPI SWRL rule into entailment rules, one per head atom.
     * The rule must only contain atoms accepted by {@link SWRLVerifier}.
     *
     * @param rule     the SWRL rule to convert
     * @param ruleName a human-readable name used in logs
     * @return one entailment rule per head atom, all sharing the rule body
     */
    public static List<SWRLEntailmentRule> fromRule(SWRLRule rule, String ruleName) {
        List<TripleTemplate> bodyTemplates = rule.getBody().stream()
                .map(SWRLEntailmentRule::toTemplate)
                .toList();
        return rule.getHead().stream()
                .map(headAtom -> new SWRLEntailmentRule(ruleName, toTemplate(headAtom), bodyTemplates))
                .toList();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean appliesTo(Quad quad) {
        return quad.getPredicate().isURI()
                && quad.getPredicate().equals(head.predicate())
                && unify(quad).isPresent();
    }

    @Override
    public Op expand(Quad quad, Node graphNode, FreshVariableAllocator allocator) {
        Unification unification = unify(quad).orElseThrow(() ->
                new IllegalStateException("expand() called on a quad the rule does not apply to: " + quad));

        Map<Var, Node> substitution = new HashMap<>(unification.ruleVarToQuadNode);

        BasicPattern bodyPattern = new BasicPattern();
        for (TripleTemplate template : body) {
            bodyPattern.add(Triple.create(
                    substitute(template.subject(), substitution, allocator),
                    substitute(template.predicate(), substitution, allocator),
                    substitute(template.object(), substitution, allocator)));
        }
        Op bodyOp = new OpQuadPattern(graphNode, bodyPattern);

        // A concrete head node facing a query variable (e.g. ?t in `?x rdf:type ?t`
        // against head Child(?x)) is bound explicitly so the expansion exposes the
        // same variables as the explicit branch of the entailment union
        if (unification.queryVarBindings.isEmpty()) {
            return bodyOp;
        }
        VarExprList bindings = new VarExprList();
        unification.queryVarBindings.forEach((var, node) -> bindings.add(var, NodeValue.makeNode(node)));
        return OpExtend.create(bodyOp, bindings);
    }

    /**
     * Unifies the head template with the quad's subject and object (the predicate
     * is already known to match). Empty when a concrete head node clashes with a
     * concrete quad node, i.e. the rule cannot produce the requested pattern.
     */
    private Optional<Unification> unify(Quad quad) {
        Unification unification = new Unification();
        if (unification.add(head.subject(), quad.getSubject())
                && unification.add(head.object(), quad.getObject())) {
            return Optional.of(unification);
        }
        return Optional.empty();
    }

    private static Node substitute(Node templateNode, Map<Var, Node> substitution, FreshVariableAllocator allocator) {
        if (!templateNode.isVariable()) {
            return templateNode;
        }
        return substitution.computeIfAbsent(Var.alloc(templateNode),
                variable -> allocator.allocate(variable.getName()));
    }

    /**
     * Mapping built while unifying the head with a quad: rule variables map to the
     * quad's nodes, and concrete head nodes facing a query variable produce a
     * binding of that variable, emitted as an OpExtend on the expansion.
     */
    private static class Unification {
        private final Map<Var, Node> ruleVarToQuadNode = new HashMap<>();
        private final Map<Var, Node> queryVarBindings = new LinkedHashMap<>();

        boolean add(Node templateNode, Node quadNode) {
            if (templateNode.isVariable()) {
                Node previous = ruleVarToQuadNode.putIfAbsent(Var.alloc(templateNode), quadNode);
                return previous == null || previous.equals(quadNode);
            }
            if (quadNode.isVariable()) {
                Node previous = queryVarBindings.putIfAbsent(Var.alloc(quadNode), templateNode);
                return previous == null || previous.equals(templateNode);
            }
            return templateNode.equals(quadNode);
        }
    }

    private static TripleTemplate toTemplate(SWRLAtom atom) {
        return switch (atom) {
            case SWRLClassAtom classAtom -> new TripleTemplate(
                    toNode(classAtom.getArgument()),
                    RDF_TYPE_NODE,
                    NodeFactory.createURI(classAtom.getPredicate().asOWLClass().getIRI().toString()));
            case SWRLObjectPropertyAtom propertyAtom -> new TripleTemplate(
                    toNode(propertyAtom.getFirstArgument()),
                    NodeFactory.createURI(propertyAtom.getPredicate().asOWLObjectProperty().getIRI().toString()),
                    toNode(propertyAtom.getSecondArgument()));
            case SWRLDataPropertyAtom propertyAtom -> new TripleTemplate(
                    toNode(propertyAtom.getFirstArgument()),
                    NodeFactory.createURI(propertyAtom.getPredicate().asOWLDataProperty().getIRI().toString()),
                    toNode(propertyAtom.getSecondArgument()));
            default -> throw new IllegalArgumentException(
                    "Unsupported SWRL atom (should have been rejected by the verifier): " + atom);
        };
    }

    private static Node toNode(SWRLArgument argument) {
        return switch (argument) {
            case SWRLVariable variable -> Var.alloc(variable.getIRI().getShortForm());
            case SWRLIndividualArgument individual -> NodeFactory.createURI(
                    individual.getIndividual().asOWLNamedIndividual().getIRI().toString());
            case SWRLLiteralArgument literal -> toLiteralNode(literal.getLiteral());
            default -> throw new IllegalArgumentException(
                    "Unsupported SWRL argument (should have been rejected by the verifier): " + argument);
        };
    }

    private static Node toLiteralNode(OWLLiteral literal) {
        if (literal.hasLang()) {
            return NodeFactory.createLiteralLang(literal.getLiteral(), literal.getLang());
        }
        return NodeFactory.createLiteralDT(literal.getLiteral(),
                NodeFactory.getType(literal.getDatatype().getIRI().toString()));
    }
}
