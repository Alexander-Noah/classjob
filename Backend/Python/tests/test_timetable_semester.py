# 课表的 Python 单元测试，测试替身用于隔离浏览器或外部服务。
# 主要处理函数：load_module。

import importlib.util
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).resolve().parents[1] / "timetable_semester.py"


def load_module(test_case):
    test_case.assertTrue(MODULE_PATH.exists(), "timetable_semester.py must exist")
    spec = importlib.util.spec_from_file_location("timetable_semester", MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class TimetableSemesterTest(unittest.TestCase):
    def test_prefers_saved_semester(self):
        module = load_module(self)

        semester = module.resolve_semester("2025-2026-2", "<html></html>")

        self.assertEqual("2025-2026-2", semester)

    def test_reads_selected_semester_from_timetable_page(self):
        module = load_module(self)
        html = """
            <select id="xnxq01id">
              <option value="2025-2026-2">2025-2026-2</option>
              <option value="2026-2027-1" selected>2026-2027-1</option>
            </select>
        """

        semester = module.resolve_semester("", html)

        self.assertEqual("2026-2027-1", semester)

    def test_rejects_missing_semester_when_page_has_no_selector(self):
        module = load_module(self)

        with self.assertRaisesRegex(RuntimeError, "课表学期"):
            module.resolve_semester("", "<html><body>no semester</body></html>")


if __name__ == "__main__":
    unittest.main()
