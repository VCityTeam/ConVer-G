import { useMemo } from "react";
import type { AbstractGraph } from "graphology-types";
import type Graph from "graphology";
import { useAppSelector } from "../state/hooks";
import { computeGraphsDelta, mergeGraphs, useBuildVersionedGraph } from "../utils/versionedGraphBuilder";
import { type Response } from "../utils/responseSerializer";

export const useVersionedGraphLogic = (response: Response, metagraphResponse: Response | null) => {
  const { distinctVersion, distinctGraph, versionedGraphs } = useBuildVersionedGraph(response, metagraphResponse);
  
  const { selectedGraph, selectedVersion, mergedGraphsEnabled } = useAppSelector(
    (state) => state.versionedGraph,
  );
  
  const travelHoverSelection = useAppSelector((state) => state.metagraph.travelHoverSelection);
  const selectedMetagraphNodeType = useAppSelector((state) => state.metagraph.selectedMetagraphNodeType);

  const graph = versionedGraphs[selectedGraph]?.[selectedVersion];

  const hoverGraph = useMemo(() => {
    if (!travelHoverSelection) {
      return null;
    }

    const hoverNodeType = travelHoverSelection.nodeType;

    if (hoverNodeType === "vng") {
      if (!selectedGraph || !selectedVersion) {
        return null;
      }
      const baseGraph = versionedGraphs[selectedGraph]?.[selectedVersion] as Graph | undefined;
      const hoverGraphKey = travelHoverSelection.graph ?? selectedGraph;
      const hoverVersionKey = travelHoverSelection.version ?? selectedVersion;
      const targetGraph = versionedGraphs[hoverGraphKey]?.[hoverVersionKey] as Graph | undefined;

      if (!baseGraph || !targetGraph) {
        return null;
      }
      return computeGraphsDelta(baseGraph, targetGraph);
    }

    if (hoverNodeType === "namedGraph" && travelHoverSelection.graph) {
      const hoveredGraph = travelHoverSelection.graph;
      const graphsToMerge: AbstractGraph[] = [];
      
      distinctVersion.forEach((version) => {
        const g = versionedGraphs[hoveredGraph]?.[version];
        if (g && g.order > 0) {
          graphsToMerge.push(g);
        }
      });

      return graphsToMerge.length > 0 ? mergeGraphs(graphsToMerge) : null;
    }

    if (hoverNodeType === "version" && travelHoverSelection.version) {
      const hoveredVersion = travelHoverSelection.version;
      const graphsToMerge: AbstractGraph[] = [];
      
      distinctGraph.forEach((graphKey) => {
        const g = versionedGraphs[graphKey]?.[hoveredVersion];
        if (g && g.order > 0) {
          graphsToMerge.push(g);
        }
      });

      return graphsToMerge.length > 0 ? mergeGraphs(graphsToMerge) : null;
    }

    return null;
  }, [travelHoverSelection, selectedGraph, selectedVersion, versionedGraphs, distinctVersion, distinctGraph]);

  const mergedGraph = useMemo(() => {
    if (selectedMetagraphNodeType !== "namedGraph" && selectedMetagraphNodeType !== "version") {
      return null;
    }

    if (!mergedGraphsEnabled) {
      return null;
    }

    const graphsToMerge: AbstractGraph[] = [];
    
    if (selectedMetagraphNodeType === "namedGraph" && selectedGraph) {
      distinctVersion.forEach((version) => {
        const g = versionedGraphs[selectedGraph]?.[version];
        if (g && g.order > 0) {
          graphsToMerge.push(g);
        }
      });
    } else if (selectedMetagraphNodeType === "version" && selectedVersion) {
      distinctGraph.forEach((graphKey) => {
        const g = versionedGraphs[graphKey]?.[selectedVersion];
        if (g && g.order > 0) {
          graphsToMerge.push(g);
        }
      });
    }

    return graphsToMerge.length > 0 ? mergeGraphs(graphsToMerge) : null;
  }, [mergedGraphsEnabled, selectedMetagraphNodeType, selectedGraph, selectedVersion, distinctVersion, distinctGraph, versionedGraphs]);

  const separateGraphs = useMemo(() => {
    if (selectedMetagraphNodeType !== "namedGraph" && selectedMetagraphNodeType !== "version") {
      return null;
    }

    if (mergedGraphsEnabled) {
      return null;
    }

    const graphs: Array<{ graph: string; version: string; data: AbstractGraph }> = [];

    if (selectedMetagraphNodeType === "namedGraph" && selectedGraph) {
      distinctVersion.forEach((version) => {
        const g = versionedGraphs[selectedGraph]?.[version];
        if (g && g.order > 0) {
          graphs.push({ graph: selectedGraph, version, data: g });
        }
      });
    } else if (selectedMetagraphNodeType === "version" && selectedVersion) {
      distinctGraph.forEach((graphKey) => {
        const g = versionedGraphs[graphKey]?.[selectedVersion];
        if (g && g.order > 0) {
          graphs.push({ graph: graphKey, version: selectedVersion, data: g });
        }
      });
    }

    return graphs.length > 0 ? graphs : null;
  }, [selectedMetagraphNodeType, mergedGraphsEnabled, selectedGraph, selectedVersion, distinctVersion, distinctGraph, versionedGraphs]);

  const displayedGraph = hoverGraph ?? mergedGraph ?? graph;

  return {
    distinctVersion,
    distinctGraph,
    versionedGraphs,
    selectedGraph,
    selectedVersion,
    mergedGraphsEnabled,
    displayedGraph,
    separateGraphs,
    selectedMetagraphNodeType,
  };
};
