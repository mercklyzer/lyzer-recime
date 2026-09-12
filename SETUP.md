# Setup

## Prerequisites

- Java 17
- Docker with the Compose v2 plugin (`docker compose`)

## Database

PostgreSQL **14.17** runs in a container, matching the version the tests use via
Testcontainers. The definition lives in [`compose.yaml`](compose.yaml).

```bash
docker compose up -d          # start (detached)
docker compose ps             # check status — wait for "healthy"
docker compose logs -f postgres
docker compose down           # stop, keep data
docker compose down -v        # stop and drop the volume (fresh database)
```

Data persists in the named volume `postgres-data` across `up`/`down` cycles.

### Connection details

| Setting  | Default   |
| -------- | --------- |
| Host     | `localhost` |
| Port     | `5432`    |
| Database | `recime`  |
| User     | `recime`  |
| Password | `recime`  |

JDBC URL: `jdbc:postgresql://localhost:5432/recime`

Every value is overridable from the environment (or a `.env` file next to
`compose.yaml`) — `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`,
`POSTGRES_PORT`. The committed defaults are development-only credentials; a real
deployment supplies its own.

If port 5432 is already taken by a local PostgreSQL install, either stop it or
run with an alternative published port:

```bash
POSTGRES_PORT=55432 docker compose up -d
```

### Opening a psql shell

```bash
docker compose exec postgres psql -U recime -d recime
```

## Running the application

Start the database first, then:

```bash
./mvnw spring-boot:run
```

## Tests

```bash
./mvnw test
```

Tests spin up their own PostgreSQL 14.17 container through Testcontainers, so the
Compose database does not need to be running — only a working Docker daemon.
