import { type ChangeEvent, type FC } from "react";

const PRESET_RELATIONS = [
  { label: "prov:wasDerivedFrom", value: "prov:wasDerivedFrom" },
  { label: "prov:specializationOf", value: "prov:specializationOf" },
  { label: "prov:atLocation", value: "prov:atLocation" },
];

interface RelationInputProps {
  value: string;
  onChange: (nextValue: string) => void;
  disabled?: boolean;
}

export const RelationInput: FC<RelationInputProps> = ({ value, onChange, disabled = false }) => {
  const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
    onChange(event.target.value);
  };

  const handlePresetClick = (presetValue: string) => {
    onChange(presetValue);
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
      <label style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
        <input
          type="text"
          value={value}
          onChange={handleChange}
          disabled={disabled}
          placeholder="Enter relation URI"
          style={{
            padding: "6px 8px",
            borderRadius: "4px",
            border: "1px solid #ced4da",
            fontSize: "0.95em",
          }}
        />
      </label>
      <div style={{ display: "flex", flexWrap: "wrap", gap: "6px" }}>
        {PRESET_RELATIONS.map(({ label, value: presetValue }) => (
          <button
            key={label}
            type="button"
            disabled={disabled}
            onClick={() => handlePresetClick(presetValue)}
            style={{
              padding: "4px 8px",
              borderRadius: "12px",
              border: "1px solid #ddd",
              backgroundColor: presetValue === value ? "#eef4ff" : "#f7f7f7",
              cursor: disabled ? "not-allowed" : "pointer",
              fontSize: "0.85em",
            }}
          >
            {label}
          </button>
        ))}
      </div>
    </div>
  );
};
