import { createAsyncThunk } from "@reduxjs/toolkit";
import { setSelectedMetagraphNode, setSelectedMetagraphNodeType } from "./metagraphSlice";
import { setSelectedGraph, setSelectedVersion } from "./versionedGraphSlice";
import type { MetagraphNodeType } from "./metagraphSlice";

export const navigateToMetagraphNode = createAsyncThunk(
  "navigation/navigateToMetagraphNode",
  (
    payload: {
      nodeKey: string;
      nodeType: MetagraphNodeType;
      graph?: string;
      version?: string;
    },
    { dispatch }
  ) => {
    const { nodeKey, nodeType, graph, version } = payload;

    dispatch(setSelectedMetagraphNodeType(nodeType));
    dispatch(setSelectedMetagraphNode(nodeKey));

    if (graph) dispatch(setSelectedGraph(graph));
    if (version) dispatch(setSelectedVersion(version));
  }
);
