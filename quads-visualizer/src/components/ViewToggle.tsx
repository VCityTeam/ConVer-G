import { type FC } from "react";
import { useAppDispatch, useAppSelector } from "../state/hooks";
import { setMetagraphView, type MetagraphView } from "../state/metagraphSlice";

const OPTIONS: Array<{ id: MetagraphView; label: string; title: string }> = [
  { id: "matrix", label: "▦ Matrix", title: "Coverage matrix — sources × versions" },
  { id: "graph", label: "⬡ Graph", title: "Node-link graph — edit & upload metadata" },
];

/**
 * Segmented control switching the metagraph panel between the readable
 * coverage matrix (default) and the editable node-link graph.
 */
export const ViewToggle: FC = () => {
  const dispatch = useAppDispatch();
  const view = useAppSelector((state) => state.metagraph.metagraphView);

  return (
    <div className="mg-view-toggle" role="group" aria-label="Metagraph view">
      {OPTIONS.map(({ id, label, title }) => (
        <button
          key={id}
          type="button"
          title={title}
          aria-pressed={view === id}
          className={view === id ? "mg-view-toggle__btn is-active" : "mg-view-toggle__btn"}
          onClick={() => dispatch(setMetagraphView(id))}
        >
          {label}
        </button>
      ))}
    </div>
  );
};
