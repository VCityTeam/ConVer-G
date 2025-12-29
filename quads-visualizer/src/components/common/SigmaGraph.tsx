import { type CSSProperties, type FC, type ReactNode } from "react";
import { SigmaContainer } from "@react-sigma/core";
import { EdgeCurvedArrowProgram } from "@sigma/edge-curve";
import type { AbstractGraph, Attributes } from "graphology-types";
import "@react-sigma/core/lib/style.css";
import type { Settings } from "sigma/settings";

export interface SigmaGraphProps {
  graph: AbstractGraph | null;
  style?: CSSProperties;
  settings?: Settings;
  children?: ReactNode;
  edgeReducer?: (edge: string, data: Attributes) => Attributes;
}

const DEFAULT_SIGMA_SETTINGS = {
  allowInvalidContainer: true,
  defaultEdgeType: "arrow",
  renderEdgeLabels: true,
  edgeLabelColor: { attribute: "labelColor" },
  autoRescale: true,
  autoCenter: true,
  edgeProgramClasses: {
    curvedArrow: EdgeCurvedArrowProgram,
  },
  labelRenderedSizeThreshold: 15,
  labelSize: 11,
  labelGridCellSize: 60,
  labelDensity: 0.5,
};

export const SigmaGraph: FC<SigmaGraphProps> = ({
  graph,
  style,
  settings,
  children,
  edgeReducer,
}) => {
  if (!graph) return null;

  return (
    <SigmaContainer
      settings={{
        ...DEFAULT_SIGMA_SETTINGS,
        ...settings,
        edgeReducer: edgeReducer ?? settings?.edgeReducer,
      }}
      style={style}
      graph={graph}
    >
      {children}
    </SigmaContainer>
  );
};
