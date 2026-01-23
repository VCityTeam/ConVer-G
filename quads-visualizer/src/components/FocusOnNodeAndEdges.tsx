import { useCamera, useSigma } from "@react-sigma/core";
import { type FC, useEffect } from "react";

export const FocusOnNodesAndEdges: FC<{ nodes: string[]; edges: string[]; move?: boolean }> = ({
  nodes,
  edges,
  move,
}) => {
  const sigma = useSigma();
  const { gotoNode } = useCamera();

  useEffect(() => {
    if (nodes.length === 0) return;

    nodes.forEach(node => {
      if (sigma.getGraph().hasNode(node)) {
        sigma.getGraph().setNodeAttribute(node, "highlighted", true);
      }
    });
    if (move) gotoNode(nodes[0]);

    return () => {
      nodes.forEach(node => {
        if (sigma.getGraph().hasNode(node)) {
          sigma.getGraph().setNodeAttribute(node, "highlighted", false);
        }
      });
    };
  }, [nodes, move, sigma, gotoNode]);

  useEffect(() => {
    if (edges.length === 0) return;

    const graph = sigma.getGraph();

    edges.forEach(edge => {
      if (graph.hasEdge(edge)) {
        graph.setEdgeAttribute(edge, "highlighted", true);
      }
    });

    // Move camera to focus on the first edge's source node if move is enabled
    if (move && edges.length > 0) {
      const firstEdge = edges[0];
      if (graph.hasEdge(firstEdge)) {
        const sourceNode = graph.source(firstEdge);
        if (sourceNode) gotoNode(sourceNode);
      }
    }

    return () => {
      edges.forEach(edge => {
        if (graph.hasEdge(edge)) {
          graph.setEdgeAttribute(edge, "highlighted", false);
        }
      });
    };
  }, [edges, move, sigma, gotoNode]);

  return null;
};
