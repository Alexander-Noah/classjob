-- Execute on the newly created smart_lab_basic only. All sample rows roll back.
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
START TRANSACTION;
SET @smoke_user = (SELECT MIN(id) FROM sys_user);
INSERT INTO lab(lab_code,lab_name,capacity,manager_user_id) VALUES ('CORE-WRITE-SMOKE','核心库写入验证',20,@smoke_user);
SET @smoke_lab = LAST_INSERT_ID();
INSERT INTO device(device_code,device_name,lab_id,owner_user_id,quantity,online) VALUES ('CORE-WRITE-SMOKE','核心库验证设备',@smoke_lab,@smoke_user,1,1);
SET @smoke_device = LAST_INSERT_ID();
INSERT INTO device_event(device_id,event_type,occurred_at,operator_user_id,after_state) VALUES
  (@smoke_device,'inventory',NOW(),@smoke_user,JSON_OBJECT('normal',true)),
  (@smoke_device,'transfer',NOW(),@smoke_user,JSON_OBJECT('labId',@smoke_lab));
INSERT INTO lab_reservation(applicant_user_id,applicant_name,lab_id,reservation_date,time_range,status) VALUES (@smoke_user,'核心库验证',@smoke_lab,CURDATE(),'08:00-09:00','已通过');
SET @smoke_reservation = LAST_INSERT_ID();
INSERT INTO repair_order(order_no,reporter_user_id,lab_id,device_id,status) VALUES ('CORE-WRITE-SMOKE',@smoke_user,@smoke_lab,@smoke_device,'已完成');
INSERT INTO usage_record(person,resource,scene,use_time,status,user_id,lab_id,source_type,source_id) VALUES ('核心库验证','核心库写入验证','验证',NOW(),'正常',@smoke_user,@smoke_lab,'reservation',CAST(@smoke_reservation AS CHAR));
INSERT INTO notice(title,content,publish_status,created_by) VALUES ('核心库验证','事务写入验证','草稿',@smoke_user);
SET @smoke_notice = LAST_INSERT_ID();
INSERT INTO notice_recipient(notice_id,user_id) VALUES (@smoke_notice,@smoke_user);
INSERT INTO class_timetable(semester,class_name,weekday,start_section,end_section,course_name,week_expanded,classroom) VALUES ('CORE-SMOKE','验证班','星期一',1,2,'验证课程','1,2','CORE-WRITE-SMOKE');
INSERT INTO timetable_integration_config(config_key,schedule_enabled,cron_expression) VALUES ('core-write-smoke',0,'0 0 2 * * *');
SET @smoke_config = LAST_INSERT_ID();
INSERT INTO timetable_import_job(id,config_id,trigger_type,status) VALUES (9000000000000000,@smoke_config,'manual','success');
INSERT INTO audit_event(aggregate_type,aggregate_id,action,actor_user_id,detail_json,occurred_at) VALUES ('lab',CAST(@smoke_lab AS CHAR),'smoke',@smoke_user,JSON_OBJECT('test',true),NOW());
SELECT JSON_OBJECT(
  'lab',(SELECT COUNT(*) FROM lab WHERE id=@smoke_lab),
  'device',(SELECT COUNT(*) FROM device WHERE id=@smoke_device),
  'device_event',(SELECT COUNT(*) FROM device_event WHERE device_id=@smoke_device),
  'reservation',(SELECT COUNT(*) FROM lab_reservation WHERE id=@smoke_reservation),
  'repair',(SELECT COUNT(*) FROM repair_order WHERE order_no='CORE-WRITE-SMOKE'),
  'usage',(SELECT COUNT(*) FROM usage_record WHERE lab_id=@smoke_lab),
  'notice',(SELECT COUNT(*) FROM notice WHERE id=@smoke_notice),
  'recipient',(SELECT COUNT(*) FROM notice_recipient WHERE notice_id=@smoke_notice),
  'timetable',(SELECT COUNT(*) FROM class_timetable WHERE semester='CORE-SMOKE'),
  'config',(SELECT COUNT(*) FROM timetable_integration_config WHERE id=@smoke_config),
  'job',(SELECT COUNT(*) FROM timetable_import_job WHERE id=9000000000000000),
  'audit',(SELECT COUNT(*) FROM audit_event WHERE aggregate_type='lab' AND aggregate_id=CAST(@smoke_lab AS CHAR))
) AS core_write_check;
ROLLBACK;
