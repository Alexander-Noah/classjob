# 课表的 Python 单元测试，测试替身用于隔离浏览器或外部服务。

import unittest
from pathlib import Path


BASE_DIR = Path(__file__).resolve().parents[1]
PROJECT_BACKEND = BASE_DIR.parent


class TimetableImportBoundaryTest(unittest.TestCase):
    def test_python_adapter_only_normalizes_files(self):
        source = (BASE_DIR / "parse_and_import_timetable.py").read_text(encoding="utf-8")

        self.assertNotIn("pymysql", source)
        self.assertNotIn("DB_CONFIG", source)
        self.assertNotIn("CLEAR_OLD_DATA", source)
        self.assertNotIn("import_dataframe_to_mysql", source)
        self.assertIn("标准 CSV 已导出", source)

    def test_database_driver_is_not_a_crawler_dependency(self):
        requirements = (BASE_DIR / "requirements.txt").read_text(encoding="utf-8")
        self.assertNotIn("pymysql", requirements.lower())

    def test_java_service_owns_the_atomic_database_import(self):
        source = (
            PROJECT_BACKEND
            / "src/main/java/org/example/backend/service/impl/ClassTimetableServiceImpl.java"
        ).read_text(encoding="utf-8")

        self.assertIn("importStandardCsv", source)
        self.assertIn("deleteBySemester", source)


if __name__ == "__main__":
    unittest.main()
