```python
"""entry-point اصلی برای Chaquopy — تحلیل APK روی Android."""
import hashlib
import json
import os
import re
import time
import zipfile
from pathlib import Path
from typing import Dict, Any, List

from logger import get_logger
from axml_parser import AXMLParser, ManifestInfo
from threat_platform import ThreatPlatform
from risk_fusion_engine import RiskFusionEngine
from reputation_engine import ReputationEngine
from database import Database
from ai_analyzer import AIAnalyzer

log = get_logger("apz.android")

PRINTABLE_RE = re.compile(rb"[\x20-\x7e]{6,}")


def _sha256(path: str, chunk: int = 1 << 20) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        while True:
            b = f.read(chunk)
            if not b:
                break
            h.update(b)
    return h.hexdigest()


def _extract_strings(apk_path: str, max_strings: int = 2000,
                     max_bytes: int = 30 * 1024 * 1024) -> List[str]:
    """استخراج strings از داخل APK بدون extract کامل."""
    out: List[str] = []
    try:
        with zipfile.ZipFile(apk_path, "r") as z:
            for info in z.infolist():
                if len(out) >= max_strings:
                    break
                # فقط فایل‌های متنی/باینری مهم
                if not info.filename.endswith((".dex", ".so", ".xml",
                                               ".json", ".txt", ".js",
                                               ".properties", ".bin")):
                    continue
                try:
                    with z.open(info) as fp:
                        data = fp.read(max_bytes)
                    for m in PRINTABLE_RE.finditer(data):
                        s = m.group().decode("ascii", errors="ignore")
                        if len(s) >= 6:
                            out.append(s)
                            if len(out) >= max_strings:
                                break
                except Exception:
                    continue
    except Exception as e:
        log.error(f"zip read failed: {e}")
    return out


class APZAnalyzer:
    def __init__(
        self,
        rules_dir: str,
        db_path: str,
        enable_yara: bool = True,
        enable_reputation: bool = False,
        enable_ai: bool = False,
        vt_key: str = "",
        ha_key: str = "",
        md_key: str = "",
        ollama_host: str = "",
        ollama_model: str = "qwen2.5:7b-instruct",
    ):
        self.rules_dir = rules_dir
        self.db = Database(db_path)
        self.risk_engine = RiskFusionEngine()
        self.threat = ThreatPlatform(rules_dir) if enable_yara else None
        self.reputation = (
            ReputationEngine(vt_key, ha_key, md_key)
            if enable_reputation and (vt_key or ha_key or md_key)
            else None
        )
        self.ai = AIAnalyzer(ollama_host, ollama_model) \
            if enable_ai and ollama_host else None

    # ---------- API ----------
    def analyze(self, apk_path: str, progress_cb=None) -> Dict[str, Any]:
        started = time.time()
        if not os.path.isfile(apk_path):
            raise FileNotFoundError(apk_path)

        if progress_cb:
            progress_cb("hash", 5)

        sha256 = _sha256(apk_path)
        size = os.path.getsize(apk_path)
        file_name = os.path.basename(apk_path)

        if progress_cb:
            progress_cb("manifest", 20)

        manifest = AXMLParser(apk_path).parse()

        if progress_cb:
            progress_cb("strings", 40)

        strings = _extract_strings(apk_path)

        threat_result = None
        if self.threat is not None:
            if progress_cb:
                progress_cb("yara", 60)
            threat_result = self.threat.scan(apk_path, strings)

        reputation = {}
        if self.reputation is not None:
            if progress_cb:
                progress_cb("reputation", 75)
            try:
                reputation = self.reputation.check(sha256)
            except Exception as e:
                log.warning(f"reputation failed: {e}")

        if progress_cb:
            progress_cb("fusion", 85)

        risk = self.risk_engine.fuse(
            manifest=manifest,
            yara_matches=(threat_result.yara if threat_result else []),
            iocs=(threat_result.iocs if threat_result else {}),
            reputation=reputation,
        )

        report: Dict[str, Any] = {
            "file_name": file_name,
            "sha256": sha256,
            "size_bytes": size,
            "risk_score": risk.score,
            "risk_level": risk.level,
            "confidence": risk.confidence,
            "breakdown": risk.breakdown,
            "findings": [f.__dict__ for f in risk.findings],
            "explanation": risk.explanation,
            "permissions": manifest.permissions,
            "exported": manifest.exported,
            "debuggable": manifest.debuggable,
            "package": manifest.package,
            "version_name": manifest.version_name,
            "min_sdk": manifest.min_sdk,
            "target_sdk": manifest.target_sdk,
            "threat": threat_result.to_dict() if threat_result else {
                "yara": [], "iocs": {}, "mitre": []
            },
            "reputation": reputation,
            "analysis_time_seconds": round(time.time() - started, 2),
            "limitations": [
                "تحلیل static/heuristic است و ممکن است برخی رفتارهای داینامیک شناسایی نشوند."
            ],
        }

        if self.ai is not None:
            if progress_cb:
                progress_cb("ai", 92)
            try:
                report["ai"] = {
                    "summary": self.ai.summarize(report),
                    "risk_explanation": self.ai.explain_risk(report),
                    "recommendations": self.ai.recommend(report),
                }
            except Exception as e:
                log.warning(f"AI failed: {e}")
                report["ai"] = {"error": str(e)}

        if progress_cb:
            progress_cb("save", 98)

        try:
            self.db.save(report)
        except Exception as e:
            log.warning(f"db save failed: {e}")

        if progress_cb:
            progress_cb("done", 100)

        return report

    def history(self, limit: int = 100, offset: int = 0):
        return self.db.list(limit, offset)

    def get_report(self, sha256: str):
        return self.db.get(sha256)

    def delete_report(self, sha256: str):
        self.db.delete(sha256)

    def stats(self):
        return self.db.stats()


# ---------- Facade برای Chaquopy ----------
_analyzer_singleton: APZAnalyzer = None  # type: ignore


def configure(config_json: str) -> str:
    """پیکربندی singleton از Kotlin."""
    global _analyzer_singleton
    cfg = json.loads(config_json)
    _analyzer_singleton = APZAnalyzer(
        rules_dir=cfg["rules_dir"],
        db_path=cfg["db_path"],
        enable_yara=cfg.get("enable_yara", True),
        enable_reputation=cfg.get("enable_reputation", False),
        enable_ai=cfg.get("enable_ai", False),
        vt_key=cfg.get("vt_key", ""),
        ha_key=cfg.get("ha_key", ""),
        md_key=cfg.get("md_key", ""),
        ollama_host=cfg.get("ollama_host", ""),
        ollama_model=cfg.get("ollama_model", "qwen2.5:7b-instruct"),
    )
    return json.dumps({"status": "ok"})


def analyze(apk_path: str) -> str:
    """تحلیل یک APK و بازگشت JSON."""
    if _analyzer_singleton is None:
        raise RuntimeError("analyzer not configured")
    return json.dumps(
        _analyzer_singleton.analyze(apk_path),
        ensure_ascii=False,
    )


def history(limit: int = 100, offset: int = 0) -> str:
    if _analyzer_singleton is None:
        return "[]"
    return json.dumps(_analyzer_singleton.history(limit, offset), ensure_ascii=False)


def get_report(sha256: str) -> str:
    if _analyzer_singleton is None:
        return "null"
    r = _analyzer_singleton.get_report(sha256)
    return json.dumps(r, ensure_ascii=False) if r else "null"


def delete_report(sha256: str) -> str:
    if _analyzer_singleton:
        _analyzer_singleton.delete_report(sha256)
    return json.dumps({"status": "ok"})


def stats() -> str:
    if _analyzer_singleton is None:
        return json.dumps({"total": 0, "by_risk_level": {}})
    return json.dumps(_analyzer_singleton.stats(), ensure_ascii=False)


def ai_check(host: str) -> bool:
    try:
        return AIAnalyzer(host).is_available()
    except Exception:
        return False
```
