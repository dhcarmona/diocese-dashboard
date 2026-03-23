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

It's a regular Maven/Spring Boot project. To build and generate the JAR file, run
  mvn clean package

The following environment variables are required to start the application:

| Variable                    | Purpose                        |
|-----------------------------|--------------------------------|
| `POSTGRESQL_PORT`           | PostgreSQL port (e.g. `5432`)  |
| `SPRING_DATABASE_NAME`      | Database name                  |
| `SPRING_DATASOURCE_USERNAME`| Database username              |
| `SPRING_DATASOURCE_PASSWORD`| Database password              |

## Authentication

The app uses session-based login. Visit `/login` in the browser and sign in with your credentials. An Admin account must be created directly in the database before first use — after that, additional accounts can be managed via `POST /api/users`.

## Running Locally

### Option A — UI only (no backend required)

Useful for reviewing and developing the frontend without a running database or Spring Boot instance.

```bash
cd frontend
npm run dev
```

Open **http://localhost:5173**. The login page will render, but submitting the form will fail since there is no backend. API calls are proxied to `localhost:8080` when running this way.

### Option B — Full stack

Requires PostgreSQL. The quickest way to start one with Docker:

```bash
docker run -d \
  --name diocese-db \
  -e POSTGRES_DB=diocese \
  -e POSTGRES_USER=diocese \
  -e POSTGRES_PASSWORD=secret \
  -p 5432:5432 \
  postgres:16
```

Then set the required environment variables and start Spring Boot:

```bash
export POSTGRESQL_PORT=5432
export SPRING_DATABASE_NAME=diocese
export SPRING_DATASOURCE_USERNAME=diocese
export SPRING_DATASOURCE_PASSWORD=secret

mvn spring-boot:run
```

Open **http://localhost:8080**. Spring Boot serves the pre-built React app from `src/main/resources/static/`.

> **Note:** The schema is recreated on every startup. The database starts empty — you must seed an initial Admin user directly in the database before you can log in. See the schema for the `DashboardUser` table structure.

## Generating the schema

There's a separate main class in charge of generating the schema. Run
  mvn compile exec:java

To run the code that generates the schema. The SQL will be generated in a schema.sql file in the root folder.

---

## TODO

- Build the **reporter-facing UI form** that a reporter sees upon following a reporter link token
  (i.e., a browser page at `/r/{token}` that the login page redirects to after authentication,
  pre-loads the linked service template's fields, and lets the reporter fill in and submit the
  report without any API knowledge).
- Add a **link management UI** for ADMIN users to create, list, copy, and revoke reporter links.
