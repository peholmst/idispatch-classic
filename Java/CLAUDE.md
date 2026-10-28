# iDispatch Classic (Java)

Maven reactor: `server` (Spring Boot 4.1 + Vaadin 25.3 Flow app, Java 25), `aspsms-integration` (generated SOAP client used by `server`), `runboard` (standalone Swing client that receives dispatch notifications over the server's `/ws/runboard` WebSocket).

## Running and testing

- Run the app with the Vaadin dev loop, not `mvn spring-boot:run`: `server/.vaadin/vaadin-dev start`, then `apply` after edits. See the `vaadin-devloop` skill in `server/.claude/skills/`.
- App: http://localhost:8080/idispatch, initial user `admin` / `changeme`.
- Database is PostgreSQL from `server/compose.yaml`. Spring Boot's Docker Compose support starts it with the app and supplies the datasource; data persists in the `postgres-data` volume (`docker compose down -v` in `server/` wipes it).
- Schema changes go in a new Flyway migration in `server/src/main/resources/db/migration` (never edit an applied one). Hibernate runs with `ddl-auto: validate`, so the app fails to start if the entities and the schema disagree.
- Tests: `mvn test` in `server/`; needs Docker, as `TestcontainersConfiguration` starts a PostgreSQL container per Spring context. Running Maven while the dev loop owns the app rewrites `target/classes` under it; run `.vaadin/vaadin-dev restart` afterwards.

## Conventions

- Roles are in `security/Roles`. Views use `@RolesAllowed`/`@PermitAll`, and layouts need an access annotation too. Services use `@Secured`; Spring Security reads it from the class that declares a method, so methods inherited from `AbstractManagementServiceBean` are secured there.
- Management console screens extend `AbstractManagementView` and `AbstractManagementDialog`. Filtering goes through `DefaultFilter` and a `*Specifications` class per entity.
- Services publish Spring application events (`events/`) synchronously. A view that refreshes on an event must go through `OwnerUpdater`, which locks the right session and runs as the view's owner. Server push is enabled.
- Theme is Lumo (dark), loaded with `@StyleSheet` on `AppShellConfiguratorImpl`. Style Grid cells with part name generators and `::part()`, not `themeFor`.
- Use the Vaadin docs MCP server for Vaadin 25.3 APIs rather than recalled Vaadin 24 knowledge.

## Tests

- Browserless UI tests extend `AbstractUITest` (`SpringBrowserlessTest`, full context, `@WithMockUser`). Services use `REQUIRES_NEW` transactions, so nothing rolls back: create fixtures with `TestData` and unique names.
- Component queries only find visible components, and exceptions from click listeners reach the test instead of the session error handler.
- Multi-user tests (`MultiDispatcherTest`) hold every session's lock, so they cannot catch unlocked cross-session UI access (vaadin/browserless-test#295).
