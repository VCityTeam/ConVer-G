import { type FC, useCallback, useMemo, useState } from "react";
import { NewNodeModal } from "./NewNodeModal";
import { RelationInput } from "./RelationInput";
import { useAppDispatch, useAppSelector } from "../state/hooks";
import { setFocusMode, setShowClusters } from "../state/metagraphSlice";
import { useMetagraphEvents, type BuilderMode } from "../hooks/useMetagraphEvents";
import { useMetagraphEditing } from "../hooks/useMetagraphEditing";

export const MetagraphBuilder: FC = () => {
  const dispatch = useAppDispatch();

  // Local UI state
  const [sourceNode, setSourceNode] = useState<string | null>(null);
  const [targetNode, setTargetNode] = useState<string | null>(null);
  const [relationValue, setRelationValue] = useState<string>("prov:wasDerivedFrom");
  const [pendingNodeCoord, setPendingNodeCoord] = useState<{ x: number; y: number } | null>(null);
  const [nodeNameError, setNodeNameError] = useState<string | null>(null);
  const [mode, setMode] = useState<BuilderMode>();
  const [dragSourceNode, setDragSourceNode] = useState<string | null>(null);
  const [showSaveMenu, setShowSaveMenu] = useState(false);
  const [showEditMenu, setShowEditMenu] = useState(false);
  const [showDisplayMenu, setShowDisplayMenu] = useState(false);

  const showClusters = useAppSelector((state) => state.metagraph.showClusters);
  const focusMode = useAppSelector((state) => state.metagraph.focusMode);

  // Hooks
  const callbacks = useMemo(() => ({
    onSetSourceNode: setSourceNode,
    onSetTargetNode: setTargetNode,
    onSetDragSourceNode: setDragSourceNode,
    onSetPendingNodeCoord: setPendingNodeCoord,
    onSetNodeNameError: setNodeNameError,
  }), []);

  const { dragCompletionRef, clearTravelState } = useMetagraphEvents(
    mode, dragSourceNode, sourceNode, targetNode, callbacks
  );
  const { downloadGraphAsTTL, uploadMetagraph, handleCreateNode, handleAddEdge } = useMetagraphEditing();

  // Mode management
  const handleModeChange = async (nextMode: BuilderMode) => {
    if (nextMode === "download") {
      downloadGraphAsTTL();
      return;
    }
    if (nextMode === "save") {
      await uploadMetagraph();
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
      clearTravelState();
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
      clearTravelState();
    }
  };

  const onCreateNode = useCallback((name: string) => {
    if (!pendingNodeCoord) return;
    const error = handleCreateNode(name, pendingNodeCoord);
    if (error) {
      setNodeNameError(error);
    } else {
      setPendingNodeCoord(null);
      setNodeNameError(null);
    }
  }, [pendingNodeCoord, handleCreateNode]);

  const onAddEdge = useCallback(() => {
    if (mode !== "createLink" || !sourceNode || !targetNode) return;
    handleAddEdge(sourceNode, targetNode, relationValue);
    setSourceNode(null);
    setTargetNode(null);
    setDragSourceNode(null);
    dragCompletionRef.current = false;
  }, [mode, sourceNode, targetNode, relationValue, handleAddEdge, dragCompletionRef]);

  const onClear = useCallback(() => {
    setSourceNode(null);
    setTargetNode(null);
    setDragSourceNode(null);
    dragCompletionRef.current = false;
  }, [dragCompletionRef]);

  // Menu data
  const saveOptions = useMemo(() => [
    { id: "download" as const, label: "💾 Download", helper: "Download the graph as TTL" },
    { id: "save" as const, label: "📤 Save Online", helper: "Upload the metagraph to ConVer-G" },
  ], []);

  const editOptions = useMemo(() => [
    { id: "createLink" as const, label: "🪢 New Link", helper: "Drag-and-drop nodes to link" },
    { id: "createNode" as const, label: "⭕ New Node", helper: "Click canvas to create a new node" },
  ], []);

  const modeOptions = useMemo(() => [
    { id: "travel" as const, label: "🏃‍♀️‍➡️", helper: "Change versioned graph" },
  ], []);

  const viewOptions = useMemo(() => [
    { id: "showClusters" as const, label: "Show Clusters", helper: "Toggle cluster panel visibility", checked: showClusters },
    { id: "focusMode" as const, label: "Focus Mode", helper: "Show only cluster-related edges and nodes", checked: focusMode },
  ], [showClusters, focusMode]);

  // Styles
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
    if (!isActive) e.currentTarget.style.backgroundColor = "#f3f4f6";
  };

  const handleItemLeave = (e: React.MouseEvent<HTMLButtonElement>, isActive: boolean = false) => {
    if (!isActive) e.currentTarget.style.backgroundColor = "transparent";
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
              <button type="button" title="File options" style={dropdownButtonStyle(false)}>File</button>
              {showSaveMenu && (
                <div style={dropdownMenuStyle}>
                  {saveOptions.map(({ id, label, helper }) => (
                    <button
                      key={id}
                      type="button"
                      onClick={() => { handleModeChange(id); setShowSaveMenu(false); }}
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
              <button type="button" title="Edit options" style={dropdownButtonStyle(mode === "createLink" || mode === "createNode")}>Edit</button>
              {showEditMenu && (
                <div style={dropdownMenuStyle}>
                  {editOptions.map(({ id, label, helper }) => {
                    const isActive = id === mode;
                    return (
                      <button
                        key={id}
                        type="button"
                        onClick={() => { handleModeChange(id); setShowEditMenu(false); }}
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
              <button type="button" title="View options" style={dropdownButtonStyle(false)}>View</button>
              {showDisplayMenu && (
                <div style={dropdownMenuStyle}>
                  {viewOptions.map(({ id, label, helper, checked }) => (
                    <button
                      key={id}
                      type="button"
                      onClick={() => {
                        if (id === "showClusters") dispatch(setShowClusters(!checked));
                        else if (id === "focusMode") dispatch(setFocusMode(!checked));
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
        {isCreateLinkMode && (
          <>
            <RelationInput value={relationValue} onChange={setRelationValue} disabled={!isCreateLinkMode} />
            <div style={{ display: "flex", gap: "5px" }}>
              <button
                onClick={onAddEdge}
                disabled={!canAddLink}
                style={{ flex: 1, padding: "5px", cursor: canAddLink ? "pointer" : "not-allowed" }}
              >
                Add Link
              </button>
              <button onClick={onClear} style={{ padding: "5px", cursor: "pointer" }}>
                Clear
              </button>
            </div>
          </>
        )}
      </div>
      <NewNodeModal
        isOpen={Boolean(pendingNodeCoord)}
        errorMessage={nodeNameError}
        onSubmit={onCreateNode}
        onCancel={() => { setPendingNodeCoord(null); setNodeNameError(null); }}
      />
    </>
  );
};
