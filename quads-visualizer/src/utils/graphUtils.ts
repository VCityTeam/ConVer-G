import Graph from "graphology";
import FA2, { type ForceAtlas2Settings } from "graphology-layout-forceatlas2";
import { responseSerializer, type Response } from "./responseSerializer";

export interface FA2Settings {
  iterations?: number;
  settings?: ForceAtlas2Settings;
}

export const computeNodesPositions = (response: Response, fa2Settings: FA2Settings = { iterations: 25 }) => {
  const g = Graph.from(
    responseSerializer(response),
  );

  FA2.assign(g, {
    iterations: fa2Settings.iterations ?? 25,
    settings: fa2Settings.settings,
  });

  const nodesPositions = new Map<string, [number, number]>();

  g.forEachNode((node, attributes) => {
    nodesPositions.set(node, [attributes.x, attributes.y]);
  });

  return { positions: nodesPositions };
}
