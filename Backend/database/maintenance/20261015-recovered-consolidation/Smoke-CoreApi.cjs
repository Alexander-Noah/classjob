#!/usr/bin/env node
'use strict';

/**
 * Node.js >= 18. Start the application separately against the intended database.
 * PowerShell usage (credentials must already be supplied securely in the environment):
 *   $env:BASE_URL = 'http://127.0.0.1:8080'
 *   node Backend/database/maintenance/20261015-recovered-consolidation/Smoke-CoreApi.cjs
 * Required: BASE_URL, ADMIN_USERNAME, ADMIN_PASSWORD. Optional: HTTP_TIMEOUT_MS (default 15000).
 * BASE_URL includes any reverse-proxy/context prefix, e.g. http://localhost:8080/api.
 * This script performs only login POST and core GET requests; login can update login
 * throttling/security state. It never creates, changes or deletes business records.
 * It does not prove write workflows, audit writes, database counts, or SQL correctness
 * when a service silently substitutes defaults without exposing loadWarnings/errors.
 * Test writes separately ONLY against an explicitly identified random-prefix rehearsal
 * clone: create/approve reservation, record usage, inventory/transfer, repair/acceptance,
 * publish/read notice, and verify audit/FK records before discarding that clone.
 */

const ENDPOINTS = Object.freeze([
  ['/auth/profile', 'object'],
  ['/users', 'array'],
  ['/labs', 'array'],
  ['/devices', 'array'],
  ['/devices/page?pageNum=1&pageSize=5', 'object'],
  ['/devices/stats', 'object'],
  ['/device-inventory-records', 'array'],
  ['/device-transfer-records', 'array'],
  ['/reservations', 'array'],
  ['/usage-records', 'array'],
  ['/repairs', 'array'],
  ['/maintenance-records', 'array'],
  ['/notices', 'array'],
  ['/class-timetables?limit=5', 'array'],
  ['/class-timetables/summary', 'object'],
  ['/class-timetables/semesters', 'array'],
  ['/class-timetables/crawler-config', 'object'],
  ['/class-timetables/crawler-tasks', 'array'],
  ['/dashboard/overview', 'object'],
  ['/business-loop/overview', 'object'],
]);

function populated(value) {
  if (value == null || value === false || value === 0 || value === '') return false;
  if (Array.isArray(value)) return value.length > 0;
  if (typeof value === 'object') return Object.keys(value).length > 0;
  return true;
}

function assertNoLoadFailures(value, path = 'response', allowTaskHistory = false) {
  if (value == null || typeof value !== 'object') return;
  for (const [key, child] of Object.entries(value)) {
    const location = `${path}.${key}`;
    const historicalTaskError = allowTaskHistory && key === 'error'
      && /^response\.data\.\d+$/.test(path)
      && value.id != null && typeof value.status === 'string';
    if (!historicalTaskError && /^(loadWarnings|warnings|loadErrors|errors|loadError|error)$/i.test(key) && populated(child)) {
      // Only report the field name, never an upstream message or payload containing secrets.
      throw new Error(`non-empty failure/warning field: ${location}`);
    }
    if (/^(loadFailed|partialFailure|degraded)$/i.test(key) && child === true) {
      throw new Error(`failure indicator: ${location}`);
    }
    assertNoLoadFailures(child, location, allowTaskHistory);
  }
}

async function run(env = process.env, fetchImpl = globalThis.fetch, log = console.log) {
  for (const name of ['BASE_URL', 'ADMIN_USERNAME', 'ADMIN_PASSWORD']) {
    if (!env[name]) throw new Error(`missing environment variable: ${name}`);
  }
  const base = new URL(env.BASE_URL);
  if (!['http:', 'https:'].includes(base.protocol) || base.username || base.password || base.search || base.hash) {
    throw new Error('BASE_URL must be an HTTP(S) URL without credentials, query or fragment');
  }
  const timeout = Number(env.HTTP_TIMEOUT_MS || 15000);
  if (!Number.isInteger(timeout) || timeout < 1 || timeout > 120000) {
    throw new Error('HTTP_TIMEOUT_MS must be an integer between 1 and 120000');
  }
  const prefix = base.href.replace(/\/$/, '');
  async function request(path, token, body) {
    let response;
    try {
      response = await fetchImpl(prefix + path, {
        method: body ? 'POST' : 'GET',
        redirect: 'error',
        headers: { Accept: 'application/json', ...(body ? { 'Content-Type': 'application/json' } : {}),
          ...(token ? { Authorization: `Bearer ${token}` } : {}) },
        ...(body ? { body: JSON.stringify(body) } : {}),
        signal: AbortSignal.timeout(timeout),
      });
    } catch {
      throw new Error('network, redirect or timeout failure');
    }
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    let result;
    try { result = await response.json(); } catch { throw new Error('response is not JSON'); }
    if (!result || result.code !== 200) throw new Error('application result.code is not 200');
    assertNoLoadFailures(result, 'response', path === '/class-timetables/crawler-tasks');
    return result.data;
  }
  const login = await request('/auth/login', null, { username: env.ADMIN_USERNAME, password: env.ADMIN_PASSWORD });
  if (!login || typeof login.token !== 'string' || !login.token || login.role !== 'systemAdmin') {
    throw new Error('login must return a token and systemAdmin role');
  }
  log('PASS POST /auth/login (systemAdmin)');
  let failures = 0;
  for (const [path, shape] of ENDPOINTS) {
    try {
      const data = await request(path, login.token);
      if (shape === 'array' ? !Array.isArray(data) : data === null || typeof data !== 'object' || Array.isArray(data)) {
        throw new Error(`expected ${shape} data`);
      }
      log(`PASS GET ${path}`);
    } catch (error) {
      failures++;
      log(`FAIL GET ${path}: ${error.message}`);
    }
  }
  log(`Core API smoke: ${ENDPOINTS.length - failures}/${ENDPOINTS.length} GET checks passed`);
  if (failures) throw new Error(`${failures} core API check(s) failed`);
}

module.exports = { run, assertNoLoadFailures, ENDPOINTS };
if (require.main === module) {
  run().catch(error => {
    console.error(`Core API smoke failed: ${error.message}`);
    process.exitCode = 1;
  });
}
