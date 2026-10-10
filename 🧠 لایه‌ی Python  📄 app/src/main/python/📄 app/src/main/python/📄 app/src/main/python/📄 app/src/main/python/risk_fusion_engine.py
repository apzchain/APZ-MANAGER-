```python
"""موتور فیوژن ریسک — نسخه‌ی سبک برای Android."""
from dataclasses import dataclass, field, asdict
from typing import Dict, List, Any, Optional
from logger import get_logger

log = get_logger(__name__)


DANGEROUS_PERMISSIONS = {
    "android.permission.READ_SMS": 20,
    "android.permission.RECEIVE_SMS": 15,
    "android.permission.SEND_SMS": 15,
    "android.permission.READ_CALL_LOG": 15,
    "android.permission.READ_CONTACTS": 10,
    "android.permission.RECORD_AUDIO": 10,
    "android.permission.CAMERA": 8,
    "android.permission.ACCESS_FINE_LOCATION": 8,
    "android.permission.READ_PHONE_STATE": 5,
    "android.permission.SYSTEM_ALERT_WINDOW": 12,
    "android.permission.REQUEST_INSTALL_PACKAGES": 15,
    "android.permission.BIND_ACCESSIBILITY_SERVICE": 20,
    "android.permission.PACKAGE_USAGE_STATS": 5,
}

DANGEROUS_COMBOS = [
    ({"READ_SMS", "INTERNET"}, 30, "ترکیب SMS + اینترنت (استخراج کلاسیک)"),
    ({"RECORD_AUDIO", "INTERNET"}, 20, "ضبط صدا + اینترنت"),
    ({"CAMERA", "INTERNET"}, 15, "دوربین + اینترنت"),
    ({"BIND_ACCESSIBILITY_SERVICE", "INTERNET"}, 30, "Accessibility Abuse"),
    ({"REQUEST_INSTALL_PACKAGES", "SYSTEM_ALERT_WINDOW"}, 25, "نصب + Overlay"),
]


@dataclass
class Finding:
    title_fa: str
    weight: int
    detail_fa: str
    evidence: List[str] = field(default_factory=list)


@dataclass
class RiskResult:
    score: int
    level: str
    confidence: int
    breakdown: Dict[str, float]
    findings: List[Finding]
    explanation: List[str]

    def to_dict(self):
        return {
            "score": self.score,
            "level": self.level,
            "confidence": self.confidence,
            "breakdown": self.breakdown,
            "findings": [asdict(f) for f in self.findings],
            "explanation": self.explanation,
        }


class RiskFusionEngine:
    WEIGHTS = {
        "manifest": 0.40,
        "yara": 0.30,
        "ioc": 0.20,
        "reputation": 0.10,
    }

    def fuse(
        self,
        manifest: Optional[Any] = None,
        yara_matches: List[Any] = None,
        iocs: Dict[str, List[str]] = None,
        reputation: Dict[str, Any] = None,
    ) -> RiskResult:
        breakdown: Dict[str, float] = {}
        findings: List[Finding] = []

        breakdown["manifest"], mf = self._score_manifest(manifest, findings)
        breakdown["yara"], yf = self._score_yara(yara_matches or [], findings)
        breakdown["ioc"], if_ = self._score_ioc(iocs or {}, findings)
        breakdown["reputation"], rf = self._score_reputation(reputation or {}, findings)

        weighted = sum(breakdown[k] * self.WEIGHTS[k] for k in self.WEIGHTS)
        # نرمال‌سازی به 0-100
        score = int(min(100, weighted * 2.5))
        level = self._level_from_score(score)
        confidence = self._compute_confidence(manifest, yara_matches, iocs, reputation)
        explanation = self._explain(breakdown, findings)

        return RiskResult(
            score=score,
            level=level,
            confidence=confidence,
            breakdown=breakdown,
            findings=findings,
            explanation=explanation,
        )

    def _score_manifest(self, manifest, findings: List[Finding]):
        if manifest is None:
            return 0.0, None
        perms = getattr(manifest, "permissions", []) or []
        score = 0.0

        dangerous = []
        for p in perms:
            w = DANGEROUS_PERMISSIONS.get(p, 0)
            if w > 0:
                score += w
                dangerous.append(p)

        if dangerous:
            findings.append(Finding(
                title_fa="مجوزهای حساس",
                weight=min(int(score), 100),
                detail_fa=f"{len(dangerous)} مجوز حساس شناسایی شد.",
                evidence=dangerous[:10],
            ))

        # combos
        short = {p.split(".")[-1] for p in perms}
        for combo, w, desc in DANGEROUS_COMBOS:
            if combo.issubset(short):
                score += w
                findings.append(Finding(
                    title_fa="ترکیب خطرناک مجوز",
                    weight=w,
                    detail_fa=desc,
                    evidence=sorted(combo),
                ))

        # debuggable
        if getattr(manifest, "debuggable", False):
            score += 20
            findings.append(Finding(
                title_fa="اپ دیباگ‌پذیر",
                weight=20,
                detail_fa="debuggable=true — امکان اجرای دستورات بدون امضا.",
                evidence=["android:debuggable=\"true\""],
            ))

        # exported components
        exported = getattr(manifest, "exported", []) or []
        if exported:
            score += min(len(exported) * 3, 20)
            findings.append(Finding(
                title_fa="کامپوننت‌های exported",
                weight=min(len(exported) * 3, 20),
                detail_fa=f"{len(exported)} کامپوننت در معرض دسترسی خارجی.",
                evidence=exported[:5],
            ))

        return min(score, 100.0), findings

    def _score_yara(self, matches, findings: List[Finding]):
        if not matches:
            return 0.0, None
        sev_weight = {"critical": 25, "high": 15, "medium": 8, "low": 3}
        score = sum(sev_weight.get(m.severity, 5) for m in matches)
        findings.append(Finding(
            title_fa="YARA Matches",
            weight=min(int(score), 100),
            detail_fa=f"{len(matches)} قانون YARA فعال شد.",
            evidence=[f"{m.rule} ({m.severity})" for m in matches[:10]],
        ))
        return min(score, 100.0), findings

    def _score_ioc(self, iocs: Dict[str, List[str]], findings: List[Finding]):
        if not iocs:
            return 0.0, None
        weights = {"url": 5, "domain": 3, "ip": 4, "sha256": 10, "md5": 8}
        score = sum(min(len(v) * weights.get(k, 1), 30) for k, v in iocs.items())
        total = sum(len(v) for v in iocs.values())
        if total:
            findings.append(Finding(
                title_fa="IOCهای استخراج‌شده",
                weight=min(int(score), 100),
                detail_fa=f"{total} IOC یافت شد.",
                evidence=[f"{k}: {len(v)}" for k, v in iocs.items()],
            ))
        return min(score, 100.0), findings

    def _score_reputation(self, rep: Dict[str, Any], findings: List[Finding]):
        if not rep:
            return 0.0, None
        score = 0.0
        detections = rep.get("detections", 0)
        total = rep.get("total", 0)
        if total > 0:
            score = (detections / total) * 100
        if detections > 0:
            findings.append(Finding(
                title_fa="اعتبارسنجی آنلاین",
                weight=int(score),
                detail_fa=f"{detections} از {total} موتور آنتی‌ویروس آن را مخرب تشخیص دادند.",
                evidence=[f"{k}: {v}" for k, v in rep.items() if k not in ("detections", "total")],
            ))
        return min(score, 100.0), findings

    def _level_from_score(self, score: int) -> str:
        if score >= 80:
            return "CRITICAL"
        if score >= 60:
            return "HIGH"
        if score >= 40:
            return "MEDIUM"
        if score >= 20:
            return "LOW"
        return "CLEAN"

    def _compute_confidence(self, manifest, yara, iocs, rep) -> int:
        score = 0
        if manifest is not None:
            score += 30
        if yara:
            score += 25
        if iocs:
            score += 25
        if rep:
            score += 20
        return min(score, 100)

    def _explain(self, breakdown, findings) -> List[str]:
        lines = []
        order = sorted(breakdown.items(), key=lambda x: -x[1])
        for cat, val in order:
            if val > 0:
                lines.append(f"{cat}: امتیاز {val:.0f}")
        return lines
```
