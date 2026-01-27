import { useCallback, useEffect, useRef, type FC, useState } from "react";
import { createPortal } from 'react-dom';
import Yasgui from "@triply/yasgui";
import "@triply/yasgui/build/yasgui.min.css";
import { QueryService } from "../services/QueryService";
import { useAppDispatch } from "../state/hooks";
import { setHighlightedNodes } from "../state/versionedGraphSlice";
import sparqlIcon from "../assets/sparql.png";
import { useSigmaSPARQLSearch } from "../hooks/useSigmaSPARQLSearch";

export const SparqlQuery: FC = () => {
  const { isValueInGraph, getFocusNodes } = useSigmaSPARQLSearch();
  const dispatch = useAppDispatch();
  const dispatchRef = useRef(dispatch);
  dispatchRef.current = dispatch;
  const yasguiRef = useRef<HTMLDivElement>(null);
  const yasguiInstance = useRef<Yasgui | null>(null);
  const [isOpened, setIsOpened] = useState(false);

  const replacePrefixes = useCallback((value: string) => {
    // Get all yasr defined prefixes from the current tab
    let allPrefixes: Record<string, string> = { };

    if (yasguiInstance.current) {
      const tab = yasguiInstance.current.getTab();
      if (tab) {
        const yasr = tab.getYasr();
        if (yasr) {
          const yasrPrefixes = yasr.getPrefixes();
          if (yasrPrefixes) {
            allPrefixes = { ...yasrPrefixes };
          }
        }
      }
    }

    Object.keys(allPrefixes).forEach(prefix => value = value.replace(prefix + ":", allPrefixes[prefix]));
    return value;
  }, []);

  const handleTermValue = useCallback((term: string) => {
    // IRI without prefix
    if (term.startsWith("<") && term.endsWith(">")) {
      return term.slice(1, -1);
    }

    // IRI with prefix
    if (term.includes(":") && !term.startsWith('"')) {
      return replacePrefixes(term);
    }

    // Literal values without types
    if (term.startsWith('"') && term.endsWith('"')) {
      return term.slice(1, -1);
    }

    // Literal values with types: return only the value (between "")
    if (term.startsWith('"') && term.endsWith('>')) {
      return term.slice(1, -1).split('"')[0];
    }

    return term;
  }, [replacePrefixes]);

  useEffect(() => {
    if (yasguiRef.current && !yasguiInstance.current) {
      yasguiRef.current.innerHTML = "";
      const yasgui = new Yasgui(yasguiRef.current, {
        requestConfig: {
          endpoint: QueryService.QUERY_ENDPOINT,
        },
        yasr: {
          prefixes: {
            prov: "http://www.w3.org/ns/prov#",
            vng: "https://github.com/VCityTeam/ConVer-G/Versioned-Named-Graph#",
            v: "https://github.com/VCityTeam/ConVer-G/Version#"
          }
        }
      });
      yasguiInstance.current = yasgui;

      const handleDrawn = () => {
        const tab = yasgui.getTab();
        if (tab) {
          const yasr = tab.getYasr();
          if (yasr) {
            const resultsEl = yasr.resultsEl;
            const headerCells = resultsEl.querySelectorAll("th");
            const hasIndexColumn = headerCells.length > 0 &&
              (headerCells[0].innerText.trim() === "" || headerCells[0].innerText.trim() === "#");

            const cells = resultsEl.querySelectorAll("td");
            cells.forEach((cell: HTMLTableCellElement) => {
              if (hasIndexColumn && cell.cellIndex === 0) return;

              if (cell.querySelector(".cell-buttons")) return;

              const cellValue = cell.innerText.trim();
              if (!cellValue) return;

              const btnContainer = document.createElement("div");
              btnContainer.className = "cell-buttons";

              const termValue = handleTermValue(cellValue);
              
              // Only show the "V" button if the value exists in the current sigma graph
              if (isValueInGraph(termValue)) {
                const versionedBtn = document.createElement("button");
                versionedBtn.innerText = "🔎";
                versionedBtn.title = "Display in Versioned Graph";
                versionedBtn.onclick = (e) => {
                  e.stopPropagation();
                  dispatchRef.current(setHighlightedNodes({ nodes: getFocusNodes(termValue), source: "sparql" }));
                };

                btnContainer.appendChild(versionedBtn);
              }

              const contentWrapper = cell.querySelector("div");
              if (contentWrapper) {
                contentWrapper.style.display = "flex";
                contentWrapper.style.justifyContent = "space-between";
                contentWrapper.style.alignItems = "center";
                contentWrapper.style.gap = "8px";

                const existingContent = contentWrapper.innerHTML;
                const textSpan = document.createElement("span");
                textSpan.innerHTML = existingContent;
                textSpan.style.flex = "1";
                textSpan.style.minWidth = "0";

                contentWrapper.innerHTML = "";
                contentWrapper.appendChild(textSpan);

                btnContainer.style.flexShrink = "0";
                contentWrapper.appendChild(btnContainer);
              } else {
                cell.style.display = "flex";
                cell.style.justifyContent = "space-between";
                cell.style.alignItems = "center";
                cell.style.gap = "8px";

                // Wrap existing content in a span to control overflow
                const existingContent = cell.innerHTML;
                const textSpan = document.createElement("span");
                textSpan.innerHTML = existingContent;
                textSpan.style.flex = "1";
                textSpan.style.minWidth = "0";

                cell.innerHTML = "";
                cell.appendChild(textSpan);

                btnContainer.style.flexShrink = "0";
                cell.appendChild(btnContainer);
              }
            });
          }
        }
      };

      yasgui.on("queryResponse", () => {
        const tab = yasgui.getTab();
        if (tab) {
          const yasr = tab.getYasr();
          if (yasr) {
            yasr.on("drawn", handleDrawn);
            yasr.on("change", handleDrawn);
            handleDrawn();
          }
        }
      });

      const tab = yasgui.getTab();
      if (tab) {
        const yasr = tab.getYasr();
        if (yasr) {
          yasr.on("drawn", handleDrawn);
          yasr.on("change", handleDrawn);
          if (yasr.results) {
            handleDrawn();
          }
        }
      }
    }

    return () => {
      if (yasguiInstance.current) {
        yasguiInstance.current.destroy();
        yasguiInstance.current = null;
      }
    };
  }, [handleTermValue, isOpened]);

  return (
    <>
      <button
        onClick={() => setIsOpened(true)}
        style={{
          backgroundColor: "rgba(255, 255, 255, 0.9)",
          pointerEvents: "auto",
          border: "none",
          cursor: "pointer"
        }}
      >
        <img src={sparqlIcon} alt="SPARQL Query" style={{ height: "24px", display: "block" }} />
      </button>
      {isOpened && createPortal(
        <div className="sparql-query-overlay">
          <div className="sparql-query-container">
            <div className="sparql-query-header">
              <h3>SPARQL Query</h3>
              <button onClick={() => setIsOpened(false)} className="close-button">×</button>
            </div>
            <div ref={yasguiRef}/>
          </div>
        </div>, document.body
      )}
    </>
  );
};
