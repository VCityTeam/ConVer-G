# Inference (query-time saturation)

`quads-query` can apply RDFS/OWL and SWRL inference at query time. Reasoning is performed **on the
data**, not on the query: the deductive closure of the store is computed as a single PostgreSQL
`WITH RECURSIVE` prefix, and every triple pattern scans that saturated relation instead of the base
`versioned_quad` table. Because the inferred triples materialise as rows (only for the duration of the
query — nothing is written to the database), they are visible to **every** query shape, including open
patterns:

| Query                              | Without inference | With `?infer=rdfs` (given `:M1 a :House`, `:House rdfs:subClassOf :Building`) |
|------------------------------------|-------------------|------------------------------------------------------------------------------|
| `?b a :Building`                   | `:M1`             | `:M1`                                                                        |
| `?b a ?type`                       | `:House`          | `:House`, **`:Building`**                                                    |
| `?b ?p ?o`                         | `:M1 a :House`    | `:M1 a :House`, **`:M1 a :Building`**                                        |

Saturation is version-aware: an inferred triple's `validity` bitstring is the intersection (`&`) of its
premises' bitstrings, and triples derived through several paths are merged with `bit_or` — so an
inference only holds in the versions where **all** of its premises hold. Only the condensed layout
(`versioned_quad`) is supported.

## Enabling reasoning

Two levers, both dynamic (no restart, no re-materialisation):

- **Per query**, with the `?infer=` HTTP query parameter on the SPARQL endpoint. This lets a user
  choose whether to include inferred knowledge in a given query:

  | `?infer=`                | Effect                                                        |
  |--------------------------|---------------------------------------------------------------|
  | _(absent)_               | Use the server default (see below)                            |
  | `off` / `none`           | No inference                                                  |
  | `rdfs`                   | RDFS entailment                                               |
  | `owl` / `owl_lite`       | OWL-Lite (currently the same rule set as RDFS)                |
  | `swrl`                   | The verified SWRL rules                                       |
  | `rdfs+swrl`, `all`, `on` | Combine sources (`+`, `,` or space separated)                 |

  ```shell
  curl --data-urlencode 'query=SELECT * WHERE { GRAPH ?g { ?b a ?type } }' \
       'http://localhost:8081/rdf/query?infer=rdfs+swrl'
  ```

- **Server default**, applied when `?infer=` is absent, via the `ENTAILMENT_REGIME`
  (`NONE` | `RDFS` | `OWL_LITE`) and `SWRL_RULES` environment variables of the `quads-query` service.
  Inference is **disabled by default** (`ENTAILMENT_REGIME` unset, no `SWRL_RULES`).

  ```shell
  ENTAILMENT_REGIME=RDFS java ... -jar target/quads-query-1.0-SNAPSHOT.jar
  # or, in the ud-quads-query Docker Compose service environment:
  #   - "ENTAILMENT_REGIME=RDFS"
  ```

  (`quads-query-cli` has no HTTP layer, so it always uses the environment-driven server default.)

## RDFS rules

Applied over the transitive `rdfs:subClassOf+` / `rdfs:subPropertyOf+` closures, which are themselves
versioned recursive CTEs:

- **rdfs9** — `?x rdf:type ?D` from `?x rdf:type ?C . ?C rdfs:subClassOf+ ?D`,
- **rdfs7** — `?s ?p ?o` from `?s ?q ?o . ?q rdfs:subPropertyOf+ ?p`,
- **rdfs2** — `?x rdf:type ?C` from `?x ?p ?_ . ?p rdfs:domain ?C`,
- **rdfs3** — `?x rdf:type ?C` from `?_ ?p ?x . ?p rdfs:range ?C`.

The strata are layered so that one rule's output feeds the next: a type derived by rdfs2 (domain) is,
for example, propagated up the whole `subClassOf` chain by rdfs9.

## SWRL reasoning and verification (Openllet)

`quads-query` and `quads-query-cli` can apply user-defined
[SWRL](https://www.w3.org/submissions/SWRL/) rules as part of the same saturation. The rules are
shipped in an OWL ontology file (any format OWLAPI can parse: RDF/XML, Turtle, OWL/XML, functional
syntax) referenced by the `SWRL_RULES` environment variable:

```shell
SWRL_RULES=/path/to/rules.owl java ... -jar target/quads-query-1.0-SNAPSHOT.jar

# combines freely with the entailment regime and the per-query ?infer= parameter:
ENTAILMENT_REGIME=RDFS SWRL_RULES=/path/to/rules.owl java ... -jar target/quads-query-1.0-SNAPSHOT.jar
```

Before any rule is used, the ontology is **verified with the [Openllet](https://github.com/Galigator/openllet)
reasoner**: an inconsistent ontology is a configuration error and is refused (the server fails loudly on
startup). Each rule is then checked against what the saturation builder supports — named-class atoms,
named object/data property atoms, and variable, named-individual or literal arguments. Rules using
builtin atoms (`swrlb:*`), `sameAs`/`differentFrom` or anonymous class/property expressions are
skipped with a warning; the remaining verified rules stay active.

Each supported rule is compiled to a Datalog-style rule over quads and evaluated as an extra stratum
of the closure. A multi-atom body becomes a self-join of the saturated relation, sharing a named graph
and intersecting the atoms' version sets; for
`hasParent(?x, ?y) ^ hasBrother(?y, ?z) -> hasUncle(?x, ?z)`, every `?x :hasParent ?y . ?y :hasBrother
?z` derives `?x :hasUncle ?z` valid in the intersection of the two premises' versions. SWRL heads are
applied in a single pass over the RDFS closure (chained SWRL rules — a rule head feeding another rule's
body — are not re-fed into the closure).

The server default inference configuration is reported by the Fuseki `$/server` endpoint: the version
string is suffixed with the mode (`RDFS`, `SWRL`, `RDFS+SWRL`, …) and left untouched when no inference
is enabled by default. A sample rules file lives in `quads-query/src/test/resources/swrl/`.

## Schema-drift virtual graph

Independently of the regime, the virtual graph `urn:converg:schema-drift` can be queried to inspect
the schema layer and how it changes across versions:

```sparql
SELECT ?s ?p ?o WHERE {
  GRAPH <urn:converg:schema-drift> { ?s ?p ?o }
}
```

It returns all schema triples (`rdfs:subClassOf`, `rdfs:subPropertyOf`, `rdfs:domain`, `rdfs:range`)
with their version sets, so ontological drift can be detected with standard SPARQL filters.

Example queries live in `quads-query/src/test/resources/queries/entailment/`.
