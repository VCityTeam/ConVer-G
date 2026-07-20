package fr.cnrs.liris.jpugetgil.converg;

import fr.cnrs.liris.jpugetgil.converg.entailment.EntailmentRegime;
import fr.cnrs.liris.jpugetgil.converg.inference.InferenceConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The inference mode is described from an {@link InferenceConfig}; it feeds the
 * {@code $/server} version string and is resolved per query from {@code ?infer=}.
 */
class InferenceModeTest {

    @Test
    void noInferenceIsEmpty() {
        assertEquals("", new InferenceConfig(EntailmentRegime.NONE, false).describe());
        assertFalse(new InferenceConfig(EntailmentRegime.NONE, false).isEnabled());
    }

    @Test
    void entailmentRegimeOnly() {
        assertEquals("RDFS", new InferenceConfig(EntailmentRegime.RDFS, false).describe());
        assertEquals("OWL_LITE", new InferenceConfig(EntailmentRegime.OWL_LITE, false).describe());
    }

    @Test
    void swrlOnly() {
        assertEquals("SWRL", new InferenceConfig(EntailmentRegime.NONE, true).describe());
    }

    @Test
    void entailmentRegimeCombinedWithSwrl() {
        assertEquals("RDFS+SWRL", new InferenceConfig(EntailmentRegime.RDFS, true).describe());
        assertEquals("OWL_LITE+SWRL", new InferenceConfig(EntailmentRegime.OWL_LITE, true).describe());
    }

    @Test
    void absentParameterFallsBackToServerDefault() {
        InferenceConfig serverDefault = new InferenceConfig(EntailmentRegime.RDFS, true);
        assertEquals(serverDefault, InferenceConfig.resolve(null, serverDefault, true));
        assertEquals(serverDefault, InferenceConfig.resolve("  ", serverDefault, true));
    }

    @Test
    void parameterOverridesServerDefaultPerQuery() {
        InferenceConfig serverDefault = new InferenceConfig(EntailmentRegime.RDFS, true);

        // explicit opt-out
        assertEquals("", InferenceConfig.resolve("off", serverDefault, true).describe());
        assertEquals("", InferenceConfig.resolve("none", serverDefault, true).describe());

        // explicit selection
        assertEquals("RDFS", InferenceConfig.resolve("rdfs", serverDefault, true).describe());
        assertEquals("SWRL", InferenceConfig.resolve("swrl", serverDefault, true).describe());
        assertEquals("RDFS+SWRL", InferenceConfig.resolve("rdfs+swrl", serverDefault, true).describe());
        assertEquals("OWL_LITE", InferenceConfig.resolve("owl", serverDefault, true).describe());
    }

    @Test
    void swrlTokenIsInertWhenNoRulesAvailable() {
        InferenceConfig serverDefault = InferenceConfig.NONE;
        assertFalse(InferenceConfig.resolve("swrl", serverDefault, false).swrl());
        assertSame(EntailmentRegime.RDFS, InferenceConfig.resolve("rdfs+swrl", serverDefault, false).regime());
        assertFalse(InferenceConfig.resolve("rdfs+swrl", serverDefault, false).swrl());
    }
}
