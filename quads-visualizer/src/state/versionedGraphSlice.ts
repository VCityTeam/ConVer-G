import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

export type VersionedGraphState = {
  selectedGraph: string;
  selectedVersion: string;
  selectedNode: string | null;
  focusNode: string | null;
  mergedGraphsEnabled: boolean;
};

const initialState: VersionedGraphState = {
  selectedGraph: "",
  selectedVersion: "",
  selectedNode: null,
  focusNode: null,
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
    setSelectedNode: (state, action: PayloadAction<string | null>) => {
      state.selectedNode = action.payload;
    },
    setFocusNode: (state, action: PayloadAction<string | null>) => {
      state.focusNode = action.payload;
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
  setSelectedNode,
  setFocusNode,
  setMergedGraphsEnabled,
  resetVersionedGraphState,
} = versionedGraphSlice.actions;

export default versionedGraphSlice.reducer;
