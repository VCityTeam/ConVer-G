import Graph from "graphology";
import type AbstractGraph from "graphology-types";
import { useMemo } from "react";
import { responseSerializer, type Response } from "./responseSerializer";
import { computeNodesPositions } from "./graphUtils";
import type { MetagraphNodeType } from "../state/metagraphSlice";

export const VERSIONED_NODE_PREFIX = "https://github.com/VCityTeam/ConVer-G/Versioned-Named-Graph#";

export const METAGRAPH_NODE_COLORS = {
    versionedGraph: "#5333ed",
    atLocation: "#ff8a00",
    specialization: "#0f9d58",
    default: "#546e7a",
} as const;

export const METAGRAPH_RELATION_SUFFIXES = {
    specialization: "specializationOf",
    location: "atLocation",
    derivation: "wasDerivedFrom",
} as const;

export type MetagraphRelationKey = keyof typeof METAGRAPH_RELATION_SUFFIXES;

export type MetagraphRelationFlags = Record<MetagraphRelationKey, boolean>;

export const createEmptyMetagraphRelations = (): MetagraphRelationFlags => ({
    specialization: false,
    location: false,
    derivation: false,
});

const RELATION_ENTRIES = Object.entries(METAGRAPH_RELATION_SUFFIXES) as Array<[MetagraphRelationKey, string]>;

export const METAGRAPH_RELATION_COLORS = {
    edge: "#d3d3d3",
    label: "#555555",
} as const;

export const applyMetagraphNodeColors = (graph: AbstractGraph, showHidden: boolean = false): void => {
    const relationTargets: Record<MetagraphRelationKey, Set<string>> = {
        specialization: new Set<string>(),
        location: new Set<string>(),
        derivation: new Set<string>(),
    };

    graph.forEachEdge((edgeKey, attributes, _source, target) => {
        const label = typeof attributes?.label === "string" ? attributes.label : "";
        const matchingEntry = RELATION_ENTRIES.find(([, suffix]) => label.endsWith(suffix));
        const relationKey = matchingEntry?.[0];

        if (relationKey) {
            relationTargets[relationKey].add(target);
        }

        const shouldHideEdge = !showHidden && (relationKey === "specialization" || relationKey === "location");
        graph.setEdgeAttribute(edgeKey, "hidden", shouldHideEdge);
    });

    graph.forEachNode((nodeKey, attributes) => {
        const label = typeof attributes?.label === "string" ? attributes.label : "";
        const relations: MetagraphRelationFlags = {
            specialization: relationTargets.specialization.has(nodeKey),
            location: relationTargets.location.has(nodeKey),
            derivation: relationTargets.derivation.has(nodeKey),
        };

        const shouldHideNode = !showHidden && (relations.specialization || relations.location);

        const color = label.startsWith(VERSIONED_NODE_PREFIX)
            ? METAGRAPH_NODE_COLORS.versionedGraph
            : relations.location
                ? METAGRAPH_NODE_COLORS.atLocation
                : relations.specialization
                    ? METAGRAPH_NODE_COLORS.specialization
                    : METAGRAPH_NODE_COLORS.default;

        if (label.startsWith(VERSIONED_NODE_PREFIX)) {
            let namedGraph: string | undefined;
            let version: string | undefined;

            graph.forEachOutboundEdge(nodeKey, (_edgeKey, attributes, _source, target) => {
                const edgeLabel = typeof attributes?.label === "string" ? attributes.label : "";
                if (edgeLabel.endsWith(METAGRAPH_RELATION_SUFFIXES.specialization)) {
                    namedGraph = (graph.getNodeAttribute(target, "label") as string) || target;
                }
                if (edgeLabel.endsWith(METAGRAPH_RELATION_SUFFIXES.location)) {
                    version = (graph.getNodeAttribute(target, "label") as string) || target;
                }
            });

            if (namedGraph) graph.setNodeAttribute(nodeKey, "namedGraph", namedGraph);
            if (version) graph.setNodeAttribute(nodeKey, "version", version);
        }

        graph.setNodeAttribute(nodeKey, "color", color);
        graph.setNodeAttribute(nodeKey, "metagraphRelations", relations);
        graph.setNodeAttribute(nodeKey, "hidden", shouldHideNode);
    });
};

export const getMetagraphNodeType = (graph: AbstractGraph, nodeKey: string): MetagraphNodeType => {
    const nodeLabel = graph.getNodeAttribute(nodeKey, "label") as string | undefined;
    if (typeof nodeLabel === "string" && nodeLabel.startsWith(VERSIONED_NODE_PREFIX)) {
        return "vng";
    }

    const relations = graph.getNodeAttribute(nodeKey, "metagraphRelations") as MetagraphRelationFlags | undefined;
    if (relations?.specialization) return "namedGraph";
    if (relations?.location) return "version";

    return "vng";
};

export const resolveTravelTarget = (graph: AbstractGraph, nodeKey: string) => {
    if (!graph.hasNode(nodeKey)) return null;

    const nodeLabel = graph.getNodeAttribute(nodeKey, "label") as string | undefined;
    const isVersionedNamedGraph = typeof nodeLabel === "string" && nodeLabel.startsWith(VERSIONED_NODE_PREFIX);

    if (!isVersionedNamedGraph) return null;

    let linkedGraph: string | undefined;
    let linkedVersion: string | undefined;

    graph.forEachOutboundEdge(nodeKey, (_edgeKey, attributes, _source, target) => {
        const relationLabel = typeof attributes?.label === "string" ? attributes.label : "";

        if (!linkedGraph && relationLabel.endsWith(METAGRAPH_RELATION_SUFFIXES.specialization)) {
            const candidate = graph.getNodeAttribute(target, "label") as string | undefined;
            linkedGraph = typeof candidate === "string" ? candidate : target;
        }

        if (!linkedVersion && relationLabel.endsWith(METAGRAPH_RELATION_SUFFIXES.location)) {
            const candidate = graph.getNodeAttribute(target, "label") as string | undefined;
            linkedVersion = typeof candidate === "string" ? candidate : target;
        }
    });

    if (!linkedGraph && !linkedVersion) return null;

    return { linkedGraph, linkedVersion };
};

export const useBuildMetagraph = (response: Response): AbstractGraph => useMemo(() => {
    const g = Graph.from(responseSerializer(response));
    const { positions: nodesPositions } = computeNodesPositions(response, {
        iterations: 30, settings: {
            barnesHutOptimize: true,
            barnesHutTheta: 0.1,
            gravity: 1.5
        }
    });

    g.forEachNode((node) => {
        const [x, y] = nodesPositions.get(node) ?? [Math.random(), Math.random()];
        g.setNodeAttribute(node, "x", x);
        g.setNodeAttribute(node, "y", y);
    });

    applyMetagraphNodeColors(g);

    return g;
}, [response]);