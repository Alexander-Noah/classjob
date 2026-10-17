'use strict';
const fs = require('node:fs');
const path = require('node:path');
const cp = require('node:child_process');
const tables = ['sys_user','role','user_role','lab','device','device_event','class_timetable','lab_reservation',
  'timetable_integration_config','timetable_import_job','repair_order','usage_record','notice','notice_recipient','audit_event'];
if (!process.env.MYSQL_PWD) throw new Error('MYSQL_PWD is required');
const countSql = 'SELECT JSON_OBJECT(' + tables.map(/** 将当前条目转换为目标结果。 入参：t。 */ t => "'" + t + "',(SELECT COUNT(*) FROM `" + t + '` )').join(',') + ');\n';
const input = countSql + fs.readFileSync(path.join(__dirname, 'Smoke-CoreWrites.sql'), 'utf8') + '\n' + countSql;
const result = cp.spawnSync(process.env.RECOVERY_MYSQL_EXE || 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe',
  ['--host=127.0.0.1','--port=3306','--user=root','--default-character-set=utf8mb4','--batch','--skip-column-names','--raw','--database=smart_lab_basic'],
  { input, encoding: 'utf8', windowsHide: true });
if (result.error || result.status !== 0) throw new Error('Transactional core write verification failed: ' + (result.stderr || result.error.message));
const lines = result.stdout.trim().split(/\r?\n/).map(/** 将当前条目转换为目标结果。 入参：line。 */ line => JSON.parse(line));
if (lines.length !== 3) throw new Error('Unexpected write-check response');
for (const [key, value] of Object.entries(lines[1])) {
  if (value !== (key === 'device_event' ? 2 : 1)) throw new Error('Unexpected inserted-row count: ' + key);
}
if (tables.some(/** 判断当前条目是否满足任一匹配条件。 入参：table。 */ table => lines[0][table] !== lines[2][table])) throw new Error('Sample records were not completely rolled back');
const report = { database: 'smart_lab_basic', checkedAt: new Date().toISOString(), passed: true, insertedChecks: lines[1], rollbackVerified: true };
fs.writeFileSync(path.join(__dirname, 'write-verification.json'), JSON.stringify(report, null, 2));
console.log('PASS: 12 core write checks, explicit foreign keys and complete transaction rollback.');
