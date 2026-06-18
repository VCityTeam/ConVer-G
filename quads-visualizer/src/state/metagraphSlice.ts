import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

export type ClusterMode = "none" | "specialization" | "location";

export type ClusterStats = Record<string, { count: number; color: string }>;

export type TravelHoverSelection = {
  graph?: string;
  version?: string;
  nodeType: MetagraphNodeType;
};

export type MetagraphNodeType = "vng" | "namedGraph" | "version";

export type MetagraphView = "matrix" | "graph";

export type MetagraphState = {
  metagraphView: MetagraphView;
  travelHoverSelection: TravelHoverSelection | null;
  selectedMetagraphNodeType: MetagraphNodeType;
  selectedMetagraphNode: string | null;
  showClusters: boolean;
  focusMode: boolean;
  clusterMode: ClusterMode;
  clusterStats: ClusterStats;
};

const initialState: MetagraphState = {
  metagraphView: "matrix",
  travelHoverSelection: null,
  selectedMetagraphNodeType: "vng",
  selectedMetagraphNode: null,
  showClusters: true,
  focusMode: false,
  clusterMode: "location",
  clusterStats: {},
};

const metagraphSlice = createSlice({
  name: "metagraph",
  initialState,
  reducers: {
    setMetagraphView: (state, action: PayloadAction<MetagraphView>) => {
      state.metagraphView = action.payload;
    },
    setTravelHoverSelection: (state, action: PayloadAction<TravelHoverSelection | null>) => {
      state.travelHoverSelection = action.payload;
    },
    setSelectedMetagraphNodeType: (state, action: PayloadAction<MetagraphNodeType>) => {
      state.selectedMetagraphNodeType = action.payload;
    },
    setSelectedMetagraphNode: (state, action: PayloadAction<string | null>) => {
      state.selectedMetagraphNode = action.payload;
    },
    setShowClusters: (state, action: PayloadAction<boolean>) => {
      state.showClusters = action.payload;
    },
    setFocusMode: (state, action: PayloadAction<boolean>) => {
      state.focusMode = action.payload;
    },
    setClusterMode: (state, action: PayloadAction<ClusterMode>) => {
      state.clusterMode = action.payload;
    },
    setClusterStats: (state, action: PayloadAction<ClusterStats>) => {
      state.clusterStats = action.payload;
    },
  },
});

export const { setMetagraphView, setTravelHoverSelection, setSelectedMetagraphNodeType, setSelectedMetagraphNode, setShowClusters, setFocusMode, setClusterMode, setClusterStats } = metagraphSlice.actions;
export default metagraphSlice.reducer;
