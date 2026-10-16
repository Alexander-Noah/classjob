'use strict';
const fs = require('node:fs');
const path = require('node:path');
const cp = require('node:child_process');
if (!process.env.MYSQL_PWD) throw new Error('MYSQL_PWD is required');
function query(input) {
  const result = cp.spawnSync(process.env.RECOVERY_MYSQL_EXE || 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe',
    ['--host=127.0.0.1','--port=3306','--user=root','--batch','--skip-column-names','--raw','--database=smart_lab_basic'],
    { input, encoding: 'utf8', windowsHide: true });
  if (result.error || result.status !== 0) throw new Error('Foreign-key verification query failed');
  return result.stdout.trim();
}
const edges = query("select table_name,column_name,referenced_table_name,referenced_column_name from information_schema.key_column_usage where table_schema=database() and referenced_table_name is not null;")
  .split(/\r?\n/).filter(Boolean).map(line => line.split('\t'));
if (!edges.length) throw new Error('Core foreign keys are missing');
const checks = edges.map(([table, column, parent, parentColumn]) => {
  if ([table,column,parent,parentColumn].some(name => !/^[a-z_]+$/.test(name))) throw new Error('Unexpected SQL identifier');
  const orphans = Number(query('select count(*) from `' + table + '` c left join `' + parent + '` p on p.`' + parentColumn + '`=c.`' + column + '` where c.`' + column + '` is not null and p.`' + parentColumn + '` is null;'));
  return { table, column, parent, orphans };
});
const report = { database: 'smart_lab_basic', checkedAt: new Date().toISOString(), foreignKeys: checks.length, passed: checks.every(check => check.orphans === 0), checks };
fs.writeFileSync(path.join(__dirname, 'foreign-key-verification.json'), JSON.stringify(report, null, 2));
if (!report.passed) throw new Error('Foreign-key orphans detected');
console.log('PASS: ' + checks.length + ' foreign keys checked, zero orphan references.');
