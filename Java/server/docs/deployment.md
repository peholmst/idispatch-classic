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

On the Docker host, check out the release to run. Releases are tagged `idispatch-server/v<version>`; the
[releases page](https://github.com/peholmst/idispatch-classic/releases) lists them, with what changed and which
runboard versions they work with. Use a tag rather than `main`, which holds unreleased work.

```shell
git clone https://github.com/peholmst/idispatch-classic.git
cd idispatch-classic
git checkout idispatch-server/v3.0.0
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
| `POSTGRES_VOLUME` | The Docker volume that holds the database, such as `idispatch-camp-a`. Each camp gets its own; see "Switching between camps". |
| `KEYCLOAK_ISSUER_URI` | The Keycloak base URL followed by `/realms/` and the realm name: `https://keycloak.example.com/realms/idispatch`. |
| `KEYCLOAK_CLIENT_ID` | `idispatch`, unless you named the client differently. |
| `KEYCLOAK_CLIENT_SECRET` | The client secret from step 1. |
| `ASPSMS_USER_KEY`, `ASPSMS_PASSWORD`, `ASPSMS_ORIGINATOR` | ASPSMS credentials for sending dispatch SMS. |
| `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY` | The server's key pair for Web Push to the mobile apps; see "Web Push for the mobile apps" below. Leave them out and the mobile apps get alerts only while they are open. |
| `VAPID_SUBJECT` | Who runs the server, as `mailto:` and an address (`mailto:admin@example.com`) or an `https://` URL. Push services use it to get in touch if the server's pushes cause trouble. Optional, but some push services expect it. |

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
the server starts. `docker compose down` keeps the database; `docker compose down -v` deletes the database of the
current camp.

### Switching between camps

Each camp has its own database, in its own Docker volume. `POSTGRES_VOLUME` in `.env` chooses which one the server
uses; everything else (Keycloak, NGINX, the server's address, the VAPID keys) stays the same. To switch:

```shell
docker compose down
sed -i 's/^POSTGRES_VOLUME=.*/POSTGRES_VOLUME=idispatch-camp-b/' .env
docker compose up -d
```

Use `down` without `-v`, which would delete the current camp's database. Stop both containers, not only `postgres`:
the server keeps database connections and state in memory. `docker volume ls` lists the camps' volumes.

A volume that doesn't exist yet is created empty, and the server sets up the database in it when it starts. Only
then, when a volume is created, does PostgreSQL take the password from `POSTGRES_PASSWORD`, so keep the same password
for every camp.

What is in the database belongs to one camp: destinations, units, assignments, and the runboards and phones paired
with destinations. A phone paired during one camp gets no alerts during another, until it is paired with a
destination in that camp too. The users are in Keycloak and are the same for every camp.

Deployments from before `POSTGRES_VOLUME` keep their database in a volume called `deploy_postgres-data` (Compose
puts the project name, the folder name, in front). Either set `POSTGRES_VOLUME=deploy_postgres-data`, or, with the
containers stopped, copy it to a volume named after the camp:

```shell
docker run --rm -v deploy_postgres-data:/from -v idispatch-camp-a:/to alpine cp -a /from/. /to/
```

### Web Push for the mobile apps

The mobile app gets alerts over a live connection while it is open. To get them also when it is in the background or
closed, as a notification on the phone, the server sends them by Web Push, through the push service of the phone's
browser (Google's, for Chrome on Android). No Firebase project or other account is needed: the server identifies
itself to the push services with a key pair of its own (VAPID). Make one with OpenSSL, on any computer:

```shell
openssl ecparam -name prime256v1 -genkey -noout -out vapid.pem
echo "VAPID_PUBLIC_KEY=$(openssl ec -in vapid.pem -pubout -outform DER | tail -c 65 | base64 | tr '/+' '_-' | tr -d '=\n')"
echo "VAPID_PRIVATE_KEY=$(openssl ec -in vapid.pem -outform DER | tail -c +8 | head -c 32 | base64 | tr '/+' '_-' | tr -d '=\n')"
rm vapid.pem
```

Put the two lines it prints in `.env`, with `VAPID_SUBJECT`. Keys made with `npx web-push generate-vapid-keys` work as
well. The private key is a secret like the others in `.env`. The server refuses to start if only one of the keys is
set, or if they are not a pair; without either, it starts and logs a warning that Web Push is off.

Keep the same keys from then on. The apps subscribe with the public key, so with new keys the server's pushes don't
reach them until each app has been opened once, which subscribes it again.

Each app asks for permission to show notifications ("Salli ilmoitukset" / "Tillåt aviseringar"), and once it has it,
gives the server its subscription. Giving a mobile destination a new key removes its subscription, so a lost phone
gets no more alerts. The server must reach the push services, such as `https://fcm.googleapis.com`, over HTTPS:
if outgoing traffic is limited, allow it. A push that a push service doesn't deliver within an hour is dropped
(`IDISPATCH_MOBILE_PUSH_TIME_TO_LIVE`, such as `30m`); the app gets the alert when it next opens.

Web notifications can't play the app's alarm sound or sound in silent mode. For that, the app must be open, with the
on-call duty started.

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

Download the runboard package, `iDispatchRunboard-<version>.zip`, from the
[releases page](https://github.com/peholmst/idispatch-classic/releases) (the `idispatch-runboard/v<version>` releases).
It is for Windows, macOS and Linux, and needs Java 25 on the runboard computer. The `README.txt` inside it explains how
to install it.

In each runboard's `runboard.properties`, use `wss://` and the context path:

```properties
idispatch.server.url=wss://idispatch.example.com/idispatch/ws/runboard
```

Each destination, runboard or SMS, has a language (Finnish or Swedish) set under Administration → Destinations.
Notifications are sent in it: municipality names and assignment type descriptions are in that language when they
have been entered in it, and in the other language otherwise. To have notifications read out loud, also give each
runboard a voice for its language, for example `idispatch.speech.piper.voice.fi`. The package's `download-voices` script downloads the Finnish and Swedish voices
that its `runboard.properties` refers to. See `runboard/docs/speech.md`. Without a voice, the runboard only plays the
alarm.

## 5. Check that it works

1. Open `https://idispatch.example.com/`. The browser goes to `/idispatch/` and from there to the Keycloak login
   page.
2. Log in as a user with the `DISPATCHER` role and open the dispatcher view. In the browser's developer tools,
   the **Network** tab shows a WebSocket request to `/idispatch/VAADIN/push` with status 101.
3. Start a runboard. `docker compose logs idispatch` shows *Runboard … connected from …*.
4. Pair a phone with a mobile destination, allow notifications in the app, close the app and dispatch its unit. The
   phone shows the alert as a notification, and with `LOGGING_LEVEL_NET_PKHAPPS: debug` the log says *Pushed
   notification … to mobile destination …*.
5. Log out. The browser returns to the Keycloak login page.

See [keycloak.md](keycloak.md), "Troubleshooting", for login problems. Problems specific to this setup:

| Symptom | Cause |
|---|---|
| Keycloak shows *Invalid parameter: redirect_uri*, and the URI it was given starts with `http://` or contains `:8080` | NGINX doesn't set the `X-Forwarded-*` headers, or the container doesn't have `SERVER_FORWARD_HEADERS_STRATEGY=framework`. |
| Pages load, but the UI says it lost the connection, or updates from other dispatchers arrive only after a click | Push can't use a WebSocket: the `Upgrade` and `Connection` headers are missing, or something between the browser and NGINX drops WebSockets. |
| Runboards disconnect every 60 seconds | `proxy_read_timeout` isn't set in the `location` that serves `/idispatch/ws/`. |
| 404 for everything, or redirect loops | `IDISPATCH_CONTEXT_PATH` doesn't match the path in the NGINX `location`, or `proxy_pass` has a URI after the upstream name, which makes NGINX rewrite the path. |
| The server stays in `health: starting` and uses all the CPU | It fails to start and Docker restarts it at once, again and again: each restart starts a new JVM and resets the health status. `docker compose logs idispatch` shows why it fails, usually one of the errors below. |
| The server restarts over and over, and the log says *Unable to resolve the Configuration with the provided Issuer* | The container can't reach Keycloak at `KEYCLOAK_ISSUER_URI`: check the URL for typos, and see "Reaching Keycloak from the container". |
| The log says *Web Push is off* | `VAPID_PUBLIC_KEY` and `VAPID_PRIVATE_KEY` are not set; see "Web Push for the mobile apps". |
| The log says *Could not reach the push service* | The server can't make outgoing HTTPS connections to the push services. |
| The log says *PKIX path building failed* | The JVM doesn't trust Keycloak's certificate; see "Reaching Keycloak from the container". |

## Upgrading

Read the release notes of every release between the one you run and the new one, then, in the checkout:

```shell
git fetch --tags
git checkout idispatch-server/v<version>
cd Java/server/deploy
docker compose up -d --build
```

Compose rebuilds the server image from the checked-out sources and replaces the container; the database stays in
its volume. When upgrading from a release without `POSTGRES_VOLUME`, add it to `.env` first, as described in
"Switching between camps"; `docker compose up` refuses to start without it and leaves the running containers alone. The server migrates the database to the new version with Flyway when it starts, so going back to an older
release afterwards is not supported: back up the database first, for example with
`docker compose exec postgres pg_dump -U idispatch idispatch > idispatch-backup.sql`.

Only the current camp's database is migrated. The others are migrated the next time you switch to them, so back
them up before that, or switch to each one in turn right after the upgrade, backing it up first.

The version that runs is shown next to the name in the drawer.
