/*
 * 设备资产的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：calculateDeviceAssetStats。
 */
/**
 * 归一化状态。
 * 入参：device（设备）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeStatus = (device = {}) => String(device.status || '正常').trim() || '正常'

/**
 * 计算设备资产Stats。
 * 入参：records（记录集合）。
 * @returns 组装的键值对象。
 */
export function calculateDeviceAssetStats(records = []) {
  const items = Array.isArray(records) ? records : []

  return {
    total: items.length,
    normal: items.filter(/** 判断当前条目是否符合筛选条件。 入参：record。 */ (record) => normalizeStatus(record) === '正常').length,
    repairing: items.filter(/** 判断当前条目是否符合筛选条件。 入参：record。 */ (record) => normalizeStatus(record) === '维修中').length,
    idle: items.filter(/** 判断当前条目是否符合筛选条件。 入参：record。 */ (record) => normalizeStatus(record) === '闲置').length,
    scrapped: items.filter(/** 判断当前条目是否符合筛选条件。 入参：record。 */ (record) => normalizeStatus(record) === '已报废').length,
  }
}
