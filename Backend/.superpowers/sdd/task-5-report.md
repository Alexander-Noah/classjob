# Task 5 Report: Timetable persistence consolidation

## Implementation

- Removed all runtime DDL mapper methods and service calls.
- Routed crawler configuration and encrypted credentials through
  `timetable_integration_config`.
- Routed crawler task history through `timetable_import_job`, including the
  consolidated configuration foreign key.
- Bound `semester_start_date` with `LocalDate` instead of a date string.
- Changed credential deletion to clear cipher columns instead of deleting the
  shared configuration row.
- Replaced the file-backed academic credential implementation with the mapper
  while retaining AES-GCM encryption/decryption, masked views, key normalization,
  and strong-secret validation through the real mapper-injected constructor.
- Added the mapper contract test from the brief and a mapper-capture test proving
  plaintext username/password values are not sent to the persistence boundary.

## TDD Evidence

1. Added `TimetablePersistenceContractTest` before production changes.
2. Ran:

   ```powershell
   mvn.cmd -pl legacy-app -Dtest=TimetablePersistenceContractTest test
   ```

   This did not reach the test because the existing dirty worktree's legacy app
   compilation referenced module classes that had been moved by unrelated work
   and were not available from installed artifacts.

## Verification Evidence

| Command | Result |
| --- | --- |
| `mvn.cmd -pl legacy-app -am -DskipTests compile` | PASS. All 10 reactor projects, including `legacy-app`, compiled successfully. |
| `mvn.cmd -pl legacy-app '-Dtest=TimetablePersistenceContractTest,AcademicCredentialServiceImplSecurityTest,ReservationMapperCalendarContractTest,DatabaseSchemaInitializerCompatibilityTest' test` | BLOCKED at `legacy-app` test compilation by unrelated existing test imports that still reference pre-move package names. The selected tests did not execute. |
| PowerShell XML parse for `ClassTimetableMapper.xml` | PASS. Mapper XML is well-formed. |
| Production-source legacy table/runtime DDL scan | PASS. No legacy timetable tables or mapper DDL identifiers remain in the Task 5 production files. |
| `git diff --check` for Task 5 files | PASS. No whitespace errors. |

## Test Compile Blocker

The focused suite cannot compile until unrelated existing tests are updated for
the module package moves already present in the shared worktree. Representative
failures are in:

- `ClassTimetableQueryCompatibilityTest` importing the old teaching-schedule web package.
- `LabQueryCompatibilityTest` importing the old lab-resource web package.
- `MySqlReservationConcurrencyIntegrationTest` importing old reservation infrastructure/API packages.

These files are outside Task 5 scope and were not modified.

## Changed Files

- `Backend/legacy-app/src/main/java/org/example/backend/mapper/ClassTimetableMapper.java`
- `Backend/legacy-app/src/main/resources/mapper/ClassTimetableMapper.xml`
- `Backend/legacy-app/src/main/java/org/example/backend/service/impl/ClassTimetableServiceImpl.java`
- `Backend/legacy-app/src/main/java/org/example/backend/service/impl/AcademicCredentialServiceImpl.java`
- `Backend/legacy-app/src/test/java/org/example/backend/timetable/TimetablePersistenceContractTest.java`
- `Backend/legacy-app/src/test/java/org/example/backend/config/DatabaseSchemaInitializerCompatibilityTest.java`
- `Backend/legacy-app/src/test/java/org/example/backend/service/impl/AcademicCredentialServiceImplSecurityTest.java`

No commit was created.

## Review Fixes for 6818d05

- Updated the production calendar joins in `DashboardMapper.xml` and
  `UsageRecordMapper.xml` to use `timetable_integration_config` while keeping
  the existing `crawler_config` alias and response/query shapes.
- Removed `str_to_date` around `crawler_config.semester_start_date`; the
  consolidated schema stores that value as a MySQL `DATE`.
- Added an idempotent `create table if not exists class_timetable` definition
  to `V20261010_01__expand_core_schema.sql`, including the columns consumed by
  `ClassTimetableMapper.insertTimetable` and `ClassTimetableEntity`.
- Updated calendar contract tests to reject the retired table and DATE string
  conversion explicitly.
- Updated `CoreSchemaMigrationContractTest` and the MySQL integration fixture
  to represent and preserve a `class_timetable` row.
- Strengthened `TimetablePersistenceContractTest` to parse the mapper with
  `XMLMapperBuilder`, inspect bound SQL for retired-table references, require
  `config_id` in task inserts, and capture encrypted credential writes plus
  clear-not-delete credential behavior.

## Review-Fix Verification

| Command | Result |
| --- | --- |
| `mvn.cmd -pl legacy-app -am '-Dtest=TimetablePersistenceContractTest,AcademicCredentialServiceImplSecurityTest,ReservationMapperCalendarContractTest,DashboardMapperCalendarContractTest,UsageRecordMapperCalendarContractTest,DatabaseSchemaInitializerCompatibilityTest,CoreSchemaMigrationContractTest' test` | BLOCKED before `legacy-app`: `platform-web-support` fails Surefire with `No tests matching pattern ...` because the reactor-wide command selects tests absent from upstream modules. |
| `mvn.cmd -pl legacy-app -am '-Dtest=TimetablePersistenceContractTest,AcademicCredentialServiceImplSecurityTest,ReservationMapperCalendarContractTest,DashboardMapperCalendarContractTest,UsageRecordMapperCalendarContractTest,DatabaseSchemaInitializerCompatibilityTest,CoreSchemaMigrationContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` | PASS. Legacy app ran 12 tests with 0 failures, 0 errors, and 0 skipped. |
| `mvn.cmd -pl legacy-app -am -DskipTests compile` | PASS. All 10 reactor projects compiled. |
| PowerShell `[xml]` parse of `DashboardMapper.xml`, `UsageRecordMapper.xml`, and `ClassTimetableMapper.xml` | PASS. |
| `git diff --check` on all Task 5 review-fix files | PASS. |
| `CoreSchemaMigrationMySqlIntegrationTest` | Not run; requires `-Dcore.schema.mysql.it=true` and an available MySQL instance. The fixture and assertions were updated, and the static migration contract passed. |
