import { useCamera, useSigma } from "@react-sigma/core";
import { useCallback, useEffect, useState } from "react";
import { useAppSelector } from "../state/hooks";
import { getMetagraphNodeType, resolveTravelTarget } from "../utils/metagraphBuilder";
import { NODE_SIZES } from "../utils/constants";

interface NodeHighlightOptions {
  targetNode: string;
  size?: number;
  highlighted?: boolean;
  selected?: boolean;
}

export const MetagraphHighlight = () => {
  const sigma = useSigma();
  const { gotoNode } = useCamera();
  const graph = sigma.getGraph();
  const selectedGraph = useAppSelector((state) => state.versionedGraph.selectedGraph);
  const selectedVersion = useAppSelector((state) => state.versionedGraph.selectedVersion);
  const selectedMetagraphNode = useAppSelector((state) => state.metagraph.selectedMetagraphNode);
  const selectedMetagraphNodeType = useAppSelector((state) => state.metagraph.selectedMetagraphNodeType);
  const [hoveredNode, setHoveredNode] = useState<string | null>(null);

  const clearNodeReducer = useCallback(() => {
    sigma.setSetting("nodeReducer", null);
  }, [sigma]);

  const applyNodeReducer = useCallback((options: NodeHighlightOptions) => {
    const { targetNode, size = NODE_SIZES.HIGHLIGHTED, highlighted, selected } = options;

    sigma.setSetting("nodeReducer", (node, data) => {
      const base = { ...data };

      if (node === targetNode) {
        base.size = size;
        if (highlighted !== undefined) base.highlighted = highlighted;
        if (selected !== undefined) base.selected = selected;
        base.label = hoveredNode === node ? data.label : "";
      }

      return base;
    });

    return clearNodeReducer;
  }, [sigma, hoveredNode, clearNodeReducer]);

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
      return applyNodeReducer({
        targetNode: selectedMetagraphNode,
        size: NODE_SIZES.HIGHLIGHTED,
        selected: true,
      });
    }

    if (!selectedGraph || !selectedVersion) {
      clearNodeReducer();
      return;
    }

    const vngNodes = graph.nodes().filter(node => getMetagraphNodeType(graph, node) === "vng");
    const versionedGraphNode = vngNodes.find(node => {
      const target = resolveTravelTarget(graph, node);
      return target?.linkedGraph === selectedGraph && target?.linkedVersion === selectedVersion;
    }) || null;

    if (!versionedGraphNode) {
      clearNodeReducer();
      return;
    }

    return applyNodeReducer({
      targetNode: versionedGraphNode,
      size: NODE_SIZES.DEFAULT,
    });
  }, [selectedGraph, selectedVersion, graph, sigma, hoveredNode, selectedMetagraphNode, selectedMetagraphNodeType, clearNodeReducer, gotoNode, applyNodeReducer]);

  return null;
};
