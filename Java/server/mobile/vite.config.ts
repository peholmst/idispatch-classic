/// <reference types="vitest/config" />
import { defineConfig } from 'vite';

// The server serves the app under <context path>/mobile/ and the API under <context path>/api/mobile/. Relative paths
// keep the app working under any context path.
export default defineConfig({
  base: './',
  build: {
    outDir: 'target/classes/META-INF/resources/mobile',
    emptyOutDir: true,
  },
  server: {
    // The app imports the resource state colors from the server's stylesheets
    fs: { allow: ['..'] },
    // `npm run dev` serves the app at http://localhost:5173/ and passes the API, which the app finds at ../api/, on to
    // the server that the dev loop runs
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        rewrite: (path) => '/idispatch' + path,
      },
    },
  },
  test: {
    environment: 'node',
  },
});
