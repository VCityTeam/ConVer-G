package fr.vcity;

import org.apache.jena.riot.Lang;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class RDFStreamDeltaTest {

    private static final Pattern SKOLEM = Pattern.compile("urn:skolem:v\\d+:[^>]+");

    private static String resource(String name) {
        return RDFStreamDeltaTest.class.getClassLoader().getResource(name).getFile();
    }

    /** Returns every skolem IRI occurring in a line, in order (subject, then object, then graph). */
    private static List<String> skolems(String line) {
        List<String> out = new ArrayList<>();
        Matcher m = SKOLEM.matcher(line);
        while (m.find()) {
            out.add(m.group());
        }
        return out;
    }

    @Test
    void computesAdditionsAndDeletionsForQuads(@TempDir Path tmp) throws IOException {
        String additions = tmp.resolve("add.nq").toString();
        String deletions = tmp.resolve("del.nq").toString();

        RDFStreamDelta.computeDelta(
                resource("dataset-1.trig"), Lang.TRIG,
                resource("dataset-2.trig"), Lang.TRIG,
                additions, deletions);

        List<String> add = Files.readAllLines(Path.of(additions), StandardCharsets.UTF_8);
        List<String> del = Files.readAllLines(Path.of(deletions), StandardCharsets.UTF_8);

        // v2 changed building:2 height 9.1 -> 10 and removed building:2 width 6.
        assertEquals(1, add.size(), "expected one addition, got: " + add);
        assertTrue(add.get(0).contains("\"10\"^^"), "addition should be the new height: " + add);

        assertEquals(2, del.size(), "expected two deletions, got: " + del);
        assertTrue(del.stream().anyMatch(l -> l.contains("\"9.1\"^^")), "deletions should include old height: " + del);
        assertTrue(del.stream().anyMatch(l -> l.contains("schema.org/width")), "deletions should include removed width: " + del);
    }

    @Test
    void blankNodesAreAlwaysReportedAsChanged(@TempDir Path tmp) throws IOException {
        // Two versions whose only content is a blank-node statement that is structurally identical.
        // Because blank nodes are skolemized per version, the statement must appear in both deltas.
        Path v1 = tmp.resolve("v1.nt");
        Path v2 = tmp.resolve("v2.nt");
        Files.writeString(v1, "_:b0 <http://schema.org/name> \"Alice\" .\n", StandardCharsets.UTF_8);
        Files.writeString(v2, "_:b0 <http://schema.org/name> \"Alice\" .\n", StandardCharsets.UTF_8);

        String additions = tmp.resolve("add.nt").toString();
        String deletions = tmp.resolve("del.nt").toString();

        RDFStreamDelta.computeDelta(
                v1.toString(), Lang.NT,
                v2.toString(), Lang.NT,
                additions, deletions);

        List<String> add = Files.readAllLines(Path.of(additions), StandardCharsets.UTF_8);
        List<String> del = Files.readAllLines(Path.of(deletions), StandardCharsets.UTF_8);

        assertEquals(1, add.size(), "blank-node statement must be an addition in v2: " + add);
        assertEquals(1, del.size(), "blank-node statement must be a deletion in v1: " + del);
        assertTrue(add.get(0).contains("urn:skolem:v2:"), "addition should carry a v2 skolem IRI: " + add);
        assertTrue(del.get(0).contains("urn:skolem:v1:"), "deletion should carry a v1 skolem IRI: " + del);
    }

    @Test
    void identicalDatasetsProduceEmptyDelta(@TempDir Path tmp) throws IOException {
        String additions = tmp.resolve("add.nq").toString();
        String deletions = tmp.resolve("del.nq").toString();

        RDFStreamDelta.computeDelta(
                resource("dataset-1.trig"), Lang.TRIG,
                resource("dataset-1.trig"), Lang.TRIG,
                additions, deletions);

        assertEquals(0, Files.readAllLines(Path.of(additions)).size());
        assertEquals(0, Files.readAllLines(Path.of(deletions)).size());
    }

    @Test
    void blankNodeKeepsStableIdentityWithinAVersion(@TempDir Path tmp) throws IOException {
        // _:alice is referenced by two triples. Within a version it must skolemize to ONE IRI,
        // so intra-version references are preserved; across versions it must differ.
        String doc = ""
                + "_:alice <http://ex/knows> _:bob .\n"
                + "_:alice <http://ex/name> \"Alice\" .\n";
        Path v1 = tmp.resolve("v1.nt");
        Path v2 = tmp.resolve("v2.nt");
        Files.writeString(v1, doc, StandardCharsets.UTF_8);
        Files.writeString(v2, doc, StandardCharsets.UTF_8);

        String additions = tmp.resolve("add.nt").toString();
        String deletions = tmp.resolve("del.nt").toString();
        RDFStreamDelta.computeDelta(v1.toString(), Lang.NT, v2.toString(), Lang.NT, additions, deletions);

        List<String> del = Files.readAllLines(Path.of(deletions), StandardCharsets.UTF_8);
        List<String> add = Files.readAllLines(Path.of(additions), StandardCharsets.UTF_8);
        assertEquals(2, del.size(), "both blank-node triples must be deletions in v1: " + del);
        assertEquals(2, add.size(), "both blank-node triples must be additions in v2: " + add);

        String knows = del.stream().filter(l -> l.contains("ex/knows")).findFirst().orElseThrow();
        String name = del.stream().filter(l -> l.contains("ex/name")).findFirst().orElseThrow();

        String aliceFromKnows = skolems(knows).get(0); // subject
        String bobFromKnows = skolems(knows).get(1);   // object
        String aliceFromName = skolems(name).get(0);   // subject

        assertEquals(aliceFromName, aliceFromKnows, "_:alice must map to the same IRI across both triples");
        assertNotEquals(aliceFromKnows, bobFromKnows, "_:alice and _:bob must map to different IRIs");
        assertTrue(aliceFromKnows.startsWith("urn:skolem:v1:"), "v1 deletions must use the v1 prefix");

        // The same identity must NOT carry over to v2.
        String addAlice = skolems(add.stream().filter(l -> l.contains("ex/name")).findFirst().orElseThrow()).get(0);
        assertTrue(addAlice.startsWith("urn:skolem:v2:"), "v2 additions must use the v2 prefix");
        assertNotEquals(aliceFromName, addAlice, "the blank node must differ between versions");
    }

    @Test
    void onlyBlankNodeStatementsAppearWhenGroundStatementsAreUnchanged(@TempDir Path tmp) throws IOException {
        // A ground (IRI-only) triple is identical in both versions and must NOT appear in the delta;
        // the structurally-identical blank-node triple must appear in both deltas.
        String doc = ""
                + "<http://ex/s> <http://ex/p> \"ground\" .\n"
                + "_:b0 <http://ex/p> \"blank\" .\n";
        Path v1 = tmp.resolve("v1.nt");
        Path v2 = tmp.resolve("v2.nt");
        Files.writeString(v1, doc, StandardCharsets.UTF_8);
        Files.writeString(v2, doc, StandardCharsets.UTF_8);

        String additions = tmp.resolve("add.nt").toString();
        String deletions = tmp.resolve("del.nt").toString();
        RDFStreamDelta.computeDelta(v1.toString(), Lang.NT, v2.toString(), Lang.NT, additions, deletions);

        List<String> add = Files.readAllLines(Path.of(additions), StandardCharsets.UTF_8);
        List<String> del = Files.readAllLines(Path.of(deletions), StandardCharsets.UTF_8);

        assertEquals(1, add.size(), "only the blank-node triple should be an addition: " + add);
        assertEquals(1, del.size(), "only the blank-node triple should be a deletion: " + del);
        assertTrue(add.get(0).contains("\"blank\""), "addition must be the blank-node triple: " + add);
        assertTrue(del.get(0).contains("\"blank\""), "deletion must be the blank-node triple: " + del);
        assertFalse(add.get(0).contains("\"ground\""), "the unchanged ground triple must not appear");
        assertFalse(del.get(0).contains("\"ground\""), "the unchanged ground triple must not appear");
    }

    @Test
    void blankNodeAsGraphNameIsVersioned(@TempDir Path tmp) throws IOException {
        // A quad whose graph label is a blank node: the graph is skolemized per version, so the
        // quad is always part of the delta even though subject/predicate/object are ground.
        String doc = "<http://ex/s> <http://ex/p> <http://ex/o> _:g .\n";
        Path v1 = tmp.resolve("v1.nq");
        Path v2 = tmp.resolve("v2.nq");
        Files.writeString(v1, doc, StandardCharsets.UTF_8);
        Files.writeString(v2, doc, StandardCharsets.UTF_8);

        String additions = tmp.resolve("add.nq").toString();
        String deletions = tmp.resolve("del.nq").toString();
        RDFStreamDelta.computeDelta(v1.toString(), Lang.NQUADS, v2.toString(), Lang.NQUADS, additions, deletions);

        List<String> add = Files.readAllLines(Path.of(additions), StandardCharsets.UTF_8);
        List<String> del = Files.readAllLines(Path.of(deletions), StandardCharsets.UTF_8);

        assertEquals(1, add.size(), "quad with blank graph must be an addition: " + add);
        assertEquals(1, del.size(), "quad with blank graph must be a deletion: " + del);
        assertTrue(add.get(0).contains("urn:skolem:v2:"), "addition graph must be a v2 skolem IRI: " + add);
        assertTrue(del.get(0).contains("urn:skolem:v1:"), "deletion graph must be a v1 skolem IRI: " + del);
    }

    @Test
    void groundChangeAlongsideAnUnchangedBlankNodeStatement(@TempDir Path tmp) throws IOException {
        // v1 and v2 share a blank-node triple (always in the delta) and differ on a ground triple.
        String v1Doc = ""
                + "_:b0 <http://ex/p> \"shared-blank\" .\n"
                + "<http://ex/s> <http://ex/age> \"30\" .\n";
        String v2Doc = ""
                + "_:b0 <http://ex/p> \"shared-blank\" .\n"
                + "<http://ex/s> <http://ex/age> \"31\" .\n";
        Path v1 = tmp.resolve("v1.nt");
        Path v2 = tmp.resolve("v2.nt");
        Files.writeString(v1, v1Doc, StandardCharsets.UTF_8);
        Files.writeString(v2, v2Doc, StandardCharsets.UTF_8);

        String additions = tmp.resolve("add.nt").toString();
        String deletions = tmp.resolve("del.nt").toString();
        RDFStreamDelta.computeDelta(v1.toString(), Lang.NT, v2.toString(), Lang.NT, additions, deletions);

        List<String> add = Files.readAllLines(Path.of(additions), StandardCharsets.UTF_8);
        List<String> del = Files.readAllLines(Path.of(deletions), StandardCharsets.UTF_8);

        // Each side: the changed ground triple + the (per-version distinct) blank-node triple.
        assertEquals(2, add.size(), "additions: new age + v2 blank triple: " + add);
        assertEquals(2, del.size(), "deletions: old age + v1 blank triple: " + del);
        assertTrue(add.stream().anyMatch(l -> l.contains("\"31\"")), "additions must include the new age");
        assertTrue(del.stream().anyMatch(l -> l.contains("\"30\"")), "deletions must include the old age");
        assertTrue(add.stream().anyMatch(l -> l.contains("\"shared-blank\"") && l.contains("urn:skolem:v2:")));
        assertTrue(del.stream().anyMatch(l -> l.contains("\"shared-blank\"") && l.contains("urn:skolem:v1:")));
    }
}
