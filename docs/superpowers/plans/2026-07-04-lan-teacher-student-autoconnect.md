# LAN Teacher Student Autoconnect Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add backend registration/query APIs plus Tauri teacher/student local WebSocket discovery so student clients automatically connect to the active teacher host in the same lab.

**Architecture:** The Spring Boot backend stores teacher host heartbeats and student client status by `labId`; it does not proxy real-time classroom messages. The teacher desktop app starts a local WebSocket server on port `8765`, registers its address every 5 seconds, and keeps an in-memory connected-student map. The student tray client reads local `labId/token`, queries the backend for the current teacher host, connects to `ws://hostIp:port/ws/student`, sends `HELLO`, then heartbeats every 5 seconds with 3-second reconnect.

**Tech Stack:** Spring Boot 4 + MyBatis XML + MySQL, Vue 3/Pinia/Tauri 2, Rust 2021, `tiny_http`, new Rust `tungstenite` and `serde_json` message types.

## Global Constraints

- Preserve existing UDP/HTTP local gateway behavior.
- Do not hardcode teacher IP.
- Enforce `labId` on every backend and WebSocket boundary.
- Students must send `labId` and `token` when connecting.
- Do not implement fake hardware control; only software connection and status sync.
- Keep changes minimal and aligned with existing Controller/Service/Mapper/XML patterns.

---

### Task 1: Backend Teacher Host And Student Client APIs

**Files:**
- Create: `Backend/src/main/java/org/example/backend/entity/TeacherHostEntity.java`
- Create: `Backend/src/main/java/org/example/backend/entity/StudentClientEntity.java`
- Create: `Backend/src/main/java/org/example/backend/mapper/TeacherHostMapper.java`
- Create: `Backend/src/main/java/org/example/backend/mapper/StudentClientMapper.java`
- Create: `Backend/src/main/resources/mapper/TeacherHostMapper.xml`
- Create: `Backend/src/main/resources/mapper/StudentClientMapper.xml`
- Create: `Backend/src/main/java/org/example/backend/service/TeacherHostService.java`
- Create: `Backend/src/main/java/org/example/backend/service/StudentClientService.java`
- Create: `Backend/src/main/java/org/example/backend/service/impl/TeacherHostServiceImpl.java`
- Create: `Backend/src/main/java/org/example/backend/service/impl/StudentClientServiceImpl.java`
- Create: `Backend/src/main/java/org/example/backend/controller/TeacherHostController.java`
- Create: `Backend/src/main/java/org/example/backend/controller/StudentClientController.java`
- Modify: `Backend/src/main/java/org/example/backend/config/DatabaseSchemaInitializer.java`
- Modify: `Backend/src/main/java/org/example/backend/security/SecurityAccessPolicy.java`
- Test: `Backend/src/test/java/org/example/backend/service/TeacherHostServiceImplTest.java`
- Test: `Backend/src/test/java/org/example/backend/service/StudentClientServiceImplTest.java`

**Interfaces:**
- Produces `POST /teacher-hosts/heartbeat`, `GET /teacher-hosts/current?labId=`, `POST /student-clients/online`, `POST /student-clients/offline`, `GET /student-clients?labId=`.
- Services validate positive `labId`, mark teacher hosts offline after 15 seconds, and upsert by `(lab_id, device_id)`.

- [ ] Write failing service tests for `labId` validation and teacher 15-second offline calculation.
- [ ] Implement entities, mappers, XML, services, controllers, schema initializer DDL, and role allow-list entries.
- [ ] Run `mvn -Dtest=TeacherHostServiceImplTest,StudentClientServiceImplTest test`.

### Task 2: Teacher Desktop Local WebSocket Host

**Files:**
- Modify: `TvTa-ui-main/src-tauri/Cargo.toml`
- Modify: `TvTa-ui-main/src-tauri/src/lib.rs`
- Modify: `TvTa-ui-main/src/stores/activation.ts`
- Modify: `TvTa-ui-main/src/stores/monitor.ts`

**Interfaces:**
- Produces Tauri command `get_lan_connection_snapshot`.
- Teacher host server accepts `/ws/student?labId=...&token=...`.
- Teacher heartbeat body: `labId`, `teacherDeviceId`, `hostIp`, `port`, `status`, `lastHeartbeatTime`.

- [ ] Add Rust tests for WebSocket query validation and in-memory student update on `HELLO/HEARTBEAT`.
- [ ] Implement teacher WebSocket server thread, 5-second backend heartbeat, and snapshot command.
- [ ] Persist `labId` from activation verification when available.
- [ ] Run `cargo test --manifest-path src-tauri/Cargo.toml tests::` and `cargo check --manifest-path src-tauri/Cargo.toml`.

### Task 3: Student Tray Backend Lookup And WebSocket Client

**Files:**
- Modify: `zhiyun-student-client/src-tauri/Cargo.toml`
- Modify: `zhiyun-student-client/src-tauri/src/lib.rs`

**Interfaces:**
- Student settings include `labId`, `token`, and `apiBaseUrl`.
- Student queries `GET /teacher-hosts/current?labId=...`.
- Student WebSocket sends JSON messages with `type`, `studentDeviceId`, `hostName`, `ipAddress`, `labId`, and `timestamp`.

- [ ] Add Rust tests for default settings and teacher current response parsing.
- [ ] Implement API lookup, WebSocket connect, `HELLO`, 5-second `HEARTBEAT`, and 3-second reconnect.
- [ ] Update tray/status/log text for “当前实验室教师端未启动”.
- [ ] Run `cargo test --manifest-path src-tauri/Cargo.toml tests::` and `cargo check --manifest-path src-tauri/Cargo.toml`.

### Task 4: Final Verification

**Files:**
- Review all modified files.

- [ ] Run backend targeted tests.
- [ ] Run backend compile/test command that is feasible in this workspace.
- [ ] Run both Rust targeted tests and `cargo check`.
- [ ] Run frontend build if dependency state allows.
- [ ] Summarize implemented endpoints, client behavior, verification commands, and any limitations.
