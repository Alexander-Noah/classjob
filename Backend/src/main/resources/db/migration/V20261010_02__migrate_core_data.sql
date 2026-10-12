insert into device_event
  (device_id, event_type, occurred_at, operator_name, result_status, after_state, remark, created_at)
select device_id, 'inventory', inspected_at, inspector_name, result_status,
       json_object('deviceCode', device_code, 'deviceName', device_name,
                   'normal', is_normal, 'missing', is_missing, 'damaged', is_damaged),
       remark, created_at
from device_inventory_record;

insert into device_event
  (device_id, event_type, occurred_at, operator_name, before_state, after_state, remark, created_at)
select device_id, 'transfer', transfer_at, operator_name,
       json_object('deviceCode', device_code, 'deviceName', device_name,
                   'labId', from_lab_id, 'labName', from_lab_name,
                   'ownerUserId', from_owner_user_id, 'ownerName', from_owner_name,
                   'location', from_location),
       json_object('deviceCode', device_code, 'deviceName', device_name,
                   'transferType', transfer_type,
                   'labId', to_lab_id, 'labName', to_lab_name,
                   'ownerUserId', to_owner_user_id, 'ownerName', to_owner_name,
                   'location', to_location),
       reason, created_at
from device_transfer_record;

insert into timetable_integration_config
  (config_key, schedule_enabled, cron_expression, semester, semester_start_date,
   credential_username_cipher, credential_password_cipher, updated_at)
select legacy_keys.config_key,
       coalesce(c.schedule_enabled, 0), coalesce(c.cron_expression, '0 0 2 * * *'),
       c.semester, str_to_date(c.semester_start_date, '%Y-%m-%d'),
       a.username_cipher, a.password_cipher,
       greatest(coalesce(c.updated_at, '1970-01-01'), coalesce(a.updated_at, '1970-01-01'))
from (
  select config_key from timetable_crawler_config
  union
  select credential_key from timetable_academic_credential
) legacy_keys
left join timetable_crawler_config c on c.config_key = legacy_keys.config_key
left join timetable_academic_credential a on a.credential_key = legacy_keys.config_key;

insert into timetable_import_job
  (id, config_id, trigger_type, status, scope, source, success_account, attempts,
   updated_count, started_at, finished_at, error_message, output, log_path, created_at)
select t.id, c.id, t.trigger_type, t.status, t.scope, t.source, t.success_account,
       coalesce(t.attempts, 0), coalesce(t.updated_count, 0), t.started_at, t.finished_at,
       t.error_message, t.output, t.log_path, t.created_at
from timetable_crawler_task t
left join timetable_integration_config c on c.config_key = 'lab-admin-class-timetable';

insert into audit_event
  (aggregate_type, aggregate_id, action, result_status, actor_name, detail_json, occurred_at, created_at)
select coalesce(json_unquote(json_extract(record_json, '$.category')), 'system'),
       coalesce(
         json_unquote(json_extract(record_json, '$.details.reservationId')),
         json_unquote(json_extract(record_json, '$.details.deviceId')),
         json_unquote(json_extract(record_json, '$.details.repairId')),
         json_unquote(json_extract(record_json, '$.details.labId')),
         json_unquote(json_extract(record_json, '$.details.noticeId'))
       ),
       coalesce(json_unquote(json_extract(record_json, '$.action')), 'unknown'),
       json_unquote(json_extract(record_json, '$.status')),
       json_unquote(json_extract(record_json, '$.operator')),
       json_extract(record_json, '$.details'),
       coalesce(str_to_date(json_unquote(json_extract(record_json, '$.occurredAt')), '%Y-%m-%d %H:%i:%s'), created_at),
       created_at
from module_record
where module_name = 'operation-events';

update usage_record ur
join (
  select real_name, min(id) as user_id
  from sys_user
  group by real_name
  having count(*) = 1
) unique_user on unique_user.real_name = ur.person
set ur.user_id = unique_user.user_id;

update usage_record ur
join (
  select matched.resource, min(matched.lab_id) as lab_id
  from (
    select lab_name as resource, id as lab_id
    from lab
    where lab_name is not null
    union all
    select lab_code as resource, id as lab_id
    from lab
    where lab_code is not null
  ) matched
  group by matched.resource
  having count(distinct matched.lab_id) = 1
) unique_lab on unique_lab.resource = ur.resource
set ur.lab_id = unique_lab.lab_id;
