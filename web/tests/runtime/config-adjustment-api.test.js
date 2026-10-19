import { beforeEach,afterEach,expect,it,vi } from 'vitest'
import * as ai from '../../src/api/services/aiAssistantConfig'
import * as adjustments from '../../src/api/services/scheduleAdjustments'
import http from '../../src/api/http'
import { getAuthToken,isAuthenticated } from '../../src/api/services/auth'
vi.mock('../../src/api/http',()=>({default:{get:vi.fn(),post:vi.fn(),put:vi.fn()}}))
vi.mock('../../src/api/config',()=>({API_BASE_URL:'https://api.test/'}))
vi.mock('../../src/api/services/auth',()=>({getAuthToken:vi.fn(),isAuthenticated:vi.fn()}))
beforeEach(()=>{vi.resetAllMocks();isAuthenticated.mockReturnValue(true);getAuthToken.mockReturnValue('token');vi.stubGlobal('fetch',vi.fn().mockResolvedValue({ok:true,json:async()=>({code:200,data:{saved:true}})}))})
afterEach(()=>vi.unstubAllGlobals())
it('AI config requests carry auth, serialize payloads and encode action identifiers',async()=>{
  expect(await ai.getAiAssistantUserConfig()).toEqual({saved:true});expect(fetch).toHaveBeenLastCalledWith('https://api.test/ai-assistant/user-config',expect.objectContaining({headers:expect.objectContaining({Authorization:'Bearer token'})}))
  await ai.saveAiAssistantUserConfig({model:'qwen'});expect(JSON.parse(fetch.mock.calls.at(-1)[1].body)).toEqual({model:'qwen'})
  await ai.saveAiAssistantUserConfig();expect(fetch.mock.calls.at(-1)[1].body).toBe('{}')
  await ai.testAiAssistantUserConfig();expect(fetch.mock.calls.at(-1)[1].body).toBeUndefined();await ai.testAiAssistantUserConfig({model:'qwen'});expect(fetch.mock.calls.at(-1)[1].body).toBe('{"model":"qwen"}')
  await ai.requestAiAssistantAnswer({question:'设备情况',conversationId:'c'});expect(JSON.parse(fetch.mock.calls.at(-1)[1].body)).toEqual({question:'设备情况',history:[],conversationId:'c'})
  await ai.decideAiAssistantAction('a/b','c',true);expect(fetch.mock.calls.at(-1)[0]).toContain('a%2Fb/decision')
})
it('AI config enforces login and reports backend or malformed-response errors',async()=>{
  isAuthenticated.mockReturnValue(false);expect(await ai.getAiAssistantUserConfig()).toEqual({});expect(await ai.saveAiAssistantUserConfig({})).toEqual({});await expect(ai.testAiAssistantUserConfig()).rejects.toThrow('请先登录');await expect(ai.requestAiAssistantAnswer({})).rejects.toThrow('请先登录');expect(fetch).not.toHaveBeenCalled()
  isAuthenticated.mockReturnValue(true);getAuthToken.mockReturnValue('');fetch.mockResolvedValueOnce({ok:false,json:async()=>({message:'配置无效'})});await expect(ai.getAiAssistantUserConfig()).rejects.toThrow('配置无效');expect(fetch.mock.calls.at(-1)[1].headers.Authorization).toBeUndefined()
  fetch.mockResolvedValueOnce({ok:false,json:async()=>{throw Error('bad json')}});await expect(ai.getAiAssistantUserConfig()).rejects.toThrow('AI 接口请求失败')
  fetch.mockResolvedValueOnce({ok:true,json:async()=>({code:403,message:'拒绝'})});await expect(ai.getAiAssistantUserConfig()).rejects.toThrow('拒绝')
  fetch.mockResolvedValueOnce({ok:true,json:async()=>({model:'local'})});expect(await ai.getAiAssistantUserConfig()).toEqual({model:'local'})
})
it('schedule list accepts wrapped and raw arrays but discards malformed data',async()=>{
  for(const data of [[{id:1}],{data:[{id:2}]},{}]){http.get.mockResolvedValueOnce({data});expect(await adjustments.listScheduleAdjustments()).toEqual(Array.isArray(data)?data:data.data||[])}
  for(const data of [{code:500,message:'错误'},{code:500}]){http.get.mockResolvedValueOnce({data});await expect(adjustments.listScheduleAdjustments()).rejects.toThrow(data.message||'获取调课申请失败')}
})
it.each([[adjustments.createScheduleAdjustment,[{course:'A'}],'post'],[adjustments.updateScheduleAdjustment,[7,{course:'A'}],'put'],[adjustments.approveScheduleAdjustment,[7],'post'],[adjustments.completeScheduleAdjustment,[7],'post']])('schedule mutations normalize saved records and acknowledgement fallbacks',async(fn,args,method)=>{
  for(const data of [{data:{id:8}},{id:9},{code:200},[]]){http[method].mockResolvedValueOnce({data});expect(await fn(...args)).toEqual(data.data||data.id&&data|| (args.length>1?args[1]:typeof args[0]==='object'?args[0]:{id:7}))}
  http[method].mockResolvedValueOnce({data:{code:400,message:'失败'}});await expect(fn(...args)).rejects.toThrow('失败');http[method].mockResolvedValueOnce({data:{code:400}});await expect(fn(...args)).rejects.toThrow()
})
