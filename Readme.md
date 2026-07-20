# ConVer-G project

> Which means Concurrent Versioning of knowledge Graphs

This project aims to create a knowledge hub that can store and query a set of RDF datasets with a versioning system.
The project is part of the BD team's research efforts within the [LIRIS](https://liris.cnrs.fr/)
and [VCity project](https://projet.liris.cnrs.fr/vcity/).
The aim of this POC is to query a set of city version and extract associated knowledge.

> This system has a demonstration and its code source is [available on GitHub](https://github.com/VCityTeam/UD-Demo-VCity-Knowledge_Evolution).
> An experiment on knowledge evolution with weather forecasting data is also available [on GitHub](https://github.com/VCityTeam/UD-Demo-VCity-Knowledge_Evolution/blob/JOSS-ConVer-G/Reproducibility.md).

## Community

- 🐛 **Found a bug or have a feature idea?** Open an issue on the [issue tracker](https://github.com/VCityTeam/ConVer-G/issues).
- ❓ **Need help?** See [SUPPORT.md](SUPPORT.md) for where to ask questions.
- 🤝 **Want to contribute?** Read [CONTRIBUTING.md](CONTRIBUTING.md) for the branching model, how to run the tests, and how to propose a change.
- 📜 All participation is governed by our [Code of Conduct](CODE_OF_CONDUCT.md).

## Documentation

The documentation is split into focused guides under [`docs/`](docs):

| Guide | What it covers |
| ----- | -------------- |
| [Motivation](docs/motivation.md) | Why a SPARQL-to-SQL translator, what the experiment measures, and related work. |
| [Getting started](docs/getting-started.md) | Installing Java 21 + PostgreSQL, the Maven modules, running each service, and the test commands. |
| [Architecture](docs/architecture.md) | The versioning ontology, the conceptual and entity–relationship models, and the translation/query/import flowcharts. |
| [Inference](docs/inference.md) | Query-time RDFS/OWL and SWRL saturation, the `?infer=` parameter, and the schema-drift virtual graph. |
| [Data model & workflow](docs/data-model.md) | The contextualization → theoretical → implementation pipeline, with a worked two-version example. |

Each Maven module also has its own `README.md` (for example
[quads-delta/README.md](quads-delta/README.md)).

## Quick start

```shell
# start the dockerized PostgreSQL 17 database
docker compose up -d

# build and test everything
mvn verify
```

See [Getting started](docs/getting-started.md) for the full setup, service-by-service run commands, and
configuration options.
