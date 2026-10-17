# 将爬虫导出的 HTML/Excel 规范化为标准 CSV；脚本名称保留历史兼容，实际入库由 Java 完成。
from bs4 import BeautifulSoup
import pandas as pd
import re
import os
from pathlib import Path


# =========================
# 文件配置
# =========================

BASE_DIR = Path(__file__).resolve().parent

# 你的爬虫导出文件夹
OUTPUT_DIR = BASE_DIR / "课表导出结果"

# 优先读取：课表导出结果/班级课表大表结果.html
HTML_FILE = OUTPUT_DIR / "班级课表大表结果.html"

# 标准化后的导出文件
OUTPUT_EXCEL = OUTPUT_DIR / "班级课表标准数据.xlsx"
OUTPUT_CSV = OUTPUT_DIR / "班级课表标准数据.csv"

SEMESTER = os.getenv("TIMETABLE_SEMESTER", "").strip()
if not SEMESTER:
    raise RuntimeError("缺少 TIMETABLE_SEMESTER，无法保证课表与学期配置一致")


WEEKDAYS = [
    "星期一",
    "星期二",
    "星期三",
    "星期四",
    "星期五",
    "星期六",
    "星期日",
]

SECTIONS = [
    "0102",
    "0304",
    "0506",
    "0708",
    "0910",
    "1112",
]


# =========================
# 文本处理
# =========================

def clean_text(text):
    if text is None:
        return ""

    text = str(text)
    text = text.replace("\xa0", " ")
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def to_int(value):
    if value is None:
        return None

    value = str(value).strip()

    if value == "" or value.lower() == "nan":
        return None

    try:
        return int(float(value))
    except Exception:
        return None


def clean(value):
    if value is None:
        return ""

    value = str(value).strip()

    if value.lower() == "nan":
        return ""

    return value


# =========================
# 课表解析
# =========================

def parse_section(section_text):
    """
    0102 -> 01-02节
    0304 -> 03-04节
    """
    section_text = str(section_text).strip()

    if len(section_text) == 4 and section_text.isdigit():
        start = section_text[:2]
        end = section_text[2:]
        return f"{start}-{end}节", int(start), int(end)

    return section_text, None, None


def expand_weeks(week_text):
    """
    16-17周 -> 16,17
    3-5,7-9,11-14周 -> 3,4,5,7,8,9,11,12,13,14
    """
    if not week_text:
        return ""

    week_text = (
        str(week_text)
        .replace("(", "")
        .replace(")", "")
        .replace("周", "")
        .strip()
    )

    result = []

    for part in week_text.split(","):
        part = part.strip()

        if not part:
            continue

        if "-" in part:
            start, end = part.split("-", 1)

            if start.isdigit() and end.isdigit():
                result.extend(range(int(start), int(end) + 1))
        else:
            if part.isdigit():
                result.append(int(part))

    return ",".join(map(str, result))


def parse_course_div(div):
    """
    解析一个 div.kbcontent1

    常见格式：
    物联网产品开发实训
    物联网2401班
    欧泽强
    (16-17周)
    15-501-通信系统设计室
    """
    text = div.get_text("\n")

    lines = []

    for line in text.splitlines():
        line = clean_text(line)

        if line and line != "&nbsp;":
            lines.append(line)

    if len(lines) < 4:
        return None

    course_name = lines[0]
    class_name = lines[1] if len(lines) > 1 else ""
    teacher = lines[2] if len(lines) > 2 else ""

    week_raw = ""
    classroom = ""

    for line in lines[3:]:
        if "周" in line:
            week_raw = line
        else:
            if classroom:
                classroom += " " + line
            else:
                classroom = line

    week_text = (
        week_raw
        .replace("(", "")
        .replace(")", "")
        .replace("周", "")
        .strip()
    )

    return {
        "课程名称": course_name,
        "班级": class_name,
        "教师": teacher,
        "周次原文": week_raw,
        "周次": week_text,
        "展开周次": expand_weeks(week_raw),
        "教室": classroom,
    }


def parse_big_timetable(html):
    soup = BeautifulSoup(html, "lxml")

    table = soup.find("table", id="timetable")

    if not table:
        raise Exception("没有找到 table#timetable，请确认 HTML 文件是否正确")

    rows = table.find_all("tr")

    result = []

    # 前两行是表头，从第 3 行开始是班级数据
    data_rows = rows[2:]

    for row in data_rows:
        cells = row.find_all(["td", "th"], recursive=False)

        if len(cells) < 2:
            continue

        row_class_name = clean_text(cells[0].get_text())

        if not row_class_name:
            continue

        # 7天 * 6节 = 42个课表格子
        course_cells = cells[1:43]

        for index, cell in enumerate(course_cells):
            day_index = index // 6
            section_index = index % 6

            if day_index >= len(WEEKDAYS):
                continue

            weekday = WEEKDAYS[day_index]
            section_code = SECTIONS[section_index]
            section_text, start_section, end_section = parse_section(section_code)

            divs = cell.find_all("div", class_="kbcontent1")

            for div in divs:
                course = parse_course_div(div)

                if not course:
                    continue

                item = {
                    "学年学期": SEMESTER,
                    "行班级": row_class_name,
                    "班级": course["班级"] or row_class_name,
                    "星期": weekday,
                    "节次代码": section_code,
                    "节次": section_text,
                    "开始节次": start_section,
                    "结束节次": end_section,
                    "课程名称": course["课程名称"],
                    "教师": course["教师"],
                    "周次原文": course["周次原文"],
                    "周次": course["周次"],
                    "展开周次": course["展开周次"],
                    "教室": course["教室"],
                }

                result.append(item)

    return result


# =========================
# 主程序
# =========================

def main():
    if not HTML_FILE.exists():
        print(f"没有找到文件：{HTML_FILE}")
        print("请先运行爬虫，生成：课表导出结果/班级课表大表结果.html")
        return

    print("正在读取 HTML：", HTML_FILE)

    html = HTML_FILE.read_text(encoding="utf-8")

    data = parse_big_timetable(html)

    df = pd.DataFrame(data)

    print("解析到课程记录数量：", len(df))

    if df.empty:
        print("没有解析到课程，请检查 HTML 内容")
        return

    print(df.head())

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    df.to_excel(OUTPUT_EXCEL, index=False)
    df.to_csv(OUTPUT_CSV, index=False, encoding="utf-8-sig")

    print("标准 Excel 已导出：", OUTPUT_EXCEL)
    print("标准 CSV 已导出：", OUTPUT_CSV)

    print("标准化完成；数据库写入由后端 JDBC 导入模块统一执行。")


if __name__ == "__main__":
    main()
