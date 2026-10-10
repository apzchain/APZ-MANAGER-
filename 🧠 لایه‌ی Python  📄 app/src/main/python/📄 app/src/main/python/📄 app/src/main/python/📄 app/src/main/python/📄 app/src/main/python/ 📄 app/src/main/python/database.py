```python
"""SQLite تاریخچه — سبک برای Android."""
import sqlite3
import json
from pathlib import Path
from typing import List, Dict, Any, Optional
from logger import get_logger

log = get_logger(__name__)


SCHEMA = """
CREATE TABLE IF NOT EXISTS analyses (
    sha256 TEXT PRIMARY KEY,
    file_name TEXT,
    size_bytes INTEGER,
    risk_score INTEGER,
    risk_level TEXT,
    confidence INTEGER,
    report_json TEXT,
    analyzed_at TEXT DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_risk_level ON analyses(risk_level);
CREATE INDEX IF NOT EXISTS idx_analyzed_at ON analyses(analyzed_at);
"""


class Database:
    def __init__(self, db_path: str):
        self.db_path = db_path
        Path(db_path).parent.mkdir(parents=True, exist_ok=True)
        self._init()

    def _conn(self):
        c = sqlite3.connect(self.db_path, timeout=10)
        c.row_factory = sqlite3.Row
        return c

    def _init(self):
        with self._conn() as c:
            c.executescript(SCHEMA)

    def save(self, report: Dict[str, Any]):
        with self._conn() as c:
            c.execute(
                """INSERT OR REPLACE INTO analyses
                   (sha256, file_name, size_bytes, risk_score, risk_level,
                    confidence, report_json)
                   VALUES (?, ?, ?, ?, ?, ?, ?)""",
                (
                    report.get("sha256", ""),
                    report.get("file_name", ""),
                    int(report.get("size_bytes", 0)),
                    int(report.get("risk_score", 0)),
                    report.get("risk_level", "CLEAN"),
                    int(report.get("confidence", 0)),
                    json.dumps(report, ensure_ascii=False),
                ),
            )

    def get(self, sha256: str) -> Optional[Dict[str, Any]]:
        with self._conn() as c:
            row = c.execute(
                "SELECT report_json FROM analyses WHERE sha256 = ?",
                (sha256,),
            ).fetchone()
        return json.loads(row["report_json"]) if row else None

    def list(self, limit: int = 100, offset: int = 0) -> List[Dict[str, Any]]:
        with self._conn() as c:
            rows = c.execute(
                """SELECT sha256, file_name, risk_score, risk_level, analyzed_at
                   FROM analyses
                   ORDER BY analyzed_at DESC
                   LIMIT ? OFFSET ?""",
                (limit, offset),
            ).fetchall()
        return [dict(r) for r in rows]

    def delete(self, sha256: str):
        with self._conn() as c:
            c.execute("DELETE FROM analyses WHERE sha256 = ?", (sha256,))

    def stats(self) -> Dict[str, Any]:
        with self._conn() as c:
            total = c.execute("SELECT COUNT(*) FROM analyses").fetchone()[0]
            by_level = dict(c.execute(
                "SELECT risk_level, COUNT(*) FROM analyses GROUP BY risk_level"
            ).fetchall())
        return {"total": total, "by_risk_level": by_level}
```
