import { useCamera, useSigma } from "@react-sigma/core";
import { type FC, useEffect } from "react";
import { useAppSelector } from "../state/hooks";

export const FocusOnNodesAndEdges: FC = () => {
  const sigma = useSigma();
  const { gotoNode } = useCamera();
  const { highlightedNodes, highlightedEdges, highlightSource } = useAppSelector(
    (state) => state.versionedGraph
  );
  const shouldMove = highlightSource === "search";

  useEffect(() => {
    if (highlightedNodes.length === 0) return;

    highlightedNodes.forEach(node => {
      if (sigma.getGraph().hasNode(node)) {
        sigma.getGraph().setNodeAttribute(node, "highlighted", true);
      }
    });
    if (shouldMove) gotoNode(highlightedNodes[0]);

    return () => {
      highlightedNodes.forEach(node => {
        if (sigma.getGraph().hasNode(node)) {
          sigma.getGraph().setNodeAttribute(node, "highlighted", false);
        }
      });
    };
  }, [highlightedNodes, shouldMove, sigma, gotoNode]);

  useEffect(() => {
    if (highlightedEdges.length === 0) return;

    const graph = sigma.getGraph();

    highlightedEdges.forEach(edge => {
      if (graph.hasEdge(edge)) {
        graph.setEdgeAttribute(edge, "highlighted", true);
      }
    });

    if (shouldMove && highlightedEdges.length > 0) {
      const firstEdge = highlightedEdges[0];
      if (graph.hasEdge(firstEdge)) {
        const sourceNode = graph.source(firstEdge);
        if (sourceNode) gotoNode(sourceNode);
      }
    }

    return () => {
      highlightedEdges.forEach(edge => {
        if (graph.hasEdge(edge)) {
          graph.setEdgeAttribute(edge, "highlighted", false);
        }
      });
    };
  }, [highlightedEdges, shouldMove, sigma, gotoNode]);

  return null;
};
