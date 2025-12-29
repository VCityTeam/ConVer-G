import { type CSSProperties, type FC, useCallback } from "react";
import { type Response } from "../utils/responseSerializer.ts";
import {
  ControlsContainer,
  FullScreenControl,
} from "@react-sigma/core";
import "@react-sigma/graph-search/lib/style.css";
import { METAGRAPH_RELATION_COLORS, useBuildMetagraph } from "../utils/metagraphBuilder.ts";
import { MetagraphBuilder } from "./MetagraphBuilder.tsx";
import { MetagraphHighlight } from "./MetagraphHighlight.tsx";
import { MetagraphClusters } from "./MetagraphClusters.tsx";
import type { Attributes } from "graphology-types";
import { SigmaGraph } from "./common/SigmaGraph.tsx";

export const Metagraph: FC<{
  response: Response;
  style?: CSSProperties;
}> = ({ response, style }) => {
  const metagraph = useBuildMetagraph(response);

  const edgeReducer = useCallback((_edge: string, data: Attributes) => {
    const res = { ...data };
    res.color = METAGRAPH_RELATION_COLORS.edge;
    res.labelColor = METAGRAPH_RELATION_COLORS.label;
    return res;
  }, []);

  return (
    <SigmaGraph
      graph={metagraph}
      edgeReducer={edgeReducer}
      style={style}
    >
      <MetagraphHighlight />
      <ControlsContainer position={"bottom-left"}>
        <FullScreenControl />
      </ControlsContainer>
      <ControlsContainer position={"top-left"}>
        <MetagraphBuilder />
      </ControlsContainer>
      <ControlsContainer position={"top-right"}>
        <MetagraphClusters />
      </ControlsContainer>
    </SigmaGraph>
  );
};
