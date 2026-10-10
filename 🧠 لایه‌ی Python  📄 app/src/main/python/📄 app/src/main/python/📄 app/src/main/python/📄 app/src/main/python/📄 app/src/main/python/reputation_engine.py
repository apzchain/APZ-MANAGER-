```python
"""اعتبارسنجی آنلاین — سبک برای Android."""
import json
import urllib.request
import urllib.error
from typing import Dict, Any
from logger import get_logger

log = get_logger(__name__)


class ReputationEngine:
    def __init__(self, vt_key: str = "", ha_key: str = "", md_key: str = ""):
        self.vt_key = vt_key.strip()
        self.ha_key = ha_key.strip()
        self.md_key = md_key.strip()

    def check(self, sha256: str) -> Dict[str, Any]:
        result: Dict[str, Any] = {}
        if self.vt_key:
            vt = self._virustotal(sha256)
            if vt:
                result.update(vt)
        if self.md_key:
            md = self._metadefender(sha256)
            if md:
                result.update(md)
        # HA معمولاً نیاز به submit دارد، فعلاً skip
        return result

    def _virustotal(self, sha256: str) -> Dict[str, Any]:
        url = f"https://www.virustotal.com/api/v3/files/{sha256}"
        req = urllib.request.Request(url, headers={"x-apikey": self.vt_key})
        try:
            with urllib.request.urlopen(req, timeout=20) as r:
                data = json.loads(r.read())
            stats = data.get("data", {}).get("attributes", {}) \
                        .get("last_analysis_stats", {})
            return {
                "detections": stats.get("malicious", 0) + stats.get("suspicious", 0),
                "total": sum(stats.values()) if stats else 0,
                "source": "virustotal",
            }
        except urllib.error.HTTPError as e:
            if e.code == 404:
                return {"detections": 0, "total": 0, "source": "virustotal", "note": "not_found"}
            log.warning(f"VT error {e.code}")
        except Exception as e:
            log.warning(f"VT exception: {e}")
        return {}

    def _metadefender(self, sha256: str) -> Dict[str, Any]:
        url = f"https://api.metadefender.com/v4/hash/{sha256}"
        req = urllib.request.Request(url, headers={"apikey": self.md_key})
        try:
            with urllib.request.urlopen(req, timeout=20) as r:
                data = json.loads(r.read())
            scans = data.get("scan_results", {})
            total = scans.get("total_scans", 0)
            detections = scans.get("total_detected_avs", 0)
            return {
                "detections": detections,
                "total": total,
                "source": "metadefender",
            }
        except Exception as e:
            log.warning(f"MD exception: {e}")
        return {}
```
