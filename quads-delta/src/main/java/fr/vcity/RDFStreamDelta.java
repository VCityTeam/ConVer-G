package fr.vcity;

import org.apache.jena.atlas.io.IndentedLineBuffer;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.out.NodeFormatter;
import org.apache.jena.riot.out.NodeFormatterNT;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.jena.sparql.core.Quad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Computes the delta (additions and deletions) between two RDF datasets in <b>stream mode</b>.
 * <p>
 * Unlike {@link RDFDelta}, this program never holds both versions in memory at once. Each input
 * is parsed as a stream of statements, canonicalised to one N-Triples/N-Quads line per statement,
 * spilled to disk, and {@linkplain #externalSort externally sorted} with a bounded amount of
 * memory. The two sorted streams are then merge-joined to emit the delta.
 * <p>
 * Blank nodes are skolemized with a <b>version-specific</b> IRI prefix
 * ({@code urn:skolem:v1:} vs {@code urn:skolem:v2:}). A blank node keeps a stable identity
 * <i>within</i> a version (so intra-version references are preserved) but can never match the
 * "same" blank node in the other version. As a result, any statement involving a blank node is
 * always reported in the delta (as both a deletion in v1 and an addition in v2).
 */
public class RDFStreamDelta {
    private static final Logger log = LoggerFactory.getLogger(RDFStreamDelta.class);

    /** Number of lines sorted in memory per run during the external sort. Override with -Ddelta.chunkLines. */
    private static final int CHUNK_LINES = Integer.getInteger("delta.chunkLines", 500_000);

    public static void main(String[] args) {
        if (args.length < 2) {
            log.error("Usage: java -cp <jar> fr.vcity.RDFStreamDelta <file1> <file2>");
            System.exit(1);
        }
        String dataDir = "/data" + File.separator;
        String file1 = dataDir + args[0];
        String file2 = dataDir + args[1];

        Lang lang1 = validateAndGetLang(file1);
        Lang lang2 = validateAndGetLang(file2);
        validateCompatibleLangs(lang1, lang2);

        boolean isTriples = RDFLanguages.isTriples(lang1);
        String[] outFiles = getOutputFilenames(args[0], args[1], dataDir, isTriples);

        try {
            computeDelta(file1, lang1, file2, lang2, outFiles[0], outFiles[1]);
        } catch (IOException e) {
            log.error("Error computing delta: " + e.getMessage(), e);
            System.exit(3);
        }

        log.info("Output written to " + outFiles[0] + " and " + outFiles[1]);
    }

    /**
     * Computes the streaming delta between two RDF files and writes the additions / deletions.
     *
     * @param file1        path to the previous version (v1)
     * @param lang1        RDF serialization of v1
     * @param file2        path to the new version (v2)
     * @param lang2        RDF serialization of v2
     * @param additionsOut output file for statements present in v2 but not in v1
     * @param deletionsOut output file for statements present in v1 but not in v2
     */
    static void computeDelta(String file1, Lang lang1, String file2, Lang lang2,
                             String additionsOut, String deletionsOut) throws IOException {
        File raw1 = File.createTempFile("delta-v1-raw-", ".lines");
        File raw2 = File.createTempFile("delta-v2-raw-", ".lines");
        File sorted1 = File.createTempFile("delta-v1-sorted-", ".lines");
        File sorted2 = File.createTempFile("delta-v2-sorted-", ".lines");
        try {
            // 1. Stream-parse + skolemize + canonicalise to one line per statement.
            normalize(file1, lang1, "v1", raw1);
            normalize(file2, lang2, "v2", raw2);

            // 2. External sort (bounded memory) so we can merge-join.
            externalSort(raw1, sorted1);
            externalSort(raw2, sorted2);

            // 3. Merge-join the two sorted streams to emit the delta.
            mergeDelta(sorted1, sorted2, additionsOut, deletionsOut);
        } finally {
            deleteQuietly(raw1);
            deleteQuietly(raw2);
            deleteQuietly(sorted1);
            deleteQuietly(sorted2);
        }
    }

    /**
     * Streams the RDF file, skolemizing blank nodes with the given version tag, and writes one
     * canonical N-Triples/N-Quads line per statement to {@code out}.
     */
    private static void normalize(String filePath, Lang lang, String version, File out) throws IOException {
        try (BufferedWriter writer = newWriter(out)) {
            StreamRDFBase sink = new StreamRDFBase() {
                private final NodeFormatter fmt = new NodeFormatterNT();

                @Override
                public void triple(Triple triple) {
                    writeLine(triple.getSubject(), triple.getPredicate(), triple.getObject(), null);
                }

                @Override
                public void quad(Quad quad) {
                    writeLine(quad.getSubject(), quad.getPredicate(), quad.getObject(), quad.getGraph());
                }

                private void writeLine(Node s, Node p, Node o, Node g) {
                    IndentedLineBuffer buf = new IndentedLineBuffer();
                    fmt.format(buf, skolem(s, version));
                    buf.print(" ");
                    fmt.format(buf, skolem(p, version));
                    buf.print(" ");
                    fmt.format(buf, skolem(o, version));
                    if (g != null && !Quad.isDefaultGraph(g)) {
                        buf.print(" ");
                        fmt.format(buf, skolem(g, version));
                    }
                    buf.print(" .");
                    try {
                        writer.write(buf.asString());
                        writer.write('\n');
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            try {
                RDFParser.source(filePath).lang(lang).parse(sink);
            } catch (RuntimeException e) {
                if (e.getCause() instanceof IOException ioe) {
                    throw ioe;
                }
                throw e;
            }
        }
    }

    /**
     * Replaces a blank node with a version-scoped Skolem IRI so it is unique per version. The label
     * is stable within a single parse, so intra-version references to the same blank node map to the
     * same IRI; the version prefix guarantees it never matches the other version.
     */
    private static Node skolem(Node n, String version) {
        if (n != null && n.isBlank()) {
            return NodeFactory.createURI("urn:skolem:" + version + ":" + n.getBlankNodeLabel());
        }
        return n;
    }

    /**
     * Sorts {@code input} into {@code output} using a bounded amount of memory: lines are read in
     * chunks of {@link #CHUNK_LINES}, each chunk is sorted in memory and written to a run file, and
     * the runs are then k-way merged.
     */
    private static void externalSort(File input, File output) throws IOException {
        List<File> runs = new ArrayList<>();
        try (BufferedReader reader = newReader(input)) {
            List<String> chunk = new ArrayList<>(Math.min(CHUNK_LINES, 1 << 16));
            String line;
            boolean eof = false;
            while (!eof) {
                line = reader.readLine();
                if (line == null) {
                    eof = true;
                } else {
                    chunk.add(line);
                }
                if (chunk.size() >= CHUNK_LINES || (eof && !chunk.isEmpty())) {
                    Collections.sort(chunk);
                    runs.add(writeRun(chunk));
                    chunk.clear();
                }
            }
        }

        if (runs.isEmpty()) {
            // Empty input: produce an empty sorted file.
            Files.write(output.toPath(), new byte[0]);
            return;
        }
        if (runs.size() == 1) {
            Files.move(runs.get(0).toPath(), output.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        kWayMerge(runs, output);
        for (File run : runs) {
            deleteQuietly(run);
        }
    }

    private static File writeRun(List<String> sortedChunk) throws IOException {
        File run = File.createTempFile("delta-run-", ".lines");
        try (BufferedWriter writer = newWriter(run)) {
            for (String l : sortedChunk) {
                writer.write(l);
                writer.write('\n');
            }
        }
        return run;
    }

    /** Merges already-sorted run files into a single sorted {@code output}. */
    private static void kWayMerge(List<File> runs, File output) throws IOException {
        PriorityQueue<LineStream> pq = new PriorityQueue<>();
        List<LineStream> streams = new ArrayList<>();
        try (BufferedWriter writer = newWriter(output)) {
            for (File run : runs) {
                LineStream ls = new LineStream(newReader(run), false);
                streams.add(ls);
                if (ls.peek() != null) {
                    pq.add(ls);
                }
            }
            while (!pq.isEmpty()) {
                LineStream ls = pq.poll();
                writer.write(ls.peek());
                writer.write('\n');
                ls.advance();
                if (ls.peek() != null) {
                    pq.add(ls);
                }
            }
        } finally {
            for (LineStream ls : streams) {
                ls.close();
            }
        }
    }

    /**
     * Merge-joins two sorted, line-based streams. Lines only in v2 are additions; lines only in v1
     * are deletions; lines in both are unchanged. Duplicate lines within a stream are collapsed.
     */
    private static void mergeDelta(File sorted1, File sorted2, String additionsOut, String deletionsOut)
            throws IOException {
        try (LineStream v1 = new LineStream(newReader(sorted1), true);
             LineStream v2 = new LineStream(newReader(sorted2), true);
             BufferedWriter additions = newWriter(new File(additionsOut));
             BufferedWriter deletions = newWriter(new File(deletionsOut))) {
            while (v1.peek() != null && v2.peek() != null) {
                int cmp = v1.peek().compareTo(v2.peek());
                if (cmp == 0) {
                    v1.advance();
                    v2.advance();
                } else if (cmp < 0) {
                    deletions.write(v1.peek());
                    deletions.write('\n');
                    v1.advance();
                } else {
                    additions.write(v2.peek());
                    additions.write('\n');
                    v2.advance();
                }
            }
            while (v1.peek() != null) {
                deletions.write(v1.peek());
                deletions.write('\n');
                v1.advance();
            }
            while (v2.peek() != null) {
                additions.write(v2.peek());
                additions.write('\n');
                v2.advance();
            }
        }
    }

    /**
     * A line reader with one-line lookahead, ordered by its current line so it can be used in a
     * priority queue. Optionally collapses consecutive duplicate lines.
     */
    private static final class LineStream implements Comparable<LineStream>, AutoCloseable {
        private final BufferedReader reader;
        private final boolean dedup;
        private String current;

        LineStream(BufferedReader reader, boolean dedup) throws IOException {
            this.reader = reader;
            this.dedup = dedup;
            this.current = reader.readLine();
        }

        String peek() {
            return current;
        }

        void advance() throws IOException {
            String previous = current;
            String line = reader.readLine();
            if (dedup) {
                while (line != null && line.equals(previous)) {
                    line = reader.readLine();
                }
            }
            current = line;
        }

        @Override
        public int compareTo(LineStream other) {
            return this.current.compareTo(other.current);
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }

    private static BufferedReader newReader(File file) throws IOException {
        return new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8));
    }

    private static BufferedWriter newWriter(File file) throws IOException {
        Writer w = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8);
        return new BufferedWriter(w);
    }

    private static void deleteQuietly(File file) {
        if (file != null) {
            try {
                Files.deleteIfExists(file.toPath());
            } catch (IOException e) {
                log.warn("Could not delete temp file " + file + ": " + e.getMessage());
            }
        }
    }

    // Helpers shared in spirit with RDFDelta (kept self-contained so the two programs are independent)
    private static Lang validateAndGetLang(String filePath) {
        Lang lang = RDFLanguages.filenameToLang(filePath);
        if (lang == null || !RDFLanguages.isRegistered(lang)) {
            log.error("Unsupported RDF serialization for file: " + filePath);
            System.exit(2);
        }
        return lang;
    }

    private static void validateCompatibleLangs(Lang lang1, Lang lang2) {
        boolean bothTriples = RDFLanguages.isTriples(lang1) && RDFLanguages.isTriples(lang2);
        boolean bothQuads = !RDFLanguages.isTriples(lang1) && !RDFLanguages.isTriples(lang2);
        if (!bothTriples && !bothQuads) {
            log.error("Incompatible RDF serializations: " + lang1 + " and " + lang2);
            System.exit(2);
        }
    }

    private static String[] getOutputFilenames(String arg1, String arg2, String dataDir, boolean isTriples) {
        String file1Base = stripExtension(new File(arg1).getName());
        String file2Base = stripExtension(new File(arg2).getName());
        String extension = isTriples ? ".nt" : ".nq";
        String additionsOut = dataDir + file1Base + "-" + file2Base + ".additions" + extension;
        String deletionsOut = dataDir + file1Base + "-" + file2Base + ".deletions" + extension;
        return new String[]{additionsOut, deletionsOut};
    }

    private static String stripExtension(String name) {
        int idx = name.lastIndexOf('.');
        return idx > 0 ? name.substring(0, idx) : name;
    }
}
