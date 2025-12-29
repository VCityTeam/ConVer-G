import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

export type VersionedGraphState = {
  selectedGraph: string;
  selectedVersion: string;
  selectedNodes: string[];
  focusNodes: string[];
  mergedGraphsEnabled: boolean;
};

const initialState: VersionedGraphState = {
  selectedGraph: "",
  selectedVersion: "",
  selectedNodes: [],
  focusNodes: [],
  mergedGraphsEnabled: false,
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
    setMergedGraphsEnabled: (state, action: PayloadAction<boolean>) => {
      state.mergedGraphsEnabled = action.payload;
    },
    resetVersionedGraphState: () => initialState,
  },
});

export const {
  setSelectedGraph,
  setSelectedVersion,
  setSelectedNodes,
  setFocusNodes,
  setMergedGraphsEnabled,
  resetVersionedGraphState,
} = versionedGraphSlice.actions;

export default versionedGraphSlice.reducer;
