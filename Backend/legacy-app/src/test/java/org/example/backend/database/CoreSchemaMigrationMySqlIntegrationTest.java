package org.example.backend.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@EnabledIfSystemProperty(named = "core.schema.mysql.it", matches = "true")
class CoreSchemaMigrationMySqlIntegrationTest {
    private static final Set<String> TARGET_TABLES = Set.of(
            "sys_user", "role", "user_role", "lab", "device", "device_event",
            "class_timetable", "lab_reservation", "timetable_integration_config",
            "timetable_import_job", "repair_order", "usage_record", "notice",
            "notice_recipient", "audit_event", "flyway_schema_history"
    );
    private static final String ROOT_URL = System.getProperty("core.schema.mysql.url", "jdbc:mysql://localhost:3306/");
    private static final String USERNAME = System.getProperty("core.schema.mysql.username", "root");
    private static final String PASSWORD = System.getProperty("core.schema.mysql.password", "root");
    private String schema;

    @BeforeEach
    void createFixtureSchema() throws SQLException {
        schema = "smart_lab_core_schema_it_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection(ROOT_URL, USERNAME, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("create database `" + schema + "` character set utf8mb4 collate utf8mb4_general_ci");
            statement.execute("create table `" + schema + "`.sys_user (id bigint primary key, real_name varchar(100)) engine=InnoDB");
            statement.execute("create table `" + schema + "`.role (id bigint primary key, role_code varchar(80)) engine=InnoDB");
            statement.execute("create table `" + schema + "`.user_role (user_id bigint not null, role_id bigint not null, primary key (user_id, role_id)) engine=InnoDB");
            statement.execute("create table `" + schema + "`.lab (id bigint primary key, lab_code varchar(80), lab_name varchar(160)) engine=InnoDB");
            statement.execute("create table `" + schema + "`.device (id bigint primary key, lab_name varchar(160)) engine=InnoDB");
            statement.execute("create table `" + schema + "`.class_timetable (id bigint primary key) engine=InnoDB");
            statement.execute("create table `" + schema + "`.lab_reservation (id bigint primary key) engine=InnoDB");
            statement.execute("create table `" + schema + "`.repair_order (id bigint primary key) engine=InnoDB");
            statement.execute("create table `" + schema + "`.notice (id bigint primary key) engine=InnoDB");
            statement.execute("create table `" + schema + "`.notice_recipient (id bigint primary key) engine=InnoDB");
            statement.execute("""
                    create table `%s`.usage_record (
                      id bigint primary key auto_increment, person varchar(100), resource varchar(160),
                      scene varchar(100), use_time datetime, status varchar(40), remark varchar(500)
                    ) engine=InnoDB
                    """.formatted(schema));
            statement.execute("""
                    create table `%s`.device_inventory_record (
                      id bigint primary key auto_increment, device_id bigint not null, device_code varchar(80),
                      device_name varchar(160), inspector_name varchar(100), inspected_at datetime,
                      result_status varchar(40), is_normal tinyint(1), is_missing tinyint(1),
                      is_damaged tinyint(1), remark text, created_at datetime
                    ) engine=InnoDB
                    """.formatted(schema));
            statement.execute("""
                    create table `%s`.device_transfer_record (
                      id bigint primary key auto_increment, device_id bigint not null, device_code varchar(80),
                      device_name varchar(160), transfer_type varchar(40), from_lab_id bigint, from_lab_name varchar(160),
                      to_lab_id bigint, to_lab_name varchar(160), from_owner_user_id bigint, from_owner_name varchar(100),
                      to_owner_user_id bigint, to_owner_name varchar(100), from_location varchar(200),
                      to_location varchar(200), reason varchar(500), operator_name varchar(100),
                      transfer_at datetime, created_at datetime
                    ) engine=InnoDB
                    """.formatted(schema));
            statement.execute("""
                    create table `%s`.timetable_crawler_config (
                      config_key varchar(80) primary key, schedule_enabled tinyint(1), cron_expression varchar(120),
                      semester varchar(40), semester_start_date varchar(20), updated_at datetime
                    ) engine=InnoDB
                    """.formatted(schema));
            statement.execute("""
                    create table `%s`.timetable_academic_credential (
                      credential_key varchar(80) primary key, username_cipher text, password_cipher text, updated_at datetime
                    ) engine=InnoDB
                    """.formatted(schema));
            statement.execute("""
                    create table `%s`.timetable_crawler_task (
                      id bigint primary key, trigger_type varchar(20), status varchar(20), scope varchar(50), source varchar(80),
                      success_account varchar(120), attempts int, updated_count int, started_at datetime, finished_at datetime,
                      error_message text, output mediumtext, log_path varchar(500), created_at datetime
                    ) engine=InnoDB
                    """.formatted(schema));
            statement.execute("""
                    create table `%s`.module_record (
                      id bigint primary key auto_increment, module_name varchar(80), record_key varchar(100),
                      record_json json, created_at datetime
                    ) engine=InnoDB
                    """.formatted(schema));
            for (String retiredTable : new String[]{
                    "iot_command_log", "iot_telemetry_latest", "iot_gateway_point", "iot_device_capability",
                    "iot_gateway_config", "iot_device", "device_status_event", "device_runtime_status",
                    "lab_terminal", "lab_activation_code", "student_client", "teacher_host",
                    "consumable_stock_record", "consumable_inventory", "course_environment_request",
                    "environment_template", "lab_software", "schedule_adjustment", "approval_countersign",
                    "ai_assistant_conversation", "ai_assistant_user_config", "host_static_asset"
            }) {
                statement.execute("create table `" + schema + "`." + retiredTable
                        + " (id bigint primary key) engine=InnoDB");
            }

            statement.execute("insert into `" + schema + "`.sys_user values (11, '王老师')");
            statement.execute("insert into `" + schema + "`.lab values (21, 'LAB-21', '软件实验室')");
            statement.execute("insert into `" + schema + "`.device values (31, '软件实验室')");
            statement.execute("insert into `" + schema + "`.usage_record(person, resource, scene, use_time, status, remark) values ('王老师', '软件实验室', '实验', '2026-10-10 09:00:00', '完成', 'fixture')");
            statement.execute("insert into `" + schema + "`.device_inventory_record(device_id, device_code, device_name, inspector_name, inspected_at, result_status, is_normal, is_missing, is_damaged, remark, created_at) values (31, 'DEV-31', '终端', '管理员', '2026-10-10 10:00:00', '正常', 1, 0, 0, '盘点', '2026-10-10 10:00:00')");
            statement.execute("insert into `" + schema + "`.device_transfer_record(device_id, device_code, device_name, transfer_type, from_lab_id, from_lab_name, to_lab_id, to_lab_name, from_owner_user_id, from_owner_name, to_owner_user_id, to_owner_name, from_location, to_location, reason, operator_name, transfer_at, created_at) values (31, 'DEV-31', '终端', '调拨', 21, '软件实验室', 21, '软件实验室', 11, '王老师', 11, '王老师', 'A区', 'B区', '布局调整', '管理员', '2026-10-10 11:00:00', '2026-10-10 11:00:00')");
            statement.execute("insert into `" + schema + "`.timetable_crawler_config values ('lab-admin-class-timetable', 1, '0 0 2 * * *', '2026-2027-1', '2026-09-01', '2026-10-10 12:00:00')");
            statement.execute("insert into `" + schema + "`.timetable_academic_credential values ('lab-admin-class-timetable', 'cipher-user', 'cipher-password', '2026-10-10 12:01:00')");
            statement.execute("insert into `" + schema + "`.timetable_crawler_task values (41, 'manual', 'success', 'all', 'school', 'account', 1, 10, '2026-10-10 12:02:00', '2026-10-10 12:03:00', null, 'ok', 'task.log', '2026-10-10 12:02:00')");
            statement.execute("insert into `" + schema + "`.module_record(module_name, record_key, record_json, created_at) values ('operation-events', 'evt-1', '{\"category\":\"device\",\"action\":\"inventory\",\"status\":\"success\",\"operator\":\"管理员\",\"occurredAt\":\"2026-10-10 12:04:00\",\"details\":{\"deviceId\":31}}', '2026-10-10 12:04:00')");
        }
    }

    @AfterEach
    void dropFixtureSchema() throws SQLException {
        if (schema == null || !schema.matches("^smart_lab_core_schema_it_[0-9a-f]{32}$")) {
            throw new IllegalStateException("unsafe schema name");
        }
        try (Connection connection = DriverManager.getConnection(ROOT_URL, USERNAME, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("drop database `" + schema + "`");
        }
    }

    @Test
    void expandAndDataMigrationsPreserveCoreRows() throws Exception {
        migrateTo("20261010.2");

        assertEquals(2, count("device_event"));
        assertEquals(1, count("timetable_integration_config"));
        assertEquals(1, count("timetable_import_job"));
        assertEquals(1, count("audit_event"));
        assertEquals("cipher-user", scalar("select credential_username_cipher from timetable_integration_config"));
        assertEquals("cipher-password", scalar("select credential_password_cipher from timetable_integration_config"));
        assertEquals("11", scalar("select user_id from usage_record where id = 1"));
        assertEquals("21", scalar("select lab_id from usage_record where id = 1"));
        assertEquals(1, count("device_inventory_record"));
        assertEquals(1, count("module_record"));
    }

    @Test
    void shrinkMigrationLeavesOnlyApprovedTablesAndPreservesRows() throws Exception {
        migrateTo("20261010.3");

        assertEquals(TARGET_TABLES, tableNames());
        assertEquals(2, count("device_event"));
        assertEquals(1, count("timetable_integration_config"));
        assertEquals(1, count("timetable_import_job"));
        assertEquals(1, count("audit_event"));
        assertFalse(columnNames("device").contains("lab_name"));
        assertEquals(0, orphanCount("usage_record", "user_id", "sys_user"));
        assertEquals(0, orphanCount("usage_record", "lab_id", "lab"));
    }

    private void migrateTo(String target) {
        Flyway.configure()
                .dataSource(ROOT_URL + schema, USERNAME, PASSWORD)
                .baselineOnMigrate(true)
                .baselineVersion("20261009")
                .target(target)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private long count(String table) throws SQLException {
        return Long.parseLong(scalar("select count(*) from `" + table + "`"));
    }

    private Set<String> tableNames() throws SQLException {
        Set<String> tables = new HashSet<>();
        try (Connection connection = DriverManager.getConnection(ROOT_URL + schema, USERNAME, PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select table_name from information_schema.tables where table_schema = '" + schema + "'")) {
            while (resultSet.next()) {
                tables.add(resultSet.getString(1));
            }
        }
        return tables;
    }

    private Set<String> columnNames(String table) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Connection connection = DriverManager.getConnection(ROOT_URL + schema, USERNAME, PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select column_name from information_schema.columns where table_schema = '" + schema + "' and table_name = '" + table + "'")) {
            while (resultSet.next()) {
                columns.add(resultSet.getString(1));
            }
        }
        return columns;
    }

    private long orphanCount(String childTable, String childColumn, String parentTable) throws SQLException {
        return Long.parseLong(scalar("select count(*) from `" + childTable + "` child left join `" + parentTable
                + "` parent on parent.id = child.`" + childColumn + "` where child.`" + childColumn
                + "` is not null and parent.id is null"));
    }

    private String scalar(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(ROOT_URL + schema, USERNAME, PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }
}
