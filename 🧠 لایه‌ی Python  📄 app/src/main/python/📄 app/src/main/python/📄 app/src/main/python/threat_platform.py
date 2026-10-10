```python
"""YARA + IOC + MITRE mapping — نسخه‌ی Android."""
import os
import re
import json
from dataclasses import dataclass, field, asdict
from typing import List, Dict, Any
from logger import get_logger

log = get_logger(__name__)

IP_RE = re.compile(
    r"\b(?:(?:25[0-5]|2[0-4]\d|[01]?\d?\d)\.){3}(?:25[0-5]|2[0-4]\d|[01]?\d?\d)\b"
)
DOMAIN_RE = re.compile(r"\b(?:[a-z0-9-]+\.)+[a-z]{2,}\b", re.IGNORECASE)
URL_RE = re.compile(r"https?://[^\s\"'<>]+", re.IGNORECASE)
SHA256_RE = re.compile(r"\b[a-f0-9]{64}\b", re.IGNORECASE)
MD5_RE = re.compile(r"\b[a-f0-9]{32}\b", re.IGNORECASE)

MITRE_MAP = {
    "Android_Accessibility_Abuse": ("T1417", "Input Capture"),
    "Android_SMS_Interception": ("T1636", "Protected User Data"),
    "Android_Overlay_Attack": ("T1417", "Input Capture"),
    "Android_Dropper": ("T1407", "Download New Code at Runtime"),
    "Android_Reflection": ("T1407", "Download New Code at Runtime"),
    "Android_Crypto_Miner": ("T1496", "Resource Hijacking"),
    "Android_Command_Exec": ("T1623", "Command and Scripting Interpreter"),
    "Android_Credential_Harvest": ("T1417", "Input Capture"),
    "Android_Persistence_Receiver": ("T1624", "Event Triggered Execution"),
}


@dataclass
class YaraMatch:
    rule: str
    severity: str = "medium"
    tags: List[str] = field(default_factory=list)
    meta: Dict[str, Any] = field(default_factory=dict)


@dataclass
class ThreatResult:
    yara: List[YaraMatch] = field(default_factory=list)
    iocs: Dict[str, List[str]] = field(default_factory=dict)
    mitre: List[Dict[str, str]] = field(default_factory=list)

    def to_dict(self):
        return {
            "yara": [asdict(y) for y in self.yara],
            "iocs": self.iocs,
            "mitre": self.mitre,
        }


class ThreatPlatform:
    def __init__(self, rules_dir: str):
        self.rules_dir = rules_dir
        self._compiled = None

    def _compile_rules(self):
        if self._compiled is not None:
            return self._compiled
        try:
            import yara
        except ImportError:
            log.warning("yara-python not available")
            return None

        files = {}
        if os.path.isdir(self.rules_dir):
            for name in os.listdir(self.rules_dir):
                if name.endswith((".yar", ".yara")):
                    files[name] = os.path.join(self.rules_dir, name)

        if not files:
            log.warning(f"no YARA rules in {self.rules_dir}")
            return None

        try:
            self._compiled = yara.compile(filepaths=files)
            log.info(f"compiled {len(files)} YARA rule files")
        except Exception as e:
            log.error(f"YARA compile failed: {e}")
            self._compiled = None
        return self._compiled

    def scan(self, apk_path: str, extracted_strings: List[str] = None) -> ThreatResult:
        result = ThreatResult()
        rules = self._compile_rules()
        if rules is not None:
            try:
                matches = rules.match(apk_path, timeout=30)
                for m in matches:
                    result.yara.append(YaraMatch(
                        rule=m.rule,
                        severity=str(m.meta.get("severity", "medium")).lower(),
                        tags=list(m.tags),
                        meta=dict(m.meta),
                    ))
            except Exception as e:
                log.error(f"YARA match failed: {e}")

        # IOC استخراج از strings
        if extracted_strings:
            result.iocs = self._extract_iocs(extracted_strings)

        # MITRE mapping
        result.mitre = self._map_mitre(result.yara, result.iocs)
        return result

    def _extract_iocs(self, strings: List[str]) -> Dict[str, List[str]]:
        blob = "\n".join(strings[:5000])
        iocs: Dict[str, List[str]] = {}

        def uniq(xs, n=50):
            seen, out = set(), []
            for x in xs:
                xl = x.lower()
                if xl not in seen:
                    seen.add(xl)
                    out.append(x)
                    if len(out) >= n:
                        break
            return out

        iocs["url"] = uniq(URL_RE.findall(blob))[:30]
        iocs["ip"] = uniq([ip for ip in IP_RE.findall(blob)
                          if not ip.startswith(("0.", "127.", "255."))])[:30]
        iocs["domain"] = uniq([
            d for d in DOMAIN_RE.findall(blob)
            if not d.lower().endswith((".apk", ".png", ".jpg", ".xml", ".txt"))
        ])[:30]
        iocs["sha256"] = uniq(SHA256_RE.findall(blob))[:10]
        iocs["md5"] = uniq(MD5_RE.findall(blob))[:10]

        return {k: v for k, v in iocs.items() if v}

    def _map_mitre(self, yara_matches: List[YaraMatch],
                   iocs: Dict[str, List[str]]) -> List[Dict[str, str]]:
        seen, techniques = set(), []
        for m in yara_matches:
            if m.rule in MITRE_MAP:
                tid, name = MITRE_MAP[m.rule]
                if tid not in seen:
                    seen.add(tid)
                    techniques.append({"id": tid, "name": name})
        return techniques
```
