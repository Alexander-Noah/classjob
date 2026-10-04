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

export const repairResourceService = createCollectionService({
  resourceName: 'repairs',
  storageKey: 'smart-lab:RepairManagement:records:v2',
  endpoints: API_ENDPOINTS.repairs,
})

export const usageRecordResourceService = createCollectionService({
  resourceName: 'usage-records',
  storageKey: 'smart-lab:UsageRecords:records:v2',
  endpoints: API_ENDPOINTS.usageRecords,
})
