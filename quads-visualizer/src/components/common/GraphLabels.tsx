import { type FC, useEffect, useState } from "react";
import { useSigma } from "@react-sigma/core";
import type { AbstractGraph } from "graphology-types";

export interface GraphLabelsProps {
  separateGraphs: Array<{ graph: string; version: string; data: AbstractGraph }>;
  columns?: number;
  xOffset?: number;
  yOffset?: number;
}

export const GraphLabels: FC<GraphLabelsProps> = ({
  separateGraphs,
  columns = 1,
  xOffset = 300,
  yOffset = 75
}) => {
  const sigma = useSigma();
  const [labels, setLabels] = useState<Array<{ id: string; label: string; x: number; y: number }>>([]);

  useEffect(() => {
    const updateLabels = () => {
      const allSameGraph = separateGraphs.every((g) => g.graph === separateGraphs[0]?.graph);
      const allSameVersion = separateGraphs.every((v) => v.version === separateGraphs[0]?.version);

      const newLabels = separateGraphs.map(({ graph, version, data }, index) => {
        // Calculate grid position
        const col = index % columns;
        const row = Math.floor(index / columns);

        // We need to find the bounding box or center of this specific graph's nodes
        let minX = Infinity, maxX = -Infinity;
        let minY = Infinity, maxY = -Infinity;
        let count = 0;

        data.forEachNode((_, attr) => {
          const x = (attr.x || 0) + col * xOffset;
          const y = (attr.y || 0) + row * yOffset;
          if (x < minX) minX = x;
          if (x > maxX) maxX = x;
          if (y < minY) minY = y;
          if (y > maxY) maxY = y;
          count++;
        });

        if (count === 0) return null;

        const centerX = (minX + maxX) / 2;
        const centerY = minY - 20;

        const viewportPos = sigma.graphToViewport({ x: centerX, y: centerY });

        let label = graph;
        if (allSameGraph) {
          label = version;
        } else if (allSameVersion) {
          label = graph;
        }

        return {
          id: `${graph}-${version}-${index}`,
          label,
          x: viewportPos.x,
          y: viewportPos.y,
        };
      }).filter((l): l is { id: string; label: string; x: number; y: number } => l !== null);

      setLabels(newLabels);
    };

    updateLabels();
    sigma.on("afterRender", updateLabels);

    return () => {
      sigma.off("afterRender", updateLabels);
    };
  }, [sigma, separateGraphs, columns, xOffset, yOffset]);

  return (
    <div
      style={{
        position: "absolute",
        top: 0,
        left: 0,
        width: "100%",
        height: "100%",
        pointerEvents: "none",
        overflow: "hidden",
      }}
    >
      {labels.map((label) => (
        <div
          key={label.id}
          style={{
            position: "absolute",
            top: label.y,
            left: label.x,
            transform: "translate(-50%, -100%)",
            backgroundColor: "rgba(255, 255, 255, 0.8)",
            padding: "2px 6px",
            borderRadius: "4px",
            fontSize: "12px",
            fontWeight: "bold",
            color: "#333",
            border: "1px solid #ccc",
            whiteSpace: "nowrap",
          }}
        >
          {label.label}
        </div>
      ))}
    </div>
  );
};
