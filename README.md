# 🗓️ Natural Language Date Interpreter

A full-stack web application that interprets natural language date expressions (e.g. *"next Tuesday"*, *"three weeks from now"*) into structured JSON responses using the OpenAI Chat Completions API by default.

---

## Architecture

```
┌─────────────────────────┐        ┌──────────────────────────┐        ┌──────────────────┐
│  React + TypeScript     │  HTTP  │  Spring Boot (Java 21)   │  HTTP  │  Model API        │
│  Vite / nginx           │ ──────▶│  Port 9600               │ ──────▶│  gpt-4o-mini     │
│  Port 3000              │◀────── │  REST + WebFlux          │◀────── │                  │
└─────────────────────────┘        └──────────┬───────────────┘        └──────────────────┘
                                              │ JPA
                                   ┌──────────▼───────────────┐
                                   │  PostgreSQL 16            │
                                   │  Port 5432               │
                                   └──────────────────────────┘
```

| Layer     | Technology                          |
|-----------|-------------------------------------|
| Frontend  | React 19, TypeScript, Vite, nginx   |
| Backend   | Spring Boot 3.3, Java 21, WebClient |
| Database  | PostgreSQL 16                       |
| Container | Docker, Docker Compose              |

---

## Features

- 🔤 Accept natural language date expressions via a clean form UI
- 🤖 Interpret them through a configurable chat model (default: `gpt-4o-mini`)
- 📦 Return structured JSON: `date`, `startDate`, `endDate`, `description`, `original`
- 💾 Persist every query and response to PostgreSQL
- 📋 Display a real-time history of all past queries (newest first)
- 📌 Example chips for quick testing

---

## Project Structure

```
dateInterpreter_project/
├── dateInterpreter/                   # Spring Boot backend
│   ├── src/main/java/com/nlp/dateInterpreter/
│   │   ├── DateInterpreterApplication.java
│   │   ├── DateInterpreterController.java
│   │   ├── DateInterpreterService.java
│   │   ├── RootConfig.java            # CORS configuration
│   │   ├── dto/InterpretRequest.java
│   │   ├── model/DateInterpreter.java
│   │   └── repository/DateInterpreterRepository.java
│   ├── src/main/resources/application.yaml
│   ├── pom.xml
│   └── Dockerfile
│
├── dateInterpreterFrontend/           # React frontend
│   ├── src/
│   │   ├── App.tsx
│   │   ├── App.css
│   │   └── services/dateInterpreter-api.ts
│   ├── nginx.conf
│   ├── Dockerfile
│   └── vite.config.ts
│
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## Prerequisites

| Tool           | Minimum version |
|----------------|-----------------|
| Java JDK       | 21              |
| Maven          | 3.9+            |
| Node.js        | 20+             |
| Docker Desktop | 24+             |
| OpenAI API key  | —              |

---

## Quick Start with Docker (Recommended)

### 1. Clone / navigate to the project root

```bash
cd dateInterpreter_project
```

### 2. Create or update your `.env` file

```env
SERVER_PORT=9600
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/nldates
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=mysecurepassword
SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT=30000
SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT=60000
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=true
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
SPRING_JPA_HIBERNATE_FORMAT_SQL=true
APP_TIME_ZONE=Asia/Kolkata
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4o-mini
OPENAI_BASE_URL=https://api.openai.com/v1/chat/completions
```

> Your current project also supports `openAi-api-token=...` in the root `.env` file. The backend now loads that root `.env` automatically for local Spring Boot runs, and Docker Compose passes it to the backend container. If both are present, `NL_MODEL_API_KEY` or `OPENAI_API_KEY` take precedence over it.
>
> If you want to keep using a non-OpenAI provider, override `NL_MODEL_ENDPOINT` and set `NL_MODEL_API_KEY` directly. `GITHUB_TOKEN` is still supported as a fallback for GitHub Models-backed endpoints.

### 3. Build and start all services

```bash
docker-compose up --build
```

Docker will:
1. Pull `postgres:16-alpine`
2. Build the Spring Boot backend (multi-stage Maven build)
3. Build the React frontend (multi-stage Node → nginx)
4. Wire all three services together

### 4. Open the app

- **Frontend:** http://localhost:3000
- **Backend API:** http://localhost:9600/api/date
- **Swagger UI:** http://localhost:9600/swagger-ui.html

### 5. Stop everything

```bash
docker-compose down          # stop containers
docker-compose down -v       # also delete the postgres volume
```

---

## Local Development (Without Docker)

### Backend

1. **Start a local PostgreSQL instance** (or use Docker for just the DB):

   ```bash
   docker run -d --name nldate-postgres \
     -e POSTGRES_DB=nldates \
     -e POSTGRES_USER=postgres \
     -e POSTGRES_PASSWORD=password \
     -p 5432:5432 postgres:16-alpine
   ```

2. **Set environment variables:**

   **Windows (PowerShell):**
   ```powershell
   $env:OPENAI_API_KEY = "sk-..."
   $env:OPENAI_MODEL = "gpt-4o-mini"
   $env:SPRING_DATASOURCE_PASSWORD = "password"
   ```

   **Linux / macOS:**
   ```bash
   export OPENAI_API_KEY=sk-...
   export OPENAI_MODEL=gpt-4o-mini
   export SPRING_DATASOURCE_PASSWORD=password
   ```

3. **Run the backend:**

   ```bash
   cd dateInterpreter
   ./mvnw spring-boot:run
   ```

   Backend starts on **port 9600**.

### Frontend

```bash
cd dateInterpreterFrontend
npm install
npm run dev
```

Frontend starts on **http://localhost:5173** (Vite proxies `/api` → `http://localhost:9600`).

---

## API Reference

### `POST /api/date/interpret`

Interprets a natural language date expression.

**Request body:**
```json
{
  "text": "Monday in two weeks",
  "timezone": "Asia/Kolkata"   // optional, defaults to UTC
}
```

**Response:**
```json
{
  "date": "2025-06-02",
  "startDate": "2025-06-02",
  "endDate": "2025-06-02",
  "description": "The Monday occurring two weeks from today",
  "original": "Monday in two weeks"
}
```

### `GET /api/date/history`

Returns all past queries, newest first.

```json
[
  {
    "id": 1,
    "userInput": "Monday in two weeks",
    "jsonResponse": { "date": "2025-06-02", ... },
    "createdAt": "2025-05-22T10:30:00"
  }
]
```

---

## Database Schema

The table `nl_dates` is auto-created by Hibernate (`ddl-auto: update`):

| Column        | Type        | Notes                        |
|---------------|-------------|------------------------------|
| `id`          | BIGINT      | Auto-increment primary key   |
| `user_input`  | VARCHAR     | Original natural language text |
| `json_response` | JSONB     | AI-generated structured JSON |
| `created_at`  | TIMESTAMP   | Auto-set on insert           |

---

## Docker Workflow

```
                  docker-compose up --build
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
   postgres:5432      backend:9600     frontend:80
  (health-check)     (waits for DB)  (waits for backend)
                           │                │
                     Spring Boot      nginx serves
                     auto-creates     React SPA +
                     nl_dates table   proxies /api/*
```

- **Startup order:** `postgres` → `backend` → `frontend`
- **Health check:** postgres readiness gate prevents backend from connecting before DB is ready
- **Volumes:** `postgres_data` persists your data between `docker-compose down/up` cycles

---

## Environment Variables

| Variable                   | Default              | Description                     |
|----------------------------|----------------------|---------------------------------|
| `SERVER_PORT`               | Required             | Backend port |
| `SPRING_DATASOURCE_URL`     | Required             | DB JDBC URL |
| `SPRING_DATASOURCE_USERNAME`| Required             | DB username |
| `SPRING_DATASOURCE_PASSWORD`| Required             | DB password |
| `SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT` | Required | Hikari connection timeout |
| `SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT` | Required | Hikari initialization timeout |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Required        | Hibernate schema mode |
| `SPRING_JPA_SHOW_SQL`       | Required             | Enable SQL logging |
| `SPRING_JPA_DATABASE_PLATFORM` | Required         | Hibernate dialect |
| `SPRING_JPA_HIBERNATE_FORMAT_SQL` | Required      | Format SQL logs |
| `APP_TIME_ZONE`             | Required             | JDBC/session timezone |
| `OPENAI_API_KEY`            | Preferred            | OpenAI API key for the backend model call |
| `openAi-api-token`          | Fallback             | Legacy/custom token name supported by the backend |
| `OPENAI_MODEL`              | Required unless `NL_MODEL_NAME` is set | Model name sent to the API |
| `OPENAI_BASE_URL`           | Required unless `NL_MODEL_ENDPOINT` is set | OpenAI-compatible chat endpoint |
| `NL_MODEL_API_KEY`          | Optional override    | Provider-agnostic API key override |
| `NL_MODEL_ENDPOINT`         | Optional override    | Provider-agnostic endpoint override |
| `NL_MODEL_NAME`             | Optional override    | Provider-agnostic model override |
| `GITHUB_TOKEN`              | Optional fallback    | GitHub Models token if you point `NL_MODEL_ENDPOINT` at GitHub Models |

---

## Challenges & Solutions

| Challenge | Solution |
|-----------|----------|
| Model responses may not be valid JSON | The backend now requests `response_format: {type: "json_object"}` and returns an HTTP error when parsing still fails |
| Frontend CORS errors during local dev | Configured Vite `server.proxy` to forward `/api` requests to the backend — browser only talks to the dev server |
| PostgreSQL not ready when backend starts | Added `healthcheck` + `condition: service_healthy` in docker-compose so the backend waits for a healthy DB |
| Serializing JSONB column as object (not string) | Used `@JsonRawValue` on the `jsonResponse` field — Jackson embeds it inline as raw JSON |
| Hardcoded credentials | Moved all secrets to environment variables; provided `.env.example` template |
