package fr.cnrs.liris.jpugetgil.converg;

import fr.cnrs.liris.jpugetgil.converg.entailment.EntailmentRegime;
import org.junit.jupiter.api.Test;

import static fr.cnrs.liris.jpugetgil.converg.VersioningQueryExecution.describeInferenceMode;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InferenceModeTest {

    @Test
    void noInferenceIsEmpty() {
        assertEquals("", describeInferenceMode(EntailmentRegime.NONE, false));
    }

    @Test
    void entailmentRegimeOnly() {
        assertEquals("RDFS", describeInferenceMode(EntailmentRegime.RDFS, false));
        assertEquals("OWL_LITE", describeInferenceMode(EntailmentRegime.OWL_LITE, false));
    }

    @Test
    void swrlOnly() {
        assertEquals("SWRL", describeInferenceMode(EntailmentRegime.NONE, true));
    }

    @Test
    void entailmentRegimeCombinedWithSwrl() {
        assertEquals("RDFS+SWRL", describeInferenceMode(EntailmentRegime.RDFS, true));
        assertEquals("OWL_LITE+SWRL", describeInferenceMode(EntailmentRegime.OWL_LITE, true));
    }
}
