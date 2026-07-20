import { describe, it, expect } from "vitest";
import reducer, {
  inferQueryParam,
  setInferenceMode,
  type InferenceMode,
} from "./inferenceSlice";

describe("inferenceSlice", () => {
  it("defaults to the server default", () => {
    const state = reducer(undefined, { type: "@@INIT" });
    expect(state.mode).toBe("default");
  });

  it("updates the mode via setInferenceMode", () => {
    const state = reducer({ mode: "default" }, setInferenceMode("rdfs+swrl"));
    expect(state.mode).toBe("rdfs+swrl");
  });
});

describe("inferQueryParam", () => {
  it("omits the parameter for the server default", () => {
    expect(inferQueryParam("default")).toBeUndefined();
  });

  it("maps every other mode to its ?infer= token verbatim", () => {
    const cases: Array<[InferenceMode, string]> = [
      ["off", "off"],
      ["rdfs", "rdfs"],
      ["owl_lite", "owl_lite"],
      ["swrl", "swrl"],
      ["rdfs+swrl", "rdfs+swrl"],
      ["all", "all"],
    ];
    for (const [mode, token] of cases) {
      expect(inferQueryParam(mode)).toBe(token);
    }
  });
});
