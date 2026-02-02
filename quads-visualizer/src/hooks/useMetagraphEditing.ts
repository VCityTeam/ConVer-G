import { useCallback } from "react";
import { useSigma } from "@react-sigma/core";
import { applyMetagraphNodeColors, createEmptyMetagraphRelations, METAGRAPH_NODE_COLORS } from "../utils/metagraphBuilder";
import { NODE_SIZES } from "../utils/constants";
import { QueryService } from "../services/QueryService";

const computeTermType = (name: string): string => {
  if (name.startsWith("http://") || name.startsWith("https://")) {
    return "resource";
  }
  return "literal";
};

export const useMetagraphEditing = () => {
  const sigma = useSigma();

  const generateTTL = useCallback(() => {
    const graph = sigma.getGraph();
    let ttl = "";

    const formatTerm = (term: string, type?: string) => {
      if (type === "literal") {
        return `"${term.replace(/"/g, '\\"')}"`;
      }
      return `<${term}>`;
    };

    graph.forEachEdge((_edge, attributes, source, target) => {
      const sourceNode = graph.getNodeAttributes(source);
      const targetNode = graph.getNodeAttributes(target);

      const s = formatTerm(source, sourceNode.termType);
      const p = `<${attributes.label}>`;
      const o = formatTerm(target, targetNode.termType);

      ttl += `${s} ${p} ${o} .\n`;
    });
    return ttl;
  }, [sigma]);

  const downloadGraphAsTTL = useCallback(() => {
    const ttl = generateTTL();
    const blob = new Blob([ttl], { type: "text/turtle" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "metagraph.ttl";
    a.click();
    URL.revokeObjectURL(url);
  }, [generateTTL]);

  const uploadMetagraph = useCallback(async () => {
    const ttl = generateTTL();
    try {
      await QueryService.uploadMetadata(ttl);
      alert("Metagraph uploaded successfully!");
    } catch (e) {
      console.error(e);
      alert("Failed to upload metagraph.");
    }
  }, [generateTTL]);

  const handleCreateNode = useCallback((
    name: string,
    pendingNodeCoord: { x: number; y: number },
  ): string | null => {
    const graph = sigma.getGraph();
    if (graph.hasNode(name)) {
      return "A node with this name already exists.";
    }

    graph.addNode(name, {
      ...pendingNodeCoord,
      termType: computeTermType(name),
      size: NODE_SIZES.DEFAULT,
      color: METAGRAPH_NODE_COLORS.default,
      label: name,
      metagraphRelations: createEmptyMetagraphRelations(),
    });

    applyMetagraphNodeColors(graph);
    return null;
  }, [sigma]);

  const handleAddEdge = useCallback((
    sourceNode: string,
    targetNode: string,
    relation: string,
  ) => {
    let finalRelation = relation.trim();

    if (finalRelation.startsWith("prov:")) {
      finalRelation = finalRelation.replace("prov:", "http://www.w3.org/ns/prov#");
    }

    if (finalRelation) {
      const graph = sigma.getGraph();
      graph.addEdge(sourceNode, targetNode, {
        label: finalRelation,
        relation: finalRelation,
        size: 4,
      });
      applyMetagraphNodeColors(graph, true);
    }
  }, [sigma]);

  return {
    downloadGraphAsTTL,
    uploadMetagraph,
    handleCreateNode,
    handleAddEdge,
  };
};
