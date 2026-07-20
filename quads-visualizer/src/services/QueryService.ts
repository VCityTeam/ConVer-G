import { type Response } from "../utils/responseSerializer";

// Runtime config injected by env.sh (config.js) at container start; absent in dev.
const runtimeEnv: Record<string, string | undefined> =
  (window as Window & { env?: Record<string, string | undefined> }).env ?? {};

export class QueryService {
  public static readonly QUERY_ENDPOINT = runtimeEnv.VITE_QUERY_ENDPOINT || import.meta.env.VITE_QUERY_ENDPOINT || "http://localhost:5173/rdf/query";
  private static readonly LOADER_ENDPOINT = runtimeEnv.VITE_LOADER_ENDPOINT || import.meta.env.VITE_LOADER_ENDPOINT || "http://localhost:5173/import/metadata";

  /**
   * The query endpoint with the inference mode encoded as the `?infer=` query
   * parameter. Pass {@code undefined} (or an empty value) to omit it, letting the
   * server apply its default. Useful for consumers that configure their own
   * request, such as the YASGUI SPARQL panel.
   */
  static endpointWithInference(infer?: string): string {
    if (!infer) {
      return this.QUERY_ENDPOINT;
    }
    const separator = this.QUERY_ENDPOINT.includes("?") ? "&" : "?";
    return `${this.QUERY_ENDPOINT}${separator}infer=${encodeURIComponent(infer)}`;
  }

  /**
   * @param query the SPARQL query
   * @param infer the `?infer=` inference mode to request; when omitted the server
   *              applies its configured default
   */
  static async executeQuery(query: string, infer?: string): Promise<Response> {
    const body = new URLSearchParams();
    body.append("query", query);

    const response = await fetch(this.endpointWithInference(infer), {
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
