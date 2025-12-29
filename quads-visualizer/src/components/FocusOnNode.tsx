import { useCamera, useSigma } from "@react-sigma/core";
import { type FC, useEffect } from "react";

export const FocusOnNode: FC<{ node: string | null; move?: boolean }> = ({
  node,
  move,
}) => {
  const sigma = useSigma();
  const { gotoNode } = useCamera();

  useEffect(() => {
    if (!node) return;

    sigma.getGraph().setNodeAttribute(node, "highlighted", true);
    if (move) gotoNode(node);

    return () => {
      sigma.getGraph().setNodeAttribute(node, "highlighted", false);
    };
  }, [node, move, sigma, gotoNode]);

  return null;
};
