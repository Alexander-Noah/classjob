/*
 * 设备资产的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：calculateDeviceAssetStats。
 */
const normalizeStatus = (device = {}) => String(device.status || '正常').trim() || '正常'

export function calculateDeviceAssetStats(records = []) {
  const items = Array.isArray(records) ? records : []

  return {
    total: items.length,
    normal: items.filter((record) => normalizeStatus(record) === '正常').length,
    repairing: items.filter((record) => normalizeStatus(record) === '维修中').length,
    idle: items.filter((record) => normalizeStatus(record) === '闲置').length,
    scrapped: items.filter((record) => normalizeStatus(record) === '已报废').length,
  }
}
