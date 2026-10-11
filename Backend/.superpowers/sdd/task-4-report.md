# Task 4: Device lifecycle event consolidation report

## Scope

Switched device inventory and transfer persistence from the two retired record
tables to the consolidated `device_event` table. The service keeps the existing
HTTP-facing entities and result shapes, while one MyBatis mapper writes and
projects both event types. Runtime table creation calls were removed.

## TDD and verification evidence

The persistence contract was added before the implementation change and checks
the consolidated table, event-type predicates, and removal of runtime DDL and
retired mapper dependencies.

Focused verification:

```text
mvn.cmd -pl legacy-app '-Dtest=DeviceEventPersistenceContractTest,DeviceModuleBoundaryTest,DeviceImportSecurityTest,DevicesServiceTransactionTest' test
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Review-finding verification:

```text
mvn.cmd -pl legacy-app '-Dtest=DeviceEventPersistenceContractTest,DeviceModuleBoundaryTest,DeviceImportSecurityTest,DevicesServiceTransactionTest' test
[ERROR] COMPILATION ERROR
[ERROR] BackendApplication.java: cannot find symbol LabResourceRuntimeConfiguration
[ERROR] BackendApplication.java: cannot find symbol ReservationPersistenceRuntimeConfiguration
[ERROR] BackendApplication.java: cannot find symbol TeachingScheduleRuntimeConfiguration
[ERROR] LegacyLabMetadataAdapter.java: cannot find symbols DeviceCountPort and ManagerNamePort
BUILD FAILURE
```

The inventory and transfer event inserts explicitly use MyBatis generated-key
propagation (`useGeneratedKeys="true" keyProperty="id"`), preserving the
input record object's generated `id` for service results and API responses.

Compilation verification:

```text
mvn.cmd -pl legacy-app -DskipTests compile
BUILD SUCCESS
```

The transfer update and event insert remain in the existing transactional
method. Inventory event insertion and inventory-state update now share a
transactional method boundary, which is covered by
`DevicesServiceTransactionTest`.

## Safety notes

- `DeviceEventMapper.xml` writes only to `device_event` and reads only from
  `device_event` for lifecycle history projections.
- The old inventory and transfer mapper Java/XML files are deleted.
- No production or local `smart_lab_basic` database was mutated by this task.
- No credentials, ciphertext, or database secrets were added to source or
  reports.

## Residual concern

The SQL compatibility projections are covered by source contracts and service
tests; the real MySQL migration integration test remains the database-level
verification for migrated historical rows and will be run again at the final
shrink-migration gate.

## Review follow-up: transactionality and transfer type preservation

- `DevicesServiceImpl.recordDeviceInventory` is now annotated with
  `@Transactional`, so inserting the inventory event and updating the device's
  inventory state share one transaction.
- `DevicesServiceTransactionTest` now requires transaction metadata and two
  transaction starts when invoking transfer and inventory through the Spring
  proxy; the former assertion that inventory had no transaction was removed.
- `V20261010_02__migrate_core_data.sql` now maps the retired
  `device_transfer_record.transfer_type` column to `transferType` in transfer
  event `after_state`, matching `DeviceEventMapper.xml`.
- `CoreSchemaMigrationMySqlIntegrationTest` now supplies a `relocation`
  transfer type and asserts that the migrated transfer event preserves it in
  `after_state.transferType`.

Verification on 2026-10-11:

```text
mvn.cmd -pl legacy-app -Dtest=DevicesServiceTransactionTest test
BUILD FAILURE before the selected test executed
```

The concurrent package-renaming work blocks `legacy-app` test compilation with
unrelated unresolved imports in
`ClassTimetableQueryCompatibilityTest` (`teachingschedule.web`),
`LabQueryCompatibilityTest` (`labresource.web`), and
`MySqlReservationConcurrencyIntegrationTest` (`reservation.infrastructure`,
including `ReservationPersistenceMapper`, `ReservationPersistencePort`,
`ManagedLaboratoryIdsPort`, `LaboratorySummaryPort`, `ReservationRecordScope`,
`ReservationRecord`, `NewReservationRecord`, `ReservationOverlapCheck`, and
`ReservationReviewUpdate`). The focused device and migration integration tests
therefore did not execute through Maven.

Static checks completed:

```text
mvn.cmd -pl legacy-app dependency:build-classpath -Dmdep.outputFile=target/task4-test-classpath.txt -Dmdep.includeScope=test
BUILD SUCCESS

javac -encoding UTF-8 --release 17 ... DevicesServiceImpl.java
javac -encoding UTF-8 --release 17 ... DevicesServiceTransactionTest.java CoreSchemaMigrationMySqlIntegrationTest.java
SUCCESS

git diff --check
SUCCESS
```
