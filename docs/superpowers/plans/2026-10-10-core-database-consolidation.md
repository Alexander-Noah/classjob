# Core Database Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Consolidate `smart_lab_basic` from 39 business tables to 15 business tables without changing core API behavior or losing core data.

**Architecture:** Introduce controlled Flyway migrations, expand the schema with four focused replacement tables, switch core persistence adapters while preserving controller contracts, remove explicitly retired modules, and only then run a guarded shrink migration. Full and per-table SQL archives plus row/key verification are mandatory before any destructive migration.

**Tech Stack:** Java 17, Spring Boot 4.0.6, MyBatis 4.0.1, MySQL 8, Flyway, Vue 3, Node.js test runner, PowerShell 7.

## Global Constraints

- Preserve login, users/roles, labs, devices, device inventory/transfer, reservations, timetable import/query, lab-admin maintenance-record entry/viewing, notices, usage records, dashboards, and core statistics.
- Preserve core HTTP paths and request/response shapes.
- Retired module data must be exported and restored successfully in a rehearsal database before tables are dropped.
- The production/local working database must not be mutated until the backup manifest and restore rehearsal pass.
- Business code, Mapper XML, and application runners must not execute DDL after the migration.
- Keep 15 business tables plus Flyway's `flyway_schema_history` table.
- Credentials remain encrypted and must never appear in logs, manifests, command lines, or test snapshots.
- Preserve unrelated working-tree changes; stage only files listed by the active task.

## Observed Test Baseline

On 2026-10-10, `mvn.cmd -pl legacy-app test` ran 100 tests and reported five failures plus one error while a separate, uncommitted maintenance-navigation change was in progress. Those files retire the maintenance role and replace work-order UI with lab-admin maintenance-record entry/viewing; the database work must wait until that change is committed and its tests are green. The remaining entry-point test searches for `SpringApplication.run` although the source correctly uses an instance `application.run`; Task 8 corrects that source-contract assertion. No database-migration implementation may overwrite the maintenance change or weaken assertions merely to obtain a green build.

---

## File Structure

### Migration and operations

- `Backend/legacy-app/src/main/resources/db/migration/V20261010_01__expand_core_schema.sql`: create replacement tables and additive columns.
- `Backend/legacy-app/src/main/resources/db/migration/V20261010_02__migrate_core_data.sql`: copy and normalize existing data without dropping sources.
- `Backend/legacy-app/src/main/resources/db/migration/V20261010_03__shrink_retired_schema.sql`: remove retired tables and redundant columns after cutover.
- `Backend/database/maintenance/20261010-core-consolidation/Backup-CoreDatabase.ps1`: full backup, retired-table archive, manifest, and checksum generation.
- `Backend/database/maintenance/20261010-core-consolidation/Test-CoreDatabaseBackup.ps1`: restore rehearsal in a guarded temporary schema.
- `Backend/database/maintenance/20261010-core-consolidation/Invoke-CoreDatabaseMigration.ps1`: preflight checks and explicit Flyway target execution.
- `Backend/database/maintenance/20261010-core-consolidation/README.md`: operator runbook and rollback commands.

### New persistence units

- `Backend/legacy-app/src/main/java/org/example/backend/entity/DeviceEventEntity.java`: internal device lifecycle event model.
- `Backend/legacy-app/src/main/java/org/example/backend/mapper/DeviceEventMapper.java`: device event persistence contract.
- `Backend/legacy-app/src/main/resources/mapper/DeviceEventMapper.xml`: device event SQL and compatibility projections.
- `Backend/legacy-app/src/main/java/org/example/backend/entity/AuditEventEntity.java`: append-only core audit event.
- `Backend/legacy-app/src/main/java/org/example/backend/mapper/AuditEventMapper.java`: audit persistence contract.
- `Backend/legacy-app/src/main/resources/mapper/AuditEventMapper.xml`: insert and reporting queries.

### Contract and integration tests

- `Backend/legacy-app/src/test/java/org/example/backend/database/CoreSchemaMigrationContractTest.java`
- `Backend/legacy-app/src/test/java/org/example/backend/database/CoreSchemaMigrationMySqlIntegrationTest.java`
- `Backend/legacy-app/src/test/java/org/example/backend/database/DatabaseMaintenanceScriptContractTest.java`
- `Backend/legacy-app/src/test/java/org/example/backend/device/DeviceEventPersistenceContractTest.java`
- `Backend/legacy-app/src/test/java/org/example/backend/timetable/TimetablePersistenceContractTest.java`
- `Backend/legacy-app/src/test/java/org/example/backend/audit/AuditEventPersistenceContractTest.java`
- `Backend/legacy-app/src/test/java/org/example/backend/database/RetiredModuleBoundaryTest.java`
- `web/tests/core-module-boundary.test.mjs`
- `web/tests/core-flow-regression.test.mjs`

---

### Task 1: Add controlled Flyway migration infrastructure and the expand schema

**Files:**
- Modify: `Backend/legacy-app/pom.xml`
- Modify: `Backend/legacy-app/src/main/resources/application.yaml`
- Modify: `Backend/legacy-app/src/main/resources/application-local.yaml`
- Create: `Backend/legacy-app/src/main/resources/db/migration/V20261010_01__expand_core_schema.sql`
- Create: `Backend/legacy-app/src/test/java/org/example/backend/database/CoreSchemaMigrationContractTest.java`

**Interfaces:**
- Consumes: `SMART_LAB_DB_URL`, `SMART_LAB_DB_USERNAME`, `SMART_LAB_DB_PASSWORD`.
- Produces: Flyway migrations that are disabled during ordinary startup unless `SMART_LAB_FLYWAY_ENABLED=true`.
- Produces: `device_event`, `timetable_integration_config`, `timetable_import_job`, `audit_event`, and additive `usage_record` columns.

- [ ] **Step 1: Write the failing migration contract test**

```java
package org.example.backend.database;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class CoreSchemaMigrationContractTest {
    private static final Path MIGRATIONS = Path.of("src", "main", "resources", "db", "migration");

    @Test
    void expandMigrationCreatesExactlyTheFourReplacementTables() throws Exception {
        String sql = Files.readString(MIGRATIONS.resolve("V20261010_01__expand_core_schema.sql")).toLowerCase();
        Set<String> expected = Set.of(
                "device_event", "timetable_integration_config",
                "timetable_import_job", "audit_event"
        );
        for (String table : expected) assertTrue(sql.contains("create table " + table), table);
        assertTrue(sql.contains("alter table usage_record"));
        assertTrue(sql.contains("add column user_id"));
        assertTrue(sql.contains("add column lab_id"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("drop column"));
    }

    @Test
    void applicationKeepsFlywayExplicitlyDisabledByDefault() throws Exception {
        String yaml = Files.readString(Path.of("src", "main", "resources", "application.yaml"));
        assertTrue(yaml.contains("enabled: ${SMART_LAB_FLYWAY_ENABLED:false}"));
        assertTrue(yaml.contains("baseline-version: 20261009"));
    }
}
```

- [ ] **Step 2: Run the focused test and confirm RED**

Run from `Backend`:

```powershell
mvn.cmd -pl legacy-app -Dtest=CoreSchemaMigrationContractTest test
```

Expected: FAIL because the migration and Flyway configuration do not exist.

- [ ] **Step 3: Add Flyway dependencies and guarded configuration**

Add to `Backend/legacy-app/pom.xml`:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
    <scope>runtime</scope>
</dependency>
```

Add under `spring:` in both YAML files:

```yaml
  flyway:
    enabled: ${SMART_LAB_FLYWAY_ENABLED:false}
    baseline-on-migrate: true
    baseline-version: 20261009
    locations: classpath:db/migration
    validate-on-migrate: true
    clean-disabled: true
```

- [ ] **Step 4: Write the additive migration**

The migration must use InnoDB/utf8mb4 and create these complete structures:

```sql
create table device_event (
  id bigint primary key auto_increment,
  device_id bigint not null,
  event_type varchar(32) not null,
  occurred_at datetime not null,
  operator_user_id bigint null,
  operator_name varchar(100) null,
  result_status varchar(40) null,
  before_state json null,
  after_state json null,
  remark varchar(500) null,
  created_at datetime not null default current_timestamp,
  key idx_device_event_device_time (device_id, occurred_at),
  key idx_device_event_type_time (event_type, occurred_at),
  constraint fk_device_event_device foreign key (device_id) references device(id),
  constraint fk_device_event_operator foreign key (operator_user_id) references sys_user(id) on delete set null,
  constraint chk_device_event_type check (event_type in ('inventory', 'transfer'))
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci;

create table timetable_integration_config (
  id bigint primary key auto_increment,
  config_key varchar(80) not null,
  schedule_enabled tinyint(1) not null default 0,
  cron_expression varchar(120) not null,
  semester varchar(40) null,
  semester_start_date date null,
  credential_username_cipher text null,
  credential_password_cipher text null,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_timetable_integration_config_key (config_key)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci;

create table timetable_import_job (
  id bigint primary key,
  config_id bigint null,
  trigger_type varchar(20) not null,
  status varchar(20) not null,
  scope varchar(50) null,
  source varchar(80) null,
  success_account varchar(120) null,
  attempts int not null default 0,
  updated_count int not null default 0,
  started_at datetime null,
  finished_at datetime null,
  error_message text null,
  output mediumtext null,
  log_path varchar(500) null,
  created_at datetime not null default current_timestamp,
  key idx_timetable_import_job_status_time (status, started_at),
  constraint fk_timetable_import_job_config foreign key (config_id)
    references timetable_integration_config(id) on delete set null
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci;

create table audit_event (
  id bigint primary key auto_increment,
  aggregate_type varchar(60) not null,
  aggregate_id varchar(80) null,
  action varchar(60) not null,
  result_status varchar(40) null,
  actor_user_id bigint null,
  actor_name varchar(100) null,
  detail_json json null,
  occurred_at datetime not null,
  created_at datetime not null default current_timestamp,
  key idx_audit_event_aggregate (aggregate_type, aggregate_id, occurred_at),
  key idx_audit_event_actor_time (actor_user_id, occurred_at),
  key idx_audit_event_occurred_at (occurred_at),
  constraint fk_audit_event_actor foreign key (actor_user_id)
    references sys_user(id) on delete set null
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci;

alter table usage_record
  add column user_id bigint null after id,
  add column lab_id bigint null after user_id,
  add column source_type varchar(40) null after remark,
  add column source_id varchar(80) null after source_type,
  add key idx_usage_record_user_time (user_id, use_time),
  add key idx_usage_record_lab_time (lab_id, use_time),
  add key idx_usage_record_source (source_type, source_id),
  add constraint fk_usage_record_user foreign key (user_id) references sys_user(id) on delete set null,
  add constraint fk_usage_record_lab foreign key (lab_id) references lab(id) on delete set null;
```

- [ ] **Step 5: Run the contract test and compile**

```powershell
mvn.cmd -pl legacy-app -Dtest=CoreSchemaMigrationContractTest test
mvn.cmd -pl legacy-app -DskipTests compile
```

Expected: PASS and BUILD SUCCESS.

- [ ] **Step 6: Commit the additive infrastructure**

```powershell
git add Backend/legacy-app/pom.xml Backend/legacy-app/src/main/resources/application.yaml Backend/legacy-app/src/main/resources/application-local.yaml Backend/legacy-app/src/main/resources/db/migration/V20261010_01__expand_core_schema.sql Backend/legacy-app/src/test/java/org/example/backend/database/CoreSchemaMigrationContractTest.java
git commit -m "feat(database): add guarded core schema migrations"
```

---

### Task 2: Add backup, restore rehearsal, and guarded migration tooling

**Files:**
- Create: `Backend/database/maintenance/20261010-core-consolidation/Backup-CoreDatabase.ps1`
- Create: `Backend/database/maintenance/20261010-core-consolidation/Test-CoreDatabaseBackup.ps1`
- Create: `Backend/database/maintenance/20261010-core-consolidation/Invoke-CoreDatabaseMigration.ps1`
- Create: `Backend/database/maintenance/20261010-core-consolidation/README.md`
- Create: `Backend/legacy-app/src/test/java/org/example/backend/database/DatabaseMaintenanceScriptContractTest.java`

**Interfaces:**
- Consumes: explicit database name, host, port, user, output directory, and `MYSQL_PWD` from the process environment.
- Produces: `full.sql`, `retired-tables.sql`, `manifest.json`, SHA-256 files, and a restore rehearsal report.
- Rejects: blank database names, names other than `smart_lab_basic`, output paths outside the supplied archive directory, or rehearsal schemas without prefix `smart_lab_restore_rehearsal_`.

- [ ] **Step 1: Write the failing script contract test**

```java
package org.example.backend.database;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseMaintenanceScriptContractTest {
    private static final Path ROOT = Path.of("..", "database", "maintenance", "20261010-core-consolidation");

    @Test
    void scriptsRequireBackupAndRestoreProofBeforeShrink() throws Exception {
        String backup = Files.readString(ROOT.resolve("Backup-CoreDatabase.ps1"));
        String restore = Files.readString(ROOT.resolve("Test-CoreDatabaseBackup.ps1"));
        String migrate = Files.readString(ROOT.resolve("Invoke-CoreDatabaseMigration.ps1"));
        assertAll(
                () -> assertTrue(backup.contains("mysqldump")),
                () -> assertTrue(backup.contains("Get-FileHash")),
                () -> assertFalse(backup.contains("--password=")),
                () -> assertTrue(restore.contains("smart_lab_restore_rehearsal_")),
                () -> assertTrue(migrate.contains("restore-rehearsal.json")),
                () -> assertTrue(migrate.contains("SMART_LAB_FLYWAY_ENABLED")),
                () -> assertTrue(migrate.contains("20261010.3"))
        );
    }
}
```

- [ ] **Step 2: Run the focused test and confirm RED**

```powershell
mvn.cmd -pl legacy-app -Dtest=DatabaseMaintenanceScriptContractTest test
```

Expected: FAIL because the scripts do not exist.

- [ ] **Step 3: Implement the backup script with fixed retired-table scope**

The script must expose this parameter contract and use argument arrays, never shell-built SQL commands:

```powershell
param(
    [Parameter(Mandatory)][string]$Database,
    [string]$DbHost = '127.0.0.1',
    [int]$Port = 3306,
    [string]$User = 'root',
    [Parameter(Mandatory)][string]$ArchiveRoot
)

$retiredTables = @(
  'ai_assistant_conversation','ai_assistant_user_config','approval_countersign',
  'consumable_inventory','consumable_stock_record','course_environment_request',
  'device_runtime_status','device_status_event','environment_template','host_static_asset',
  'iot_command_log','iot_device','iot_device_capability','iot_gateway_config',
  'iot_gateway_point','iot_telemetry_latest','lab_activation_code','lab_software',
  'lab_terminal','schedule_adjustment','student_client','teacher_host',
  'device_inventory_record','device_transfer_record','module_record',
  'timetable_crawler_config','timetable_academic_credential','timetable_crawler_task'
)

if ($Database -ne 'smart_lab_basic') { throw 'Database must be smart_lab_basic' }
if ([string]::IsNullOrWhiteSpace($env:MYSQL_PWD)) { throw 'MYSQL_PWD must be configured in the process environment' }
```

Resolve `$ArchiveRoot` to an absolute path, create one timestamped child directory, run one full `mysqldump --single-transaction --routines --triggers`, run one retired-table dump, query row counts/min/max IDs into `manifest.json`, calculate SHA-256 for both dumps, and write no credential value to output.

- [ ] **Step 4: Implement restore rehearsal and guarded migration scripts**

`Test-CoreDatabaseBackup.ps1` must create a random schema matching `^smart_lab_restore_rehearsal_[0-9a-f]{32}$`, import `full.sql`, compare manifest row counts, write `restore-rehearsal.json`, and drop only the exact resolved rehearsal schema in a `finally` block.

`Invoke-CoreDatabaseMigration.ps1` must accept `-Phase Expand|Shrink`. `Expand` targets Flyway `20261010.2`; `Shrink` targets `20261010.3` and refuses to run unless dump hashes match and `restore-rehearsal.json` reports `success: true`. Set `SMART_LAB_FLYWAY_ENABLED=true` only in the child process, not globally.

- [ ] **Step 5: Document exact operator commands**

The runbook must contain these executable commands; the backup script returns the created archive directory as its only pipeline output:

```powershell
$env:MYSQL_PWD = Read-Host 'MySQL password' -MaskInput
$archive = ./Backup-CoreDatabase.ps1 -Database smart_lab_basic -ArchiveRoot 'E:\smart-lab-db-archives'
./Test-CoreDatabaseBackup.ps1 -ArchiveDirectory $archive
./Invoke-CoreDatabaseMigration.ps1 -Phase Expand -ArchiveDirectory $archive
./Invoke-CoreDatabaseMigration.ps1 -Phase Shrink -ArchiveDirectory $archive
Remove-Item Env:MYSQL_PWD
```

- [ ] **Step 6: Run the script contract test**

```powershell
mvn.cmd -pl legacy-app -Dtest=DatabaseMaintenanceScriptContractTest test
```

Expected: PASS.

- [ ] **Step 7: Commit operational safety tooling**

```powershell
git add Backend/database/maintenance/20261010-core-consolidation Backend/legacy-app/src/test/java/org/example/backend/database/DatabaseMaintenanceScriptContractTest.java
git commit -m "feat(database): add guarded backup and migration workflow"
```

---

### Task 3: Migrate existing data into the replacement tables

**Files:**
- Create: `Backend/legacy-app/src/main/resources/db/migration/V20261010_02__migrate_core_data.sql`
- Create: `Backend/legacy-app/src/test/java/org/example/backend/database/CoreSchemaMigrationMySqlIntegrationTest.java`

**Interfaces:**
- Consumes: the current source tables and the four tables created by V20261010.01.
- Produces: idempotent-by-version copied data, preserving source rows until the shrink phase.
- Produces: a system-property-gated MySQL integration test using a random schema with prefix `smart_lab_core_schema_it_`.

- [ ] **Step 1: Write a gated MySQL migration integration test**

Follow `MySqlReservationConcurrencyIntegrationTest` for connection and guarded cleanup. The test must:

```java
@EnabledIfSystemProperty(named = "core.schema.mysql.it", matches = "true")
class CoreSchemaMigrationMySqlIntegrationTest {
    @Test
    void expandAndDataMigrationsPreserveCoreRows() throws Exception {
        // Arrange one device inventory row, one transfer row, one timetable config,
        // one encrypted credential, one import task, one operation-events JSON row,
        // and one usage row in a random test schema.
        // Run Flyway with target "20261010.2".
        assertEquals(2, count("device_event"));
        assertEquals(1, count("timetable_integration_config"));
        assertEquals(1, count("timetable_import_job"));
        assertEquals(1, count("audit_event"));
        assertEquals("cipher-user", scalar("select credential_username_cipher from timetable_integration_config"));
        assertEquals("cipher-password", scalar("select credential_password_cipher from timetable_integration_config"));
        assertEquals(1, count("device_inventory_record"));
        assertEquals(1, count("module_record"));
    }
}
```

Create all required fixture tables explicitly in `@BeforeAll`; drop only the random prefixed schema in `@AfterAll`.

- [ ] **Step 2: Run the integration test and confirm RED**

```powershell
mvn.cmd -pl legacy-app -Dcore.schema.mysql.it=true -Dtest=CoreSchemaMigrationMySqlIntegrationTest test
```

Expected: FAIL because V20261010.02 does not exist.

- [ ] **Step 3: Implement complete data-copy SQL**

Use `insert ... select` statements with explicit columns:

```sql
insert into device_event
  (device_id, event_type, occurred_at, operator_name, result_status, after_state, remark, created_at)
select device_id, 'inventory', inspected_at, inspector_name, result_status,
       json_object('deviceCode', device_code, 'deviceName', device_name,
                   'normal', is_normal, 'missing', is_missing, 'damaged', is_damaged),
       remark, created_at
from device_inventory_record;

insert into device_event
  (device_id, event_type, occurred_at, operator_name, before_state, after_state, remark, created_at)
select device_id, 'transfer', transfer_at, operator_name,
       json_object('deviceCode', device_code, 'deviceName', device_name,
                   'labId', from_lab_id, 'labName', from_lab_name,
                   'ownerUserId', from_owner_user_id, 'ownerName', from_owner_name,
                   'location', from_location),
       json_object('deviceCode', device_code, 'deviceName', device_name,
                   'labId', to_lab_id, 'labName', to_lab_name,
                   'ownerUserId', to_owner_user_id, 'ownerName', to_owner_name,
                   'location', to_location),
       reason, created_at
from device_transfer_record;

insert into timetable_integration_config
  (config_key, schedule_enabled, cron_expression, semester, semester_start_date,
   credential_username_cipher, credential_password_cipher, updated_at)
select keys.config_key,
       coalesce(c.schedule_enabled, 0), coalesce(c.cron_expression, '0 0 2 * * *'),
       c.semester, str_to_date(c.semester_start_date, '%Y-%m-%d'),
       a.username_cipher, a.password_cipher,
       greatest(coalesce(c.updated_at, '1970-01-01'), coalesce(a.updated_at, '1970-01-01'))
from (
  select config_key from timetable_crawler_config
  union
  select credential_key from timetable_academic_credential
) keys
left join timetable_crawler_config c on c.config_key = keys.config_key
left join timetable_academic_credential a on a.credential_key = keys.config_key;

insert into timetable_import_job
  (id, config_id, trigger_type, status, scope, source, success_account, attempts,
   updated_count, started_at, finished_at, error_message, output, log_path, created_at)
select t.id, c.id, t.trigger_type, t.status, t.scope, t.source, t.success_account,
       coalesce(t.attempts, 0), coalesce(t.updated_count, 0), t.started_at, t.finished_at,
       t.error_message, t.output, t.log_path, t.created_at
from timetable_crawler_task t
left join timetable_integration_config c on c.config_key = 'lab-admin-class-timetable';

insert into audit_event
  (aggregate_type, aggregate_id, action, result_status, actor_name, detail_json, occurred_at, created_at)
select coalesce(json_unquote(json_extract(record_json, '$.category')), 'system'),
       coalesce(
         json_unquote(json_extract(record_json, '$.details.reservationId')),
         json_unquote(json_extract(record_json, '$.details.deviceId')),
         json_unquote(json_extract(record_json, '$.details.repairId')),
         json_unquote(json_extract(record_json, '$.details.labId')),
         json_unquote(json_extract(record_json, '$.details.noticeId'))
       ),
       coalesce(json_unquote(json_extract(record_json, '$.action')), 'unknown'),
       json_unquote(json_extract(record_json, '$.status')),
       json_unquote(json_extract(record_json, '$.operator')),
       json_extract(record_json, '$.details'),
       coalesce(str_to_date(json_unquote(json_extract(record_json, '$.occurredAt')), '%Y-%m-%d %H:%i:%s'), created_at),
       created_at
from module_record
where module_name = 'operation-events';
```

Also backfill `usage_record.user_id` only on an exact unique match to `sys_user.real_name`, backfill `lab_id` only on an exact unique match to `lab.lab_name` or `lab.lab_code`, and leave ambiguous rows null.

- [ ] **Step 4: Run contract and integration tests**

```powershell
mvn.cmd -pl legacy-app -Dcore.schema.mysql.it=true -Dtest=CoreSchemaMigrationContractTest,CoreSchemaMigrationMySqlIntegrationTest test
```

Expected: PASS; source tables still exist after Flyway target `20261010.2`.

- [ ] **Step 5: Commit the data migration**

```powershell
git add Backend/legacy-app/src/main/resources/db/migration/V20261010_02__migrate_core_data.sql Backend/legacy-app/src/test/java/org/example/backend/database/CoreSchemaMigrationMySqlIntegrationTest.java
git commit -m "feat(database): migrate core data into consolidated tables"
```

---

### Task 4: Switch device inventory and transfer to `device_event`

**Files:**
- Create: `Backend/legacy-app/src/main/java/org/example/backend/entity/DeviceEventEntity.java`
- Create: `Backend/legacy-app/src/main/java/org/example/backend/mapper/DeviceEventMapper.java`
- Create: `Backend/legacy-app/src/main/resources/mapper/DeviceEventMapper.xml`
- Modify: `Backend/legacy-app/src/main/java/org/example/backend/service/impl/DevicesServiceImpl.java`
- Delete: `Backend/legacy-app/src/main/java/org/example/backend/mapper/DeviceInventoryRecordMapper.java`
- Delete: `Backend/legacy-app/src/main/java/org/example/backend/mapper/DeviceTransferRecordMapper.java`
- Delete: `Backend/legacy-app/src/main/resources/mapper/DeviceInventoryRecordMapper.xml`
- Delete: `Backend/legacy-app/src/main/resources/mapper/DeviceTransferRecordMapper.xml`
- Create: `Backend/legacy-app/src/test/java/org/example/backend/device/DeviceEventPersistenceContractTest.java`
- Modify: `Backend/legacy-app/src/test/java/org/example/backend/device/DeviceModuleBoundaryTest.java`

**Interfaces:**
- Consumes: existing `DeviceInventoryRecordEntity` and `DeviceTransferRecordEntity` service inputs/outputs.
- Produces: `insertInventoryEvent`, `insertTransferEvent`, `listInventoryEvents`, and `listTransferEvents` Mapper methods.
- Preserves: `/devices/{id}/inventory`, `/devices/{id}/transfer`, and existing list response shapes.

- [ ] **Step 1: Write the failing persistence contract**

```java
@Test
void deviceLifecycleUsesOneEventTableWithoutRuntimeDdl() throws Exception {
    String mapper = Files.readString(Path.of("src/main/resources/mapper/DeviceEventMapper.xml"));
    String service = Files.readString(Path.of("src/main/java/org/example/backend/service/impl/DevicesServiceImpl.java"));
    assertAll(
            () -> assertTrue(mapper.contains("insert into device_event")),
            () -> assertTrue(mapper.contains("event_type = 'inventory'")),
            () -> assertTrue(mapper.contains("event_type = 'transfer'")),
            () -> assertFalse(service.contains("createTableIfNotExists")),
            () -> assertFalse(service.contains("DeviceInventoryRecordMapper")),
            () -> assertFalse(service.contains("DeviceTransferRecordMapper")),
            () -> assertTrue(service.contains("DeviceEventMapper"))
    );
}
```

- [ ] **Step 2: Run and confirm RED**

```powershell
mvn.cmd -pl legacy-app -Dtest=DeviceEventPersistenceContractTest test
```

- [ ] **Step 3: Add the event entity and Mapper interface**

```java
@Data
public class DeviceEventEntity {
    private Long id;
    private Long deviceId;
    private String eventType;
    private LocalDateTime occurredAt;
    private Long operatorUserId;
    private String operatorName;
    private String resultStatus;
    private String beforeState;
    private String afterState;
    private String remark;
    private LocalDateTime createdAt;
}
```

```java
@Mapper
public interface DeviceEventMapper {
    int insertInventoryEvent(DeviceInventoryRecordEntity record);
    int insertTransferEvent(DeviceTransferRecordEntity record);
    List<DeviceInventoryRecordEntity> listInventoryEvents(@Param("deviceId") Long deviceId);
    List<DeviceTransferRecordEntity> listTransferEvents(@Param("deviceId") Long deviceId);
}
```

In XML, use `json_object` for writes and `json_unquote(json_extract(...))` for compatibility projections. Do not query the retired tables.

- [ ] **Step 4: Replace both old Mapper dependencies in `DevicesServiceImpl`**

Inject one `DeviceEventMapper`, remove both `createTableIfNotExists()` calls, and retain the existing transaction boundaries: update device current state and insert its event in the same transaction.

- [ ] **Step 5: Run focused and service tests**

```powershell
mvn.cmd -pl legacy-app -Dtest=DeviceEventPersistenceContractTest,DeviceModuleBoundaryTest test
```

Expected: PASS.

- [ ] **Step 6: Commit the device cutover**

```powershell
git add Backend/legacy-app/src/main/java/org/example/backend/entity/DeviceEventEntity.java Backend/legacy-app/src/main/java/org/example/backend/mapper/DeviceEventMapper.java Backend/legacy-app/src/main/resources/mapper/DeviceEventMapper.xml Backend/legacy-app/src/main/java/org/example/backend/service/impl/DevicesServiceImpl.java Backend/legacy-app/src/test/java/org/example/backend/device
git add -u Backend/legacy-app/src/main/java/org/example/backend/mapper Backend/legacy-app/src/main/resources/mapper
git commit -m "refactor(devices): consolidate lifecycle events"
```

---

### Task 5: Switch timetable configuration, credentials, and jobs to consolidated tables

**Files:**
- Modify: `Backend/legacy-app/src/main/java/org/example/backend/mapper/ClassTimetableMapper.java`
- Modify: `Backend/legacy-app/src/main/resources/mapper/ClassTimetableMapper.xml`
- Modify: `Backend/legacy-app/src/main/java/org/example/backend/service/impl/ClassTimetableServiceImpl.java`
- Modify: `Backend/legacy-app/src/main/java/org/example/backend/service/impl/AcademicCredentialServiceImpl.java`
- Create: `Backend/legacy-app/src/test/java/org/example/backend/timetable/TimetablePersistenceContractTest.java`
- Modify: `Backend/legacy-app/src/test/java/org/example/backend/config/DatabaseSchemaInitializerCompatibilityTest.java`

**Interfaces:**
- Preserves: current timetable crawler config, credential, import, task-list, and reservation-conflict service methods.
- Replaces: three old physical tables with `timetable_integration_config` and `timetable_import_job`.

- [ ] **Step 1: Write a failing Mapper contract test**

```java
@Test
void timetableMapperUsesConsolidatedTablesAndNoDdl() throws Exception {
    String xml = Files.readString(Path.of("src/main/resources/mapper/ClassTimetableMapper.xml"));
    String mapper = Files.readString(Path.of("src/main/java/org/example/backend/mapper/ClassTimetableMapper.java"));
    assertAll(
            () -> assertTrue(xml.contains("timetable_integration_config")),
            () -> assertTrue(xml.contains("timetable_import_job")),
            () -> assertFalse(xml.contains("timetable_crawler_config")),
            () -> assertFalse(xml.contains("timetable_academic_credential")),
            () -> assertFalse(xml.contains("timetable_crawler_task")),
            () -> assertFalse(xml.toLowerCase().contains("create table")),
            () -> assertFalse(mapper.contains("createCrawler")),
            () -> assertFalse(mapper.contains("addCrawlerConfigSemesterColumn"))
    );
}
```

- [ ] **Step 2: Run and confirm RED**

```powershell
mvn.cmd -pl legacy-app -Dtest=TimetablePersistenceContractTest test
```

- [ ] **Step 3: Rewrite SQL while preserving Java-facing method behavior**

Keep `getCrawlerConfig`, `upsertCrawlerConfig`, `listCrawlerTasks`, `insertCrawlerTask`, `updateCrawlerTask`, `clearCrawlerTasks`, `getCrawlerCredential`, `upsertCrawlerCredential`, and `deleteCrawlerCredential`. Point config and credential methods to the same `timetable_integration_config` row. Convert `LocalDate` to/from the `date` column rather than persisting date text. Remove every DDL Mapper method and every call to it.

- [ ] **Step 4: Verify credential safety and reservation queries**

```powershell
mvn.cmd -pl legacy-app -Dtest=TimetablePersistenceContractTest,ReservationMapperCalendarContractTest,DatabaseSchemaInitializerCompatibilityTest test
```

Expected: PASS; credential tests assert only encrypted values reach the Mapper.

- [ ] **Step 5: Commit the timetable cutover**

```powershell
git add Backend/legacy-app/src/main/java/org/example/backend/mapper/ClassTimetableMapper.java Backend/legacy-app/src/main/resources/mapper/ClassTimetableMapper.xml Backend/legacy-app/src/main/java/org/example/backend/service/impl/ClassTimetableServiceImpl.java Backend/legacy-app/src/main/java/org/example/backend/service/impl/AcademicCredentialServiceImpl.java Backend/legacy-app/src/test/java/org/example/backend/timetable/TimetablePersistenceContractTest.java Backend/legacy-app/src/test/java/org/example/backend/config/DatabaseSchemaInitializerCompatibilityTest.java
git commit -m "refactor(timetable): consolidate integration persistence"
```

---

### Task 6: Replace generic module records with structured audit events

**Files:**
- Create: `Backend/legacy-app/src/main/java/org/example/backend/entity/AuditEventEntity.java`
- Create: `Backend/legacy-app/src/main/java/org/example/backend/mapper/AuditEventMapper.java`
- Create: `Backend/legacy-app/src/main/resources/mapper/AuditEventMapper.xml`
- Modify: `Backend/legacy-app/src/main/java/org/example/backend/service/impl/BusinessLoopServiceImpl.java`
- Modify: `Backend/legacy-app/src/main/java/org/example/backend/service/impl/BusinessLoopReportServiceImpl.java`
- Modify: `Backend/legacy-app/src/main/resources/mapper/BusinessLoopReportMapper.xml`
- Delete: `Backend/legacy-app/src/main/java/org/example/backend/mapper/ModuleRecordMapper.java`
- Delete: `Backend/legacy-app/src/main/resources/mapper/ModuleRecordMapper.xml`
- Delete: `Backend/legacy-app/src/main/java/org/example/backend/service/ModuleRecordService.java`
- Delete: `Backend/legacy-app/src/main/java/org/example/backend/service/impl/ModuleRecordServiceImpl.java`
- Delete: `Backend/legacy-app/src/main/java/org/example/backend/controller/ModuleRecordController.java`
- Create: `Backend/legacy-app/src/test/java/org/example/backend/audit/AuditEventPersistenceContractTest.java`

**Interfaces:**
- Consumes: existing `BusinessLoopService.recordEvent(category, action, subject, status, details)` calls.
- Produces: append-only rows in `audit_event` and the same business-loop overview response shape for retained modules.

- [ ] **Step 1: Write the failing audit persistence test**

```java
@Test
void businessLoopAppendsStructuredAuditEvents() throws Exception {
    String service = Files.readString(Path.of("src/main/java/org/example/backend/service/impl/BusinessLoopServiceImpl.java"));
    String xml = Files.readString(Path.of("src/main/resources/mapper/AuditEventMapper.xml"));
    assertAll(
            () -> assertTrue(service.contains("AuditEventMapper")),
            () -> assertFalse(service.contains("ModuleRecordMapper")),
            () -> assertTrue(xml.contains("insert into audit_event")),
            () -> assertFalse(xml.contains("delete from audit_event")),
            () -> assertFalse(xml.contains("module_record"))
    );
}
```

- [ ] **Step 2: Run and confirm RED**

```powershell
mvn.cmd -pl legacy-app -Dtest=AuditEventPersistenceContractTest test
```

- [ ] **Step 3: Add structured audit persistence**

```java
@Mapper
public interface AuditEventMapper {
    int insertEvent(AuditEventEntity event);
    List<AuditEventEntity> listRecent(@Param("limit") int limit);
    long countByTypeAndRange(@Param("aggregateType") String aggregateType,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to);
}
```

Map the existing subject into `detail_json.subject`; map `details.reservationId`, `details.deviceId`, or `details.repairId` to `aggregate_id` when present. Redact keys matching `password`, `secret`, `token`, `apiKey`, or `credential` before JSON serialization.

- [ ] **Step 4: Rewrite retained dashboard/business-loop queries**

Remove consumable, course-environment, and template metrics. Keep lab, device, reservation, repair, notice, usage, and audit metrics with the existing top-level response keys used by retained dashboards.

- [ ] **Step 5: Run focused tests**

```powershell
mvn.cmd -pl legacy-app -Dtest=AuditEventPersistenceContractTest,DashboardMapperCalendarContractTest test
```

Expected: both tests pass and retained dashboard calendar metrics still resolve from core tables.

- [ ] **Step 6: Commit the audit cutover**

```powershell
git add Backend/legacy-app/src/main/java/org/example/backend/entity/AuditEventEntity.java Backend/legacy-app/src/main/java/org/example/backend/mapper/AuditEventMapper.java Backend/legacy-app/src/main/resources/mapper/AuditEventMapper.xml Backend/legacy-app/src/main/java/org/example/backend/service/impl/BusinessLoopServiceImpl.java Backend/legacy-app/src/main/java/org/example/backend/service/impl/BusinessLoopReportServiceImpl.java Backend/legacy-app/src/main/resources/mapper/BusinessLoopReportMapper.xml Backend/legacy-app/src/test/java/org/example/backend/audit/AuditEventPersistenceContractTest.java
git add -u Backend/legacy-app/src/main/java/org/example/backend Backend/legacy-app/src/main/resources/mapper
git commit -m "refactor(audit): replace generic module records"
```

---

### Task 7: Normalize device and usage-record references without changing APIs

**Files:**
- Modify: `Backend/legacy-app/src/main/resources/mapper/DevicesMapper.xml`
- Modify: `Backend/legacy-app/src/main/java/org/example/backend/service/impl/DevicesServiceImpl.java`
- Modify: `Backend/legacy-app/src/main/resources/mapper/UsageRecordMapper.xml`
- Modify: `Backend/legacy-app/src/main/java/org/example/backend/entity/UsageRecordEntity.java`
- Modify: `Backend/legacy-app/src/main/java/org/example/backend/service/impl/UsageRecordServiceImpl.java`
- Create: `Backend/legacy-app/src/test/java/org/example/backend/database/CoreReferenceNormalizationTest.java`

**Interfaces:**
- Preserves: `DevicesEntity.labName`, `UsageRecordEntity.person`, and `UsageRecordEntity.resource` response fields.
- Produces: writes by foreign key when a unique user/lab is known; compatibility text remains for historical or anonymous records.

- [ ] **Step 1: Write the failing normalization test**

```java
@Test
void currentStateWritesForeignKeysAndReadsDisplayNamesByJoin() throws Exception {
    String devices = Files.readString(Path.of("src/main/resources/mapper/DevicesMapper.xml"));
    String usage = Files.readString(Path.of("src/main/resources/mapper/UsageRecordMapper.xml"));
    assertAll(
            () -> assertFalse(extractInsert(devices, "insertDevices").contains("lab_name")),
            () -> assertTrue(devices.contains("left join lab l on d.lab_id = l.id")),
            () -> assertTrue(usage.contains("user_id")),
            () -> assertTrue(usage.contains("lab_id")),
            () -> assertTrue(usage.contains("source_type")),
            () -> assertTrue(usage.contains("source_id"))
    );
}
```

- [ ] **Step 2: Run and confirm RED**

```powershell
mvn.cmd -pl legacy-app -Dtest=CoreReferenceNormalizationTest test
```

- [ ] **Step 3: Stop persisting `device.lab_name`**

Remove `lab_name` from device insert/update SQL. Continue selecting `l.lab_name AS labName` through the existing lab join. `DevicesServiceImpl.bindDeviceLab` must set `labId` and location; `labName` remains a response-only field.

- [ ] **Step 4: Add normalized usage fields**

Add entity fields `userId`, `labId`, `sourceType`, and `sourceId`. Update insert/update SQL and service normalization. When an exact unique ID is unavailable, leave the FK null and retain the current `person`/`resource` text.

- [ ] **Step 5: Run focused core tests**

```powershell
mvn.cmd -pl legacy-app -Dtest=CoreReferenceNormalizationTest,DeviceModuleBoundaryTest,ReservationMapperCalendarContractTest test
```

- [ ] **Step 6: Commit normalization**

```powershell
git add Backend/legacy-app/src/main/resources/mapper/DevicesMapper.xml Backend/legacy-app/src/main/java/org/example/backend/service/impl/DevicesServiceImpl.java Backend/legacy-app/src/main/resources/mapper/UsageRecordMapper.xml Backend/legacy-app/src/main/java/org/example/backend/entity/UsageRecordEntity.java Backend/legacy-app/src/main/java/org/example/backend/service/impl/UsageRecordServiceImpl.java Backend/legacy-app/src/test/java/org/example/backend/database/CoreReferenceNormalizationTest.java
git commit -m "refactor(database): normalize core references"
```

---

### Task 8: Remove retired backend modules and all runtime DDL

**Files:**
- Delete: retired controllers, services, entities, mappers, WebSocket/IoT adapters listed below.
- Delete: `Backend/legacy-app/src/main/java/org/example/backend/config/DatabaseSchemaInitializer.java`
- Modify: `Backend/legacy-app/src/main/java/org/example/backend/security/SecurityAccessPolicy.java`
- Modify: `Backend/legacy-app/src/main/java/org/example/backend/BackendApplication.java`
- Modify: `Backend/legacy-app/src/main/java/org/example/backend/service/impl/DashboardServiceImpl.java`
- Modify: `Backend/legacy-app/src/main/resources/mapper/DashboardMapper.xml`
- Modify: `Backend/legacy-app/src/main/resources/mapper/NoticeMapper.xml`
- Modify: `Backend/legacy-app/src/main/java/org/example/backend/service/impl/NoticeServiceImpl.java`
- Modify: `Backend/legacy-app/pom.xml`
- Modify: `Backend/legacy-app/src/test/java/org/example/backend/BackendApplicationTests.java`
- Preserve and re-run: `Backend/legacy-app/src/test/java/org/example/backend/maintenance/MaintenanceRecordNavigationContractTest.java`
- Modify without reversing maintenance-role retirement: `Backend/legacy-app/src/test/java/org/example/backend/security/SecurityAccessPolicyTest.java`
- Create: `Backend/legacy-app/src/test/java/org/example/backend/database/RetiredModuleBoundaryTest.java`
- Delete or rewrite: `Backend/legacy-app/src/test/java/org/example/backend/device/DeviceModuleBoundaryTest.java`
- Delete or rewrite: `Backend/legacy-app/src/test/java/org/example/backend/config/DatabaseSchemaInitializerCompatibilityTest.java`

**Interfaces:**
- Preserves: security rules and dashboard queries for all retained endpoints.
- Removes: retired endpoints and all Java/XML references to the 28 replaced/retired source tables.

- [ ] **Step 1: Write a failing retired-module boundary test**

```java
@Test
void productionSourcesReferenceOnlyTargetBusinessTables() throws Exception {
    Set<String> retired = Set.of(
        "ai_assistant_conversation","ai_assistant_user_config","approval_countersign",
        "consumable_inventory","consumable_stock_record","course_environment_request",
        "device_inventory_record","device_runtime_status","device_status_event","device_transfer_record",
        "environment_template","host_static_asset","iot_command_log","iot_device",
        "iot_device_capability","iot_gateway_config","iot_gateway_point","iot_telemetry_latest",
        "lab_activation_code","lab_software","lab_terminal","module_record","schedule_adjustment",
        "student_client","teacher_host","timetable_academic_credential",
        "timetable_crawler_config","timetable_crawler_task"
    );
    String production = readAllText(Path.of("src/main"));
    for (String table : retired) assertFalse(production.contains(table), table);
    assertFalse(production.toLowerCase().contains("create table"));
    assertFalse(production.toLowerCase().contains("alter table"));
    assertFalse(production.toLowerCase().contains("drop table"));
}
```

Exclude `src/main/resources/db/migration` from `readAllText` because DDL is valid only there.

- [ ] **Step 2: Run and confirm RED**

```powershell
mvn.cmd -pl legacy-app -Dtest=RetiredModuleBoundaryTest test
```

- [ ] **Step 3: Delete retired backend vertical slices**

Delete the exact retired sources with one repository-scoped `git rm` operation:

```powershell
git rm -- `
  Backend/legacy-app/src/main/java/org/example/backend/config/AiAssistantWebSocketConfig.java `
  Backend/legacy-app/src/main/java/org/example/backend/config/IotHardwareProperties.java `
  Backend/legacy-app/src/main/java/org/example/backend/controller/ActivationCodeController.java `
  Backend/legacy-app/src/main/java/org/example/backend/controller/AiAssistantConfigController.java `
  Backend/legacy-app/src/main/java/org/example/backend/controller/AiConversationController.java `
  Backend/legacy-app/src/main/java/org/example/backend/controller/ApprovalCountersignController.java `
  Backend/legacy-app/src/main/java/org/example/backend/controller/ConsumableController.java `
  Backend/legacy-app/src/main/java/org/example/backend/controller/CourseEnvironmentController.java `
  Backend/legacy-app/src/main/java/org/example/backend/controller/DeviceStatusController.java `
  Backend/legacy-app/src/main/java/org/example/backend/controller/EnvironmentTemplateController.java `
  Backend/legacy-app/src/main/java/org/example/backend/controller/IotHardwareController.java `
  Backend/legacy-app/src/main/java/org/example/backend/controller/ScheduleAdjustmentController.java `
  Backend/legacy-app/src/main/java/org/example/backend/controller/StudentClientController.java `
  Backend/legacy-app/src/main/java/org/example/backend/controller/SystemSettingsController.java `
  Backend/legacy-app/src/main/java/org/example/backend/controller/TeacherHostController.java `
  Backend/legacy-app/src/main/java/org/example/backend/controller/softwareController.java `
  Backend/legacy-app/src/main/java/org/example/backend/entity/AiAssistantUserConfigEntity.java `
  Backend/legacy-app/src/main/java/org/example/backend/entity/AiConversationEntity.java `
  Backend/legacy-app/src/main/java/org/example/backend/entity/ApprovalCountersignEntity.java `
  Backend/legacy-app/src/main/java/org/example/backend/entity/ConsumableEntity.java `
  Backend/legacy-app/src/main/java/org/example/backend/entity/ConsumableStockRecordEntity.java `
  Backend/legacy-app/src/main/java/org/example/backend/entity/CourseEnvironmentEntity.java `
  Backend/legacy-app/src/main/java/org/example/backend/entity/EnvironmentTemplateEntity.java `
  Backend/legacy-app/src/main/java/org/example/backend/entity/ScheduleAdjustmentEntity.java `
  Backend/legacy-app/src/main/java/org/example/backend/entity/StudentClientEntity.java `
  Backend/legacy-app/src/main/java/org/example/backend/entity/TeacherHostEntity.java `
  Backend/legacy-app/src/main/java/org/example/backend/entity/softwareEntity.java `
  Backend/legacy-app/src/main/java/org/example/backend/mapper/AiAssistantUserConfigMapper.java `
  Backend/legacy-app/src/main/java/org/example/backend/mapper/AiConversationMapper.java `
  Backend/legacy-app/src/main/java/org/example/backend/mapper/ApprovalCountersignMapper.java `
  Backend/legacy-app/src/main/java/org/example/backend/mapper/ConsumableMapper.java `
  Backend/legacy-app/src/main/java/org/example/backend/mapper/CourseEnvironmentMapper.java `
  Backend/legacy-app/src/main/java/org/example/backend/mapper/EnvironmentTemplateMapper.java `
  Backend/legacy-app/src/main/java/org/example/backend/mapper/ScheduleAdjustmentMapper.java `
  Backend/legacy-app/src/main/java/org/example/backend/mapper/StudentClientMapper.java `
  Backend/legacy-app/src/main/java/org/example/backend/mapper/TeacherHostMapper.java `
  Backend/legacy-app/src/main/java/org/example/backend/mapper/softwareMapper.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/AiAssistantService.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/AiAssistantUserConfigService.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/AiConversationService.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/ApprovalCountersignService.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/ConsumableService.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/CourseEnvironmentService.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/DeviceStatusService.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/EnvironmentTemplateService.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/IotDeviceService.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/IotHardwareService.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/ScheduleAdjustmentService.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/SpeechRecognitionService.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/StudentClientService.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/TeacherHostService.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/TongyiAgentService.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/softwareService.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/impl/AiAssistantServiceImpl.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/impl/AiAssistantUserConfigServiceImpl.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/impl/AiConversationServiceImpl.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/impl/ApprovalCountersignServiceImpl.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/impl/ConsumableServiceImpl.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/impl/CourseEnvironmentServiceImpl.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/impl/DeviceStatusServiceImpl.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/impl/EnvironmentTemplateServiceImpl.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/impl/IotDeviceServiceImpl.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/impl/IotHardwareServiceImpl.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/impl/ScheduleAdjustmentServiceImpl.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/impl/StudentClientServiceImpl.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/impl/TeacherHostServiceImpl.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/impl/TongyiAgentServiceImpl.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/impl/VoskSpeechRecognitionServiceImpl.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/impl/softwareServiceImpl.java `
  Backend/legacy-app/src/main/java/org/example/backend/service/gateway `
  Backend/legacy-app/src/main/java/org/example/backend/websocket/AiAssistantWebSocketHandler.java `
  Backend/legacy-app/src/main/resources/application-iot-example.yaml `
  Backend/legacy-app/src/main/resources/mapper/AiAssistantUserConfigMapper.xml `
  Backend/legacy-app/src/main/resources/mapper/AiConversationMapper.xml `
  Backend/legacy-app/src/main/resources/mapper/ApprovalCountersignMapper.xml `
  Backend/legacy-app/src/main/resources/mapper/ConsumableMapper.xml `
  Backend/legacy-app/src/main/resources/mapper/CourseEnvironmentMapper.xml `
  Backend/legacy-app/src/main/resources/mapper/EnvironmentTemplateMapper.xml `
  Backend/legacy-app/src/main/resources/mapper/ScheduleAdjustmentMapper.xml `
  Backend/legacy-app/src/main/resources/mapper/StudentClientMapper.xml `
  Backend/legacy-app/src/main/resources/mapper/TeacherHostMapper.xml `
  Backend/legacy-app/src/main/resources/mapper/softwareMapper.xml
```

`ModuleRecordController`, its Service/Mapper, and both XML/Java persistence files were already deleted in Task 6. Remove Vosk and WebSocket dependencies from `legacy-app/pom.xml` after `rg` confirms no retained source imports them.

- [ ] **Step 4: Remove retired references from retained services**

Remove consumable/environment counts from business-loop and notice generation. Rewrite dashboard SQL to use only target tables. Remove retired URL prefixes from `SecurityAccessPolicy`; do not loosen any retained role rule.

- [ ] **Step 5: Remove runtime DDL**

Delete `DatabaseSchemaInitializer`. Remove all Mapper DDL methods still present in `ClassTimetableMapper`, `ConsumableMapper`, or record Mappers as those modules are removed. Verify with:

```powershell
rg -n -i "create table|alter table|drop table" Backend/legacy-app/src/main --glob '!resources/db/migration/**'
```

Expected: no matches.

- [ ] **Step 6: Run backend tests**

After the separate maintenance-navigation work is committed, align the entry-point source contract and retain the maintenance-role retirement assertions:

```java
// BackendApplicationTests: the entry point uses a SpringApplication instance.
int secretsIndex = source.indexOf("LocalDevelopmentSecretsInitializer.ensure");
int springRunIndex = source.indexOf("application.run");
assertTrue(secretsIndex >= 0);
assertTrue(springRunIndex > secretsIndex);

// MaintenanceRecordNavigationContractTest / SecurityAccessPolicyTest:
assertTrue(policy.isAllowed("GET", "/maintenance-records", "labAdmin"));
assertFalse(policy.isAllowed("GET", "/maintenance-records", "teacher"));
assertFalse(policy.isAllowed("GET", "/repairs", "maintenance"));
assertFalse(policy.isAllowed("GET", "/dashboard", "maintenance"));
assertFalse(policy.isAllowed("DELETE", "/users/1", "maintenance"));
```

Delete assertions for retired activation, consumable, IoT, software environment, course environment, schedule adjustment, and system-settings endpoints. Keep all retained authorization assertions.

```powershell
mvn.cmd -pl legacy-app test
```

Expected: all retained tests pass. Delete tests whose only contract was a retired module; rewrite mixed tests so retained device, error handling, and dashboard assertions remain.

- [ ] **Step 7: Commit backend retirement**

```powershell
git add -u Backend/legacy-app
git add Backend/legacy-app/src/test/java/org/example/backend/database/RetiredModuleBoundaryTest.java
git commit -m "refactor(backend): remove retired data modules"
```

---

### Task 9: Remove retired frontend modules while preserving core routes

**Files:**
- Modify: `web/src/constants/routeModules.js`
- Modify: `web/src/router/index.js`
- Modify: `web/src/constants/navigation.js`
- Modify: `web/src/constants/dashboard.js`
- Modify: `web/src/constants/managementModules.js`
- Modify: `web/src/constants/permissions.js`
- Modify: `web/src/views/roles/LabAdminPortal.vue`
- Modify: `web/src/views/roles/SystemAdminPortal.vue`
- Modify: `web/src/views/roles/TeacherPortal.vue`
- Modify: `web/src/views/teacher/TeacherSchedule.vue`
- Delete: `web/src/views/ai-assistant/AiAssistant.vue`
- Delete: `web/src/views/consumables/ConsumableInventory.vue`
- Delete: `web/src/views/device-status/DeviceStatusPage.vue`
- Delete: `web/src/views/environments/EnvironmentManagement.vue`
- Delete: `web/src/views/environments/components/CourseEnvironmentDialog.vue`
- Delete: `web/src/views/environments/components/EnvironmentTemplateDialog.vue`
- Delete: `web/src/views/environments/components/SoftwareEnvironmentDialog.vue`
- Delete: `web/src/views/room-control/RoomControlCenter.vue`
- Delete: `web/src/views/room-control/components/DeviceControlPanel.vue`
- Delete: `web/src/views/room-control/components/EventRecordPanel.vue`
- Delete: `web/src/views/room-control/components/LabSafetyPanel.vue`
- Delete: `web/src/views/room-control/components/LabSelectorPanel.vue`
- Delete: `web/src/views/room-control/components/RoomControlHeader.vue`
- Delete: `web/src/views/system/ActivationCodeManagement.vue`
- Delete: `web/src/views/system/SystemSettings.vue`
- Delete: `web/src/views/teacher/TeacherAdjustments.vue`
- Delete: `web/src/views/teacher/TeacherCourseEnvironments.vue`
- Delete: `web/src/views/teacher/components/TeacherAdjustmentDialog.vue`
- Delete: `web/src/views/teacher/components/TeacherEnvironmentDrawer.vue`
- Delete: `web/src/views/teacher/components/TeacherEnvironmentRequestDialog.vue`
- Delete: `web/src/api/services/activationCodes.js`
- Delete: `web/src/api/services/aiAssistantConfig.js`
- Delete: `web/src/api/services/aiAssistantHistory.js`
- Delete: `web/src/api/services/deviceActivation.js`
- Delete: `web/src/api/services/deviceStatus.js`
- Delete: `web/src/api/services/iot.js`
- Delete: `web/src/api/services/scheduleAdjustments.js`
- Delete: `web/src/api/services/systemSettings.js`
- Delete: `web/src/composables/useBusinessModuleRecords.js`
- Delete: `web/src/composables/useRoomControl.js`
- Create: `web/tests/core-module-boundary.test.mjs`
- Create: `web/tests/core-flow-regression.test.mjs`
- Delete or rewrite: `web/tests/device-status-page-boundary.test.mjs`
- Delete or rewrite: `web/tests/room-control-boundary.test.mjs`
- Modify: `web/tests/frontend-service-boundaries.test.mjs`
- Modify: `web/tests/ui-issue-regressions.test.mjs`

**Interfaces:**
- Preserves routes for lab admin core pages including maintenance-record entry/viewing, system users/notices/statistics, teacher schedule/timetable, and public scan reservation.
- Removes retired navigation entries, redirects, lazy imports, endpoint constants, and permissions.

- [ ] **Step 1: Write failing frontend boundary tests**

```js
import { readFileSync } from 'node:fs'
import test from 'node:test'
import assert from 'node:assert/strict'

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('router exposes only approved core modules', () => {
  const modules = read('../src/constants/routeModules.js')
  const router = read('../src/router/index.js')
  for (const retired of ['ai-assistant', 'consumables', 'environments', 'room-control',
    'device-status', 'activation-codes', 'course-environments']) {
    assert.doesNotMatch(modules + router, new RegExp(retired))
  }
  for (const core of ['labs', 'devices', 'reservations', 'maintenance-records', 'records',
    'statistics', 'notices', 'class-timetable', 'users', 'schedule']) {
    assert.match(modules + router, new RegExp(core))
  }
})

test('public scan reservation remains public', () => {
  const router = read('../src/router/index.js')
  assert.match(router, /path: '\/scan\/reserve'/)
  assert.match(router, /public: true/)
})
```

- [ ] **Step 2: Run and confirm RED**

```powershell
node --test web/tests/core-module-boundary.test.mjs web/tests/core-flow-regression.test.mjs
```

- [ ] **Step 3: Remove retired routes, imports, navigation, permissions, and cards**

Delete only retired entries. Retain the teacher schedule route and refactor `TeacherSchedule.vue` to remove adjustment/environment dialogs while keeping timetable display. Redirect old retired paths to the role home page rather than a missing component.

- [ ] **Step 4: Delete retired pages, services, and composables**

Delete the files listed above. Use `rg` to remove dangling imports and endpoint references:

```powershell
rg -n "ai-assistant|consumable|course-environment|environment-template|activation-code|room-control|device-status|schedule-adjustment" web/src
```

Expected: only explicit legacy redirect comments are allowed; remove all executable imports and calls.

- [ ] **Step 5: Run frontend tests and production build**

```powershell
node --test web/tests/*.test.mjs
npm.cmd --prefix web run build
```

Expected: all tests pass and Vite build succeeds without unresolved imports.

- [ ] **Step 6: Commit frontend retirement**

```powershell
git add -u web/src web/tests
git add web/tests/core-module-boundary.test.mjs web/tests/core-flow-regression.test.mjs
git commit -m "refactor(frontend): focus navigation on core modules"
```

---

### Task 10: Add the guarded shrink migration and complete end-to-end verification

**Files:**
- Create: `Backend/legacy-app/src/main/resources/db/migration/V20261010_03__shrink_retired_schema.sql`
- Modify: `Backend/legacy-app/src/test/java/org/example/backend/database/CoreSchemaMigrationContractTest.java`
- Modify: `Backend/legacy-app/src/test/java/org/example/backend/database/CoreSchemaMigrationMySqlIntegrationTest.java`
- Modify: `Backend/database/maintenance/20261010-core-consolidation/README.md`
- Create: `docs/database/2026-10-10-core-database-migration-report.md` during the real rehearsal/migration run.

**Interfaces:**
- Consumes: a successful backup manifest and restore rehearsal report through the guarded PowerShell runner.
- Produces: exactly 15 business tables plus `flyway_schema_history`.
- Produces: a migration report containing before/after row counts, constraints, test commands, and backup paths without secrets.

- [ ] **Step 1: Extend the integration test with final table-set assertions**

```java
private static final Set<String> TARGET_TABLES = Set.of(
    "sys_user", "role", "user_role", "lab", "device", "device_event",
    "class_timetable", "lab_reservation", "timetable_integration_config",
    "timetable_import_job", "repair_order", "usage_record", "notice",
    "notice_recipient", "audit_event", "flyway_schema_history"
);

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
```

- [ ] **Step 2: Run and confirm RED**

```powershell
mvn.cmd -pl legacy-app -Dcore.schema.mysql.it=true -Dtest=CoreSchemaMigrationMySqlIntegrationTest test
```

- [ ] **Step 3: Write the shrink migration in foreign-key-safe order**

The SQL must drop child tables before parents and finish with the replaced source tables:

```sql
drop table if exists iot_command_log;
drop table if exists iot_telemetry_latest;
drop table if exists iot_gateway_point;
drop table if exists iot_device_capability;
drop table if exists iot_gateway_config;
drop table if exists iot_device;
drop table if exists device_status_event;
drop table if exists device_runtime_status;
drop table if exists lab_terminal;
drop table if exists lab_activation_code;
drop table if exists student_client;
drop table if exists teacher_host;
drop table if exists consumable_stock_record;
drop table if exists consumable_inventory;
drop table if exists course_environment_request;
drop table if exists environment_template;
drop table if exists lab_software;
drop table if exists schedule_adjustment;
drop table if exists approval_countersign;
drop table if exists ai_assistant_conversation;
drop table if exists ai_assistant_user_config;
drop table if exists host_static_asset;
drop table if exists device_inventory_record;
drop table if exists device_transfer_record;
drop table if exists timetable_crawler_task;
drop table if exists timetable_academic_credential;
drop table if exists timetable_crawler_config;
drop table if exists module_record;
alter table device drop column lab_name;
```

- [ ] **Step 4: Run all automated verification on a rehearsal schema**

```powershell
mvn.cmd -f Backend/pom.xml test
node --test web/tests/*.test.mjs
npm.cmd --prefix web run build
mvn.cmd -f Backend/pom.xml -pl legacy-app -Dcore.schema.mysql.it=true -Dtest=CoreSchemaMigrationMySqlIntegrationTest test
```

Expected: all retained backend tests pass, all frontend tests pass, build succeeds, and the migration integration test reports exactly 16 physical tables.

- [ ] **Step 5: Back up and rehearse against a copy of the real database**

Stop backend writes, then execute:

```powershell
$env:MYSQL_PWD = Read-Host 'MySQL password' -MaskInput
Set-Location 'Backend/database/maintenance/20261010-core-consolidation'
$archive = ./Backup-CoreDatabase.ps1 -Database smart_lab_basic -ArchiveRoot 'E:\smart-lab-db-archives'
./Test-CoreDatabaseBackup.ps1 -ArchiveDirectory $archive
./Invoke-CoreDatabaseMigration.ps1 -Phase Expand -ArchiveDirectory $archive
```

Start the cutover application against the expanded schema and run the core API/browser smoke matrix. Do not run `-Phase Shrink` until every smoke check passes.

- [ ] **Step 6: Execute guarded shrink and verify live schema**

```powershell
./Invoke-CoreDatabaseMigration.ps1 -Phase Shrink -ArchiveDirectory $archive
```

Query `information_schema.tables`, row counts, foreign-key orphans, and Flyway success. Restart the backend twice and confirm no retired table is recreated.

- [ ] **Step 7: Perform real core smoke tests**

Verify, in order:

1. Login for lab admin, system admin, and teacher roles; confirm retired maintenance-role sessions are rejected with the existing explicit message.
2. Lab list/detail/update.
3. Device list/create/update, inventory, transfer, and both history views.
4. Public phone scan reservation, admin approval/rejection/cancellation, capacity conflict, timetable conflict.
5. Timetable config, encrypted credential status, manual import, task list, and class timetable view.
6. Lab-admin maintenance-record create, update, detail, list, and export; confirm retired work-order operations remain absent.
7. Notice publish, recipient read, archive, and delete.
8. Usage records, dashboard, and statistics.

Any 500 response, missing core route, response contract mismatch, or unexplained count difference triggers rollback from `full.sql`.

- [ ] **Step 8: Write the migration report**

Record exact backup directory, SHA-256 values, before/after table sets and row counts, Flyway versions, automated test outputs, smoke-test results, exceptions, and rollback readiness. Store database usernames only in the local operator log, not in the committed report; always redact passwords, tokens, ciphertext, and API keys.

- [ ] **Step 9: Commit the shrink migration and verified report**

```powershell
git add Backend/legacy-app/src/main/resources/db/migration/V20261010_03__shrink_retired_schema.sql Backend/legacy-app/src/test/java/org/example/backend/database/CoreSchemaMigrationContractTest.java Backend/legacy-app/src/test/java/org/example/backend/database/CoreSchemaMigrationMySqlIntegrationTest.java Backend/database/maintenance/20261010-core-consolidation/README.md docs/database/2026-10-10-core-database-migration-report.md
git commit -m "feat(database): complete guarded core schema consolidation"
```

---

## Final Verification Gate

Before claiming completion, run all commands below from the repository root and retain their outputs in the migration report:

```powershell
git status --short
mvn.cmd -f Backend/pom.xml test
node --test web/tests/*.test.mjs
npm.cmd --prefix web run build
mvn.cmd -f Backend/pom.xml -pl legacy-app -Dcore.schema.mysql.it=true -Dtest=CoreSchemaMigrationMySqlIntegrationTest test
```

Then verify the live/rehearsal database:

```sql
select table_name
from information_schema.tables
where table_schema = 'smart_lab_basic'
order by table_name;

select installed_rank, version, description, success
from flyway_schema_history
order by installed_rank;
```

Expected table count: 16 physical tables = 15 approved business tables + `flyway_schema_history`. Completion is blocked unless backup restoration, migration checks, automated tests, the core browser/API smoke matrix, and two consecutive backend restarts all pass.
