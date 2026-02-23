"""Shared utility helpers."""

import time
import urllib.request


def do_healthcheck(endpoint: str, wait_dur_ms: int = 100, total_dur_ms: int = 10_000) -> None:
    deadline = time.time() + (total_dur_ms / 1000.0)
    while True:
        try:
            with urllib.request.urlopen(endpoint, timeout=2) as response:
                if 200 <= response.status < 300:
                    return
        except Exception:
            if time.time() >= deadline:
                raise RuntimeError(f"Healthcheck failed for {endpoint}")
            time.sleep(wait_dur_ms / 1000.0)
