import { useSigma } from "@react-sigma/core";
import { useEffect, useState } from "react";
import { useAppSelector } from "../state/hooks";
import { METAGRAPH_RELATION_SUFFIXES } from "../utils/metagraphBuilder";

export const MetagraphHighlight = () => {
  const sigma = useSigma();
  const graph = sigma.getGraph();
  const currentView = useAppSelector((state) => state.metagraph.currentView);
  const [hoveredNode, setHoveredNode] = useState<string | null>(null);

  const clearNodeReducer = () => {
    sigma.setSetting("nodeReducer", (_node, data) => ({ ...data }));
  };

  useEffect(() => {
    const handleEnter = ({ node }: { node: string }) => setHoveredNode(node);
    const handleLeave = () => setHoveredNode(null);

    sigma.on("enterNode", handleEnter);
    sigma.on("leaveNode", handleLeave);

    return () => {
      sigma.off("enterNode", handleEnter);
      sigma.off("leaveNode", handleLeave);
    };
  }, [sigma]);

  useEffect(() => {
    if (!currentView) {
      clearNodeReducer();
      return;
    }

    const { graph: selectedGraph, version: selectedVersion } = currentView;

    if (!selectedGraph || !selectedVersion) {
      clearNodeReducer();
      return;
    }
    
    let versionedGraphNode: string | null = null;

    if (graph.hasNode(selectedGraph)) {
      const candidates = graph.inNeighbors(selectedGraph);
      versionedGraphNode = candidates.find(candidate => {
        const hasSpecialization = graph.someOutEdge(candidate, (_edge, attr, _source, target) => {
          return target === selectedGraph && (attr.label as string).endsWith(METAGRAPH_RELATION_SUFFIXES.specialization);
        });

        if (!hasSpecialization) return false;
        if (!graph.hasNode(selectedVersion)) return false;
        
        const hasLocation = graph.someOutEdge(candidate, (_edge, attr, _source, target) => {
          return target === selectedVersion && (attr.label as string).endsWith(METAGRAPH_RELATION_SUFFIXES.location);
        });

        return hasLocation;
      }) || null;
    }

    if (!versionedGraphNode) {
      clearNodeReducer();
      return;
    }

    sigma.setSetting("nodeReducer", (node, data) => {
      const base = { ...data };

      if (node === versionedGraphNode) {
        base.size = 10;
        base.label = hoveredNode === node ? data.label : "";
      }

      return base;
    });

    return () => {
      clearNodeReducer();
    };
  }, [currentView, graph, sigma, hoveredNode]);

  return null;
};
