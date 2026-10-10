"""دیکودر سبک AndroidManifest با androguard."""
from dataclasses import dataclass, field, asdict
from typing import List, Dict, Any
from logger import get_logger

log = get_logger(__name__)


@dataclass
class ManifestInfo:
    package: str = ""
    version_name: str = ""
    version_code: int = 0
    min_sdk: int = 0
    target_sdk: int = 0
    permissions: List[str] = field(default_factory=list)
    activities: List[str] = field(default_factory=list)
    services: List[str] = field(default_factory=list)
    receivers: List[str] = field(default_factory=list)
    providers: List[str] = field(default_factory=list)
    exported: List[str] = field(default_factory=list)
    main_activity: str = ""
    debuggable: bool = False
    allow_backup: bool = True
    uses_cleartext: bool = True

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


class AXMLParser:
    """دیکودر Manifest با fallback به androguard."""

    def __init__(self, apk_path: str):
        self.apk_path = apk_path

    def parse(self) -> ManifestInfo:
        try:
            from androguard.core.apk import APK  # v4
        except ImportError:
            try:
                from androguard.core.bytecodes.apk import APK  # v3
            except ImportError as e:
                log.error(f"androguard not available: {e}")
                return ManifestInfo()

        try:
            apk = APK(self.apk_path)
            info = ManifestInfo(
                package=apk.get_package() or "",
                version_name=apk.get_androidversion_name() or "",
                version_code=apk.get_androidversion_code() or 0,
                min_sdk=apk.get_min_sdk_version() or 0,
                target_sdk=apk.get_target_sdk_version() or 0,
                permissions=apk.get_permissions() or [],
                activities=apk.get_activities() or [],
                services=apk.get_services() or [],
                receivers=apk.get_receivers() or [],
                providers=apk.get_providers() or [],
            )
            info.main_activity = apk.get_main_activity() or ""
            info.exported = self._exported_components(apk)
            info.debuggable = self._is_debuggable(apk)
            return info
        except Exception as e:
            log.error(f"AXML parse failed: {e}")
            return ManifestInfo()

    def _exported_components(self, apk) -> List[str]:
        try:
            return list(apk.get_all_attribute_value(
                "activity", "exported", format_value=True
            ) or [])
        except Exception:
            return []

    def _is_debuggable(self, apk) -> bool:
        try:
            return str(apk.get_element("application", "debuggable")).lower() == "true"
        except Exception:
            return False
