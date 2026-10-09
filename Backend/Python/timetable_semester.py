from bs4 import BeautifulSoup


def resolve_semester(configured_semester, timetable_page_html):
    configured = str(configured_semester or "").strip()
    if configured:
        return configured

    soup = BeautifulSoup(timetable_page_html or "", "lxml")
    selector = soup.find(id="xnxq01id") or soup.find(attrs={"name": "xnxq01id"})
    if selector is not None:
        selected = selector.find("option", selected=True)
        option = selected or selector.find("option", value=True)
        if option is not None:
            value = str(option.get("value") or option.get_text(" ", strip=True)).strip()
            if value:
                return value

    raise RuntimeError("缺少课表学期，且无法从教务系统课表页面自动识别，请保存课表采集配置")
