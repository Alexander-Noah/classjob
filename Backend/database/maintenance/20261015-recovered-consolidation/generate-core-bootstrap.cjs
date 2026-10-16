'use strict';
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../../..');
const retained = new Set(['sys_user','role','user_role','lab','device','device_event','class_timetable',
  'lab_reservation','timetable_integration_config','timetable_import_job','repair_order','usage_record',
  'notice','notice_recipient','audit_event']);
const sql = fs.readFileSync(path.join(root, 'database/recovery/01-schema.sql'), 'utf8');
const markers = [...sql.matchAll(/^-- ([a-z_]+)\r?$/gm)];
const definitions = [];
for (let i = 0; i < markers.length; i++) {
  const name = markers[i][1];
  if (!retained.has(name)) continue;
  let block = sql.slice(markers[i].index + markers[i][0].length, markers[i + 1]?.index ?? sql.length).trim();
  if (!new RegExp('^create table ' + name + ' \\(', 'i').test(block)) throw new Error('Invalid table definition: ' + name);
  if (name === 'device') block = block.replace(/^\s*lab_name varchar\(100\)[^\n]*\r?\n/m, '');
  if (name === 'notice_recipient') block = block.replace(") comment '通知接收对象表';", ") engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci comment '通知接收对象表';");
  definitions.push(block);
  retained.delete(name);
}
if (retained.size || definitions.length !== 15) throw new Error('Incomplete core schema');
const constraints = `
alter table user_role
  add constraint fk_core_user_role_user foreign key (user_id) references sys_user(id) on delete cascade,
  add constraint fk_core_user_role_role foreign key (role_id) references role(id) on delete cascade;
alter table lab add constraint fk_core_lab_manager foreign key (manager_user_id) references sys_user(id) on delete set null;
alter table device
  add constraint fk_core_device_lab foreign key (lab_id) references lab(id),
  add constraint fk_core_device_owner foreign key (owner_user_id) references sys_user(id) on delete set null;
alter table lab_reservation
  add constraint fk_core_reservation_lab foreign key (lab_id) references lab(id),
  add constraint fk_core_reservation_user foreign key (applicant_user_id) references sys_user(id) on delete set null;
alter table repair_order
  add constraint fk_core_repair_lab foreign key (lab_id) references lab(id) on delete set null,
  add constraint fk_core_repair_device foreign key (device_id) references device(id) on delete set null,
  add constraint fk_core_repair_reporter foreign key (reporter_user_id) references sys_user(id) on delete set null;
alter table notice add constraint fk_core_notice_author foreign key (created_by) references sys_user(id) on delete set null;
alter table notice_recipient
  add constraint fk_core_recipient_notice foreign key (notice_id) references notice(id) on delete cascade,
  add constraint fk_core_recipient_user foreign key (user_id) references sys_user(id) on delete cascade;
`;
const destination = path.join(root, 'src/main/resources/db/core-bootstrap/V20261015_01__create_core_schema.sql');
fs.mkdirSync(path.dirname(destination), { recursive: true });
fs.writeFileSync(destination, '-- Fresh empty core database only; managed by the independent bootstrap Flyway location.\nSET NAMES utf8mb4 COLLATE utf8mb4_general_ci;\n\n' + definitions.join('\n\n') + '\n' + constraints);
console.log('Generated 15-table core bootstrap with explicit foreign keys.');
