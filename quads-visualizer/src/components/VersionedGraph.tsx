import { type CSSProperties, type FC, useEffect, useCallback } from "react";
import type { Attributes } from "graphology-types";
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
import { FocusOnNodesAndEdges } from "./FocusOnNodeAndEdges.tsx";
import { GraphInfoDisplay } from "./GraphInfoDisplay.tsx";
import { SigmaGraph } from "./common/SigmaGraph.tsx";
import { GraphLabels } from "./common/GraphLabels.tsx";
import { useVersionedGraphLogic } from "../hooks/useVersionedGraphLogic.ts";
import { useVersionedGraphNavigation } from "../hooks/useVersionedGraphNavigation.ts";
import { MergedGraphsToggle } from "./MergedGraphsToggle.tsx";
import { useMemo } from "react";
import { mergeGraphsSeparately } from "../utils/versionedGraphBuilder.ts";
import { SigmaSearch } from "./common/SigmaSearch.tsx";
import { GridLayoutControls } from "./GridLayoutControls.tsx";

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

  const { focusNodes, focusEdges, selectedNodes, selectedEdges, gridColumns, gridGap } = useAppSelector((state) => state.versionedGraph);
  const { externalSelection, selectedMetagraphNodeType } = useAppSelector((state) => state.metagraph);

  useVersionedGraphNavigation(distinctGraph, distinctVersion, selectedGraph, selectedVersion);

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

  const Y_OFFSET = gridGap;
  const X_OFFSET = 300;

  const multiViewGraph = useMemo(() => {
    if (separateGraphs && separateGraphs.length > 0) {
      return mergeGraphsSeparately(separateGraphs, {
        columns: gridColumns,
        xOffset: X_OFFSET,
        yOffset: Y_OFFSET,
      });
    }
    return null;
  }, [separateGraphs, gridColumns, Y_OFFSET]);

  const isMultiView = separateGraphs && separateGraphs.length > 0 && multiViewGraph;
  const graphToDisplay = isMultiView ? multiViewGraph : displayedGraph;

  return (
    <SigmaGraph
      graph={graphToDisplay}
      edgeReducer={edgeReducer}
      style={style}
    >
      {
        isMultiView ? (
          <GraphLabels
            separateGraphs={separateGraphs}
            columns={gridColumns}
            xOffset={X_OFFSET}
            yOffset={Y_OFFSET}
          />
        ) : null
      }
      <FocusOnNodesAndEdges
        nodes={focusNodes.length > 0 ? focusNodes : selectedNodes}
        edges={focusEdges.length > 0 ? focusEdges : selectedEdges}
        move={focusNodes.length === 0}
      />
      <ControlsContainer position={"bottom-right"}>
        <FullScreenControl />
      </ControlsContainer>
      <ControlsContainer position={"top-left"}>
        <SigmaSearch />
      </ControlsContainer>
      {isMultiView && (
        <ControlsContainer position={"top-right"}>
          <GridLayoutControls />
        </ControlsContainer>
      )}
      <ControlsContainer position={"bottom-left"}>
        {
          selectedMetagraphNodeType === "vng" ? (
            <GraphInfoDisplay graph={selectedGraph} version={selectedVersion} />
          ) : (
            <MergedGraphsToggle enabled={mergedGraphsEnabled} />
          )
        }
      </ControlsContainer>
    </SigmaGraph>
  );
};
