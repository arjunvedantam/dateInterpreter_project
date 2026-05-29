# 🗓️ Natural Language Date Interpreter

A full-stack web application that interprets natural language date expressions (e.g. *"next Tuesday"*, *"three weeks from now"*) into structured JSON responses using the OpenAI API.

---

## Architecture

```
┌─────────────────────────┐        ┌──────────────────────────┐        ┌──────────────────┐
│  React + TypeScript     │  HTTP  │  Spring Boot (Java 21)   │  HTTP  │  OpenAI API      │
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
| Backend   | Spring Boot 3.3, Java 21, WebFlux   |
| Database  | PostgreSQL 16                       |
| Container | Docker, Docker Compose              |

---

## Features

- 🔤 Accept natural language date expressions via a clean form UI
- 🤖 Interpret them through OpenAI's `gpt-4o-mini` model
- 📦 Return structured JSON: `date`, `startDate`, `endDate`, `description`, `original`
- 💾 Persist every query and response to PostgreSQL
- 📋 Display a real-time history of all past queries (newest first)
- 📌 Example chips for quick testing

---

## Project Structure

```
Learnbench/
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
├── dateInterpreterFrontend/
│   └── dateInterpreterFrontend/       # React frontend
│       ├── src/
│       │   ├── App.tsx
│       │   ├── App.css
│       │   └── services/dateInterpreter-api.ts
│       ├── nginx.conf
│       ├── Dockerfile
│       └── vite.config.ts
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
| OpenAI API key | —               |

---

## Quick Start with Docker (Recommended)

### 1. Clone / navigate to the project root

```bash
cd Learnbench
```

### 2. Create your `.env` file

```bash
cp .env.example .env
```

Edit `.env` and fill in your values:

```env
GITHUB_TOKEN=ghp_...        # your GitHub personal access token
POSTGRES_PASSWORD=mysecurepassword
```

> **GitHub token requirements:**  
> Generate a token at [github.com/settings/tokens](https://github.com/settings/tokens).  
> An active **GitHub Copilot** subscription is required to use `api.githubcopilot.com`.

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
   $env:GITHUB_TOKEN = "ghp_..."
   $env:SPRING_DATASOURCE_PASSWORD = "password"
   ```

   **Linux / macOS:**
   ```bash
   export GITHUB_TOKEN=ghp_...
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
cd dateInterpreterFrontend/dateInterpreterFrontend
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
| `GITHUB_TOKEN`           | *(required)*         | GitHub PAT with Copilot API access  |
| `POSTGRES_PASSWORD`        | `password`           | PostgreSQL password             |
| `SPRING_DATASOURCE_URL`    | `jdbc:postgresql://localhost:5432/nldates` | DB JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres`         | DB username                     |
| `SPRING_DATASOURCE_PASSWORD` | `password`         | DB password                     |
| `NL_MODEL_ENDPOINT`        | `https://api.githubcopilot.com/chat/completions` | Override API endpoint |

---

## Challenges & Solutions

| Challenge | Solution |
|-----------|----------|
| OpenAI sometimes returns markdown-wrapped JSON | Added `response_format: {type: "json_object"}` to the API call + system message enforcing raw JSON |
| Frontend CORS errors during local dev | Configured Vite `server.proxy` to forward `/api` requests to the backend — browser only talks to the dev server |
| PostgreSQL not ready when backend starts | Added `healthcheck` + `condition: service_healthy` in docker-compose so backend waits for a healthy DB |
| Serializing JSONB column as object (not string) | Used `@JsonRawValue` on the `jsonResponse` field — Jackson embeds it inline as raw JSON |
| Hardcoded credentials | Moved all secrets to environment variables; provided `.env.example` template |
