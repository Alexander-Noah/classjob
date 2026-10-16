'use strict';
// Creates a new target only. The existing recovered database is never changed.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const cp = require('node:child_process');
const { run: smoke } = require('./Smoke-CoreApi.cjs');
const backend = path.resolve(__dirname, '../../..');
const source = 'smart_lab_recovered_20261015';
const target = 'smart_lab_basic';
const mysql = process.env.RECOVERY_MYSQL_EXE || 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe';
const dump = process.env.RECOVERY_MYSQLDUMP_EXE || 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysqldump.exe';
const jar = path.join(backend, 'target/Backend-0.0.1-SNAPSHOT.jar');
const core = ['sys_user','role','user_role','lab','device','device_event','class_timetable','lab_reservation',
  'timetable_integration_config','timetable_import_job','repair_order','usage_record','notice','notice_recipient','audit_event'];
const identity = ['sys_user', 'role', 'user_role'];
const setupTables = [...identity, 'timetable_integration_config', 'timetable_import_job'];
const common = ['--host=127.0.0.1','--port=3306','--user=root','--default-character-set=utf8mb4'];
const hash = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
let archive = path.join(__dirname, '.local-archives', new Date().toISOString().replace(/[:.]/g, '-') + '-' + crypto.randomUUID());
const reportPath = path.join(__dirname, 'deployment-report.json');
let report = { source, target, startedAt: new Date().toISOString(), success: false, archive, checks: [] };
function native(exe, args, options = {}) {
  const result = cp.spawnSync(exe, args, { encoding: 'utf8', windowsHide: true, maxBuffer: 20 * 1024 * 1024, ...options });
  if (result.error || result.status !== 0) throw new Error('Native command failed: ' + path.basename(exe) + ' (details retained in local log)');
  return result.stdout || '';
}
function query(sql, database) {
  return native(mysql, [...common, '--batch','--skip-column-names','--raw', ...(database ? ['--database=' + database] : [])], { input: sql }).trim();
}
function importSql(file, database) {
  const input = fs.openSync(file, 'r');
  try { native(mysql, [...common, '--database=' + database], { stdio: [input, 'pipe', 'pipe'] }); }
  finally { fs.closeSync(input); }
}
function counts(database, tables) {
  return Object.fromEntries(tables.map(table => [table, Number(query('select count(*) from `' + table + '`;', database))]));
}
function check(condition, message) { if (!condition) throw new Error(message); }
function save() { fs.writeFileSync(reportPath, JSON.stringify(report, null, 2)); }
function checkTableSet() {
  const actual = query('show tables;', target).split(/\r?\n/).sort();
  check(JSON.stringify(actual) === JSON.stringify([...core, 'flyway_schema_history'].sort()), 'Unexpected final table set');
  check(query("select count(*) from information_schema.columns where table_schema=database() and table_name='device' and column_name='lab_name';", target) === '0', 'Redundant device column remains');
}
function credentials() {
  const text = fs.readFileSync(path.join(backend, 'database/recovery/.local-admin-credentials.txt'), 'utf8');
  const username = text.match(/^系统登录用户名\s*[:：=]\s*(.+)$/m)?.[1]?.trim();
  const password = text.match(/^系统登录密码\s*[:：=]\s*(.+)$/m)?.[1]?.trim();
  check(username && password, 'Local administrator credentials were not found');
  return { username, password };
}
async function startAndCheck(pass, admin) {
  const port = 18088;
  const logPath = path.join(archive, 'backend-start-' + pass + '.log');
  const log = fs.openSync(logPath, 'w');
  const child = cp.spawn('java', ['-jar', jar, '--server.port=' + port], {
    cwd: backend, windowsHide: true, stdio: ['ignore', log, log],
    env: { ...process.env, SMART_LAB_DB_URL: 'jdbc:mysql://127.0.0.1:3306/' + target + '?useSSL=false&allowPublicKeyRetrieval=true',
      SMART_LAB_DB_USERNAME: 'root', SMART_LAB_DB_PASSWORD: process.env.MYSQL_PWD, SMART_LAB_FLYWAY_ENABLED: 'false' },
  });
  fs.closeSync(log);
  child.on('error', () => {});
  try {
    let ready = false;
    for (let attempt = 0; attempt < 60; attempt++) {
      if (child.exitCode != null) throw new Error('Backend exited before becoming ready; inspect local startup log');
      try {
        const response = await fetch('http://127.0.0.1:' + port + '/auth/profile', { signal: AbortSignal.timeout(1000) });
        if ([200, 401, 403].includes(response.status)) { ready = true; break; }
      } catch { }
      await new Promise(resolve => setTimeout(resolve, 1000));
    }
    check(ready, 'Backend readiness timed out; inspect local startup log');
    await smoke({ BASE_URL: 'http://127.0.0.1:' + port, ADMIN_USERNAME: admin.username, ADMIN_PASSWORD: admin.password }, globalThis.fetch, line => console.log(line));
    checkTableSet();
    report.checks.push('startup-' + pass + ': core HTTP checks passed; table set unchanged');
  } finally {
    if (child.exitCode == null && child.pid) {
      child.kill();
      await new Promise(resolve => { child.once('exit', resolve); setTimeout(resolve, 5000); });
    }
  }
}
async function main() {
  check(process.env.MYSQL_PWD, 'MYSQL_PWD is required in the process environment');
  check(fs.existsSync(jar), 'Build the backend Jar first');
  check(query("select count(*) from information_schema.schemata where schema_name='" + target + "';") === '0', 'Target database already exists; no database will be overwritten');
  check(query("select count(*) from information_schema.schemata where schema_name='" + source + "';") === '1', 'Recovered source database is missing');
  check(query("select count(*) from information_schema.processlist where db='" + source + "' and command<>'Sleep' and id<>connection_id();") === '0', 'Source has running statements; stop its writers first');
  check(query("select count(*) from information_schema.innodb_trx t join information_schema.processlist p on p.id=t.trx_mysql_thread_id where p.db='" + source + "';") === '0', 'Source has open transactions; finish them before replacement');
  const originalTables = query('show tables;', source).split(/\r?\n/);
  report.sourceTableCount = originalTables.length;
  report.beforeCoreCounts = counts(source, core);
  check(core.filter(table => !setupTables.includes(table)).every(table => report.beforeCoreCounts[table] === 0), 'Source has business data; empty-database replacement is not applicable');
  const admin = credentials();
  fs.mkdirSync(archive, { recursive: true });
  const full = path.join(archive, 'recovered-full.sql');
  const seed = path.join(archive, 'identity-seed.sql');
  native(dump, [...common, '--single-transaction','--routines','--triggers','--hex-blob','--no-tablespaces','--set-gtid-purged=OFF','--result-file=' + full, source]);
  native(dump, [...common, '--single-transaction','--no-create-info','--complete-insert','--skip-triggers','--no-tablespaces','--set-gtid-purged=OFF','--result-file=' + seed, source, ...core]);
  report.backupSha256 = hash(full);
  report.identitySeedSha256 = hash(seed);
  report.bootstrapSha256 = hash(path.join(backend, 'src/main/resources/db/core-bootstrap/V20261015_01__create_core_schema.sql'));
  fs.writeFileSync(path.join(archive, 'manifest.json'), JSON.stringify(report, null, 2));
  console.log('Existing recovered database preserved; backup created. Creating the fresh core database.');
  query('create database `' + target + '` character set utf8mb4 collate utf8mb4_general_ci;');
  const migrationLog = path.join(archive, 'bootstrap.log');
  const migration = cp.spawnSync('java', ['-Dloader.main=org.example.backend.database.FreshCoreDatabaseInitializer', '-cp', jar, 'org.springframework.boot.loader.launch.PropertiesLauncher'], {
    cwd: backend, windowsHide: true, encoding: 'utf8',
    env: { ...process.env, SMART_LAB_DB_URL: 'jdbc:mysql://127.0.0.1:3306/' + target + '?useSSL=false&allowPublicKeyRetrieval=true', SMART_LAB_DB_USERNAME: 'root', SMART_LAB_DB_PASSWORD: process.env.MYSQL_PWD, SMART_LAB_FRESH_CORE_ENABLED: 'true' },
  });
  fs.writeFileSync(migrationLog, (migration.stdout || '') + (migration.stderr || ''));
  check(!migration.error && migration.status === 0, 'Fresh initialization failed; inspect local bootstrap.log. Source is untouched.');
  importSql(seed, target);
  checkTableSet();
  report.afterSeedCounts = counts(target, core);
  check(JSON.stringify(report.beforeCoreCounts) === JSON.stringify(report.afterSeedCounts), 'Seed row counts differ');
  report.checks.push('fresh schema, 16 tables, identity counts preserved');
  const selects = JSON.parse(fs.readFileSync(path.join(backend, 'database/recovery/mapper-select-checks.json'), 'utf8'));
  for (const [name, sql] of selects) {
    try { query('explain ' + sql + ';', target); } catch { throw new Error('SELECT verification failed: ' + name); }
  }
  report.selectsVerified = selects.length;
  console.log('Fresh schema and ' + selects.length + ' SELECT statements verified. Checking application startup twice.');
  await startAndCheck(1, admin);
  await startAndCheck(2, admin);
  check(query('show tables;', source).split(/\r?\n/).length === originalTables.length, 'Source table set unexpectedly changed');
  check(JSON.stringify(counts(source, core)) === JSON.stringify(report.beforeCoreCounts), 'Source row counts unexpectedly changed');
  report.finalCoreCounts = counts(target, core);
  report.flyway = query('select version from flyway_schema_history where success=1 order by installed_rank;', target);
  report.success = true;
  report.completedAt = new Date().toISOString();
  save();
  console.log('SUCCESS: smart_lab_basic is ready with 15 core tables plus Flyway history; original database retained.');
}
async function verifyExisting() {
  const previous = JSON.parse(fs.readFileSync(reportPath, 'utf8'));
  check(previous.source === source && previous.target === target && previous.checks.includes('fresh schema, 16 tables, identity counts preserved'), 'No matching initialized replacement report');
  const prefix = path.resolve(__dirname, '.local-archives') + path.sep;
  check(path.resolve(previous.archive).startsWith(prefix), 'Invalid backup archive path');
  archive = previous.archive;
  report = { ...previous, success: false };
  delete report.error;
  check(hash(path.join(archive, 'recovered-full.sql')) === report.backupSha256, 'Backup hash mismatch');
  check(hash(path.join(archive, 'identity-seed.sql')) === report.identitySeedSha256, 'Seed hash mismatch');
  checkTableSet();
  check(JSON.stringify(counts(source, core)) === JSON.stringify(report.beforeCoreCounts), 'Source changed since replacement');
  await startAndCheck(1, credentials());
  await startAndCheck(2, credentials());
  report.finalCoreCounts = counts(target, core);
  report.flyway = query('select version from flyway_schema_history where success=1 order by installed_rank;', target);
  report.success = true;
  report.completedAt = new Date().toISOString();
  save();
  console.log('SUCCESS: new core database passed two complete application startups.');
}
const operation = process.argv.length === 2 ? main : process.argv.length === 3 && process.argv[2] === '--verify-existing' ? verifyExisting : async () => { throw new Error('Unsupported arguments'); };
operation().catch(error => {
  report.error = error.message;
  report.completedAt = new Date().toISOString();
  save();
  console.error(error.message);
  process.exitCode = 1;
});
