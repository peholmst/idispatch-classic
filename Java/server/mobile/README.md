# iDispatch mobile app

The mobile app of a unit: an installable web app, written in TypeScript with [Lit](https://lit.dev) and Vaadin's web
components in the Aura theme, and built with [Vite](https://vite.dev). The server serves it under
`/idispatch/mobile/` and its API under `/idispatch/api/mobile/`.

An administrator pairs a device by adding a mobile destination for the unit and scanning its QR code. The code holds
the pairing link `…/mobile/#pair=<key>`; the app stores the key in IndexedDB, removes it from the address, and sends it
with every API call in an `Authorization: Bearer` header. The server stores only the key's hash.

## Building

This directory is a Maven module of the server (`idispatch-mobile`). `mvn package` installs Node into `target/`, runs
`npm ci`, type-checks and builds the app into `target/classes/META-INF/resources/mobile`, and runs the tests with
Vitest (skipped with `-DskipTests`). The server depends on the module, so its jar and Docker image include the app.
Nothing needs to be installed beforehand.

## Working on the app

With Node installed, and the server running in the dev loop (`../app/.vaadin/vaadin-dev start`):

```shell
npm install
npm run dev    # http://localhost:5173/, passes the API on to the server at localhost:8080
npm test       # Vitest
npm run build  # what Maven runs
```

To pair the dev server with a destination, open `http://localhost:5173/#pair=<key>`, with the key from the destination's
pairing link.
