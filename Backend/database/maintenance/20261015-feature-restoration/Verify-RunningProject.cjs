'use strict';
const fs = require('node:fs');
const path = require('node:path');
const backend = path.resolve(__dirname, '../../..');
const base = 'http://127.0.0.1:8080';
async function main() {
  const credentials = fs.readFileSync(path.join(backend, 'database/recovery/.local-admin-credentials.txt'), 'utf8');
  const username = credentials.match(/^系统登录用户名\s*[:：=]\s*(.+)$/m)?.[1]?.trim();
  const password = credentials.match(/^系统登录密码\s*[:：=]\s*(.+)$/m)?.[1]?.trim();
  if (!username || !password) throw new Error('Local administrator credentials missing');
  async function request(endpoint, token, body) {
    const response = await fetch(base + endpoint, {
      method: body ? 'POST' : 'GET',
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) },
      ...(body ? { body: JSON.stringify(body) } : {}),
      signal: AbortSignal.timeout(15000)
    });
    if (response.status !== 200) throw new Error(endpoint + ' HTTP ' + response.status);
    const result = await response.json();
    if (result.code !== 200) throw new Error(endpoint + ' business response failed');
    return result.data;
  }
  const login = await request('/auth/login', null, { username, password });
  const endpoints = ['/consumables', '/consumables/stock-records', '/iot/devices', '/iot/hardware',
    '/iot/command-logs', '/device-status/summary', '/device-status/events', '/system-settings',
    '/admin/activation-codes', '/activation-codes/lab-bindings', '/environment-templates',
    '/course-environment-requests', '/lab-software', '/schedule-adjustments',
    '/ai-assistant/user-config', '/ai-assistant/conversations'];
  for (const endpoint of endpoints) await request(endpoint, login.token);
  const frontend = await fetch('http://127.0.0.1:5173/', { signal: AbortSignal.timeout(5000) });
  if (!frontend.ok) throw new Error('Frontend HTTP ' + frontend.status);
  const report = { success: true, checkedAt: new Date().toISOString(), frontendStatus: frontend.status,
    authenticatedFeatureReads: endpoints.length, backend: base };
  fs.writeFileSync(path.join(__dirname, 'running-project-verification.json'), JSON.stringify(report, null, 2));
  console.log('PASS running frontend, administrator login and all 16 restored feature API reads');
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
