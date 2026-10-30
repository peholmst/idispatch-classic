# iDispatch Classic (Java)

Maven reactor: `server` (Spring Boot 4.1 + Vaadin 25.3 Flow app, Java 25), `aspsms-integration` (generated SOAP client used by `server`), `runboard` (standalone Swing client that receives dispatch notifications over the server's `/ws/runboard` WebSocket).

## Running and testing

- Run the app with the Vaadin dev loop, not `mvn spring-boot:run`: `server/.vaadin/vaadin-dev start`, then `apply` after edits. See the `vaadin-devloop` skill in `server/.claude/skills/`.
- App: http://localhost:8080/idispatch. Login is OIDC through Keycloak; dev users `admin` / `changeme` (all roles), `dispatcher` / `dispatcher`, `reports` / `reports` (reads reports), `reporter` / `reporter` (writes reports), `reviewer` / `reviewer` (closes and re-opens reports).
- Database is PostgreSQL from `server/compose.yaml`. Spring Boot's Docker Compose support starts it with the app and supplies the datasource; data persists in the `postgres-data` volume (`docker compose down -v` in `server/` wipes it).
- Keycloak (http://localhost:8180, admin console `admin` / `admin`) is in the same `compose.yaml` and imports the `idispatch` realm from `server/keycloak/idispatch-realm.json` when its container is created. Its data lives in the container and the import skips an existing realm, so a restart keeps the old realm: after editing the JSON file, run `docker compose up -d --force-recreate --wait keycloak` in `server/`, then log out of the app and back in, since roles are read at login. The app runs `docker compose up` on every start, bringing up whichever of PostgreSQL and Keycloak is missing, and leaves both running when it stops; `docker compose stop` in `server/` stops them. The app fails to start if Keycloak is down. Production setup: `server/docs/keycloak.md`.
- Schema changes go in a new Flyway migration in `server/src/main/resources/db/migration` (never edit an applied one). Hibernate runs with `ddl-auto: validate`, so the app fails to start if the entities and the schema disagree.
- Production runs in Docker behind NGINX: `server/Dockerfile` (build context is this directory) and `server/deploy/compose.yaml`, documented in `server/docs/deployment.md`.
- `mvn -pl runboard -Passemble package` builds the runboard's installation zip, `runboard/target/iDispatchRunboard-<version>.zip`: the fat jar, launchers and voice download scripts for Windows (`.cmd`) and macOS/Linux (`.command`) from `runboard/src/main/dist`, and license texts. It needs Java 25 preinstalled.
- Run just the runboard against any server, such as production, with `runboard/run-local.sh` (settings in the Git-ignored `runboard/local/`; see `runboard/docs/speech.md`). Setting it up on a Raspberry Pi: `runboard/docs/raspberry-pi.md`.
- Tests: `mvn test` in `server/`; needs Docker, as `TestcontainersConfiguration` starts a PostgreSQL container per Spring context. Running Maven while the dev loop owns the app rewrites `target/classes` under it; run `.vaadin/vaadin-dev restart` afterwards.

## Conventions

- Users and roles live only in Keycloak. Roles are the `idispatch` client roles `ADMIN`, `DISPATCHER`, `REPORT_READER`, `REPORT_WRITER`, `REPORT_REVIEWER`, read from the ID token's `roles` claim by `security/KeycloakRolesMapper` and mapped to the constants in `security/Roles`. Views use `@RolesAllowed`/`@PermitAll`, and layouts need an access annotation too. Services use `@Secured`; Spring Security reads it from the class that declares a method, so methods inherited from `AbstractManagementServiceBean` are secured there.
- The UI is in English, Swedish and Finnish. Texts come from `getTranslation(key)` (or `I18NProvider.translate` outside components) and live in `src/main/resources/vaadin-i18n/translations*.properties`; `translations.properties` is English, and every key must also be in `_sv` and `_fi`. Bean validation messages are `{idispatch.…}` keys in `ValidationMessages*.properties`, shown through `ValidationFailedException.getViolationMessages(locale)`. The language comes from the ID token's `locale` claim (`ui/UserLocale`), set on each UI when it is created; English is the fallback.
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
