# Log Intelligence Platform – Technical Specification

## 1. Overview

The **Log Intelligence Platform** is a lightweight, self‑contained system for ingesting, storing, analysing, and visualising application logs (originally targeting WildFly server logs). It consists of:

- an **ingestion script** (`ingestion.py`) that parses a plain‑text log file and loads the data into a SQLite database with full‑text search capabilities;
- a **FastAPI web server** (`main.py`) that exposes RESTful endpoints for log retrieval, search, and analytics;
- a **single‑page frontend** (`view.html`) that provides a real‑time log viewer and an interactive analytics dashboard.

The platform is designed for developer‑friendly local deployment, requiring only Python 3.8+ and no external databases or heavy infrastructure.

---

## 2. Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   server.log    │────▶│  ingestion.py   │────▶│    logs.db      │
│ (raw log file)  │     │ (parser & ETL)  │     │   (SQLite)      │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                         │
                                                         ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Web Browser   │◀───▶│   main.py       │◀───▶│   SQLite API    │
│  (view.html)    │     │ (FastAPI)       │     │  + FTS5         │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

- **Data flow**: Raw logs → ingestion → SQLite with FTS5 → API → frontend.
- **Processing**: Ingestion is a one‑time or batch run; the API server reads the resulting database.
- **Frontend**: Static HTML/JS served from the FastAPI server (or directly opened).

---

## 3. Components

### 3.1 Ingestion Module (`ingestion.py`)

**Purpose**: Parse a log file, create a fresh SQLite database, and insert structured records in batches.

**Configuration** (hardcoded constants):
| Variable       | Default          | Description                              |
|----------------|------------------|------------------------------------------|
| `LOG_FILE`     | `server.log`     | Input log file path.                     |
| `DB_FILE`      | `logs.db`        | Output SQLite database path.             |
| `BATCH_SIZE`   | `10000`          | Rows inserted per transaction.           |

**Log line format** (regex `LOG_PATTERN`):
```
YYYY-MM-DD HH:MM:SS,mmm LEVEL [logger] (thread) message
```
- `LEVEL`: INFO, DEBUG, WARN, ERROR.
- `logger`, `thread`: any characters except `]` and `)` respectively.
- `message`: rest of the line.

**Database Schema**:
```sql
-- Main table
CREATE TABLE logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp TEXT,
    level TEXT,
    logger TEXT,
    thread TEXT,
    message TEXT,
    raw TEXT
);

-- FTS5 virtual table (full‑text search on message + logger)
CREATE VIRTUAL TABLE logs_fts USING fts5(
    message, logger,
    content='logs',
    content_rowid='id',
    tokenize="porter unicode61"
);

-- Vocabulary table for term statistics (word cloud)
CREATE VIRTUAL TABLE logs_fts_vocab USING fts5vocab(logs_fts, 'row');

-- Triggers to keep FTS index in sync
CREATE TRIGGER logs_ai AFTER INSERT ON logs BEGIN
    INSERT INTO logs_fts(rowid, message, logger)
    VALUES (new.id, new.message, new.logger);
END;
```

**Optimisations**:
- WAL journal mode (`PRAGMA journal_mode=WAL`).
- `synchronous=NORMAL`.
- Final `INSERT INTO logs_fts(logs_fts) VALUES('optimize')` after ingestion.

**Execution**: `python ingestion.py` – removes any existing `logs.db` and recreates it.

### 3.2 API Server (`main.py`)

**Framework**: FastAPI, served via Uvicorn.

**CORS**: Allowed origins `["*"]` (configurable).

**Database helper**: Context manager `get_db()` that returns a connection with `row_factory = sqlite3.Row`. Raises `FileNotFoundError` if `logs.db` does not exist.

**Regex for analytics**:
- `UUID_PATTERN`: matches standard UUIDs (`[0-9a-f]{8}-...`).
- `FORM_CONTEXT_PATTERN`: extracts the context after `Form--> Cxt:-` (up to `Id`).

### 3.3 Frontend (`view.html`)

A single HTML file that:
- Uses **Tailwind CSS** for styling.
- Implements two tabs: **Stream Viewer** and **Analytics**.
- Communicates with the API server (default `http://localhost:8000`).
- **Stream Viewer**: infinite scroll (keyset pagination), level filter, FTS search, real‑time log display.
- **Analytics**: time distribution charts (line & cumulative), word cloud (clickable terms), UUID and form context lists.

---

## 4. API Specification

Base URL: `http://localhost:8000`

### 4.1 `GET /`
Returns server status and total log count.

**Response** (example):
```json
{ "status": "online", "total_logs": 123456 }
```

### 4.2 `GET /view`
Serves the frontend HTML file (`view.html`).

**Response**: `text/html`

### 4.3 `GET /api/logs/stream`
Streams logs as newline‑delimited JSON objects (Server‑Sent Events style, but with one‑time fetch).

**Query Parameters**:
| Parameter | Type   | Default | Description                                         |
|-----------|--------|---------|-----------------------------------------------------|
| `level`   | string | `all`   | Filter by log level (`INFO`, `DEBUG`, `WARN`, `ERROR`, or `all`). |
| `search`  | string | `""`    | Full‑text search query (FTS5 syntax).              |
| `last_id` | int    | `999999999` | Keyset pagination: returns rows with `id < last_id`. |

**Behaviour**:
- Returns at most 500 rows per request, ordered by `id DESC`.
- Uses FTS5 join when `search` is provided.
- Response is plain text, each line a JSON object representing a log row.

**Example line**:
```json
{"id": 4521, "timestamp": "2025-02-15 10:32:45,123", "level": "ERROR", "logger": "com.example.Service", "thread": "http-nio-8080-exec-5", "message": "Connection timeout", "raw": "2025-02-15 10:32:45,123 ERROR [com.example.Service] (http-nio-8080-exec-5) Connection timeout"}
```

### 4.4 `GET /api/analytics/overview`
Returns total count and per‑level counts.

**Response**:
```json
{
  "total": 123456,
  "levels": { "INFO": 90000, "DEBUG": 20000, "WARN": 3000, "ERROR": 456 }
}
```

### 4.5 `GET /api/analytics/time-distribution`
Returns time‑bucketed log counts and total raw text size.

**Query Parameter**:
| Parameter    | Type   | Default | Description                                    |
|--------------|--------|---------|------------------------------------------------|
| `resolution` | string | `1m`    | Bucket size: `1m`, `5m`, `15m`, `30m`, `1h`.  |

**Response**:
```json
{
  "data": [
    { "time": "2025-02-15 10:32", "count": 125, "size": 18750 },
    { "time": "2025-02-15 10:33", "count": 98, "size": 14200 }
  ]
}
```
- `time`: bucket label (e.g., `YYYY-MM-DD HH:MM` for 1m, `YYYY-MM-DD HH:00` for 1h).
- `size`: sum of length of the `raw` field in bytes.

### 4.6 `GET /api/analytics/word-cloud`
Returns the most frequent terms from the FTS vocabulary.

**Query Parameter**:
| Parameter | Type | Default | Description                |
|-----------|------|---------|----------------------------|
| `limit`   | int  | `50`    | Maximum number of terms.   |

**Response**:
```json
{
  "data": [
    { "word": "exception", "count": 234 },
    { "word": "timeout", "count": 187 }
  ]
}
```
- `count` = document frequency (number of rows containing the term).

### 4.7 `GET /api/analytics/patterns`
Scans the `raw` column to extract UUIDs and form contexts.

**Response**:
```json
{
  "uuids": [
    { "id": "550e8400-e29b-41d4-a716-446655440000", "count": 42 }
  ],
  "forms": [
    { "context": "UserLogin", "count": 15 }
  ]
}
```
- Both lists are sorted by frequency (most common first), limited to 50 entries each.

---

## 5. Frontend Features (view.html)

### 5.1 Stream Viewer Tab
- **Live log list**: shows timestamp, level badge, logger, and message.
- **Filter buttons**: All / INFO / DEBUG / WARN / ERROR.
- **Search input**: real‑time FTS search (debounced 350ms).
- **Infinite scroll**: “Load Older Logs” button (keyset pagination using `last_id`).
- **Stats cards**: total logs (overview), INFO count, DEBUG count, number of loaded logs.

### 5.2 Analytics Tab
- **Time Distribution** (line chart): logs per time bucket, with interactive crosshair and tooltip. Horizontal scrolling for many buckets.
- **Cumulative Chart**: two lines – cumulative log count and cumulative raw data size.
- **Word Cloud**: clickable terms that pre‑fill the search box and switch to the Stream Viewer.
- **UUIDs & Form Contexts**: lists of extracted patterns with occurrence counts.

### 5.3 Styling & UX
- Dark theme (CSS variables: `--bg`, `--accent`, etc.).
- Responsive layout (flex/grid, Tailwind).
- Animations (fade‑in, line drawing, area fade).
- Tooltips and crosshair on charts.
- Resolution selector for time distribution (`1m`, `5m`, `15m`, `30m`, `1h`).

---

## 6. Deployment & Usage

### 6.1 Prerequisites
- Python 3.8+
- Install dependencies:
  ```bash
  pip install fastapi uvicorn
  ```
  (SQLite3 is built‑in.)

### 6.2 Ingestion
Place the log file as `server.log` (or change the constant). Run:
```bash
python ingestion.py
```
A new `logs.db` file will be created. The script prints progress and final record count.

### 6.3 Start the API Server
```bash
python main.py
```
Server runs at `http://0.0.0.0:8000`. Access the frontend at `http://localhost:8000/view`.

### 6.4 Customisation
- Change log file path: edit `LOG_FILE` in `ingestion.py`.
- Change database path: edit `DB_FILE` in both `ingestion.py` and `main.py`.
- Adjust CORS: modify `allow_origins` in `main.py`.
- Port/host: modify `uvicorn.run(app, host="0.0.0.0", port=8000)`.

---

## 7. Assumptions & Limitations

### 7.1 Log Format
The regex parser assumes exactly the pattern shown. Lines that do not match are silently ignored (not inserted). Multiline messages are **not** supported.

### 7.2 Database Lifetime
`ingestion.py` always removes the existing database. For incremental ingestion, the script would need to be extended (e.g., by tracking file offset or using `INSERT OR IGNORE`).

### 7.3 Performance
- **Ingestion**: batch inserts + WAL – suitable for files up to several hundred MB.
- **API**: `patterns` endpoint scans the entire `raw` column each time; for databases >1M rows this may become slow. A background pre‑computation would be needed for production.
- **FTS**: vocabulary table provides fast word cloud, but the query may still be heavy for huge vocabularies.

### 7.4 Concurrency
The API server uses `check_same_thread=False` on the SQLite connection; multiple requests are allowed. However, SQLite’s write concurrency is limited – this platform is read‑only after ingestion, so no issues.

### 7.5 Security
- No authentication / authorisation.
- CORS is wide open (`*`).
- SQLite injection is prevented by parameterised queries.
- The frontend is static; no user input is evaluated.

---

## 8. Future Extensions (Suggested)
- Support for incremental ingestion (tail a growing log file).
- Background worker for pattern extraction (caching results).
- Export logs as CSV / JSON.
- User‑defined log format (regex configurable at runtime).
- Authentication layer and multi‑user support.
- Deployment via Docker.
- Persistent configuration (e.g., `.env` file).

---

## Appendix A – Example Log Line

```
2025-02-15 10:32:45,123 ERROR [com.example.Service] (http-nio-8080-exec-5) Failed to process request: timeout
```

## Appendix B – Database ER Diagram

```
┌─────────────┐       ┌─────────────────┐       ┌──────────────────┐
│    logs     │       │    logs_fts     │       │ logs_fts_vocab   │
├─────────────┤       ├─────────────────┤       ├──────────────────┤
│ id (PK)     │◀──────│ rowid (FK)      │       │ term             │
│ timestamp   │       │ message         │       │ doc (doc count)  │
│ level       │       │ logger          │       │ cnt (total freq) │
│ logger      │       └─────────────────┘       └──────────────────┘
│ thread      │
│ message     │
│ raw         │
└─────────────┘
```
