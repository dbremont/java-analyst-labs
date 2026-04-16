import sqlite3
import json
import os
from contextlib import contextmanager
from fastapi import FastAPI, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse, HTMLResponse, FileResponse

app = FastAPI(title="Log Intelligence API")

# CORS Configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

"""
id INTEGER PRIMARY KEY AUTOINCREMENT,
timestamp TEXT,
level TEXT,
logger TEXT,
thread TEXT,
message TEXT,
raw TEXT
"""
DB_PATH = "logs.db"

# --- DB Helper ---
@contextmanager
def get_db():
    if not os.path.exists(DB_PATH):
        raise FileNotFoundError("Database not found. Run ingestion.py first.")
    
    conn = sqlite3.connect(DB_PATH, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    try:
        yield conn
    finally:
        conn.close()

# --- Endpoints ---

@app.get("/")
async def root():
    try:
        with get_db() as conn:
            cur = conn.cursor()
            cur.execute("SELECT count(*) FROM logs")
            total = cur.fetchone()[0]
        return {"status": "online", "total_logs": total}
    except FileNotFoundError:
        return {"status": "error", "message": "Database not initialized. Run ingestion.py"}

@app.get("/view", response_class=HTMLResponse)
def view_logs():
    file_path = "parse.html"
    return FileResponse(file_path)

@app.get("/api/logs/stream")
async def stream_logs(level: str = "all", search: str = "", last_id: int = 999999999):
    async def log_generator():
        with get_db() as conn:
            cur = conn.cursor()
            
            # Base query with keyset pagination (fastest)
            query = "SELECT id, timestamp, level, logger, thread, message, raw FROM logs WHERE id < ?"
            params = [last_id]

            if level != "all":
                query += " AND level = ?"
                params.append(level)
            
            # Use FTS5 for search
            if search:
                # Combine FTS with standard filters
                query = '''
                    SELECT l.id, l.timestamp, l.level, l.logger, l.thread, l.message, l.raw 
                    FROM logs l 
                    JOIN logs_fts fts ON l.id = fts.rowid 
                    WHERE logs_fts MATCH ? AND l.id < ?
                '''
                params = [search, last_id]
                if level != "all":
                    query += " AND l.level = ?"
                    params.append(level)

            query += " ORDER BY id DESC LIMIT 500"
            
            cur.execute(query, params)
            
            for row in cur.fetchall():
                yield json.dumps(dict(row)) + "\n"

    return StreamingResponse(log_generator(), media_type="application/json")

@app.get("/api/analytics/overview")
async def get_overview():
    with get_db() as conn:
        cur = conn.cursor()
        cur.execute("SELECT level, count(*) as count FROM logs GROUP BY level")
        levels = {row['level']: row['count'] for row in cur.fetchall()}
        cur.execute("SELECT count(*) FROM logs")
        total = cur.fetchone()[0]
    return {"total": total, "levels": levels}

@app.get("/api/analytics/time-distribution")
async def get_time_distribution():
    # ADDED: SUM(LENGTH(raw)) to calculate data volume per time bucket
    query = '''
        SELECT 
            substr(timestamp, 1, 16) as bucket_time, 
            count(*) as count,
            SUM(LENGTH(raw)) as size
        FROM logs
        GROUP BY bucket_time
        ORDER BY bucket_time ASC
    '''
    with get_db() as conn:
        cur = conn.cursor()
        cur.execute(query)
        data = [
            {
                "time": row['bucket_time'], 
                "count": row['count'], 
                "size": row['size'] if row['size'] is not None else 0
            } for row in cur.fetchall()
        ]
    return {"data": data}

@app.get("/api/analytics/word-cloud")
async def get_word_cloud(limit: int = 50):
    # Query the FTS5 Vocabulary table
    # Columns provided by fts5vocab 'row' mode: term, doc, cnt
    with get_db() as conn:
        cur = conn.cursor()
        try:
            cur.execute(f'''
                SELECT term, doc 
                FROM logs_fts_vocab
                WHERE length(term) > 3
                ORDER BY doc DESC 
                LIMIT ?
            ''', (limit,))
            words = [{"word": row['term'], "count": row['doc']} for row in cur.fetchall()]
        except Exception as e:
            print(f"Word cloud error: {e}")
            words = []
    return {"data": words}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)