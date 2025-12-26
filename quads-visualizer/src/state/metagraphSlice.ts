import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

export type VersionedGraphExternalSelection = {
  origin: "travel";
  graph?: string;
  version?: string;
  updatedAt: number;
};

export type TravelHoverSelection = {
  graph?: string;
  version?: string;
  nodeType: MetagraphNodeType;
  updatedAt: number;
};

export type CurrentView = {
  graph: string;
  version: string;
};

export type MetagraphNodeType = "vng" | "namedGraph" | "version" | null;

export type MetagraphState = {
  externalSelection: VersionedGraphExternalSelection | null;
  travelHoverSelection: TravelHoverSelection | null;
  currentView: CurrentView | null;
  selectedMetagraphNodeType: MetagraphNodeType;
  selectedMetagraphNode: string | null;
};

const initialState: MetagraphState = {
  externalSelection: null,
  travelHoverSelection: null,
  currentView: null,
  selectedMetagraphNodeType: null,
  selectedMetagraphNode: null,
};

const metagraphSlice = createSlice({
  name: "metagraph",
  initialState,
  reducers: {
    setExternalSelection: (state, action: PayloadAction<VersionedGraphExternalSelection | null>) => {
      state.externalSelection = action.payload;
    },
    setTravelHoverSelection: (state, action: PayloadAction<TravelHoverSelection | null>) => {
      state.travelHoverSelection = action.payload;
    },
    setCurrentView: (state, action: PayloadAction<CurrentView | null>) => {
      state.currentView = action.payload;
    },
    setSelectedMetagraphNodeType: (state, action: PayloadAction<MetagraphNodeType>) => {
      state.selectedMetagraphNodeType = action.payload;
    },
    setSelectedMetagraphNode: (state, action: PayloadAction<string | null>) => {
      state.selectedMetagraphNode = action.payload;
    },
  },
});

export const { setExternalSelection, setTravelHoverSelection, setCurrentView, setSelectedMetagraphNodeType, setSelectedMetagraphNode } = metagraphSlice.actions;
export default metagraphSlice.reducer;
