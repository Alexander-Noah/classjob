<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage, type DataTableColumns } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const message = useMessage()
const authStore = useAuthStore()

const activeTab = ref('manual')
const manualInput = ref('')
const csvPreviewData = ref<Array<{ username: string; password: string; role: 'admin' | 'teacher' }>>([])
const result = ref<{ success: number; failed: Array<{ username: string; reason: string }> } | null>(null)

const csvColumns: DataTableColumns = [
  { title: '用户名', key: 'username' },
  { title: '密码', key: 'password' },
  {
    title: '角色',
    key: 'role',
    render: row => row.role === 'admin' ? '管理员' : '教师'
  }
]

const failedColumns: DataTableColumns = [
  { title: '用户名', key: 'username' },
  { title: '失败原因', key: 'reason' }
]

function parseRole(raw: string): 'admin' | 'teacher' {
  const trimmed = raw.trim().toLowerCase()
  return trimmed === 'admin' || trimmed === '管理员' ? 'admin' : 'teacher'
}

function parseManualInput() {
  const lines = manualInput.value.split('\n').filter(line => line.trim())
  return lines.map((line) => {
    const parts = line.split(',').map(item => item.trim())
    return {
      username: parts[0] || '',
      password: parts[1] || '',
      role: parseRole(parts[2] || 'teacher')
    }
  })
}

function parseCsvText(text: string) {
  const lines = text.split(/\r?\n/).filter(line => line.trim())
  if (lines.length < 2) return []

  return lines.slice(1).map((line) => {
    const parts = line.split(',').map(item => item.trim())
    return {
      username: parts[0] || '',
      password: parts[1] || '',
      role: parseRole(parts[2] || 'teacher')
    }
  }).filter(item => item.username)
}

function handleCsvUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = (eventPayload) => {
    const text = eventPayload.target?.result as string
    const data = parseCsvText(text)
    if (!data.length) {
      message.warning('CSV 文件内容为空或格式不正确')
      return
    }

    csvPreviewData.value = data
    result.value = null
    message.success(`已解析 ${data.length} 条账号记录`)
  }
  reader.readAsText(file, 'UTF-8')
  input.value = ''
}

function downloadTemplate() {
  const bom = '\uFEFF'
  const header = '用户名,密码,角色'
  const rows = [
    'zhangsan,123456,teacher',
    'lisi,123456,teacher',
    'admin2,admin123,admin'
  ]
  const blob = new Blob([bom + [header, ...rows].join('\n')], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = '批量注册模板.csv'
  link.click()
  URL.revokeObjectURL(url)
}

function handleBatchRegister() {
  const entries = activeTab.value === 'manual' ? parseManualInput() : csvPreviewData.value

  if (!entries.length) {
    message.warning(activeTab.value === 'manual' ? '请输入至少一条用户信息' : '请先上传 CSV 文件')
    return
  }

  result.value = authStore.batchRegister(entries)
  message.success(`批量注册完成：成功 ${result.value.success} 个，失败 ${result.value.failed.length} 个`)
}
</script>

<template>
  <main class="batch-shell">
    <section class="batch-panel">
      <header class="batch-header">
        <div>
          <h1>批量注册</h1>
          <p>通过手动粘贴或 CSV 文件快速创建教师、管理员账号。</p>
        </div>
        <div class="header-actions">
          <n-button tertiary @click="router.push('/register')">单个注册</n-button>
          <n-button tertiary @click="router.push('/')">返回登录</n-button>
        </div>
      </header>

      <div class="batch-body">
        <section class="input-panel">
          <n-tabs v-model:value="activeTab" type="line" animated>
            <n-tab-pane name="manual" tab="手动输入">
              <div class="tab-content">
                <p class="hint-text">
                  每行一个账号，格式为 <code>用户名,密码,角色</code>。角色支持 admin、teacher、管理员、教师。
                </p>
                <n-input
                  v-model:value="manualInput"
                  type="textarea"
                  placeholder="zhangsan,123456,teacher&#10;lisi,123456,teacher&#10;admin2,admin123,admin"
                  :autosize="{ minRows: 9, maxRows: 14 }"
                />
              </div>
            </n-tab-pane>

            <n-tab-pane name="csv" tab="CSV 导入">
              <div class="tab-content">
                <p class="hint-text">
                  CSV 第一行为表头：用户名、密码、角色。上传后可先预览，再执行批量注册。
                </p>
                <div class="csv-actions">
                  <label class="upload-label">
                    <input type="file" accept=".csv" class="file-input" @change="handleCsvUpload" />
                    <n-button type="primary" tag="span">
                      <template #icon>
                        <span class="i-carbon-upload" />
                      </template>
                      选择 CSV 文件
                    </n-button>
                  </label>
                  <n-button tertiary @click="downloadTemplate">
                    <template #icon>
                      <span class="i-carbon-download" />
                    </template>
                    下载模板
                  </n-button>
                </div>
                <n-data-table
                  v-if="csvPreviewData.length"
                  :columns="csvColumns"
                  :data="csvPreviewData"
                  :bordered="false"
                  :single-line="false"
                  size="small"
                />
                <n-empty v-else description="上传 CSV 后在这里预览账号记录" />
              </div>
            </n-tab-pane>
          </n-tabs>
        </section>

        <aside class="result-panel">
          <div class="result-card">
            <div class="result-title">导入结果</div>
            <template v-if="result">
              <div class="result-metrics">
                <div>
                  <span>成功</span>
                  <strong>{{ result.success }}</strong>
                </div>
                <div>
                  <span>失败</span>
                  <strong>{{ result.failed.length }}</strong>
                </div>
              </div>
              <n-data-table
                v-if="result.failed.length"
                :columns="failedColumns"
                :data="result.failed"
                :bordered="false"
                :single-line="false"
                size="small"
              />
              <n-result v-else status="success" title="账号已全部创建" description="可以返回登录页使用新账号登录。" />
            </template>
            <n-empty v-else description="执行批量注册后显示结果" />
          </div>
          <n-button type="primary" size="large" block @click="handleBatchRegister">
            批量注册
          </n-button>
        </aside>
      </div>
    </section>
  </main>
</template>

<style scoped>
.batch-shell {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 28px;
  background: #eef3f8;
}

.batch-panel {
  max-width: 1120px;
  margin: 0 auto;
  border: 1px solid #dbe4ef;
  border-radius: 14px;
  background: #ffffff;
  padding: 24px;
}

.batch-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e2e8f0;
}

.batch-header h1 {
  margin: 0;
  color: #0f172a;
  font-size: 24px;
  font-weight: 800;
}

.batch-header p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
}

.header-actions,
.csv-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.batch-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 20px;
  padding-top: 20px;
}

.input-panel,
.result-card {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 18px;
}

.tab-content {
  padding-top: 10px;
}

.hint-text {
  margin: 0 0 14px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.hint-text code {
  border-radius: 5px;
  background: #f1f5f9;
  color: #334155;
  padding: 2px 6px;
  font-size: 12px;
}

.upload-label {
  display: inline-flex;
  cursor: pointer;
}

.file-input {
  display: none;
}

.result-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.result-card {
  min-height: 360px;
}

.result-title {
  margin-bottom: 14px;
  color: #0f172a;
  font-size: 16px;
  font-weight: 800;
}

.result-metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 14px;
}

.result-metrics div {
  border-radius: 8px;
  background: #f8fafc;
  padding: 12px;
}

.result-metrics span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.result-metrics strong {
  display: block;
  margin-top: 4px;
  color: #0f172a;
  font-size: 24px;
  line-height: 1;
}

@media (max-width: 860px) {
  .batch-body {
    grid-template-columns: 1fr;
  }
}
</style>
