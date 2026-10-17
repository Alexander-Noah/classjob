/*
 * basicResources的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：labResourceService、reservationResourceService、usageRecordResourceService。
 */
import { API_ENDPOINTS } from '../endpoints'
import { createCollectionService } from './collectionService'

export const labResourceService = createCollectionService({
  resourceName: 'labs',
  storageKey: 'smart-lab:lab-management:records',
  endpoints: API_ENDPOINTS.labs,
})

export const reservationResourceService = createCollectionService({
  resourceName: 'reservations',
  storageKey: 'smart-lab:ReservationManagement:records:v2',
  endpoints: API_ENDPOINTS.reservations,
  serverAuthoritativeRecords: true,
})

export const usageRecordResourceService = createCollectionService({
  resourceName: 'usage-records',
  storageKey: 'smart-lab:UsageRecords:records:v2',
  endpoints: API_ENDPOINTS.usageRecords,
})
