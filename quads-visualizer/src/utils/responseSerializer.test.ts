import { describe, expect, it } from "vitest";
import {
  emptyResponse,
  responseSerializer,
  type RDFBinding,
  type Response,
} from "./responseSerializer";

const uri = (value: string) => ({ value, type: "uri" as const });

const triple = (s: string, p: string, o: string): RDFBinding => ({
  subject: uri(s),
  predicate: uri(p),
  object: uri(o),
});

const responseOf = (bindings: RDFBinding[] | undefined): Response => ({
  head: { vars: ["subject", "predicate", "object"] },
  results: { bindings },
});

describe("responseSerializer", () => {
  it("creates one node per distinct resource and one edge per triple", () => {
    const graph = responseSerializer(
      responseOf([
        triple("s1", "p1", "o1"),
        triple("s1", "p2", "o2"),
      ]),
    );

    // s1, o1, o2 -> three distinct nodes; two triples -> two edges.
    expect(graph.nodes).toHaveLength(3);
    expect(graph.edges).toHaveLength(2);
    expect(graph.nodes.map((n) => n.key).sort()).toEqual(["o1", "o2", "s1"]);
  });

  it("uses a curved edge for the second edge between the same node pair", () => {
    const graph = responseSerializer(
      responseOf([
        triple("s1", "p1", "o1"),
        triple("s1", "p2", "o1"),
      ]),
    );

    const types = graph.edges.map((e) => e.attributes?.type);
    expect(types).toContain("arrow");
    expect(types).toContain("curvedArrow");
  });

  it("returns an empty graph when bindings are undefined", () => {
    const graph = responseSerializer(responseOf(undefined));

    expect(graph.nodes).toEqual([]);
    expect(graph.edges).toEqual([]);
  });

  it("exposes a reusable empty response", () => {
    expect(emptyResponse.nodes).toEqual([]);
    expect(emptyResponse.edges).toEqual([]);
    expect(emptyResponse.options.type).toBe("directed");
  });
});
