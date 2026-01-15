import { useSigma } from "@react-sigma/core";
import { useCallback, useEffect, useRef, useState } from "react";
import distinctColors from "distinct-colors";
import { METAGRAPH_RELATION_SUFFIXES, VERSIONED_NODE_PREFIX } from "../utils/metagraphBuilder.ts";

type ClusterMode = "none" | "specialization" | "location";

type ClusterStats = Record<string, { count: number; color: string }>;

type Point = { x: number; y: number };

const generateClusterColors = (count: number): string[] => {
  if (count <= 0) return [];
  const palette = distinctColors({
    count,
    chromaMin: 40,
    chromaMax: 90,
    lightMin: 35,
    lightMax: 70,
  });
  return palette.map((color) => color.hex());
};

const UNASSIGNED_KEY = "Unassigned";

export const MetagraphClusters = () => {
  const sigma = useSigma();
  const graph = sigma.getGraph();
  const [mode, setMode] = useState<ClusterMode>("none");
  const [stats, setStats] = useState<ClusterStats>({});
  const originalPositionsRef = useRef(new Map<string, Point>());
  const originalColorsRef = useRef(new Map<string, string>());
  const previousGraphRef = useRef(graph);

  const getVersionedNodes = useCallback(
    (): string[] =>
      graph
        .nodes()
        .filter((node) => {
          const isHidden = graph.getNodeAttribute(node, "hidden") === true;
          const label = graph.getNodeAttribute(node, "label") as string | undefined;
          return !isHidden && typeof label === "string" && label.startsWith(VERSIONED_NODE_PREFIX);
        }),
    [graph],
  );

  const rememberBaselines = useCallback(
    (nodes: string[]) => {
      nodes.forEach((node) => {
        if (!originalPositionsRef.current.has(node)) {
          originalPositionsRef.current.set(node, {
            x: graph.getNodeAttribute(node, "x"),
            y: graph.getNodeAttribute(node, "y"),
          });
        }
        if (!originalColorsRef.current.has(node)) {
          originalColorsRef.current.set(node, graph.getNodeAttribute(node, "color"));
        }
      });
    },
    [graph],
  );

  const restoreBaseline = useCallback(() => {
    getVersionedNodes().forEach((node) => {
      const baseline = originalPositionsRef.current.get(node);
      const baseColor = originalColorsRef.current.get(node);
      if (baseline) {
        graph.setNodeAttribute(node, "x", baseline.x);
        graph.setNodeAttribute(node, "y", baseline.y);
      }
      if (baseColor) {
        graph.setNodeAttribute(node, "color", baseColor);
      }
    });
    setStats({});
  }, [getVersionedNodes, graph]);

  const getClusterKey = useCallback(
    (node: string, selectedMode: ClusterMode): string => {
      if (selectedMode === "none") return UNASSIGNED_KEY;

      let found: string | null = null;
      graph.forEachOutboundEdge(node, (_edgeKey, attributes, _source, target) => {
        const label = typeof attributes?.label === "string" ? attributes.label : "";

        if (!found && selectedMode === "specialization" && label.endsWith(METAGRAPH_RELATION_SUFFIXES.specialization)) {
          const candidate = graph.getNodeAttribute(target, "label") as string | undefined;
          found = candidate ?? target;
        }

        if (!found && selectedMode === "location" && label.endsWith(METAGRAPH_RELATION_SUFFIXES.location)) {
          const candidate = graph.getNodeAttribute(target, "label") as string | undefined;
          found = candidate ?? target;
        }
      });

      return found ?? UNASSIGNED_KEY;
    },
    [graph],
  );

  const computeClusterCenters = useCallback(
    (groups: string[], bounds: { center: Point; radius: number }): Record<string, Point> => {
      if (groups.length === 0) return {};
      const centers: Record<string, Point> = {};

      const radius = Math.max(bounds.radius, 1);
      const angleStep = (2 * Math.PI) / Math.max(groups.length, 1);

      groups.forEach((group, index) => {
        const angle = angleStep * index;
        centers[group] = {
          x: bounds.center.x + Math.cos(angle) * radius,
          y: bounds.center.y + Math.sin(angle) * radius,
        };
      });

      return centers;
    },
    [],
  );

  const computeBounds = useCallback((clusterCount: number = 1): { center: Point; radius: number } => {
    let minX = Number.POSITIVE_INFINITY;
    let maxX = Number.NEGATIVE_INFINITY;
    let minY = Number.POSITIVE_INFINITY;
    let maxY = Number.NEGATIVE_INFINITY;

    getVersionedNodes().forEach((node) => {
      const baseline = originalPositionsRef.current.get(node);
      const x = baseline?.x ?? graph.getNodeAttribute(node, "x") ?? 0;
      const y = baseline?.y ?? graph.getNodeAttribute(node, "y") ?? 0;
      minX = Math.min(minX, x);
      maxX = Math.max(maxX, x);
      minY = Math.min(minY, y);
      maxY = Math.max(maxY, y);
    });

    if (!Number.isFinite(minX) || !Number.isFinite(minY)) {
      return { center: { x: 0, y: 0 }, radius: 200 };
    }

    const width = Math.max(maxX - minX, 1);
    const height = Math.max(maxY - minY, 1);
    // Scale radius based on cluster count: more clusters need larger radius to avoid overlap
    const baseRadius = Math.max(width, height) * 0.65 + 40;
    const clusterScaleFactor = clusterCount > 4 ? 1 + (clusterCount - 4) * 0.15 : 1;
    const radius = baseRadius * clusterScaleFactor;
    return {
      center: { x: (minX + maxX) / 2, y: (minY + maxY) / 2 },
      radius,
    };
  }, [getVersionedNodes, graph]);

  const repositionNodes = useCallback(
    (clusters: Map<string, string[]>, centers: Record<string, Point>, colors: string[]) => {
      const clusterEntries = Array.from(clusters.entries());

      clusterEntries.forEach(([clusterKey, nodes], index) => {
        const clusterColor = colors[index % colors.length];
        const center = centers[clusterKey] ?? { x: 0, y: 0 };
        const ring = Math.max(nodes.length, 1);

        nodes.forEach((node, nodeIndex) => {
          const angle = (2 * Math.PI * nodeIndex) / ring;
          const distance = 14 + (nodeIndex % 5) * 4;
          graph.setNodeAttribute(node, "x", center.x + Math.cos(angle) * distance);
          graph.setNodeAttribute(node, "y", center.y + Math.sin(angle) * distance);
          graph.setNodeAttribute(node, "color", clusterColor);
        });
      });
    },
    [graph],
  );

  const applyClustering = useCallback(
    (selectedMode: ClusterMode) => {
      const versionedNodes = getVersionedNodes();

      rememberBaselines(versionedNodes);

      if (selectedMode === "none" || versionedNodes.length === 0) {
        restoreBaseline();
        return;
      }

      const clusters = new Map<string, string[]>();

      versionedNodes.forEach((node) => {
        const clusterKey = getClusterKey(node, selectedMode);
        if (!clusters.has(clusterKey)) {
          clusters.set(clusterKey, []);
        }
        clusters.get(clusterKey)!.push(node);
      });

      const clusterCenters = computeClusterCenters(
        Array.from(clusters.keys()),
        computeBounds(clusters.size),
      );

      const clusterColors = generateClusterColors(clusters.size);

      repositionNodes(clusters, clusterCenters, clusterColors);

      setStats(
        Object.fromEntries(
          Array.from(clusters.entries()).map(([key, value], index) => [
            key,
            {
              count: value.length,
              color: clusterColors[index % clusterColors.length],
            },
          ]),
        ),
      );
    },
    [computeBounds, computeClusterCenters, getClusterKey, getVersionedNodes, rememberBaselines, repositionNodes, restoreBaseline],
  );

  useEffect(() => {
    if (previousGraphRef.current !== graph) {
      previousGraphRef.current = graph;
      originalColorsRef.current.clear();
      originalPositionsRef.current.clear();
      setMode("none");
      setStats({});
    }
  }, [graph]);

  useEffect(() => {
    applyClustering(mode);
  }, [applyClustering, mode]);

  return (
    <div
      style={{
        padding: "10px",
        borderRadius: "10px",
        maxWidth: "240px",
        pointerEvents: "auto",
        display: "flex",
        flexDirection: "column",
        gap: "8px",
      }}
    >
      <div style={{ fontWeight: 700, fontSize: "14px", color: "#0f172a" }}>
        Cluster metadata using <a href="https://www.w3.org/TR/prov-o/" target="_blank"
          rel="noopener noreferrer">PROV-O</a>
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "6px" }}>
        <button
          type="button"
          onClick={() => setMode((current) => (current === "specialization" ? "none" : "specialization"))}
          style={{
            padding: "6px 8px",
            borderRadius: "6px",
            border: mode === "specialization" ? "2px solid #2563eb" : "1px solid #cbd5e1",
            background: mode === "specialization" ? "#e0edff" : "#f8fafc",
            fontWeight: 600,
            cursor: "pointer",
          }}
        >
          Specialization
        </button>
        <button
          type="button"
          onClick={() => setMode((current) => (current === "location" ? "none" : "location"))}
          style={{
            padding: "6px 8px",
            borderRadius: "6px",
            border: mode === "location" ? "2px solid #2563eb" : "1px solid #cbd5e1",
            background: mode === "location" ? "#e0edff" : "#f8fafc",
            fontWeight: 600,
            cursor: "pointer",
          }}
        >
          Location
        </button>
      </div>
      <div style={{ fontSize: "12px", color: "#475569", lineHeight: 1.4 }}>
        {mode === "none" && (
          <>Toggle a mode to reposition metadata nodes into clusters. Click again to restore the base layout.</>
        )}
        {mode === "specialization" && (
          <>
            Groups entities that are specializations of a general entity. See{" "}
            <a href="https://www.w3.org/TR/prov-o/#specializationOf" target="_blank" rel="noopener noreferrer">
              specializationOf
            </a>
            .
          </>
        )}
        {mode === "location" && (
          <>
            Groups entities based on their location. See{" "}
            <a href="https://www.w3.org/TR/prov-o/#atLocation" target="_blank" rel="noopener noreferrer">
              atLocation
            </a>
            .
          </>
        )}
      </div>
      {
        Object.keys(stats).length > 0 && (
          <div
            style={{
              background: "#f8fafc",
              border: "1px solid #e2e8f0",
              borderRadius: "8px",
              padding: "8px",
              maxHeight: "100px",
              overflowY: "auto",
              display: "flex",
              flexDirection: "column",
              gap: "6px",
            }}
          >
            {(
              Object.entries(stats)
                .sort(([, aValue], [, bValue]) => bValue.count - aValue.count)
                .map(([key, { count, color }]) => (
                  <div
                    key={key}
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      padding: "6px 8px",
                      background: "white",
                      borderRadius: "6px",
                      border: "1px solid #e2e8f0",
                      fontSize: "12px",
                      color: "#0f172a",
                    }}
                  >
                    <div style={{ display: "flex", alignItems: "center", gap: "6px", overflow: "hidden" }}>
                      <div
                        style={{
                          width: "8px",
                          height: "8px",
                          borderRadius: "50%",
                          backgroundColor: color,
                          flexShrink: 0,
                        }}
                      />
                      <span
                        title={key}
                        style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}
                      >
                        {key}
                      </span>
                    </div>
                    <span style={{ fontWeight: 700, marginLeft: "8px" }}>{count}</span>
                  </div>
                ))
            )}
          </div>
        )
      }
    </div>
  );
};
