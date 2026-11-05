# iDispatch

iDispatch is a dispatching system for small fire and rescue organisations. A dispatcher records an assignment, picks
the units to send, and dispatches them. iDispatch alerts the units by SMS, on runboards at the stations, and in a
mobile app, keeps track of every unit's status, and collects a report of each assignment once it is over.

The user interface is in English, Swedish and Finnish.

## What's in the repository

The repository holds two products, each released on its own with its own version (see
[RELEASING.md](Java/RELEASING.md)):

| Product | Directory | What it is |
|---|---|---|
| **Server** | [`Java/server`](Java/server) | The dispatching application: a Spring Boot and [Vaadin](https://vaadin.com) Flow web app on Java 25, with PostgreSQL for storage and Keycloak for logging in. It also serves the units' mobile app. |
| **Runboard** | [`Java/runboard`](Java/runboard) | A full-screen Swing client for a station. It receives alerts from the server over a WebSocket, plays the alarm, reads the alert out loud with [Piper](https://github.com/OHF-Voice/piper1-gpl) text-to-speech, and can switch an amplifier and a flashing light through GPIO on a Raspberry Pi. |

The server is made of these modules:

- [`server/app`](Java/server/app): the web application.
- [`server/mobile`](Java/server/mobile): the units' mobile app, an installable web app (TypeScript, Lit and Vaadin's
  web components) that receives alerts, works offline and lets the unit set its own status. See its
  [README](Java/server/mobile/README.md).
- [`server/aspsms-integration`](Java/server/aspsms-integration): the generated SOAP client for sending SMS through
  ASPSMS.

### Features

- **Dispatching:** record assignments bit by bit, see which units are available, dispatch them, and follow every
  unit's status on the dispatcher's board. Several dispatchers can work at the same time.
- **Alerts:** to SMS recipients, runboards and mobile apps. Mobile apps also get alerts by Web Push when closed, and
  sound an alarm in vehicle mode.
- **Reports:** each unit and the scene are reported after the assignment, and reviewers close and re-open the
  reports.
- **Administration:** municipalities, assignment types, resources and alert destinations, with an audit trail of who
  changed what.

## Getting started

You need Java 25, Maven and Docker.

### Build and test

```shell
cd Java
mvn verify
```

The server's tests start PostgreSQL with Testcontainers, so Docker must be running. Add `-Passemble` to also build the
runboard's installation zip, `runboard/target/iDispatchRunboard-<version>.zip`.

### Run the server for development

```shell
cd Java/server
mvn -pl aspsms-integration,mobile -am install -DskipTests
mvn -pl app spring-boot:run
```

The first command installs the modules the app depends on into your local Maven repository; run it again after
changing them.

On start, the app brings up PostgreSQL and a Keycloak with a ready-made `idispatch` realm from
[`server/app/compose.yaml`](Java/server/app/compose.yaml). Open <http://localhost:8080/idispatch> and log in with one
of the development users:

| User | Password | Roles |
|---|---|---|
| `admin` | `changeme` | All |
| `dispatcher` | `dispatcher` | Dispatcher |
| `reports` | `reports` | Reads reports |
| `reporter` | `reporter` | Writes reports |
| `reviewer` | `reviewer` | Closes and re-opens reports |

Keycloak's admin console is at <http://localhost:8180> (`admin` / `admin`). The containers keep running after the app
stops; `docker compose stop` in `Java/server/app` stops them, and `docker compose down -v` also wipes the data.

### Run a runboard

```shell
cd Java/runboard
./run-local.sh
```

The settings live in the Git-ignored `runboard/local/` directory. See [speech.md](Java/runboard/docs/speech.md) for the
voices.

## Documentation

| Topic | Document |
|---|---|
| Deploying the server with Docker behind NGINX | [server/docs/deployment.md](Java/server/docs/deployment.md) |
| Setting up Keycloak, in development and production | [server/docs/keycloak.md](Java/server/docs/keycloak.md) |
| The mobile app | [server/mobile/README.md](Java/server/mobile/README.md) |
| Setting up a runboard on a Raspberry Pi | [runboard/docs/raspberry-pi.md](Java/runboard/docs/raspberry-pi.md) |
| The runboard's speech | [runboard/docs/speech.md](Java/runboard/docs/speech.md) |
| The runboard's amplifier and flashing light | [runboard/docs/outputs.md](Java/runboard/docs/outputs.md) |
| Versions and releases | [RELEASING.md](Java/RELEASING.md) |
| Architecture and conventions, for contributors | [CLAUDE.md](Java/CLAUDE.md) |

## Getting iDispatch

- **Server:** distributed as source. Check out an `idispatch-server/v<version>` tag and follow
  [deployment.md](Java/server/docs/deployment.md).
- **Runboard:** download `iDispatchRunboard-<version>.zip` from the
  [latest release](https://github.com/peholmst/idispatch-classic/releases/latest), unzip it on the runboard computer
  and start it with the launcher for your system. It needs Java 25.

## License

iDispatch is licensed under the GNU Affero General Public License, version 3 (AGPL-3.0). See [LICENSE](LICENSE).

The runboard bundles Piper text-to-speech (piper-jni, with piper1-gpl and espeak-ng), which is licensed under the GNU
General Public License, version 3. Section 13 of the AGPL-3.0 permits combining the two; the GPL-3.0 still applies to
those parts of the combined work.
