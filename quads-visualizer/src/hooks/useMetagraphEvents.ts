import { useRegisterEvents, useSigma } from "@react-sigma/core";
import { useCallback, useEffect, useRef } from "react";
import type { MouseCoords, SigmaEventPayload } from "sigma/types";
import { applyMetagraphNodeColors, getMetagraphNodeType, resolveTravelTarget } from "../utils/metagraphBuilder";
import { useAppDispatch } from "../state/hooks";
import { setTravelHoverSelection } from "../state/metagraphSlice";
import { navigateToMetagraphNode } from "../state/thunks";

export type BuilderMode = "createLink" | "createNode" | "travel" | "download" | "save";

interface MetagraphEventsCallbacks {
  onSetSourceNode: (node: string | null) => void;
  onSetTargetNode: (node: string | null) => void;
  onSetDragSourceNode: (node: string | null) => void;
  onSetPendingNodeCoord: (coord: { x: number; y: number } | null) => void;
  onSetNodeNameError: (error: string | null) => void;
}

export const useMetagraphEvents = (
  mode: BuilderMode | undefined,
  dragSourceNode: string | null,
  sourceNode: string | null,
  targetNode: string | null,
  callbacks: MetagraphEventsCallbacks,
) => {
  const sigma = useSigma();
  const registerEvents = useRegisterEvents();
  const dispatch = useAppDispatch();
  const selectedNodesRef = useRef<string[]>([]);
  const dragCompletionRef = useRef(false);
  const travelHoverNodeRef = useRef<string | null>(null);

  const handleTravelClick = useCallback((nodeKey: string) => {
    if (travelHoverNodeRef.current === nodeKey) {
      return;
    }

    const nodeType = getMetagraphNodeType(sigma.getGraph(), nodeKey);

    if (nodeType === "vng") {
      const target = resolveTravelTarget(sigma.getGraph(), nodeKey);
      if (!target) {
        travelHoverNodeRef.current = null;
        return;
      }
      travelHoverNodeRef.current = nodeKey;
      dispatch(navigateToMetagraphNode({ nodeKey, nodeType, graph: target.linkedGraph, version: target.linkedVersion }));
    } else if (nodeType === "namedGraph") {
      const graph = sigma.getGraph();
      const nodeLabel = graph.getNodeAttribute(nodeKey, "label") as string | undefined;
      travelHoverNodeRef.current = nodeKey;
      dispatch(navigateToMetagraphNode({ nodeKey, nodeType, graph: nodeLabel ?? nodeKey }));
    } else if (nodeType === "version") {
      const graph = sigma.getGraph();
      const nodeLabel = graph.getNodeAttribute(nodeKey, "label") as string | undefined;
      travelHoverNodeRef.current = nodeKey;
      dispatch(navigateToMetagraphNode({ nodeKey, nodeType, version: nodeLabel ?? nodeKey }));
    }
  }, [dispatch, sigma]);

  const handleTravelHover = useCallback((nodeKey: string) => {
    const nodeType = getMetagraphNodeType(sigma.getGraph(), nodeKey);

    if (nodeType === "vng") {
      const target = resolveTravelTarget(sigma.getGraph(), nodeKey);
      if (!target) {
        dispatch(setTravelHoverSelection(null));
        return;
      }
      dispatch(setTravelHoverSelection({
        updatedAt: Date.now(),
        graph: target.linkedGraph,
        version: target.linkedVersion,
        nodeType: "vng",
      }));
    } else if (nodeType === "namedGraph") {
      const graph = sigma.getGraph();
      const nodeLabel = graph.getNodeAttribute(nodeKey, "label") as string | undefined;
      dispatch(setTravelHoverSelection({
        updatedAt: Date.now(),
        graph: nodeLabel ?? nodeKey,
        nodeType: "namedGraph",
      }));
    } else if (nodeType === "version") {
      const graph = sigma.getGraph();
      const nodeLabel = graph.getNodeAttribute(nodeKey, "label") as string | undefined;
      dispatch(setTravelHoverSelection({
        updatedAt: Date.now(),
        version: nodeLabel ?? nodeKey,
        nodeType: "version",
      }));
    } else {
      dispatch(setTravelHoverSelection(null));
    }
  }, [dispatch, sigma]);

  const handleTravelLeave = useCallback(() => {
    travelHoverNodeRef.current = null;
    dispatch(setTravelHoverSelection(null));
  }, [dispatch]);

  const clearTravelState = useCallback(() => {
    travelHoverNodeRef.current = null;
    dispatch(setTravelHoverSelection(null));
  }, [dispatch]);

  // Register sigma events
  useEffect(() => {
    return registerEvents({
      downNode: (event) => {
        if (mode !== "createLink") return;
        dragCompletionRef.current = false;
        callbacks.onSetSourceNode(event.node);
        callbacks.onSetTargetNode(null);
        callbacks.onSetDragSourceNode(event.node);
      },
      enterNode: (event) => {
        if (mode === "createLink" && dragSourceNode) {
          if (event.node !== dragSourceNode) {
            callbacks.onSetTargetNode(event.node);
          }
          return;
        }
        if (mode === "travel") {
          handleTravelHover(event.node);
        }
      },
      leaveNode: () => {
        if (mode === "createLink" && dragSourceNode) {
          callbacks.onSetTargetNode(null);
          return;
        }
        if (mode === "travel") {
          handleTravelLeave();
        }
      },
      clickNode: (event) => {
        if (mode !== "travel") return;
        handleTravelClick(event.node);
      },
      upNode: (event) => {
        if (mode !== "createLink" || !dragSourceNode) return;
        if (event.node !== dragSourceNode) {
          callbacks.onSetTargetNode(event.node);
        }
        dragCompletionRef.current = true;
        callbacks.onSetDragSourceNode(null);
      },
      mouseup: () => {
        if (dragCompletionRef.current) {
          dragCompletionRef.current = false;
          return;
        }
        if (dragSourceNode) {
          callbacks.onSetDragSourceNode(null);
          callbacks.onSetTargetNode(null);
        }
      },
      mousemovebody: (coordinates: MouseCoords) => {
        if (mode !== "createLink" || !dragSourceNode) return;
        coordinates.preventSigmaDefault();
        const originalEvent = coordinates.original;
        originalEvent?.preventDefault?.();
        originalEvent?.stopPropagation?.();
      },
      clickStage: ({ event: coords, preventSigmaDefault }: SigmaEventPayload) => {
        if (mode !== "createNode") return;
        preventSigmaDefault();
        const coord = sigma.viewportToGraph(coords);
        callbacks.onSetNodeNameError(null);
        callbacks.onSetPendingNodeCoord(coord);
      },
    });
  }, [registerEvents, sigma, mode, dragSourceNode, handleTravelHover, handleTravelClick, handleTravelLeave, callbacks]);

  // Highlight selected nodes during link creation
  useEffect(() => {
    const graph = sigma.getGraph();
    const nodesToSelect = mode === "createLink"
      ? [sourceNode, targetNode].filter((node): node is string => Boolean(node))
      : [];
    const previousNodes = selectedNodesRef.current;

    previousNodes.forEach((node) => {
      if (!nodesToSelect.includes(node) && graph.hasNode(node)) {
        graph.setNodeAttribute(node, "highlighted", false);
      }
    });

    nodesToSelect.forEach((node) => {
      if (!previousNodes.includes(node) && graph.hasNode(node)) {
        graph.setNodeAttribute(node, "highlighted", true);
      }
    });

    selectedNodesRef.current = nodesToSelect;
  }, [sigma, mode, sourceNode, targetNode]);

  // Apply node colors based on mode
  useEffect(() => {
    const graph = sigma.getGraph();
    applyMetagraphNodeColors(graph, mode === "createLink" || mode === "travel");
  }, [mode, sigma]);

  // Cleanup highlights on unmount
  useEffect(() => () => {
    const graph = sigma.getGraph();
    selectedNodesRef.current.forEach((node) => {
      if (graph.hasNode(node)) {
        graph.setNodeAttribute(node, "highlighted", false);
      }
    });
    selectedNodesRef.current = [];
  }, [sigma]);

  return { dragCompletionRef, clearTravelState };
};
