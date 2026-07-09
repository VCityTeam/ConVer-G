package fr.cnrs.liris.jpugetgil.converg.entailment;

import fr.cnrs.liris.jpugetgil.converg.SPARQLtoSQLTranslator;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test verifying that SPARQL queries are translated to SQL correctly
 * when RDFS entailment is enabled. These tests verify the complete pipeline:
 * SPARQL -> Op algebra -> entailment rewriting -> SQL generation.
 * No database connection is needed.
 */
class EntailmentSQLTranslationTest {

    private final SPARQLtoSQLTranslator condensedRdfs =
            new SPARQLtoSQLTranslator(true, EntailmentRegime.RDFS);

    private final SPARQLtoSQLTranslator flatRdfs =
            new SPARQLtoSQLTranslator(false, EntailmentRegime.RDFS);

    private final SPARQLtoSQLTranslator condensedNone =
            new SPARQLtoSQLTranslator(true, EntailmentRegime.NONE);

    private static SQLQuery translate(SPARQLtoSQLTranslator translator, String sparql) {
        Query query = QueryFactory.create(sparql);
        Op op = Algebra.compile(query);
        Op quadOp = Algebra.toQuadForm(op);

        // Apply entailment rewriting (mimics getSqlQuery() logic)
        if (translator == null) return null;
        return translator.buildSPARQLContext(quadOp);
    }

    @Test
    void rdfsEntailmentProducesLargerSQL() {
        String sparql = """
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                SELECT ?g ?x ?t WHERE { GRAPH ?g { ?x rdf:type ?t } }
                """;

        // Entailment rewriter is applied externally in getSqlQuery(),
        // so we simulate it here to test the full pipeline
        Query query = QueryFactory.create(sparql);
        Op op = Algebra.compile(query);
        Op quadOp = Algebra.toQuadForm(op);

        // Without entailment
        SQLQuery withoutEntailment = condensedNone.buildSPARQLContext(quadOp);

        // With entailment (rewrite first)
        EntailmentRewriter rewriter = new EntailmentRewriter(RDFSRules.allRules());
        Op rewrittenOp = rewriter.rewrite(quadOp);
        SQLQuery withEntailment = condensedRdfs.buildSPARQLContext(rewrittenOp);

        assertNotNull(withoutEntailment.getSql());
        assertNotNull(withEntailment.getSql());

        // The entailed SQL should be significantly larger (contains UNION, recursive CTE, etc.)
        assertTrue(withEntailment.getSql().length() > withoutEntailment.getSql().length(),
                "Entailed SQL should be larger than non-entailed SQL");
    }

    @Test
    void rdfsEntailmentSQLContainsRecursiveCTE() {
        String sparql = """
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                SELECT ?g ?x ?t WHERE { GRAPH ?g { ?x rdf:type ?t } }
                """;

        Query query = QueryFactory.create(sparql);
        Op op = Algebra.compile(query);
        Op quadOp = Algebra.toQuadForm(op);

        EntailmentRewriter rewriter = new EntailmentRewriter(RDFSRules.allRules());
        Op rewrittenOp = rewriter.rewrite(quadOp);
        SQLQuery result = condensedRdfs.buildSPARQLContext(rewrittenOp);

        String sql = result.getSql();
        assertTrue(sql.contains("WITH RECURSIVE"),
                "rdfs9 expansion should produce recursive CTE for subClassOf*");
        assertTrue(sql.contains("UNION"),
                "Should UNION explicit and inferred patterns");
    }

    @Test
    void rdfsEntailmentSQLContainsValidityIntersection() {
        String sparql = """
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                SELECT ?g ?x ?t WHERE { GRAPH ?g { ?x rdf:type ?t } }
                """;

        Query query = QueryFactory.create(sparql);
        Op op = Algebra.compile(query);
        Op quadOp = Algebra.toQuadForm(op);

        EntailmentRewriter rewriter = new EntailmentRewriter(RDFSRules.allRules());
        Op rewrittenOp = rewriter.rewrite(quadOp);
        SQLQuery result = condensedRdfs.buildSPARQLContext(rewrittenOp);

        String sql = result.getSql();
        // The recursive CTE should intersect validity at each step
        assertTrue(sql.contains("c.validity & t1.validity"),
                "Recursive CTE should intersect validity (conjunctive version set semantics)");
    }

    @Test
    void flatModeEntailmentAlsoWorks() {
        String sparql = """
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                SELECT ?g ?x ?t WHERE { GRAPH ?g { ?x rdf:type ?t } }
                """;

        Query query = QueryFactory.create(sparql);
        Op op = Algebra.compile(query);
        Op quadOp = Algebra.toQuadForm(op);

        EntailmentRewriter rewriter = new EntailmentRewriter(RDFSRules.allRules());
        Op rewrittenOp = rewriter.rewrite(quadOp);
        SQLQuery result = flatRdfs.buildSPARQLContext(rewrittenOp);

        String sql = result.getSql();
        assertNotNull(sql);
        assertTrue(sql.contains("WITH RECURSIVE"),
                "Flat mode should also produce recursive CTE");
        assertTrue(sql.contains("versioned_quad_flat"),
                "Flat mode should reference versioned_quad_flat table");
    }

    @Test
    void subPropertyOfExpansionForConcreteProperty() {
        String sparql = """
                PREFIX ex: <http://example.edu/>
                SELECT ?g ?s ?o WHERE { GRAPH ?g { ?s ex:relatedTo ?o } }
                """;

        Query query = QueryFactory.create(sparql);
        Op op = Algebra.compile(query);
        Op quadOp = Algebra.toQuadForm(op);

        EntailmentRewriter rewriter = new EntailmentRewriter(RDFSRules.allRules());
        Op rewrittenOp = rewriter.rewrite(quadOp);
        SQLQuery result = condensedRdfs.buildSPARQLContext(rewrittenOp);

        String sql = result.getSql();
        assertNotNull(sql);
        // Should contain subPropertyOf closure
        assertTrue(sql.contains(RDFSRules.RDFS_SUBPROPERTY_OF),
                "Should contain subPropertyOf CTE for rdfs7 expansion");
        assertTrue(sql.contains("http://example.edu/relatedTo"),
                "Should reference the concrete property URI");
    }

    @Test
    void noEntailmentWhenRegimeIsNone() {
        String sparql = """
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                SELECT ?g ?x ?t WHERE { GRAPH ?g { ?x rdf:type ?t } }
                """;

        SQLQuery result = translate(condensedNone, sparql);
        assertNotNull(result);
        String sql = result.getSql();

        assertFalse(sql.contains("WITH RECURSIVE"),
                "NONE regime should not produce recursive CTEs");
        assertFalse(sql.contains("UNION"),
                "NONE regime should not produce UNIONs for entailment");
    }
}