import { configureStore } from "@reduxjs/toolkit";
import metagraphReducer from "./metagraphSlice";
import versionedGraphReducer from "./versionedGraphSlice";

export const store = configureStore({
  reducer: {
    metagraph: metagraphReducer,
    versionedGraph: versionedGraphReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
