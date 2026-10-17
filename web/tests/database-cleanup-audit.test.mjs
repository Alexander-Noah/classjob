/*
 * database-cleanup-audit.test的自动化测试；夹具用于验证行为，不是运行时配置。
 */
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

/**
 * 读取来源。
 * 入参：path（路径）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('unused table cleanup only drops audited legacy host static asset table', /** 验证当前测试用例的预期行为。 */ () => {
  const cleanupSql = readSource('../database/migrations/20260703_unused_table_cleanup.sql')

  assert.match(cleanupSql, /create table if not exists host_static_asset_backup_20260703/i)
  assert.match(cleanupSql, /drop table if exists host_static_asset/i)
  assert.doesNotMatch(cleanupSql, /drop table if exists iot_device\b/i)
  assert.doesNotMatch(cleanupSql, /drop table if exists iot_device_capability\b/i)
  assert.doesNotMatch(cleanupSql, /drop table if exists lab_activation_code\b/i)
  assert.doesNotMatch(cleanupSql, /drop table if exists lab_terminal\b/i)
  assert.doesNotMatch(cleanupSql, /drop table if exists device_runtime_status\b/i)
  assert.doesNotMatch(cleanupSql, /drop table if exists device_status_event\b/i)
  assert.doesNotMatch(cleanupSql, /drop table if exists module_record\b/i)
})
