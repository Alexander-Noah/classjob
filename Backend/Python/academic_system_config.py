from dataclasses import dataclass
import os
from urllib.parse import urlparse


@dataclass(frozen=True)
class AcademicSystemConfig:
    portal_url: str
    portal_app_id: str
    class_timetable_url: str
    class_timetable_result_url: str
    timetable_mode_id: str

    @property
    def academic_origin(self) -> str:
        parsed = urlparse(self.class_timetable_url)
        return f"{parsed.scheme}://{parsed.netloc}"

    def validate(self) -> "AcademicSystemConfig":
        named_urls = {
            "TIMETABLE_PORTAL_URL": self.portal_url,
            "TIMETABLE_CLASS_PAGE_URL": self.class_timetable_url,
            "TIMETABLE_RESULT_URL": self.class_timetable_result_url,
        }
        for name, value in named_urls.items():
            parsed = urlparse(value)
            if parsed.scheme not in {"http", "https"} or not parsed.netloc:
                raise ValueError(f"{name} 必须是有效的 HTTP(S) 地址")

        result = urlparse(self.class_timetable_result_url)
        page = urlparse(self.class_timetable_url)
        if (result.scheme, result.netloc) != (page.scheme, page.netloc):
            raise ValueError("课表页面和查询接口必须属于同一个教务系统来源")
        if not self.portal_app_id:
            raise ValueError("TIMETABLE_PORTAL_APP_ID 不能为空")
        if not self.timetable_mode_id:
            raise ValueError("TIMETABLE_MODE_ID 不能为空")
        return self


def load_academic_system_config(environ=None) -> AcademicSystemConfig:
    values = os.environ if environ is None else environ
    return AcademicSystemConfig(
        portal_url=values.get(
            "TIMETABLE_PORTAL_URL",
            "https://ehall.hniu.cn/new/index.html?browser=no",
        ).strip(),
        portal_app_id=values.get(
            "TIMETABLE_PORTAL_APP_ID",
            "6190757434612287",
        ).strip(),
        class_timetable_url=values.get(
            "TIMETABLE_CLASS_PAGE_URL",
            "https://jw.hniu.cn/jsxsd/kbcx/kbxx_xzb",
        ).strip(),
        class_timetable_result_url=values.get(
            "TIMETABLE_RESULT_URL",
            "https://jw.hniu.cn/jsxsd/kbcx/kbxx_xzb_ifr",
        ).strip(),
        timetable_mode_id=values.get(
            "TIMETABLE_MODE_ID",
            "67FB3A89FDC146ADA865DCC81B9EC143",
        ).strip(),
    ).validate()
