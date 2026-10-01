-- 未使用数据库表清理脚本
-- 审计日期：2026-07-03
-- 审计范围：Backend/src、web/src、web/tests 中的 Mapper、Service、Controller、前端接口与页面引用。
--
-- 当前确认可清理：
-- 1. host_static_asset
--    原因：当前后端 Mapper/Service/Controller 与前端页面均未引用该表；
--         物联设备已统一使用 iot_device、iot_device_capability、iot_command_log、iot_telemetry_latest；
--         设备资产仍使用 device 表。
--
-- 当前明确保留：
-- 1. iot_device_capability：物联设备能力表，属于物联感知与控制模块后续网关能力配置。
-- 2. lab_activation_code、lab_terminal、device_runtime_status、device_status_event：设备接入授权与运行状态核心表。
-- 3. module_record：激活码旧逻辑仍保留兼容写入，暂不删除。

-- 执行前确认表是否存在以及行数。
select
  table_name as '表名',
  table_rows as '估算行数',
  table_comment as '表注释'
from information_schema.tables
where table_schema = database()
  and table_name in ('host_static_asset');

-- 删除前做一次本库备份，便于误删后恢复。
create table if not exists host_static_asset_backup_20260703 as
select *
from host_static_asset;

-- 确认备份行数。
select count(1) as 'host_static_asset备份行数'
from host_static_asset_backup_20260703;

-- 清理当前代码未使用的历史静态资产表。
drop table if exists host_static_asset;
