/*
 * 实验室预约的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：getScanReservationProfile、submitScanReservation、getScanReservationStatus。
 */
import http from '../http'
import { API_ENDPOINTS } from '../endpoints'

/**
 * unwrap数据的处理逻辑。
 * 入参：response（响应）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const unwrapData = (response) => {
  const payload = response?.data ?? response
  if (payload?.code && payload.code !== 200) {
    throw new Error(payload?.message || '扫码预约接口调用失败')
  }
  return payload?.data ?? payload
}

/**
 * 获取Scan预约Profile。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function getScanReservationProfile({ labId, code }) {
  return unwrapData(
    await http.get(API_ENDPOINTS.scanReservations.labProfile(labId), {
      params: { code },
    }),
  )
}

/**
 * 提交Scan预约。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function submitScanReservation(payload) {
  return unwrapData(await http.post(API_ENDPOINTS.scanReservations.submit, payload))
}

/**
 * 获取Scan预约状态。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function getScanReservationStatus({ id, contact }) {
  return unwrapData(
    await http.get(API_ENDPOINTS.scanReservations.status(id), {
      params: { contact },
    }),
  )
}
