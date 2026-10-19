"""将前后端真实行覆盖率写入 Actions 摘要；报告缺失时返回失败。"""

import json
import os
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


def summarize(module):
    """读取全量源码报告，避免把测试通过率当作代码覆盖率。"""
    if module == "backend":
        report = Path("Backend/target/site/jacoco/jacoco.xml")
        counter = ET.parse(report).getroot().find("counter[@type='LINE']")
        if counter is None:
            raise ValueError("JaCoCo report has no bundle LINE counter")
        covered = int(counter.attrib["covered"])
        total = covered + int(counter.attrib["missed"])
    elif module == "frontend":
        report = Path("web/coverage/coverage-summary.json")
        lines = json.loads(report.read_text(encoding="utf-8"))["total"]["lines"]
        covered, total = lines["covered"], lines["total"]
    else:
        raise ValueError(f"Unknown module: {module}")
    if total <= 0:
        raise ValueError("Coverage report contains no executable source lines")
    percentage = covered / total * 100
    passed = covered * 100 >= total * 96
    text = (
        f"### {module} coverage\n\n"
        f"| Metric | Result | Required |\n| --- | --- | --- |\n"
        f"| Source line coverage | {percentage:.2f}% ({covered}/{total}) | 96% |\n\n"
        f"Gate: {'PASS' if passed else 'FAIL'}\n"
    )
    print(text)
    if summary_path := os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(summary_path, "a", encoding="utf-8") as summary:
            summary.write(text)
    return 0 if passed else 1


if __name__ == "__main__":
    try:
        sys.exit(summarize(sys.argv[1]))
    except (OSError, ValueError, KeyError, ET.ParseError, IndexError) as error:
        print(f"Coverage summary unavailable: {error}", file=sys.stderr)
        sys.exit(1)
