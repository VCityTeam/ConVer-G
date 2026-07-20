import { configureStore } from "@reduxjs/toolkit";
import metagraphReducer from "./metagraphSlice";
import versionedGraphReducer from "./versionedGraphSlice";
import inferenceReducer from "./inferenceSlice";

export const store = configureStore({
  reducer: {
    metagraph: metagraphReducer,
    versionedGraph: versionedGraphReducer,
    inference: inferenceReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
