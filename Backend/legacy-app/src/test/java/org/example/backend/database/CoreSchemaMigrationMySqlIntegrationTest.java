package org.example.backend.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfSystemProperty(named = "core.schema.mysql.it", matches = "true")
class CoreSchemaMigrationMySqlIntegrationTest {
    private static final String SCHEMA = "smart_lab_core_schema_it_" + UUID.randomUUID().toString().replace("-", "");
    private static final String ROOT_URL = System.getProperty("core.schema.mysql.url", "jdbc:mysql://localhost:3306/");
    private static final String USERNAME = System.getProperty("core.schema.mysql.username", "root");
    private static final String PASSWORD = System.getProperty("core.schema.mysql.password", "root");

    @BeforeAll
    static void createSchemaAndFixtures() throws SQLException {
        try (Connection connection = DriverManager.getConnection(ROOT_URL, USERNAME, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("create database `" + SCHEMA + "` character set utf8mb4");
            createFixtureTables(statement);
            insertFixtures(statement);
        }
    }

    @AfterAll
    static void dropSchema() throws SQLException {
        if (!SCHEMA.startsWith("smart_lab_core_schema_it_")) throw new IllegalStateException("unsafe schema name");
        try (Connection connection = DriverManager.getConnection(ROOT_URL, USERNAME, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("drop database `" + SCHEMA + "`");
        }
    }

    @Test
    void expandAndDataMigrationsPreserveCoreRows() throws Exception {
        Flyway.configure()
                .dataSource(ROOT_URL + SCHEMA + "?useSSL=false&allowPublicKeyRetrieval=true", USERNAME, PASSWORD)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("20261009")
                .target("20261010.2")
                .load()
                .migrate();

        assertEquals(2, count("device_event"));
        assertEquals(1, count("timetable_integration_config"));
        assertEquals(1, count("timetable_import_job"));
        assertEquals(1, count("audit_event"));
        assertEquals("relocation", scalar("""
                select json_unquote(json_extract(after_state, '$.transferType'))
                from device_event
                where event_type = 'transfer'
                """));
        assertEquals("cipher-user", scalar("select credential_username_cipher from timetable_integration_config"));
        assertEquals("cipher-password", scalar("select credential_password_cipher from timetable_integration_config"));
        assertEquals(1, scalarLong("select count(*) from usage_record where id = 1 and user_id = 1"));
        assertEquals(1, scalarLong("select count(*) from usage_record where id = 1 and lab_id = 1"));
        assertEquals(1, scalarLong("select count(*) from usage_record where id = 2 and user_id is null"));
        assertEquals(1, scalarLong("select count(*) from usage_record where id = 3 and lab_id is null"));
        assertEquals(1, count("device_inventory_record"), "source row lost from device_inventory_record");
        assertEquals(1, count("device_transfer_record"), "source row lost from device_transfer_record");
        assertEquals(1, count("timetable_crawler_config"), "source row lost from timetable_crawler_config");
        assertEquals(1, count("timetable_academic_credential"), "source row lost from timetable_academic_credential");
        assertEquals(1, count("timetable_crawler_task"), "source row lost from timetable_crawler_task");
        assertEquals(1, count("module_record"), "source row lost from module_record");
        assertEquals(3, count("usage_record"), "source rows lost from usage_record");
    }

    private static long count(String table) throws SQLException {
        return scalarLong("select count(*) from " + table);
    }

    private static String scalar(String sql) throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }

    private static long scalarLong(String sql) throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(ROOT_URL + SCHEMA + "?useSSL=false&allowPublicKeyRetrieval=true", USERNAME, PASSWORD);
    }

    private static void createFixtureTables(Statement statement) throws SQLException {
        statement.execute("""
                create table `%s`.sys_user (
                  id bigint primary key,
                  real_name varchar(100) not null
                ) engine=InnoDB
                """.formatted(SCHEMA));
        statement.execute("""
                create table `%s`.lab (
                  id bigint primary key,
                  lab_code varchar(80) not null,
                  lab_name varchar(160) not null
                ) engine=InnoDB
                """.formatted(SCHEMA));
        statement.execute("""
                create table `%s`.device (
                  id bigint primary key,
                  device_code varchar(80) not null,
                  device_name varchar(160) not null
                ) engine=InnoDB
                """.formatted(SCHEMA));
        statement.execute("""
                create table `%s`.device_inventory_record (
                  id bigint primary key,
                  device_id bigint not null,
                  device_code varchar(80),
                  device_name varchar(160),
                  inspector_name varchar(100) not null,
                  inspected_at datetime not null,
                  result_status varchar(40) not null,
                  is_normal tinyint(1) not null,
                  is_missing tinyint(1) not null,
                  is_damaged tinyint(1) not null,
                  remark text,
                  created_at datetime not null
                ) engine=InnoDB
                """.formatted(SCHEMA));
        statement.execute("""
                create table `%s`.device_transfer_record (
                  id bigint primary key,
                  device_id bigint not null,
                  device_code varchar(80),
                  device_name varchar(160),
                  from_lab_id bigint,
                  from_lab_name varchar(160),
                  to_lab_id bigint,
                  to_lab_name varchar(160),
                  from_owner_user_id bigint,
                  from_owner_name varchar(100),
                  to_owner_user_id bigint,
                  to_owner_name varchar(100),
                  from_location varchar(200),
                  to_location varchar(200),
                  transfer_type varchar(80),
                  reason varchar(500),
                  operator_name varchar(100),
                  transfer_at datetime not null,
                  created_at datetime not null
                ) engine=InnoDB
                """.formatted(SCHEMA));
        statement.execute("""
                create table `%s`.timetable_crawler_config (
                  config_key varchar(80) primary key,
                  schedule_enabled tinyint(1),
                  cron_expression varchar(120),
                  semester varchar(40),
                  semester_start_date varchar(20),
                  updated_at datetime
                ) engine=InnoDB
                """.formatted(SCHEMA));
        statement.execute("""
                create table `%s`.timetable_academic_credential (
                  credential_key varchar(80) primary key,
                  username_cipher text not null,
                  password_cipher text not null,
                  updated_at datetime
                ) engine=InnoDB
                """.formatted(SCHEMA));
        statement.execute("""
                create table `%s`.timetable_crawler_task (
                  id bigint primary key,
                  trigger_type varchar(20) not null,
                  status varchar(20) not null,
                  scope varchar(50),
                  source varchar(80),
                  success_account varchar(120),
                  attempts int,
                  updated_count int,
                  started_at datetime,
                  finished_at datetime,
                  error_message text,
                  output mediumtext,
                  log_path varchar(500),
                  created_at datetime not null
                ) engine=InnoDB
                """.formatted(SCHEMA));
        statement.execute("""
                create table `%s`.module_record (
                  id bigint primary key,
                  module_name varchar(100) not null,
                  record_key varchar(100) not null,
                  record_json json not null,
                  created_at datetime not null
                ) engine=InnoDB
                """.formatted(SCHEMA));
        statement.execute("""
                create table `%s`.usage_record (
                  id bigint primary key,
                  person varchar(100) not null,
                  resource varchar(160) not null,
                  scene varchar(100) not null,
                  use_time datetime not null,
                  status varchar(40) not null,
                  remark varchar(500),
                  created_at datetime not null,
                  updated_at datetime not null
                ) engine=InnoDB
                """.formatted(SCHEMA));
    }

    private static void insertFixtures(Statement statement) throws SQLException {
        statement.execute("insert into `" + SCHEMA + "`.sys_user (id, real_name) values (1, 'Alice'), (2, 'Duplicate'), (3, 'Duplicate')");
        statement.execute("insert into `" + SCHEMA + "`.lab (id, lab_code, lab_name) values (1, 'LAB-1', 'Main Lab'), (2, 'LAB-2', 'Shared Lab'), (3, 'LAB-3', 'Shared Lab')");
        statement.execute("insert into `" + SCHEMA + "`.device (id, device_code, device_name) values (1, 'DEV-1', 'Microscope')");
        statement.execute("""
                insert into `%s`.device_inventory_record
                values (1, 1, 'DEV-1', 'Microscope', 'Alice', '2026-10-10 09:00:00', 'normal', 1, 0, 0, 'inventory', '2026-10-10 09:01:00')
                """.formatted(SCHEMA));
        statement.execute("""
                insert into `%s`.device_transfer_record
                values (1, 1, 'DEV-1', 'Microscope', 1, 'Main Lab', 1, 'Main Lab', 1, 'Alice', 1, 'Alice', 'A-101', 'A-102', 'relocation', 'transfer', 'Alice', '2026-10-10 10:00:00', '2026-10-10 10:01:00')
                """.formatted(SCHEMA));
        statement.execute("""
                insert into `%s`.timetable_crawler_config
                values ('lab-admin-class-timetable', 1, '0 0 2 * * *', '2026-Fall', '2026-09-01', '2026-10-10 08:00:00')
                """.formatted(SCHEMA));
        statement.execute("""
                insert into `%s`.timetable_academic_credential
                values ('lab-admin-class-timetable', 'cipher-user', 'cipher-password', '2026-10-10 08:01:00')
                """.formatted(SCHEMA));
        statement.execute("""
                insert into `%s`.timetable_crawler_task
                values (1, 'manual', 'success', 'all', 'fixture', 'alice', 1, 1, '2026-10-10 08:00:00', '2026-10-10 08:05:00', null, 'done', null, '2026-10-10 08:00:00')
                """.formatted(SCHEMA));
        statement.execute("""
                insert into `%s`.module_record
                values (1, 'operation-events', 'fixture', JSON_OBJECT('category', 'device', 'action', 'inspect', 'status', 'success', 'operator', 'Alice', 'occurredAt', '2026-10-10 09:00:00', 'details', JSON_OBJECT('deviceId', '1')), '2026-10-10 09:01:00')
                """.formatted(SCHEMA));
        statement.execute("""
                insert into `%s`.usage_record
                values (1, 'Alice', 'Main Lab', 'teaching', '2026-10-10 09:00:00', 'normal', 'usage', '2026-10-10 09:00:00', '2026-10-10 09:00:00'),
                       (2, 'Duplicate', 'Main Lab', 'teaching', '2026-10-10 09:10:00', 'normal', 'ambiguous user', '2026-10-10 09:10:00', '2026-10-10 09:10:00'),
                       (3, 'Alice', 'Shared Lab', 'teaching', '2026-10-10 09:20:00', 'normal', 'ambiguous lab', '2026-10-10 09:20:00', '2026-10-10 09:20:00')
                """.formatted(SCHEMA));
    }
}
