import { useCamera, useSigma } from "@react-sigma/core";
import { type FC, useEffect } from "react";

export const FocusOnNodes: FC<{ nodes: string[]; move?: boolean }> = ({
  nodes,
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

  return null;
};
