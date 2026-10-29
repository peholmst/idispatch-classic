# Running in Docker behind NGINX

`deploy/compose.yaml` runs iDispatch as two containers:

* `idispatch`, the server, built from `Dockerfile`. It gets its own address on the external `lan_apps` network and
  listens on port 8080 there, over plain HTTP.
* `postgres`, PostgreSQL 18, on a private `backend` network that only the server can reach. The data lives in the
  `postgres-data` volume.

Keycloak is not part of the deployment; the server logs in users through an existing Keycloak server. NGINX, also
outside the deployment, terminates TLS and forwards HTTP and WebSocket traffic to the server.

```
browser ── HTTPS ──▶ NGINX ── HTTP / WS ──▶ idispatch (IDISPATCH_IP:8080) ──▶ postgres
runboard ── WSS ───▶   │                           │
                       │                           └── HTTPS ──▶ Keycloak
browser ── HTTPS ──────┴─────────────────────────────────────▶ Keycloak
```

The examples below assume that iDispatch is served at `https://idispatch.example.com/idispatch/`, that Keycloak is
at `https://keycloak.example.com` with a realm named `idispatch`, and that the server container has the address
`192.168.1.50`.

## 1. Set up Keycloak

Create the realm, the `idispatch` client, its roles, the roles mapper and the users as described in
[keycloak.md](keycloak.md), "Production", steps 1–5. The client's URLs include the context path:

| Field | Value |
|---|---|
| Root URL | `https://idispatch.example.com/idispatch` |
| Home URL | `https://idispatch.example.com/idispatch` |
| Valid redirect URIs | `https://idispatch.example.com/idispatch/login/oauth2/code/keycloak` |
| Valid post logout redirect URIs | `https://idispatch.example.com/idispatch` |
| Web origins | `+` |

Copy the client secret from the client's **Credentials** tab.

## 2. Configure and start the containers

On the Docker host, in a checkout of the repository:

```shell
cd Java/server/deploy
cp .env.example .env
```

Fill in `.env`:

| Variable | Value |
|---|---|
| `IDISPATCH_IP` | An unused address in the `lan_apps` network, such as `192.168.1.50`. NGINX forwards to port 8080 on it. |
| `IDISPATCH_CONTEXT_PATH` | The path the application is served under, `/idispatch` by default. Set it empty (`IDISPATCH_CONTEXT_PATH=`) to serve from the root of the host name. It must be the same path the browser sees: NGINX passes paths through unchanged. |
| `TZ` | Time zone for times shown in the UI, such as `Europe/Helsinki`. |
| `POSTGRES_PASSWORD` | A long random password, for example from `openssl rand -base64 32`. Changing it later doesn't change the password of an existing database; use `ALTER USER` in `psql` for that. |
| `KEYCLOAK_ISSUER_URI` | The Keycloak base URL followed by `/realms/` and the realm name: `https://keycloak.example.com/realms/idispatch`. |
| `KEYCLOAK_CLIENT_ID` | `idispatch`, unless you named the client differently. |
| `KEYCLOAK_CLIENT_SECRET` | The client secret from step 1. |
| `ASPSMS_USER_KEY`, `ASPSMS_PASSWORD`, `ASPSMS_ORIGINATOR` | ASPSMS credentials for sending dispatch SMS. |
| `RUNBOARD_LANGUAGE` | The language runboards read notifications out loud in, as an ISO 639-1 code: `fi` (the default) or `sv`. Use the language that assignment types and municipalities are entered in. Each runboard needs a voice for it; see `runboard/docs/speech.md`. |

`.env` holds secrets: keep it readable only by the user that runs Docker (`chmod 600 .env`).

Any other Spring Boot property can be set by adding it to the `environment` of the `idispatch` service, written as an
environment variable (`server.servlet.session.timeout` becomes `SERVER_SERVLET_SESSION_TIMEOUT`).

Build and start:

```shell
docker compose up -d --build
docker compose ps
```

The server is healthy once it has migrated the database and read the realm's OpenID configuration from Keycloak.
**It doesn't start if it can't reach Keycloak**; check `docker compose logs idispatch` if it keeps restarting.

To upgrade, pull the new version and run `docker compose up -d --build` again. Flyway migrates the database when
the server starts. `docker compose down` keeps the database; `docker compose down -v` deletes it.

### Reaching Keycloak from the container

The server must reach Keycloak at the same URL as the browsers, `KEYCLOAK_ISSUER_URI`, because it checks that the
issuer in each token is the one it was configured with. From inside the container, that means:

* The Keycloak host name must resolve. If it's only in the LAN's DNS, make sure Docker uses that DNS server, or add
  `extra_hosts: ["keycloak.example.com:192.168.1.10"]` to the `idispatch` service.
* The address must be reachable. If `lan_apps` is a `macvlan` or `ipvlan` network, containers on it can't talk to
  the Docker host itself, so Keycloak (or the NGINX in front of it) can't run on the same host unless you've set up
  a macvlan interface for the host.
* The TLS certificate must be trusted. Certificates from a public CA work as such. For an internal CA, mount its
  certificate into the container and tell the image to trust it:

  ```yaml
  services:
    idispatch:
      environment:
        USE_SYSTEM_CA_CERTS: "1"
      volumes:
        - ./internal-ca.crt:/certificates/internal-ca.crt:ro
  ```

Test with `docker compose exec idispatch wget -qO- https://keycloak.example.com/realms/idispatch/.well-known/openid-configuration`
(this uses the system trust store, not the JVM's, but shows DNS and routing problems).

## 3. Set up NGINX

NGINX terminates TLS and forwards everything under the context path to the server. Two things on top of plain
proxying matter:

* **WebSockets.** Vaadin keeps a WebSocket open to each browser for server push (`/idispatch/VAADIN/push`), and
  runboards connect to `/idispatch/ws/runboard`. NGINX must pass the `Upgrade` and `Connection` headers, and must
  not close idle connections after its default 60 seconds.
* **Forwarded headers.** The server builds the redirect URI it sends to Keycloak from the request. It trusts the
  `X-Forwarded-Proto`, `X-Forwarded-Host` and `X-Forwarded-Port` headers (the compose file sets
  `SERVER_FORWARD_HEADERS_STRATEGY=framework`), so NGINX must set them; otherwise Keycloak rejects the login with
  *Invalid parameter: redirect_uri*.

Put this in, for example, `/etc/nginx/conf.d/idispatch.conf`:

```nginx
# Upgrade the connection only when the client asks for a WebSocket.
map $http_upgrade $connection_upgrade {
    default upgrade;
    ''      close;
}

upstream idispatch {
    server 192.168.1.50:8080;
}

server {
    listen 80;
    listen [::]:80;
    server_name idispatch.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    listen [::]:443 ssl;
    http2 on;
    server_name idispatch.example.com;

    ssl_certificate     /etc/letsencrypt/live/idispatch.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/idispatch.example.com/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;

    add_header Strict-Transport-Security "max-age=31536000" always;

    location = / {
        return 302 /idispatch/;
    }

    # The health endpoint is for Docker's health check, not for the internet.
    location /idispatch/actuator/ {
        return 404;
    }

    # No URI after the upstream name: the path is passed on unchanged, context path included.
    location /idispatch/ {
        proxy_pass http://idispatch;
        proxy_http_version 1.1;

        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host  $host;
        proxy_set_header X-Forwarded-Port  $server_port;

        proxy_set_header Upgrade    $http_upgrade;
        proxy_set_header Connection $connection_upgrade;

        # Push and runboard WebSockets stay open for hours. The runboard heartbeat is every 10 seconds, but a
        # dispatcher's push connection can be quiet for longer.
        proxy_read_timeout 1h;
        proxy_send_timeout 1h;
        # Pass push messages on as soon as they arrive, also when push falls back to long polling.
        proxy_buffering off;
    }
}
```

If `IDISPATCH_CONTEXT_PATH` is empty, drop the `location = /` block, and use `location /actuator/` and
`location /` instead.

Check and reload:

```shell
sudo nginx -t && sudo systemctl reload nginx
```

## 4. Point the runboards at the server

Build the runboard package with `mvn -pl runboard -Passemble package` in this directory. It is
`runboard/target/iDispatchRunboard-<version>.zip`, for Windows, macOS and Linux, and needs Java 25 on the runboard
computer. The `README.txt` inside it explains how to install it.

In each runboard's `runboard.properties`, use `wss://` and the context path:

```properties
idispatch.server.url=wss://idispatch.example.com/idispatch/ws/runboard
```

To have notifications read out loud, also give each runboard a voice for `RUNBOARD_LANGUAGE`, for example
`idispatch.speech.piper.voice.fi`. The package's `download-voices` script downloads the Finnish and Swedish voices
that its `runboard.properties` refers to. See `runboard/docs/speech.md`. Without a voice, the runboard only plays the
alarm.

## 5. Check that it works

1. Open `https://idispatch.example.com/`. The browser goes to `/idispatch/` and from there to the Keycloak login
   page.
2. Log in as a user with the `DISPATCHER` role and open the dispatcher view. In the browser's developer tools,
   the **Network** tab shows a WebSocket request to `/idispatch/VAADIN/push` with status 101.
3. Start a runboard. `docker compose logs idispatch` shows *Runboard … connected from …*.
4. Log out. The browser returns to the Keycloak login page.

See [keycloak.md](keycloak.md), "Troubleshooting", for login problems. Problems specific to this setup:

| Symptom | Cause |
|---|---|
| Keycloak shows *Invalid parameter: redirect_uri*, and the URI it was given starts with `http://` or contains `:8080` | NGINX doesn't set the `X-Forwarded-*` headers, or the container doesn't have `SERVER_FORWARD_HEADERS_STRATEGY=framework`. |
| Pages load, but the UI says it lost the connection, or updates from other dispatchers arrive only after a click | Push can't use a WebSocket: the `Upgrade` and `Connection` headers are missing, or something between the browser and NGINX drops WebSockets. |
| Runboards disconnect every 60 seconds | `proxy_read_timeout` isn't set in the `location` that serves `/idispatch/ws/`. |
| 404 for everything, or redirect loops | `IDISPATCH_CONTEXT_PATH` doesn't match the path in the NGINX `location`, or `proxy_pass` has a URI after the upstream name, which makes NGINX rewrite the path. |
| The server stays in `health: starting` and uses all the CPU | It fails to start and Docker restarts it at once, again and again: each restart starts a new JVM and resets the health status. `docker compose logs idispatch` shows why it fails, usually one of the errors below. |
| The server restarts over and over, and the log says *Unable to resolve the Configuration with the provided Issuer* | The container can't reach Keycloak at `KEYCLOAK_ISSUER_URI`: check the URL for typos, and see "Reaching Keycloak from the container". |
| The log says *PKIX path building failed* | The JVM doesn't trust Keycloak's certificate; see "Reaching Keycloak from the container". |
