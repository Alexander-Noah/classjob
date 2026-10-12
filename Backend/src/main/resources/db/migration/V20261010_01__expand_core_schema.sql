create table device_event (
  id bigint primary key auto_increment,
  device_id bigint not null,
  event_type varchar(32) not null,
  occurred_at datetime not null,
  operator_user_id bigint null,
  operator_name varchar(100) null,
  result_status varchar(40) null,
  before_state json null,
  after_state json null,
  remark varchar(500) null,
  created_at datetime not null default current_timestamp,
  key idx_device_event_device_time (device_id, occurred_at),
  key idx_device_event_type_time (event_type, occurred_at),
  constraint fk_device_event_device foreign key (device_id) references device(id),
  constraint fk_device_event_operator foreign key (operator_user_id) references sys_user(id) on delete set null,
  constraint chk_device_event_type check (event_type in ('inventory', 'transfer'))
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci;

create table timetable_integration_config (
  id bigint primary key auto_increment,
  config_key varchar(80) not null,
  schedule_enabled tinyint(1) not null default 0,
  cron_expression varchar(120) not null,
  semester varchar(40) null,
  semester_start_date date null,
  credential_username_cipher text null,
  credential_password_cipher text null,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_timetable_integration_config_key (config_key)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci;

create table timetable_import_job (
  id bigint primary key,
  config_id bigint null,
  trigger_type varchar(20) not null,
  status varchar(20) not null,
  scope varchar(50) null,
  source varchar(80) null,
  success_account varchar(120) null,
  attempts int not null default 0,
  updated_count int not null default 0,
  started_at datetime null,
  finished_at datetime null,
  error_message text null,
  output mediumtext null,
  log_path varchar(500) null,
  created_at datetime not null default current_timestamp,
  key idx_timetable_import_job_status_time (status, started_at),
  constraint fk_timetable_import_job_config foreign key (config_id)
    references timetable_integration_config(id) on delete set null
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci;

create table if not exists class_timetable (
  id bigint primary key auto_increment,
  semester varchar(40) not null,
  row_class_name varchar(160) null,
  class_name varchar(160) null,
  weekday varchar(20) null,
  section_code varchar(40) null,
  section_text varchar(80) null,
  start_section int null,
  end_section int null,
  course_name varchar(200) null,
  teacher varchar(100) null,
  week_raw varchar(200) null,
  week_text varchar(200) null,
  week_expanded varchar(500) null,
  classroom varchar(200) null,
  create_time datetime not null default current_timestamp,
  key idx_class_timetable_semester_weekday (semester, weekday),
  key idx_class_timetable_classroom (classroom)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci;

create table audit_event (
  id bigint primary key auto_increment,
  aggregate_type varchar(60) not null,
  aggregate_id varchar(80) null,
  action varchar(60) not null,
  result_status varchar(40) null,
  actor_user_id bigint null,
  actor_name varchar(100) null,
  detail_json json null,
  occurred_at datetime not null,
  created_at datetime not null default current_timestamp,
  key idx_audit_event_aggregate (aggregate_type, aggregate_id, occurred_at),
  key idx_audit_event_actor_time (actor_user_id, occurred_at),
  key idx_audit_event_occurred_at (occurred_at),
  constraint fk_audit_event_actor foreign key (actor_user_id)
    references sys_user(id) on delete set null
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_general_ci;

alter table usage_record
  add column user_id bigint null after id,
  add column lab_id bigint null after user_id,
  add column source_type varchar(40) null after remark,
  add column source_id varchar(80) null after source_type,
  add key idx_usage_record_user_time (user_id, use_time),
  add key idx_usage_record_lab_time (lab_id, use_time),
  add key idx_usage_record_source (source_type, source_id),
  add constraint fk_usage_record_user foreign key (user_id) references sys_user(id) on delete set null,
  add constraint fk_usage_record_lab foreign key (lab_id) references lab(id) on delete set null;
