import { type FC } from "react";
import { useAppDispatch, useAppSelector } from "../state/hooks";
import { setGridColumns, setGridGap } from "../state/versionedGraphSlice";

export const GridLayoutControls: FC = () => {
    const dispatch = useAppDispatch();
    const gridColumns = useAppSelector((state) => state.versionedGraph.gridColumns);
    const gridGap = useAppSelector((state) => state.versionedGraph.gridGap);

    return (
        <div
            style={{
                backgroundColor: "white",
                padding: "10px",
                borderRadius: "6px",
                display: "flex",
                flexDirection: "column",
                gap: "8px",
                minWidth: "140px",
            }}
        >
            <div style={{ fontWeight: 600, fontSize: "13px", color: "#374151" }}>
                Grid Layout
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "12px", color: "#4b5563" }}>
                    <span style={{ minWidth: "60px" }}>Columns:</span>
                    <input
                        type="number"
                        min={1}
                        max={10}
                        value={gridColumns}
                        onChange={(e) => dispatch(setGridColumns(Math.max(1, parseInt(e.target.value) || 1)))}
                        style={{
                            width: "50px",
                            padding: "4px 6px",
                            border: "1px solid #d1d5db",
                            borderRadius: "4px",
                            fontSize: "12px",
                        }}
                    />
                </label>
                <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "12px", color: "#4b5563" }}>
                    <span style={{ minWidth: "60px" }}>Gap:</span>
                    <input
                        type="number"
                        min={0}
                        max={500}
                        step={25}
                        value={gridGap}
                        onChange={(e) => dispatch(setGridGap(Math.max(0, parseInt(e.target.value) || 0)))}
                        style={{
                            width: "50px",
                            padding: "4px 6px",
                            border: "1px solid #d1d5db",
                            borderRadius: "4px",
                            fontSize: "12px",
                        }}
                    />
                </label>
            </div>
        </div>
    );
};
