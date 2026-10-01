<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, DocumentChecked, Download, Refresh, Setting } from '@element-plus/icons-vue'
import { AUTH_ROLE_DEFAULT_PATH } from '../../constants/auth'
import { getSystemSettings, resetSystemSettings, saveSystemSettings } from '../../api/services/systemSettings'
import { exportTableToExcel } from '../../utils/exportExcel'

defineOptions({
  name: 'SystemSettings',
})

const router = useRouter()

const activeTab = ref('basic')
const isLoading = ref(false)

const metrics = [
  { label: '配置分组', value: 3, trend: '基础配置 / 登录策略 / AI 接口' },
  { label: '开关项', value: 9, trend: '支持实时启停' },
  { label: '审计记录', value: 24, trend: '本月已归档' },
  { label: '服务策略', value: 4, trend: '统一后端配置' },
]

const basicConfig = reactive({
  platformName: '智云实验室协同管控平台',
  noticeTarget: '实验室管理员',
  defaultHome: '按角色首页',
  autoBackup: true,
  dataRetention: 180,
})

const loginPolicy = reactive({
  singleSignOn: true,
  forcePasswordReset: false,
  loginAudit: true,
  sessionTimeout: 45,
  captchaEnabled: true,
})

const aiSettings = reactive({
  provider: '通义智能体',
  baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
  timeout: 30,
  autoSuggestion: true,
  fallbackReply: '服务异常时返回平台内置运维建议。',
})

const updateLogs = [
  {
    time: '今天 10:30',
    title: '更新登录策略',
    description: '启用了登录审计，并保持单点登录开关开启。',
    type: 'primary',
  },
  {
    time: '昨天 16:20',
    title: '调整公告默认对象',
    description: '平台公告默认优先触达实验室管理员。',
    type: 'success',
  },
  {
    time: '本周一 09:00',
    title: '更新 AI 服务参数',
    description: '配置企业智能体服务地址、超时时间和降级回复策略。',
    type: 'warning',
  },
]

const collectSettings = () => ({
  basicConfig: { ...basicConfig },
  loginPolicy: { ...loginPolicy },
  aiSettings: { ...aiSettings },
})

const applySettings = (settings = {}) => {
  Object.assign(basicConfig, settings.basicConfig || {})
  Object.assign(loginPolicy, settings.loginPolicy || {})
  Object.assign(aiSettings, settings.aiSettings || {})
}

const loadSettings = async () => {
  isLoading.value = true
  try {
    const settings = await getSystemSettings(collectSettings())
    applySettings(settings)
  } catch (error) {
    ElMessage.warning(error?.message || '系统配置加载失败，已使用默认配置')
  } finally {
    isLoading.value = false
  }
}

const saveSettings = async () => {
  try {
    const settings = await saveSystemSettings(collectSettings())
    applySettings(settings)
    ElMessage.success('系统配置已保存')
  } catch (error) {
    ElMessage.error(error?.message || '系统配置保存失败')
  }
}

const publishChangeLog = () => {
  ElMessage.success('配置变更说明已生成')
}

const resetDraft = async () => {
  try {
    const settings = await resetSystemSettings()
    applySettings(settings)
    ElMessage.info('已恢复后端默认配置')
  } catch (error) {
    ElMessage.error(error?.message || '恢复默认配置失败')
  }
}

const exportSettings = () => {
  const groupNameMap = {
    basicConfig: '基础配置',
    loginPolicy: '登录策略',
    aiSettings: 'AI 接口',
  }
  const settings = collectSettings()
  const rows = Object.entries(settings).flatMap(([group, values]) =>
    Object.entries(values).map(([key, value]) => ({
      group: groupNameMap[group] || group,
      key,
      value,
    })),
  )
  const exportedCount = exportTableToExcel({
    filename: '系统配置',
    sheetName: '系统配置',
    rows,
    columns: [
      { label: '配置分组', prop: 'group' },
      { label: '配置项', prop: 'key' },
      { label: '配置值', prop: 'value' },
    ],
  })

  ElMessage.success(`已导出 ${exportedCount} 项配置`)
}

const handleBack = () => {
  router.push(AUTH_ROLE_DEFAULT_PATH.systemAdmin)
}

onMounted(loadSettings)
</script>

<template>
  <section class="settings-page">
    <el-card class="settings-hero" shadow="never">
      <el-page-header title="返回" @back="handleBack">
        <template #content>
          <div class="hero-title">
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </div>
        </template>
        <template #extra>
          <el-space wrap>
            <el-button type="primary" :icon="Check" @click="saveSettings">保存配置</el-button>
            <el-button :icon="DocumentChecked" @click="publishChangeLog">生成变更说明</el-button>
            <el-button :icon="Download" @click="exportSettings">导出配置</el-button>
            <el-button :icon="Refresh" @click="resetDraft">恢复默认</el-button>
          </el-space>
        </template>
      </el-page-header>

      <el-descriptions class="hero-summary" :column="1">
        <el-descriptions-item label="说明">
          维护平台基础配置、登录策略与 AI 服务参数，配置变更会记录到右侧审计时间线。
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-row :gutter="16" class="settings-metrics">
      <el-col v-for="metric in metrics" :key="metric.label" :lg="6" :md="12" :sm="12" :xs="24">
        <el-card class="metric-card" shadow="never">
          <el-statistic :title="metric.label" :value="metric.value" />
          <div class="metric-trend">{{ metric.trend }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="18">
      <el-col :xs="24" :xl="16">
        <el-card v-loading="isLoading" class="settings-card" shadow="never">
          <template #header>
            <div class="section-title">
              <h2>参数面板</h2>
              <span>按配置分组维护平台参数，不同设置项对应不同 Element Plus 表单组件。</span>
            </div>
          </template>

          <el-tabs v-model="activeTab">
            <el-tab-pane label="基础配置" name="basic">
              <el-form label-position="top" class="settings-form">
                <el-form-item label="平台名称">
                  <el-input v-model="basicConfig.platformName" />
                </el-form-item>
                <el-form-item label="公告默认对象">
                  <el-select v-model="basicConfig.noticeTarget" class="full-width">
                    <el-option label="实验室管理员" value="实验室管理员" />
                    <el-option label="教师" value="教师" />
                    <el-option label="维修人员" value="维修人员" />
                  </el-select>
                </el-form-item>
                <el-form-item label="默认首页">
                  <el-input v-model="basicConfig.defaultHome" />
                </el-form-item>
                <el-form-item label="数据保留天数">
                  <el-input-number v-model="basicConfig.dataRetention" :min="30" :max="365" class="full-width" />
                </el-form-item>
                <el-form-item label="自动备份" class="full-span">
                  <el-switch v-model="basicConfig.autoBackup" active-text="已开启" inactive-text="已关闭" />
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="登录策略" name="login">
              <el-form label-position="top" class="settings-form">
                <el-form-item label="单点登录">
                  <el-switch v-model="loginPolicy.singleSignOn" active-text="启用" inactive-text="关闭" />
                </el-form-item>
                <el-form-item label="验证码校验">
                  <el-switch v-model="loginPolicy.captchaEnabled" active-text="启用" inactive-text="关闭" />
                </el-form-item>
                <el-form-item label="会话超时（分钟）">
                  <el-input-number v-model="loginPolicy.sessionTimeout" :min="15" :max="180" class="full-width" />
                </el-form-item>
                <el-form-item label="登录审计">
                  <el-switch v-model="loginPolicy.loginAudit" active-text="启用" inactive-text="关闭" />
                </el-form-item>
                <el-form-item label="首次登录强制修改密码" class="full-span">
                  <el-switch v-model="loginPolicy.forcePasswordReset" active-text="启用" inactive-text="关闭" />
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="AI 接口" name="ai">
              <el-form label-position="top" class="settings-form">
                <el-form-item label="接口来源">
                  <el-select v-model="aiSettings.provider" class="full-width">
                    <el-option label="通义智能体" value="通义智能体" />
                    <el-option label="企业智能体平台" value="企业智能体平台" />
                    <el-option label="校园私有部署" value="校园私有部署" />
                  </el-select>
                </el-form-item>
                <el-form-item label="接口地址">
                  <el-input v-model="aiSettings.baseUrl" />
                </el-form-item>
                <el-form-item label="超时时间（秒）">
                  <el-input-number v-model="aiSettings.timeout" :min="5" :max="120" class="full-width" />
                </el-form-item>
                <el-form-item label="自动建议">
                  <el-switch v-model="aiSettings.autoSuggestion" active-text="开启" inactive-text="关闭" />
                </el-form-item>
                <el-form-item label="降级回复说明" class="full-span">
                  <el-input v-model="aiSettings.fallbackReply" type="textarea" :rows="4" />
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="8">
        <el-card class="timeline-card" shadow="never">
          <template #header>
            <div class="section-title">
              <h2>变更留痕</h2>
            </div>
          </template>

          <el-timeline>
            <el-timeline-item v-for="item in updateLogs" :key="item.title" :timestamp="item.time" :type="item.type">
              <strong>{{ item.title }}</strong>
              <p>{{ item.description }}</p>
            </el-timeline-item>
          </el-timeline>
          <el-alert class="settings-alert" :closable="false" title="配置变更已接入后端持久化，并写入平台操作事件。" type="success" show-icon />
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>

<style scoped>
.settings-page {
  display: grid;
  gap: 18px;
}

.settings-hero,
.settings-card,
.timeline-card,
.metric-card {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
}

.settings-hero :deep(.el-card__body) {
  padding: 22px;
}

.hero-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
  font-size: 22px;
  font-weight: 700;
}

.hero-title .el-icon {
  color: var(--primary-color);
  font-size: 24px;
}

.hero-summary {
  margin-top: 16px;
}

.hero-summary :deep(.el-descriptions__label) {
  color: var(--text-secondary);
  font-weight: 400;
}

.settings-metrics {
  row-gap: 16px;
}

.metric-card {
  min-height: 112px;
}

.metric-card :deep(.el-card__body) {
  padding: 18px;
}

.metric-card :deep(.el-statistic__head) {
  color: var(--text-secondary);
  font-size: 13px;
}

.metric-card :deep(.el-statistic__content) {
  color: var(--text-primary);
  font-size: 28px;
  font-weight: 700;
}

.metric-trend {
  margin-top: 10px;
  color: var(--text-secondary);
  font-size: 13px;
}

.section-title h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 700;
}

.section-title span {
  display: block;
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.settings-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.settings-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.full-width {
  width: 100%;
}

.full-span {
  grid-column: 1 / -1;
}

.timeline-card :deep(.el-timeline) {
  padding-left: 0;
}

.timeline-card p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.settings-alert {
  margin-top: 16px;
}

@media (max-width: 760px) {
  .settings-form {
    grid-template-columns: 1fr;
  }
}
</style>
