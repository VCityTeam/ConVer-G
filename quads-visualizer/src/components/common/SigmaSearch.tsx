import type { FC } from "react";
import { useSigmaSearch } from "../../hooks/useSigmaSearch.ts";
import { GraphSearch } from "@react-sigma/graph-search";
import { SparqlQuery } from "../SparqlQuery.tsx";

export const SigmaSearch: FC = () => {
  const { selectedNodes, selectedEdges, onFocus, onChange, postSearchResult } = useSigmaSearch();  
  return (
    <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
      <GraphSearch
        value={selectedNodes.length > 0 ? { type: "nodes", id: selectedNodes[0] } : 
               selectedEdges.length > 0 ? { type: "edges", id: selectedEdges[0] } : null}
        onFocus={onFocus}
        onChange={onChange}
        postSearchResult={postSearchResult}
      />
      <SparqlQuery />
    </div>
  );
};