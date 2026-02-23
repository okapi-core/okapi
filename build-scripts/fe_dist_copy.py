#!/usr/bin/env python3
import json
import os
import shutil
import sys


def main() -> int:
    setup_path = os.environ.get("FE_SETUP", "fe-setup.json")
    with open(setup_path, "r", encoding="utf-8") as handle:
        fe_root = json.load(handle)["fe_project_root"]
    dist_dir = os.path.join(fe_root, "dist")
    if not os.path.isdir(dist_dir):
        print(f"missing dist directory: {dist_dir}", file=sys.stderr)
        return 1

    target_dir = os.path.join("okapi-web", "src", "main", "resources", "public")
    if os.path.exists(target_dir):
        shutil.rmtree(target_dir)
    shutil.copytree(dist_dir, target_dir)
    print(f"copied {dist_dir} -> {target_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
