import { afterEach, expect, it, vi } from 'vitest'
import { getAiAssistantUserConfig } from '../../src/api/services/aiAssistantConfig'
import { listAiConversationHistories, saveAiConversationHistories } from '../../src/api/services/aiAssistantHistory'
import { normalizeAvatarUrl } from '../../src/utils/avatarUrl'
import { hasUnsafeContent } from '../../src/utils/reservationValidation'
vi.mock('../../src/api/config',()=>({API_BASE_URL:''}))
vi.mock('../../src/api/services/auth',()=>({getAuthToken:()=> 'session',isAuthenticated:()=>true}))
afterEach(()=>vi.unstubAllGlobals())

it('same-origin deployments keep API and avatar URLs local and normalize non-list history responses',async()=>{
  const fetcher=vi.fn().mockResolvedValue({ok:true,json:async()=>({code:200,data:{configured:true}})})
  vi.stubGlobal('fetch',fetcher)
  expect(await getAiAssistantUserConfig()).toEqual({configured:true})
  expect(await listAiConversationHistories()).toEqual([])
  expect(await saveAiConversationHistories([])).toEqual([])
  expect(fetcher.mock.calls.every(([url])=>url.startsWith('/')&&!url.startsWith('//'))).toBe(true)
  expect(normalizeAvatarUrl('头像/photo.png')).toBe('/files/avatar-proxy?objectKey=%E5%A4%B4%E5%83%8F%2Fphoto.png')
  expect(hasUnsafeContent(null)).toBe(false)
})
