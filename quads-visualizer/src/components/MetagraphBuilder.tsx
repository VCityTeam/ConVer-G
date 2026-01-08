import { useRegisterEvents, useSigma } from "@react-sigma/core";
import { type FC, useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { MouseCoords, SigmaEventPayload } from "sigma/types";
import { applyMetagraphNodeColors, createEmptyMetagraphRelations, getMetagraphNodeType, resolveTravelTarget, METAGRAPH_NODE_COLORS } from "../utils/metagraphBuilder.ts";
import { NewNodeModal } from "./NewNodeModal";
import { RelationInput } from "./RelationInput";
import { useAppDispatch } from "../state/hooks";
import { setExternalSelection, setSelectedMetagraphNode, setSelectedMetagraphNodeType, setTravelHoverSelection } from "../state/metagraphSlice";
import { QueryService } from "../services/QueryService";

type BuilderMode = "createLink" | "createNode" | "travel" | "download" | "save";

export const MetagraphBuilder: FC = () => {
  const sigma = useSigma();
  const registerEvents = useRegisterEvents();
  const dispatch = useAppDispatch();
  const [sourceNode, setSourceNode] = useState<string | null>(null);
  const [targetNode, setTargetNode] = useState<string | null>(null);
  const [relationValue, setRelationValue] = useState<string>("prov:wasDerivedFrom");
  const [pendingNodeCoord, setPendingNodeCoord] = useState<{ x: number; y: number } | null>(null);
  const [nodeNameError, setNodeNameError] = useState<string | null>(null);
  const [mode, setMode] = useState<BuilderMode>();
  const [dragSourceNode, setDragSourceNode] = useState<string | null>(null);
  const selectedNodesRef = useRef<string[]>([]);
  const dragCompletionRef = useRef(false);
  const travelHoverNodeRef = useRef<string | null>(null);

  const emitTravelSelection = useCallback((payload: { graph?: string; version?: string }) => {
    dispatch(setExternalSelection({
      origin: "travel",
      updatedAt: Date.now(),
      ...payload,
    }));
  }, [dispatch]);


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
      emitTravelSelection({ graph: target.linkedGraph, version: target.linkedVersion });
    } else if (nodeType === "namedGraph") {
      const graph = sigma.getGraph();
      const nodeLabel = graph.getNodeAttribute(nodeKey, "label") as string | undefined;
      travelHoverNodeRef.current = nodeKey;
      emitTravelSelection({ graph: nodeLabel ?? nodeKey });
    } else if (nodeType === "version") {
      const graph = sigma.getGraph();
      const nodeLabel = graph.getNodeAttribute(nodeKey, "label") as string | undefined;
      travelHoverNodeRef.current = nodeKey;
      emitTravelSelection({ version: nodeLabel ?? nodeKey });
    }
  }, [emitTravelSelection, sigma]);

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

  const [showSaveMenu, setShowSaveMenu] = useState(false);
  const [showEditMenu, setShowEditMenu] = useState(false);

  const saveOptions = useMemo(() => (
    [
      { id: "download" as const, label: "💾 Locally", helper: "Download the graph as TTL" },
      { id: "save" as const, label: "📤 Online", helper: "Upload the metagraph to ConVer-G" },
    ]
  ), []);

  const editOptions = useMemo(() => (
    [
      { id: "createLink" as const, label: "🪢 New Link", helper: "Drag-and-drop nodes to link" },
      { id: "createNode" as const, label: "⭕ New Node", helper: "Click canvas to create a new node" },
    ]
  ), []);

  const modeOptions = useMemo(() => (
    [
      { id: "travel" as const, label: "🏃‍♀️‍➡️", helper: "Change versioned graph" }
    ]
  ), []);

  useEffect(() => {
    return registerEvents({
      downNode: (event) => {
        if (mode !== "createLink") {
          return;
        }

        dragCompletionRef.current = false;
        setSourceNode(event.node);
        setTargetNode(null);
        setDragSourceNode(event.node);
      },
      enterNode: (event) => {
        if (mode === "createLink" && dragSourceNode) {
          if (event.node !== dragSourceNode) {
            setTargetNode(event.node);
          }
          return;
        }

        if (mode === "travel") {
          handleTravelHover(event.node);
        }
      },
      leaveNode: (event) => {
        if (mode === "createLink" && dragSourceNode) {
          setTargetNode((current) => (current === event.node ? null : current));
          return;
        }

        if (mode === "travel") {
          handleTravelLeave();
        }
      },
      clickNode: (event) => {
        if (mode !== "travel") {
          return;
        }

        const nodeType = getMetagraphNodeType(sigma.getGraph(), event.node);
        dispatch(setSelectedMetagraphNodeType(nodeType));
        dispatch(setSelectedMetagraphNode(event.node));
        handleTravelClick(event.node);
      },
      upNode: (event) => {
        if (mode !== "createLink" || !dragSourceNode) {
          return;
        }

        if (event.node !== dragSourceNode) {
          setTargetNode(event.node);
        }
        dragCompletionRef.current = true;
        setDragSourceNode(null);
      },
      mouseup: () => {
        if (dragCompletionRef.current) {
          dragCompletionRef.current = false;
          return;
        }

        if (dragSourceNode) {
          setDragSourceNode(null);
          setTargetNode(null);
        }
      },
      mousemovebody: (coordinates: MouseCoords) => {
        if (mode !== "createLink" || !dragSourceNode) {
          return;
        }

        coordinates.preventSigmaDefault();
        const originalEvent = coordinates.original;
        originalEvent?.preventDefault?.();
        originalEvent?.stopPropagation?.();
      },
      clickStage: ({ event: coords, preventSigmaDefault }: SigmaEventPayload) => {
        if (mode !== "createNode") {
          return;
        }

        preventSigmaDefault();
        const coord = sigma.viewportToGraph(coords);
        setNodeNameError(null);
        setPendingNodeCoord(coord);
      },
    });
  }, [registerEvents, sigma, mode, dragSourceNode, handleTravelHover, handleTravelClick, handleTravelLeave, dispatch]);

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

  useEffect(() => {
    const graph = sigma.getGraph();
    applyMetagraphNodeColors(graph, mode === "createLink" || mode === "travel");
  }, [mode, sigma]);

  useEffect(() => () => {
    const graph = sigma.getGraph();
    selectedNodesRef.current.forEach((node) => {
      if (graph.hasNode(node)) {
        graph.setNodeAttribute(node, "highlighted", false);
      }
    });
    selectedNodesRef.current = [];
  }, [sigma]);

  const generateTTL = () => {
    const graph = sigma.getGraph();
    let ttl = "";

    const formatTerm = (term: string, type?: string) => {
      if (type === "literal") {
        return `"${term.replace(/"/g, '\\"')}"`;
      }
      return `<${term}>`;
    };

    graph.forEachEdge((_edge, attributes, source, target) => {
      const sourceNode = graph.getNodeAttributes(source);
      const targetNode = graph.getNodeAttributes(target);

      const s = formatTerm(source, sourceNode.termType);
      const p = `<${attributes.label}>`;
      const o = formatTerm(target, targetNode.termType);

      ttl += `${s} ${p} ${o} .\n`;
    });
    return ttl;
  };

  const downloadGraphAsTTL = () => {
    const ttl = generateTTL();
    const blob = new Blob([ttl], { type: "text/turtle" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "metagraph.ttl";
    a.click();
    URL.revokeObjectURL(url);
  }

  const handleModeChange = async (nextMode: BuilderMode) => {
    if (nextMode === "download") {
      downloadGraphAsTTL();
      return;
    }

    if (nextMode === "save") {
      const ttl = generateTTL();
      try {
        await QueryService.uploadMetadata(ttl);
        alert("Metagraph uploaded successfully!");
      } catch (e) {
        console.error(e);
        alert("Failed to upload metagraph.");
      }
      return;
    }

    if (nextMode === mode) {
      setMode(undefined);
      setSourceNode(null);
      setTargetNode(null);
      setDragSourceNode(null);
      dragCompletionRef.current = false;
      setPendingNodeCoord(null);
      setNodeNameError(null);
      travelHoverNodeRef.current = null;
      dispatch(setTravelHoverSelection(null));
      return;
    }

    setMode(nextMode);
    if (nextMode !== "createLink") {
      setSourceNode(null);
      setTargetNode(null);
      setDragSourceNode(null);
      dragCompletionRef.current = false;
    }
    if (nextMode !== "createNode") {
      setPendingNodeCoord(null);
      setNodeNameError(null);
    }
    if (nextMode !== "travel") {
      travelHoverNodeRef.current = null;
      dispatch(setTravelHoverSelection(null));
    }
  };

  const computeTermType = (name: string): string => {
    if (name.startsWith("http://") || name.startsWith("https://")) {
      return "resource";
    }
    return "literal";
  }

  const handleCreateNode = (name: string) => {
    if (!pendingNodeCoord) {
      return;
    }

    const graph = sigma.getGraph();
    if (graph.hasNode(name)) {
      setNodeNameError("A node with this name already exists.");
      return;
    }

    graph.addNode(name, {
      ...pendingNodeCoord,
      termType: computeTermType(name),
      size: 10,
      color: METAGRAPH_NODE_COLORS.default,
      label: name,
      metagraphRelations: createEmptyMetagraphRelations(),
    });

    applyMetagraphNodeColors(graph);

    setPendingNodeCoord(null);
    setNodeNameError(null);
  };

  const handleCancelNewNode = () => {
    setPendingNodeCoord(null);
    setNodeNameError(null);
  };

  const handleAddEdge = () => {
    if (mode !== "createLink") {
      return;
    }
    let finalRelation = trimmedRelation;

    if (finalRelation.startsWith("prov:")) {
      finalRelation = finalRelation.replace("prov:", "http://www.w3.org/ns/prov#");
    }

    if (sourceNode && targetNode && finalRelation) {
      const graph = sigma.getGraph();

      graph.addEdge(sourceNode, targetNode, {
        label: finalRelation,
        relation: finalRelation,
        size: 4
      });

      applyMetagraphNodeColors(graph, true);

      setSourceNode(null);
      setTargetNode(null);
      setDragSourceNode(null);
      dragCompletionRef.current = false;
    }
  };

  const handleClear = () => {
    setSourceNode(null);
    setTargetNode(null);
    setDragSourceNode(null);
    dragCompletionRef.current = false;
  };

  const isCreateLinkMode = mode === "createLink";
  const trimmedRelation = relationValue.trim();
  const canAddLink = Boolean(sourceNode && targetNode && isCreateLinkMode && trimmedRelation.length > 0);

  return (
    <>
      <div style={{ backgroundColor: "white", padding: "10px", display: "flex", flexDirection: "column", gap: "5px" }}>
        <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
          <div className="metagraph-builder-buttons">
            <div
              style={{ position: "relative", display: "inline-block", flex: 1 }}
              onMouseEnter={() => setShowSaveMenu(true)}
              onMouseLeave={() => setShowSaveMenu(false)}
              onClick={() => setShowSaveMenu(!showSaveMenu)}
            >
              <button
                type="button"
                title="Save options"
                style={{
                  width: "100%",
                  padding: "5px",
                  cursor: "pointer",
                  border: "1px solid #ccc",
                  backgroundColor: "#f7f7f7",
                  borderRadius: "4px",
                }}
              >
                Save
              </button>
              {showSaveMenu && (
                <div
                  style={{
                    position: "absolute",
                    top: "100%",
                    left: 0,
                    backgroundColor: "white",
                    border: "1px solid #ccc",
                    borderRadius: "4px",
                    boxShadow: "0 2px 8px rgba(0,0,0,0.15)",
                    zIndex: 100,
                    minWidth: "100%",
                    display: "flex",
                    flexDirection: "column",
                  }}
                >
                  {saveOptions.map(({ id, label, helper }) => (
                    <button
                      key={id}
                      type="button"
                      onClick={() => {
                        handleModeChange(id);
                        setShowSaveMenu(false);
                      }}
                      title={helper}
                      style={{
                        padding: "8px 12px",
                        cursor: "pointer",
                        border: "none",
                        backgroundColor: "transparent",
                        textAlign: "left",
                        whiteSpace: "nowrap",
                        zIndex: 2,
                      }}
                      onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = "#f0f0f0")}
                      onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = "transparent")}
                    >
                      {label}
                    </button>
                  ))}
                </div>
              )}
            </div>
            <div
              style={{ position: "relative", display: "inline-block", flex: 1 }}
              onMouseEnter={() => setShowEditMenu(true)}
              onMouseLeave={() => setShowEditMenu(false)}
              onClick={() => setShowEditMenu(!showEditMenu)}
            >
              <button
                type="button"
                title="Edit options"
                style={{
                  width: "100%",
                  padding: "5px",
                  cursor: "pointer",
                  border: (mode === "createLink" || mode === "createNode") ? "2px solid #0077ff" : "1px solid #ccc",
                  backgroundColor: (mode === "createLink" || mode === "createNode") ? "#e8f2ff" : "#f7f7f7",
                  borderRadius: "4px",
                  fontWeight: (mode === "createLink" || mode === "createNode") ? "bold" : "normal",
                }}
              >
                Edit
              </button>
              {showEditMenu && (
                <div
                  style={{
                    position: "absolute",
                    top: "100%",
                    left: 0,
                    backgroundColor: "white",
                    border: "1px solid #ccc",
                    borderRadius: "4px",
                    boxShadow: "0 2px 8px rgba(0,0,0,0.15)",
                    zIndex: 100,
                    minWidth: "100%",
                    display: "flex",
                    flexDirection: "column",
                  }}
                >
                  {editOptions.map(({ id, label, helper }) => (
                    <button
                      key={id}
                      type="button"
                      onClick={() => {
                        handleModeChange(id);
                        setShowEditMenu(false);
                      }}
                      title={helper}
                      style={{
                        padding: "8px 12px",
                        cursor: "pointer",
                        border: "none",
                        backgroundColor: id === mode ? "#e8f2ff" : "transparent",
                        textAlign: "left",
                        whiteSpace: "nowrap",
                        fontWeight: id === mode ? "bold" : "normal",
                      }}
                      onMouseEnter={(e) => { if (id !== mode) e.currentTarget.style.backgroundColor = "#f0f0f0"; }}
                      onMouseLeave={(e) => { if (id !== mode) e.currentTarget.style.backgroundColor = "transparent"; }}
                    >
                      {label}
                    </button>
                  ))}
                </div>
              )}
            </div>
            {modeOptions.map(({ id, label, helper }) => (
              <button
                key={id}
                type="button"
                onClick={() => handleModeChange(id)}
                title={helper}
                style={{
                  flex: 1,
                  padding: "5px",
                  cursor: "pointer",
                  border: id === mode ? "2px solid #0077ff" : "1px solid #ccc",
                  backgroundColor: id === mode ? "#e8f2ff" : "#f7f7f7",
                  borderRadius: "4px",
                  fontWeight: id === mode ? "bold" : "normal",
                }}
              >
                {label}
              </button>
            ))}
          </div>
        </div>
        {
          isCreateLinkMode && (
            <>
              <RelationInput value={relationValue} onChange={setRelationValue} disabled={!isCreateLinkMode} />

              <div style={{ display: "flex", gap: "5px" }}>
                <button
                  onClick={handleAddEdge}
                  disabled={!canAddLink}
                  style={{ flex: 1, padding: "5px", cursor: canAddLink ? "pointer" : "not-allowed" }}
                >
                  Add Link
                </button>
                <button onClick={handleClear} style={{ padding: "5px", cursor: "pointer" }}>
                  Clear
                </button>
              </div>
            </>
          )
        }
      </div>
      <NewNodeModal
        isOpen={Boolean(pendingNodeCoord)}
        errorMessage={nodeNameError}
        onSubmit={handleCreateNode}
        onCancel={handleCancelNewNode}
      />
    </>
  );
};
