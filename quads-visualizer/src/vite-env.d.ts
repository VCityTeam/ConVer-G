/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_QUERY_ENDPOINT: string
  readonly VITE_LOADER_ENDPOINT: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

