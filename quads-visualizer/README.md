# Quads-visualizer

**Quads-visualizer** is a lightweight React frontend for exploring the metagraph and the versioned RDF graphs produced by the ConVer-G suite.
It visualizes the history and structure of versions (the “metagraph”) and lets you inspect the RDF quads present in a selected snapshot, compare versions (deltas), and interactively explore node relationships.

## Features ✅

- **Coverage matrix** of the metagraph (default view): named graphs (sources) × versions, one cell per snapshot. Click a cell to load that snapshot, a row/column header for the merged source/version view; hovering gives a live delta/merge preview. Scales to any number of sources and versions (virtualized, scroll-synced headers, source/version filters).
- Editable node-link metagraph behind a **Matrix / Graph** toggle (ForceAtlas2 layout, clustering, drag-to-link, download/upload TTL to ConVer-G)
- Interactive view of a selected Versioned Graph (quads as nodes/edges)
- Delta visualization to compare two snapshots and highlight additions/removals
- Stable node layout across versions for consistent topology-based analysis
- **Inference mode selector** (toolbar): choose the query-time reasoning applied to every query — `Server default`, `Off`, `RDFS`, `OWL Lite`, `SWRL`, `RDFS + SWRL` or `All`. The choice is sent to Quads-Query as the [`?infer=` parameter](../docs/inference.md) and applies to both graph panels and the ad-hoc SPARQL panel; changing it reloads the graphs.
- Minimal config: connects to `Quads-Query` (SPARQL-to-SQL) and `Quads-Loader` endpoints

## Configuration ⚙️

This project reads runtime endpoints from `window.env` created by `env.sh`. Set the following environment variables before containerizing or deploying:

- `VITE_QUERY_ENDPOINT` — URL of the Quads-Query/SPARQL endpoint
- `VITE_LOADER_ENDPOINT` — URL of the Quads-Loader import endpoint

The provided `env.sh` writes a small `config.js` to ` /usr/share/nginx/html` so the containerized app can be configured at runtime.

## Development 🛠️

```shell
npm install      # install dependencies
npm run dev      # start the dev server
npm run lint     # static analysis (ESLint)
npm test         # run the component/unit tests (Vitest)
npm run build    # type-check and build for production
```

Tests live next to the code as `*.test.ts` files under `src/` and run with
[Vitest](https://vitest.dev/). The same `lint` and `test` steps run in
continuous integration on every push.
