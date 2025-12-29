import { useSigma } from "@react-sigma/core";
import { useCallback, useEffect, useState } from "react";
import { useAppSelector } from "../state/hooks";
import { getMetagraphNodeType, resolveTravelTarget } from "../utils/metagraphBuilder";

export const MetagraphHighlight = () => {
  const sigma = useSigma();
  const graph = sigma.getGraph();
  const currentView = useAppSelector((state) => state.metagraph.currentView);
  const selectedMetagraphNode = useAppSelector((state) => state.metagraph.selectedMetagraphNode);
  const selectedMetagraphNodeType = useAppSelector((state) => state.metagraph.selectedMetagraphNodeType);
  const [hoveredNode, setHoveredNode] = useState<string | null>(null);

  const clearNodeReducer = useCallback(() => {
    sigma.setSetting("nodeReducer", null);
  }, [sigma]);

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
    if (selectedMetagraphNode && (selectedMetagraphNodeType === "namedGraph" || selectedMetagraphNodeType === "version")) {
      sigma.setSetting("nodeReducer", (node, data) => {
        const base = { ...data };

        if (node === selectedMetagraphNode) {
          base.size = 12;
          base.highlighted = true;
          base.label = hoveredNode === node ? data.label : "";
        }

        return base;
      });

      return () => {
        clearNodeReducer();
      };
    }

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

    const vngNodes = graph.nodes().filter(node => getMetagraphNodeType(graph, node) === "vng");
    versionedGraphNode = vngNodes.find(node => {
      const target = resolveTravelTarget(graph, node);
      return target?.linkedGraph === selectedGraph && target?.linkedVersion === selectedVersion;
    }) || null;

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
  }, [currentView, graph, sigma, hoveredNode, selectedMetagraphNode, selectedMetagraphNodeType, clearNodeReducer]);

  return null;
};
