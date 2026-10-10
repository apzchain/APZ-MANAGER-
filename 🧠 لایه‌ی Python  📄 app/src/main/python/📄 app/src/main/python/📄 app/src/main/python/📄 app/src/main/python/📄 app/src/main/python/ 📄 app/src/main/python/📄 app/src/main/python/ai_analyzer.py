```python
"""AI Analyzer — سبک برای Android.

این ماژول فقط به سرور Ollama محلی/شبکه‌ای متصل می‌شود.
هیچ داده‌ای به سرویس ابری ارسال نمی‌شود.
"""
import json
import urllib.request
import urllib.error
from typing import Dict, Any, Optional
from logger import get_logger

log = get_logger(__name__)


SYSTEM_PROMPT = """You are a senior Android malware analyst.
Analyze the given APK static-analysis report.
Respond ONLY in Persian, concisely and technically.
Never invent IOCs or behaviors not present in the report."""


class AIAnalyzer:
    def __init__(self, host: str, model: str = "qwen2.5:7b-instruct"):
        self.host = host.rstrip("/")
        self.model = model

    def is_available(self) -> bool:
        try:
            req = urllib.request.Request(f"{self.host}/api/tags")
            with urllib.request.urlopen(req, timeout=5) as r:
                return r.status == 200
        except Exception:
            return False

    def summarize(self, report: Dict[str, Any]) -> str:
        prompt = self._build_prompt(report, mode="summary")
        return self._chat(prompt) or ""

    def explain_risk(self, report: Dict[str, Any]) -> str:
        prompt = self._build_prompt(report, mode="risk")
        return self._chat(prompt) or ""

    def recommend(self, report: Dict[str, Any]) -> str:
        prompt = self._build_prompt(report, mode="recommend")
        return self._chat(prompt) or ""

    def ask(self, report: Dict[str, Any], question: str) -> str:
        ctx = self._condensed(report)
        prompt = f"""گزارش APK:
{ctx}

سؤال کاربر: {question}

پاسخ کوتاه و فنی به فارسی بده."""
        return self._chat(prompt) or ""

    def _condensed(self, report: Dict[str, Any]) -> str:
        keep = {
            "file_name": report.get("file_name"),
            "sha256": report.get("sha256"),
            "risk_score": report.get("risk_score"),
            "risk_level": report.get("risk_level"),
            "permissions": report.get("permissions", [])[:15],
            "yara": [y.get("rule") for y in report.get("threat", {}).get("yara", [])][:10],
            "iocs": report.get("threat", {}).get("iocs", {}),
            "findings": [f.get("title_fa") for f in report.get("findings", [])],
        }
        return json.dumps(keep, ensure_ascii=False, indent=2)

    def _build_prompt(self, report: Dict[str, Any], mode: str) -> str:
        ctx = self._condensed(report)
        if mode == "summary":
            return f"این گزارش را در ۳ تا ۵ خط خلاصه کن:\n{ctx}"
        if mode == "risk":
            return f"دلیل اصلی ریسک را به ترتیب اهمیت توضیح بده:\n{ctx}"
        if mode == "recommend":
            return f"۳ تا ۵ توصیه‌ی امنیتی عملی بده:\n{ctx}"
        return ctx

    def _chat(self, prompt: str) -> Optional[str]:
        payload = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": prompt},
            ],
            "stream": False,
            "options": {"temperature": 0.2, "num_predict": 512},
        }
        try:
            data = json.dumps(payload).encode()
            req = urllib.request.Request(
                f"{self.host}/api/chat",
                data=data,
                headers={"Content-Type": "application/json"},
                method="POST",
            )
            with urllib.request.urlopen(req, timeout=120) as r:
                out = json.loads(r.read())
            return out.get("message", {}).get("content", "")
        except urllib.error.HTTPError as e:
            log.error(f"Ollama HTTP {e.code}")
        except Exception as e:
            log.error(f"Ollama error: {e}")
        return None
```
