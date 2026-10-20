# 课表的 Python 脚本模块，入口及参数以 main/命令行解析和环境变量读取为准。
# 主要处理函数：resolve_semester。

from bs4 import BeautifulSoup


# 解析学期。
# 入参：configured_semester（configured学期）；timetable_page_html（课表页码HTML）。
# 返回：由当前表达式或分支生成的处理结果。
def resolve_semester(configured_semester, timetable_page_html):
    # 用户显式配置优先；缺省时读取学校页面选中项，不根据当前月份猜测学期。
    configured = str(configured_semester or "").strip()
    if configured:
        return configured

    soup = BeautifulSoup(timetable_page_html or "", "lxml")
    selector = (
        soup.find(id="xnxqh")
        or soup.find(attrs={"name": "xnxqh"})
        or soup.find(id="xnxq01id")
        or soup.find(attrs={"name": "xnxq01id"})
    )
    if selector is not None:
        selected = selector.find("option", selected=True)
        option = selected or selector.find("option", value=True)
        if option is not None:
            value = str(option.get("value") or option.get_text(" ", strip=True)).strip()
            if value:
                return value

    raise RuntimeError("缺少课表学期，且无法从教务系统课表页面自动识别，请保存课表采集配置")
