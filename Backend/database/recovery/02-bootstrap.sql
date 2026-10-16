USE smart_lab_recovered_20261015;
START TRANSACTION;
INSERT INTO role(role_code,role_name) VALUES ('systemAdmin','系统管理员'),('labAdmin','实验室管理员'),('teacher','教师'),('academic','教务人员');
INSERT INTO sys_user(username,password,real_name,status,permissions) VALUES ('recovery_admin','pbkdf2$120000$a1PAiO1uLAFU/3N4J3baIQ==$yrJckNYdICgliS7gcwPa+k3bkrehjwa07g85anQHO1A=','恢复管理员',1,'[{"group":"平台治理","items":["用户管理","系统设置","激活管理","平台统计","通知公告"]}]');
INSERT INTO user_role(user_id,role_id) SELECT u.id,r.id FROM sys_user u CROSS JOIN role r WHERE u.username='recovery_admin' AND r.role_code='systemAdmin';
COMMIT;
