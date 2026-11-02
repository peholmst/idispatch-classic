# iDispatch mobile app

The mobile app of a unit: an installable web app, written in TypeScript with [Lit](https://lit.dev) and Vaadin's web
components in the Aura theme, and built with [Vite](https://vite.dev). The server serves it under
`/idispatch/mobile/` and its API under `/idispatch/api/mobile/`.

An administrator pairs a device by adding a mobile destination for the unit and scanning its QR code. The code holds
the pairing link `…/mobile/#pair=<key>`; the app stores the key in IndexedDB, removes it from the address, and sends it
with every API call in an `Authorization: Bearer` header. The server stores only the key's hash.

The app keeps the event stream `…/api/mobile/events` open, which tells it the unit and every change of its status,
whoever made it, and every dispatch notification for the unit. The server sends a notification again on every connect
until the app confirms it, which the app does once it has stored it in IndexedDB; an alert stays there until the user
deletes it. It reads the stream with `fetch`, since `EventSource` can't send the key in a header, and connects
again whenever the stream ends, ever more slowly while the server can't be reached. The status buttons work only while
the stream is open: a change is never queued to be sent later. The state colors are the dispatcher's, imported from the
server's `styles/dispatch-view-styles.css`.

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
