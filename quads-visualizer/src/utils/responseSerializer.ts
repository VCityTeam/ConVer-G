import type { SerializedGraph, SerializedNode } from "graphology-types";

export type RDFTerm = {
  value: string;
  type: "uri" | "literal";
};

export type RDFBinding = {
  subject: RDFTerm;
  predicate: RDFTerm;
  object: RDFTerm;
  [key: string]: RDFTerm;
};

export type Response = {
  head: {
    vars: string[];
  };
  results: {
    bindings: RDFBinding[] | undefined;
  };
};


const getNode = (
  binding: RDFBinding,
  type: "subject" | "object",
  previousNode: SerializedNode | undefined = undefined,
): SerializedNode => {
  const keyAttribute = binding[type].value;

  return {
    key: keyAttribute,
    attributes: {
      key: keyAttribute,
      label: keyAttribute,
      termType: binding[type].type,
      nodeType: binding[type + "type"]?.value,
      graphs: new Set([
        ...(previousNode?.attributes?.graphs ?? []),
        binding.graph?.value,
      ]).values(),
      versionedGraphs: new Set([
        ...(previousNode?.attributes?.versionedGraphs ?? []),
        binding.versionedgraph?.value,
      ]).values(),
      versions: new Set([
        ...(previousNode?.attributes?.versions ?? []),
        binding.version?.value,
      ]).values(),
      x: Math.random(),
      color: "#6c3e81",
      y: Math.random(),
      size: 4,
    },
  };
};

export const emptyResponse: SerializedGraph = {
  edges: [],
  nodes: [],
  options: {
    type: "directed",
    multi: true,
    allowSelfLoops: true,
  },
  attributes: {},
};

export const responseSerializer = (response: Response): SerializedGraph => {
  const edges = new Map();
  const nodes = new Map();
  const edgeCounts = new Map<string, number>();

  if (response.results.bindings === undefined) {
    return {
      edges: [],
      nodes: [],
      options: {
        type: "directed",
        multi: true,
        allowSelfLoops: true,
      },
      attributes: {},
    };
  }

  response.results.bindings.forEach((binding) => {
    nodes.set(
      binding.subject.value,
      getNode(binding, "subject", nodes.get(binding.subject.value)),
    );
    nodes.set(
      binding.object.value,
      getNode(binding, "object", nodes.get(binding.object.value)),
    );

    const edgeKey = [
      binding.subject.value,
      binding.predicate.value,
      binding.object.value,
    ].join("-");

    const pairKey = `${binding.subject.value}|${binding.object.value}`;
    const count = edgeCounts.get(pairKey) || 0;
    edgeCounts.set(pairKey, count + 1);

    edges.set(edgeKey, {
      key: edgeKey,
      source: binding.subject.value,
      target: binding.object.value,
      attributes: {
        label: binding.predicate.value,
        size: 2,
        type: count > 0 ? "curvedArrow" : "arrow",
      },
    });
  });

  return {
    edges: Array.from(edges.values()),
    nodes: Array.from(nodes.values()),
    options: {
      type: "directed",
      multi: true,
      allowSelfLoops: true,
    },
    attributes: {},
  };
};
