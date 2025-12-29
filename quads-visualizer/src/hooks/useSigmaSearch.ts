import { useCallback } from "react";
import { useAppDispatch, useAppSelector } from "../state/hooks";
import { setFocusNodes, setSelectedNodes } from "../state/versionedGraphSlice";
import type { GraphSearchOption } from "@react-sigma/graph-search";
import { useSigma } from "@react-sigma/core";

export const useSigmaSearch = () => {
  const dispatch = useAppDispatch();
  const sigma = useSigma();
  const selectedNodes = useAppSelector((state) => state.versionedGraph.selectedNodes);

  const findRelatedNodes = useCallback((nodeId: string): string[] => {
    const graph = sigma.getGraph();
    if (!graph.hasNode(nodeId)) return [nodeId];

    const parts = nodeId.split(":::");
    const originalId = parts[parts.length - 1];

    return graph.filterNodes((n) => n === originalId || n.endsWith(`:::${originalId}`));
  }, [sigma]);

  const onFocus = useCallback((value: GraphSearchOption | null) => {
    if (value === null) dispatch(setFocusNodes([]));
    else if (value.type === "nodes") dispatch(setFocusNodes(findRelatedNodes(value.id)));
  }, [dispatch, findRelatedNodes]);

  const onChange = useCallback((value: GraphSearchOption | null) => {
    if (value === null) dispatch(setSelectedNodes([]));
    else if (value.type === "nodes") dispatch(setSelectedNodes(findRelatedNodes(value.id)));
  }, [dispatch, findRelatedNodes]);

  const postSearchResult = useCallback(
    (options: GraphSearchOption[]): GraphSearchOption[] => {
      // Filter out duplicates based on the original ID if they are prefixed
      const seen = new Set<string>();
      const uniqueOptions: GraphSearchOption[] = [];

      options.forEach(option => {
        if (option.type === "nodes") {
          const parts = option.id.split(":::");
          const originalId = parts[parts.length - 1];
          if (!seen.has(originalId)) {
            seen.add(originalId);
            uniqueOptions.push(option);
          }
        } else {
          uniqueOptions.push(option);
        }
      });

      return uniqueOptions.length <= 10
        ? uniqueOptions
        : [
          ...uniqueOptions.slice(0, 10),
          {
            type: "message",
            message: "And " + (uniqueOptions.length - 10) + " others",
          },
        ];
    },
    [],
  );

  return {
    selectedNodes,
    onFocus,
    onChange,
    postSearchResult,
  };
};
