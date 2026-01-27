import { useAppDispatch, useAppSelector } from "../state/hooks.ts";
import { setClusterMode } from "../state/metagraphSlice.ts";

/**
 * UI component for cluster mode controls.
 * The actual clustering logic is handled by MetagraphEffectsCoordinator.
 */
export const MetagraphClustersUI = () => {
  const dispatch = useAppDispatch();
  const mode = useAppSelector((state) => state.metagraph.clusterMode);
  const stats = useAppSelector((state) => state.metagraph.clusterStats);

  return (
    <div
      style={{
        padding: "10px",
        borderRadius: "10px",
        maxWidth: "240px",
        pointerEvents: "auto",
        display: "flex",
        flexDirection: "column",
        gap: "8px",
      }}
    >
      <div style={{ fontWeight: 700, fontSize: "14px", color: "#0f172a" }}>
        Cluster metadata using <a href="https://www.w3.org/TR/prov-o/" target="_blank"
          rel="noopener noreferrer">PROV-O</a>
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "6px" }}>
        <button
          type="button"
          onClick={() => dispatch(setClusterMode(mode === "specialization" ? "none" : "specialization"))}
          style={{
            padding: "6px 8px",
            borderRadius: "6px",
            border: mode === "specialization" ? "2px solid #2563eb" : "1px solid #cbd5e1",
            background: mode === "specialization" ? "#e0edff" : "#f8fafc",
            fontWeight: 600,
            cursor: "pointer",
          }}
        >
          Specialization
        </button>
        <button
          type="button"
          onClick={() => dispatch(setClusterMode(mode === "location" ? "none" : "location"))}
          style={{
            padding: "6px 8px",
            borderRadius: "6px",
            border: mode === "location" ? "2px solid #2563eb" : "1px solid #cbd5e1",
            background: mode === "location" ? "#e0edff" : "#f8fafc",
            fontWeight: 600,
            cursor: "pointer",
          }}
        >
          Location
        </button>
      </div>
      <div style={{ fontSize: "12px", color: "#475569", lineHeight: 1.4 }}>
        {mode === "none" && (
          <>Toggle a mode to reposition metadata nodes into clusters. Click again to restore the base layout.</>
        )}
        {mode === "specialization" && (
          <>
            Groups entities that are specializations of a general entity. See{" "}
            <a href="https://www.w3.org/TR/prov-o/#specializationOf" target="_blank" rel="noopener noreferrer">
              specializationOf
            </a>
            .
          </>
        )}
        {mode === "location" && (
          <>
            Groups entities based on their location. See{" "}
            <a href="https://www.w3.org/TR/prov-o/#atLocation" target="_blank" rel="noopener noreferrer">
              atLocation
            </a>
            .
          </>
        )}
      </div>
      {
        Object.keys(stats).length > 0 && (
          <div
            style={{
              background: "#f8fafc",
              border: "1px solid #e2e8f0",
              borderRadius: "8px",
              padding: "8px",
              maxHeight: "100px",
              overflowY: "auto",
              display: "flex",
              flexDirection: "column",
              gap: "6px",
            }}
          >
            {(
              Object.entries(stats)
                .sort(([, aValue], [, bValue]) => bValue.count - aValue.count)
                .map(([key, { count, color }]) => (
                  <div
                    key={key}
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      padding: "6px 8px",
                      background: "white",
                      borderRadius: "6px",
                      border: "1px solid #e2e8f0",
                      fontSize: "12px",
                      color: "#0f172a",
                    }}
                  >
                    <div style={{ display: "flex", alignItems: "center", gap: "6px", overflow: "hidden" }}>
                      <div
                        style={{
                          width: "8px",
                          height: "8px",
                          borderRadius: "50%",
                          backgroundColor: color,
                          flexShrink: 0,
                        }}
                      />
                      <span
                        title={key}
                        style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}
                      >
                        {key}
                      </span>
                    </div>
                    <span style={{ fontWeight: 700, marginLeft: "8px" }}>{count}</span>
                  </div>
                ))
            )}
          </div>
        )
      }
    </div>
  );
};
