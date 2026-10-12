# Optional Maintenance Device Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make laboratory selection mandatory and device entry optional, supporting a linked asset, unlinked manual text, or no device for room-wide maintenance.

**Architecture:** Add a nullable `repair_order.device_text` column alongside the existing nullable `device_id`. The maintenance API accepts explicit `labId`, `deviceId`, and `deviceText` values, validates only selected asset IDs, and returns both IDs plus a unified display value. The Vue form loads laboratories first, then laboratory-scoped devices, and makes the user choose between asset selection and manual entry.

**Tech Stack:** Java 17, Spring Boot 4, MyBatis XML, Flyway/MySQL, JUnit 5, Vue 3, Element Plus, Node test runner.

## Global Constraints

- Laboratory is required and must be selected from the laboratory API.
- Device is optional: linked asset, unlinked manual text, or blank.
- A linked device must belong to the selected laboratory.
- Manual device text must not create or resolve a device association.
- Existing records with `device_id` must continue to display their linked device.
- Preserve unrelated dirty-worktree changes.

---

### Task 1: Persist Unlinked Device Text

**Files:**
- Create: `Backend/src/main/resources/db/migration/V20261012_01__add_maintenance_device_text.sql`
- Create: `Backend/src/test/java/org/example/backend/maintenance/MaintenanceRecordDeviceTextMigrationTest.java`
- Modify: `Backend/src/main/resources/mapper/MaintenanceRecordMapper.xml`
- Modify: `Backend/src/test/java/org/example/backend/maintenance/MaintenanceRecordContractTest.java`

**Interfaces:**
- Consumes: existing `repair_order.device_id` foreign key and `device.device_name`.
- Produces: nullable `repair_order.device_text`; maintenance reads expose `labId`, `deviceId`, `deviceText`, and display field `device`.

- [ ] **Step 1: Write failing migration and mapper contract tests**

Add a test that reads the new migration and asserts it contains `alter table repair_order`, `add column device_text varchar(255) null`, and no update that rewrites historical rows. Extend the mapper XML assertions to require:

```java
assertTrue(mapperXml.contains("r.lab_id as labId"));
assertTrue(mapperXml.contains("r.device_id as deviceId"));
assertTrue(mapperXml.contains("r.device_text as deviceText"));
assertTrue(mapperXml.contains("coalesce(nullif(r.device_text, ''), d.device_name, '') as device"));
assertTrue(mapperXml.contains("#{deviceText}"));
```

- [ ] **Step 2: Run the focused tests and verify RED**

Run:

```powershell
mvn.cmd -f Backend/pom.xml -Dtest=MaintenanceRecordDeviceTextMigrationTest,MaintenanceRecordContractTest test
```

Expected: FAIL because the migration does not exist and the mapper does not read or write `device_text`.

- [ ] **Step 3: Add the migration and mapper persistence**

Create the migration:

```sql
alter table repair_order
  add column device_text varchar(255) null after device_id;
```

Update `MaintenanceRecordColumns` to select raw IDs and text, and calculate the display value:

```xml
r.lab_id as labId,
r.device_id as deviceId,
r.device_text as deviceText,
coalesce(nullif(r.device_text, ''), d.device_name, '') as device,
```

Add `device_text` to inserts and set both device fields during updates:

```xml
#{deviceId},
#{deviceText},
```

```xml
device_id = #{deviceId},
device_text = #{deviceText},
```

- [ ] **Step 4: Run the focused tests and verify GREEN**

Run the command from Step 2. Expected: both tests PASS.

- [ ] **Step 5: Commit Task 1**

```powershell
git add -- Backend/src/main/resources/db/migration/V20261012_01__add_maintenance_device_text.sql Backend/src/main/resources/mapper/MaintenanceRecordMapper.xml Backend/src/test/java/org/example/backend/maintenance/MaintenanceRecordDeviceTextMigrationTest.java Backend/src/test/java/org/example/backend/maintenance/MaintenanceRecordContractTest.java
git commit -m "feat: persist manual maintenance device text"
```

### Task 2: Support Three Device Modes in the Backend

**Files:**
- Modify: `Backend/src/main/java/org/example/backend/entity/MaintenanceRecord.java`
- Modify: `Backend/src/main/java/org/example/backend/mapper/MaintenanceRecordMapper.java`
- Modify: `Backend/src/main/java/org/example/backend/service/impl/MaintenanceRecordServiceImpl.java`
- Modify: `Backend/src/main/resources/mapper/MaintenanceRecordMapper.xml`
- Modify: `Backend/src/test/java/org/example/backend/maintenance/MaintenanceRecordContractTest.java`

**Interfaces:**
- Consumes: request fields `labId: String`, optional `deviceId: String`, optional `deviceText: String`.
- Produces: stored `lab_id`, mutually exclusive `device_id`/`device_text`, plus compatible `lab` and `device` display fields.

- [ ] **Step 1: Write failing service tests for the three modes**

Extend the recording mapper and add tests that assert:

```java
@Test
void deviceMayBeBlankForRoomWideMaintenance() {
    RecordingMapper mapper = new RecordingMapper();
    MaintenanceRecord record = validRecord();
    record.setDeviceId(null);
    record.setDeviceText(" ");
    new MaintenanceRecordServiceImpl(mapper).createMaintenanceRecord(record);
    assertEquals(11L, mapper.insertedLabId);
    assertEquals(null, mapper.insertedDeviceId);
    assertEquals(null, mapper.insertedDeviceText);
    assertFalse(mapper.deviceLookupCalled);
}

@Test
void selectedDeviceIsValidatedAndLinked() {
    RecordingMapper mapper = new RecordingMapper();
    MaintenanceRecord record = validRecord();
    record.setDeviceId("12");
    record.setDeviceText(null);
    new MaintenanceRecordServiceImpl(mapper).createMaintenanceRecord(record);
    assertEquals(12L, mapper.insertedDeviceId);
    assertEquals(null, mapper.insertedDeviceText);
    assertEquals(11L, mapper.requestedDeviceLabId);
}

@Test
void manualDeviceTextIsStoredWithoutLookupOrLink() {
    RecordingMapper mapper = new RecordingMapper();
    MaintenanceRecord record = validRecord();
    record.setDeviceId(null);
    record.setDeviceText("全机房网络设备");
    new MaintenanceRecordServiceImpl(mapper).createMaintenanceRecord(record);
    assertEquals(null, mapper.insertedDeviceId);
    assertEquals("全机房网络设备", mapper.insertedDeviceText);
    assertFalse(mapper.deviceLookupCalled);
}
```

Add explicit rejection assertions:

```java
mapper.labId = null;
IllegalArgumentException missingLab = assertThrows(
        IllegalArgumentException.class,
        () -> new MaintenanceRecordServiceImpl(mapper).createMaintenanceRecord(validRecord())
);
assertTrue(missingLab.getMessage().contains("lab not found"));

mapper.labId = 11L;
mapper.deviceId = null;
MaintenanceRecord selected = validRecord();
selected.setDeviceId("404");
IllegalArgumentException missingDevice = assertThrows(
        IllegalArgumentException.class,
        () -> new MaintenanceRecordServiceImpl(mapper).createMaintenanceRecord(selected)
);
assertTrue(missingDevice.getMessage().contains("device not found"));
```

- [ ] **Step 2: Run the service test and verify RED**

Run:

```powershell
mvn.cmd -f Backend/pom.xml -Dtest=MaintenanceRecordContractTest test
```

Expected: compilation/test FAIL because `labId`, `deviceId`, and `deviceText` do not yet exist and device is still mandatory.

- [ ] **Step 3: Implement explicit references and mutually exclusive persistence**

Add fields to `MaintenanceRecord`:

```java
private String labId;
private String deviceId;
private String deviceText;
```

Change mapper write signatures to accept `deviceText` in addition to resolved IDs. In `validateAndResolve`:

```java
String labReference = isBlank(record.getLabId()) ? record.getLab() : record.getLabId();
requireValue("labId", labReference);
Long labId = maintenanceRecordMapper.findLabId(labReference);
if (labId == null) throw new IllegalArgumentException("lab not found: " + labReference);

String manualText = isBlank(record.getDeviceText()) ? null : record.getDeviceText().trim();
Long deviceId = null;
if (!isBlank(record.getDeviceId())) {
    deviceId = maintenanceRecordMapper.findDeviceId(record.getDeviceId(), labId);
    if (deviceId == null) throw new IllegalArgumentException("device not found: " + record.getDeviceId());
    manualText = null;
}
return new ResolvedReferences(labId, deviceId, manualText);
```

Remove `requireValue("device", ...)`, pass all three resolved values to insert/update, and extend `ResolvedReferences` with `String deviceText`.

- [ ] **Step 4: Run backend tests and verify GREEN**

Run:

```powershell
mvn.cmd -f Backend/pom.xml -Dtest=MaintenanceRecordContractTest,MaintenanceRecordCompatibilityTest test
```

Expected: PASS with all three device modes and existing API compatibility covered.

- [ ] **Step 5: Commit Task 2**

```powershell
git add -- Backend/src/main/java/org/example/backend/entity/MaintenanceRecord.java Backend/src/main/java/org/example/backend/mapper/MaintenanceRecordMapper.java Backend/src/main/java/org/example/backend/service/impl/MaintenanceRecordServiceImpl.java Backend/src/main/resources/mapper/MaintenanceRecordMapper.xml Backend/src/test/java/org/example/backend/maintenance/MaintenanceRecordContractTest.java
git commit -m "feat: support optional maintenance device modes"
```

### Task 3: Add Laboratory and Device Selection to the Form

**Files:**
- Create: `web/src/views/maintenance-records/maintenanceRecordFormModel.js`
- Modify: `web/src/views/maintenance-records/components/MaintenanceRecordForm.vue`
- Modify: `web/src/modules/maintenance-records/maintenanceRecordModel.js`
- Modify: `web/tests/maintenance-record-pages.test.mjs`
- Modify: `web/tests/maintenance-record-service.test.mjs`

**Interfaces:**
- Consumes: `listLabs()` and `getLabDevices(labId)` from `web/src/api/services/lab.js`.
- Produces: payload `{ labId, deviceId, deviceText, maintenanceType, maintenanceDate, maintainer, content, result, remark }`.

- [ ] **Step 1: Write failing frontend model and source-contract tests**

Test exported helpers with real values:

```js
import { createMaintenanceRecordPayload, normalizeMaintenanceLab, normalizeMaintenanceDevice } from '../src/views/maintenance-records/maintenanceRecordFormModel.js'

test('maintenance form builds linked, manual, and room-wide payloads', () => {
  const base = { labId: '7', maintenanceType: '维护', maintenanceDate: '2026-10-12', maintainer: '张三', content: '网络维护', result: '已修复', remark: '' }
  assert.deepEqual(createMaintenanceRecordPayload({ ...base, deviceMode: 'asset', deviceId: '9', deviceText: 'ignored' }), { ...base, deviceId: '9', deviceText: null })
  assert.deepEqual(createMaintenanceRecordPayload({ ...base, deviceMode: 'manual', deviceId: '9', deviceText: ' 全机房 ' }), { ...base, deviceId: null, deviceText: '全机房' })
  assert.deepEqual(createMaintenanceRecordPayload({ ...base, deviceMode: 'none', deviceId: '9', deviceText: 'ignored' }), { ...base, deviceId: null, deviceText: null })
})
```

Add source assertions:

```js
assert.match(form, /import \{ getLabDevices, listLabs \}/)
assert.match(form, /<el-select[\s\S]*?v-model="form\.labId"[\s\S]*?filterable/)
assert.match(form, /getLabDevices\(labId\)/)
assert.doesNotMatch(form, /device:\s*\[\{\s*required:\s*true/)
```

Extend `maintenance-record-service.test.mjs` to lock the required blank display behavior:

```js
assert.equal(normalizeMaintenanceRecord({ device: '' }).device, '')
assert.equal(normalizeMaintenanceRecord({ deviceText: '临时交换机' }).device, '临时交换机')
```

- [ ] **Step 2: Run the frontend test and verify RED**

Run:

```powershell
node --test web/tests/maintenance-record-pages.test.mjs
```

Expected: FAIL because the model module and selection UI do not exist.

- [ ] **Step 3: Implement form state and payload helpers**

Implement normalizers that accept backend field variants:

```js
export const normalizeMaintenanceLab = (lab = {}) => ({
  id: String(lab.id ?? lab.labId ?? ''),
  label: [lab.labCode ?? lab.code, lab.labName ?? lab.name].filter(Boolean).join(' ') || String(lab.id ?? ''),
})

export const normalizeMaintenanceDevice = (device = {}) => ({
  id: String(device.id ?? device.deviceId ?? ''),
  label: [device.deviceCode ?? device.code, device.deviceName ?? device.name].filter(Boolean).join(' ') || String(device.id ?? ''),
})
```

Implement `createMaintenanceRecordPayload` so only one of `deviceId` and `deviceText` is sent and required text fields are trimmed.

Change only the device normalization fallback in `maintenanceRecordModel.js`:

```js
device: firstPresent(record, ['device', 'deviceText', 'deviceName', 'device_name'], ''),
```

- [ ] **Step 4: Implement the dependent selects and manual entry UI**

In `MaintenanceRecordForm.vue`:

- Load laboratories on mount with `listLabs()`.
- Render laboratory as a required, filterable `el-select` bound to `form.labId`.
- On laboratory change, clear `deviceId` and `deviceText`, then call `getLabDevices(labId)`.
- Render device mode choices: `台账设备`, `手动填写`, `不填写`.
- For `asset`, show a filterable device `el-select`; for `manual`, show an `el-input`; for `none`, show no device input.
- During edit hydration, infer `asset` from `record.deviceId`, `manual` from `record.deviceText`, otherwise `none`, and load devices for the stored lab.
- Use `createMaintenanceRecordPayload(form)` in `submit`.

- [ ] **Step 5: Run frontend tests and build**

Run:

```powershell
node --test web/tests/maintenance-record-pages.test.mjs web/tests/maintenance-record-service.test.mjs
npm.cmd --prefix web run build
```

Expected: tests PASS and Vite build completes without errors.

- [ ] **Step 6: Commit Task 3**

```powershell
git add -- web/src/views/maintenance-records/maintenanceRecordFormModel.js web/src/views/maintenance-records/components/MaintenanceRecordForm.vue web/src/modules/maintenance-records/maintenanceRecordModel.js web/tests/maintenance-record-pages.test.mjs web/tests/maintenance-record-service.test.mjs
git commit -m "feat: add maintenance lab and device selectors"
```

### Task 4: Full Regression Verification

**Files:**
- Verify only; modify a task-owned file only if a regression is caused by this feature.

**Interfaces:**
- Consumes: Tasks 1-3.
- Produces: verified backend and frontend behavior ready for handoff.

- [ ] **Step 1: Run the complete backend test suite**

```powershell
mvn.cmd -f Backend/pom.xml test
```

Expected: PASS. If a pre-existing unrelated failure remains, record its exact test name and output without weakening its assertion.

- [ ] **Step 2: Run all frontend tests and production build**

```powershell
node --test web/tests/*.test.mjs
npm.cmd --prefix web run build
```

Expected: all tests PASS and the production build succeeds.

- [ ] **Step 3: Check the final diff**

```powershell
git diff --check HEAD~3..HEAD
git status --short
```

Expected: no whitespace errors; unrelated pre-existing worktree changes remain untouched.

