package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.entailment.EntailmentRegime;
import fr.cnrs.liris.jpugetgil.converg.entailment.OpTransitiveClosure;
import fr.cnrs.liris.jpugetgil.converg.entailment.RDFSRules;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransitiveClosureSQLOperatorTest {

    private static final Node GRAPH_VAR = Var.alloc("g");
    private static final Node START_VAR = Var.alloc("sub");
    private static final Node END_VAR = Var.alloc("super");
    private static final Node END_URI = NodeFactory.createURI("http://example.edu/Person");

    private static SQLContext condensedContext() {
        Map<Node, List<SPARQLOccurrence>> occurrences = new HashMap<>();
        return new SQLContext(occurrences, true, EntailmentRegime.RDFS, null, null);
    }

    private static SQLContext flatContext() {
        Map<Node, List<SPARQLOccurrence>> occurrences = new HashMap<>();
        return new SQLContext(occurrences, false, EntailmentRegime.RDFS, null, null);
    }

    @Test
    void condensedModeGeneratesRecursiveCTE() {
        OpTransitiveClosure opTC = new OpTransitiveClosure(
                GRAPH_VAR, START_VAR, END_VAR, RDFSRules.RDFS_SUBCLASS_OF);

        TransitiveClosureSQLOperator operator = new TransitiveClosureSQLOperator(opTC, condensedContext());
        SQLQuery result = operator.buildSQLQuery();

        String sql = result.getSql();
        assertNotNull(sql);

        // Must contain recursive CTE
        assertTrue(sql.contains("WITH RECURSIVE closure"),
                "Condensed mode should generate recursive CTE");

        // Must reference versioned_quad table
        assertTrue(sql.contains("versioned_quad"),
                "Should query versioned_quad table");
        assertFalse(sql.contains("versioned_quad_flat"),
                "Should NOT query versioned_quad_flat in condensed mode");

        // Must contain validity intersection
        assertTrue(sql.contains("c.validity & t1.validity"),
                "Recursive step must intersect validity bitsets (conjunctive version set semantics)");

        // Must check bit_count for non-empty intersection
        assertTrue(sql.contains("bit_count"),
                "Should filter out empty intersections with bit_count");

        // Must contain predicate filter for rdfs:subClassOf
        assertTrue(sql.contains(RDFSRules.RDFS_SUBCLASS_OF),
                "Should filter on rdfs:subClassOf predicate");
    }

    @Test
    void condensedModeOutputsGraphVariableAsCondensed() {
        OpTransitiveClosure opTC = new OpTransitiveClosure(
                GRAPH_VAR, START_VAR, END_VAR, RDFSRules.RDFS_SUBCLASS_OF);

        TransitiveClosureSQLOperator operator = new TransitiveClosureSQLOperator(opTC, condensedContext());
        SQLQuery result = operator.buildSQLQuery();

        String sql = result.getSql();

        // Graph variable should be output as condensed (ng$ + bs$)
        assertTrue(sql.contains("ng$g"), "Should output ng$g for graph variable");
        assertTrue(sql.contains("bs$g"), "Should output bs$g for graph variable");

        // Start/end should be output as ID (v$)
        assertTrue(sql.contains("v$sub"), "Should output v$sub for start variable");
        assertTrue(sql.contains("v$super"), "Should output v$super for end variable");
    }

    @Test
    void flatModeGeneratesRecursiveCTE() {
        OpTransitiveClosure opTC = new OpTransitiveClosure(
                GRAPH_VAR, START_VAR, END_VAR, RDFSRules.RDFS_SUBCLASS_OF);

        TransitiveClosureSQLOperator operator = new TransitiveClosureSQLOperator(opTC, flatContext());
        SQLQuery result = operator.buildSQLQuery();

        String sql = result.getSql();
        assertNotNull(sql);

        assertTrue(sql.contains("WITH RECURSIVE closure"),
                "Flat mode should also generate recursive CTE");
        assertTrue(sql.contains("versioned_quad_flat"),
                "Should query versioned_quad_flat table");
        assertFalse(sql.contains("validity"),
                "Flat mode should not use validity bitsets");
    }

    @Test
    void concreteEndNodeFiltersInSQL() {
        // When the end node is a concrete URI, the SQL should filter on it
        OpTransitiveClosure opTC = new OpTransitiveClosure(
                GRAPH_VAR, START_VAR, END_URI, RDFSRules.RDFS_SUBCLASS_OF);

        TransitiveClosureSQLOperator operator = new TransitiveClosureSQLOperator(opTC, condensedContext());
        SQLQuery result = operator.buildSQLQuery();

        String sql = result.getSql();

        assertTrue(sql.contains("http://example.edu/Person"),
                "Should filter on the concrete end URI");
        // Should NOT output v$super since end is a URI, not a variable
        assertFalse(sql.contains("v$super"),
                "Should not project a variable column for a concrete URI end node");
    }

    @Test
    void subPropertyOfClosureUsesCorrectPredicate() {
        OpTransitiveClosure opTC = new OpTransitiveClosure(
                GRAPH_VAR, START_VAR, END_VAR, RDFSRules.RDFS_SUBPROPERTY_OF);

        TransitiveClosureSQLOperator operator = new TransitiveClosureSQLOperator(opTC, condensedContext());
        SQLQuery result = operator.buildSQLQuery();

        String sql = result.getSql();

        assertTrue(sql.contains(RDFSRules.RDFS_SUBPROPERTY_OF),
                "Should filter on rdfs:subPropertyOf predicate");
        assertFalse(sql.contains(RDFSRules.RDFS_SUBCLASS_OF),
                "Should NOT contain rdfs:subClassOf");
    }

    @Test
    void contextHasCorrectVariableOccurrences() {
        OpTransitiveClosure opTC = new OpTransitiveClosure(
                GRAPH_VAR, START_VAR, END_VAR, RDFSRules.RDFS_SUBCLASS_OF);

        TransitiveClosureSQLOperator operator = new TransitiveClosureSQLOperator(opTC, condensedContext());
        SQLQuery result = operator.buildSQLQuery();

        SQLContext ctx = result.getContext();
        Map<Node, List<SPARQLOccurrence>> occurrences = ctx.sparqlVarOccurrences();

        // Should have entries for start, end, and graph variables
        assertTrue(occurrences.containsKey(START_VAR), "Should have occurrence for start variable");
        assertTrue(occurrences.containsKey(END_VAR), "Should have occurrence for end variable");
        assertTrue(occurrences.containsKey(GRAPH_VAR), "Should have occurrence for graph variable");
        assertEquals(3, occurrences.size(), "Should have exactly 3 variable occurrences");
    }
}
