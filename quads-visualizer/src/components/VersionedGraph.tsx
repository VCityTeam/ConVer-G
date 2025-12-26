import { type CSSProperties, type FC, useCallback, useEffect, useMemo } from "react";
import Graph from "graphology";
import type AbstractGraph from "graphology-types";
import { GraphSearch, type GraphSearchOption } from "@react-sigma/graph-search";
import {
  ControlsContainer,
  FullScreenControl,
  SigmaContainer,
} from "@react-sigma/core";
import "@react-sigma/core/lib/style.css";
import "@react-sigma/graph-search/lib/style.css";
import { useAppDispatch, useAppSelector } from "../state/hooks";
import { setCurrentView } from "../state/metagraphSlice";
import {
  resetVersionedGraphState,
  setFocusNode,
  setMergedGraphsEnabled,
  setSelectedGraph,
  setSelectedNode,
  setSelectedVersion,
} from "../state/versionedGraphSlice";
import { type Response } from "../utils/responseSerializer.ts";
import { computeGraphsDelta, mergeGraphs, useBuildVersionedGraph } from "../utils/versionedGraphBuilder.ts";
import { FocusOnNode } from "./FocusOnNode.tsx";
import { GraphInfoDisplay } from "./GraphInfoDisplay.tsx";
import { EdgeCurvedArrowProgram } from "@sigma/edge-curve";

export const VersionedGraph: FC<{
  response: Response;
  metagraph: Response | null;
  style?: CSSProperties;
}> = ({ response, metagraph, style }) => {
  const { distinctVersion, distinctGraph, versionedGraphs } = useBuildVersionedGraph(response, metagraph);
  const dispatch = useAppDispatch();
  const { selectedGraph, selectedVersion, selectedNode, focusNode, mergedGraphsEnabled } = useAppSelector(
    (state) => state.versionedGraph,
  );
  const externalSelection = useAppSelector((state) => state.metagraph.externalSelection);
  const travelHoverSelection = useAppSelector((state) => state.metagraph.travelHoverSelection);
  const selectedMetagraphNodeType = useAppSelector((state) => state.metagraph.selectedMetagraphNodeType);
  const graph = versionedGraphs[selectedGraph]?.[selectedVersion];

  // Show merged graphs option always (default disabled) - user can toggle to see merged view
  const showMergedGraphsOption = true;

  // Compute hover graph based on hover node type
  const hoverGraph = useMemo(() => {
    if (!travelHoverSelection) {
      return null;
    }

    const hoverNodeType = travelHoverSelection.nodeType;

    // VNG hover: show diff between current view and hovered VNG
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

    // Named graph hover: show all VNGs linked to this named graph
    if (hoverNodeType === "namedGraph" && travelHoverSelection.graph) {
      const hoveredGraph = travelHoverSelection.graph;
      const graphsToMerge: AbstractGraph[] = [];
      
      distinctVersion.forEach((version) => {
        const g = versionedGraphs[hoveredGraph]?.[version];
        if (g && g.order > 0) {
          graphsToMerge.push(g);
        }
      });

      if (graphsToMerge.length === 0) {
        return null;
      }

      if (mergedGraphsEnabled) {
        return mergeGraphs(graphsToMerge);
      } else {
        // For non-merged mode, return the first available graph as a preview
        // The full separate view will be handled by the component structure
        return mergeGraphs(graphsToMerge);
      }
    }

    // Version hover: show all VNGs linked to this version
    if (hoverNodeType === "version" && travelHoverSelection.version) {
      const hoveredVersion = travelHoverSelection.version;
      const graphsToMerge: AbstractGraph[] = [];
      
      distinctGraph.forEach((graphKey) => {
        const g = versionedGraphs[graphKey]?.[hoveredVersion];
        if (g && g.order > 0) {
          graphsToMerge.push(g);
        }
      });

      if (graphsToMerge.length === 0) {
        return null;
      }

      if (mergedGraphsEnabled) {
        return mergeGraphs(graphsToMerge);
      } else {
        // For non-merged mode, return merged as preview
        return mergeGraphs(graphsToMerge);
      }
    }

    return null;
  }, [travelHoverSelection, selectedGraph, selectedVersion, versionedGraphs, distinctVersion, distinctGraph, mergedGraphsEnabled]);

  // Compute merged graph for selected non-VNG node
  const mergedGraph = useMemo(() => {
    // Only compute merged graph when viewing a named graph or version (not VNG)
    if (selectedMetagraphNodeType !== "namedGraph" && selectedMetagraphNodeType !== "version") {
      return null;
    }

    if (!mergedGraphsEnabled) {
      return null;
    }

    const graphsToMerge: AbstractGraph[] = [];
    
    if (selectedMetagraphNodeType === "namedGraph" && selectedGraph) {
      // Merge all versions for the selected named graph
      distinctVersion.forEach((version) => {
        const g = versionedGraphs[selectedGraph]?.[version];
        if (g && g.order > 0) {
          graphsToMerge.push(g);
        }
      });
    } else if (selectedMetagraphNodeType === "version" && selectedVersion) {
      // Merge all named graphs for the selected version
      distinctGraph.forEach((graphKey) => {
        const g = versionedGraphs[graphKey]?.[selectedVersion];
        if (g && g.order > 0) {
          graphsToMerge.push(g);
        }
      });
    }

    if (graphsToMerge.length === 0) {
      return null;
    }

    return mergeGraphs(graphsToMerge);
  }, [mergedGraphsEnabled, selectedMetagraphNodeType, selectedGraph, selectedVersion, distinctVersion, distinctGraph, versionedGraphs]);

  // Get list of separate graphs when not in merged mode for non-VNG selection
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

  const handleMergedGraphsToggle = useCallback(() => {
    dispatch(setMergedGraphsEnabled(!mergedGraphsEnabled));
  }, [dispatch, mergedGraphsEnabled]);

  const mergedGraphsToggle = (
    <div
      style={{
        backgroundColor: "rgba(255, 255, 255, 0.9)",
        padding: "8px 12px",
        borderRadius: "5px",
        boxShadow: "0 2px 4px rgba(0,0,0,0.1)",
        display: "flex",
        alignItems: "center",
        gap: "8px",
        cursor: "pointer",
        userSelect: "none",
      }}
      onClick={handleMergedGraphsToggle}
    >
      <input
        type="checkbox"
        checked={mergedGraphsEnabled}
        onChange={handleMergedGraphsToggle}
        style={{ cursor: "pointer" }}
      />
      <label style={{ margin: 0, cursor: "pointer", fontSize: "14px" }}>
        Merged graphs
      </label>
    </div>
  );

  useEffect(() => {
    if (!selectedGraph && distinctGraph.length > 0) {
      dispatch(setSelectedGraph(distinctGraph[0]));
    }

    if (!selectedVersion && distinctVersion.length > 0) {
      dispatch(setSelectedVersion(distinctVersion[0]));
    }
  }, [dispatch, distinctGraph, distinctVersion, selectedGraph, selectedVersion]);

  useEffect(() => () => {
    dispatch(resetVersionedGraphState());
  }, [dispatch]);

  useEffect(() => {
    if (selectedGraph && selectedVersion) {
      dispatch(setCurrentView({ graph: selectedGraph, version: selectedVersion }));
    }
  }, [dispatch, selectedGraph, selectedVersion]);

  const onFocus = useCallback((value: GraphSearchOption | null) => {
    if (value === null) dispatch(setFocusNode(null));
    else if (value.type === "nodes") dispatch(setFocusNode(value.id));
  }, [dispatch]);

  const onChange = useCallback((value: GraphSearchOption | null) => {
    if (value === null) dispatch(setSelectedNode(null));
    else if (value.type === "nodes") dispatch(setSelectedNode(value.id));
  }, [dispatch]);

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === "ArrowLeft") {
        const currentIndex = distinctVersion.indexOf(selectedVersion);
        if (currentIndex > 0) {
          dispatch(setSelectedVersion(distinctVersion[currentIndex - 1]));
        }
      } else if (e.key === "ArrowRight") {
        const currentIndex = distinctVersion.indexOf(selectedVersion);
        if (currentIndex < distinctVersion.length - 1) {
          dispatch(setSelectedVersion(distinctVersion[currentIndex + 1]));
        }
      } else if (e.key === "ArrowUp") {
        const currentIndex = distinctGraph.indexOf(selectedGraph);
        if (currentIndex > 0) {
          dispatch(setSelectedGraph(distinctGraph[currentIndex - 1]));
        }
      } else if (e.key === "ArrowDown") {
        const currentIndex = distinctGraph.indexOf(selectedGraph);
        if (currentIndex < distinctGraph.length - 1) {
          dispatch(setSelectedGraph(distinctGraph[currentIndex + 1]));
        }
      }
    },
    [dispatch, distinctGraph, distinctVersion, selectedGraph, selectedVersion],
  );

  const postSearchResult = useCallback(
    (options: GraphSearchOption[]): GraphSearchOption[] => {
      return options.length <= 10
        ? options
        : [
          ...options.slice(0, 10),
          {
            type: "message",
            message: (
              <span className="text-center text-muted">
                And {options.length - 10} others
              </span>
            ),
          },
        ];
    },
    [],
  );

  useEffect(() => {
    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [handleKeyDown, selectedGraph, selectedVersion]);

  useEffect(() => {
    if (!externalSelection || externalSelection.origin !== "travel") {
      return;
    }

    const nextGraph = externalSelection.graph;
    if (nextGraph && nextGraph !== selectedGraph && distinctGraph.includes(nextGraph)) {
      dispatch(setSelectedGraph(nextGraph));
    }

    const nextVersion = externalSelection.version;
    if (nextVersion && nextVersion !== selectedVersion && distinctVersion.includes(nextVersion)) {
      dispatch(setSelectedVersion(nextVersion));
    }
  }, [dispatch, externalSelection, distinctGraph, distinctVersion, selectedGraph, selectedVersion]);

  // If we have separate graphs to display (non-merged mode with namedGraph or version selection)
  if (separateGraphs && separateGraphs.length > 0) {
    return (
      <div style={{ ...style, display: "flex", flexDirection: "column", gap: "4px", overflow: "auto", position: "relative" }}>
        {separateGraphs.map(({ graph: graphKey, version, data }) => (
          <div 
            key={`${graphKey}-${version}`} 
            style={{ 
              flex: 1, 
              minHeight: "300px", 
              position: "relative",
              border: "1px solid #e0e0e0",
              borderRadius: "4px",
            }}
          >
            <SigmaContainer
              settings={{
                allowInvalidContainer: true,
                defaultEdgeType: "arrow",
                edgeProgramClasses: {
                  curvedArrow: EdgeCurvedArrowProgram
                },
                renderEdgeLabels: true,
                autoRescale: true,
                autoCenter: true,
              }}
              style={{ height: "100%", width: "100%" }}
              graph={data}
            >
              <ControlsContainer position={"bottom-left"}>
                <GraphInfoDisplay graph={graphKey} version={version} />
              </ControlsContainer>
            </SigmaContainer>
          </div>
        ))}
        <div style={{ position: "sticky", bottom: 8, alignSelf: "flex-end", zIndex: 10 }}>
          {mergedGraphsToggle}
        </div>
      </div>
    );
  }

  return (
    <SigmaContainer
      settings={{
        allowInvalidContainer: true,
        defaultEdgeType: "arrow",
        edgeProgramClasses: {
          curvedArrow: EdgeCurvedArrowProgram
        },
        renderEdgeLabels: true,
        autoRescale: true,
        autoCenter: true,
      }}
      style={style}
      graph={displayedGraph}
    >
      <FocusOnNode node={focusNode ?? selectedNode} move={!focusNode} />
      <ControlsContainer position={"top-right"}>
        <FullScreenControl />
      </ControlsContainer>
      <ControlsContainer position={"top-left"}>
        <GraphSearch
          type="nodes"
          value={selectedNode ? { type: "nodes", id: selectedNode } : null}
          onFocus={onFocus}
          onChange={onChange}
          postSearchResult={postSearchResult}
        />
      </ControlsContainer>
      <ControlsContainer position={"bottom-left"}>
        <GraphInfoDisplay graph={selectedGraph} version={selectedVersion} />
      </ControlsContainer>
      {showMergedGraphsOption && (
        <ControlsContainer position={"bottom-right"}>
          {mergedGraphsToggle}
        </ControlsContainer>
      )}
    </SigmaContainer>
  );
};
