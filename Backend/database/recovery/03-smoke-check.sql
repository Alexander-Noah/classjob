USE smart_lab_recovered_20261015;
-- All sample business rows are rolled back; these are not recovered records.
START TRANSACTION;
INSERT INTO lab(lab_code,lab_name,capacity) VALUES ('RECOVERY-CHECK','恢复验证实验室',20);
SET @lab = LAST_INSERT_ID();
INSERT INTO device(device_code,device_name,lab_id,online,quantity) VALUES ('RECOVERY-CHECK','恢复验证设备',@lab,1,1);
SET @device = LAST_INSERT_ID();
INSERT INTO lab_reservation(applicant_user_id,applicant_name,lab_id,reservation_date,time_range,status)
 SELECT id,'恢复验证',@lab,CURDATE(),'08:00-09:00','待审核' FROM sys_user WHERE username='recovery_admin';
INSERT INTO repair_order(order_no,lab_id,device_id,status) VALUES ('RECOVERY-CHECK',@lab,@device,'待派单');
INSERT INTO usage_record(person,resource,scene,use_time,status,lab_id) VALUES ('恢复验证','恢复验证实验室','验证',NOW(),'正常',@lab);
INSERT INTO consumable_inventory(name,stock) VALUES ('恢复验证耗材',10);
SET @consumable = LAST_INSERT_ID();
INSERT INTO consumable_stock_record(consumable_id,consumable_name,movement_type,quantity,before_stock,after_stock)
 VALUES (@consumable,'恢复验证耗材','入库',1,9,10);
SELECT 'business_write_check' AS check_name,
 (SELECT COUNT(*) FROM lab WHERE id=@lab) AS lab_written,
 (SELECT COUNT(*) FROM device WHERE id=@device AND online=1) AS device_written,
 (SELECT COUNT(*) FROM consumable_stock_record WHERE consumable_id=@consumable) AS stock_written;
ROLLBACK;
SELECT 'administrator_role_check' AS check_name,u.username,r.role_code,u.status
 FROM sys_user u JOIN user_role ur ON ur.user_id=u.id JOIN role r ON r.id=ur.role_id;
SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema=DATABASE();
SELECT (SELECT COUNT(*) FROM lab) AS labs,(SELECT COUNT(*) FROM device) AS devices,
 (SELECT COUNT(*) FROM lab_reservation) AS reservations,(SELECT COUNT(*) FROM repair_order) AS repairs;
