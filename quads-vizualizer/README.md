# Quads-Vizualizer

**Quads-Vizualizer** is a lightweight React frontend for exploring the metagraph and the versioned RDF graphs produced by the ConVer-G suite.
It visualizes the history and structure of versions (the “metagraph”) and lets you inspect the RDF quads present in a selected snapshot, compare versions (deltas), and interactively explore node relationships.

## Features ✅

- Visual metagraph of versions and derivations (nodes = snapshots, edges = parent/child)
- Interactive view of a selected Versioned Graph (quads as nodes/edges)
- Delta visualization to compare two snapshots and highlight additions/removals
- Stable node layout across versions for consistent topology-based analysis
- Minimal config: connects to `Quads-Query` (SPARQL-to-SQL) and `Quads-Loader` endpoints

## Configuration ⚙️

This project reads runtime endpoints from `window.env` created by `env.sh`. Set the following environment variables before containerizing or deploying:

- `VITE_QUERY_ENDPOINT` — URL of the Quads-Query/SPARQL endpoint
- `VITE_LOADER_ENDPOINT` — URL of the Quads-Loader import endpoint

The provided `env.sh` writes a small `config.js` to ` /usr/share/nginx/html` so the containerized app can be configured at runtime.
