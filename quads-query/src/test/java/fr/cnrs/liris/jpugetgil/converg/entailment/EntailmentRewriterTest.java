package fr.cnrs.liris.jpugetgil.converg.entailment;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EntailmentRewriterTest {

    private final EntailmentRewriter rdfsRewriter = new EntailmentRewriter(RDFSRules.allRules());

    /**
     * Parse a SPARQL query and convert to quad-form Op.
     */
    private static Op parseToQuadOp(String sparql) {
        Query query = QueryFactory.create(sparql);
        Op op = Algebra.compile(query);
        return Algebra.toQuadForm(op);
    }

    @Test
    void noEntailmentOnDefaultGraph() {
        // Default graph patterns should NOT be rewritten
        Op op = parseToQuadOp("SELECT ?s ?p ?o WHERE { ?s ?p ?o }");
        Op rewritten = rdfsRewriter.rewrite(op);
        // The structure should be unchanged (default graph quad pattern)
        assertEquals(op, rewritten, "Default graph patterns should not be rewritten");
    }

    @Test
    void rdfTypePatternTriggersRdfs9Expansion() {
        // A ?x rdf:type ?C pattern in a GRAPH should be expanded with subClassOf
        Op op = parseToQuadOp(
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT ?g ?x ?t WHERE { GRAPH ?g { ?x rdf:type ?t } }");
        Op rewritten = rdfsRewriter.rewrite(op);

        // The rewritten op should NOT be the same as the original
        assertNotEquals(op, rewritten, "rdf:type pattern should be expanded by entailment rules");

        // The top-level should be a Project wrapping something that contains OpUnion
        assertInstanceOf(OpProject.class, rewritten, "Top level should still be OpProject");
        Op inner = ((OpProject) rewritten).getSubOp();

        // The inner op should contain unions (explicit UNION rdfs9 UNION rdfs2 UNION rdfs3)
        assertTrue(containsOpType(inner, OpUnion.class),
                "Rewritten tree should contain OpUnion for explicit + inferred results");
    }

    @Test
    void concretePredicateTriggersRdfs7() {
        // A pattern with a concrete non-type predicate should trigger rdfs7
        Op op = parseToQuadOp(
                "PREFIX ex: <http://example.edu/>\n" +
                "SELECT ?g ?s ?o WHERE { GRAPH ?g { ?s ex:teaches ?o } }");
        Op rewritten = rdfsRewriter.rewrite(op);

        assertNotEquals(op, rewritten, "Concrete predicate pattern should trigger rdfs7");

        Op inner = ((OpProject) rewritten).getSubOp();
        assertTrue(containsOpType(inner, OpUnion.class),
                "Should contain OpUnion for explicit + subPropertyOf expansion");
        assertTrue(containsOpType(inner, OpTransitiveClosure.class),
                "Should contain OpTransitiveClosure for rdfs:subPropertyOf*");
    }

    @Test
    void rdfs9ExpansionContainsTransitiveClosure() {
        // The rdfs9 expansion should produce an OpTransitiveClosure for subClassOf*
        Op op = parseToQuadOp(
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT ?g ?x ?t WHERE { GRAPH ?g { ?x rdf:type ?t } }");
        Op rewritten = rdfsRewriter.rewrite(op);

        assertTrue(containsOpType(rewritten, OpTransitiveClosure.class),
                "rdfs9 expansion should include OpTransitiveClosure for subClassOf*");
    }

    @Test
    void freshVariablesDoNotConflict() {
        // Fresh variables should be prefixed with __ent_
        FreshVariableAllocator allocator = new FreshVariableAllocator();
        var v1 = allocator.allocate("sub");
        var v2 = allocator.allocate("sub");

        assertTrue(v1.getName().startsWith("__ent_sub_"),
                "Fresh var should be prefixed with __ent_");
        assertNotEquals(v1.getName(), v2.getName(),
                "Each allocation should produce a unique name");
    }

    @Test
    void schemaPredicatesAreNotExpandedByRdfs7() {
        // rdfs:subClassOf, rdfs:domain etc. should NOT trigger rdfs7
        Op op = parseToQuadOp(
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "SELECT ?g ?s ?o WHERE { GRAPH ?g { ?s rdfs:subClassOf ?o } }");
        Op rewritten = rdfsRewriter.rewrite(op);

        // rdfs:subClassOf is a schema property, rdfs7 should not apply
        assertFalse(containsOpType(rewritten, OpTransitiveClosure.class),
                "Schema predicates like rdfs:subClassOf should not trigger rdfs7 subPropertyOf expansion");
    }

    @Test
    void variablePredicateIsNotExpandedByRdfs7() {
        // ?s ?p ?o with variable predicate should NOT trigger rdfs7 (needs concrete URI)
        Op op = parseToQuadOp(
                "SELECT ?g ?s ?p ?o WHERE { GRAPH ?g { ?s ?p ?o } }");
        Op rewritten = rdfsRewriter.rewrite(op);

        // Variable predicate: only rdf:type rules don't apply, rdfs7 requires concrete predicate
        // This should be unmodified since ?p is a variable (not a concrete URI)
        assertFalse(containsOpType(rewritten, OpTransitiveClosure.class),
                "Variable predicate should not trigger rdfs7");
    }

    @Test
    void multiTripleBGPSplitsCorrectly() {
        // A BGP with a type pattern and a non-type pattern should split:
        // the type pattern gets expanded, the other stays as-is, joined together
        Op op = parseToQuadOp(
                """
                        PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                        PREFIX ex: <http://example.edu/>
                        SELECT ?g ?x ?t ?course WHERE {
                            GRAPH ?g {
                                ?x rdf:type ?t .
                                ?x ex:enrolledIn ?course .
                            }
                        }""");
        Op rewritten = rdfsRewriter.rewrite(op);

        assertNotEquals(op, rewritten);

        // Should have both OpUnion (for entailment) and OpJoin (for BGP recombination)
        Op inner = ((OpProject) rewritten).getSubOp();
        assertTrue(containsOpType(inner, OpUnion.class),
                "Should contain union for entailed type pattern");
        assertTrue(containsOpType(inner, OpJoin.class),
                "Should contain join to recombine split BGP");
    }

    @Test
    void rewritePreservesProjection() {
        Op op = parseToQuadOp(
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT ?g ?x ?t WHERE { GRAPH ?g { ?x rdf:type ?t } }");
        Op rewritten = rdfsRewriter.rewrite(op);

        assertInstanceOf(OpProject.class, rewritten);
        OpProject proj = (OpProject) rewritten;
        assertEquals(3, proj.getVars().size(), "Projection should preserve original 3 variables");
    }

    @Test
    void emptyRulesProducesNoChange() {
        EntailmentRewriter noOpRewriter = new EntailmentRewriter(List.of());
        Op op = parseToQuadOp(
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT ?g ?x ?t WHERE { GRAPH ?g { ?x rdf:type ?t } }");
        Op rewritten = noOpRewriter.rewrite(op);
        assertEquals(op, rewritten, "No rules should mean no rewriting");
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
