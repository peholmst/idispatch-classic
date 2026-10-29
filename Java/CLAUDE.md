# iDispatch Classic (Java)

Maven reactor: `server` (Spring Boot 4.1 + Vaadin 25.3 Flow app, Java 25), `aspsms-integration` (generated SOAP client used by `server`), `runboard` (standalone Swing client that receives dispatch notifications over the server's `/ws/runboard` WebSocket).

## Running and testing

- Run the app with the Vaadin dev loop, not `mvn spring-boot:run`: `server/.vaadin/vaadin-dev start`, then `apply` after edits. See the `vaadin-devloop` skill in `server/.claude/skills/`.
- App: http://localhost:8080/idispatch. Login is OIDC through Keycloak; dev users `admin` / `changeme` (all roles), `dispatcher` / `dispatcher`, `reports` / `reports`.
- Database is PostgreSQL from `server/compose.yaml`. Spring Boot's Docker Compose support starts it with the app and supplies the datasource; data persists in the `postgres-data` volume (`docker compose down -v` in `server/` wipes it).
- Keycloak (http://localhost:8180, admin console `admin` / `admin`) is in the same `compose.yaml` and imports the `idispatch` realm from `server/keycloak/idispatch-realm.json` on every start, keeping no other state. Spring Boot skips `docker compose up` if any service is already running, so start a missing one with `docker compose up -d --wait keycloak`. The app fails to start if Keycloak is down. Production setup: `server/docs/keycloak.md`.
- Schema changes go in a new Flyway migration in `server/src/main/resources/db/migration` (never edit an applied one). Hibernate runs with `ddl-auto: validate`, so the app fails to start if the entities and the schema disagree.
- Production runs in Docker behind NGINX: `server/Dockerfile` (build context is this directory) and `server/deploy/compose.yaml`, documented in `server/docs/deployment.md`.
- Run just the runboard against any server, such as production, with `runboard/run-local.sh` (settings in the Git-ignored `runboard/local/`; see `runboard/docs/speech.md`).
- Tests: `mvn test` in `server/`; needs Docker, as `TestcontainersConfiguration` starts a PostgreSQL container per Spring context. Running Maven while the dev loop owns the app rewrites `target/classes` under it; run `.vaadin/vaadin-dev restart` afterwards.

## Conventions

- Users and roles live only in Keycloak. Roles are the `idispatch` client roles `ADMIN`, `DISPATCHER`, `REPORT_READER`, read from the ID token's `roles` claim by `security/KeycloakRolesMapper` and mapped to the constants in `security/Roles`. Views use `@RolesAllowed`/`@PermitAll`, and layouts need an access annotation too. Services use `@Secured`; Spring Security reads it from the class that declares a method, so methods inherited from `AbstractManagementServiceBean` are secured there.
- Management console screens extend `AbstractManagementView` and `AbstractManagementDialog`. Filtering goes through `DefaultFilter` and a `*Specifications` class per entity.
- Services publish Spring application events (`events/`) synchronously. A view that refreshes on an event must go through `OwnerUpdater`, which locks the right session and runs as the view's owner. Server push is enabled.
- Theme is Lumo (dark), loaded with `@StyleSheet` on `AppShellConfiguratorImpl`. Style Grid cells with part name generators and `::part()`, not `themeFor`.
- Use the Vaadin docs MCP server for Vaadin 25.3 APIs rather than recalled Vaadin 24 knowledge.
- The runboard reads notifications out loud through the speech engine SPI in `runboard/.../speech` (Piper is the only engine). Engine code stays behind that SPI; see `runboard/docs/speech.md`.

## Tests

- Spring Boot tests import `TestSecurityConfiguration`, which replaces the Keycloak client registration so the context starts without Keycloak; tests authenticate with `@WithMockUser`.
- Browserless UI tests extend `AbstractUITest` (`SpringBrowserlessTest`, full context, `@WithMockUser`). Services use `REQUIRES_NEW` transactions, so nothing rolls back: create fixtures with `TestData` and unique names.
- Component queries only find visible components, and exceptions from click listeners reach the test instead of the session error handler.
- Multi-user tests (`MultiDispatcherTest`) hold every session's lock, so they cannot catch unlocked cross-session UI access (vaadin/browserless-test#295).
