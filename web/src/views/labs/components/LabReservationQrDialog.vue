<script setup>
import { computed } from 'vue'
import { CopyDocument, Printer, Tickets } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { generateQrSvgDataUrl } from '../../../utils/qrCode'

defineOptions({
  name: 'LabReservationQrDialog',
})

const props = defineProps({
  lab: {
    type: Object,
    default: null,
  },
  reservationLink: {
    type: String,
    default: '',
  },
})

const visible = defineModel({ type: Boolean, default: false })

const qrDataUrl = computed(() =>
  props.reservationLink
    ? generateQrSvgDataUrl(props.reservationLink, {
        foreground: '#0f172a',
        background: '#ffffff',
      })
    : '',
)

const labTitle = computed(() => props.lab?.name || '实验室预约二维码')
const reservationUrl = computed(() => {
  try {
    return props.reservationLink ? new URL(props.reservationLink) : null
  } catch {
    return null
  }
})
const isLoopbackLink = computed(() => {
  const hostname = reservationUrl.value?.hostname || ''
  return hostname === 'localhost' || hostname === '0.0.0.0' || hostname === '::1' || hostname.startsWith('127.')
})
const linkHint = computed(() =>
  isLoopbackLink.value
    ? '当前二维码是本机地址，手机扫码会打不开。请设置 VITE_PUBLIC_APP_URL 为电脑局域网地址，或用局域网地址打开后台后重新生成。'
    : '该链接可用于手机扫码访问，请确认手机与服务地址网络可达。',
)

const copyLink = async () => {
  if (!props.reservationLink) return

  try {
    await navigator.clipboard.writeText(props.reservationLink)
    ElMessage.success('预约二维码链接已复制')
  } catch {
    ElMessage.warning('当前浏览器不支持自动复制，请手动复制链接')
  }
}

const printQr = () => {
  window.print()
}
</script>

<template>
  <el-dialog v-model="visible" align-center class="lab-reservation-qr-dialog" title="预约二维码" width="min(560px, calc(100vw - 32px))">
    <div v-if="lab" class="qr-dialog-body">
      <div class="qr-poster">
        <div class="poster-header">
          <el-icon aria-hidden="true">
            <Tickets />
          </el-icon>
          <div>
            <h2>{{ labTitle }}</h2>
            <span>{{ lab.code }} · {{ lab.building }} · {{ lab.room }}</span>
          </div>
        </div>
        <img v-if="qrDataUrl" :alt="`${labTitle}预约二维码`" :src="qrDataUrl" class="qr-image" />
        <div class="poster-footer">
          <strong>扫码预约</strong>
          <span>自动识别实验室，填写时间、人数和用途后提交审核</span>
        </div>
      </div>

      <el-input :model-value="reservationLink" readonly>
        <template #prepend>链接</template>
      </el-input>
      <el-alert
        class="link-alert"
        :closable="false"
        show-icon
        :title="isLoopbackLink ? '手机不可访问本机地址' : '手机扫码链接'"
        :type="isLoopbackLink ? 'warning' : 'success'"
        :description="linkHint"
      />
    </div>

    <template #footer>
      <el-button :icon="Printer" @click="printQr">打印张贴</el-button>
      <el-button type="primary" :icon="CopyDocument" @click="copyLink">复制链接</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.qr-dialog-body {
  display: grid;
  gap: 16px;
}

.qr-poster {
  display: grid;
  gap: 18px;
  justify-items: center;
  padding: 22px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: #ffffff;
}

.poster-header {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-self: stretch;
}

.poster-header :deep(.el-icon) {
  display: inline-flex;
  width: 44px;
  height: 44px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: var(--primary-color);
  background: #eff6ff;
  font-size: 22px;
}

.poster-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0;
}

.poster-header span,
.poster-footer span {
  color: var(--text-secondary);
  font-size: 13px;
}

.qr-image {
  width: min(260px, 72vw);
  height: min(260px, 72vw);
  image-rendering: pixelated;
}

.poster-footer {
  display: grid;
  gap: 6px;
  justify-items: center;
  text-align: center;
}

.poster-footer strong {
  color: var(--text-primary);
  font-size: 18px;
}

.link-alert {
  line-height: 1.5;
}

@media (max-width: 560px) {
  .qr-poster {
    gap: 14px;
    padding: 16px;
  }

  .poster-header {
    align-items: flex-start;
  }

  .qr-image {
    width: min(238px, 76vw);
    height: min(238px, 76vw);
  }
}

@media print {
  :global(body *) {
    visibility: hidden;
  }

  .lab-reservation-qr-dialog,
  .lab-reservation-qr-dialog * {
    visibility: visible;
  }
}
</style>
