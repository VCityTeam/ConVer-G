import { useCallback, useEffect, useRef } from "react";
import { useSigma } from "@react-sigma/core";
import { useAppDispatch, useAppSelector } from "../state/hooks";
import { setClusterStats, type ClusterMode, type ClusterStats } from "../state/metagraphSlice";
import { METAGRAPH_RELATION_SUFFIXES, VERSIONED_NODE_PREFIX } from "../utils/metagraphBuilder";
import distinctColors from "distinct-colors";

type Point = { x: number; y: number };

const UNASSIGNED_KEY = "Unassigned";

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

/**
 * Unified coordinator for all metagraph visual effects.
 * Handles focus mode visibility, clustering, and ensures proper mutation order.
 * 
 * Mutation order:
 * 1. Layout (clustering positions)
 * 2. Visibility (focus mode hidden states)
 * 3. Style (colors from clustering)
 */
export const MetagraphEffectsCoordinator = () => {
  const sigma = useSigma();
  const graph = sigma.getGraph();
  const dispatch = useAppDispatch();
  
  // Redux state
  const focusMode = useAppSelector((state) => state.metagraph.focusMode);
  const clusterMode = useAppSelector((state) => state.metagraph.clusterMode);
  
  // Baseline refs for restoration
  const originalPositionsRef = useRef(new Map<string, Point>());
  const originalColorsRef = useRef(new Map<string, string>());
  const previousGraphRef = useRef(graph);
  const isInitialMountRef = useRef(true);

  // ============================================
  // Utility functions
  // ============================================
  
  const getVersionedNodes = useCallback((): string[] =>
    graph.nodes().filter((node) => {
      const isHidden = graph.getNodeAttribute(node, "hidden") === true;
      const label = graph.getNodeAttribute(node, "label") as string | undefined;
      return !isHidden && typeof label === "string" && label.startsWith(VERSIONED_NODE_PREFIX);
    }), [graph]);

  const getClusterKey = useCallback((node: string, selectedMode: ClusterMode): string => {
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
  }, [graph]);

  // ============================================
  // Focus Mode Visibility
  // ============================================
  
  const applyFocusModeVisibility = useCallback(() => {
    // Focus mode: hide edges that are not specializationOf or atLocation
    graph.forEachEdge((edgeKey, attributes) => {
      const label = typeof attributes?.label === "string" ? attributes.label : "";
      const isSpecialization = label.endsWith(METAGRAPH_RELATION_SUFFIXES.specialization);
      const isLocation = label.endsWith(METAGRAPH_RELATION_SUFFIXES.location);

      if (focusMode) {
        graph.setEdgeAttribute(edgeKey, "hidden", !(isSpecialization || isLocation));
      } else {
        graph.setEdgeAttribute(edgeKey, "hidden", isSpecialization || isLocation);
      }
    });

    // Hide/show nodes based on focus mode
    graph.forEachNode((nodeKey, attributes) => {
      const label = typeof attributes?.label === "string" ? attributes.label : "";
      const isVNG = label.startsWith(VERSIONED_NODE_PREFIX);
      const relations = attributes.metagraphRelations as { specialization?: boolean; location?: boolean } | undefined;
      const isNamedGraph = relations?.specialization === true;
      const isVersion = relations?.location === true;

      if (focusMode) {
        graph.setNodeAttribute(nodeKey, "hidden", !(isVNG || isNamedGraph || isVersion));
      } else {
        graph.setNodeAttribute(nodeKey, "hidden", isNamedGraph || isVersion);
      }
    });
  }, [focusMode, graph]);

  // ============================================
  // Clustering Layout
  // ============================================
  
  const rememberBaselines = useCallback((nodes: string[]) => {
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
  }, [graph]);

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
  }, [getVersionedNodes, graph]);

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
    const baseRadius = Math.max(width, height) * 0.65 + 40;
    const clusterScaleFactor = clusterCount > 4 ? 1 + (clusterCount - 4) * 0.15 : 1;
    const radius = baseRadius * clusterScaleFactor;
    
    return {
      center: { x: (minX + maxX) / 2, y: (minY + maxY) / 2 },
      radius,
    };
  }, [getVersionedNodes, graph]);

  const computeClusterCenters = useCallback((groups: string[], bounds: { center: Point; radius: number }): Record<string, Point> => {
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
  }, []);

  const applyClustering = useCallback((): ClusterStats => {
    const versionedNodes = getVersionedNodes();
    
    rememberBaselines(versionedNodes);

    if (clusterMode === "none" || versionedNodes.length === 0) {
      restoreBaseline();
      return {};
    }

    const clusters = new Map<string, string[]>();

    versionedNodes.forEach((node) => {
      const clusterKey = getClusterKey(node, clusterMode);
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
    const clusterEntries = Array.from(clusters.entries());

    clusterEntries.forEach(([clusterKey, nodes], index) => {
      const clusterColor = clusterColors[index % clusterColors.length];
      const center = clusterCenters[clusterKey] ?? { x: 0, y: 0 };
      const ring = Math.max(nodes.length, 1);

      nodes.forEach((node, nodeIndex) => {
        const angle = (2 * Math.PI * nodeIndex) / ring;
        const distance = 14 + (nodeIndex % 5) * 4;
        graph.setNodeAttribute(node, "x", center.x + Math.cos(angle) * distance);
        graph.setNodeAttribute(node, "y", center.y + Math.sin(angle) * distance);
        graph.setNodeAttribute(node, "color", clusterColor);
      });
    });

    // Return stats
    return Object.fromEntries(
      clusterEntries.map(([key, value], index) => [
        key,
        {
          count: value.length,
          color: clusterColors[index % clusterColors.length],
        },
      ]),
    );
  }, [clusterMode, computeBounds, computeClusterCenters, getClusterKey, getVersionedNodes, graph, rememberBaselines, restoreBaseline]);

  // ============================================
  // Coordinated Effect
  // ============================================
  
  useEffect(() => {
    if (previousGraphRef.current !== graph) {
      previousGraphRef.current = graph;
      originalColorsRef.current.clear();
      originalPositionsRef.current.clear();
      
      if (!isInitialMountRef.current) {
        dispatch(setClusterStats({}));
      }
    }
    isInitialMountRef.current = false;
  }, [dispatch, graph]);

  useEffect(() => {
    // Use requestAnimationFrame to batch updates
    const frameId = requestAnimationFrame(() => {
      const stats = applyClustering();
      dispatch(setClusterStats(stats));
      
      applyFocusModeVisibility();
    });

    return () => {
      cancelAnimationFrame(frameId);
    };
  }, [applyClustering, applyFocusModeVisibility, dispatch, focusMode, clusterMode]);

  return null;
};
