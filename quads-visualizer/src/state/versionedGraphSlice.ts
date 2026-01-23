import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

export type VersionedGraphState = {
  selectedGraph: string;
  selectedVersion: string;
  selectedNodes: string[];
  focusNodes: string[];
  selectedEdges: string[];
  focusEdges: string[];
  mergedGraphsEnabled: boolean;
  gridColumns: number;
  gridGap: number;
};

const initialState: VersionedGraphState = {
  selectedGraph: "",
  selectedVersion: "",
  selectedNodes: [],
  focusNodes: [],
  selectedEdges: [],
  focusEdges: [],
  mergedGraphsEnabled: false,
  gridColumns: 3,
  gridGap: 200,
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
    setSelectedNodes: (state, action: PayloadAction<string[]>) => {
      state.selectedNodes = action.payload;
    },
    setFocusNodes: (state, action: PayloadAction<string[]>) => {
      state.focusNodes = action.payload;
    },
    setSelectedEdges: (state, action: PayloadAction<string[]>) => {
      state.selectedEdges = action.payload;
    },
    setFocusEdges: (state, action: PayloadAction<string[]>) => {
      state.focusEdges = action.payload;
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
  setSelectedNodes,
  setFocusNodes,
  setSelectedEdges,
  setFocusEdges,
  setMergedGraphsEnabled,
  setGridColumns,
  setGridGap,
  resetVersionedGraphState,
} = versionedGraphSlice.actions;

export default versionedGraphSlice.reducer;
