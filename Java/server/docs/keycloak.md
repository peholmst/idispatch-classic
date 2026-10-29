# Logging in with Keycloak

iDispatch has no users of its own. People log in through [Keycloak](https://www.keycloak.org/) using OpenID
Connect. Keycloak also decides what each person may do: the application reads its roles from the ID token, and
nothing about users or roles is stored in the iDispatch database.

| Keycloak client role | Gives access to |
|---|---|
| `DISPATCHER` | The dispatcher workstation (open and dispatch assignments, change resource statuses) |
| `REPORT_READER` | Reports of closed assignments |
| `ADMIN` | The management console (municipalities, assignment types, resources, resource types, destinations) |

A user can have any combination of these roles. A user with none of them can log in, but only sees the home page.
Role names are case sensitive, and the application ignores roles it doesn't recognize.

Users manage their own password, and other account details, in the Keycloak account console. The **My Account**
link in the application's menu opens it.

## Development

`compose.yaml` runs Keycloak next to PostgreSQL. Spring Boot's Docker Compose support starts both when the
application starts, and waits until Keycloak has imported the realm.

* Keycloak: <http://localhost:8180>, admin console at <http://localhost:8180/admin> (`admin` / `admin`)
* Realm `idispatch`, imported from `keycloak/idispatch-realm.json` every time the container starts. Keycloak
  doesn't persist anything, so changes made in the admin console are lost when the container is recreated. To
  keep a change, put it in the JSON file.
* Users:

  | Username | Password | Roles |
  |---|---|---|
  | `admin` | `changeme` | `ADMIN`, `DISPATCHER`, `REPORT_READER` |
  | `dispatcher` | `dispatcher` | `DISPATCHER` |
  | `reports` | `reports` | `REPORT_READER` |

Spring Boot skips `docker compose up` entirely if *any* of the services is already running. If PostgreSQL was
running before Keycloak was added to `compose.yaml`, start Keycloak once by hand:

```shell
docker compose up -d --wait keycloak
```

The development client settings are the defaults in `src/main/resources/application.yml`.

## Production

In production, iDispatch uses an existing Keycloak server. You need:

* Keycloak 26 or later, reachable over HTTPS both from the users' browsers and from the iDispatch server, **at the
  same URL**. The application checks that the issuer in each token is the issuer it was configured with.
* An administrator account in Keycloak that can create realms and clients, or manage an existing realm.

### 1. Create the realm

You can use an existing realm, or create one for iDispatch:

1. In the admin console, open the realm drop-down at the top left and select **Create realm**.
2. Set **Realm name** to `idispatch` (any name works, but it becomes part of the issuer URI) and select **Create**.
3. Under **Realm settings → General**, set **Display name** to `iDispatch`. Users see it on the login page.

Configure password policies, brute-force detection, two-factor authentication, identity brokering and so on as
your organization requires. iDispatch doesn't depend on any of it.

### 2. Create the client

1. Go to **Clients → Create client**.
2. **General settings**: **Client type** `OpenID Connect`, **Client ID** `idispatch`, **Name** `iDispatch`.
   Select **Next**.
3. **Capability config**: turn **Client authentication** on. Under **Authentication flow**, keep only
   **Standard flow** checked. Select **Next**.
4. **Login settings**, assuming iDispatch is served at `https://idispatch.example.com`:

   | Field | Value |
   |---|---|
   | Root URL | `https://idispatch.example.com` |
   | Home URL | `https://idispatch.example.com` |
   | Valid redirect URIs | `https://idispatch.example.com/login/oauth2/code/keycloak` |
   | Valid post logout redirect URIs | `https://idispatch.example.com` |
   | Web origins | `+` |

   If iDispatch runs under a context path, such as `/idispatch`, add it to all of these URLs:
   `https://example.com/idispatch/login/oauth2/code/keycloak` and so on. The post logout redirect URI is the
   application's base URL exactly, without a trailing slash.
5. Select **Save**.
6. Open the **Credentials** tab and copy the **Client secret**. The application needs it in step 6.

### 3. Create the roles

On the `idispatch` client, open the **Roles** tab and create three roles, with exactly these names:

* `ADMIN`
* `DISPATCHER`
* `REPORT_READER`

These are *client* roles, not realm roles. The application only reads roles of the `idispatch` client.

### 4. Put the roles in the ID token

By default, Keycloak puts client roles only in the access token. The application reads them from the ID token,
from a claim named `roles`:

1. On the `idispatch` client, open the **Client scopes** tab and select **idispatch-dedicated**.
2. Select **Configure a new mapper** (or **Add mapper → By configuration**), then **User Client Role**.
3. Fill in:

   | Field | Value |
   |---|---|
   | Name | `application roles` |
   | Client ID | `idispatch` |
   | Client Role prefix | *(empty)* |
   | Multivalued | On |
   | Token Claim Name | `roles` |
   | Claim JSON Type | `String` |
   | Add to ID token | On |
   | Add to access token | On |
   | Add to userinfo | On |

4. Select **Save**.

To check the result, open the client's **Client scopes → Evaluate** tab, pick a user that has some roles, and
select **Generated ID token**. The token must contain, for example, `"roles": ["DISPATCHER"]`.

### 5. Add the users

1. Under **Users → Add user**, create each person who uses iDispatch. Give them a username, which is the name the
   application shows, and a first and last name.
2. On the user's **Credentials** tab, set a temporary password. Keycloak asks them to change it the first time they
   log in.
3. On the user's **Role mapping** tab, select **Assign role**, switch the filter to **Filter by clients**, and pick
   the `idispatch` roles the user needs.

With more than a few users, it's easier to give the roles to groups: create groups such as `Dispatchers`,
`Report readers` and `Administrators` under **Groups**, assign the client roles on each group's **Role mapping**
tab, and add users to the groups. Keycloak can also take users and groups from LDAP or Active Directory under
**User federation**.

Create at least one user with the `ADMIN` role. iDispatch no longer creates an `admin` user by itself.

#### Upgrading from a version with built-in users

Earlier versions of iDispatch kept users in the `users` table of the database. The database migration
`V2__drop_users.sql` drops that table. The usernames and roles aren't migrated, and the old password hashes can't be
moved into Keycloak, so create the users in Keycloak as above before upgrading. Each old flag maps to a role:
*Dispatcher* to `DISPATCHER`, *Report Reader* to `REPORT_READER` and *Admin* to `ADMIN`. Users that were disabled
are simply not created, or are created with **Enabled** off.

### 6. Configure the application

Set the Keycloak connection in the `application.yml` next to `idispatch-server.jar`. (When running in Docker,
set it in `deploy/.env` instead; see [deployment.md](deployment.md).)

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: idispatch
            client-secret: <the client secret from step 2>
        provider:
          keycloak:
            issuer-uri: https://keycloak.example.com/realms/idispatch
```

* `issuer-uri` is the Keycloak base URL followed by `/realms/` and the realm name. Opening
  `<issuer-uri>/.well-known/openid-configuration` in a browser must show a JSON document whose `issuer` is the same
  URL.
* Keep the registration name `keycloak`: the application, and the redirect URI from step 2, refer to it.
* The client secret can be left out of the file and given as the environment variable
  `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KEYCLOAK_CLIENT_SECRET` instead, for example with an
  `Environment=` or `EnvironmentFile=` line in `idispatch.service`.

The application reads the realm's OpenID configuration when it starts, and **doesn't start if Keycloak can't be
reached**. If Keycloak runs on the same machine, make the service start after it (`After=` in
`idispatch.service`).

#### Behind a reverse proxy

The application builds the redirect URI it sends to Keycloak from the request it received. When a reverse proxy
terminates TLS, tell the application to trust the proxy's `X-Forwarded-*` headers, so that the redirect URI starts
with `https://idispatch.example.com` and not with `http://localhost:8080`:

```yaml
server:
  forward-headers-strategy: framework
```

The proxy must set `X-Forwarded-Proto`, `X-Forwarded-Host` and `X-Forwarded-Port`. The runboard WebSocket
(`/ws/runboard`) doesn't use Keycloak and is not affected.

### 7. Check that it works

1. Open iDispatch. The browser goes to the Keycloak login page of your realm.
2. Log in as a user with the `ADMIN` role. The menu shows **Management** and **My Account**, and the username at
   the top of the menu.
3. Log out with the power button. The Keycloak session ends too, and the browser returns to the Keycloak login page.

### Troubleshooting

| Symptom | Cause |
|---|---|
| The application doesn't start: *Unable to resolve the Configuration with the provided Issuer* | `issuer-uri` is wrong, or Keycloak isn't reachable from the server. |
| Keycloak shows *Invalid parameter: redirect_uri* | The redirect URI in step 2 doesn't match the URL the application sends. Check the scheme, host, port and context path, and the reverse proxy settings. |
| Keycloak shows *Invalid redirect uri* when logging out | **Valid post logout redirect URIs** doesn't contain the application's base URL. |
| Login fails with *invalid_token_response* or *invalid_client* | The client secret is wrong, or **Client authentication** is off. |
| Login works, but the user only sees **Home** | The ID token has no `roles` claim (check the mapper in step 4), or the user has no `idispatch` client roles, or the roles are realm roles instead of client roles. |
| **My Account** shows *Sorry, an unexpected error has occurred* | The user lacks the realm's default roles (`default-roles-<realm>`, which grants the `account` client's `manage-account` and `view-profile`). Users created in the admin console get them automatically, but users imported from JSON or created through the admin REST API may not. Assign the role under the user's **Role mapping** tab. |
| Login fails with *invalid_id_token* and *iss* | The browser and the server reach Keycloak through different URLs. Set Keycloak's hostname (`KC_HOSTNAME`) so that both use the same one. |

Role changes in Keycloak take effect the next time the user logs in. Disabling a user, or ending their session in
Keycloak, stops them from logging in again, but doesn't end a session they already have in iDispatch. That session
lasts until they log out or it expires. Restart the application if it must end immediately.
