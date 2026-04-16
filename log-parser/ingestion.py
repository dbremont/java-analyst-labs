import sqlite3
import os
import re
from datetime import datetime

# Configuration
LOG_FILE = "server.log"
DB_FILE = "logs.db"
BATCH_SIZE = 10000  # Insert in batches for speed

# Regex Pattern
LOG_PATTERN = re.compile(
    r'^(?P<timestamp>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3})\s+'
    r'(?P<level>INFO|DEBUG|WARN|ERROR)\s+'
    r'\[(?P<logger>[^\]]+)\]\s+'
    r'\((?P<thread>[^)]+)\)\s+'
    r'(?P<message>.*)$'
)

def create_database():
    if os.path.exists(DB_FILE):
        print(f"Removing old database: {DB_FILE}")
        os.remove(DB_FILE)

    print(f"Creating new database: {DB_FILE}")
    conn = sqlite3.connect(DB_FILE)
    cur = conn.cursor()

    # 1. Create Main Table
    cur.execute('''
        CREATE TABLE logs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            timestamp TEXT,
            level TEXT,
            logger TEXT,
            thread TEXT,
            message TEXT,
            raw TEXT
        )
    ''')

    # 2. Create FTS5 Virtual Table (Inverted Index for Search)
    # Note: Added 'tokenize="porter unicode61"' to handle word stemming (e.g., "error" matches "errors")
    cur.execute('''
        CREATE VIRTUAL TABLE logs_fts USING fts5(
            message, logger, 
            content='logs', 
            content_rowid='id',
            tokenize="porter unicode61"
        )
    ''')

    # 3. Create Vocabulary Table (Required for Word Cloud Analytics)
    # This creates a view into the FTS index listing all terms and their counts.
    cur.execute('''
        CREATE VIRTUAL TABLE logs_fts_vocab USING fts5vocab(
            logs_fts, 'row'
        )
    ''')

    # 4. Create Triggers to keep FTS in sync automatically
    cur.execute('''
        CREATE TRIGGER logs_ai AFTER INSERT ON logs BEGIN
            INSERT INTO logs_fts(rowid, message, logger) 
            VALUES (new.id, new.message, new.logger);
        END;
    ''')
    
    # Optimization: WAL Mode for better performance
    cur.execute("PRAGMA journal_mode=WAL")
    cur.execute("PRAGMA synchronous=NORMAL")

    conn.commit()
    return conn

def parse_line(line):
    match = LOG_PATTERN.match(line.strip())
    if match:
        data = match.groupdict()
        return (
            data['timestamp'], 
            data['level'], 
            data['logger'],
            data['thread'],
            data['message'], 
            line.strip()
        )
    return None

def ingest_logs():
    if not os.path.exists(LOG_FILE):
        print(f"Error: {LOG_FILE} not found. Please create a log file first.")
        return

    conn = create_database()
    cur = conn.cursor()
    
    batch = []
    total_count = 0
    
    print(f"Starting ingestion of {LOG_FILE}...")

    with open(LOG_FILE, 'r', encoding='utf-8', errors='ignore') as f:
        for line in f:
            parsed = parse_line(line)
            if parsed:
                batch.append(parsed)
            
            if len(batch) >= BATCH_SIZE:
                cur.executemany(
                    "INSERT INTO logs (timestamp, level, logger, thread, message, raw) VALUES (?, ?, ?, ?, ?, ?)", 
                    batch
                )
                conn.commit()
                total_count += len(batch)
                print(f"Processed {total_count} lines...", end='\r')
                batch = []

        # Insert remaining
        if batch:
            cur.executemany(
                "INSERT INTO logs (timestamp, level, logger, thread, message, raw) VALUES (?, ?, ?, ?, ?, ?)", 
                batch
            )
            conn.commit()
            total_count += len(batch)

    # Optimize database after ingestion
    print("\nOptimizing database (VACUUM)...")
    cur.execute("INSERT INTO logs_fts(logs_fts) VALUES('optimize')")
    conn.commit()
    conn.close()
    
    print(f"Done. Total records inserted: {total_count}")

if __name__ == "__main__":
    ingest_logs()