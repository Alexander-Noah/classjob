/*
 * crawler-presentation.test的自动化测试；夹具用于验证行为，不是运行时配置。
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { credentialPresentation, crawlerTaskStatusLabel } from '../src/modules/timetable/crawlerPresentation.js'

test('an unreadable saved account asks for reconfiguration and cannot start collection', /** 验证当前测试用例的预期行为。 */ () => {
  const state = credentialPresentation({configured:true,usable:false,status:'needs_update'})
  assert.equal(state.title, '教务账号需要更新')
  assert.equal(state.canCollect, false)
  assert.match(state.description, /重新填写账号和密码/)
})

test('missing, loading, failed and ready accounts have distinct states', /** 验证当前测试用例的预期行为。 */ () => {
  assert.equal(credentialPresentation({configured:false}).title, '待配置账号')
  assert.equal(credentialPresentation({status:'loading'}).title, '正在读取账号')
  assert.equal(credentialPresentation({status:'load_failed',configured:true}).title, '账号状态加载失败')
  assert.equal(credentialPresentation({configured:true,usable:true,usernameMasked:'ab***'}).canCollect, true)
})

test('task statuses are displayed as readable Chinese without inventing success', /** 验证当前测试用例的预期行为。 */ () => {
  assert.equal(crawlerTaskStatusLabel('failed'), '采集失败')
  assert.equal(crawlerTaskStatusLabel('running'), '正在采集')
  assert.equal(crawlerTaskStatusLabel('success'), '采集成功')
  assert.equal(crawlerTaskStatusLabel('unexpected-state'), '状态未知')
  assert.equal(crawlerTaskStatusLabel(), '暂无记录')
})
