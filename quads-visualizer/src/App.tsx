import { useCallback, useEffect, useState, type FC } from "react";
import { VersionedGraph } from "./components/VersionedGraph.tsx";
import { type Response } from "./utils/responseSerializer.ts";
import { Metagraph } from "./components/Metagraph.tsx";
import { QueryService } from "./services/QueryService.ts";
import "./App.css";

const METAGRAPH_QUERY = `
  SELECT * WHERE {
      ?subject ?predicate ?object .
  }
`;

const DATA_QUERY = `
  SELECT * WHERE {
      GRAPH ?versionedgraph {
          ?subject ?predicate ?object .
      }
      ?versionedgraph <http://www.w3.org/ns/prov#specializationOf> ?graph ;
                      <http://www.w3.org/ns/prov#atLocation> ?version .
  }
`;

const PanelStatus: FC<{ message: string; onRetry?: () => void }> = ({ message, onRetry }) => (
  <div className="panel-status">
    <span>{message}</span>
    {onRetry && (
      <button type="button" onClick={onRetry}>
        Retry
      </button>
    )}
  </div>
);

export const App: FC = () => {
  const [metagraph, setMetagraph] = useState<Response | null>(null);
  const [data, setData] = useState<Response | null>(null);
  const [metagraphError, setMetagraphError] = useState<string | null>(null);
  const [dataError, setDataError] = useState<string | null>(null);

  const fetchMetagraph = useCallback(async () => {
    setMetagraphError(null);
    try {
      setMetagraph(await QueryService.executeQuery(METAGRAPH_QUERY));
    } catch (e) {
      setMetagraphError(e instanceof Error ? e.message : "Unknown error");
    }
  }, []);

  const fetchData = useCallback(async () => {
    setDataError(null);
    try {
      setData(await QueryService.executeQuery(DATA_QUERY));
    } catch (e) {
      setDataError(e instanceof Error ? e.message : "Unknown error");
    }
  }, []);

  useEffect(() => {
    fetchMetagraph();
    fetchData();
  }, [fetchMetagraph, fetchData]);

  return (
    <div className="app-grid">
      <div className="graph-panel">
        {metagraphError ? (
          <PanelStatus message={`Failed to load metagraph: ${metagraphError}`} onRetry={fetchMetagraph} />
        ) : metagraph ? (
          <Metagraph response={metagraph} style={{ height: "100%" }} />
        ) : (
          <PanelStatus message="Loading…" />
        )}
      </div>
      <div className="graph-panel">
        {dataError ? (
          <PanelStatus message={`Failed to load data: ${dataError}`} onRetry={fetchData} />
        ) : data ? (
          <VersionedGraph response={data} metagraph={metagraph} style={{ height: "100%" }} />
        ) : (
          <PanelStatus message="Loading…" />
        )}
      </div>
    </div>
  );
};

export default App;
