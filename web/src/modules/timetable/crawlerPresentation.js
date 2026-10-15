export function credentialPresentation(credential = {}) {
  if (credential.status === 'loading') return { title: '正在读取账号', description: '请稍候', canCollect: false }
  if (credential.status === 'load_failed') return { title: '账号状态加载失败', description: '请刷新页面重试', canCollect: false }
  if (credential.configured && credential.usable === false) {
    return { title: '教务账号需要更新', description: '请重新填写账号和密码后保存', canCollect: false }
  }
  if (credential.configured) return { title: '账号已配置', description: credential.usernameMasked || '已加密保存', canCollect: true }
  return { title: '待配置账号', description: '新增账号后才能启动采集', canCollect: false }
}

export function crawlerTaskStatusLabel(status) {
  return ({ success: '采集成功', failed: '采集失败', error: '采集失败', running: '正在采集', pending: '等待采集', skipped: '已跳过', cancelled: '已取消' })[status] || (status ? '状态未知' : '暂无记录')
}
