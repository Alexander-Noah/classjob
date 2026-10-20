"""Generate a Shields endpoint from both real reports; never substitute a threshold."""

import importlib.util
import json
import os
from pathlib import Path


def generate():
    spec = importlib.util.spec_from_file_location("coverage_summary", Path(__file__).with_name("coverage-summary.py"))
    summary = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(summary)
    modules = {}
    for name in ("backend", "frontend"):
        covered, total = summary.read_coverage(name)
        if covered * 100 < total * 96:
            raise ValueError(f"{name} coverage is below the 96% gate")
        modules[name] = {"covered": covered, "total": total, "percent": covered / total * 100}
    covered = sum(item["covered"] for item in modules.values())
    total = sum(item["total"] for item in modules.values())
    badge = {
        "schemaVersion": 1,
        "label": "Line coverage",
        "message": f"{covered / total * 100:.2f}%",
        "color": "brightgreen",
        "cacheSeconds": 300,
        "covered": covered,
        "total": total,
        "modules": modules,
        "commit": os.environ.get("GITHUB_SHA", "local"),
        "runUrl": f"{os.environ.get('GITHUB_SERVER_URL', 'https://github.com')}/{os.environ.get('GITHUB_REPOSITORY', '')}/actions/runs/{os.environ.get('GITHUB_RUN_ID', '')}",
    }
    output = Path("coverage-badge")
    output.mkdir(exist_ok=True)
    endpoint = {key: badge[key] for key in ("schemaVersion", "label", "message", "color", "cacheSeconds")}
    (output / "coverage.json").write_text(json.dumps(endpoint, indent=2) + "\n", encoding="utf-8")
    (output / "report.json").write_text(json.dumps(badge, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(badge, indent=2))


if __name__ == "__main__":
    generate()
