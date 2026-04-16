import sqlite3
import json
import os
import re
from collections import Counter
from contextlib import contextmanager
from fastapi import FastAPI, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse, HTMLResponse, FileResponse

app = FastAPI(title="Log Intelligence API")


# --- Regex Patterns ---
UUID_PATTERN = re.compile(r'\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b', re.IGNORECASE)
FORM_CONTEXT_PATTERN = re.compile(r'Form-->\s*Cxt:-(.*?)\s+Id', re.IGNORECASE)

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
    file_path = "view.html"
    return FileResponse(file_path)

@app.get("/view/docs", response_class=HTMLResponse)
def view_logs():
    file_path = "docs.html"
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
async def get_time_distribution(
    resolution: str = Query(
        "1m", 
        enum=["1m", "5m", "15m", "30m", "1h"],
        description="Time bucket resolution"
    )
):
    # Determine the SQLite expression for bucketing based on resolution
    # Timestamp format: "YYYY-MM-DD HH:MM:SS,ms"
    # substr 1-10: Date
    # substr 12-13: Hour
    # substr 15-16: Minute
    
    if resolution == "1m":
        # Standard minute grouping: "YYYY-MM-DD HH:MM"
        bucket_expr = "substr(timestamp, 1, 16)"
    
    elif resolution == "5m":
        # "YYYY-MM-DD HH:" + floor(Minute / 5) * 5
        bucket_expr = "substr(timestamp, 1, 14) || printf('%02d', (CAST(substr(timestamp, 15, 2) AS INTEGER) / 5) * 5)"
        
    elif resolution == "15m":
        # "YYYY-MM-DD HH:" + floor(Minute / 15) * 15
        bucket_expr = "substr(timestamp, 1, 14) || printf('%02d', (CAST(substr(timestamp, 15, 2) AS INTEGER) / 15) * 15)"
        
    elif resolution == "30m":
        # "YYYY-MM-DD HH:" + floor(Minute / 30) * 30
        bucket_expr = "substr(timestamp, 1, 14) || printf('%02d', (CAST(substr(timestamp, 15, 2) AS INTEGER) / 30) * 30)"
        
    elif resolution == "1h":
        # "YYYY-MM-DD HH:00"
        bucket_expr = "substr(timestamp, 1, 13) || ':00'"
        
    else:
        # Fallback to 1m
        bucket_expr = "substr(timestamp, 1, 16)"

    query = f'''
        SELECT 
            {bucket_expr} as bucket_time, 
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

@app.get("/api/analytics/patterns")
async def get_patterns():
    """
    Extracts UUIDs and Form Contexts from the raw logs using Python Regex.
    Note: This scans the DB which may be intensive for huge datasets.
    """
    uuids = Counter()
    forms = Counter()

    with get_db() as conn:
        cur = conn.cursor()
        # Optimization: Only select the 'raw' column to reduce memory usage
        cur.execute("SELECT raw FROM logs")
        
        # Iterate over rows to extract patterns
        # Note: For massive DBs (millions of rows), this should be done in a background worker
        # or pre-calculated during ingestion. For standard usage, this is acceptable.
        for row in cur:
            raw_text = row['raw']
            if not raw_text: continue
            
            # Find UUIDs
            found_uuids = UUID_PATTERN.findall(raw_text)
            for u in found_uuids:
                uuids[u.lower()] += 1

            # Find Form Contexts
            found_forms = FORM_CONTEXT_PATTERN.findall(raw_text)
            for f in found_forms:
                forms[f.strip()] += 1

    # Convert Counters to sorted lists
    sorted_uuids = [{"id": k, "count": v} for k, v in uuids.most_common(50)]
    sorted_forms = [{"context": k, "count": v} for k, v in forms.most_common(50)]

    return {
        "uuids": sorted_uuids,
        "forms": sorted_forms
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)