/*
 * 实验室的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：getLabDevices、listLabs。
 */
import http from '../http'
import { API_ENDPOINTS } from '../endpoints'

const unwrapData = (response) => {
    const payload = response?.data ?? response
    if (payload?.code && payload.code !== 200) {
        throw new Error(payload?.message || payload?.msg || '实验室接口调用失败')
    }
    return payload?.data ?? payload
}

export async function getLabDevices(labId) {
    return unwrapData(await http.get(API_ENDPOINTS.labs.devices(labId)))
}

export async function listLabs() {
    const labs = unwrapData(await http.get(API_ENDPOINTS.labs.list))
    return Array.isArray(labs) ? labs : []
}
