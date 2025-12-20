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
  updatedAt: number;
};

export type CurrentView = {
  graph: string;
  version: string;
};

export type MetagraphState = {
  externalSelection: VersionedGraphExternalSelection | null;
  travelHoverSelection: TravelHoverSelection | null;
  currentView: CurrentView | null;
};

const initialState: MetagraphState = {
  externalSelection: null,
  travelHoverSelection: null,
  currentView: null,
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
  },
});

export const { setExternalSelection, setTravelHoverSelection, setCurrentView } = metagraphSlice.actions;
export default metagraphSlice.reducer;
