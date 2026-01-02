import { useEffect, useState, type FC } from "react";
import { VersionedGraph } from "./components/VersionedGraph.tsx";
import { type Response } from "./utils/responseSerializer.ts";
import { Metagraph } from "./components/Metagraph.tsx";
import { QueryService } from "./services/QueryService.ts";
import { SparqlQuery } from "./components/SparqlQuery.tsx";
import "./App.css";

export const App: FC = () => {
  const [metagraph, setMetagraph] = useState<Response | null>(null);
  const [data, setData] = useState<Response | null>(null);

  const fetchMetagraph = async () => {
      const response = await QueryService.executeQuery(`
        SELECT * WHERE {
            ?subject ?predicate ?object .
        }
      `);
      setMetagraph(response);
  };
  const fetchData = async () => {
      const response = await QueryService.executeQuery(`
        SELECT * WHERE {
            GRAPH ?versionedgraph {
                ?subject ?predicate ?object .
            }
            ?versionedgraph <http://www.w3.org/ns/prov#specializationOf> ?graph ;
                            <http://www.w3.org/ns/prov#atLocation> ?version .
        }
      `);
      setData(response);
  };

  useEffect(() => {
    fetchMetagraph();
    fetchData();
  }, []);

  return (
    <div className="app-grid">
      <div className="graph-panel">
        {metagraph ? <Metagraph response={metagraph} style={{ height: "100%" }} /> : "Loading..."}
      </div>
      <div className="graph-panel">
        {data ? <VersionedGraph response={data} metagraph={metagraph} style={{ height: "100%" }} /> : "Loading..."}
      </div>
      <SparqlQuery />
    </div>
  );
};

export default App;
