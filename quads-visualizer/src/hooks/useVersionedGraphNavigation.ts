import { useCallback, useEffect } from "react";
import { useAppDispatch } from "../state/hooks";
import { setSelectedGraph, setSelectedVersion } from "../state/versionedGraphSlice";

export const useVersionedGraphNavigation = (
  distinctGraph: string[],
  distinctVersion: string[],
  selectedGraph: string,
  selectedVersion: string,
) => {
  const dispatch = useAppDispatch();

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === "ArrowLeft") {
        const currentIndex = distinctVersion.indexOf(selectedVersion);
        if (currentIndex > 0) {
          dispatch(setSelectedVersion(distinctVersion[currentIndex - 1]));
        }
      } else if (e.key === "ArrowRight") {
        const currentIndex = distinctVersion.indexOf(selectedVersion);
        if (currentIndex < distinctVersion.length - 1) {
          dispatch(setSelectedVersion(distinctVersion[currentIndex + 1]));
        }
      } else if (e.key === "ArrowUp") {
        const currentIndex = distinctGraph.indexOf(selectedGraph);
        if (currentIndex > 0) {
          dispatch(setSelectedGraph(distinctGraph[currentIndex - 1]));
        }
      } else if (e.key === "ArrowDown") {
        const currentIndex = distinctGraph.indexOf(selectedGraph);
        if (currentIndex < distinctGraph.length - 1) {
          dispatch(setSelectedGraph(distinctGraph[currentIndex + 1]));
        }
      }
    },
    [dispatch, distinctGraph, distinctVersion, selectedGraph, selectedVersion],
  );

  useEffect(() => {
    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [handleKeyDown]);
};
