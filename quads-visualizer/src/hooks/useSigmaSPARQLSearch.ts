import { useCallback } from "react";
import { useSigma } from "@react-sigma/core";

export const useSigmaSPARQLSearch = () => {
  const sigma = useSigma();

  /**
   * Checks if a value exists as a node in the current sigma graph.
   * Supports both exact matches and prefixed node IDs (e.g., "prefix:::nodeId").
   */
  const isValueInGraph = useCallback((value: string): boolean => {
    const graph = sigma.getGraph();   
    
    // Check for exact match
    if (graph.hasNode(value)) {
      return true;
    }

    // Check if any node ends with the value (for prefixed nodes like "prefix:::nodeId")
    const matchingNodes = graph.filterNodes((nodeId) => 
      nodeId === value || nodeId.endsWith(`:::${value}`)
    );

    return matchingNodes.length > 0;
  }, [sigma]);

  const getFocusNodes = useCallback((value: string) => {
    const graph = sigma.getGraph();
    const focusNodes = graph.filterNodes((nodeId) => 
      nodeId === value || nodeId.endsWith(`:::${value}`)
    );
    return focusNodes;
  }, [sigma]);

  return {
    isValueInGraph,
    getFocusNodes,
  };
};
