/*
 * 实验室的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：getLabDevices、listLabs。
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
        throw new Error(payload?.message || payload?.msg || '实验室接口调用失败')
    }
    return payload?.data ?? payload
}

/**
 * 获取实验室设备。
 * 入参：labId（实验室标识）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function getLabDevices(labId) {
    return unwrapData(await http.get(API_ENDPOINTS.labs.devices(labId)))
}

/**
 * 列出实验室。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export async function listLabs() {
    const labs = unwrapData(await http.get(API_ENDPOINTS.labs.list))
    return Array.isArray(labs) ? labs : []
}
