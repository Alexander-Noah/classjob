// Rebuild a fresh MySQL schema from the local backend. No database connection is made.
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../..');
/**
 * 读取当前操作。
 * 入参：p（p）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const read = p => fs.readFileSync(path.join(root, p), 'utf8');
const tables = new Map();
const timestamps = 'created_at datetime not null default current_timestamp, updated_at datetime not null default current_timestamp on update current_timestamp';
/**
 * 基础的处理逻辑。
 * 入参：name（名称）；columns（columns）；keys（键）。
 */
function base(name, columns, keys = '') {
  tables.set(name, `create table ${name} (\n id bigint primary key auto_increment,\n ${columns},\n ${timestamps}${keys ? ',\n '+keys : ''}\n) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci`);
}
base('sys_user', 'username varchar(100) not null, password varchar(255) not null, real_name varchar(100), phone varchar(40), email varchar(255), department varchar(160), status int not null default 1, permissions text', 'unique key uk_sys_user_username (username)');
base('role', 'role_code varchar(50) not null, role_name varchar(100) not null, status int not null default 1, permissions text, remark text', 'unique key uk_role_code (role_code)');
base('user_role', 'user_id bigint not null, role_id bigint not null', 'unique key uk_user_role (user_id,role_id), key idx_user_role_role (role_id)');
base('lab', 'lab_code varchar(100) not null, lab_name varchar(200) not null, building varchar(100), floor varchar(40), room_no varchar(80), lab_type varchar(100), capacity int default 0, manager_user_id bigint, open_status varchar(40) default \'开放\', remark text', 'unique key uk_lab_code (lab_code), key idx_lab_manager (manager_user_id)');
base('device', 'device_code varchar(100) not null, device_name varchar(200) not null, category varchar(100), lab_id bigint, location varchar(255), owner_user_id bigint, status varchar(40) default \'正常\', health varchar(40), online tinyint(1), specs text, purchase_date date, inventory_date date, warranty_date date, usage_hours int default 0, maintenance text', 'unique key uk_device_code (device_code), key idx_device_lab (lab_id), key idx_device_owner (owner_user_id)');
base('lab_reservation', 'applicant_user_id bigint, applicant_name varchar(100), department varchar(160), contact varchar(100), lab_id bigint not null, reservation_date date not null, time_range varchar(80) not null, scene varchar(100), attendee_count int, reason text, status varchar(40) not null, conflict_flag tinyint(1) not null default 0, reviewer_name varchar(100), review_remark text', 'key idx_reservation_lab_date (lab_id,reservation_date,status), key idx_reservation_applicant (applicant_user_id)');
base('repair_order', 'order_no varchar(100) not null, reporter_user_id bigint, reporter_name varchar(100), contact varchar(100), lab_id bigint, device_id bigint, fault_type varchar(100), priority_level varchar(40), status varchar(40), assignee_name varchar(100), deadline varchar(100), progress int default 0, description text, result_desc text', 'unique key uk_repair_order_no (order_no), key idx_repair_lab (lab_id), key idx_repair_device (device_id)');
base('usage_record', 'person varchar(160), resource varchar(255), scene varchar(100), use_time datetime, status varchar(40), remark text');
base('course_environment_request', 'course_name varchar(200), teacher_user_id bigint, class_name varchar(200), use_time varchar(255), lab_type varchar(100), software_requirements text, system_requirements text, special_requirements text, process_status varchar(40), assigned_lab_id bigint, confirm_status varchar(40)', 'key idx_course_teacher (teacher_user_id), key idx_course_lab (assigned_lab_id)');
base('environment_template', 'name varchar(200), course varchar(200), os varchar(200), software_list text, labs text, status varchar(40), tag_type varchar(40)');
base('consumable_inventory', 'name varchar(100), category varchar(50), stock int not null default 0, unit varchar(20), location varchar(255), warn_threshold int not null default 10, status varchar(40), tag_type varchar(40)');
base('lab_software', 'lab_id bigint, software_name varchar(200), software_version varchar(100), install_path varchar(500), license_info text, install_time datetime, remark text', 'key idx_lab_software_lab (lab_id)');
base('notice', 'title varchar(255), notice_type varchar(50), target_role varchar(100), content longtext, publish_status varchar(40), publish_time datetime, created_by bigint');
base('schedule_adjustment', 'course varchar(200), original_time varchar(255), target_time varchar(255), reason text, status varchar(40), tag_type varchar(40), flow_step int default 0, reviewer varchar(100), teacher_name varchar(100)');
base('module_record', 'module_name varchar(100) not null, record_key varchar(160), record_json json', 'key idx_module_name (module_name)');
const initializer = read('src/main/java/org/example/backend/config/DatabaseSchemaInitializer.java');
const blocks = [...initializer.matchAll(/"""([\s\S]*?)"""/g)].map(/** 将当前条目转换为目标结果。 入参：m。 */ m=>m[1].trim());
const migration = read('src/main/resources/db/migration/V20261010_01__expand_core_schema.sql');
const mapperDir = path.join(root, 'src/main/resources/mapper');
const mapperFiles = fs.readdirSync(mapperDir).filter(/** 判断当前条目是否符合筛选条件。 入参：f。 */ f=>f.endsWith('.xml'));
const mapperTexts = mapperFiles.map(/** 将当前条目转换为目标结果。 入参：f。 */ f=>fs.readFileSync(path.join(mapperDir,f),'utf8'));
const creates = [...blocks, ...migration.split(';'), ...mapperTexts.flatMap(/** 转换并展开当前条目。 入参：s。 */ s=>[...s.matchAll(/create table[\s\S]*?engine=InnoDB default charset=utf8mb4/gi)].map(/** 将当前条目转换为目标结果。 入参：m。 */ m=>m[0]))];
for (let sql of creates) {
  const match = sql.trim().match(/^create table (?:if not exists )?(\w+)/i);
  if (!match) continue;
  sql = sql.trim().replace(/create table if not exists/i,'create table');
  if (!/collate=/i.test(sql)) sql = sql.replace(/default charset=utf8mb4/i,'default charset=utf8mb4 collate=utf8mb4_general_ci');
  tables.set(match[1], sql);
}
/**
 * 判断是否具有Column。
 * 入参：sql（SQL）；col（col）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
function hasColumn(sql, col) { return new RegExp('(?:\\(|,)\\s*`?'+col+'`?\\s+(?:bigint|int|tinyint|varchar|text|longtext|mediumtext|json|datetime|date|decimal|double|float|timestamp|char|boolean)\\b','i').test(sql); }
const alters = [...blocks.filter(/** 判断当前条目是否符合筛选条件。 入参：s。 */ s=>/^alter table/i.test(s)), ...migration.split(';').map(/** 将当前条目转换为目标结果。 入参：s。 */ s=>s.trim()).filter(/** 判断当前条目是否符合筛选条件。 入参：s。 */ s=>/^alter table/i.test(s))];
for (const sql of alters) {
 const name = sql.match(/^alter table (\w+)/i)[1];
 if (!tables.has(name)) throw Error('Unknown ALTER table '+name);
 let ddl = tables.get(name);
 let additions = sql.replace(/^alter table \w+/i,'').trim().replace(/\s+after\s+\w+/gi,'');
 if (/^add column/i.test(additions) && !/,\s*add /i.test(additions)) {
   const col = additions.match(/^add column (\w+)/i)[1];
   if (hasColumn(ddl,col)) continue;
 }
 additions = additions.replace(/\badd column\s+/gi,'').replace(/\badd (key|constraint)\s+/gi,'$1 ');
 const end = ddl.toLowerCase().lastIndexOf(') engine');
 if (end < 0) throw Error('Missing table end '+name);
 ddl = ddl.slice(0,end).trimEnd()+',\n '+additions+'\n'+ddl.slice(end);
 tables.set(name,ddl);
}
// Preserve parent-before-child ordering without disabling foreign key checks.
const ordered = [], pending = new Map(tables);
while(pending.size) {
 let progress = false;
 for(const [name,sql] of pending) {
  const refs = [...sql.matchAll(/references\s+(\w+)\s*\(/gi)].map(/** 将当前条目转换为目标结果。 入参：m。 */ m=>m[1]);
  if(refs.every(/** 判断当前条目是否满足全部匹配条件。 入参：ref。 */ ref=>ordered.some(/** 判断当前条目是否满足任一匹配条件。 入参：t。 */ t=>t[0]===ref)||ref===name)) {ordered.push([name,sql]);pending.delete(name);progress=true;}
 }
 if(!progress) throw Error('Unresolved FK parents: '+[...pending.keys()]);
}
const header = `-- Reconstructed from local backend source, 2026-10-15. MySQL 8.0+.\n-- Fresh database ONLY. CREATE DATABASE deliberately fails if it already exists.\n-- Change BOTH database names below if a different target is needed.\n-- No original business records are included. Keep Flyway disabled for this reconstructed current schema.\nSET NAMES utf8mb4 COLLATE utf8mb4_general_ci;\nCREATE DATABASE smart_lab_recovered_20261015 CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;\nUSE smart_lab_recovered_20261015;\n\n`;
fs.writeFileSync(path.join(__dirname,'01-schema.sql'), header+ordered.map(/** 将当前条目转换为目标结果。 */ ([n,s])=>'-- '+n+'\n'+s+';').join('\n\n')+'\n');
fs.writeFileSync(path.join(__dirname,'schema-manifest.json'), JSON.stringify(Object.fromEntries(ordered),null,2));
console.log('Generated '+tables.size+' tables; no database was modified.');

