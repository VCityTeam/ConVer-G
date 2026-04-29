---
title: 'ConVer-G: A Suite for Versioning, Querying and Visualization of Dynamic Knowledge Graphs'
tags:
  - Java
  - Javascript
  - RDF
  - SPARQL
  - semantic web
  - knowledge graphs
authors:
  - name: Jey Puget Gil
    orcid: 0009-0006-6198-7488
    equal-contrib: true
    affiliation: "1"
  - name: Emmanuel Coquery
    orcid: 0000-0001-7015-5604
    affiliation: 1
  - name: John Samuel
    orcid: 0000-0001-8721-7007
    equal-contrib: true
    affiliation: 2
  - name: Gilles Gesquière
    orcid: 0000-0001-7088-1067
    affiliation: 3
affiliations:
 - name: Universite Claude Bernard Lyon 1, CNRS, INSA Lyon, LIRIS, UMR 5205
   index: 1
 - name: CPE Lyon, CNRS, INSA Lyon, Universite Claude Bernard Lyon 1, Université Lumière Lyon 2, Ecole Centrale de Lyon, LIRIS, UMR 5205
   index: 2
 - name: Université Lumière Lyon 2, CNRS, Université Claude Bernard Lyon 1, INSA Lyon, Ecole Centrale de Lyon, LIRIS, UMR 5205
   index: 3
date: 17 December 2025
bibliography: paper.bib
---

# Summary

Knowledge Graphs (KGs) are dynamic artifacts that evolve continuously as data is updated, corrected, and expanded.
Managing this evolution through explicit versioning is essential to support reproducibility, auditability, provenance tracking, and temporal analysis.
ConVer-G (Concurrent Versioning of Knowledge Graphs) is a software suite designed to address these challenges by providing snapshot-based version management for RDF datasets [@gil2024convergconcurrentversioningknowledge].

The suite is composed of three modular and interoperable components:

1. **Quads-loaDer:** A command-line interface and service for ingesting RDF data, and storing versioned quads efficiently.
2. **Quads-Query:** A query translation engine that converts SPARQL queries into SQL, enabling the interrogation of specific graph versions as well as cross-snapshot and temporal analyses directly over a relational backend.
3. **Quads-Visualizer:** A web-based visualization tool that allows users to explore both the metagraph—capturing the structural history of versions, branches, and their ancestry—and the versioned graph, which represents the RDF quads contained in a selected snapshot.

Together, these components implement a condensed snapshot-based representation of RDF graphs that optimizes storage by sharing common quads across versions, while simultaneously supporting concurrent workflows such as branching and merging.

# Statement of need

The management of evolving RDF data presents significant challenges regarding storage efficiency and query performance.
Traditional approaches often rely on independent snapshots, which lead to massive data redundancy, or simple change logs (deltas), which can degrade query performance as the history grows.

We consider weather forecasting as a use case to demonstrate these needs because it inherently involves data that evolves along multiple dimensions.
Predictions for a target date are updated daily, and different agencies produce competing forecasts.
Effectively managing this requires a system capable of handling non-linear histories where users can query a specific forecast version or compare diverging predictions.

Although several RDF versioning solutions have been proposed—such as SemVersion **SemVersion** [@volkel2005semversion], **R&WBase** [@vander2013r], **R43ples** [@graube2014r43ples], and **OSTRICH** [@taelman2018ostrich]—there remains a clear need for systems that support concurrent versioning, i.e., non-linear histories with branching and merging, while benefiting from the robustness, scalability, and maturity of standard relational database management systems (RDBMS).

Furthermore, translating SPARQL, the standard query language for RDF, into SQL for versioned contexts remains a complex task [@prud2009mapping].
**ConVer-G** addresses these needs by providing an architecture where the **Quads-loaDer** handles the provenance of quads (handling named graphs and snapshots), and **Quads-Query** provides a transparent SPARQL endpoint that translates queries into optimized SQL, leveraging the storage capabilities of PostgreSQL.
This approach allows users to perform snapshot-based queries and analyze the lineage of data, modeled using PROV-O concepts [@provo], without the need for specialized, experimental triple stores.

# State of the field

Existing approaches to RDF versioning generally fall into four categories.
*Independent-copy* (full snapshot) strategies store each revision in full, providing fast version-specific queries at the cost of significant data redundancy.
*Change-based* (delta) strategies, such as those underpinning **R43ples** [@graube2014r43ples], record only the differences between revisions, which is space-efficient but degrades query performance as histories grow because reconstructing a state requires composing many deltas.
*Timestamp-based* approaches, like **Dydra** [@thompson2011dydra], that associates each RDF triple with a timestamp or a validity interval, indicating the period during which it belongs to the knowledge graph.
*Hybrid* strategies, exemplified by **OSTRICH** [@taelman2018ostrich], combine snapshots and deltas to balance storage and query performance, but they rely on dedicated triple-store engines whose operational maturity, indexing, and ecosystem tooling differ from mainstream RDBMS.
Earlier systems such as **SemVersion** [@volkel2005semversion] and **R&WBase** [@vander2013r] introduced versioning concepts (including branching) for RDF, but were not designed around a relational backend nor around an explicit metagraph that captures non-linear histories with branching and merging at the level of named graphs and snapshots.

ConVer-G is positioned within this landscape as a *condensed snapshot* approach realized on top of a standard RDBMS (PostgreSQL).
By materializing quads once and using a bitmask to record their presence across snapshots, it avoids the redundancy of independent copies while preserving the query simplicity of a snapshot model.
Compared to the systems above, the suite distinguishes itself in three ways: (i) it explicitly models concurrent (non-linear) version histories with branching and merging, rather than purely linear revision chains; (ii) it exposes a standard SPARQL endpoint backed by a SPARQL-to-SQL translator that injects versioning constraints into query rewriting, enabling both snapshot-specific and cross-snapshot analyses; and (iii) it provides an interactive visualization of both the metagraph (versions, branches, derivation) and the versioned graph, which is, to the best of our knowledge, not jointly available in the systems cited above.

# Contributions
## Software design

The software suite contributes three distinct but interoperable tools that operationalize the theoretical framework of concurrent KG versioning.
The architecture is designed to be modular, allowing each component to be used independently or in combination, depending on user needs.
The overall architecture is illustrated in Figure \autoref{fig:architecture}.

![Architecture of the ConVer-G system.\label{fig:architecture}](architecture.png){ width=75% }

### Quads-loaDer

The **Quads-loaDer** is the ingestion engine of the suite.
It is responsible for mapping standard RDF serialization formats (Turtle, TriG, N-Quads) into the internal relational schema.
Its primary goals are:

* **Metadata Management:** It manages the metadata associated with provenance, effectively building the "Versioned Named Graph."
* **Storage Optimization:** It condenses storage by identifying and storing the quads by managing a bitmask that indicates the presence of each quad across different snapshots.

### Quads-Query

**Quads-Query** acts as the middleware layer.
It exposes a SPARQL endpoint compatible with standard clients (e.g., Yasgui, Jena).
Its core contribution is the **SPARQL-to-SQL translator**.
Unlike direct mapping approaches[@rodriguez2013evaluating], Quads-Query injects versioning constraints into the SQL generation process.
It allows users to execute queries against a specific snapshot or named branch, dynamically rewriting the query to filter quads valid at that specific point in the version tree.

### Quads-Visualizer

**Quads-Visualizer** is a React-based frontend application designed to visualize the metagraph: the metadata of the versioned KG.
While the backend manages the data, Quads-Visualizer renders two graphs:

- the **Metagraph**—a graph where nodes represent versions (snapshot) and edges represent derivation (parent-child relationships)
- the **Versioned Graph**—a view of the RDF quads present in a selected snapshot.

![Left panel shows the metagraph and right panel shows the versioned graph.\label{fig:qua-viz}](qua-viz.png)

The clustering feature organizes Versioned Named Graph (VNG) nodes using two PROV-O predicates: `prov:specializationOf` links each VNG node to its *graph name*, grouping all temporal versions of the same named graph; `prov:atLocation` links each VNG node to its *version identifier*, grouping all named graphs captured within the same snapshot.
This clustering enables users to navigate either by structure—observing how a single named graph evolves—or by time—inspecting the complete dataset state at a specific version.

When users find the visualization cluttered by numerous metadata triples, they can toggle the **Focus mode**.
This feature hides all metadata except the PROV-O related triples, allowing users to concentrate on the actual data and its provenance annotations without visual noise.

![Clustering of nodes in the metagraph.\label{fig:qua-viz-clustering}](cluster.png)

To enhance comprehension through topology, the tool computes a static position for every node across all versions of all quads, ensuring a consistent structural layout regardless of the version being viewed.
Additionally, the **Change versioned graph** facilitates evolution analysis by computing and displaying the delta between the currently selected Versioned Graph and any other version, allowing administrators and users to visually inspect changes, annotate versions, and understand the derivation history.
This feature also handles selecting a version or a named graph: displaying respectively only the versioned graphs inside the version or the versioned graphs inside the named graph.

![Visualization of differences between two versioned graphs.\label{fig:delta-viz}](delta-viz.png)

The **Merged graphs** option allows users to visualize all versioned graphs merged into a single graph.
When the merged graph mode is disabled, a list of versioned graphs is displayed, allowing users to select and view individual graphs.
A search bar is provided to filter nodes by their labels, facilitating navigation in large datasets with many versioned graphs.

![Merged graph option visualization (left enabled, right disabled).\label{fig:merged-viz}](merged.png)

## Reproducibility

A fully reproducible experiment demonstrating the ConVer-G suite is available in the [UD-Demo-VCity-Knowledge_Evolution repository](https://github.com/VCityTeam/UD-Demo-VCity-Knowledge_Evolution/blob/JOSS-ConVer-G/Reproducibility.md).
The experiment uses a weather forecasting use case where the system ingests daily weather predictions from multiple sources, stores them as versioned RDF graphs, and enables snapshots analysis to compare forecast accuracy.
The demonstration requires only Docker and Docker Compose, and includes pre-configured services for all three ConVer-G components along with example SPARQL queries.

# Research impact statement

ConVer-G targets researchers and practitioners who work with evolving RDF datasets and need to manage their history explicitly rather than implicitly through ad-hoc copies or external scripts.
By materializing concurrent versioning over a RDBMS and exposing a standard SPARQL endpoint, the suite lowers the entry cost for adopting versioned KGs in projects that already rely on relational infrastructure, and it makes versioned KGs usable from existing SPARQL clients without modification.

The expected impact spans several research and application areas.
For *urban and geospatial data*, where datasets evolve across time and across competing sources (the original motivation of the VCity use cases), the metagraph view makes the structure of these histories inspectable and navigable.
More broadly, by demonstrating that concurrent versioning of RDF can be built on top of PostgreSQL with competitive functionality, ConVer-G contributes a reusable architectural template for the semantic web and RDBMS communities, and a teaching artifact for courses on KGs, data versioning, and SPARQL-to-SQL translation.

# AI usage disclosure

AI tool was used in language refinement and formatting of the manuscript.
All technical content—including the system design, the SPARQL-to-SQL translation approach, the visualization features, the experiments, and the citations—was conceived, implemented, verified, and authored by the human authors.
No AI tool was used to generate experimental results, source code that was committed without review, or citations; every reference in this paper was checked by the authors.
The authors take full responsibility for the content of the manuscript and the associated software.

# Acknowledgements

This work was supported by the LIRIS laboratory (Laboratoire d'InfoRmatique en Image et Systèmes d'information) and funded in part by the IADoc@UdL program.
We acknowledge the contributions of the VCity project members for the initial urban data use cases that motivated this research.

# References
