import { describe, it, expect, beforeAll } from "vitest";

// QueryService reads `window.env` at module load; the tests run in the node
// environment where `window` is absent, so provide a minimal stub before import.
type QueryServiceModule = typeof import("./QueryService");
let QueryService: QueryServiceModule["QueryService"];

beforeAll(async () => {
  (globalThis as unknown as { window: { env?: Record<string, string> } }).window = {};
  ({ QueryService } = await import("./QueryService"));
});

describe("QueryService.endpointWithInference", () => {
  it("returns the bare endpoint when no mode is given", () => {
    expect(QueryService.endpointWithInference()).toBe(QueryService.QUERY_ENDPOINT);
    expect(QueryService.endpointWithInference(undefined)).toBe(QueryService.QUERY_ENDPOINT);
  });

  it("appends the ?infer= parameter when a mode is given", () => {
    expect(QueryService.endpointWithInference("rdfs")).toBe(
      `${QueryService.QUERY_ENDPOINT}?infer=rdfs`,
    );
  });

  it("url-encodes combined modes", () => {
    expect(QueryService.endpointWithInference("rdfs+swrl")).toBe(
      `${QueryService.QUERY_ENDPOINT}?infer=rdfs%2Bswrl`,
    );
  });
});
