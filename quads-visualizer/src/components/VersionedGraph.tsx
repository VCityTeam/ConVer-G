import { type CSSProperties, type FC, useCallback, useEffect, useMemo } from "react";
import Graph from "graphology";
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
  setSelectedGraph,
  setSelectedNode,
  setSelectedVersion,
} from "../state/versionedGraphSlice";
import { type Response } from "../utils/responseSerializer.ts";
import { computeGraphsDelta, useBuildVersionedGraph } from "../utils/versionedGraphBuilder.ts";
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
  const { selectedGraph, selectedVersion, selectedNode, focusNode } = useAppSelector(
    (state) => state.versionedGraph,
  );
  const externalSelection = useAppSelector((state) => state.metagraph.externalSelection);
  const travelHoverSelection = useAppSelector((state) => state.metagraph.travelHoverSelection);
  const graph = versionedGraphs[selectedGraph]?.[selectedVersion];

  const deltaGraph = useMemo(() => {
    if (!travelHoverSelection || !selectedGraph || !selectedVersion) {
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
  }, [travelHoverSelection, selectedGraph, selectedVersion, versionedGraphs]);

  const displayedGraph = deltaGraph ?? graph;

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
    </SigmaContainer>
  );
};
