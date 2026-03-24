# Diocese Dashboard project

## Description

This simple Spring Boot project will serve as a replacement for the current Google Forms based approach for church reports and statistics for the Episcopal Church in Costa Rica.

## Project Goals

The application targets two types of users:

### Admin Users
- Log in to the system to manage core data.
- Add and remove **Churches** and **Celebrants**.
- Define **Service Info Items** — the fields that make up a service report (e.g., attendance count, offering amount), each with a specific type.
- Create **Service Templates**, which bundle a set of Service Info Items together with a required Church field and an optional Celebrant field.
- Create and manage **Reporter** and **Admin** user accounts, assigning each Reporter to a specific Church.
- Can also do everything a Reporter can do.

### Reporter Users
- Are assigned to a specific Church at account creation.
- Submit **Service Instances** for their assigned church by:
  - (Optionally) selecting the Celebrant for that service.
  - Filling in the Service Info Items defined by the template.
- Each Service Instance is tied to a specific date, and users may submit multiple instances for the same template.
- Can only view service instances belonging to their assigned church.

---

## Building

It's a regular Maven/Spring Boot project. `mvn clean package` compiles the Java code, **builds the React frontend** (via the `frontend-maven-plugin`), and packages everything into a runnable JAR:

```bash
mvn clean package
```

> **Note:** The first run downloads Node.js into `frontend/node/` — this is normal and takes a moment.

The following environment variables are required to start the application:

| Variable                    | Purpose                        |
|-----------------------------|--------------------------------|
| `POSTGRESQL_PORT`           | PostgreSQL port (e.g. `5432`)  |
| `SPRING_DATABASE_NAME`      | Database name                  |
| `SPRING_DATASOURCE_USERNAME`| Database username              |
| `SPRING_DATASOURCE_PASSWORD`| Database password              |

## Authentication

The app uses session-based login. Visit `/login` in the browser and sign in with your
credentials.

For first-time setup, you can ask the app to create the initial Admin account on startup:

```bash
export DASHBOARD_BOOTSTRAP_ADMIN_ENABLED=true
export DASHBOARD_BOOTSTRAP_ADMIN_USERNAME=admin
export DASHBOARD_BOOTSTRAP_ADMIN_PASSWORD=change-me
```

That bootstrap step only runs when enabled and only if no `ADMIN` user already exists.
After the first Admin is in place, additional accounts can be managed via `POST /api/users`.

## Running Locally

### Option A — UI only (no backend required)

Useful for reviewing and developing the frontend without a running database or Spring Boot instance.

```bash
cd frontend
npm install   # first time only
npm run dev
```

Open **http://localhost:5173**. The login page will render, but submitting the form will fail since there is no backend. API calls are proxied to `localhost:8080` when running this way.

### Option B — Full stack

Requires PostgreSQL. The repository now includes a ready-to-use Docker Compose file,
so the quickest way to start a local database is:

```bash
docker compose up -d db
```

Then set the required environment variables and start Spring Boot:

```bash
export POSTGRESQL_PORT=5432
export SPRING_DATABASE_NAME=diocese
export SPRING_DATASOURCE_USERNAME=diocese
export SPRING_DATASOURCE_PASSWORD=secret
export DASHBOARD_BOOTSTRAP_ADMIN_ENABLED=true
export DASHBOARD_BOOTSTRAP_ADMIN_USERNAME=admin
export DASHBOARD_BOOTSTRAP_ADMIN_PASSWORD=change-me

mvn package spring-boot:run
```

If you prefer different local credentials or a different port, override the Compose defaults:

```bash
POSTGRES_DB=diocese POSTGRES_USER=diocese POSTGRES_PASSWORD=secret POSTGRES_PORT=5432 \
  docker compose up -d db
```

> **Important:** `mvn spring-boot:run` alone does **not** rebuild the frontend. Always use `mvn package spring-boot:run` (or run `mvn package` first) so the React app is compiled into `src/main/resources/static/` before the server starts. On a fresh checkout this directory will be empty until a build runs.

Open **http://localhost:8080**. Spring Boot serves the pre-built React app from `src/main/resources/static/`.

> **Note:** The application now keeps data between runs. Schema changes are managed with
> Flyway migrations, and PostgreSQL data persists as long as you keep the Docker volume (or
> your regular PostgreSQL data directory). The bootstrap Admin settings are only needed for
> first-time setup.

To stop the local database:

```bash
docker compose stop db
```

To completely reset the local database during development:

```bash
docker compose down -v
docker compose up -d db
```

If Flyway reports a checksum mismatch, prefer creating a new migration (`V2__...sql`,
`V3__...sql`, etc.) instead of editing an existing applied migration. If you intentionally
changed an already-applied migration in your local environment, you can repair Flyway once:

```bash
mvn org.flywaydb:flyway-maven-plugin:10.15.2:repair \
  -Dflyway.url=jdbc:postgresql://localhost:$POSTGRESQL_PORT/$SPRING_DATABASE_NAME \
  -Dflyway.user=$SPRING_DATASOURCE_USERNAME \
  -Dflyway.password=$SPRING_DATASOURCE_PASSWORD
```

## Generating the schema

There's a separate main class in charge of generating the schema. Run

```bash
mvn compile exec:java
```

The SQL will be generated in `schema.sql` in the root folder.

---

## TODO

- Build the **reporter-facing UI form** that a reporter sees upon following a reporter link token
  (i.e., a browser page at `/r/{token}` that the login page redirects to after authentication,
  pre-loads the linked service template's fields, and lets the reporter fill in and submit the
  report without any API knowledge).
- Add a **link management UI** for ADMIN users to create, list, copy, and revoke reporter links.

---

## Internationalization (i18n)

The UI is internationalized with [i18next](https://www.i18next.com/) + `react-i18next`. A compact **EN / ES** toggle is pinned to the top-right corner of every page. The selected language is persisted in `localStorage` automatically.

Translation files live in `frontend/src/locales/`:

```
frontend/src/locales/
├── en.json   ← English strings
└── es.json   ← Spanish strings
```

### Adding a new UI string

1. Add the key under the appropriate section in **both** `en.json` and `es.json`:
   ```json
   // en.json
   { "mySection": { "myKey": "Hello" } }

   // es.json
   { "mySection": { "myKey": "Hola" } }
   ```
2. Use `const { t } = useTranslation()` in your component and reference it with `t('mySection.myKey')`.

### Adding a new language

1. Create `frontend/src/locales/{code}.json` (e.g. `pt.json` for Portuguese) with the same keys as `en.json`.
2. Import it and register it in `frontend/src/i18n.ts`:
   ```ts
   import pt from './locales/pt.json';
   // inside init resources:
   resources: { en: { translation: en }, es: { translation: es }, pt: { translation: pt } }
   ```
3. Add the new `value`/`aria-label` pair to the `ToggleButtonGroup` in `frontend/src/components/LanguageSwitcher.tsx`.
