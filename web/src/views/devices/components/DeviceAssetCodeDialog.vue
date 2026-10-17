<!--
  Web 管理平台的设备资产弹窗视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Printer } from '@element-plus/icons-vue'
import { generateQrSvgDataUrl } from '../../../utils/qrCode'

defineOptions({
  name: 'DeviceAssetCodeDialog',
})

const props = defineProps({
  device: {
    type: Object,
    default: null,
  },
  devices: {
    type: Array,
    /**
     * 构造当前类型的默认状态。
     * @returns 生成的数组。
     */
    default: () => [],
  },
  modelValue: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  /**
   * 获取当前操作。
   * @returns props.modelValue 的当前值。
   */
  get: () => props.modelValue,
  /**
   * 设置当前操作。
   * 入参：value（值）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  set: (value) => emit('update:modelValue', value),
})

const selectedDevices = computed(/** 计算派生状态。 */ () => {
  if (props.devices?.length) return props.devices.filter(Boolean)
  return props.device ? [props.device] : []
})

const activeDevice = computed(/** 计算派生状态。 */ () => selectedDevices.value[0] || null)

/**
 * 转义HTML。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const escapeHtml = (value) =>
  String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')

/**
 * 获取资产URL。
 * 入参：device（设备）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getAssetUrl = (device) => {
  if (!device || typeof window === 'undefined') return ''

  const url = new URL(`${window.location.origin}${window.location.pathname}`)
  url.searchParams.set('device', String(device.id))
  return url.toString()
}

/**
 * 获取Qr编码URL。
 * 入参：device（设备）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getQrCodeUrl = (device) => {
  const url = getAssetUrl(device)
  return url ? generateQrSvgDataUrl(url) : ''
}

const assetUrl = computed(/** 计算派生状态。 */ () => getAssetUrl(activeDevice.value))
const qrCodeUrl = computed(/** 计算派生状态。 */ () => getQrCodeUrl(activeDevice.value))
const inventoryRecords = computed(/** 计算派生状态。 */ () => activeDevice.value?.inventoryRecords || [])
const batchCount = computed(/** 计算派生状态。 */ () => selectedDevices.value.length)

/**
 * 下载Qr编码。
 */
const downloadQrCode = () => {
  if (!activeDevice.value || !qrCodeUrl.value) return

  const link = document.createElement('a')
  link.href = qrCodeUrl.value
  link.download = `${activeDevice.value.code || activeDevice.value.name || '设备'}-资产二维码.svg`
  link.click()
}

/**
 * 输出标签。
 */
const printLabel = () => {
  if (!selectedDevices.value.length) return

  const printWindow = window.open('', '_blank', 'width=760,height=680')
  if (!printWindow) {
    ElMessage.warning('浏览器拦截了打印窗口，请允许弹窗后重试')
    return
  }

  const labelsHtml = selectedDevices.value
    .map(/** 将当前条目转换为目标结果。 入参：device。 */ (device) => {
      const maintenanceHtml = (device.maintenance || [])
        .slice(0, 4)
        .map(/** 将当前条目转换为目标结果。 入参：record。 */ (record) => `<li>${escapeHtml(record)}</li>`)
        .join('')
      const inventoryHtml = (device.inventoryRecords || [])
        .slice(0, 4)
        .map(
          /**
           * 将当前条目转换为目标结果。
           * 入参：record。
           */
          (record) =>
            `<li>${escapeHtml(record.inspectedAt || '-')} ${escapeHtml(record.inspectorName || '-')} ${escapeHtml(record.resultStatus || '-')}</li>`,
        )
        .join('')

      return `
        <section class="label">
          <div class="header">
            <div>
              <h1>${escapeHtml(device.name)}</h1>
              <div class="code">${escapeHtml(device.code)} · ${escapeHtml(device.category)}</div>
            </div>
            <img src="${getQrCodeUrl(device)}" alt="设备二维码" />
          </div>
          <dl>
            <dt>状态</dt><dd>${escapeHtml(device.status)}</dd>
            <dt>实验室</dt><dd>${escapeHtml(device.lab || '-')}</dd>
            <dt>责任人</dt><dd>${escapeHtml(device.owner || '-')}</dd>
            <dt>位置</dt><dd>${escapeHtml(device.location || '-')}</dd>
            <dt>盘点</dt><dd>${escapeHtml(device.inventoryDate || '-')}</dd>
          </dl>
          <ul>${maintenanceHtml}</ul>
          <ul>${inventoryHtml}</ul>
        </section>
      `
    })
    .join('')

  printWindow.document.write(`
    <!doctype html>
    <html lang="zh-CN">
      <head>
        <meta charset="UTF-8" />
        <title>设备资产标签</title>
        <style>
          * { box-sizing: border-box; }
          body { margin: 0; padding: 28px; font-family: Arial, "Microsoft YaHei", sans-serif; color: #0f172a; }
          .label { width: 420px; min-height: 260px; padding: 18px; border: 1px solid #cbd5e1; border-radius: 8px; }
          .label + .label { margin-top: 22px; page-break-before: always; }
          .header { display: flex; justify-content: space-between; gap: 18px; }
          h1 { margin: 0 0 8px; font-size: 22px; }
          .code { color: #475569; font-size: 14px; }
          img { width: 132px; height: 132px; }
          dl { display: grid; grid-template-columns: 78px 1fr; gap: 8px 12px; margin: 16px 0 0; font-size: 13px; }
          dt { color: #64748b; }
          dd { margin: 0; font-weight: 700; text-align: right; }
          ul { margin: 14px 0 0; padding-left: 18px; color: #475569; font-size: 12px; }
          @media print { body { padding: 0; } .label { border-color: #0f172a; } }
        </style>
      </head>
      <body>
        ${labelsHtml}
        <script>
          window.onload = () => {
            window.focus();
            window.print();
          };
        <\/script>
      </body>
    </html>
  `)
  printWindow.document.close()
}
</script>

<template>
  <el-dialog v-model="visible" align-center destroy-on-close title="设备资产码" width="min(760px, calc(100vw - 32px))">
    <div v-if="activeDevice" class="asset-code-dialog">
      <section class="asset-label-preview">
        <img class="qr-image" :alt="`${activeDevice.name} 资产二维码`" :src="qrCodeUrl" />
        <div class="asset-title">
          <span>智云实验室资产标签</span>
          <h2>{{ activeDevice.name }}</h2>
          <p>{{ activeDevice.code }} · {{ activeDevice.category }}</p>
          <el-tag v-if="batchCount > 1" effect="light" type="primary">已选择 {{ batchCount }} 台设备</el-tag>
        </div>
      </section>

      <section class="asset-info-grid">
        <div>
          <span>当前状态</span>
          <strong>{{ activeDevice.status }}</strong>
        </div>
        <div>
          <span>所属实验室</span>
          <strong>{{ activeDevice.lab || '-' }}</strong>
        </div>
        <div>
          <span>责任人</span>
          <strong>{{ activeDevice.owner || '-' }}</strong>
        </div>
        <div>
          <span>安装位置</span>
          <strong>{{ activeDevice.location || '-' }}</strong>
        </div>
        <div>
          <span>最近盘点</span>
          <strong>{{ activeDevice.inventoryDate || '-' }}</strong>
        </div>
      </section>

      <section class="asset-records">
        <div>
          <h3>维修记录</h3>
          <p v-if="!activeDevice.maintenance?.length">暂无维修记录</p>
          <ul v-else>
            <li v-for="record in activeDevice.maintenance" :key="record">{{ record }}</li>
          </ul>
        </div>
        <div>
          <h3>盘点记录</h3>
          <p v-if="!inventoryRecords.length">{{ activeDevice.inventoryDate || '暂无盘点记录' }}</p>
          <ul v-else>
            <li v-for="record in inventoryRecords" :key="record.id">
              {{ record.inspectedAt }} · {{ record.inspectorName }} · {{ record.resultStatus }}
              <span v-if="record.remark">（{{ record.remark }}）</span>
            </li>
          </ul>
        </div>
      </section>

      <div class="asset-url">
        <span>扫码访问</span>
        <code>{{ assetUrl }}</code>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button :icon="Download" @click="downloadQrCode">下载二维码</el-button>
      <el-button :icon="Printer" type="primary" @click="printLabel">
        {{ batchCount > 1 ? '批量打印标签' : '打印标签' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.asset-code-dialog {
  display: grid;
  gap: 16px;
}

.asset-label-preview {
  display: grid;
  grid-template-columns: 156px minmax(0, 1fr);
  gap: 18px;
  align-items: center;
  padding: 18px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: #f8fbff;
}

.qr-image {
  width: 150px;
  height: 150px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: #fff;
}

.asset-title span,
.asset-info-grid span,
.asset-url span {
  color: var(--text-secondary);
  font-size: 12px;
}

.asset-title h2 {
  margin: 8px 0;
  color: var(--text-primary);
  font-size: 24px;
  letter-spacing: 0;
}

.asset-title p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.asset-info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.asset-info-grid div {
  min-height: 74px;
  padding: 12px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--surface-color);
}

.asset-info-grid strong {
  display: block;
  margin-top: 8px;
  color: var(--text-primary);
  font-size: 15px;
  word-break: break-word;
}

.asset-records {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.asset-records div {
  padding: 14px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
}

.asset-records h3 {
  margin: 0 0 10px;
  color: var(--text-primary);
  font-size: 14px;
}

.asset-records p,
.asset-records li {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.asset-records ul {
  display: grid;
  gap: 6px;
  margin: 0;
  padding-left: 18px;
}

.asset-url {
  display: grid;
  gap: 8px;
}

.asset-url code {
  padding: 10px 12px;
  overflow-wrap: anywhere;
  border-radius: 8px;
  color: #334155;
  background: #f8fafc;
  font-size: 12px;
}

@media (max-width: 760px) {
  .asset-label-preview,
  .asset-records,
  .asset-info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
