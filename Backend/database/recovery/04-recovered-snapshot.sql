-- MySQL dump 10.13  Distrib 8.4.11, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: smart_lab_recovered_20261015
-- ------------------------------------------------------
-- Server version	8.4.11

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `ai_assistant_conversation`
--

DROP TABLE IF EXISTS `ai_assistant_conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_assistant_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int NOT NULL COMMENT '用户ID',
  `conversation_id` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT '前端会话ID',
  `title` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话标题',
  `preview` varchar(300) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '会话预览',
  `updated_at_millis` bigint NOT NULL COMMENT '会话更新时间毫秒时间戳',
  `messages_json` mediumtext COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息列表JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_conversation_user_session` (`user_id`,`conversation_id`),
  KEY `idx_ai_conversation_user_time` (`user_id`,`updated_at_millis`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI助手聊天记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_assistant_conversation`
--

LOCK TABLES `ai_assistant_conversation` WRITE;
/*!40000 ALTER TABLE `ai_assistant_conversation` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_assistant_conversation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_assistant_user_config`
--

DROP TABLE IF EXISTS `ai_assistant_user_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_assistant_user_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int NOT NULL COMMENT '用户ID',
  `enabled` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否启用自定义API',
  `base_url` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'OpenAI兼容接口地址',
  `api_key` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'API密钥',
  `model` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT '文本模型',
  `vision_model` varchar(120) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '视觉模型',
  `system_prompt` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '系统提示词',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_user_config_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI助手用户配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_assistant_user_config`
--

LOCK TABLES `ai_assistant_user_config` WRITE;
/*!40000 ALTER TABLE `ai_assistant_user_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `ai_assistant_user_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `approval_countersign`
--

DROP TABLE IF EXISTS `approval_countersign`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_countersign` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `business_type` varchar(60) COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务类型，如 reservation、repair、usage-record',
  `business_id` varchar(80) COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务记录ID',
  `business_title` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务标题或摘要',
  `business_status` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发起加签时业务状态',
  `assigner_id` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发起人ID',
  `assigner_name` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发起人姓名',
  `assignee_id` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '加签处理人ID',
  `assignee_name` varchar(80) COLLATE utf8mb4_general_ci NOT NULL COMMENT '加签处理人姓名',
  `reason` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '加签原因',
  `status` varchar(30) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '待加签' COMMENT '加签状态',
  `result` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '处理结果',
  `result_remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '处理意见',
  `handled_at` datetime DEFAULT NULL COMMENT '处理时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_countersign_business` (`business_type`,`business_id`),
  KEY `idx_countersign_assignee` (`assignee_name`,`status`),
  KEY `idx_countersign_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通用审批加签表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `approval_countersign`
--

LOCK TABLES `approval_countersign` WRITE;
/*!40000 ALTER TABLE `approval_countersign` DISABLE KEYS */;
/*!40000 ALTER TABLE `approval_countersign` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `audit_event`
--

DROP TABLE IF EXISTS `audit_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aggregate_type` varchar(60) COLLATE utf8mb4_general_ci NOT NULL,
  `aggregate_id` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `action` varchar(60) COLLATE utf8mb4_general_ci NOT NULL,
  `result_status` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `actor_user_id` bigint DEFAULT NULL,
  `actor_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `detail_json` json DEFAULT NULL,
  `occurred_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_event_aggregate` (`aggregate_type`,`aggregate_id`,`occurred_at`),
  KEY `idx_audit_event_actor_time` (`actor_user_id`,`occurred_at`),
  KEY `idx_audit_event_occurred_at` (`occurred_at`),
  CONSTRAINT `fk_audit_event_actor` FOREIGN KEY (`actor_user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_event`
--

LOCK TABLES `audit_event` WRITE;
/*!40000 ALTER TABLE `audit_event` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class_timetable`
--

DROP TABLE IF EXISTS `class_timetable`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_timetable` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `semester` varchar(40) COLLATE utf8mb4_general_ci NOT NULL,
  `row_class_name` varchar(160) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `class_name` varchar(160) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `weekday` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `section_code` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `section_text` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `start_section` int DEFAULT NULL,
  `end_section` int DEFAULT NULL,
  `course_name` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `teacher` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `week_raw` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `week_text` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `week_expanded` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `classroom` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_class_timetable_semester_weekday` (`semester`,`weekday`),
  KEY `idx_class_timetable_classroom` (`classroom`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_timetable`
--

LOCK TABLES `class_timetable` WRITE;
/*!40000 ALTER TABLE `class_timetable` DISABLE KEYS */;
/*!40000 ALTER TABLE `class_timetable` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `consumable_inventory`
--

DROP TABLE IF EXISTS `consumable_inventory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `consumable_inventory` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `category` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `stock` int NOT NULL DEFAULT '0',
  `unit` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `location` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `warn_threshold` int NOT NULL DEFAULT '10',
  `status` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `tag_type` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `consumable_inventory`
--

LOCK TABLES `consumable_inventory` WRITE;
/*!40000 ALTER TABLE `consumable_inventory` DISABLE KEYS */;
/*!40000 ALTER TABLE `consumable_inventory` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `consumable_stock_record`
--

DROP TABLE IF EXISTS `consumable_stock_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `consumable_stock_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `consumable_id` bigint NOT NULL,
  `consumable_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `category` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `movement_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `quantity` int NOT NULL,
  `unit` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `before_stock` int NOT NULL,
  `after_stock` int NOT NULL,
  `operator_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `source_target` varchar(160) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `reason` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `remark` text COLLATE utf8mb4_general_ci,
  `operated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_consumable_stock_record_consumable_id` (`consumable_id`),
  KEY `idx_consumable_stock_record_operated_at` (`operated_at`),
  CONSTRAINT `fk_consumable_stock_record_consumable` FOREIGN KEY (`consumable_id`) REFERENCES `consumable_inventory` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `consumable_stock_record`
--

LOCK TABLES `consumable_stock_record` WRITE;
/*!40000 ALTER TABLE `consumable_stock_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `consumable_stock_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `course_environment_request`
--

DROP TABLE IF EXISTS `course_environment_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_environment_request` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_name` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `teacher_user_id` bigint DEFAULT NULL,
  `class_name` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `use_time` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `lab_type` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `software_requirements` text COLLATE utf8mb4_general_ci,
  `system_requirements` text COLLATE utf8mb4_general_ci,
  `special_requirements` text COLLATE utf8mb4_general_ci,
  `process_status` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `assigned_lab_id` bigint DEFAULT NULL,
  `confirm_status` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_course_teacher` (`teacher_user_id`),
  KEY `idx_course_lab` (`assigned_lab_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `course_environment_request`
--

LOCK TABLES `course_environment_request` WRITE;
/*!40000 ALTER TABLE `course_environment_request` DISABLE KEYS */;
/*!40000 ALTER TABLE `course_environment_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device`
--

DROP TABLE IF EXISTS `device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `device_code` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `device_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL,
  `category` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `lab_id` bigint DEFAULT NULL,
  `location` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `status` varchar(40) COLLATE utf8mb4_general_ci DEFAULT '正常',
  `health` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `online` tinyint(1) DEFAULT NULL,
  `specs` text COLLATE utf8mb4_general_ci,
  `purchase_date` date DEFAULT NULL,
  `inventory_date` date DEFAULT NULL,
  `warranty_date` date DEFAULT NULL,
  `usage_hours` int DEFAULT '0',
  `maintenance` text COLLATE utf8mb4_general_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `lab_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属实验室',
  `quantity` int NOT NULL DEFAULT '1' COMMENT '数量',
  `unit` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '台' COMMENT '单位',
  `standard_requirement` text COLLATE utf8mb4_general_ci COMMENT '执行标准和数量要求',
  `remark` text COLLATE utf8mb4_general_ci COMMENT '备注',
  `source_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '手动录入' COMMENT '数据来源',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_code` (`device_code`),
  KEY `idx_device_lab` (`lab_id`),
  KEY `idx_device_owner` (`owner_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device`
--

LOCK TABLES `device` WRITE;
/*!40000 ALTER TABLE `device` DISABLE KEYS */;
/*!40000 ALTER TABLE `device` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_event`
--

DROP TABLE IF EXISTS `device_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `device_id` bigint NOT NULL,
  `event_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `occurred_at` datetime NOT NULL,
  `operator_user_id` bigint DEFAULT NULL,
  `operator_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `result_status` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `before_state` json DEFAULT NULL,
  `after_state` json DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_device_event_device_time` (`device_id`,`occurred_at`),
  KEY `idx_device_event_type_time` (`event_type`,`occurred_at`),
  KEY `fk_device_event_operator` (`operator_user_id`),
  CONSTRAINT `fk_device_event_device` FOREIGN KEY (`device_id`) REFERENCES `device` (`id`),
  CONSTRAINT `fk_device_event_operator` FOREIGN KEY (`operator_user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL,
  CONSTRAINT `chk_device_event_type` CHECK ((`event_type` in (_utf8mb4'inventory',_utf8mb4'transfer')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_event`
--

LOCK TABLES `device_event` WRITE;
/*!40000 ALTER TABLE `device_event` DISABLE KEYS */;
/*!40000 ALTER TABLE `device_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_runtime_status`
--

DROP TABLE IF EXISTS `device_runtime_status`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_runtime_status` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `lab_code` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '实验室编号快照',
  `lab_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '实验室名称快照',
  `device_id` bigint DEFAULT NULL COMMENT '可选关联资产设备ID',
  `terminal_record_id` bigint DEFAULT NULL COMMENT '实验室终端绑定ID',
  `terminal_id` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '终端ID',
  `device_code` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '可选资产编号快照',
  `host_name` varchar(120) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主机名',
  `ip_address` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'IP地址',
  `mac_address` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'MAC地址',
  `online_status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'offline' COMMENT '在线状态：online在线，offline离线',
  `runtime_status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'normal' COMMENT '运行状态：normal正常，abnormal异常，warning预警，unknown未知',
  `health` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'unknown' COMMENT '健康度：good良好，warning预警，abnormal异常，unknown未知',
  `cpu_usage` decimal(5,2) DEFAULT NULL COMMENT 'CPU使用率百分比',
  `memory_usage` decimal(5,2) DEFAULT NULL COMMENT '内存使用率百分比',
  `disk_usage` decimal(5,2) DEFAULT NULL COMMENT '磁盘使用率百分比',
  `login_user` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '当前登录用户',
  `client_version` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '客户端版本',
  `last_report_time` datetime DEFAULT NULL COMMENT '最近上报时间',
  `source_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'manual' COMMENT '来源：terminal终端，manual手动，mock模拟，import导入',
  `metric_snapshot` json DEFAULT NULL COMMENT '原始指标快照',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_runtime_terminal` (`terminal_id`),
  KEY `idx_device_runtime_lab_status` (`lab_id`,`online_status`,`runtime_status`),
  KEY `idx_device_runtime_device` (`device_id`),
  KEY `idx_device_runtime_last_report` (`last_report_time`),
  KEY `fk_device_runtime_terminal` (`terminal_record_id`),
  CONSTRAINT `fk_device_runtime_device` FOREIGN KEY (`device_id`) REFERENCES `device` (`id`),
  CONSTRAINT `fk_device_runtime_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`),
  CONSTRAINT `fk_device_runtime_terminal` FOREIGN KEY (`terminal_record_id`) REFERENCES `lab_terminal` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备运行状态表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_runtime_status`
--

LOCK TABLES `device_runtime_status` WRITE;
/*!40000 ALTER TABLE `device_runtime_status` DISABLE KEYS */;
/*!40000 ALTER TABLE `device_runtime_status` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_status_event`
--

DROP TABLE IF EXISTS `device_status_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_status_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `runtime_status_id` bigint DEFAULT NULL COMMENT '设备运行状态ID',
  `terminal_record_id` bigint DEFAULT NULL COMMENT '实验室终端绑定ID',
  `terminal_id` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '终端ID快照',
  `device_id` bigint DEFAULT NULL COMMENT '可选关联资产设备ID',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `event_type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '事件类型：heartbeat心跳，offline离线，online在线，abnormal异常，recover恢复，manual手动',
  `event_level` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'info' COMMENT '事件级别：info信息，warning预警，danger严重',
  `before_status` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '变更前状态',
  `after_status` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '变更后状态',
  `title` varchar(160) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '事件标题',
  `content` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '事件内容',
  `source_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'terminal' COMMENT '来源：terminal终端，manual手动，mock模拟，system系统',
  `occurred_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_device_status_event_lab_time` (`lab_id`,`occurred_at`),
  KEY `idx_device_status_event_terminal` (`terminal_id`),
  KEY `idx_device_status_event_type` (`event_type`,`event_level`),
  KEY `fk_device_status_event_runtime` (`runtime_status_id`),
  KEY `fk_device_status_event_terminal_record` (`terminal_record_id`),
  KEY `fk_device_status_event_device` (`device_id`),
  CONSTRAINT `fk_device_status_event_device` FOREIGN KEY (`device_id`) REFERENCES `device` (`id`),
  CONSTRAINT `fk_device_status_event_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`),
  CONSTRAINT `fk_device_status_event_runtime` FOREIGN KEY (`runtime_status_id`) REFERENCES `device_runtime_status` (`id`),
  CONSTRAINT `fk_device_status_event_terminal_record` FOREIGN KEY (`terminal_record_id`) REFERENCES `lab_terminal` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备状态事件表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_status_event`
--

LOCK TABLES `device_status_event` WRITE;
/*!40000 ALTER TABLE `device_status_event` DISABLE KEYS */;
/*!40000 ALTER TABLE `device_status_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `environment_template`
--

DROP TABLE IF EXISTS `environment_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `environment_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `course` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `os` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `software_list` text COLLATE utf8mb4_general_ci,
  `labs` text COLLATE utf8mb4_general_ci,
  `status` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `tag_type` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `environment_template`
--

LOCK TABLES `environment_template` WRITE;
/*!40000 ALTER TABLE `environment_template` DISABLE KEYS */;
/*!40000 ALTER TABLE `environment_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `iot_command_log`
--

DROP TABLE IF EXISTS `iot_command_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_command_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `command_no` varchar(80) COLLATE utf8mb4_general_ci NOT NULL COMMENT '控制命令业务编号',
  `iot_device_id` bigint DEFAULT NULL COMMENT '物联设备ID',
  `iot_code` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物联设备编码快照',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `gateway_id` bigint DEFAULT NULL COMMENT '网关配置ID',
  `point_id` bigint DEFAULT NULL COMMENT '点位配置ID',
  `command_key` varchar(80) COLLATE utf8mb4_general_ci NOT NULL COMMENT '控制命令键',
  `action` varchar(80) COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作名称',
  `payload_json` json DEFAULT NULL COMMENT '请求载荷',
  `request_params_json` json DEFAULT NULL COMMENT '网关请求参数',
  `response_result_json` json DEFAULT NULL COMMENT '网关响应结果',
  `result_status` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'pending' COMMENT '控制结果：mock_success模拟成功，pending待处理，success成功，failed失败',
  `execution_status` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'pending' COMMENT '执行状态：pending待执行，completed完成，failed失败',
  `error_message` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '错误信息',
  `response_summary` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '响应摘要',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人用户ID',
  `operator_name` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作人姓名',
  `source_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'manual' COMMENT '来源：manual手动，schedule计划任务，mock模拟，gateway网关',
  `gateway_mode` varchar(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'mock_gateway' COMMENT '网关接入模式：mock_gateway模拟网关，real_gateway_unconfigured真实网关未配置，real_gateway真实网关',
  `executed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '命令执行开始时间',
  `finished_at` datetime DEFAULT NULL COMMENT '命令执行完成时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iot_command_no` (`command_no`),
  KEY `idx_iot_command_lab_time` (`lab_id`,`executed_at`),
  KEY `idx_iot_command_device` (`iot_device_id`),
  KEY `idx_iot_command_gateway` (`gateway_id`,`point_id`),
  KEY `idx_iot_command_result` (`result_status`),
  CONSTRAINT `fk_iot_command_log_device` FOREIGN KEY (`iot_device_id`) REFERENCES `iot_device` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_iot_command_log_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='物联控制命令日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `iot_command_log`
--

LOCK TABLES `iot_command_log` WRITE;
/*!40000 ALTER TABLE `iot_command_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `iot_command_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `iot_device`
--

DROP TABLE IF EXISTS `iot_device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_device` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `iot_code` varchar(80) COLLATE utf8mb4_general_ci NOT NULL COMMENT '物联设备编码',
  `iot_name` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT '物联设备名称',
  `device_type` varchar(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '设备类型：access_control门禁，air_conditioner空调，light灯光，camera摄像头，temperature_sensor温度传感器，humidity_sensor湿度传感器，smoke_sensor烟雾传感器',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `lab_code` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '实验室编号快照',
  `lab_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '实验室名称快照',
  `asset_device_id` bigint DEFAULT NULL COMMENT '可选关联资产设备ID',
  `protocol` varchar(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'mock' COMMENT '接入协议：mock模拟，http接口，tcp网关，mqtt消息，rtsp视频',
  `base_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '网关基础地址',
  `endpoint` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态或控制接口地址',
  `auth_type` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '鉴权类型：none无，token令牌，basic基础认证，custom自定义',
  `configured` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已配置硬件网关',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用设备',
  `status` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'unconfigured' COMMENT '状态：unconfigured未配置，online在线，offline离线，abnormal异常',
  `snapshot_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '摄像头抓拍地址',
  `stream_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '摄像头视频流地址',
  `source_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'manual' COMMENT '来源：manual手动，yaml配置，import导入，mock模拟',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iot_device_code` (`iot_code`),
  KEY `idx_iot_device_lab_type` (`lab_id`,`device_type`,`enabled`,`deleted`),
  KEY `idx_iot_device_asset` (`asset_device_id`),
  CONSTRAINT `fk_iot_device_asset` FOREIGN KEY (`asset_device_id`) REFERENCES `device` (`id`),
  CONSTRAINT `fk_iot_device_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='物联设备配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `iot_device`
--

LOCK TABLES `iot_device` WRITE;
/*!40000 ALTER TABLE `iot_device` DISABLE KEYS */;
/*!40000 ALTER TABLE `iot_device` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `iot_device_capability`
--

DROP TABLE IF EXISTS `iot_device_capability`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_device_capability` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `iot_device_id` bigint NOT NULL COMMENT '物联设备ID',
  `capability_key` varchar(80) COLLATE utf8mb4_general_ci NOT NULL COMMENT '能力键',
  `capability_name` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT '能力名称',
  `capability_type` varchar(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'control' COMMENT '能力类型：control控制，telemetry遥测，media媒体',
  `command_key` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '可写能力的控制命令键',
  `readable` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否可读取',
  `writable` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否可控制',
  `unit` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '指标单位',
  `min_value` decimal(10,2) DEFAULT NULL COMMENT '最小值',
  `max_value` decimal(10,2) DEFAULT NULL COMMENT '最大值',
  `options_json` json DEFAULT NULL COMMENT '选项或控制载荷模板',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用能力',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iot_capability_device_key` (`iot_device_id`,`capability_key`),
  KEY `idx_iot_capability_type` (`capability_type`,`enabled`),
  CONSTRAINT `fk_iot_capability_device` FOREIGN KEY (`iot_device_id`) REFERENCES `iot_device` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='物联设备能力表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `iot_device_capability`
--

LOCK TABLES `iot_device_capability` WRITE;
/*!40000 ALTER TABLE `iot_device_capability` DISABLE KEYS */;
/*!40000 ALTER TABLE `iot_device_capability` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `iot_gateway_config`
--

DROP TABLE IF EXISTS `iot_gateway_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_gateway_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `gateway_code` varchar(80) COLLATE utf8mb4_general_ci NOT NULL COMMENT '网关编码',
  `gateway_name` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT '网关名称',
  `protocol_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'mock' COMMENT '协议类型：mock模拟，http接口，mqtt消息',
  `gateway_address` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '网关地址',
  `auth_type` varchar(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'none' COMMENT '鉴权方式：none无，token令牌，basic基础认证，custom自定义',
  `auth_config_json` json DEFAULT NULL COMMENT '鉴权配置JSON，不保存明文密钥时可存引用键',
  `status` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'unconfigured' COMMENT '状态：unconfigured未配置，enabled启用，disabled停用，abnormal异常',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iot_gateway_code` (`gateway_code`),
  KEY `idx_iot_gateway_protocol_status` (`protocol_type`,`status`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='物联网关配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `iot_gateway_config`
--

LOCK TABLES `iot_gateway_config` WRITE;
/*!40000 ALTER TABLE `iot_gateway_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `iot_gateway_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `iot_gateway_point`
--

DROP TABLE IF EXISTS `iot_gateway_point`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_gateway_point` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `iot_device_id` bigint NOT NULL COMMENT '物联设备ID',
  `gateway_id` bigint DEFAULT NULL COMMENT '网关配置ID',
  `command_type` varchar(80) COLLATE utf8mb4_general_ci NOT NULL COMMENT '命令类型：open开门，lock关门，set设置，off关闭，snapshot抓拍，status状态',
  `point_code` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT '点位编码',
  `request_path` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求路径',
  `request_template` text COLLATE utf8mb4_general_ci COMMENT '请求模板',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用点位',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iot_gateway_point_device_command` (`iot_device_id`,`command_type`),
  KEY `idx_iot_gateway_point_gateway` (`gateway_id`,`enabled`,`deleted`),
  KEY `idx_iot_gateway_point_code` (`point_code`),
  CONSTRAINT `fk_iot_gateway_point_device` FOREIGN KEY (`iot_device_id`) REFERENCES `iot_device` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_iot_gateway_point_gateway` FOREIGN KEY (`gateway_id`) REFERENCES `iot_gateway_config` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='物联网关点位配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `iot_gateway_point`
--

LOCK TABLES `iot_gateway_point` WRITE;
/*!40000 ALTER TABLE `iot_gateway_point` DISABLE KEYS */;
/*!40000 ALTER TABLE `iot_gateway_point` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `iot_telemetry_latest`
--

DROP TABLE IF EXISTS `iot_telemetry_latest`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_telemetry_latest` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `iot_device_id` bigint NOT NULL COMMENT '物联设备ID',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `metric_key` varchar(80) COLLATE utf8mb4_general_ci NOT NULL COMMENT '指标键：temperature温度，humidity湿度，smoke烟雾，motion人体感应',
  `metric_value` decimal(12,4) DEFAULT NULL COMMENT '数值型指标值',
  `metric_text` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文本型指标值',
  `unit` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '指标单位',
  `status` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'normal' COMMENT '状态：normal正常，warning预警，abnormal异常，unknown未知',
  `source_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'gateway' COMMENT '来源：gateway网关，manual手动，mock模拟',
  `raw_payload` json DEFAULT NULL COMMENT '原始遥测载荷',
  `reported_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上报时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iot_telemetry_device_metric` (`iot_device_id`,`metric_key`),
  KEY `idx_iot_telemetry_lab_metric` (`lab_id`,`metric_key`,`status`),
  KEY `idx_iot_telemetry_reported_at` (`reported_at`),
  CONSTRAINT `fk_iot_telemetry_device` FOREIGN KEY (`iot_device_id`) REFERENCES `iot_device` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_iot_telemetry_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='物联环境最新遥测表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `iot_telemetry_latest`
--

LOCK TABLES `iot_telemetry_latest` WRITE;
/*!40000 ALTER TABLE `iot_telemetry_latest` DISABLE KEYS */;
/*!40000 ALTER TABLE `iot_telemetry_latest` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lab`
--

DROP TABLE IF EXISTS `lab`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lab` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lab_code` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `lab_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL,
  `building` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `floor` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `room_no` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `lab_type` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `capacity` int DEFAULT '0',
  `manager_user_id` bigint DEFAULT NULL,
  `open_status` varchar(40) COLLATE utf8mb4_general_ci DEFAULT '开放',
  `remark` text COLLATE utf8mb4_general_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lab_code` (`lab_code`),
  KEY `idx_lab_manager` (`manager_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lab`
--

LOCK TABLES `lab` WRITE;
/*!40000 ALTER TABLE `lab` DISABLE KEYS */;
/*!40000 ALTER TABLE `lab` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lab_activation_code`
--

DROP TABLE IF EXISTS `lab_activation_code`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lab_activation_code` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `lab_code` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '实验室编号快照',
  `lab_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '实验室名称快照',
  `code_hash` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '接入码哈希',
  `masked_code` varchar(80) COLLATE utf8mb4_general_ci NOT NULL COMMENT '脱敏接入码',
  `status` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'active' COMMENT '状态：active启用，inactive停用，expired过期，revoked撤销',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否可用于接入',
  `terminal_quota` int NOT NULL DEFAULT '1' COMMENT '终端接入额度',
  `bound_terminal_count` int NOT NULL DEFAULT '0' COMMENT '已绑定终端数快照',
  `expire_at` datetime DEFAULT NULL COMMENT '过期时间',
  `source_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'manual' COMMENT '来源：manual手动，import导入，system系统',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人用户ID',
  `created_by_name` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人姓名快照',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lab_activation_code_hash` (`code_hash`),
  KEY `idx_lab_activation_code_lab_status` (`lab_id`,`status`,`enabled`,`deleted`),
  KEY `idx_lab_activation_code_expire_at` (`expire_at`),
  CONSTRAINT `fk_lab_activation_code_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='实验室接入码表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lab_activation_code`
--

LOCK TABLES `lab_activation_code` WRITE;
/*!40000 ALTER TABLE `lab_activation_code` DISABLE KEYS */;
/*!40000 ALTER TABLE `lab_activation_code` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lab_reservation`
--

DROP TABLE IF EXISTS `lab_reservation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lab_reservation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `applicant_user_id` bigint DEFAULT NULL,
  `applicant_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `department` varchar(160) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `contact` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `lab_id` bigint NOT NULL,
  `reservation_date` date NOT NULL,
  `time_range` varchar(80) COLLATE utf8mb4_general_ci NOT NULL,
  `scene` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `attendee_count` int DEFAULT NULL,
  `reason` text COLLATE utf8mb4_general_ci,
  `status` varchar(40) COLLATE utf8mb4_general_ci NOT NULL,
  `conflict_flag` tinyint(1) NOT NULL DEFAULT '0',
  `reviewer_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `review_remark` text COLLATE utf8mb4_general_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_reservation_lab_date` (`lab_id`,`reservation_date`,`status`),
  KEY `idx_reservation_applicant` (`applicant_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lab_reservation`
--

LOCK TABLES `lab_reservation` WRITE;
/*!40000 ALTER TABLE `lab_reservation` DISABLE KEYS */;
/*!40000 ALTER TABLE `lab_reservation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lab_software`
--

DROP TABLE IF EXISTS `lab_software`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lab_software` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lab_id` bigint DEFAULT NULL,
  `software_name` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `software_version` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `install_path` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `license_info` text COLLATE utf8mb4_general_ci,
  `install_time` datetime DEFAULT NULL,
  `remark` text COLLATE utf8mb4_general_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_lab_software_lab` (`lab_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lab_software`
--

LOCK TABLES `lab_software` WRITE;
/*!40000 ALTER TABLE `lab_software` DISABLE KEYS */;
/*!40000 ALTER TABLE `lab_software` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lab_terminal`
--

DROP TABLE IF EXISTS `lab_terminal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lab_terminal` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `terminal_id` varchar(80) COLLATE utf8mb4_general_ci NOT NULL COMMENT '返回给客户端的稳定终端ID',
  `terminal_token_hash` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '终端令牌哈希',
  `lab_id` bigint NOT NULL COMMENT '接入码解析出的实验室ID',
  `lab_code` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '实验室编号快照',
  `lab_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '实验室名称快照',
  `activation_code_id` bigint DEFAULT NULL COMMENT '实验室接入码ID',
  `terminal_name` varchar(120) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '终端显示名称',
  `terminal_type` varchar(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'lab_client' COMMENT '终端类型：teacher_client教师端，lab_client实验室客户端，probe_client探针客户端',
  `host_name` varchar(120) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主机名',
  `machine_code` varchar(160) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '机器指纹',
  `mac_address` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'MAC地址',
  `ip_address` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'IP地址',
  `client_version` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '客户端版本',
  `status` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'active' COMMENT '状态：active启用，inactive停用，unbound已解绑，blocked已阻止',
  `bound_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `first_connected_at` datetime DEFAULT NULL COMMENT '首次接入时间',
  `last_seen_at` datetime DEFAULT NULL COMMENT '最近心跳时间',
  `unbound_at` datetime DEFAULT NULL COMMENT '解绑时间',
  `unbound_by` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '解绑操作人',
  `unbind_reason` varchar(300) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '解绑原因',
  `source_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'activation_code' COMMENT '来源：activation_code接入码，manual手动，mock模拟',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lab_terminal_terminal_id` (`terminal_id`),
  UNIQUE KEY `uk_lab_terminal_token_hash` (`terminal_token_hash`),
  KEY `idx_lab_terminal_lab_status` (`lab_id`,`status`,`deleted`),
  KEY `idx_lab_terminal_activation_code` (`activation_code_id`),
  KEY `idx_lab_terminal_machine` (`machine_code`,`status`),
  KEY `idx_lab_terminal_mac` (`mac_address`,`status`),
  CONSTRAINT `fk_lab_terminal_activation_code` FOREIGN KEY (`activation_code_id`) REFERENCES `lab_activation_code` (`id`),
  CONSTRAINT `fk_lab_terminal_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='实验室终端绑定表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lab_terminal`
--

LOCK TABLES `lab_terminal` WRITE;
/*!40000 ALTER TABLE `lab_terminal` DISABLE KEYS */;
/*!40000 ALTER TABLE `lab_terminal` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `module_record`
--

DROP TABLE IF EXISTS `module_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `module_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `module_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `record_key` varchar(160) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `record_json` json DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_module_name` (`module_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `module_record`
--

LOCK TABLES `module_record` WRITE;
/*!40000 ALTER TABLE `module_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `module_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notice`
--

DROP TABLE IF EXISTS `notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `notice_type` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `target_role` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `content` longtext COLLATE utf8mb4_general_ci,
  `publish_status` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `publish_time` datetime DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `priority` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '普通' COMMENT '通知优先级',
  `source_module` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务来源模块',
  `source_id` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务来源ID',
  `business_type` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务提醒类型',
  `withdrawn_at` datetime DEFAULT NULL COMMENT '撤回时间',
  `archived_at` datetime DEFAULT NULL COMMENT '归档时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notice`
--

LOCK TABLES `notice` WRITE;
/*!40000 ALTER TABLE `notice` DISABLE KEYS */;
/*!40000 ALTER TABLE `notice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notice_recipient`
--

DROP TABLE IF EXISTS `notice_recipient`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notice_recipient` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notice_id` bigint NOT NULL COMMENT '通知ID',
  `user_id` bigint NOT NULL COMMENT '接收用户ID',
  `username` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '接收账号',
  `real_name` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '接收人姓名',
  `role_code` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '接收角色',
  `read_status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '未读' COMMENT '阅读状态',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `archived` tinyint(1) NOT NULL DEFAULT '0' COMMENT '用户侧归档',
  `archived_at` datetime DEFAULT NULL COMMENT '用户侧归档时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '用户侧删除',
  `deleted_at` datetime DEFAULT NULL COMMENT '用户侧删除时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_recipient_user` (`notice_id`,`user_id`),
  KEY `idx_notice_recipient_user_status` (`user_id`,`read_status`),
  KEY `idx_notice_recipient_notice` (`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知接收对象表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notice_recipient`
--

LOCK TABLES `notice_recipient` WRITE;
/*!40000 ALTER TABLE `notice_recipient` DISABLE KEYS */;
/*!40000 ALTER TABLE `notice_recipient` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `repair_order`
--

DROP TABLE IF EXISTS `repair_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `repair_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `reporter_user_id` bigint DEFAULT NULL,
  `reporter_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `contact` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `lab_id` bigint DEFAULT NULL,
  `device_id` bigint DEFAULT NULL,
  `fault_type` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `priority_level` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `assignee_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `deadline` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `progress` int DEFAULT '0',
  `description` text COLLATE utf8mb4_general_ci,
  `result_desc` text COLLATE utf8mb4_general_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_repair_order_no` (`order_no`),
  KEY `idx_repair_lab` (`lab_id`),
  KEY `idx_repair_device` (`device_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `repair_order`
--

LOCK TABLES `repair_order` WRITE;
/*!40000 ALTER TABLE `repair_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `repair_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `role_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `status` int NOT NULL DEFAULT '1',
  `permissions` text COLLATE utf8mb4_general_ci,
  `remark` text COLLATE utf8mb4_general_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role`
--

LOCK TABLES `role` WRITE;
/*!40000 ALTER TABLE `role` DISABLE KEYS */;
INSERT INTO `role` VALUES (1,'systemAdmin','系统管理员',1,NULL,NULL,'2026-10-15 11:43:23','2026-10-15 11:43:23'),(2,'labAdmin','实验室管理员',1,NULL,NULL,'2026-10-15 11:43:23','2026-10-15 11:43:23'),(3,'teacher','教师',1,NULL,NULL,'2026-10-15 11:43:23','2026-10-15 11:43:23'),(4,'academic','教务人员',1,NULL,NULL,'2026-10-15 11:43:23','2026-10-15 11:43:23');
/*!40000 ALTER TABLE `role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `schedule_adjustment`
--

DROP TABLE IF EXISTS `schedule_adjustment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedule_adjustment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `original_time` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `target_time` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `reason` text COLLATE utf8mb4_general_ci,
  `status` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `tag_type` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `flow_step` int DEFAULT '0',
  `reviewer` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `teacher_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `schedule_adjustment`
--

LOCK TABLES `schedule_adjustment` WRITE;
/*!40000 ALTER TABLE `schedule_adjustment` DISABLE KEYS */;
/*!40000 ALTER TABLE `schedule_adjustment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_client`
--

DROP TABLE IF EXISTS `student_client`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_client` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `student_device_id` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT '学生端设备ID',
  `host_name` varchar(120) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '学生端主机名',
  `ip_address` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '学生端内网IP',
  `status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'offline' COMMENT '状态：online在线，offline离线',
  `token` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '客户端授权令牌快照',
  `last_heartbeat_time` datetime NOT NULL COMMENT '最近心跳时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_client_lab_device` (`lab_id`,`student_device_id`),
  KEY `idx_student_client_lab_status_time` (`lab_id`,`status`,`last_heartbeat_time`),
  CONSTRAINT `fk_student_client_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='实验室学生端在线状态表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_client`
--

LOCK TABLES `student_client` WRITE;
/*!40000 ALTER TABLE `student_client` DISABLE KEYS */;
/*!40000 ALTER TABLE `student_client` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `real_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `phone` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `department` varchar(160) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` int NOT NULL DEFAULT '1',
  `permissions` text COLLATE utf8mb4_general_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `avatar_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像OSS访问地址',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'recovery_admin','pbkdf2$120000$a1PAiO1uLAFU/3N4J3baIQ==$yrJckNYdICgliS7gcwPa+k3bkrehjwa07g85anQHO1A=','恢复管理员',NULL,NULL,NULL,1,'[{\"group\":\"平台治理\",\"items\":[\"用户管理\",\"系统设置\",\"激活管理\",\"平台统计\",\"通知公告\"]}]','2026-10-15 11:43:23','2026-10-15 11:43:23',NULL);
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teacher_host`
--

DROP TABLE IF EXISTS `teacher_host`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_host` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lab_id` bigint NOT NULL COMMENT '实验室ID',
  `teacher_device_id` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT '教师端设备ID',
  `host_ip` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '教师端内网IP',
  `port` int NOT NULL DEFAULT '8765' COMMENT '教师端本地WebSocket端口',
  `status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'offline' COMMENT '状态：online在线，offline离线',
  `token` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '客户端授权令牌快照',
  `last_heartbeat_time` datetime NOT NULL COMMENT '最近心跳时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_host_lab_device` (`lab_id`,`teacher_device_id`),
  KEY `idx_teacher_host_lab_status_time` (`lab_id`,`status`,`last_heartbeat_time`),
  CONSTRAINT `fk_teacher_host_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='实验室教师端主机登记表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teacher_host`
--

LOCK TABLES `teacher_host` WRITE;
/*!40000 ALTER TABLE `teacher_host` DISABLE KEYS */;
/*!40000 ALTER TABLE `teacher_host` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `timetable_import_job`
--

DROP TABLE IF EXISTS `timetable_import_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `timetable_import_job` (
  `id` bigint NOT NULL,
  `config_id` bigint DEFAULT NULL,
  `trigger_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `scope` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `source` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `success_account` varchar(120) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `attempts` int NOT NULL DEFAULT '0',
  `updated_count` int NOT NULL DEFAULT '0',
  `started_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  `error_message` text COLLATE utf8mb4_general_ci,
  `output` mediumtext COLLATE utf8mb4_general_ci,
  `log_path` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_timetable_import_job_status_time` (`status`,`started_at`),
  KEY `fk_timetable_import_job_config` (`config_id`),
  CONSTRAINT `fk_timetable_import_job_config` FOREIGN KEY (`config_id`) REFERENCES `timetable_integration_config` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `timetable_import_job`
--

LOCK TABLES `timetable_import_job` WRITE;
/*!40000 ALTER TABLE `timetable_import_job` DISABLE KEYS */;
/*!40000 ALTER TABLE `timetable_import_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `timetable_integration_config`
--

DROP TABLE IF EXISTS `timetable_integration_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `timetable_integration_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(80) COLLATE utf8mb4_general_ci NOT NULL,
  `schedule_enabled` tinyint(1) NOT NULL DEFAULT '0',
  `cron_expression` varchar(120) COLLATE utf8mb4_general_ci NOT NULL,
  `semester` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `semester_start_date` date DEFAULT NULL,
  `credential_username_cipher` text COLLATE utf8mb4_general_ci,
  `credential_password_cipher` text COLLATE utf8mb4_general_ci,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_timetable_integration_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `timetable_integration_config`
--

LOCK TABLES `timetable_integration_config` WRITE;
/*!40000 ALTER TABLE `timetable_integration_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `timetable_integration_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usage_record`
--

DROP TABLE IF EXISTS `usage_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usage_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `person` varchar(160) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `resource` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `scene` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `use_time` datetime DEFAULT NULL,
  `status` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `remark` text COLLATE utf8mb4_general_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `user_id` bigint DEFAULT NULL,
  `lab_id` bigint DEFAULT NULL,
  `source_type` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `source_id` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_usage_record_user_time` (`user_id`,`use_time`),
  KEY `idx_usage_record_lab_time` (`lab_id`,`use_time`),
  KEY `idx_usage_record_source` (`source_type`,`source_id`),
  CONSTRAINT `fk_usage_record_lab` FOREIGN KEY (`lab_id`) REFERENCES `lab` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_usage_record_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usage_record`
--

LOCK TABLES `usage_record` WRITE;
/*!40000 ALTER TABLE `usage_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `usage_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_role`
--

DROP TABLE IF EXISTS `user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_user_role_role` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_role`
--

LOCK TABLES `user_role` WRITE;
/*!40000 ALTER TABLE `user_role` DISABLE KEYS */;
INSERT INTO `user_role` VALUES (1,1,1,'2026-10-15 11:43:23','2026-10-15 11:43:23');
/*!40000 ALTER TABLE `user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'smart_lab_recovered_20261015'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-15 11:43:53
