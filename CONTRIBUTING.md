# Contributing to ConVer-G

First off, thank you for taking the time to contribute! ConVer-G (Concurrent
Versioning of knowledge Graphs) is an open research project, and contributions of
all kinds — bug reports, documentation, tests, and code — are very welcome.

This document explains how the project is organised, how to set up a local
development environment, how to run the tests, and how to propose a change.

## Code of conduct

This project and everyone participating in it is governed by our
[Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to
uphold this code. Please report unacceptable behaviour as described there.

## Where to ask questions and report problems

- **Bugs and feature requests:** open an issue on the
  [issue tracker](https://github.com/VCityTeam/ConVer-G/issues). Please search
  the existing issues first to avoid duplicates, and use the provided issue
  templates.
- **Questions and support:** see [SUPPORT.md](SUPPORT.md).

## Project layout

ConVer-G is a multi-module Maven project plus a web front end:

| Module | Description |
| ------ | ----------- |
| `quads-creator` | Annotates RDF datasets (versions, named graphs) before import. |
| `quads-delta` | Computes additions/deletions between two RDF datasets. |
| `quads-loader` / `quads-loader-cli` | Ingests versioned quads into PostgreSQL. |
| `quads-query` / `quads-query-cli` | SPARQL-to-SQL translator and SPARQL endpoint. |
| `quads-visualizer` | React/TypeScript front end (metagraph + versioned graph). |
| `versioned-queries` | Example SPARQL queries used in the reproducibility study. |

## Branching model

We follow a [Git Flow](https://nvie.com/posts/a-successful-git-branching-model/)
style workflow:

- **`main`** — stable, released code. Protected; only updated through reviewed
  pull requests from `develop`.
- **`develop`** — integration branch for the next release. Target your pull
  requests here unless you are fixing a released bug via a hotfix.
- **`feature/<short-name>`** — branch off `develop` for new work, then open a
  pull request back into `develop`.
- **`fix/<short-name>`** / **`hotfix/<short-name>`** — bug fixes.

## Setting up a local development environment

### Prerequisites

- Java 21 (we recommend [SDKMAN!](https://sdkman.io/): `sdk install java 21.0.1-amzn`)
- Maven
- [Docker](https://www.docker.com/) and Docker Compose (for the PostgreSQL and
  test databases)
- Node.js 20+ and npm (only for the `quads-visualizer` front end)

### Start the dependencies

```shell
# from the repository root — starts the PostgreSQL databases declared in docker-compose.yml
docker compose up -d
```

## Running the tests locally

The Java modules are built and tested with Maven. The same commands are run by
our continuous integration:

```shell
# make sure the databases are up first (docker compose up -d)

# unit tests
mvn test

# unit + integration tests (the integration tests need the databases running)
mvn verify

# integration tests against the flattened representation
mvn verify -Pflat
```

For the visualizer:

```shell
cd quads-visualizer
npm install
npm run lint   # static analysis
npm test       # component / unit tests
```

## Proposing a change

1. **Open (or comment on) an issue** describing the bug or feature, so the
   change can be discussed before significant effort is spent.
2. **Fork** the repository and create a branch from `develop`
   (e.g. `feature/my-improvement`).
3. **Make your change.** Keep commits focused and write clear commit messages.
   Match the style of the surrounding code.
4. **Add or update tests.** New behaviour should be covered by tests, and the
   existing suite (`mvn verify`, and `npm test` for the visualizer) must pass.
5. **Update the documentation** (README, module READMEs, or the JOSS paper) when
   your change affects user-facing behaviour.
6. **Open a pull request** against `develop`, filling in the pull request
   template. Link the issue it resolves.

A maintainer will review your pull request. Continuous integration must be green
before a change can be merged.

## License

By contributing, you agree that your contributions will be licensed under the
same license as the project (see [LICENSE](LICENSE)).
