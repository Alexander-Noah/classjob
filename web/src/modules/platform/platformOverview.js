const roleNames = {
  systemAdmin: '系统管理员',
  labAdmin: '实验室管理员',
  teacher: '教师',
  academic: '教务人员',
}

const enabled = (user) => Number(user.status) === 1
const administrator = (user) => ['systemAdmin', 'labAdmin'].includes(user.roleCode)
const roleName = (user) => roleNames[user.roleCode] || user.roleName || '未分配角色'
const countText = (rows, predicate = () => true) =>
  Array.isArray(rows) ? String(rows.filter(predicate).length) : '—'

export function buildPlatformSummary(users, notices) {
  return [
    { label: '账号总数', value: countText(users), trend: '当前数据库中的账号' },
    { label: '启用账号', value: countText(users, enabled), trend: '当前启用状态' },
    { label: '管理员账号', value: countText(users, administrator), trend: '系统管理员与实验室管理员' },
    { label: '系统通知', value: countText(notices), trend: '当前通知记录数' },
  ]
}

export function buildPlatformAccountRows(users) {
  return (users || []).map((user) => [
    user.username,
    user.realName || '未设置',
    roleName(user),
    user.createdAt || '未记录',
    enabled(user) ? '启用' : '停用',
  ])
}

const groupCounts = (rows, key) => {
  const counts = new Map()
  for (const row of rows || []) {
    const name = key(row)
    if (name) counts.set(name, (counts.get(name) || 0) + 1)
  }
  return [...counts].map(([name, value]) => ({ name, value })).sort((a, b) => a.name.localeCompare(b.name))
}

export function buildPlatformStatistics(users, notices) {
  const roles = groupCounts(users, roleName)
  const creationDates = groupCounts(users, (user) => String(user.createdAt || '').match(/^\d{4}-\d{2}-\d{2}/)?.[0])
  const noticeStatuses = groupCounts(notices, (notice) => notice.publishStatus || '未标记')
  return {
    summary: buildPlatformSummary(users, notices).map((item, index) => ({ ...item, iconKey: ['account', 'account', 'governance', 'notice'][index] })),
    trend: { name: '账号创建数', unit: '个', categories: creationDates.map((row) => row.name), values: creationDates.map((row) => row.value), color: '#4f46e5' },
    compare: { name: '账号数', unit: '个', categories: Array.isArray(users) ? ['启用', '停用'] : [], values: Array.isArray(users) ? [users.filter(enabled).length, users.filter((user) => !enabled(user)).length] : [], color: '#6366f1' },
    distribution: { name: '角色分布', data: roles },
    ranking: { name: '通知数', unit: '条', categories: noticeStatuses.map((row) => row.name), values: noticeStatuses.map((row) => row.value), color: '#8b5cf6' },
    details: roles.map((row) => ({ metric: row.name, value: `${row.value} 个`, change: '当前数量', owner: '用户管理', status: '数据库记录', tagType: 'info' })),
    insights: [{ title: '当前数据库快照', description: '账号、角色和通知均来自实时接口；未采集的登录活跃度、权限命中率不生成推测值。', type: 'info' }],
  }
}
