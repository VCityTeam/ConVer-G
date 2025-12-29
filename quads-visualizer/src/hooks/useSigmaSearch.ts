import { useCallback } from "react";
import { useAppDispatch, useAppSelector } from "../state/hooks";
import { setFocusNode, setSelectedNode } from "../state/versionedGraphSlice";
import type { GraphSearchOption } from "@react-sigma/graph-search";

export const useSigmaSearch = () => {
  const dispatch = useAppDispatch();
  const selectedNode = useAppSelector((state) => state.versionedGraph.selectedNode);

  const onFocus = useCallback((value: GraphSearchOption | null) => {
    if (value === null) dispatch(setFocusNode(null));
    else if (value.type === "nodes") dispatch(setFocusNode(value.id));
  }, [dispatch]);

  const onChange = useCallback((value: GraphSearchOption | null) => {
    if (value === null) dispatch(setSelectedNode(null));
    else if (value.type === "nodes") dispatch(setSelectedNode(value.id));
  }, [dispatch]);

  const postSearchResult = useCallback(
    (options: GraphSearchOption[]): GraphSearchOption[] => {
      return options.length <= 10
        ? options
        : [
          ...options.slice(0, 10),
          {
            type: "message",
            message: "And " + (options.length - 10) + " others",
          },
        ];
    },
    [],
  );

  return {
    selectedNode,
    onFocus,
    onChange,
    postSearchResult,
  };
};
