import {
  type CSSProperties,
  type FC,
  type ReactNode,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { useAppDispatch, useAppSelector } from "../state/hooks";
import { setTravelHoverSelection } from "../state/metagraphSlice";
import { navigateToMetagraphNode } from "../state/thunks";
import { type Response } from "../utils/responseSerializer";
import {
  getCellKey,
  sourceLabel,
  useMetagraphMatrix,
  versionLabel,
} from "../utils/metagraphMatrix";
import { ViewToggle } from "./ViewToggle";

const ROW_HEADER_W = 172;
const COL_HEADER_H = 86;
const OVERSCAN = 4;

// Cell-size slider bounds (px); the gap and corner radius are derived from it.
const CELL_MIN = 12;
const CELL_MAX = 36;
const CELL_DEFAULT = 18;

type Hover = { r: number | null; c: number | null };

const COLORS = {
  filled: "#6e56f0",
  filledHover: "#4f2fe0",
  filledSelected: "#3413c4",
  empty: "#eef0f4",
  emptyHover: "#dde1ea",
  guide: "rgba(110, 86, 240, 0.10)",
  guideStrong: "rgba(110, 86, 240, 0.18)",
};

export const MetagraphMatrix: FC<{ response: Response; style?: CSSProperties }> = ({
  response,
  style,
}) => {
  const dispatch = useAppDispatch();
  const matrix = useMetagraphMatrix(response);
  const selectedGraph = useAppSelector((state) => state.versionedGraph.selectedGraph);
  const selectedVersion = useAppSelector((state) => state.versionedGraph.selectedVersion);
  const selectedType = useAppSelector((state) => state.metagraph.selectedMetagraphNodeType);

  const [cellSize, setCellSize] = useState<number>(CELL_DEFAULT);
  const [sourceFilter, setSourceFilter] = useState("");
  const [versionFilter, setVersionFilter] = useState("");
  const [scroll, setScroll] = useState({ left: 0, top: 0 });
  const [bodySize, setBodySize] = useState({ w: 0, h: 0 });
  const [hover, setHover] = useState<Hover | null>(null);

  const bodyRef = useRef<HTMLDivElement | null>(null);
  const rafRef = useRef<number | null>(null);
  const previewTimer = useRef<number | null>(null);

  const cell = cellSize;
  const gap = Math.max(2, Math.round(cell / 6));
  const cellRadius = cell <= 20 ? 3 : 4;
  const stride = cell + gap;

  // Filtered axes (case-insensitive substring match on the display label).
  const sources = useMemo(() => {
    const q = sourceFilter.trim().toLowerCase();
    if (!q) return matrix.sources;
    return matrix.sources.filter((s) => sourceLabel(s).toLowerCase().includes(q));
  }, [matrix.sources, sourceFilter]);

  const versions = useMemo(() => {
    const q = versionFilter.trim().toLowerCase();
    if (!q) return matrix.versions;
    return matrix.versions.filter((v) => versionLabel(v).toLowerCase().includes(q));
  }, [matrix.versions, versionFilter]);

  const contentW = versions.length * stride;
  const contentH = sources.length * stride;
  const isFiltered = sources.length !== matrix.sources.length || versions.length !== matrix.versions.length;

  // Measure the scrollable body so virtualization knows the viewport.
  useLayoutEffect(() => {
    const el = bodyRef.current;
    if (!el) return;
    const update = () => setBodySize({ w: el.clientWidth, h: el.clientHeight });
    update();
    const ro = new ResizeObserver(update);
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  // Clear any live preview when the matrix unmounts.
  useEffect(
    () => () => {
      if (previewTimer.current != null) window.clearTimeout(previewTimer.current);
      dispatch(setTravelHoverSelection(null));
    },
    [dispatch],
  );

  const onScroll = () => {
    if (rafRef.current != null) return;
    rafRef.current = requestAnimationFrame(() => {
      rafRef.current = null;
      const el = bodyRef.current;
      if (el) setScroll({ left: el.scrollLeft, top: el.scrollTop });
    });
  };

  // Debounce the right-panel preview so sweeping across cells stays smooth.
  const schedulePreview = (selection: Parameters<typeof setTravelHoverSelection>[0]) => {
    if (previewTimer.current != null) window.clearTimeout(previewTimer.current);
    previewTimer.current = window.setTimeout(() => {
      dispatch(setTravelHoverSelection(selection));
    }, 60);
  };

  const clearPreview = () => {
    if (previewTimer.current != null) window.clearTimeout(previewTimer.current);
    dispatch(setTravelHoverSelection(null));
  };

  // Deferred clear for when the cursor leaves a snapshot cell (into a gap, a
  // header or off the grid). It is cancelled by the next `schedulePreview`, so
  // sweeping between cells stays smooth, but resting off a snapshot ends the
  // preview instead of leaving a stale diff on the right panel.
  const scheduleClear = () => {
    if (previewTimer.current != null) window.clearTimeout(previewTimer.current);
    previewTimer.current = window.setTimeout(() => {
      dispatch(setTravelHoverSelection(null));
    }, 60);
  };

  // Visible window (virtualization).
  const colStart = Math.max(0, Math.floor(scroll.left / stride) - OVERSCAN);
  const colEnd = Math.min(versions.length, Math.ceil((scroll.left + bodySize.w) / stride) + OVERSCAN);
  const rowStart = Math.max(0, Math.floor(scroll.top / stride) - OVERSCAN);
  const rowEnd = Math.min(sources.length, Math.ceil((scroll.top + bodySize.h) / stride) + OVERSCAN);

  const selectedRow = selectedType === "vng" || selectedType === "namedGraph" ? selectedGraph : null;
  const selectedCol = selectedType === "vng" || selectedType === "version" ? selectedVersion : null;

  // --- Interactions -------------------------------------------------------
  const onCellEnter = (r: number, c: number, filled: boolean, source: string, version: string) => {
    setHover({ r, c });
    if (filled) {
      schedulePreview({ graph: source, version, nodeType: "vng" });
    } else {
      clearPreview();
    }
  };

  const onCellClick = (filled: boolean, source: string, version: string) => {
    if (!filled) return;
    const vng = matrix.cells.get(getCellKey(source, version));
    if (!vng) return;
    dispatch(navigateToMetagraphNode({ nodeKey: vng, nodeType: "vng", graph: source, version }));
  };

  const onRowEnter = (r: number, source: string) => {
    setHover({ r, c: null });
    schedulePreview({ graph: source, nodeType: "namedGraph" });
  };

  const onColEnter = (c: number, version: string) => {
    setHover({ r: null, c });
    schedulePreview({ version, nodeType: "version" });
  };

  const onBodyLeave = () => {
    setHover(null);
    clearPreview();
  };

  // --- Cells --------------------------------------------------------------
  const cells = [] as ReactNode[];
  for (let r = rowStart; r < rowEnd; r++) {
    const source = sources[r];
    for (let c = colStart; c < colEnd; c++) {
      const version = versions[c];
      const filled = matrix.cells.has(getCellKey(source, version));
      const isHovered = hover?.r === r && hover?.c === c;
      const isSelected = selectedType === "vng" && source === selectedRow && version === selectedCol;

      let background = filled ? COLORS.filled : COLORS.empty;
      if (isSelected && filled) background = COLORS.filledSelected;
      else if (isHovered) background = filled ? COLORS.filledHover : COLORS.emptyHover;

      cells.push(
        <div
          key={`${r}:${c}`}
          className="mg-cell"
          title={`${sourceLabel(source)} · ${versionLabel(version)}\n${filled ? "snapshot — click to load" : "no snapshot"}`}
          onMouseEnter={() => onCellEnter(r, c, filled, source, version)}
          onMouseLeave={() => scheduleClear()}
          onClick={() => onCellClick(filled, source, version)}
          style={{
            position: "absolute",
            left: c * stride,
            top: r * stride,
            width: cell,
            height: cell,
            background,
            borderRadius: cellRadius,
            cursor: filled ? "pointer" : "default",
            boxShadow: isSelected && filled ? "0 0 0 2px #1e0fa8" : undefined,
            zIndex: isSelected ? 2 : 1,
          }}
        />,
      );
    }
  }

  // Crosshair guide bands (behind cells) for the hovered/selected row & column.
  const bands = [] as ReactNode[];
  const pushRowBand = (r: number, color: string, key: string) =>
    bands.push(
      <div key={key} style={{ position: "absolute", left: 0, top: r * stride, width: contentW, height: cell, background: color, borderRadius: 3 }} />,
    );
  const pushColBand = (c: number, color: string, key: string) =>
    bands.push(
      <div key={key} style={{ position: "absolute", top: 0, left: c * stride, width: cell, height: contentH, background: color, borderRadius: 3 }} />,
    );
  if (selectedRow) {
    const r = sources.indexOf(selectedRow);
    if (r >= 0) pushRowBand(r, COLORS.guideStrong, "sel-row");
  }
  if (selectedCol) {
    const c = versions.indexOf(selectedCol);
    if (c >= 0) pushColBand(c, COLORS.guideStrong, "sel-col");
  }
  if (hover?.r != null) pushRowBand(hover.r, COLORS.guide, "hov-row");
  if (hover?.c != null) pushColBand(hover.c, COLORS.guide, "hov-col");

  // --- Headers ------------------------------------------------------------
  const colHeaders = [] as ReactNode[];
  for (let c = colStart; c < colEnd; c++) {
    const version = versions[c];
    const active = version === selectedCol;
    const hot = hover?.c === c;
    colHeaders.push(
      <div
        key={version}
        className={`mg-col-head${active ? " is-active" : ""}${hot ? " is-hot" : ""}`}
        title={version}
        onMouseEnter={() => onColEnter(c, version)}
        onClick={() => dispatch(navigateToMetagraphNode({ nodeKey: version, nodeType: "version", version }))}
        style={{ position: "absolute", left: c * stride, top: 0, width: stride, height: COL_HEADER_H }}
      >
        <span style={{ width: cell }}>{versionLabel(version)}</span>
      </div>,
    );
  }

  const rowHeaders = [] as ReactNode[];
  for (let r = rowStart; r < rowEnd; r++) {
    const source = sources[r];
    const active = source === selectedRow;
    const hot = hover?.r === r;
    rowHeaders.push(
      <div
        key={source}
        className={`mg-row-head${active ? " is-active" : ""}${hot ? " is-hot" : ""}`}
        title={source}
        onMouseEnter={() => onRowEnter(r, source)}
        onClick={() => dispatch(navigateToMetagraphNode({ nodeKey: source, nodeType: "namedGraph", graph: source }))}
        style={{ position: "absolute", left: 0, top: r * stride, width: ROW_HEADER_W, height: cell }}
      >
        <span className="mg-row-head__label">{sourceLabel(source)}</span>
      </div>,
    );
  }

  const empty = matrix.sources.length === 0 || matrix.versions.length === 0;

  return (
    <div className="mg-matrix" style={style}>
      <div className="mg-toolbar">
        <div className="mg-toolbar__row">
          <ViewToggle />
          <span className="mg-counts">
            <b>{matrix.sources.length}</b> sources × <b>{matrix.versions.length}</b> versions
            {" · "}
            <b>{matrix.snapshotCount}</b> snapshots
            {isFiltered && (
              <em className="mg-counts__filtered"> (showing {sources.length}×{versions.length})</em>
            )}
          </span>
          <div className="mg-spacer" />
          <label className="mg-size" title="Cell size">
            <span className="mg-size__icon" aria-hidden="true">⊖</span>
            <input
              type="range"
              className="mg-size__slider"
              min={CELL_MIN}
              max={CELL_MAX}
              value={cellSize}
              aria-label="Matrix cell size"
              onChange={(e) => setCellSize(Number(e.target.value))}
            />
            <span className="mg-size__icon" aria-hidden="true">⊕</span>
          </label>
        </div>
        <div className="mg-toolbar__row">
          <input
            className="mg-input"
            placeholder="Filter sources…"
            value={sourceFilter}
            onChange={(e) => setSourceFilter(e.target.value)}
          />
          <input
            className="mg-input"
            placeholder="Filter versions… (e.g. 2026-03)"
            value={versionFilter}
            onChange={(e) => setVersionFilter(e.target.value)}
          />
          <div className="mg-legend">
            <span><i style={{ background: COLORS.filled }} /> snapshot</span>
            <span><i style={{ background: COLORS.empty }} /> none</span>
          </div>
        </div>
      </div>

      {empty ? (
        <div className="mg-empty">No versioned graphs found in the metagraph.</div>
      ) : (
        <div className="mg-grid">
          {/* Corner */}
          <div className="mg-corner" style={{ width: ROW_HEADER_W, height: COL_HEADER_H }}>
            <span className="mg-corner__y">versions →</span>
            <span className="mg-corner__x">sources ↓</span>
          </div>

          {/* Column headers (scroll-synced horizontally) */}
          <div className="mg-colhead-pane" style={{ left: ROW_HEADER_W, height: COL_HEADER_H }}>
            <div style={{ position: "absolute", inset: 0, width: contentW, transform: `translateX(${-scroll.left}px)` }}>
              {colHeaders}
            </div>
          </div>

          {/* Row headers (scroll-synced vertically) */}
          <div className="mg-rowhead-pane" style={{ top: COL_HEADER_H, width: ROW_HEADER_W }}>
            <div style={{ position: "absolute", inset: 0, height: contentH, transform: `translateY(${-scroll.top}px)` }}>
              {rowHeaders}
            </div>
          </div>

          {/* Body (the scroller) */}
          <div
            className="mg-body"
            ref={bodyRef}
            onScroll={onScroll}
            onMouseLeave={onBodyLeave}
            style={{ top: COL_HEADER_H, left: ROW_HEADER_W }}
          >
            <div style={{ position: "relative", width: contentW, height: contentH }}>
              {bands}
              {cells}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
