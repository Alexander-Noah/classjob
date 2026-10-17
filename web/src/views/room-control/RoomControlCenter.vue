<!--
  Web 管理平台的实验室控制视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  业务状态复用：../../composables/useRoomControl。
-->
<script setup>
import MetricGrid from '../../components/common/MetricGrid.vue'
import DeviceControlPanel from './components/DeviceControlPanel.vue'
import EventRecordPanel from './components/EventRecordPanel.vue'
import LabSelectorPanel from './components/LabSelectorPanel.vue'
import RoomControlHeader from './components/RoomControlHeader.vue'
import { useRoomControl } from '../../composables/useRoomControl'

defineOptions({
  name: 'RoomControlCenter',
})

const {
  applyScenePreset,
  capabilityState,
  categoryFilter,
  categoryOptions,
  deviceCapabilityMetrics,
  filteredRecords,
  gatewayModeInfo,
  handleDeviceCapability,
  keyword,
  labs,
  refreshAll,
  roomFilter,
  roomMetrics,
  roomOptions,
  selectedLabDeviceLoading,
  selectedLabDevices,
  selectedLab,
  selectedRisk,
  selectLab,
} = useRoomControl()
</script>

<template>
  <section class="room-control-page">
    <RoomControlHeader :gateway-mode="gatewayModeInfo" :risk="selectedRisk" @refresh="refreshAll" @scene="applyScenePreset" />

    <MetricGrid :metrics="roomMetrics" />

    <div class="room-workbench">
      <LabSelectorPanel class="room-selector-panel" :labs="labs" :selected-lab-id="selectedLab?.id" @select="selectLab" />

      <div class="room-main-stack">
        <DeviceControlPanel
          :capability-state="capabilityState"
          :devices="selectedLabDevices"
          :lab-name="selectedLab?.name"
          :loading="selectedLabDeviceLoading"
          :metrics="deviceCapabilityMetrics"
          @capability="handleDeviceCapability"
        />
      </div>
    </div>

    <EventRecordPanel
      v-model:category-filter="categoryFilter"
      v-model:keyword="keyword"
      v-model:room-filter="roomFilter"
      :category-options="categoryOptions"
      :records="filteredRecords"
      :room-options="roomOptions"
    />
  </section>
</template>

<style scoped>
.room-control-page {
  display: grid;
  width: 100%;
  min-width: 0;
  gap: 16px;
}

.room-control-page :deep(.metric-grid) {
  gap: 14px;
}

.room-control-page :deep(.metric-card) {
  min-height: 108px;
  border-color: #dbe7f5;
  border-radius: 8px;
  background:
    linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.room-control-page :deep(.metric-card .el-card__body) {
  padding: 16px;
}

.room-workbench {
  display: grid;
  grid-template-columns: minmax(290px, 330px) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
  min-width: 0;
  width: 100%;
}

.room-main-stack {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  width: 100%;
  min-width: 0;
  gap: 16px;
  justify-self: stretch;
}

.room-workbench > *,
.room-main-stack > * {
  min-width: 0;
}

.room-main-stack :deep(.module-card) {
  width: 100%;
}

.room-selector-panel {
  position: sticky;
  top: 14px;
}

@media (max-width: 1100px) {
  .room-workbench {
    grid-template-columns: 1fr;
  }

  .room-selector-panel {
    position: static;
  }
}
</style>
