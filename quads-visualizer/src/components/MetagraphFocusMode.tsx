import { useSigma } from "@react-sigma/core";
import { useEffect } from "react";
import { METAGRAPH_RELATION_SUFFIXES, VERSIONED_NODE_PREFIX } from "../utils/metagraphBuilder.ts";
import { useAppSelector } from "../state/hooks";

/**
 * Component that manages focus mode visibility for the metagraph.
 * When focus mode is enabled, it shows only cluster-related edges (specialization, location)
 * and their associated nodes (VNGs, named graphs, versions).
 * Renders nothing - purely a side-effect component.
 */
export const MetagraphFocusMode = () => {
    const sigma = useSigma();
    const graph = sigma.getGraph();
    const focusMode = useAppSelector((state) => state.metagraph.focusMode);

    useEffect(() => {
        // Focus mode: hide edges that are not specializationOf or atLocation (i.e., hide derivation edges)
        graph.forEachEdge((edgeKey, attributes) => {
            const label = typeof attributes?.label === "string" ? attributes.label : "";
            const isSpecialization = label.endsWith(METAGRAPH_RELATION_SUFFIXES.specialization);
            const isLocation = label.endsWith(METAGRAPH_RELATION_SUFFIXES.location);

            if (focusMode) {
                // In focus mode, only show specialization and location edges
                graph.setEdgeAttribute(edgeKey, "hidden", !(isSpecialization || isLocation));
            } else {
                // When not in focus mode, restore visibility based on original state
                // Specialization and location edges are normally hidden
                graph.setEdgeAttribute(edgeKey, "hidden", isSpecialization || isLocation);
            }
        });

        // Hide/show nodes that are not VNGs, named graphs, or versions
        graph.forEachNode((nodeKey, attributes) => {
            const label = typeof attributes?.label === "string" ? attributes.label : "";
            const isVNG = label.startsWith(VERSIONED_NODE_PREFIX);
            const relations = attributes.metagraphRelations as { specialization?: boolean; location?: boolean } | undefined;
            const isNamedGraph = relations?.specialization === true;
            const isVersion = relations?.location === true;

            if (focusMode) {
                // In focus mode, show VNGs, named graphs, and versions
                graph.setNodeAttribute(nodeKey, "hidden", !(isVNG || isNamedGraph || isVersion));
            } else {
                // When not in focus mode, hide named graphs and versions (restore default)
                graph.setNodeAttribute(nodeKey, "hidden", isNamedGraph || isVersion);
            }
        });
    }, [focusMode, graph]);

    return null;
};
