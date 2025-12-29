import { type FC } from "react";
import { useAppDispatch } from "../state/hooks";
import { setMergedGraphsEnabled } from "../state/versionedGraphSlice";

interface MergedGraphsToggleProps {
  enabled: boolean;
}

export const MergedGraphsToggle: FC<MergedGraphsToggleProps> = ({ enabled }) => {
  const dispatch = useAppDispatch();

  const handleToggle = () => {
    dispatch(setMergedGraphsEnabled(!enabled));
  };

  return (
    <div
      style={{
        backgroundColor: "rgba(255, 255, 255, 0.9)",
        padding: "8px 12px",
        borderRadius: "5px",
        boxShadow: "0 2px 4px rgba(0,0,0,0.1)",
        display: "flex",
        alignItems: "center",
        gap: "8px",
        cursor: "pointer",
        userSelect: "none",
      }}
      onClick={handleToggle}
    >
      <input
        type="checkbox"
        checked={enabled}
        onChange={handleToggle}
        style={{ cursor: "pointer" }}
      />
      <label style={{ margin: 0, cursor: "pointer", fontSize: "14px" }}>
        Merged graphs
      </label>
    </div>
  );
};
