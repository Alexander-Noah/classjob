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
method. Inventory retains its existing transaction boundary, which is covered
by `DevicesServiceTransactionTest`.

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
