# Motivations

## Why create a "SPARQL to SQL" translator rather than a from scratch engine?

Our motivation is to find a method for *retrieving knowledge* from a *set of urban data versions* stored in *RDF
format*.

Motivations for linking SPARQL and SQL are numerous, particularly in the fields of science, technology,
and business, where there is a growing need to integrate increasingly diverse data sources (captors, institutions, ...).
By using a SPARQL to SQL translator, we can enable relational databases to be exposed on the Semantic Web and queried
with SPARQL while reusing the maturity of an existing RDBMS.
This allows researchers and developers to work with RDF and relational data seamlessly while leveraging
the storage, indexing, and transactional optimizations of existing relational databases.

A *"from scratch"* engine that is not based on SPARQL nor SQL *would not be interoperable* with these query systems.
It is simpler than reimplementing the full stack (algebra, including join algorithms, optimisation, efficient
storage and indexing).
An evaluation of the query performance of the condensed representation against an extensional (one-dataset-per-version)
baseline is reported in the accompanying paper [(ConVer-G, arXiv:2409.04499)](https://arxiv.org/abs/2409.04499); see the
[reproducibility guide](https://github.com/VCityTeam/UD-Demo-VCity-Knowledge_Evolution/blob/JOSS-ConVer-G/Reproducibility.md)
to reproduce those measurements.

## Why this experiment?

We want to ensure the **provenance, accuracy, efficiency, and reliability** of querying a condensed representation of
the various version of a dataset with regard to querying each different version represented extensionally,
that is understanding whether our proposal of adding to each quad the set of versions it appears in,
instead of representing each version as a separate dataset, leads to a more efficient way of answering queries across
versions.
A query-scenario of this experiment can be : *"Which city version has the highest number of trees in the district 1?"*

## Notes

Using a SQL as a backend for SPARQL has been done in some cases.

- **[A Mapping of SPARQL Onto Conventional SQL - W3C](https://www.w3.org/2008/07/MappingRules/StemMapping)**
  This paper discusses a semantics for expressing relational data as an RDF graph and an algebra for mapping SPARQL
  SELECT queries over that RDF to SQL queries over the original relational data.
  The goal is to provide a specification for SPARQL tool vendors and a foundation for the Semantic Web. It highlights
  the importance of creating a computable mapping from SPARQL semantics to SQL semantics.
- **[Evaluating SPARQL to SQL Translation in Ontop - Free University of Bozen-Bolzano](https://www.inf.unibz.it/~calvanese/papers/rodr-etal-ORE-2013.pdf)**
  This research paper discusses the importance of mapping relational databases into RDF using the R2RML standard.
  It mentions the research focused on translating SPARQL queries into SQL and evaluates the SPARQL to SQL translation in
  the Ontop system.
- **[RDF and SPARQL: Using Semantic Web Technology to Integrate the World's Data - W3C](https://www.w3.org/2007/03/VLDB/)**
  This resource explains how RDF and SPARQL can be used to improve access to relational databases. It discusses
  techniques for improving mappings between RDF and relational data and mentions that several query engines map SPARQL
  queries to relational queries,
  either by rewriting them into SQL queries or by compiling them directly into evaluation structures native to the
  database.
