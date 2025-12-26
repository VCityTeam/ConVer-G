import Graph from 'graphology';
import FA2 from 'graphology-layout-forceatlas2';
import type AbstractGraph from 'graphology-types';
import { useMemo } from 'react';
import { emptyResponse, responseSerializer, type RDFBinding, type Response } from './responseSerializer';

type VersionedGraphBuild = {
    distinctVersion: string[];
    distinctGraph: string[];
    versionedGraphs: Record<string, Record<string, AbstractGraph>>;
}

const DERIVATION_PREDICATE = "http://www.w3.org/ns/prov#wasDerivedFrom";

const getDerivations = (metagraph: Response | null): Map<string, string[]> => {
    const derivations = new Map<string, string[]>();
    if (!metagraph || !metagraph.results.bindings) return derivations;

    metagraph.results.bindings.forEach(binding => {
        if (binding.predicate.value === DERIVATION_PREDICATE) {
            const derived = binding.subject.value;
            const source = binding.object.value;
            if (!derivations.has(derived)) {
                derivations.set(derived, []);
            }
            derivations.get(derived)!.push(source);
        }
    });
    return derivations;
}

const getVersionedGraphToVersionMap = (bindings: RDFBinding[] | undefined): Map<string, string> => {
    const map = new Map<string, string>();
    if (!bindings) return map;
    bindings.forEach(binding => {
        if (binding.versionedgraph && binding.version) {
            map.set(binding.versionedgraph.value, binding.version.value);
        }
    });
    return map;
}

const sortVersions = (versions: string[], bindings: RDFBinding[] | undefined, metagraph: Response | null): string[] => {
    if (!metagraph) return versions.sort();

    const derivations = getDerivations(metagraph); // versionedGraph -> [sourceVersionedGraph]
    const vgToVersion = getVersionedGraphToVersionMap(bindings);

    // Build version dependencies: version -> [sourceVersion]
    const versionDeps = new Map<string, Set<string>>();
    
    derivations.forEach((sources, derivedVg) => {
        const derivedVersion = vgToVersion.get(derivedVg);
        if (!derivedVersion) return;

        sources.forEach(sourceVg => {
            const sourceVersion = vgToVersion.get(sourceVg);
            if (sourceVersion) {
                if (!versionDeps.has(derivedVersion)) {
                    versionDeps.set(derivedVersion, new Set());
                }
                versionDeps.get(derivedVersion)!.add(sourceVersion);
            }
        });
    });

    // Topological sort
    const visited = new Set<string>();
    const sorted: string[] = [];
    const visiting = new Set<string>(); // Detect cycles

    const visit = (v: string) => {
        if (visited.has(v)) return;
        if (visiting.has(v)) {
            console.warn("Cycle detected in version derivation");
            return;
        }
        visiting.add(v);

        const deps = versionDeps.get(v);
        if (deps) {
            // Sort dependencies to ensure deterministic order
            Array.from(deps).sort().forEach(dep => visit(dep));
        }

        visiting.delete(v);
        visited.add(v);
        sorted.push(v);
    };

    // Sort initial versions to ensure deterministic order for independent components
    versions.sort().forEach(v => visit(v));

    return sorted;
}

export const buildDistinct = (bindings: RDFBinding[] | undefined, metagraph: Response | null = null) => [
    sortVersions(
        Array.from(
            new Set(
                (bindings ?? []).map(
                    ({ version }) => version.value,
                ),
            ),
        ),
        bindings,
        metagraph
    ),
    Array.from(
    new Set(
        (bindings ?? []).map(
            ({ graph }) => graph.value,
        ),
    ),
).sort()];

export const buildGroupByVersionedGraph = (bindings: RDFBinding[] | undefined) => (Object.groupBy(
    bindings ?? [],
    ({ versionedgraph }) => versionedgraph.value,
))

export const computeNodesPositions = (response: Response) => {
    const g = Graph.from(
        responseSerializer(response),
    );
    FA2.assign(g, { iterations: 250 });
    const nodesPositions = new Map<string, [number, number]>();

    g.forEachNode((node, attributes) => {
        nodesPositions.set(node, [attributes.x, attributes.y]);
    });

    return { positions: nodesPositions };
}

export const buildGraph = (
    groupByVersionedGraph: Partial<Record<string, RDFBinding[]>>,
    graph: string,
    version: string,
    head: Response["head"],
    nodesPositions: Map<string, [number, number]>
): AbstractGraph => {
    const elements = Object.values(groupByVersionedGraph).find(
        (elements) =>
            elements?.[0]?.graph.value === graph &&
            elements?.[0]?.version.value === version,
    );

    if (!elements?.length) return Graph.from(emptyResponse);

    const g = Graph.from(
        responseSerializer({
            head: head,
            results: { bindings: elements },
        }),
    );

    g.forEachNode((node) => {
        const [x, y] = nodesPositions.get(node) ?? [Math.random(), Math.random()];
        g.setNodeAttribute(node, "x", x);
        g.setNodeAttribute(node, "y", y);
    });

    return g
}

export const useBuildVersionedGraph = (response: Response, metagraph: Response | null = null): VersionedGraphBuild => useMemo(() => {
    const [distinctVersion, distinctGraph] = buildDistinct(response.results.bindings, metagraph);
    const groupByVersionedGraph = buildGroupByVersionedGraph(response.results.bindings);
    const { positions: nodesPositions } = computeNodesPositions(response);

    const versionedGraphs: Record<string, Record<string, AbstractGraph>> = {};

    distinctGraph.forEach((graph) => {
        versionedGraphs[graph] = {};
        distinctVersion.forEach((version) => {
            versionedGraphs[graph][version] = buildGraph(
                groupByVersionedGraph,
                graph,
                version,
                response.head,
                nodesPositions,
            );
        });
    });

    return {
        distinctVersion,
        distinctGraph,
        versionedGraphs,
    };
}, [response, metagraph]);

export const computeGraphsDelta = (graph1: Graph, graph2: Graph): Graph => {
    const REMOVED_COLOR = "#d14343";
    const ADDED_COLOR = "#2e8b57";
    const UNCHANGED_COLOR = "#b0b0b0";

    // Create a new graph to store the differences, using the same options as the input graphs.
    // const diffGraph = new Graph(graph1.nullCopy());
    const diffGraph = new Graph({multi: true, type: 'directed'});

    graph1.forEachNode((node, attributes) => {
        diffGraph.addNode(node, { ...attributes, status: 'deleted', color: REMOVED_COLOR });
    });

    graph2.forEachNode((node, attributes) => {
        if (diffGraph.hasNode(node)) {
            // This node existed in graph1. It's an "unchanged" or "modified" node.
            diffGraph.replaceNodeAttributes(node, {...attributes, status: 'unchanged', color: UNCHANGED_COLOR});
        } else {
            // It's a new node. Add it with an 'added' status.
            diffGraph.addNode(node, { ...attributes, status: 'added', color: ADDED_COLOR });
        }
    });

    // Similar to nodes, add all edges from the original graph and mark them as 'deleted'.
    graph1.forEachEdge((edge, attributes, source, target) => {
        // We must use `addEdgeWithKey` to preserve the original edge identifier.
        diffGraph.addEdgeWithKey(edge, source, target, { ...attributes, status: 'deleted', color: REMOVED_COLOR });
    });

    // Now, iterate through the new graph's edges to find additions or changes.
    graph2.forEachEdge((edge, attributes, source, target) => {
        if (diffGraph.hasEdge(edge)) {
            // We replace its attributes with those from graph2, which also removes the 'status'.
            diffGraph.replaceEdgeAttributes(edge, {...attributes, status: 'unchanged', color: UNCHANGED_COLOR});
        } else {
            // It's a new edge. Add it with an 'added' status.
            diffGraph.addEdgeWithKey(edge, source, target, { ...attributes, status: 'added', color: ADDED_COLOR });
        }
    });

    return diffGraph;
}
