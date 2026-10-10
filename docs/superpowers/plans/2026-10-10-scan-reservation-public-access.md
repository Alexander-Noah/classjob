# Scan Reservation Public Access Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent anonymous QR reservation visits from being redirected to login while preserving teaching-week enrichment for authenticated users.

**Architecture:** Keep `/scan/reserve` and all existing public reservation APIs unchanged. Gate the protected teaching-calendar request behind the existing `isAuthenticated()` session check, then always load the public laboratory profile independently.

**Tech Stack:** Vue 3, Vue Router, Axios, Node.js built-in test runner, Vite.

## Global Constraints

- Do not add a new public backend endpoint.
- Do not change the QR-code URL format.
- Do not change reservation approval, conflict validation, or status transitions.
- Preserve the existing global 401 handling for protected requests.
- Preserve unrelated working-tree changes.

---

### Task 1: Gate Teaching Calendar Loading on Authentication

**Files:**
- Modify: `web/src/views/reservations/ScanReservation.vue:5-9,542-551`
- Test: `web/tests/scan-reservation-public-access.test.mjs`

**Interfaces:**
- Consumes: `isAuthenticated(): boolean` from `web/src/api/services/auth.js`; `loadTeachingCalendar(): Promise<TeachingCalendar>`; `loadLabProfile(): Promise<void>`.
- Produces: anonymous mount behavior that skips `loadTeachingCalendar()` and always awaits `loadLabProfile()`.

- [ ] **Step 1: Write the failing source-boundary regression test**

```js
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'

const scanReservationUrl = new URL('../src/views/reservations/ScanReservation.vue', import.meta.url)
const routerUrl = new URL('../src/router/index.js', import.meta.url)

test('anonymous scan reservation visits skip the protected teaching calendar request', () => {
  const source = readFileSync(scanReservationUrl, 'utf8')

  assert.match(source, /import\s*\{[^}]*isAuthenticated[^}]*\}\s*from\s*['"]\.\.\/\.\.\/api\/services\/auth['"]/s)
  assert.match(source, /if\s*\(isAuthenticated\(\)\)\s*\{\s*teachingCalendar\.value\s*=\s*await loadTeachingCalendar\(\)\s*\}/s)
  assert.match(source, /await loadLabProfile\(\)/)
  assert.doesNotMatch(source, /catch\s*\([^)]*\)\s*\{[^}]*return[^}]*\}\s*await loadLabProfile\(\)/s)
})

test('scan reservation route remains public', () => {
  const source = readFileSync(routerUrl, 'utf8')
  const route = source.match(/\{\s*path:\s*['"]\/scan\/reserve['"][\s\S]*?\n\s*\},/)?.[0] || ''

  assert.match(route, /public:\s*true/)
})
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `node --test tests/scan-reservation-public-access.test.mjs` from `web`.

Expected: the anonymous-access test fails because `ScanReservation.vue` does not import or call `isAuthenticated()`.

- [ ] **Step 3: Implement the minimal authentication gate**

Change the auth import to:

```js
import { getStoredUser, isAuthenticated } from '../../api/services/auth'
```

Change the mount hook to:

```js
onMounted(async () => {
  if (isAuthenticated()) {
    try {
      teachingCalendar.value = await loadTeachingCalendar()
    } catch (error) {
      loadError.value = error?.message || '教学日历加载失败'
    }
  }

  await loadLabProfile()
})
```

- [ ] **Step 4: Run the focused test and verify GREEN**

Run: `node --test tests/scan-reservation-public-access.test.mjs` from `web`.

Expected: 2 tests pass, 0 fail.

- [ ] **Step 5: Run regression tests and production build**

Run from `web`:

```powershell
node --test tests/*.test.mjs
npm run build
```

Expected: all Node tests pass and Vite exits with code 0.

- [ ] **Step 6: Inspect the scoped diff and commit**

Run from the repository root:

```powershell
git diff --check -- web/src/views/reservations/ScanReservation.vue web/tests/scan-reservation-public-access.test.mjs
git diff -- web/src/views/reservations/ScanReservation.vue web/tests/scan-reservation-public-access.test.mjs
git add -- web/src/views/reservations/ScanReservation.vue web/tests/scan-reservation-public-access.test.mjs docs/superpowers/plans/2026-10-10-scan-reservation-public-access.md
git commit -m "fix(reservations): keep scan entry public"
```

Expected: no whitespace errors; only the authenticated calendar gate, regression test, and implementation plan are committed.
