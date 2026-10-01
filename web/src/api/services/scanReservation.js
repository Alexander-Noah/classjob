import http from '../http'
import { API_ENDPOINTS } from '../endpoints'

const unwrapData = (response) => {
  const payload = response?.data ?? response
  if (payload?.code && payload.code !== 200) {
    throw new Error(payload?.message || '扫码预约接口调用失败')
  }
  return payload?.data ?? payload
}

export async function getScanReservationProfile({ labId, code }) {
  return unwrapData(
    await http.get(API_ENDPOINTS.scanReservations.labProfile(labId), {
      params: { code },
    }),
  )
}

export async function submitScanReservation(payload) {
  return unwrapData(await http.post(API_ENDPOINTS.scanReservations.submit, payload))
}

export async function getScanReservationStatus({ id, contact }) {
  return unwrapData(
    await http.get(API_ENDPOINTS.scanReservations.status(id), {
      params: { contact },
    }),
  )
}
