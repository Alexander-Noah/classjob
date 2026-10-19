-- Schema-only export for source control. Data rows and credentials excluded.
/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80411 (8.4.11)
 Source Host           : localhost:3306
 Source Schema         : smart_lab_basic

 Target Server Type    : MySQL
 Target Server Version : 80411 (8.4.11)
 File Encoding         : 65001

 Date: 17/10/2026 16:44:11
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_assistant_conversation
-- ----------------------------
DROP TABLE IF EXISTS `ai_assistant_conversation`;
CREATE TABLE `ai_assistant_conversation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int NOT NULL COMMENT '用户ID',
  `conversation_id` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '前端会话ID',
  `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话标题',
  `preview` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '会话预览',
  `updated_at_millis` bigint NOT NULL COMMENT '会话更新时间毫秒时间戳',
  `messages_json` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息列表JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ai_conversation_user_session`(`user_id` ASC, `conversation_id` ASC) USING BTREE,
  INDEX `idx_ai_conversation_user_time`(`user_id` ASC, `updated_at_millis` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 293 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI助手聊天记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_assistant_conversation
-- ----------------------------

-- ----------------------------
-- Table structure for ai_assistant_user_config
-- ----------------------------
DROP TABLE IF EXISTS `ai_assistant_user_config`;
CREATE TABLE `ai_assistant_user_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int NOT NULL COMMENT '用户ID',
  `enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用自定义API',
  `base_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'OpenAI兼容接口地址',
  `api_key` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'API密钥',
  `model` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文本模型',
  `vision_model` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '视觉模型',
  `system_prompt` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '系统提示词',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ai_user_config_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI助手用户配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_assistant_user_config
-- ----------------------------

-- ----------------------------
-- Table structure for approval_countersign
-- ----------------------------
DROP TABLE IF EXISTS `approval_countersign`;
CREATE TABLE `approval_countersign`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `business_type` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务类型，如 reservation、repair、usage-record',
  `business_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务记录ID',
  `business_title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '业务标题或摘要',
  `business_status` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '发起加签时业务状态',
  `assigner_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '发起人ID',
  `assigner_name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '发起人姓名',
  `assignee_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '加签处理人ID',
  `assignee_name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '加签处理人姓名',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '加签原因',
  `status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '待加签' COMMENT '加签状态',
  `result` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '处理结果',
  `result_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '处理意见',
  `handled_at` datetime NULL DEFAULT NULL COMMENT '处理时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_countersign_business`(`business_type` ASC, `business_id` ASC) USING BTREE,
  INDEX `idx_countersign_assignee`(`assignee_name` ASC, `status` ASC) USING BTREE,
  INDEX `idx_countersign_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '通用审批加签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of approval_countersign
-- ----------------------------

-- ----------------------------
-- Table structure for audit_event
-- ----------------------------
DROP TABLE IF EXISTS `audit_event`;
CREATE TABLE `audit_event`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aggregate_type` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `aggregate_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `action` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `result_status` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `actor_user_id` bigint NULL DEFAULT NULL,
  `actor_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `detail_json` json NULL,
  `occurred_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_audit_event_aggregate`(`aggregate_type` ASC, `aggregate_id` ASC, `occurred_at` ASC) USING BTREE,
  INDEX `idx_audit_event_actor_time`(`actor_user_id` ASC, `occurred_at` ASC) USING BTREE,
  INDEX `idx_audit_event_occurred_at`(`occurred_at` ASC) USING BTREE,
  CONSTRAINT `fk_audit_event_actor` FOREIGN KEY (`actor_user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of audit_event
-- ----------------------------

-- ----------------------------
-- Table structure for class_timetable
-- ----------------------------
DROP TABLE IF EXISTS `class_timetable`;
CREATE TABLE `class_timetable`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `semester` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `row_class_name` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `class_name` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `weekday` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `section_code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `section_text` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `start_section` int NULL DEFAULT NULL,
  `end_section` int NULL DEFAULT NULL,
  `course_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `teacher` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `week_raw` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `week_text` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `week_expanded` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `classroom` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_class_timetable_semester_weekday`(`semester` ASC, `weekday` ASC) USING BTREE,
  INDEX `idx_class_timetable_classroom`(`classroom` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 112833 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of class_timetable
-- ----------------------------

-- ----------------------------
-- Table structure for consumable_inventory
-- ----------------------------
DROP TABLE IF EXISTS `consumable_inventory`;
CREATE TABLE `consumable_inventory`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `stock` int NOT NULL DEFAULT 0,
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `warn_threshold` int NOT NULL DEFAULT 10,
  `status` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `tag_type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of consumable_inventory
-- ----------------------------

-- ----------------------------
-- Table structure for consumable_loan
-- ----------------------------
DROP TABLE IF EXISTS `consumable_loan`;
CREATE TABLE `consumable_loan`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `request_key` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `consumable_id` bigint NOT NULL,
  `consumable_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `borrower` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `contact` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `quantity` int NOT NULL,
  `returned_quantity` int NOT NULL DEFAULT 0,
  `purpose` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `due_date` date NOT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `operator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `borrowed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_returned_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_consumable_loan_request`(`request_key` ASC) USING BTREE,
  INDEX `idx_consumable_loan_item`(`consumable_id` ASC) USING BTREE,
  INDEX `idx_consumable_loan_due`(`due_date` ASC) USING BTREE,
  CONSTRAINT `fk_consumable_loan_item` FOREIGN KEY (`consumable_id`) REFERENCES `consumable_inventory` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_consumable_loan_quantities` CHECK ((`quantity` > 0) and (`returned_quantity` >= 0) and (`returned_quantity` <= `quantity`))
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '耗材借用及归还台账' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of consumable_loan
-- ----------------------------

-- ----------------------------
-- Table structure for consumable_stock_record
-- ----------------------------
DROP TABLE IF EXISTS `consumable_stock_record`;
CREATE TABLE `consumable_stock_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `consumable_id` bigint NOT NULL,
  `consumable_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `movement_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `quantity` int NOT NULL,
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `before_stock` int NOT NULL,
  `after_stock` int NOT NULL,
  `operator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `source_target` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `operated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_consumable_stock_record_consumable_id`(`consumable_id` ASC) USING BTREE,
  INDEX `idx_consumable_stock_record_operated_at`(`operated_at` ASC) USING BTREE,
  CONSTRAINT `fk_consumable_stock_record_consumable` FOREIGN KEY (`consumable_id`) REFERENCES `consumable_inventory` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of consumable_stock_record
-- ----------------------------

-- ----------------------------
-- Table structure for course_environment_request
-- ----------------------------
DROP TABLE IF EXISTS `course_environment_request`;
CREATE TABLE `course_environment_request`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `teacher_user_id` bigint NULL DEFAULT NULL,
  `class_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `use_time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `lab_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `software_requirements` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `system_requirements` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `special_requirements` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `process_status` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `assigned_lab_id` bigint NULL DEFAULT NULL,
  `confirm_status` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course_teacher`(`teacher_user_id` ASC) USING BTREE,
  INDEX `idx_course_lab`(`assigned_lab_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course_environment_request
-- ----------------------------

-- ----------------------------
-- Table structure for device
-- ----------------------------
DROP TABLE IF EXISTS `device`;
CREATE TABLE `device`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `device_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `device_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `lab_id` bigint NULL DEFAULT NULL,
  `lab_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所属实验室',
  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `owner_user_id` bigint NULL DEFAULT NULL,
  `status` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '正常',
  `health` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `online` tinyint(1) NULL DEFAULT NULL,
  `specs` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `purchase_date` date NULL DEFAULT NULL,
  `inventory_date` date NULL DEFAULT NULL,
  `warranty_date` date NULL DEFAULT NULL,
  `usage_hours` int NULL DEFAULT 0,
  `maintenance` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '台' COMMENT '单位',
  `standard_requirement` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '执行标准和数量要求',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '备注',
  `source_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '手动录入' COMMENT '数据来源',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_device_code`(`device_code` ASC) USING BTREE,
  INDEX `idx_device_lab`(`lab_id` ASC) USING BTREE,
  INDEX `idx_device_owner`(`owner_user_id` ASC) USING BTREE,
  CONSTRAINT `fk_core_device_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_core_device_owner` FOREIGN KEY (`owner_user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of device
-- ----------------------------

-- ----------------------------
-- Table structure for device_event
-- ----------------------------
DROP TABLE IF EXISTS `device_event`;
CREATE TABLE `device_event`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `device_id` bigint NOT NULL,
  `event_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `occurred_at` datetime NOT NULL,
  `operator_user_id` bigint NULL DEFAULT NULL,
  `operator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `result_status` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `before_state` json NULL,
  `after_state` json NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_device_event_device_time`(`device_id` ASC, `occurred_at` ASC) USING BTREE,
  INDEX `idx_device_event_type_time`(`event_type` ASC, `occurred_at` ASC) USING BTREE,
  INDEX `fk_device_event_operator`(`operator_user_id` ASC) USING BTREE,
  CONSTRAINT `fk_device_event_device` FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_device_event_operator` FOREIGN KEY (`operator_user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `chk_device_event_type` CHECK (`event_type` in (_utf8mb4'inventory',_utf8mb4'transfer'))
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of device_event
-- ----------------------------

-- ----------------------------
-- Table structure for device_runtime_status
-- ----------------------------
DROP TABLE IF EXISTS `device_runtime_status`;
CREATE TABLE `device_runtime_status`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `lab_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '实验室编号快照',
  `lab_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '实验室名称快照',
  `device_id` bigint NULL DEFAULT NULL COMMENT '可选关联资产设备ID',
  `terminal_record_id` bigint NULL DEFAULT NULL COMMENT '实验室终端绑定ID',
  `terminal_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '终端ID',
  `device_code` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '可选资产编号快照',
  `host_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '主机名',
  `ip_address` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `mac_address` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'MAC地址',
  `online_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'offline' COMMENT '在线状态：online在线，offline离线',
  `runtime_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'normal' COMMENT '运行状态：normal正常，abnormal异常，warning预警，unknown未知',
  `health` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'unknown' COMMENT '健康度：good良好，warning预警，abnormal异常，unknown未知',
  `cpu_usage` decimal(5, 2) NULL DEFAULT NULL COMMENT 'CPU使用率百分比',
  `memory_usage` decimal(5, 2) NULL DEFAULT NULL COMMENT '内存使用率百分比',
  `disk_usage` decimal(5, 2) NULL DEFAULT NULL COMMENT '磁盘使用率百分比',
  `login_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '当前登录用户',
  `client_version` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '客户端版本',
  `last_report_time` datetime NULL DEFAULT NULL COMMENT '最近上报时间',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'manual' COMMENT '来源：terminal终端，manual手动，mock模拟，import导入',
  `metric_snapshot` json NULL COMMENT '原始指标快照',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_device_runtime_terminal`(`terminal_id` ASC) USING BTREE,
  INDEX `idx_device_runtime_lab_status`(`lab_id` ASC, `online_status` ASC, `runtime_status` ASC) USING BTREE,
  INDEX `idx_device_runtime_device`(`device_id` ASC) USING BTREE,
  INDEX `idx_device_runtime_last_report`(`last_report_time` ASC) USING BTREE,
  INDEX `fk_device_runtime_terminal`(`terminal_record_id` ASC) USING BTREE,
  CONSTRAINT `fk_device_runtime_device` FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_device_runtime_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_device_runtime_terminal` FOREIGN KEY (`terminal_record_id`) REFERENCES `lab_terminal` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '设备运行状态表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of device_runtime_status
-- ----------------------------

-- ----------------------------
-- Table structure for device_status_event
-- ----------------------------
DROP TABLE IF EXISTS `device_status_event`;
CREATE TABLE `device_status_event`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `runtime_status_id` bigint NULL DEFAULT NULL COMMENT '设备运行状态ID',
  `terminal_record_id` bigint NULL DEFAULT NULL COMMENT '实验室终端绑定ID',
  `terminal_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '终端ID快照',
  `device_id` bigint NULL DEFAULT NULL COMMENT '可选关联资产设备ID',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `event_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '事件类型：heartbeat心跳，offline离线，online在线，abnormal异常，recover恢复，manual手动',
  `event_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'info' COMMENT '事件级别：info信息，warning预警，danger严重',
  `before_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '变更前状态',
  `after_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '变更后状态',
  `title` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '事件标题',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '事件内容',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'terminal' COMMENT '来源：terminal终端，manual手动，mock模拟，system系统',
  `occurred_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_device_status_event_lab_time`(`lab_id` ASC, `occurred_at` ASC) USING BTREE,
  INDEX `idx_device_status_event_terminal`(`terminal_id` ASC) USING BTREE,
  INDEX `idx_device_status_event_type`(`event_type` ASC, `event_level` ASC) USING BTREE,
  INDEX `fk_device_status_event_runtime`(`runtime_status_id` ASC) USING BTREE,
  INDEX `fk_device_status_event_terminal_record`(`terminal_record_id` ASC) USING BTREE,
  INDEX `fk_device_status_event_device`(`device_id` ASC) USING BTREE,
  CONSTRAINT `fk_device_status_event_device` FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_device_status_event_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_device_status_event_runtime` FOREIGN KEY (`runtime_status_id`) REFERENCES `device_runtime_status` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_device_status_event_terminal_record` FOREIGN KEY (`terminal_record_id`) REFERENCES `lab_terminal` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '设备状态事件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of device_status_event
-- ----------------------------

-- ----------------------------
-- Table structure for environment_template
-- ----------------------------
DROP TABLE IF EXISTS `environment_template`;
CREATE TABLE `environment_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `course` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `os` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `software_list` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `labs` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `status` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `tag_type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of environment_template
-- ----------------------------

-- ----------------------------
-- Table structure for flyway_schema_history
-- ----------------------------
DROP TABLE IF EXISTS `flyway_schema_history`;
CREATE TABLE `flyway_schema_history`  (
  `installed_rank` int NOT NULL,
  `version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `script` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `checksum` int NULL DEFAULT NULL,
  `installed_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`) USING BTREE,
  INDEX `flyway_schema_history_s_idx`(`success` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of flyway_schema_history
-- ----------------------------

-- ----------------------------
-- Table structure for iot_command_log
-- ----------------------------
DROP TABLE IF EXISTS `iot_command_log`;
CREATE TABLE `iot_command_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `command_no` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '控制命令业务编号',
  `iot_device_id` bigint NULL DEFAULT NULL COMMENT '物联设备ID',
  `iot_code` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '物联设备编码快照',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `gateway_id` bigint NULL DEFAULT NULL COMMENT '网关配置ID',
  `point_id` bigint NULL DEFAULT NULL COMMENT '点位配置ID',
  `command_key` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '控制命令键',
  `action` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作名称',
  `payload_json` json NULL COMMENT '请求载荷',
  `request_params_json` json NULL COMMENT '网关请求参数',
  `response_result_json` json NULL COMMENT '网关响应结果',
  `result_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'pending' COMMENT '控制结果：mock_success模拟成功，pending待处理，success成功，failed失败',
  `execution_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'pending' COMMENT '执行状态：pending待执行，completed完成，failed失败',
  `error_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '错误信息',
  `response_summary` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '响应摘要',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作人用户ID',
  `operator_name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作人姓名',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'manual' COMMENT '来源：manual手动，schedule计划任务，mock模拟，gateway网关',
  `gateway_mode` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'mock_gateway' COMMENT '网关接入模式：mock_gateway模拟网关，real_gateway_unconfigured真实网关未配置，real_gateway真实网关',
  `executed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '命令执行开始时间',
  `finished_at` datetime NULL DEFAULT NULL COMMENT '命令执行完成时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_iot_command_no`(`command_no` ASC) USING BTREE,
  INDEX `idx_iot_command_lab_time`(`lab_id` ASC, `executed_at` ASC) USING BTREE,
  INDEX `idx_iot_command_device`(`iot_device_id` ASC) USING BTREE,
  INDEX `idx_iot_command_gateway`(`gateway_id` ASC, `point_id` ASC) USING BTREE,
  INDEX `idx_iot_command_result`(`result_status` ASC) USING BTREE,
  CONSTRAINT `fk_iot_command_log_device` FOREIGN KEY (`iot_device_id`) REFERENCES `iot_device` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_iot_command_log_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '物联控制命令日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iot_command_log
-- ----------------------------

-- ----------------------------
-- Table structure for iot_device
-- ----------------------------
DROP TABLE IF EXISTS `iot_device`;
CREATE TABLE `iot_device`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `iot_code` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '物联设备编码',
  `iot_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '物联设备名称',
  `device_type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '设备类型：access_control门禁，air_conditioner空调，light灯光，camera摄像头，temperature_sensor温度传感器，humidity_sensor湿度传感器，smoke_sensor烟雾传感器',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `lab_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '实验室编号快照',
  `lab_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '实验室名称快照',
  `asset_device_id` bigint NULL DEFAULT NULL COMMENT '可选关联资产设备ID',
  `protocol` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'mock' COMMENT '接入协议：mock模拟，http接口，tcp网关，mqtt消息，rtsp视频',
  `base_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '网关基础地址',
  `endpoint` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '状态或控制接口地址',
  `auth_type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '鉴权类型：none无，token令牌，basic基础认证，custom自定义',
  `configured` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已配置硬件网关',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用设备',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'unconfigured' COMMENT '状态：unconfigured未配置，online在线，offline离线，abnormal异常',
  `snapshot_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '摄像头抓拍地址',
  `stream_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '摄像头视频流地址',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'manual' COMMENT '来源：manual手动，yaml配置，import导入，mock模拟',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_iot_device_code`(`iot_code` ASC) USING BTREE,
  INDEX `idx_iot_device_lab_type`(`lab_id` ASC, `device_type` ASC, `enabled` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_iot_device_asset`(`asset_device_id` ASC) USING BTREE,
  CONSTRAINT `fk_iot_device_asset` FOREIGN KEY (`asset_device_id`) REFERENCES `device` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_iot_device_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '物联设备配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iot_device
-- ----------------------------

-- ----------------------------
-- Table structure for iot_device_capability
-- ----------------------------
DROP TABLE IF EXISTS `iot_device_capability`;
CREATE TABLE `iot_device_capability`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `iot_device_id` bigint NOT NULL COMMENT '物联设备ID',
  `capability_key` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '能力键',
  `capability_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '能力名称',
  `capability_type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'control' COMMENT '能力类型：control控制，telemetry遥测，media媒体',
  `command_key` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '可写能力的控制命令键',
  `readable` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可读取',
  `writable` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否可控制',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '指标单位',
  `min_value` decimal(10, 2) NULL DEFAULT NULL COMMENT '最小值',
  `max_value` decimal(10, 2) NULL DEFAULT NULL COMMENT '最大值',
  `options_json` json NULL COMMENT '选项或控制载荷模板',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用能力',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_iot_capability_device_key`(`iot_device_id` ASC, `capability_key` ASC) USING BTREE,
  INDEX `idx_iot_capability_type`(`capability_type` ASC, `enabled` ASC) USING BTREE,
  CONSTRAINT `fk_iot_capability_device` FOREIGN KEY (`iot_device_id`) REFERENCES `iot_device` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '物联设备能力表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iot_device_capability
-- ----------------------------

-- ----------------------------
-- Table structure for iot_gateway_config
-- ----------------------------
DROP TABLE IF EXISTS `iot_gateway_config`;
CREATE TABLE `iot_gateway_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `gateway_code` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '网关编码',
  `gateway_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '网关名称',
  `protocol_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'mock' COMMENT '协议类型：mock模拟，http接口，mqtt消息',
  `gateway_address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '网关地址',
  `auth_type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'none' COMMENT '鉴权方式：none无，token令牌，basic基础认证，custom自定义',
  `auth_config_json` json NULL COMMENT '鉴权配置JSON，不保存明文密钥时可存引用键',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'unconfigured' COMMENT '状态：unconfigured未配置，enabled启用，disabled停用，abnormal异常',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_iot_gateway_code`(`gateway_code` ASC) USING BTREE,
  INDEX `idx_iot_gateway_protocol_status`(`protocol_type` ASC, `status` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '物联网关配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iot_gateway_config
-- ----------------------------

-- ----------------------------
-- Table structure for iot_gateway_point
-- ----------------------------
DROP TABLE IF EXISTS `iot_gateway_point`;
CREATE TABLE `iot_gateway_point`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `iot_device_id` bigint NOT NULL COMMENT '物联设备ID',
  `gateway_id` bigint NULL DEFAULT NULL COMMENT '网关配置ID',
  `command_type` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '命令类型：open开门，lock关门，set设置，off关闭，snapshot抓拍，status状态',
  `point_code` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '点位编码',
  `request_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '请求路径',
  `request_template` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '请求模板',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用点位',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_iot_gateway_point_device_command`(`iot_device_id` ASC, `command_type` ASC) USING BTREE,
  INDEX `idx_iot_gateway_point_gateway`(`gateway_id` ASC, `enabled` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_iot_gateway_point_code`(`point_code` ASC) USING BTREE,
  CONSTRAINT `fk_iot_gateway_point_device` FOREIGN KEY (`iot_device_id`) REFERENCES `iot_device` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_iot_gateway_point_gateway` FOREIGN KEY (`gateway_id`) REFERENCES `iot_gateway_config` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '物联网关点位配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iot_gateway_point
-- ----------------------------

-- ----------------------------
-- Table structure for iot_telemetry_latest
-- ----------------------------
DROP TABLE IF EXISTS `iot_telemetry_latest`;
CREATE TABLE `iot_telemetry_latest`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `iot_device_id` bigint NOT NULL COMMENT '物联设备ID',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `metric_key` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '指标键：temperature温度，humidity湿度，smoke烟雾，motion人体感应',
  `metric_value` decimal(12, 4) NULL DEFAULT NULL COMMENT '数值型指标值',
  `metric_text` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文本型指标值',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '指标单位',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'normal' COMMENT '状态：normal正常，warning预警，abnormal异常，unknown未知',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'gateway' COMMENT '来源：gateway网关，manual手动，mock模拟',
  `raw_payload` json NULL COMMENT '原始遥测载荷',
  `reported_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上报时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_iot_telemetry_device_metric`(`iot_device_id` ASC, `metric_key` ASC) USING BTREE,
  INDEX `idx_iot_telemetry_lab_metric`(`lab_id` ASC, `metric_key` ASC, `status` ASC) USING BTREE,
  INDEX `idx_iot_telemetry_reported_at`(`reported_at` ASC) USING BTREE,
  CONSTRAINT `fk_iot_telemetry_device` FOREIGN KEY (`iot_device_id`) REFERENCES `iot_device` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_iot_telemetry_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '物联环境最新遥测表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iot_telemetry_latest
-- ----------------------------

-- ----------------------------
-- Table structure for lab
-- ----------------------------
DROP TABLE IF EXISTS `lab`;
CREATE TABLE `lab`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lab_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `lab_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `building` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `floor` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `room_no` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `lab_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `capacity` int NULL DEFAULT 0,
  `manager_user_id` bigint NULL DEFAULT NULL,
  `open_status` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '开放',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_lab_code`(`lab_code` ASC) USING BTREE,
  INDEX `idx_lab_manager`(`manager_user_id` ASC) USING BTREE,
  CONSTRAINT `fk_core_lab_manager` FOREIGN KEY (`manager_user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of lab
-- ----------------------------

-- ----------------------------
-- Table structure for lab_activation_code
-- ----------------------------
DROP TABLE IF EXISTS `lab_activation_code`;
CREATE TABLE `lab_activation_code`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `lab_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '实验室编号快照',
  `lab_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '实验室名称快照',
  `code_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '接入码哈希',
  `masked_code` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '脱敏接入码',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'active' COMMENT '状态：active启用，inactive停用，expired过期，revoked撤销',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可用于接入',
  `terminal_quota` int NOT NULL DEFAULT 1 COMMENT '终端接入额度',
  `bound_terminal_count` int NOT NULL DEFAULT 0 COMMENT '已绑定终端数快照',
  `expire_at` datetime NULL DEFAULT NULL COMMENT '过期时间',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'manual' COMMENT '来源：manual手动，import导入，system系统',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_by_name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人姓名快照',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_lab_activation_code_hash`(`code_hash` ASC) USING BTREE,
  INDEX `idx_lab_activation_code_lab_status`(`lab_id` ASC, `status` ASC, `enabled` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_lab_activation_code_expire_at`(`expire_at` ASC) USING BTREE,
  CONSTRAINT `fk_lab_activation_code_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '实验室接入码表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of lab_activation_code
-- ----------------------------

-- ----------------------------
-- Table structure for lab_reservation
-- ----------------------------
DROP TABLE IF EXISTS `lab_reservation`;
CREATE TABLE `lab_reservation`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `applicant_user_id` bigint NULL DEFAULT NULL,
  `applicant_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `department` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `contact` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `lab_id` bigint NOT NULL,
  `reservation_date` date NOT NULL,
  `time_range` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `scene` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `attendee_count` int NULL DEFAULT NULL,
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `status` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `conflict_flag` tinyint(1) NOT NULL DEFAULT 0,
  `reviewer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `review_remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_reservation_lab_date`(`lab_id` ASC, `reservation_date` ASC, `status` ASC) USING BTREE,
  INDEX `idx_reservation_applicant`(`applicant_user_id` ASC) USING BTREE,
  CONSTRAINT `fk_core_reservation_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_core_reservation_user` FOREIGN KEY (`applicant_user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of lab_reservation
-- ----------------------------

-- ----------------------------
-- Table structure for lab_software
-- ----------------------------
DROP TABLE IF EXISTS `lab_software`;
CREATE TABLE `lab_software`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lab_id` bigint NULL DEFAULT NULL,
  `software_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `software_version` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `install_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `license_info` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `install_time` datetime NULL DEFAULT NULL,
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_lab_software_lab`(`lab_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of lab_software
-- ----------------------------

-- ----------------------------
-- Table structure for lab_terminal
-- ----------------------------
DROP TABLE IF EXISTS `lab_terminal`;
CREATE TABLE `lab_terminal`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `terminal_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '返回给客户端的稳定终端ID',
  `terminal_token_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '终端令牌哈希',
  `lab_id` bigint NOT NULL COMMENT '接入码解析出的实验室ID',
  `lab_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '实验室编号快照',
  `lab_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '实验室名称快照',
  `activation_code_id` bigint NULL DEFAULT NULL COMMENT '实验室接入码ID',
  `terminal_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '终端显示名称',
  `terminal_type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'lab_client' COMMENT '终端类型：teacher_client教师端，lab_client实验室客户端，probe_client探针客户端',
  `host_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '主机名',
  `machine_code` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '机器指纹',
  `mac_address` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'MAC地址',
  `ip_address` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `client_version` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '客户端版本',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'active' COMMENT '状态：active启用，inactive停用，unbound已解绑，blocked已阻止',
  `bound_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `first_connected_at` datetime NULL DEFAULT NULL COMMENT '首次接入时间',
  `last_seen_at` datetime NULL DEFAULT NULL COMMENT '最近心跳时间',
  `unbound_at` datetime NULL DEFAULT NULL COMMENT '解绑时间',
  `unbound_by` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '解绑操作人',
  `unbind_reason` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '解绑原因',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'activation_code' COMMENT '来源：activation_code接入码，manual手动，mock模拟',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_lab_terminal_terminal_id`(`terminal_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_lab_terminal_token_hash`(`terminal_token_hash` ASC) USING BTREE,
  INDEX `idx_lab_terminal_lab_status`(`lab_id` ASC, `status` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_lab_terminal_activation_code`(`activation_code_id` ASC) USING BTREE,
  INDEX `idx_lab_terminal_machine`(`machine_code` ASC, `status` ASC) USING BTREE,
  INDEX `idx_lab_terminal_mac`(`mac_address` ASC, `status` ASC) USING BTREE,
  CONSTRAINT `fk_lab_terminal_activation_code` FOREIGN KEY (`activation_code_id`) REFERENCES `lab_activation_code` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_lab_terminal_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '实验室终端绑定表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of lab_terminal
-- ----------------------------

-- ----------------------------
-- Table structure for module_record
-- ----------------------------
DROP TABLE IF EXISTS `module_record`;
CREATE TABLE `module_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `module_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `record_key` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `record_json` json NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_module_name`(`module_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of module_record
-- ----------------------------

-- ----------------------------
-- Table structure for notice
-- ----------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `notice_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `target_role` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `publish_status` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `publish_time` datetime NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `priority` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '普通' COMMENT '通知优先级',
  `source_module` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '业务来源模块',
  `source_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '业务来源ID',
  `business_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '业务提醒类型',
  `withdrawn_at` datetime NULL DEFAULT NULL COMMENT '撤回时间',
  `archived_at` datetime NULL DEFAULT NULL COMMENT '归档时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_core_notice_author`(`created_by` ASC) USING BTREE,
  CONSTRAINT `fk_core_notice_author` FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notice
-- ----------------------------

-- ----------------------------
-- Table structure for notice_recipient
-- ----------------------------
DROP TABLE IF EXISTS `notice_recipient`;
CREATE TABLE `notice_recipient`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notice_id` bigint NOT NULL COMMENT '通知ID',
  `user_id` bigint NOT NULL COMMENT '接收用户ID',
  `username` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '接收账号',
  `real_name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '接收人姓名',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '接收角色',
  `read_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '未读' COMMENT '阅读状态',
  `read_time` datetime NULL DEFAULT NULL COMMENT '阅读时间',
  `archived` tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户侧归档',
  `archived_at` datetime NULL DEFAULT NULL COMMENT '用户侧归档时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户侧删除',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '用户侧删除时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_notice_recipient_user`(`notice_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_notice_recipient_user_status`(`user_id` ASC, `read_status` ASC) USING BTREE,
  INDEX `idx_notice_recipient_notice`(`notice_id` ASC) USING BTREE,
  CONSTRAINT `fk_core_recipient_notice` FOREIGN KEY (`notice_id`) REFERENCES `notice` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_core_recipient_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '通知接收对象表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notice_recipient
-- ----------------------------

-- ----------------------------
-- Table structure for repair_order
-- ----------------------------
DROP TABLE IF EXISTS `repair_order`;
CREATE TABLE `repair_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `reporter_user_id` bigint NULL DEFAULT NULL,
  `reporter_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `contact` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `lab_id` bigint NULL DEFAULT NULL,
  `device_id` bigint NULL DEFAULT NULL,
  `fault_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `priority_level` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `assignee_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `deadline` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `progress` int NULL DEFAULT 0,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `result_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `record_kind` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'repair' COMMENT 'repair报修，lab_maintenance实验室维护',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_repair_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_repair_lab`(`lab_id` ASC) USING BTREE,
  INDEX `idx_repair_device`(`device_id` ASC) USING BTREE,
  INDEX `fk_core_repair_reporter`(`reporter_user_id` ASC) USING BTREE,
  INDEX `idx_repair_record_kind`(`record_kind` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_core_repair_device` FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_core_repair_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_core_repair_reporter` FOREIGN KEY (`reporter_user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of repair_order
-- ----------------------------

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `role_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` int NOT NULL DEFAULT 1,
  `permissions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_code`(`role_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role
-- ----------------------------

-- ----------------------------
-- Table structure for schedule_adjustment
-- ----------------------------
DROP TABLE IF EXISTS `schedule_adjustment`;
CREATE TABLE `schedule_adjustment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `original_time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `target_time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `status` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `tag_type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `flow_step` int NULL DEFAULT 0,
  `reviewer` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `teacher_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of schedule_adjustment
-- ----------------------------

-- ----------------------------
-- Table structure for student_client
-- ----------------------------
DROP TABLE IF EXISTS `student_client`;
CREATE TABLE `student_client`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `student_device_id` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '学生端设备ID',
  `host_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '学生端主机名',
  `ip_address` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '学生端内网IP',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'offline' COMMENT '状态：online在线，offline离线',
  `token` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '客户端授权令牌快照',
  `last_heartbeat_time` datetime NOT NULL COMMENT '最近心跳时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_client_lab_device`(`lab_id` ASC, `student_device_id` ASC) USING BTREE,
  INDEX `idx_student_client_lab_status_time`(`lab_id` ASC, `status` ASC, `last_heartbeat_time` ASC) USING BTREE,
  CONSTRAINT `fk_student_client_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '实验室学生端在线状态表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of student_client
-- ----------------------------

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `real_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `phone` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `department` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` int NOT NULL DEFAULT 1,
  `permissions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `avatar_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像OSS访问地址',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_user_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------

-- ----------------------------
-- Table structure for teacher_host
-- ----------------------------
DROP TABLE IF EXISTS `teacher_host`;
CREATE TABLE `teacher_host`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `teacher_device_id` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '教师端设备ID',
  `host_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '教师端内网IP',
  `port` int NOT NULL DEFAULT 8765 COMMENT '教师端本地WebSocket端口',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'offline' COMMENT '状态：online在线，offline离线',
  `token` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '客户端授权令牌快照',
  `last_heartbeat_time` datetime NOT NULL COMMENT '最近心跳时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_teacher_host_lab_device`(`lab_id` ASC, `teacher_device_id` ASC) USING BTREE,
  INDEX `idx_teacher_host_lab_status_time`(`lab_id` ASC, `status` ASC, `last_heartbeat_time` ASC) USING BTREE,
  CONSTRAINT `fk_teacher_host_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '实验室教师端主机登记表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of teacher_host
-- ----------------------------

-- ----------------------------
-- Table structure for timetable_import_job
-- ----------------------------
DROP TABLE IF EXISTS `timetable_import_job`;
CREATE TABLE `timetable_import_job`  (
  `id` bigint NOT NULL,
  `config_id` bigint NULL DEFAULT NULL,
  `trigger_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `scope` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `source` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `success_account` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `attempts` int NOT NULL DEFAULT 0,
  `updated_count` int NOT NULL DEFAULT 0,
  `started_at` datetime NULL DEFAULT NULL,
  `finished_at` datetime NULL DEFAULT NULL,
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `output` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `log_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_timetable_import_job_status_time`(`status` ASC, `started_at` ASC) USING BTREE,
  INDEX `fk_timetable_import_job_config`(`config_id` ASC) USING BTREE,
  CONSTRAINT `fk_timetable_import_job_config` FOREIGN KEY (`config_id`) REFERENCES `timetable_integration_config` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of timetable_import_job
-- ----------------------------

-- ----------------------------
-- Table structure for timetable_integration_config
-- ----------------------------
DROP TABLE IF EXISTS `timetable_integration_config`;
CREATE TABLE `timetable_integration_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `schedule_enabled` tinyint(1) NOT NULL DEFAULT 0,
  `cron_expression` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `semester` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `semester_start_date` date NULL DEFAULT NULL,
  `credential_username_cipher` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `credential_password_cipher` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_timetable_integration_config_key`(`config_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of timetable_integration_config
-- ----------------------------

-- ----------------------------
-- Table structure for usage_record
-- ----------------------------
DROP TABLE IF EXISTS `usage_record`;
CREATE TABLE `usage_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `person` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `resource` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `scene` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `use_time` datetime NULL DEFAULT NULL,
  `status` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `user_id` bigint NULL DEFAULT NULL,
  `lab_id` bigint NULL DEFAULT NULL,
  `source_type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `source_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_usage_record_user_time`(`user_id` ASC, `use_time` ASC) USING BTREE,
  INDEX `idx_usage_record_lab_time`(`lab_id` ASC, `use_time` ASC) USING BTREE,
  INDEX `idx_usage_record_source`(`source_type` ASC, `source_id` ASC) USING BTREE,
  CONSTRAINT `fk_usage_record_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_usage_record_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of usage_record
-- ----------------------------

-- ----------------------------
-- Table structure for user_role
-- ----------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_role`(`user_id` ASC, `role_id` ASC) USING BTREE,
  INDEX `idx_user_role_role`(`role_id` ASC) USING BTREE,
  CONSTRAINT `fk_core_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_core_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_role
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
