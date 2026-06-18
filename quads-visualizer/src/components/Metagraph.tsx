import { type CSSProperties, type FC } from "react";
import { useAppSelector } from "../state/hooks";
import { type Response } from "../utils/responseSerializer.ts";
import { MetagraphMatrix } from "./MetagraphMatrix.tsx";
import { MetagraphGraphView } from "./MetagraphGraphView.tsx";

/**
 * Metagraph panel. Defaults to the readable coverage matrix (sources × versions)
 * and switches to the editable node-link graph via the in-view toggle.
 */
export const Metagraph: FC<{
  response: Response;
  style?: CSSProperties;
}> = ({ response, style }) => {
  const view = useAppSelector((state) => state.metagraph.metagraphView);

  return view === "matrix" ? (
    <MetagraphMatrix response={response} style={style} />
  ) : (
    <MetagraphGraphView response={response} style={style} />
  );
};
