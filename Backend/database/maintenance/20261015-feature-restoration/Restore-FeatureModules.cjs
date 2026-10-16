'use strict';
const fs=require('node:fs'),path=require('node:path'),cp=require('node:child_process'),crypto=require('node:crypto');
const backend=path.resolve(__dirname,'../../..'),database='smart_lab_basic';
const mysql=process.env.RECOVERY_MYSQL_EXE || 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe';
const dump=process.env.RECOVERY_MYSQLDUMP_EXE || 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysqldump.exe';
const common=['--host=127.0.0.1','--port=3306','--user=root','--default-character-set=utf8mb4'];
const jar=path.join(backend,'target/Backend-0.0.1-SNAPSHOT.jar');
const migration=path.join(backend,'src/main/resources/db/core-bootstrap/V20261015_02__restore_feature_modules.sql');
const core=['sys_user','role','user_role','lab','device','device_event','class_timetable','lab_reservation','timetable_integration_config','timetable_import_job','repair_order','usage_record','notice','notice_recipient','audit_event'];
const archive=path.join(__dirname,'.local-archives',new Date().toISOString().replace(/[:.]/g,'-')+'-'+crypto.randomUUID());
const report={database,startedAt:new Date().toISOString(),success:false,archive};
const hash=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
function check(ok,message){if(!ok)throw new Error(message)}
function query(sql){const r=cp.spawnSync(mysql,[...common,'--batch','--skip-column-names','--raw','--database='+database],{input:sql,encoding:'utf8',windowsHide:true});check(!r.error&&r.status===0,'Database verification query failed');return r.stdout.trim()}
const counts=()=>Object.fromEntries(core.map(t=>[t,Number(query('select count(*) from `'+t+'`;'))]));
function main(){
 check(process.env.MYSQL_PWD,'MYSQL_PWD is required');check(fs.existsSync(jar),'Build the backend first');
 const sql=fs.readFileSync(migration,'utf8');
 check(!/\b(drop|truncate|delete|alter)\s+(table|from)/i.test(sql),'Restoration must be additive');
 const names=[...sql.matchAll(/create table if not exists ([a-z_]+)/gi)].map(m=>m[1]);
 check(names.length===22 && names.every(n=>!core.includes(n)),'Unexpected feature table set');
 report.beforeCoreCounts=counts();
 report.beforeTableCount=Number(query('select count(*) from information_schema.tables where table_schema=database();'));
 fs.mkdirSync(archive,{recursive:true});
 const backup=path.join(archive,'before-feature-restoration.sql');
 const result=cp.spawnSync(dump,[...common,'--single-transaction','--routines','--triggers','--hex-blob','--no-tablespaces','--set-gtid-purged=OFF','--result-file='+backup,database],{encoding:'utf8',windowsHide:true});
 check(!result.error&&result.status===0,'Backup failed; no migration was attempted');
 report.backupSha256=hash(backup);report.migrationSha256=hash(migration);
 fs.writeFileSync(path.join(archive,'manifest.json'),JSON.stringify(report,null,2));
 console.log('Backup complete. Applying additive feature-table migration.');
 const migrated=cp.spawnSync('java',['-Dloader.main=org.example.backend.database.FeatureModuleRestorer','-cp',jar,'org.springframework.boot.loader.launch.PropertiesLauncher'],{
  cwd:backend,encoding:'utf8',windowsHide:true,env:{...process.env,SMART_LAB_DB_URL:'jdbc:mysql://127.0.0.1:3306/'+database+'?useSSL=false&allowPublicKeyRetrieval=true',SMART_LAB_DB_USERNAME:'root',SMART_LAB_DB_PASSWORD:process.env.MYSQL_PWD,SMART_LAB_FEATURE_RESTORE_ENABLED:'true'}});
 fs.writeFileSync(path.join(archive,'migration.log'),(migrated.stdout||'')+(migrated.stderr||''));
 check(!migrated.error&&migrated.status===0,'Migration failed; inspect the local migration log');
 report.afterCoreCounts=counts();
 check(JSON.stringify(report.beforeCoreCounts)===JSON.stringify(report.afterCoreCounts),'Core row counts changed during migration');
 const actual=new Set(query('show tables;').split(/\r?\n/));
 check([...core,...names,'flyway_schema_history'].every(t=>actual.has(t)) && actual.size===38,'Restored table set differs from the expected 38 tables');
 report.restoredTables=names;report.afterTableCount=actual.size;
 report.flywayVersion=query('select version from flyway_schema_history where success=1 order by installed_rank desc limit 1;');
 report.success=true;report.completedAt=new Date().toISOString();
 console.log('SUCCESS: 22 feature tables restored; all core row counts preserved.');
}
try{main()}catch(error){report.error=error.message;process.exitCode=1;console.error(error.message)}
finally{fs.writeFileSync(path.join(__dirname,'restoration-report.json'),JSON.stringify(report,null,2))}
