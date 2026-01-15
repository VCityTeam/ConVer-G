import { useRegisterEvents, useSigma } from "@react-sigma/core";
import { type FC, useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { MouseCoords, SigmaEventPayload } from "sigma/types";
import { applyMetagraphNodeColors, createEmptyMetagraphRelations, getMetagraphNodeType, resolveTravelTarget, METAGRAPH_NODE_COLORS } from "../utils/metagraphBuilder.ts";
import { NewNodeModal } from "./NewNodeModal";
import { RelationInput } from "./RelationInput";
import { useAppDispatch, useAppSelector } from "../state/hooks";
import { setExternalSelection, setFocusMode, setSelectedMetagraphNode, setSelectedMetagraphNodeType, setShowClusters, setTravelHoverSelection } from "../state/metagraphSlice";
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
  const [showDisplayMenu, setShowDisplayMenu] = useState(false);
  const showClusters = useAppSelector((state) => state.metagraph.showClusters);
  const focusMode = useAppSelector((state) => state.metagraph.focusMode);

  const saveOptions = useMemo(() => (
    [
      { id: "download" as const, label: "💾 Download", helper: "Download the graph as TTL" },
      { id: "save" as const, label: "📤 Save Online", helper: "Upload the metagraph to ConVer-G" },
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

  const viewOptions = useMemo(() => (
    [
      { id: "showClusters" as const, label: "Show Clusters", helper: "Toggle cluster panel visibility", checked: showClusters },
      { id: "focusMode" as const, label: "Focus Mode", helper: "Show only cluster-related edges and nodes", checked: focusMode },
    ]
  ), [showClusters, focusMode]);

  // Shared dropdown styles
  const dropdownContainerStyle: React.CSSProperties = {
    position: "relative",
    display: "inline-block",
    flex: 1,
  };

  const dropdownButtonStyle = (isActive: boolean = false): React.CSSProperties => ({
    width: "100%",
    padding: "6px 10px",
    cursor: "pointer",
    border: isActive ? "2px solid #0077ff" : "1px solid #d1d5db",
    backgroundColor: isActive ? "#e8f2ff" : "#fafafa",
    borderRadius: "6px",
    fontWeight: isActive ? 600 : 500,
    fontSize: "13px",
    color: "#374151",
    transition: "all 0.15s ease",
  });

  const dropdownMenuStyle: React.CSSProperties = {
    position: "absolute",
    top: "100%",
    left: 0,
    backgroundColor: "white",
    border: "1px solid #e5e7eb",
    borderRadius: "4px",
    zIndex: 100,
    minWidth: "100%",
    display: "flex",
    flexDirection: "column",
    overflow: "hidden",
  };

  const dropdownItemStyle = (isActive: boolean = false): React.CSSProperties => ({
    padding: "8px 14px",
    cursor: "pointer",
    border: "none",
    backgroundColor: isActive ? "#e8f2ff" : "transparent",
    textAlign: "left",
    whiteSpace: "nowrap",
    fontWeight: isActive ? 600 : 400,
    fontSize: "13px",
    color: "#374151",
    transition: "background-color 0.1s ease",
  });

  const handleItemHover = (e: React.MouseEvent<HTMLButtonElement>, isActive: boolean = false) => {
    if (!isActive) {
      e.currentTarget.style.backgroundColor = "#f3f4f6";
    }
  };

  const handleItemLeave = (e: React.MouseEvent<HTMLButtonElement>, isActive: boolean = false) => {
    if (!isActive) {
      e.currentTarget.style.backgroundColor = "transparent";
    }
  };

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
            {/* File dropdown */}
            <div
              style={dropdownContainerStyle}
              onMouseEnter={() => setShowSaveMenu(true)}
              onMouseLeave={() => setShowSaveMenu(false)}
              onClick={() => setShowSaveMenu(!showSaveMenu)}
            >
              <button
                type="button"
                title="File options"
                style={dropdownButtonStyle(false)}
              >
                File
              </button>
              {showSaveMenu && (
                <div style={dropdownMenuStyle}>
                  {saveOptions.map(({ id, label, helper }) => (
                    <button
                      key={id}
                      type="button"
                      onClick={() => {
                        handleModeChange(id);
                        setShowSaveMenu(false);
                      }}
                      title={helper}
                      style={dropdownItemStyle(false)}
                      onMouseEnter={(e) => handleItemHover(e, false)}
                      onMouseLeave={(e) => handleItemLeave(e, false)}
                    >
                      {label}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Edit dropdown */}
            <div
              style={dropdownContainerStyle}
              onMouseEnter={() => setShowEditMenu(true)}
              onMouseLeave={() => setShowEditMenu(false)}
              onClick={() => setShowEditMenu(!showEditMenu)}
            >
              <button
                type="button"
                title="Edit options"
                style={dropdownButtonStyle(mode === "createLink" || mode === "createNode")}
              >
                Edit
              </button>
              {showEditMenu && (
                <div style={dropdownMenuStyle}>
                  {editOptions.map(({ id, label, helper }) => {
                    const isActive = id === mode;
                    return (
                      <button
                        key={id}
                        type="button"
                        onClick={() => {
                          handleModeChange(id);
                          setShowEditMenu(false);
                        }}
                        title={helper}
                        style={dropdownItemStyle(isActive)}
                        onMouseEnter={(e) => handleItemHover(e, isActive)}
                        onMouseLeave={(e) => handleItemLeave(e, isActive)}
                      >
                        {label}
                      </button>
                    );
                  })}
                </div>
              )}
            </div>

            {/* View dropdown */}
            <div
              style={dropdownContainerStyle}
              onMouseEnter={() => setShowDisplayMenu(true)}
              onMouseLeave={() => setShowDisplayMenu(false)}
              onClick={() => setShowDisplayMenu(!showDisplayMenu)}
            >
              <button
                type="button"
                title="View options"
                style={dropdownButtonStyle(false)}
              >
                View
              </button>
              {showDisplayMenu && (
                <div style={dropdownMenuStyle}>
                  {viewOptions.map(({ id, label, helper, checked }) => (
                    <button
                      key={id}
                      type="button"
                      onClick={() => {
                        if (id === "showClusters") {
                          dispatch(setShowClusters(!checked));
                        } else if (id === "focusMode") {
                          dispatch(setFocusMode(!checked));
                        }
                        setShowDisplayMenu(false);
                      }}
                      title={helper}
                      style={dropdownItemStyle(false)}
                      onMouseEnter={(e) => handleItemHover(e, false)}
                      onMouseLeave={(e) => handleItemLeave(e, false)}
                    >
                      {checked ? `☑️ ${label}` : `⬜ ${label}`}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Mode buttons */}
            {modeOptions.map(({ id, label, helper }) => {
              const isActive = id === mode;
              return (
                <button
                  key={id}
                  type="button"
                  onClick={() => handleModeChange(id)}
                  title={helper}
                  style={dropdownButtonStyle(isActive)}
                >
                  {label}
                </button>
              );
            })}
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
