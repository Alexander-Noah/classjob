import unittest
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from academic_system_config import load_academic_system_config


class AcademicSystemConfigTest(unittest.TestCase):
    def test_defaults_describe_current_school_adapter(self):
        config = load_academic_system_config({})

        self.assertEqual("https://jw.hniu.cn", config.academic_origin)
        self.assertEqual("6190757434612287", config.portal_app_id)

    def test_environment_can_replace_school_specific_endpoints(self):
        config = load_academic_system_config({
            "TIMETABLE_PORTAL_URL": "https://portal.example.edu/home",
            "TIMETABLE_PORTAL_APP_ID": "academic-service",
            "TIMETABLE_CLASS_PAGE_URL": "https://jw.example.edu/timetable/classes",
            "TIMETABLE_RESULT_URL": "https://jw.example.edu/timetable/query",
            "TIMETABLE_MODE_ID": "standard-periods",
        })

        self.assertEqual("https://portal.example.edu/home", config.portal_url)
        self.assertEqual("https://jw.example.edu", config.academic_origin)
        self.assertEqual("standard-periods", config.timetable_mode_id)

    def test_rejects_cross_origin_query_endpoint(self):
        with self.assertRaisesRegex(ValueError, "同一个教务系统来源"):
            load_academic_system_config({
                "TIMETABLE_CLASS_PAGE_URL": "https://jw.example.edu/timetable/classes",
                "TIMETABLE_RESULT_URL": "https://untrusted.example.net/query",
            })


if __name__ == "__main__":
    unittest.main()
