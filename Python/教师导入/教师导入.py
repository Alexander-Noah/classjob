# 教师导入的 Python 脚本模块，入口及参数以 main/命令行解析和环境变量读取为准。
# 主要处理函数：generate_digits、generate_username、generate_password、split_teacher_names、username_exists 等。

import csv
import secrets
from datetime import datetime

import pymysql


# =========================
# 数据库配置：按你的实际情况修改
# =========================
DB_CONFIG = {
    "host": "127.0.0.1",
    "port": 3306,
    "user": "root",
    "password": "root",
    "database": "smart_lab_basic",
    "charset": "utf8mb4",
    "cursorclass": pymysql.cursors.DictCursor,
}


# =========================
# 默认配置
# =========================
USERNAME_LENGTH = 8
PASSWORD_LENGTH = 8
DEFAULT_DEPARTMENT = "软件学院"
DEFAULT_STATUS = 1

# 你的系统如果第一次登录会自动把明文密码转成加密密码，这里保存明文初始密码即可
# 这里不要加 teacher，不要加中文，只生成 8 位纯数字
DEFAULT_TEACHER_PERMISSIONS = (
    '[{"group":"实验室业务","items":["环境管理","预约管理","课表数据"]},'
    '{"group":"个人工作","items":["教师课表","课程环境","调课申请"]}]'
)


def generate_digits(length: int = 8) -> str:
    """生成指定长度的纯数字字符串"""
    return "".join(str(secrets.randbelow(10)) for _ in range(length))


def generate_username() -> str:
    """生成 8 位纯数字账号"""
    return generate_digits(USERNAME_LENGTH)


def generate_password() -> str:
    """生成 8 位纯数字密码"""
    return generate_digits(PASSWORD_LENGTH)


def split_teacher_names(raw_name):
    """
    处理课表中多个教师的情况：
    张三、李四
    张三,李四
    张三/李四
    """
    if raw_name is None:
        return []

    text = str(raw_name).strip()

    if not text:
        return []

    separators = ["、", ",", "，", "/", "／", ";", "；", "|"]

    names = [text]

    for sep in separators:
        temp = []
        for item in names:
            temp.extend(item.split(sep))
        names = temp

    return [name.strip() for name in names if name.strip()]


def username_exists(cursor, username: str) -> bool:
    cursor.execute(
        """
        SELECT id
        FROM sys_user
        WHERE username = %s
        LIMIT 1
        """,
        (username,),
    )
    return cursor.fetchone() is not None


def generate_unique_username(cursor) -> str:
    """生成唯一 8 位数字账号"""
    while True:
        username = generate_username()
        if not username_exists(cursor, username):
            return username


def get_existing_user_by_real_name(cursor, real_name: str):
    cursor.execute(
        """
        SELECT id, username, real_name
        FROM sys_user
        WHERE real_name = %s
        LIMIT 1
        """,
        (real_name,),
    )
    return cursor.fetchone()


def get_or_create_teacher_role(cursor):
    """
    确保 role 表中存在教师角色。
    """
    cursor.execute(
        """
        SELECT id
        FROM role
        WHERE role_code = %s
        LIMIT 1
        """,
        ("teacher",),
    )
    role = cursor.fetchone()

    if role:
        return role["id"]

    cursor.execute(
        """
        INSERT INTO role (role_code, role_name)
        VALUES (%s, %s)
        """,
        ("teacher", "教师"),
    )

    return cursor.lastrowid


def bind_teacher_role(cursor, user_id, teacher_role_id) -> bool:
    """
    给用户绑定教师角色，避免重复绑定。
    """
    cursor.execute(
        """
        SELECT id
        FROM user_role
        WHERE user_id = %s
          AND role_id = %s
        LIMIT 1
        """,
        (user_id, teacher_role_id),
    )

    exists = cursor.fetchone()

    if exists:
        return False

    cursor.execute(
        """
        INSERT INTO user_role (user_id, role_id)
        VALUES (%s, %s)
        """,
        (user_id, teacher_role_id),
    )

    return True


def load_teachers_from_timetable(cursor):
    """
    从 class_timetable.teacher 提取教师姓名并去重。
    """
    cursor.execute(
        """
        SELECT DISTINCT teacher
        FROM class_timetable
        WHERE teacher IS NOT NULL
          AND TRIM(teacher) != ''
        """
    )

    rows = cursor.fetchall()

    teacher_set = set()

    for row in rows:
        raw_teacher = row.get("teacher")
        names = split_teacher_names(raw_teacher)

        for name in names:
            teacher_set.add(name)

    return sorted(teacher_set)


def insert_teacher_user(cursor, real_name: str):
    """
    插入教师用户。
    账号：8 位纯数字
    密码：8 位纯数字
    """
    username = generate_unique_username(cursor)
    password = generate_password()

    cursor.execute(
        """
        INSERT INTO sys_user (
            username,
            password,
            real_name,
            phone,
            email,
            department,
            status,
            created_at,
            updated_at,
            permissions
        )
        VALUES (
            %s,
            %s,
            %s,
            %s,
            %s,
            %s,
            %s,
            NOW(),
            NOW(),
            %s
        )
        """,
        (
            username,
            password,
            real_name,
            "",
            "",
            DEFAULT_DEPARTMENT,
            DEFAULT_STATUS,
            DEFAULT_TEACHER_PERMISSIONS,
        ),
    )

    return cursor.lastrowid, username, password


def save_created_accounts_to_csv(accounts):
    """
    保存本次新建的教师账号，防止忘记初始密码。
    """
    if not accounts:
        return

    filename = f"teacher_accounts_{datetime.now().strftime('%Y%m%d%H%M%S')}.csv"

    with open(filename, "w", newline="", encoding="utf-8-sig") as file:
        writer = csv.writer(file)
        writer.writerow(["教师姓名", "账号", "初始密码"])

        for item in accounts:
            writer.writerow([
                item["real_name"],
                item["username"],
                item["password"],
            ])

    print(f"\n新建账号已导出到：{filename}")


def main():
    connection = pymysql.connect(**DB_CONFIG)

    created_count = 0
    skipped_count = 0
    bind_count = 0
    created_accounts = []

    try:
        with connection.cursor() as cursor:
            connection.begin()

            teacher_role_id = get_or_create_teacher_role(cursor)

            teachers = load_teachers_from_timetable(cursor)

            print(f"从课表中提取到 {len(teachers)} 个教师：")
            for teacher in teachers:
                print(f" - {teacher}")

            print("\n开始同步教师账号...\n")

            for teacher_name in teachers:
                existing_user = get_existing_user_by_real_name(cursor, teacher_name)

                if existing_user:
                    user_id = existing_user["id"]
                    username = existing_user["username"]
                    skipped_count += 1
                    print(f"已存在，跳过创建：{teacher_name}，账号：{username}")
                else:
                    user_id, username, password = insert_teacher_user(cursor, teacher_name)
                    created_count += 1

                    created_accounts.append({
                        "real_name": teacher_name,
                        "username": username,
                        "password": password,
                    })

                    print(f"新增教师账号：{teacher_name}，账号：{username}，初始密码：{password}")

                if bind_teacher_role(cursor, user_id, teacher_role_id):
                    bind_count += 1
                    print(f"  已绑定教师角色：{teacher_name}")
                else:
                    print(f"  已有教师角色，无需重复绑定：{teacher_name}")

            connection.commit()

            save_created_accounts_to_csv(created_accounts)

            print("\n同步完成")
            print(f"新增账号数量：{created_count}")
            print(f"已存在跳过数量：{skipped_count}")
            print(f"新增角色绑定数量：{bind_count}")

    except Exception as e:
        connection.rollback()
        print("\n同步失败，已回滚")
        print("错误信息：", e)

    finally:
        connection.close()


if __name__ == "__main__":
    main()