import { type CSSProperties, type FC, useEffect, useCallback } from "react";
import type { Attributes } from "graphology-types";
import { GraphSearch } from "@react-sigma/graph-search";
import {
  ControlsContainer,
  FullScreenControl,
} from "@react-sigma/core";
import "@react-sigma/graph-search/lib/style.css";
import { useAppDispatch, useAppSelector } from "../state/hooks";
import { setCurrentView } from "../state/metagraphSlice";
import {
  resetVersionedGraphState,
  setSelectedGraph,
  setSelectedVersion,
} from "../state/versionedGraphSlice";
import { type Response } from "../utils/responseSerializer.ts";
import { FocusOnNode } from "./FocusOnNode.tsx";
import { GraphInfoDisplay } from "./GraphInfoDisplay.tsx";
import { SigmaGraph } from "./common/SigmaGraph.tsx";
import { useVersionedGraphLogic } from "../hooks/useVersionedGraphLogic.ts";
import { useVersionedGraphNavigation } from "../hooks/useVersionedGraphNavigation.ts";
import { useSigmaSearch } from "../hooks/useSigmaSearch.ts";
import { MergedGraphsToggle } from "./MergedGraphsToggle.tsx";

export const VersionedGraph: FC<{
  response: Response;
  metagraph: Response | null;
  style?: CSSProperties;
}> = ({ response, metagraph, style }) => {
  const dispatch = useAppDispatch();
  const {
    distinctVersion,
    distinctGraph,
    selectedGraph,
    selectedVersion,
    mergedGraphsEnabled,
    displayedGraph,
    separateGraphs,
  } = useVersionedGraphLogic(response, metagraph);

  const { focusNode } = useAppSelector((state) => state.versionedGraph);
  const externalSelection = useAppSelector((state) => state.metagraph.externalSelection);

  useVersionedGraphNavigation(distinctGraph, distinctVersion, selectedGraph, selectedVersion);
  const { selectedNode, onFocus, onChange, postSearchResult } = useSigmaSearch();

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

  const edgeReducer = useCallback((_edge: string, data: Attributes) => {
    const res = { ...data };
    if (res.status === "deleted") {
      res.color = "#d14343";
    } else if (res.status === "added") {
      res.color = "#2e8b57";
    } else {
      res.color = "#d3d3d3";
    }
    res.labelColor = "#555555";
    return res;
  }, []);

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
            <SigmaGraph
              graph={data}
              edgeReducer={edgeReducer}
              style={{ height: "100%", width: "100%" }}
            >
              <ControlsContainer position={"bottom-left"}>
                <GraphInfoDisplay graph={graphKey} version={version} />
              </ControlsContainer>
            </SigmaGraph>
          </div>
        ))}
        <div style={{ position: "sticky", bottom: 8, alignSelf: "flex-end", zIndex: 10 }}>
          <MergedGraphsToggle enabled={mergedGraphsEnabled} />
        </div>
      </div>
    );
  }

  return (
    <SigmaGraph
      graph={displayedGraph}
      edgeReducer={edgeReducer}
      style={style}
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
      <ControlsContainer position={"bottom-right"}>
        <MergedGraphsToggle enabled={mergedGraphsEnabled} />
      </ControlsContainer>
    </SigmaGraph>
  );
};
