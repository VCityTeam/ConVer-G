import { useCallback } from "react";
import { useAppDispatch, useAppSelector } from "../state/hooks";
import { setHighlightedNodes, setHighlightedEdges, clearHighlights } from "../state/versionedGraphSlice";
import type { GraphSearchOption } from "@react-sigma/graph-search";
import { useSigma } from "@react-sigma/core";

export const useSigmaSearch = () => {
  const dispatch = useAppDispatch();
  const sigma = useSigma();
  const highlightedNodes = useAppSelector((state) => state.versionedGraph.highlightedNodes);
  const highlightedEdges = useAppSelector((state) => state.versionedGraph.highlightedEdges);

  const findRelatedNodes = useCallback((nodeId: string): string[] => {
    const graph = sigma.getGraph();
    if (!graph.hasNode(nodeId)) return [nodeId];

    const parts = nodeId.split(":::");
    const originalId = parts[parts.length - 1];

    return graph.filterNodes((n) => n === originalId || n.endsWith(`:::${originalId}`));
  }, [sigma]);

  const findRelatedEdges = useCallback((edgeId: string): string[] => {
    const graph = sigma.getGraph();
    if (!graph.hasEdge(edgeId)) return [edgeId];

    const parts = edgeId.split(":::");
    const originalId = parts[parts.length - 1];

    return graph.filterEdges((e) => e === originalId || e.endsWith(`:::${originalId}`));
  }, [sigma]);

  const onFocus = useCallback((value: GraphSearchOption | null) => {
    if (value === null) {
      dispatch(clearHighlights());
    } else if (value.type === "nodes") {
      dispatch(setHighlightedEdges({ edges: [], source: "search" }));
      dispatch(setHighlightedNodes({ nodes: findRelatedNodes(value.id), source: "search" }));
    } else if (value.type === "edges") {
      dispatch(setHighlightedNodes({ nodes: [], source: "search" }));
      dispatch(setHighlightedEdges({ edges: findRelatedEdges(value.id), source: "search" }));
    }
  }, [dispatch, findRelatedNodes, findRelatedEdges]);

  const onChange = useCallback((value: GraphSearchOption | null) => {
    if (value === null) {
      dispatch(clearHighlights());
    } else if (value.type === "nodes") {
      dispatch(setHighlightedEdges({ edges: [], source: "search" }));
      dispatch(setHighlightedNodes({ nodes: findRelatedNodes(value.id), source: "search" }));
    } else if (value.type === "edges") {
      dispatch(setHighlightedNodes({ nodes: [], source: "search" }));
      dispatch(setHighlightedEdges({ edges: findRelatedEdges(value.id), source: "search" }));
    }
  }, [dispatch, findRelatedNodes, findRelatedEdges]);

  const postSearchResult = useCallback((options: GraphSearchOption[]): GraphSearchOption[] => {
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
    selectedNodes: highlightedNodes,
    selectedEdges: highlightedEdges,
    onFocus,
    onChange,
    postSearchResult,
  };
};
