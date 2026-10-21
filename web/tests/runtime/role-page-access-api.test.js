import { beforeEach, expect, it, vi } from 'vitest'
import http from '../../src/api/http'
import { currentPageAccess, refreshPageAccess } from '../../src/api/services/rolePageAccess'
vi.mock('../../src/api/http',()=>({default:{get:vi.fn(),put:vi.fn()}}))
beforeEach(()=>{vi.clearAllMocks();currentPageAccess.value=null})
it('missing or restarting backend leaves super-admin recovery menus instead of a blank sidebar',async()=>{
  http.get.mockRejectedValue(new Error('missing endpoint'))
  await expect(refreshPageAccess('systemAdmin')).rejects.toThrow('missing endpoint')
  expect(currentPageAccess.value.visiblePaths).toEqual(['/system-admin','/system-admin/role-access'])
  expect(currentPageAccess.value.accessiblePaths).not.toContain('/system-admin/users')
})
it('a successful retry restores actual server permissions',async()=>{
  const policy={role:'labAdmin',accessiblePaths:['/lab-admin','/lab-admin/devices'],visiblePaths:['/lab-admin']}
  http.get.mockResolvedValue({data:{data:policy}})
  expect(await refreshPageAccess('labAdmin')).toEqual(policy)
  expect(currentPageAccess.value).toEqual(policy)
})
it('another role response cannot overwrite current role permissions',async()=>{
  http.get.mockResolvedValue({data:{data:{role:'systemAdmin',accessiblePaths:['/system-admin/users'],visiblePaths:[]}}})
  await expect(refreshPageAccess('labAdmin')).rejects.toThrow()
  expect(currentPageAccess.value.role).toBe('labAdmin')
  expect(currentPageAccess.value.accessiblePaths).toEqual(['/lab-admin'])
})
