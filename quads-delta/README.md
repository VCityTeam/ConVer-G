# quads-delta

Computes the delta (additions and deletions) between two RDF datasets. The module ships **two**
programs that produce the same result but use different strategies:

- **`fr.vcity.RDFDelta`** (`quads-delta.jar`) — loads *both* versions fully into memory (Jena
  `Model`/`Dataset`) and diffs them.
- **`fr.vcity.RDFStreamDelta`** (`quads-stream-delta.jar`) — *stream mode*: parses each version one
  statement at a time, spills canonical lines to disk, externally sorts them with a bounded amount of
  memory, and merge-joins the two sorted streams. Blank nodes are skolemized with a version-specific
  IRI prefix, so a blank node keeps its identity *within* a version but never matches the other
  version (any statement with a blank node therefore always appears in the delta).

## Which one should I use?

Use **`RDFStreamDelta`** by default, and especially for large datasets or memory-constrained
environments: it never holds both versions in memory, so its footprint stays roughly constant
regardless of input size (it trades memory for temporary disk space and an external sort). Reach for
**`RDFDelta`** only for small inputs where simplicity matters and both versions comfortably fit in
memory, or when you specifically want blank nodes compared by Jena's in-memory semantics rather than
skolemized as version-distinct. If blank-node statements must be treated as changed across versions,
`RDFStreamDelta` is the correct choice.

## Build

```shell
mvn package
# produces target/quads-delta.jar and target/quads-stream-delta.jar
```

## Run

Both take two input filenames (resolved under `/data`) and write `<a>-<b>.additions` and
`<a>-<b>.deletions` next to them:

```shell
# in-memory delta
java -jar target/quads-delta.jar v1.trig v2.trig

# streaming delta (override the chunk size with -Ddelta.chunkLines if needed)
java -jar target/quads-stream-delta.jar v1.trig v2.trig
```

With Docker, the image defaults to `RDFDelta`; run the streaming variant by overriding the entrypoint:

```shell
docker run --entrypoint java <image> -jar /opt/app/quads-stream-delta.jar v1.trig v2.trig
```
