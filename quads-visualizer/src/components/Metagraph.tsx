import { type CSSProperties, type FC } from "react";
import {
  type Response,
} from "../utils/responseSerializer.ts";
import {
  ControlsContainer,
  FullScreenControl,
  SigmaContainer,
} from "@react-sigma/core";
import "@react-sigma/core/lib/style.css";
import "@react-sigma/graph-search/lib/style.css";
import { useBuildMetagraph } from "../utils/metagraphBuilder.ts";
import { MetagraphBuilder } from "./MetagraphBuilder.tsx";
import { MetagraphHighlight } from "./MetagraphHighlight.tsx";
import { MetagraphClusters } from "./MetagraphClusters.tsx";
import { EdgeCurvedArrowProgram } from "@sigma/edge-curve";

export const Metagraph: FC<{
  response: Response;
  style?: CSSProperties;
}> = ({ response, style }) => {
  const metagraph = useBuildMetagraph(response);
  
  return (
    <SigmaContainer
      settings={{
        allowInvalidContainer: true,
        defaultEdgeType: "arrow",
        renderEdgeLabels: true,
        autoRescale: true,
        autoCenter: true,
        edgeProgramClasses: {
          curvedArrow: EdgeCurvedArrowProgram
        },
      }}
      style={style}
      graph={metagraph}
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
    </SigmaContainer>
  );
};
