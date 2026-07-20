import { type FC } from "react";
import { useAppDispatch, useAppSelector } from "../state/hooks";
import { setInferenceMode, type InferenceMode } from "../state/inferenceSlice";

const OPTIONS: Array<{ id: InferenceMode; label: string; title: string }> = [
  { id: "default", label: "Server default", title: "Use the server's configured ENTAILMENT_REGIME / SWRL_RULES" },
  { id: "off", label: "Off", title: "No inference — explicit facts only" },
  { id: "rdfs", label: "RDFS", title: "RDFS entailment (subClassOf, subPropertyOf, domain, range)" },
  { id: "owl_lite", label: "OWL Lite", title: "OWL-Lite (currently the same rule set as RDFS)" },
  { id: "swrl", label: "SWRL", title: "The verified SWRL rules" },
  { id: "rdfs+swrl", label: "RDFS + SWRL", title: "RDFS entailment combined with the SWRL rules" },
  { id: "all", label: "All", title: "Every available inference source" },
];

/**
 * Dropdown selecting the query-time inference mode. Bound to the {@code inference}
 * slice; the chosen mode is sent to quads-query as the `?infer=` parameter (see
 * {@link QueryService}), so both graph panels and the ad-hoc SPARQL panel honour it.
 */
export const InferenceModeSelect: FC = () => {
  const dispatch = useAppDispatch();
  const mode = useAppSelector((state) => state.inference.mode);
  const active = OPTIONS.find((option) => option.id === mode);

  return (
    <div className="inference-select">
      <label htmlFor="inference-mode">Inference</label>
      <select
        id="inference-mode"
        value={mode}
        title={active?.title}
        onChange={(event) => dispatch(setInferenceMode(event.target.value as InferenceMode))}
      >
        {OPTIONS.map(({ id, label, title }) => (
          <option key={id} value={id} title={title}>
            {label}
          </option>
        ))}
      </select>
    </div>
  );
};
