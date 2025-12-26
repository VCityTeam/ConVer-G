import { type Response } from "../utils/responseSerializer";

export class QueryService {
  private static readonly QUERY_ENDPOINT = (window as any).env?.VITE_QUERY_ENDPOINT ||import.meta.env.VITE_QUERY_ENDPOINT || "http://localhost:5173/rdf/query";
  private static readonly LOADER_ENDPOINT = (window as any).env?.VITE_LOADER_ENDPOINT || import.meta.env.VITE_LOADER_ENDPOINT || "http://localhost:5173/import/metadata";

  static async executeQuery(query: string): Promise<Response> {
    const body = new URLSearchParams();
    body.append("query", query);

    const response = await fetch(this.QUERY_ENDPOINT, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: body.toString(),
    });

    if (!response.ok) {
      throw new Error(`Query failed with status ${response.status}`);
    }

    return (await response.json()) as Response;
  }

  static async uploadMetadata(turtle: string): Promise<void> {
    const formData = new FormData();
    formData.append("file", new Blob([turtle], { type: "text/turtle" }), "metadata.ttl");

    const response = await fetch(this.LOADER_ENDPOINT, {
      method: "POST",
      body: formData,
    });

    if (!response.ok) {
      throw new Error(`Upload failed with status ${response.status}`);
    }
  }
}
