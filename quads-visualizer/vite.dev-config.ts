import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  base: '/RDF-vers/',
  server: {
    proxy: {
      '/import': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/rdf': {
        target: 'https://quaque-reasoning.ud-evolution.pagoda.liris.cnrs.fr',
        changeOrigin: true,
        secure: true,
      },
    }
  }
})
