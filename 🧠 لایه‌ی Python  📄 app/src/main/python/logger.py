"""لاگر سبک برای APZ Android."""
import logging
import sys

_LOGGER_NAME = "apz"
_configured = False


def get_logger(name: str = _LOGGER_NAME) -> logging.Logger:
    global _configured
    if not _configured:
        root = logging.getLogger(_LOGGER_NAME)
        root.setLevel(logging.INFO)
        if not root.handlers:
            handler = logging.StreamHandler(sys.stdout)
            handler.setFormatter(
                logging.Formatter("%(asctime)s [%(levelname)s] %(name)s: %(message)s")
            )
            root.addHandler(handler)
        _configured = True
    return logging.getLogger(name)
