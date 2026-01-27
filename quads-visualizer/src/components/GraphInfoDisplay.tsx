import { type FC, useState } from "react";
import { useAppSelector } from "../state/hooks";

export const GraphInfoDisplay: FC = () => {
    const graph = useAppSelector((state) => state.versionedGraph.selectedGraph);
    const version = useAppSelector((state) => state.versionedGraph.selectedVersion);
    const [isHovered, setIsHovered] = useState(false);

    return (
        <div
            onMouseEnter={() => setIsHovered(true)}
            onMouseLeave={() => setIsHovered(false)}
            style={{
                backgroundColor: "rgba(255, 255, 255, 0.9)",
                padding: "10px",
                borderRadius: "5px",
                boxShadow: isHovered ? "0 2px 4px rgba(0,0,0,0.1)" : "none",
                pointerEvents: "auto",
            }}
        >
            {isHovered ? <>
                <p style={{ margin: 0 }}>Graph: {graph}</p>
                <p style={{ margin: 0 }}>Version: {version}</p>
            </> : "ℹ️"}
        </div>
    );
};
