<script setup>
import { CreditCard, Key, Lock, Postcard, Refresh, Unlock, User, VideoCamera, WarningFilled } from '@element-plus/icons-vue'

defineOptions({
  name: 'LabSafetyPanel',
})

defineProps({
  lab: {
    type: Object,
    default: null,
  },
})

defineEmits(['access', 'camera', 'credential', 'sensor'])
</script>

<template>
  <el-card v-if="lab" class="module-card" shadow="never">
    <template #header>
      <div class="card-header">
        <div>
          <h2>{{ lab.name }}</h2>
          <span>{{ lab.location }} · {{ lab.capacity }}</span>
        </div>
        <el-tag :type="lab.statusType" effect="dark">{{ lab.status }}</el-tag>
      </div>
    </template>

    <div class="lab-detail-grid">
      <article class="status-card">
        <div class="item-heading">
          <span class="status-icon" aria-hidden="true">
            <el-icon><Lock /></el-icon>
          </span>
          <div>
            <h3>门禁控制</h3>
            <p>{{ lab.door.door }} · {{ lab.door.lastEvent }}</p>
          </div>
          <el-tag :type="lab.door.statusType" effect="light">{{ lab.door.status }}</el-tag>
        </div>
        <div class="door-state">
          <strong>{{ lab.door.doorState }}</strong>
          <span>门体状态</span>
        </div>
        <div class="button-row">
          <el-button class="control-button primary-action" size="small" type="primary" @click="$emit('access', '远程开门')">
            <el-icon><Unlock /></el-icon>
            远程开门
          </el-button>
          <el-button class="control-button muted-action" size="small" @click="$emit('access', '锁定')">
            <el-icon><Lock /></el-icon>
            锁定
          </el-button>
        </div>
        <div class="access-tool-grid">
          <el-button class="access-tool-button" size="small" @click="$emit('credential', 'card-enroll')">
            <el-icon><CreditCard /></el-icon>
            卡录入
          </el-button>
          <el-button class="access-tool-button" size="small" @click="$emit('credential', 'face-enroll')">
            <el-icon><User /></el-icon>
            人脸录入
          </el-button>
          <el-button class="access-tool-button" size="small" @click="$emit('credential', 'fingerprint-enroll')">
            <el-icon><Postcard /></el-icon>
            指纹录入
          </el-button>
          <el-button class="access-tool-button" size="small" @click="$emit('credential', 'student-enroll')">
            <el-icon><Key /></el-icon>
            学号录入
          </el-button>
        </div>
      </article>

      <article class="status-card">
        <div class="camera-screen">
          <span class="camera-scan"></span>
          <strong>{{ lab.camera.code }}</strong>
          <small>{{ lab.name }}</small>
        </div>
        <div class="item-heading compact">
          <span class="status-icon" aria-hidden="true">
            <el-icon><VideoCamera /></el-icon>
          </span>
          <div>
            <h3>监控查看</h3>
            <p>{{ lab.camera.name }} · {{ lab.camera.lastFrame }}</p>
          </div>
          <el-tag :type="lab.camera.statusType" effect="dark">{{ lab.camera.status }}</el-tag>
        </div>
        <el-button class="full-button panel-action" size="small" type="primary" @click="$emit('camera')">
          <el-icon><VideoCamera /></el-icon>
          查看监控
        </el-button>
      </article>

      <article class="status-card">
        <div class="item-heading">
          <span class="status-icon warning" aria-hidden="true">
            <el-icon><WarningFilled /></el-icon>
          </span>
          <div>
            <h3>烟雾明火监测</h3>
            <p>温度 {{ lab.sensor.temperature }}</p>
          </div>
          <el-tag :type="lab.sensor.statusType" effect="light">{{ lab.sensor.status }}</el-tag>
        </div>
        <div class="sensor-values">
          <div>
            <span>烟雾浓度</span>
            <strong>{{ lab.sensor.smoke }}ppm</strong>
          </div>
          <div>
            <span>明火识别</span>
            <strong>{{ lab.sensor.flame }}</strong>
          </div>
        </div>
        <div class="sensor-meter" aria-hidden="true">
          <span :style="{ width: `${lab.sensor.smokeLevel}%` }"></span>
        </div>
        <el-button class="full-button panel-action warning-action" size="small" @click="$emit('sensor')">
          <el-icon><Refresh /></el-icon>
          传感器自检
        </el-button>
      </article>
    </div>
  </el-card>
</template>

<style scoped>
.module-card {
  width: 100%;
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
}

.module-card :deep(.el-card__header) {
  min-height: 70px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--border-light);
}

.card-header,
.item-heading {
  display: flex;
  align-items: center;
}

.card-header {
  justify-content: space-between;
  gap: 16px;
}

.card-header h2,
.item-heading h3 {
  margin: 0;
  color: var(--text-primary);
  font-weight: 700;
  letter-spacing: 0;
}

.card-header h2 {
  font-size: 16px;
}

.card-header span,
.item-heading p {
  color: var(--text-secondary);
  font-size: 13px;
}

.lab-detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 280px), 1fr));
  gap: 14px;
}

.status-card {
  min-width: 0;
  width: 100%;
  padding: 16px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: #f8fbff;
}

.item-heading {
  justify-content: space-between;
  gap: 12px;
}

.item-heading.compact {
  margin-top: 14px;
}

.item-heading > div {
  flex: 1 1 auto;
  min-width: 0;
}

.item-heading h3 {
  font-size: 15px;
  line-height: 1.35;
  word-break: keep-all;
}

.item-heading p {
  margin: 6px 0 0;
  line-height: 1.45;
}

.status-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 8px;
  color: var(--primary-color);
  background: #eff6ff;
  font-size: 20px;
}

.status-icon.warning {
  color: #d97706;
  background: #fffbeb;
}

.door-state {
  display: grid;
  min-height: 86px;
  margin: 16px 0;
  place-items: center;
  border: 1px dashed #bfdbfe;
  border-radius: 8px;
  background: #ffffff;
}

.door-state strong {
  color: var(--text-primary);
  font-size: 24px;
}

.door-state span {
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 12px;
}

.button-row {
  display: flex;
  gap: 10px;
}

.access-tool-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.button-row .el-button + .el-button,
.access-tool-grid .el-button + .el-button {
  margin-left: 0;
}

.control-button.el-button,
.access-tool-button.el-button,
.panel-action.el-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 34px;
  border-radius: 7px;
  font-weight: 600;
  letter-spacing: 0;
}

.control-button.el-button {
  flex: 1 1 0;
  min-width: 0;
}

.access-tool-button.el-button {
  width: 100%;
  min-width: 0;
  color: #334155;
  border-color: #dbeafe;
  background: #ffffff;
}

.access-tool-button.el-button:hover,
.access-tool-button.el-button:focus-visible {
  color: var(--primary-color);
  border-color: #93c5fd;
  background: #eff6ff;
}

.primary-action.el-button {
  border-color: transparent;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.16);
}

.muted-action.el-button {
  color: #475569;
  border-color: #dbeafe;
  background: #ffffff;
}

.muted-action.el-button:hover,
.muted-action.el-button:focus-visible {
  color: var(--primary-color);
  border-color: #93c5fd;
  background: #eff6ff;
}

.camera-screen {
  position: relative;
  display: grid;
  min-height: 156px;
  overflow: hidden;
  place-items: center;
  border-radius: 8px;
  color: #e0f2fe;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.72), rgba(15, 23, 42, 0.94)),
    repeating-linear-gradient(0deg, rgba(255, 255, 255, 0.08) 0 1px, transparent 1px 18px);
}

.camera-screen strong,
.camera-screen small {
  position: relative;
  z-index: 1;
}

.camera-screen strong {
  font-size: 18px;
}

.camera-screen small {
  color: rgba(224, 242, 254, 0.76);
  font-size: 12px;
}

.camera-scan {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent, rgba(125, 211, 252, 0.28), transparent);
  animation: scanMove 4s linear infinite;
}

.full-button {
  width: 100%;
  margin-top: 14px;
}

.panel-action.el-button {
  gap: 6px;
  border-color: transparent;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.14);
}

.warning-action.el-button {
  color: #b45309;
  border-color: #fde68a;
  background: #fffbeb;
  box-shadow: none;
}

.warning-action.el-button:hover,
.warning-action.el-button:focus-visible {
  color: #92400e;
  border-color: #fbbf24;
  background: #fef3c7;
}

.sensor-values {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 16px 0;
}

.sensor-values div {
  padding: 12px;
  border-radius: 8px;
  background: #ffffff;
}

.sensor-values span {
  display: block;
  color: var(--text-secondary);
  font-size: 12px;
}

.sensor-values strong {
  display: block;
  margin-top: 8px;
  color: var(--text-primary);
  font-size: 18px;
}

.sensor-meter {
  height: 8px;
  margin-bottom: 14px;
  overflow: hidden;
  border-radius: 999px;
  background: #e2e8f0;
}

.sensor-meter span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #22c55e, #f59e0b);
}

@keyframes scanMove {
  from {
    transform: translateY(-100%);
  }
  to {
    transform: translateY(100%);
  }
}

@media (max-width: 1100px) {
  .lab-detail-grid {
    grid-template-columns: repeat(auto-fit, minmax(min(100%, 260px), 1fr));
  }
}

@media (max-width: 640px) {
  .lab-detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
