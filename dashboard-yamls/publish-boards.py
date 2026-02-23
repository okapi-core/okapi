#!/usr/bin/env python3
import json
import os
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError


def read_env(name: str) -> str:
    val = os.getenv(name)
    if not val:
        raise SystemExit(f"Missing env var: {name}")
    return val


def publish_yaml(base_url: str, token: str, yaml_path: Path) -> dict:
    url = f"{base_url.rstrip('/')}/api/v1/dashboards/yaml/apply"
    payload = {"yaml": yaml_path.read_text()}
    data = json.dumps(payload).encode("utf-8")
    req = Request(
        url,
        data=data,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "X-Okapi-Temp-Token": token,
        },
    )
    with urlopen(req) as resp:
        return json.loads(resp.read().decode("utf-8"))


def publish_version(base_url: str, token: str, dashboard_id: str, version_id: str) -> dict:
    url = f"{base_url.rstrip('/')}/api/v1/dashboards/{dashboard_id}/publish"
    payload = {"versionId": version_id}
    data = json.dumps(payload).encode("utf-8")
    req = Request(
        url,
        data=data,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "X-Okapi-Temp-Token": token,
        },
    )
    with urlopen(req) as resp:
        return json.loads(resp.read().decode("utf-8"))


def main() -> None:
    base_url = read_env("OKAPI_ENDPOINT")
    token = read_env("OKAPI_TEMP_TOKEN")

    root = Path(__file__).resolve().parent
    yaml_files = sorted(root.glob("*/dashboard.yaml"))
    if not yaml_files:
        raise SystemExit(f"No dashboard.yaml files found under {root}")

    for yaml_path in yaml_files:
        try:
            result = publish_yaml(base_url, token, yaml_path)
            if result.get("ok") and result.get("dashboardId") and result.get("versionId"):
                published = publish_version(
                    base_url, token, result["dashboardId"], result["versionId"]
                )
                print(f"{yaml_path}: apply={result} publish={published}")
            else:
                print(f"{yaml_path}: {result}")
        except HTTPError as e:
            body = e.read().decode("utf-8", errors="ignore")
            print(f"{yaml_path}: HTTP {e.code} {body}")
        except URLError as e:
            print(f"{yaml_path}: URL error {e}")


if __name__ == "__main__":
    main()
