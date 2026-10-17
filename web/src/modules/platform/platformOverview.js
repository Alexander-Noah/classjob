/*
 * 平台概览公共逻辑模块，供相关视图或服务复用。
 * 主要导出：buildPlatformSummary、buildPlatformAccountRows、buildPlatformStatistics。
 */
const roleNames = {
  systemAdmin: '系统管理员',
  labAdmin: '实验室管理员',
  teacher: '教师',
  academic: '教务人员',
}

/**
 * 已启用的处理逻辑。
 * 入参：user（用户）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const enabled = (user) => Number(user.status) === 1
/**
 * administrator的处理逻辑。
 * 入参：user（用户）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const administrator = (user) => ['systemAdmin', 'labAdmin'].includes(user.roleCode)
/**
 * 角色名称的处理逻辑。
 * 入参：user（用户）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const roleName = (user) => roleNames[user.roleCode] || user.roleName || '未分配角色'
/**
 * 统计文本。
 * 入参：rows（行集合）；predicate（predicate）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const countText = (rows, predicate = /** 处理当前调用传入的回调逻辑。 */ () => true) =>
  Array.isArray(rows) ? String(rows.filter(predicate).length) : '—'

/**
 * 构建Platform摘要。
 * 入参：users（用户）；notices（通知）。
 * @returns 生成的数组。
 */
export function buildPlatformSummary(users, notices) {
  return [
    { label: '账号总数', value: countText(users), trend: '当前数据库中的账号' },
    { label: '启用账号', value: countText(users, enabled), trend: '当前启用状态' },
    { label: '管理员账号', value: countText(users, administrator), trend: '系统管理员与实验室管理员' },
    { label: '系统通知', value: countText(notices), trend: '当前通知记录数' },
  ]
}

/**
 * 构建PlatformAccount行集合。
 * 入参：users（用户）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export function buildPlatformAccountRows(users) {
  return (users || []).map(/** 将当前条目转换为目标结果。 入参：user。 */ (user) => [
    user.username,
    user.realName || '未设置',
    roleName(user),
    user.createdAt || '未记录',
    enabled(user) ? '启用' : '停用',
  ])
}

/**
 * 分组数量的处理逻辑。
 * 入参：rows（行集合）；key（键）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const groupCounts = (rows, key) => {
  const counts = new Map()
  for (const row of rows || []) {
    const name = key(row)
    if (name) counts.set(name, (counts.get(name) || 0) + 1)
  }
  return [...counts].map(/** 将当前条目转换为目标结果。 */ ([name, value]) => ({ name, value })).sort(/** 比较当前两项的排序先后。 入参：a、b。 */ (a, b) => a.name.localeCompare(b.name))
}

/**
 * 构建PlatformStatistics。
 * 入参：users（用户）；notices（通知）。
 * @returns 组装的键值对象。
 */
export function buildPlatformStatistics(users, notices) {
  const roles = groupCounts(users, roleName)
  const creationDates = groupCounts(users, /** 处理 groupCounts 调用中的回调逻辑。 入参：user。 */ (user) => String(user.createdAt || '').match(/^\d{4}-\d{2}-\d{2}/)?.[0])
  const noticeStatuses = groupCounts(notices, /** 处理 groupCounts 调用中的回调逻辑。 入参：notice。 */ (notice) => notice.publishStatus || '未标记')
  return {
    summary: buildPlatformSummary(users, notices).map(/** 将当前条目转换为目标结果。 入参：item、index。 */ (item, index) => ({ ...item, iconKey: ['account', 'account', 'governance', 'notice'][index] })),
    trend: { name: '账号创建数', unit: '个', categories: creationDates.map(/** 将当前条目转换为目标结果。 入参：row。 */ (row) => row.name), values: creationDates.map(/** 将当前条目转换为目标结果。 入参：row。 */ (row) => row.value), color: '#4f46e5' },
    compare: { name: '账号数', unit: '个', categories: Array.isArray(users) ? ['启用', '停用'] : [], values: Array.isArray(users) ? [users.filter(enabled).length, users.filter(/** 判断当前条目是否符合筛选条件。 入参：user。 */ (user) => !enabled(user)).length] : [], color: '#6366f1' },
    distribution: { name: '角色分布', data: roles },
    ranking: { name: '通知数', unit: '条', categories: noticeStatuses.map(/** 将当前条目转换为目标结果。 入参：row。 */ (row) => row.name), values: noticeStatuses.map(/** 将当前条目转换为目标结果。 入参：row。 */ (row) => row.value), color: '#8b5cf6' },
    details: roles.map(/** 将当前条目转换为目标结果。 入参：row。 */ (row) => ({ metric: row.name, value: `${row.value} 个`, change: '当前数量', owner: '用户管理', status: '数据库记录', tagType: 'info' })),
    insights: [{ title: '当前数据库快照', description: '账号、角色和通知均来自实时接口；未采集的登录活跃度、权限命中率不生成推测值。', type: 'info' }],
  }
}
