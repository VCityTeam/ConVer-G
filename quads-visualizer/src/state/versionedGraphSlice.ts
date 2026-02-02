import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import { GRID_LAYOUT } from "../utils/constants";

export type HighlightSource = "search" | "sparql" | null;

export type VersionedGraphState = {
  selectedGraph: string;
  selectedVersion: string;
  highlightedNodes: string[];
  highlightedEdges: string[];
  highlightSource: HighlightSource;
  mergedGraphsEnabled: boolean;
  gridColumns: number;
  gridGap: number;
};

const initialState: VersionedGraphState = {
  selectedGraph: "",
  selectedVersion: "",
  highlightedNodes: [],
  highlightedEdges: [],
  highlightSource: null,
  mergedGraphsEnabled: false,
  gridColumns: GRID_LAYOUT.DEFAULT_COLUMNS,
  gridGap: GRID_LAYOUT.DEFAULT_GAP,
};

const versionedGraphSlice = createSlice({
  name: "versionedGraph",
  initialState,
  reducers: {
    setSelectedGraph: (state, action: PayloadAction<string>) => {
      state.selectedGraph = action.payload;
    },
    setSelectedVersion: (state, action: PayloadAction<string>) => {
      state.selectedVersion = action.payload;
    },
    setHighlightedNodes: (state, action: PayloadAction<{ nodes: string[]; source: HighlightSource }>) => {
      state.highlightedNodes = action.payload.nodes;
      state.highlightSource = action.payload.source;
    },
    setHighlightedEdges: (state, action: PayloadAction<{ edges: string[]; source: HighlightSource }>) => {
      state.highlightedEdges = action.payload.edges;
      state.highlightSource = action.payload.source;
    },
    clearHighlights: (state) => {
      state.highlightedNodes = [];
      state.highlightedEdges = [];
      state.highlightSource = null;
    },
    setMergedGraphsEnabled: (state, action: PayloadAction<boolean>) => {
      state.mergedGraphsEnabled = action.payload;
    },
    setGridColumns: (state, action: PayloadAction<number>) => {
      state.gridColumns = action.payload;
    },
    setGridGap: (state, action: PayloadAction<number>) => {
      state.gridGap = action.payload;
    },
    resetVersionedGraphState: () => initialState,
  },
});

export const {
  setSelectedGraph,
  setSelectedVersion,
  setHighlightedNodes,
  setHighlightedEdges,
  clearHighlights,
  setMergedGraphsEnabled,
  setGridColumns,
  setGridGap,
  resetVersionedGraphState,
} = versionedGraphSlice.actions;

export default versionedGraphSlice.reducer;
