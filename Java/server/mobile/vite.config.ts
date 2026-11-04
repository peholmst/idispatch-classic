/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import { VitePWA } from 'vite-plugin-pwa';

/** The dark background of Aura, which the app shows, so that opening the app doesn't flash white. */
const BACKGROUND = '#1a1d23';

// The server serves the app under <context path>/mobile/ and the API under <context path>/api/mobile/. Relative paths
// keep the app working under any context path.
export default defineConfig({
  base: './',
  build: {
    outDir: 'target/classes/META-INF/resources/mobile',
    emptyOutDir: true,
  },
  plugins: [
    VitePWA({
      // The app registers the service worker itself, and decides when to reload into a new version (src/updates.ts)
      registerType: 'autoUpdate',
      injectRegister: false,
      // The icons are in the app shell's files already
      includeManifestIcons: false,
      manifest: {
        name: 'iDispatch',
        short_name: 'iDispatch',
        description: 'Yksikön hälytykset ja tila – Enhetens larm och status',
        lang: 'fi',
        // Relative to the manifest, so that the installed app opens where it is served and shares the browser tab's
        // storage, and with it the pairing
        start_url: './',
        scope: './',
        display: 'standalone',
        background_color: BACKGROUND,
        theme_color: BACKGROUND,
        icons: [
          { src: 'icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'icon-512.png', sizes: '512x512', type: 'image/png' },
          { src: 'icon-maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        ],
      },
      // The service worker is the app's own (src/sw.ts), as it also receives the alerts by Web Push; Workbox puts the
      // list of files of the app shell in it
      strategies: 'injectManifest',
      srcDir: 'src',
      filename: 'sw.ts',
      injectManifest: {
        // The app shell: the page, its script, styles and font, and the icons
        globPatterns: ['**/*.{html,js,css,woff2,png,svg}'],
      },
    }),
  ],
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
