import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

/**
 * Query-time inference mode chosen in the UI. The values mirror the quads-query
 * `?infer=` HTTP parameter tokens, with one extra sentinel: {@link "default"}
 * sends *no* parameter, so the server falls back to its configured
 * `ENTAILMENT_REGIME` / `SWRL_RULES` default.
 */
export type InferenceMode =
  | "default"
  | "off"
  | "rdfs"
  | "owl_lite"
  | "swrl"
  | "rdfs+swrl"
  | "all";

export type InferenceState = {
  mode: InferenceMode;
};

const initialState: InferenceState = {
  mode: "default",
};

const inferenceSlice = createSlice({
  name: "inference",
  initialState,
  reducers: {
    setInferenceMode: (state, action: PayloadAction<InferenceMode>) => {
      state.mode = action.payload;
    },
  },
});

/**
 * The `?infer=` token to send for a given mode, or {@code undefined} when the
 * parameter must be omitted (i.e. the server default is wanted).
 */
export const inferQueryParam = (mode: InferenceMode): string | undefined =>
  mode === "default" ? undefined : mode;

export const { setInferenceMode } = inferenceSlice.actions;
export default inferenceSlice.reducer;
