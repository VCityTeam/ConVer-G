package fr.cnrs.liris.jpugetgil.converg.swrl;

import fr.cnrs.liris.jpugetgil.converg.entailment.EntailmentRegime;
import fr.cnrs.liris.jpugetgil.converg.inference.InferenceConfig;
import fr.cnrs.liris.jpugetgil.converg.inference.InferenceRule;
import fr.cnrs.liris.jpugetgil.converg.inference.SaturationSqlBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private static String saturationSql(List<InferenceRule> rules) {
        // SWRL only (no RDFS regime) so the SQL isolates the SWRL stratum
        return new SaturationSqlBuilder(true, new InferenceConfig(EntailmentRegime.NONE, true), rules)
                .buildWithPrefix();
    }

    @Test
    void verifierAcceptsSupportedRulesAndRejectsBuiltins() {
        assertTrue(reasoner.isEnabled(), "Reasoner should be enabled with verified rules");
        assertTrue(reasoner.getReport().consistent(), "Family ontology should be consistent");

        // "uncle" and "child" are supported, "adult" uses a builtin atom and is rejected
        assertEquals(2, reasoner.getReport().supportedRules().size(),
                "Two rules should pass verification");
        assertEquals(2, reasoner.getRules().size(),
                "Each supported single-head rule should yield one inference rule");
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
    void saturationExposesInferredRelation() {
        String sql = saturationSql(reasoner.getRules());
        assertTrue(sql.startsWith("WITH RECURSIVE"), "Saturation must be a WITH prefix");
        assertTrue(sql.contains("inf_quad AS ("),
                "Saturation must define the inf_quad relation scanned by the BGP");
        assertTrue(sql.contains("bit_or(validity)"),
                "Derivations of the same triple should be merged with bit_or over the version sets");
    }

    @Test
    void multiAtomRuleBecomesSelfJoinWithVersionIntersection() {
        // uncle: hasParent(x,y) ^ hasBrother(y,z) -> hasUncle(x,z)
        String sql = saturationSql(reasoner.getRules());
        assertTrue(sql.contains("http://example.org/family#hasUncle"),
                "The hasUncle head predicate should be produced");
        assertTrue(sql.contains("http://example.org/family#hasParent")
                        && sql.contains("http://example.org/family#hasBrother"),
                "Both body predicates should be matched");
        assertTrue(sql.contains("b0.validity & b1.validity"),
                "A two-atom body should intersect the two atoms' version sets");
        assertTrue(sql.contains("b0.id_named_graph = b1.id_named_graph"),
                "Body atoms of one rule should share a named graph");
        assertTrue(sql.contains("UNION ALL"),
                "The SWRL stratum should union the rule results onto the base relation");
    }

    @Test
    void inactiveConfigProducesNoPrefixAndScansBaseTable() {
        SaturationSqlBuilder inactive =
                new SaturationSqlBuilder(true, InferenceConfig.NONE, reasoner.getRules());
        assertFalse(inactive.isActive());
        assertEquals("", inactive.buildWithPrefix());
        assertEquals("versioned_quad", inactive.quadSourceRelation());
    }

    @Test
    void flatModeInferenceIsRejected() {
        SaturationSqlBuilder flat =
                new SaturationSqlBuilder(false, new InferenceConfig(EntailmentRegime.NONE, true), reasoner.getRules());
        assertThrows(IllegalStateException.class, flat::buildWithPrefix);
    }
}
