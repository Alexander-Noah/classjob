<script setup>
import { Lock, Monitor, Refresh, Sunny } from '@element-plus/icons-vue'

defineOptions({
  name: 'RoomControlHeader',
})

defineProps({
  gatewayMode: {
    type: Object,
    default: () => ({
      label: '待配置真实网关',
      type: 'info',
      description: '后续补充协议、地址、鉴权和点位配置后即可接入真实网关。',
    }),
  },
  risk: {
    type: Object,
    required: true,
  },
})

defineEmits(['refresh', 'scene'])
</script>

<template>
  <el-card class="page-heading" shadow="never">
    <div class="page-heading-content">
      <span class="heading-icon" aria-hidden="true">
        <el-icon>
          <Monitor />
        </el-icon>
      </span>
      <div class="heading-copy">
        <el-tag effect="light">智能实验室</el-tag>
        <h1>实验室智能控制平台</h1>
        <div class="gateway-mode">
          <el-tag :type="gatewayMode.type" effect="light">{{ gatewayMode.label }}</el-tag>
          <span>{{ gatewayMode.description }}</span>
        </div>
      </div>

      <div class="heading-side">
        <div class="risk-pill">
          <el-tag :type="risk.type" effect="dark">{{ risk.label }}</el-tag>
          <span>{{ risk.description }}</span>
        </div>
        <div class="heading-actions">
          <div class="scene-actions" aria-label="场景模式">
            <el-button class="scene-button" @click="$emit('scene', 'class')">
              <el-icon><Monitor /></el-icon>
              授课
            </el-button>
            <el-button class="scene-button" @click="$emit('scene', 'energy')">
              <el-icon><Sunny /></el-icon>
              节能
            </el-button>
            <el-button class="scene-button" @click="$emit('scene', 'security')">
              <el-icon><Lock /></el-icon>
              安防
            </el-button>
          </div>
          <el-button class="refresh-button" type="primary" @click="$emit('refresh')">
            <el-icon>
              <Refresh />
            </el-icon>
            刷新状态
          </el-button>
        </div>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.page-heading {
  overflow: hidden;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: var(--surface-color);
}

.page-heading :deep(.el-card__body) {
  padding: 22px 24px;
  background:
    linear-gradient(135deg, #ffffff 0%, #f8fbff 56%, #eef6ff 100%);
}

.page-heading-content,
.heading-side,
.heading-actions,
.gateway-mode,
.risk-pill {
  display: flex;
  align-items: center;
}

.page-heading-content {
  justify-content: space-between;
  gap: 20px;
}

.heading-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  border-radius: 8px;
  color: var(--primary-color);
  background: #eaf2ff;
  font-size: 28px;
}

.heading-copy {
  flex: 1 1 auto;
  min-width: 0;
}

.heading-copy h1 {
  margin: 8px 0 0;
  color: var(--text-primary);
  font-size: 25px;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: 0;
}

.gateway-mode {
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

.heading-side {
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.risk-pill {
  max-width: 360px;
  justify-content: flex-end;
  gap: 10px;
  color: #64748b;
  font-size: 13px;
  text-align: right;
}

.heading-actions,
.scene-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.scene-actions {
  display: inline-flex;
  padding: 4px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #ffffff;
}

.scene-button.el-button,
.refresh-button.el-button {
  height: 36px;
  border-radius: 6px;
  font-weight: 700;
}

.scene-button.el-button {
  margin-left: 0;
  border: 0;
  color: #475569;
  background: transparent;
}

.scene-button.el-button:hover,
.scene-button.el-button:focus-visible {
  color: var(--primary-color);
  background: #eff6ff;
}

.refresh-button.el-button {
  padding-inline: 16px;
}

.heading-actions .el-button + .el-button {
  margin-left: 0;
}

@media (max-width: 900px) {
  .page-heading-content,
  .heading-side {
    align-items: flex-start;
    flex-direction: column;
  }

  .risk-pill,
  .heading-actions {
    justify-content: flex-start;
    text-align: left;
  }
}

@media (max-width: 560px) {
  .page-heading :deep(.el-card__body) {
    padding: 18px;
  }

  .heading-icon {
    width: 48px;
    height: 48px;
    font-size: 24px;
  }

  .heading-copy h1 {
    font-size: 21px;
  }

  .scene-actions,
  .refresh-button.el-button {
    width: 100%;
  }

  .scene-button.el-button {
    flex: 1 1 0;
  }
}
</style>
