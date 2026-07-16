package fr.cnrs.liris.jpugetgil.converg.swrl;

import fr.cnrs.liris.jpugetgil.converg.SPARQLtoSQLTranslator;
import fr.cnrs.liris.jpugetgil.converg.entailment.EntailmentRegime;
import fr.cnrs.liris.jpugetgil.converg.entailment.EntailmentRewriter;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.Op1;
import org.apache.jena.sparql.algebra.op.Op2;
import org.apache.jena.sparql.algebra.op.OpExtend;
import org.apache.jena.sparql.algebra.op.OpUnion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class SWRLReasonerTest {

    private static SWRLReasoner reasoner;

    @BeforeAll
    static void loadRules() {
        reasoner = SWRLReasoner.fromFile(resourcePath("swrl/family-rules.ofn"));
    }

    private static String resourcePath(String name) {
        try {
            return Paths.get(Objects.requireNonNull(
                    SWRLReasonerTest.class.getClassLoader().getResource(name)).toURI()).toString();
        } catch (Exception e) {
            throw new IllegalStateException("Test resource not found: " + name, e);
        }
    }

    private static Op parseToQuadOp(String sparql) {
        Query query = QueryFactory.create(sparql);
        Op op = Algebra.compile(query);
        return Algebra.toQuadForm(op);
    }

    @Test
    void verifierAcceptsSupportedRulesAndRejectsBuiltins() {
        assertTrue(reasoner.isEnabled(), "Reasoner should be enabled with verified rules");
        assertTrue(reasoner.getReport().consistent(), "Family ontology should be consistent");

        // "uncle" and "child" are supported, "adult" uses a builtin atom and is rejected
        assertEquals(2, reasoner.getReport().supportedRules().size(),
                "Two rules should pass verification");
        assertEquals(2, reasoner.getRules().size(),
                "Each supported single-head rule should yield one entailment rule");
        assertEquals(1, reasoner.getReport().rejectedRules().size(),
                "The builtin rule should be rejected");
        assertTrue(reasoner.getReport().rejectedRules().values().iterator().next()
                        .contains("unsupported atom type"),
                "Rejection reason should mention the unsupported builtin atom");
    }

    @Test
    void inconsistentOntologyIsRefused() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> SWRLReasoner.fromFile(resourcePath("swrl/inconsistent.ofn")));
        assertTrue(e.getMessage().contains("inconsistent"),
                "The verifier should refuse an inconsistent ontology");
    }

    @Test
    void missingRulesFileIsRefused() {
        assertThrows(IllegalStateException.class,
                () -> SWRLReasoner.fromFile("/does/not/exist.ofn"));
    }

    @Test
    void uncleRuleExpandsPropertyPattern() {
        // ?a :hasUncle ?u should be unioned with the rule body
        // (?a :hasParent ?__ent_y . ?__ent_y :hasBrother ?u)
        Op op = parseToQuadOp(
                "PREFIX fam: <http://example.org/family#>\n" +
                "SELECT ?g ?a ?u WHERE { GRAPH ?g { ?a fam:hasUncle ?u } }");
        Op rewritten = new EntailmentRewriter(reasoner.getRules()).rewrite(op);

        assertNotEquals(op, rewritten, "hasUncle pattern should be expanded by the SWRL rule");
        assertTrue(containsOpType(rewritten, OpUnion.class),
                "Rewritten tree should union explicit and inferred results");
    }

    @Test
    void classAtomHeadBindsVariableObject() {
        // ?x rdf:type ?t triggers the "child" rule; the expansion must bind ?t to
        // fam:Child (OpExtend) so both union branches expose the same variables
        Op op = parseToQuadOp(
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT ?g ?x ?t WHERE { GRAPH ?g { ?x rdf:type ?t } }");
        Op rewritten = new EntailmentRewriter(reasoner.getRules()).rewrite(op);

        assertNotEquals(op, rewritten, "rdf:type pattern should trigger the class-atom rule");
        assertTrue(containsOpType(rewritten, OpExtend.class),
                "Variable type object should be bound to the rule head class via OpExtend");
    }

    @Test
    void classAtomHeadMatchesConcreteClass() {
        Op op = parseToQuadOp(
                """
                        PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                        PREFIX fam: <http://example.org/family#>
                        SELECT ?g ?x WHERE { GRAPH ?g { ?x rdf:type fam:Child } }""");
        Op rewritten = new EntailmentRewriter(reasoner.getRules()).rewrite(op);

        assertNotEquals(op, rewritten, "Concrete fam:Child pattern should trigger the child rule");
        assertTrue(containsOpType(rewritten, OpUnion.class),
                "Rewritten tree should union the explicit pattern with the rule body BGP");
        assertFalse(containsOpType(rewritten, OpExtend.class),
                "Concrete head match needs no extra binding");
    }

    @Test
    void nonMatchingPatternsAreUntouched() {
        // hasBrother is not the head of any rule
        Op op = parseToQuadOp(
                "PREFIX fam: <http://example.org/family#>\n" +
                "SELECT ?g ?s ?o WHERE { GRAPH ?g { ?s fam:hasBrother ?o } }");
        Op rewritten = new EntailmentRewriter(reasoner.getRules()).rewrite(op);

        assertEquals(op, rewritten, "Patterns matching no rule head should not be rewritten");
    }

    @Test
    void otherConcreteClassDoesNotTriggerClassAtomRule() {
        // fam:Person is not the head of the child rule (head is fam:Child)
        Op op = parseToQuadOp(
                """
                        PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                        PREFIX fam: <http://example.org/family#>
                        SELECT ?g ?x WHERE { GRAPH ?g { ?x rdf:type fam:Person } }""");
        Op rewritten = new EntailmentRewriter(reasoner.getRules()).rewrite(op);

        assertEquals(op, rewritten, "A concrete class that is no rule head should not be rewritten");
    }

    @Test
    void swrlRewritingTranslatesToSQL() {
        String sparql = """
                PREFIX fam: <http://example.org/family#>
                SELECT ?g ?a ?u WHERE { GRAPH ?g { ?a fam:hasUncle ?u } }
                """;
        Op quadOp = parseToQuadOp(sparql);

        SPARQLtoSQLTranslator translator =
                new SPARQLtoSQLTranslator(true, EntailmentRegime.NONE, reasoner.getRules());

        SQLQuery withoutRules = translator.buildSPARQLContext(quadOp);
        Op rewritten = new EntailmentRewriter(reasoner.getRules()).rewrite(quadOp);
        SQLQuery withRules = translator.buildSPARQLContext(rewritten);

        assertNotNull(withoutRules.getSql());
        assertNotNull(withRules.getSql());
        assertTrue(withRules.getSql().contains("UNION"),
                "SWRL expansion should union explicit and inferred results in SQL");
        assertTrue(withRules.getSql().length() > withoutRules.getSql().length(),
                "SWRL-expanded SQL should be larger than the plain translation");
    }

    /**
     * Recursively check if an Op tree contains a node of the given type.
     */
    private static boolean containsOpType(Op op, Class<? extends Op> type) {
        if (type.isInstance(op)) return true;
        return switch (op) {
            case Op2 op2 -> containsOpType(op2.getLeft(), type) || containsOpType(op2.getRight(), type);
            case Op1 op1 -> containsOpType(op1.getSubOp(), type);
            default -> false;
        };
    }
}
