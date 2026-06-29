import { describe, expect, it } from "vitest";
import {
  buildMetagraphMatrix,
  getCellKey,
  METAGRAPH_RELATION_IRIS,
  sourceLabel,
  versionLabel,
} from "./metagraphMatrix";
import type { RDFBinding, Response } from "./responseSerializer";

const uri = (value: string) => ({ value, type: "uri" as const });

const triple = (s: string, p: string, o: string): RDFBinding => ({
  subject: uri(s),
  predicate: uri(p),
  object: uri(o),
});

const responseOf = (bindings: RDFBinding[]): Response => ({
  head: { vars: ["subject", "predicate", "object"] },
  results: { bindings },
});

describe("getCellKey", () => {
  it("builds a key containing both the source and the version", () => {
    const key = getCellKey("ng#A", "v#2015");
    expect(key).toContain("ng#A");
    expect(key).toContain("v#2015");
  });

  it("separates source and version so ambiguous inputs do not collide", () => {
    expect(getCellKey("a", "bc")).not.toBe(getCellKey("ab", "c"));
  });
});

describe("versionLabel", () => {
  it("extracts an embedded date", () => {
    expect(versionLabel("http://example.edu/version#2015-06-29")).toBe(
      "2015-06-29",
    );
  });

  it("strips an RDF file extension when no date is present", () => {
    expect(versionLabel("http://example.edu/version#buildings-2018.trig")).toBe(
      "buildings-2018",
    );
  });
});

describe("sourceLabel", () => {
  it("returns the last fragment of a named-graph IRI", () => {
    expect(sourceLabel("http://example.edu/Named-Graph#Grand-Lyon")).toBe(
      "Grand-Lyon",
    );
  });

  it("falls back to the last path segment when there is no fragment", () => {
    expect(sourceLabel("http://example.edu/Named-Graph/IGN")).toBe("IGN");
  });
});

describe("buildMetagraphMatrix", () => {
  const { specialization, location } = METAGRAPH_RELATION_IRIS;

  it("derives sources, versions and present cells from PROV-O bindings", () => {
    const matrix = buildMetagraphMatrix(
      responseOf([
        triple("vng#A-2015", specialization, "ng#Grand-Lyon"),
        triple("vng#A-2015", location, "loc#buildings-2015"),
        triple("vng#A-2018", specialization, "ng#Grand-Lyon"),
        triple("vng#A-2018", location, "loc#buildings-2018"),
        triple("vng#B-2015", specialization, "ng#IGN"),
        triple("vng#B-2015", location, "loc#buildings-2015"),
      ]),
    );

    expect(matrix.sources).toEqual(["ng#Grand-Lyon", "ng#IGN"]);
    expect(matrix.versions).toEqual(["loc#buildings-2015", "loc#buildings-2018"]);
    expect(matrix.snapshotCount).toBe(3);
    expect(matrix.cells.get(getCellKey("ng#Grand-Lyon", "loc#buildings-2018"))).toBe(
      "vng#A-2018",
    );
    // (IGN, 2018) is not a present snapshot.
    expect(matrix.cells.has(getCellKey("ng#IGN", "loc#buildings-2018"))).toBe(false);
  });

  it("ignores a versioned named graph that lacks a location", () => {
    const matrix = buildMetagraphMatrix(
      responseOf([triple("vng#orphan", specialization, "ng#Grand-Lyon")]),
    );

    expect(matrix.snapshotCount).toBe(0);
    expect(matrix.sources).toEqual(["ng#Grand-Lyon"]);
    expect(matrix.versions).toEqual([]);
  });

  it("returns an empty matrix when there are no bindings", () => {
    const matrix = buildMetagraphMatrix(responseOf([]));

    expect(matrix.sources).toEqual([]);
    expect(matrix.versions).toEqual([]);
    expect(matrix.snapshotCount).toBe(0);
  });
});
