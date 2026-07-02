# Aastrika Entity Service

A Spring Boot microservice for managing master data — **Positions, Roles, Activities, Competencies and its levels** — with support for multi-language content, hierarchical mappings, and full-text search via OpenSearch.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Entity Types](#entity-types)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Configuration](#configuration)
    - [Run Locally](#run-locally)
    - [Docker](#docker)
- [API Reference](#api-reference)
    - [Entity Management](#entity-management)
    - [Entity Mapping & Hierarchy](#entity-mapping--hierarchy)
- [Behaviour Notes](#behaviour-notes)
    - [Hierarchy Language Fallback](#hierarchy-language-fallback)
    - [Entity Delete and Multi-Language Variants](#entity-delete-and-multi-language-variants)
- [Data Upload Format](#data-upload-format)
- [Running Tests](#running-tests)
- [Contributing](#contributing)

---

## Overview

The Aastrika Entity Service provides a REST API to:

- **Create, update, and delete** master entities (Competencies, Roles, Activities, Positions)
- **Upload** entities in bulk via CSV or XLSX files (currently CSV only — XLSX support planned for the next version)
- **Map entities** in a parent-child hierarchy (Position → Role → Activity → Competency)
- **Search entities** using fuzzy/exact search powered by OpenSearch
- **Support multiple languages** — each entity can exist in multiple language variants identified by a language code

---

## Tech Stack

| Component      | Technology                        |
|----------------|-----------------------------------|
| Language       | Java 21                           |
| Framework      | Spring Boot 3.4.2                 |
| Database       | PostgreSQL                        |
| Search         | OpenSearch (spring-data-opensearch-starter 1.6.0) |
| ORM            | Spring Data JPA / Hibernate 6     |
| Mapping        | MapStruct 1.6.3                   |
| File Parsing   | Apache POI (XLSX), Apache Commons CSV |
| API Docs       | SpringDoc OpenAPI (Swagger UI)    |
| Build Tool     | Maven                             |

---

## Entity Types

| Type         | Description                                          |
|--------------|------------------------------------------------------|
| `POSITION`   | Organizational position (top of hierarchy)           |
| `ROLE`       | Role within a position                               |
| `ACTIVITY`   | Activity performed within a role                     |
| `COMPETENCY` | Competency required for an activity (with levels 1–5)|

### Hierarchy

```
POSITION
  └── ROLE
        └── ACTIVITY
              └── COMPETENCY
```

Allowed parent-child mapping combinations: `POSITION_ROLE`, `ROLE_ACTIVITY`, `ACTIVITY_COMPETENCY`

---

## Architecture

```
src/main/java/com/aastrika/entity/
├── controller/        # REST controllers
├── service/           # Service interfaces and implementations
├── repository/
│   ├── jpa/           # PostgreSQL repositories
│   └── es/            # OpenSearch repositories
├── model/             # JPA entities (MasterEntity, EntityMap, CompetencyLevel)
├── document/          # OpenSearch documents
├── dto/               # Request/Response DTOs
├── mapper/            # MapStruct mappers
├── reader/            # CSV and XLSX sheet readers
├── enums/             # EntityType enum
├── exception/         # Custom exceptions
└── common/            # Constants and utilities
```

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL (default: `localhost:5433`, database: `aastrika_entity_es7`)
- OpenSearch (default: `http://localhost:9200`)

### Configuration

All configuration is in `src/main/resources/application.properties`. Key properties can be overridden via environment variables:

| Environment Variable          | Default                                           | Description                     |
|-------------------------------|---------------------------------------------------|---------------------------------|
| `DATABASE_URL`                | `jdbc:postgresql://localhost:5433/aastrika_entity_es7` | PostgreSQL JDBC URL        |
| `DATABASE_USERNAME`           | `postgres`                                        | PostgreSQL username              |
| `DATABASE_PASSWORD`           | `postgres`                                        | PostgreSQL password              |
| `OPENSEARCH_URIS`             | `http://localhost:9200`                           | OpenSearch URI                   |
| `OPENSEARCH_USERNAME`         | _(empty)_                                         | OpenSearch username              |
| `OPENSEARCH_PASSWORD`         | _(empty)_                                         | OpenSearch password              |
| `JPA_DDL_AUTO`                | `update` ⚠ set to `validate` in non-local envs   | Hibernate DDL strategy           |
| `OPENSEARCH_LOG_LEVEL`        | `WARN`                                            | OpenSearch client log level      |

The service runs on **port 8082** by default.

### Run Locally

```bash
# Build
./mvnw clean package -DskipTests

# Run
./mvnw spring-boot:run
```

Or run the packaged JAR:

```bash
java -jar target/entity-0.0.1-SNAPSHOT.jar
```

### Docker

**Build the image:**

```bash
./mvnw clean package -DskipTests
docker build -t aastrika/entity:latest .
```

**Run the container:**

```bash
docker run -p 8082:8082 \
  -e DATABASE_URL=jdbc:postgresql://<host>:5432/aastrika_entity \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=postgres \
  -e OPENSEARCH_URIS=http://<host>:9200 \
  aastrika/entity:latest
```

---

## API Reference

Swagger UI is available at: `http://localhost:8082/swagger-ui/index.html`

A ready-to-import Postman collection covering all endpoints is available at [`postman_collection.json`](postman_collection.json).

### Entity Management

Base path: `/v1/entity`

| Method   | Endpoint                    | Description                                           |
|----------|-----------------------------|-------------------------------------------------------|
| `POST`   | `/upload`                   | Upload entities from a CSV or XLSX file               |
| `POST`   | `/create`                   | Create a single entity                                |
| `PUT`    | `/update`                   | Update one or more entities (partial update)          |
| `DELETE` | `/delete`                   | Delete entities by code, type, and language           |
| `GET`    | `/search/name/fuzzy`        | Fuzzy search entities by name (Elasticsearch)         |
| `POST`   | `/search`                   | Advanced search with filters (Elasticsearch)          |

#### Upload Entity Sheet

```
POST /v1/entity/upload
Content-Type: multipart/form-data
```

| Parameter     | Type   | Required | Description                     |
|---------------|--------|----------|---------------------------------|
| `language`    | string | Yes      | Language code (e.g., `en`, `hi`)|
| `userId`      | string | Yes      | ID of the user uploading        |
| `entitySheet` | file   | Yes      | CSV or XLSX file                |

#### Create Entity

```
POST /v1/entity/create?userId={userId}
Content-Type: application/json
```

#### Update Entities

```
PUT /v1/entity/update?userId={userId}
Content-Type: application/json
Body: [ { EntityUpdateDTO }, ... ]
```

#### Delete Entities

```
DELETE /v1/entity/delete
Content-Type: application/json
Body: [ { "entityCode": "...", "entityType": "COMPETENCY", "language": "en" }, ... ]
```

#### Fuzzy Search by Name

```
GET /v1/entity/search/name/fuzzy?name={name}
```

#### Advanced Search

```
POST /v1/entity/search
Content-Type: application/json
```

---

### Entity Mapping & Hierarchy

Base path: `/v1/entity`

| Method | Endpoint           | Description                                      |
|--------|--------------------|--------------------------------------------------|
| `POST` | `/mapping`         | Create parent-child entity mappings              |
| `POST` | `/mapping/search`  | Get hierarchy starting from a specific entity    |
| `POST` | `/hierarchy`       | Get the full hierarchy tree from a root entity   |

#### Save Mapping

```
POST /v1/entity/mapping
Content-Type: application/json
Body: [ { EntityMappingRequestDTO }, ... ]
```

#### Get Mapping Hierarchy

```
POST /v1/entity/mapping/search
Content-Type: application/json
Body: { EntitySearchRequestDTO }
```

#### Get Full Hierarchy

```
POST /v1/entity/hierarchy
Content-Type: application/json
Body: { EntitySearchRequestDTO }
```

---

## Behaviour Notes

### Hierarchy Language Fallback

When fetching the full hierarchy (`POST /v1/entity/hierarchy`), the caller passes a preferred `entityLanguage` in the request body. The service resolves entity names and descriptions across the entire hierarchy tree in that language. However, not every entity in the tree is guaranteed to have a variant in the requested language. The fallback logic works as follows:

1. All entity codes reachable from the root are collected via BFS.
2. Entity details are fetched in bulk for the **preferred language** in a single query.
3. For any code that has no record in the preferred language, the service issues a second query to fetch the same codes in **English (`en`)** as the default fallback.
4. The tree is then built entirely from in-memory data — no per-node database calls.

This means a single hierarchy response may contain nodes in different languages: some nodes rendered in the caller's preferred language, and others falling back to English when no translation exists yet.

> **Note:** The `POST /v1/entity/mapping/search` endpoint does **not** apply this fallback. It requires the parent entity to exist in the exact requested language and will return a `400` error if no match is found.

**Example — request body:**

```json
{
  "entityType": "POSITION",
  "entityCode": "P1",
  "entityLanguage": "hi"
}
```

If `P1` and some of its descendants exist in Hindi (`hi`), those nodes are returned in Hindi. Any node that only exists in English is returned in English rather than being omitted from the tree.

---

### Entity Delete and Multi-Language Variants

Each entity code can have multiple language variants stored independently (e.g., `C1` in `en`, `hi`, `ta`). Mappings between entities are shared across all language variants — they are keyed on entity code, not on a specific language.

The delete endpoint (`DELETE /v1/entity/delete`) accepts a list of delete requests. Each request must supply **either** a `language` or `purgeAllLanguage: true`.

#### Single-Language Delete (`purgeAllLanguage: false` or omitted)

Deletes only the specified language variant of the entity.

- If the entity has **other language variants remaining**, the entity mappings (parent and child relationships) are **preserved** — the entity is still logically present in the system via its other variants.
- If this is the **last remaining language variant**, the entity mappings are **also deleted** along with the entity record and its Elasticsearch document.

```json
[
  {
    "entityCode": "C1",
    "entityType": "COMPETENCY",
    "language": "hi"
  }
]
```

In this case, only the Hindi record for `C1` is removed. The English and Tamil variants (if they exist) are untouched, and the hierarchy mappings remain intact.

#### Full Purge (`purgeAllLanguage: true`)

Wipes out the entity entirely — all language variants, all mappings where this entity appears as a parent or child, and all Elasticsearch documents.

```json
[
  {
    "entityCode": "C1",
    "entityType": "COMPETENCY",
    "purgeAllLanguage": true
  }
]
```

This is a **destructive, irreversible** operation. All hierarchy relationships referencing `C1` are removed along with every language record.

| Scenario | Language Variants Deleted | Mappings Deleted | ES Docs Deleted |
|---|---|---|---|
| Single-language delete (other variants exist) | Specified language only | No | Specified language only |
| Single-language delete (last variant) | Last remaining | Yes (parent + child) | Yes |
| `purgeAllLanguage: true` | All variants | Yes (parent + child) | All |

---

## Data Upload Format

Supported formats: **CSV** and **XLSX**

### Required Headers

| Column        | Description                                  |
|---------------|----------------------------------------------|
| `id`          | External entity ID                           |
| `entity_type` | One of: `COMPETENCY`, `ROLE`, `ACTIVITY`, `POSITION` |
| `name`        | Display name                                 |
| `description` | Description text                             |
| `code`        | Unique code for the entity                   |
| `language`    | Language code (e.g., `en`)                   |

### Optional Headers

`type`, `area`, `level_id`, `created_by`, `updated_by`, `reviewed_by`, `created_date`, `updated_date`, `reviewed_date`, `additional_properties`

### Competency Level Headers (for COMPETENCY type)

Up to 5 levels supported:

```
competency_level_1_name, competency_level_1_description
competency_level_2_name, competency_level_2_description
...
competency_level_5_name, competency_level_5_description
```

---

## Running Tests

```bash
./mvnw test
```

Test data files are located in `src/test/resources/test_data/`:
- `master_entities.csv` — sample master entity data
- `entity_map.csv` — sample entity mapping data
- `competency_level.csv` — sample competency level data

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the developer guide — development setup, scripts reference (`check-triage.sh`, `update-release-notes.sh`), release process, and commit guidelines.
