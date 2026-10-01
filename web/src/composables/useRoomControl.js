import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { iotService } from '../api/services/iot'
import { labResourceService } from '../api/services/basicResources'

const SILENT_IOT_REQUEST = { skipErrorMessage: true }
const cloneData = (data) => JSON.parse(JSON.stringify(data))
const airModeOptions = ['制冷', '制热', '自动', '送风', '除湿']
const accessCredentialActions = {
  'card-enroll': {
    label: '卡片录入',
    action: 'card-enroll',
    credentialType: 'card',
    promptTitle: '卡片录入',
    promptMessage: '请输入卡号、学号或工号，系统会下发到门禁控制器进入录卡流程。',
    inputPlaceholder: '例如 CARD-20260506-001',
    payloadKey: 'cardNo',
  },
  'face-enroll': {
    label: '人脸录入',
    action: 'face-enroll',
    credentialType: 'face',
    promptTitle: '人脸录入',
    promptMessage: '请输入人员姓名、学号或工号，门禁终端收到指令后进行现场人脸采集。',
    inputPlaceholder: '例如 20260001 张明',
    payloadKey: 'personNo',
  },
  'fingerprint-enroll': {
    label: '指纹录入',
    action: 'fingerprint-enroll',
    credentialType: 'fingerprint',
    promptTitle: '指纹录入',
    promptMessage: '请输入人员姓名、学号或工号，门禁终端收到指令后进入指纹采集流程。',
    inputPlaceholder: '例如 20260001 张明',
    payloadKey: 'personNo',
  },
  'student-enroll': {
    label: '学号开通',
    action: 'student-enroll',
    credentialType: 'student',
    promptTitle: '学号录入',
    promptMessage: '请输入学生学号，系统会开通该学生的学生卡和校园码开门权限。',
    inputPlaceholder: '例如 20260001',
    payloadKey: 'studentNo',
    payload: {
      allowStudentCard: true,
      allowCampusCode: true,
      openMethods: 'student-card,campus-code',
    },
  },
}
const deviceStatusOptions = [
  { label: '正常', type: 'success' },
  { label: '借用中', type: 'primary' },
  { label: '维护中', type: 'warning' },
  { label: '故障', type: 'danger' },
  { label: '已盘点', type: 'success' },
  { label: '停用', type: 'info' },
]

const getStatusType = (status) => deviceStatusOptions.find((option) => option.label === status)?.type || 'info'
const getLabDisplayName = (lab) => lab?.name || lab?.labName || lab?.labCode || lab?.code || ''
const getLabHardwareId = (lab) => {
  const labId = lab?.rawId ?? lab?.id
  const numericId = Number(labId)
  return Number.isFinite(numericId) ? numericId : null
}

const getCurrentMinute = () =>
  new Date().toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })

const normalizeLabStatus = (status = '') => {
  if (status.includes('维护')) return { status: '维护中', type: 'warning' }
  if (status.includes('使用')) return { status: '使用中', type: 'warning' }
  if (status.includes('预约')) return { status: '预约中', type: 'primary' }
  if (status.includes('停') || status.includes('关闭')) return { status: '暂停开放', type: 'info' }
  return { status: '运行正常', type: 'success' }
}

const normalizeIotType = (value = '') => String(value || '').replace(/_/g, '-').toLowerCase()
const resolveGatewayMode = (device = {}) => {
  const mode = String(device.gatewayMode || device.gateway_mode || '').trim()
  if (mode) return mode
  const protocol = String(device.protocol || '').toLowerCase()
  const sourceType = String(device.sourceType || device.source_type || '').toLowerCase()
  if (protocol === 'mock' || sourceType === 'mock') return 'mock_gateway'
  if (device.configured || device.gatewayConfigured || device.gatewayId) return 'real_gateway_unconfigured'
  return 'real_gateway_unconfigured'
}

const getGatewayModeCopy = (mode) => {
  if (mode === 'mock_gateway') {
    return {
      label: '模拟网关演示',
      type: 'warning',
      description: '控制命令经后端 MockGatewayAdapter 返回，并写入控制日志；结果不是现场设备回执。',
    }
  }
  return {
    label: '待配置真实网关',
    type: 'info',
    description: '后续补充协议、地址、鉴权和点位配置后即可接入真实网关。',
  }
}

const createRoomLab = (lab, deviceRecords = [], index = 0) => {
  const labName = getLabDisplayName(lab)
  const labId = lab.id ?? lab.rawId
  const currentDevices = deviceRecords
    .filter(
      (device) =>
        String(device.labId || device.lab_id || '') === String(labId) ||
        device.labName === labName ||
        device.lab === labName,
    )
  const accessDevice = currentDevices.find((device) => device.type === 'access-control')
  const cameraDevice = currentDevices.find((device) => device.type === 'camera')
  const sensorDevice = currentDevices.find((device) => device.type === 'sensor')
  const hasIotDevices = currentDevices.length > 0
  const faultCount = currentDevices.filter((device) => device.status === '故障' || device.status === '异常' || device.online === false).length
  const statusInfo = !hasIotDevices
    ? { status: '未接入', type: 'info' }
    : faultCount
      ? { status: '硬件关注', type: 'warning' }
      : normalizeLabStatus(lab.openStatus || lab.status)

  return {
    id: String(lab.id || lab.labCode || lab.code || `lab-${index}`),
    rawId: lab.id,
    name: labName || `实验室 ${index + 1}`,
    location: [lab.building, lab.floor, lab.roomNo || lab.room].filter(Boolean).join(' ') || lab.location || '未填写位置',
    status: statusInfo.status,
    statusType: statusInfo.type,
    capacity: `${lab.capacity || 0} 人`,
    door: {
      door: '前门',
      status: accessDevice ? accessDevice.status : '未接入',
      statusType: accessDevice ? accessDevice.tagType : 'info',
      doorState: accessDevice ? '关闭' : '未配置',
      lastEvent: accessDevice ? `${getCurrentMinute()} 网关状态同步` : '未接入门禁',
    },
    camera: {
      code: cameraDevice?.code || `CAM-${lab.labCode || lab.code || lab.id || index}`,
      name: cameraDevice?.name || `${labName || '实验室'} 摄像头`,
      status: cameraDevice ? cameraDevice.status : '未接入',
      statusType: cameraDevice ? cameraDevice.tagType : 'info',
      lastFrame: cameraDevice ? `${getCurrentMinute()} 网关状态同步` : '未接入摄像头',
    },
    sensor: {
      smoke: sensorDevice?.smoke ?? '-',
      smokeLevel: sensorDevice?.smokeLevel ?? sensorDevice?.smoke ?? '-',
      flame: sensorDevice?.flame || '未接入',
      temperature: sensorDevice?.temperature || '-',
      status: sensorDevice ? sensorDevice.status : '未接入',
      statusType: sensorDevice ? sensorDevice.tagType : 'info',
    },
    lights: [],
    airConditioners: [],
    deviceCount: currentDevices.length,
  }
}

export function useRoomControl() {
  const keyword = ref('')
  const roomFilter = ref('all')
  const categoryFilter = ref('all')
  const deviceKeyword = ref('')
  const deviceLabFilter = ref('all')
  const deviceCategoryFilter = ref('all')
  const deviceStatusFilter = ref('all')
  const selectedLabId = ref('')
  const controlState = reactive({})
  const labs = ref([])
  const iotDevices = ref([])
  const labRecords = ref([])
  const hardwareOverview = ref({})
  const eventRecords = ref([])
  const selectedLabDevices = ref([])
  const selectedLabDeviceLoading = ref(false)
  const capabilityState = reactive({})

  const selectedLab = computed(() => labs.value.find((lab) => lab.id === selectedLabId.value) || labs.value[0])

  const selectedLabControls = computed(() => {
    if (!selectedLab.value) return []

    return selectedLabDevices.value
      .filter((device) => ['air-conditioner', 'light'].includes(device.type))
      .map((device, index) => {
        const isAir = device.type === 'air-conditioner'
        const capabilities = Array.isArray(device.capabilities) ? device.capabilities : []
        const valueCapability = capabilities.find((item) => ['temperature', 'brightness'].includes(item.key)) || {}
        const enabled = Boolean(device.online && device.enabled !== false)

        return {
          id: `iot-control-${device.id || device.code || index}`,
          sourceDeviceId: device.id,
          sourceDeviceCode: device.iotCode || device.code,
          name: device.name || (isAir ? '空调设备' : '灯光设备'),
          enabled,
          min: valueCapability.min ?? (isAir ? 16 : 0),
          max: valueCapability.max ?? (isAir ? 30 : 100),
          unit: valueCapability.unit || (isAir ? '°C' : '%'),
          value: valueCapability.value ?? (isAir ? 24 : enabled ? 80 : 0),
          mode: isAir ? valueCapability.mode || '自动' : '',
          status: enabled ? device.status || '运行中' : '离线',
          statusType: enabled ? getStatusType(device.status) : 'info',
          controlKind: isAir ? 'air' : 'light',
          controlGroup: isAir ? '空调温控' : '智能照明',
        }
      })
  })

  const roomOptions = computed(() => labs.value.map((lab) => ({ label: lab.name, value: lab.name })))
  const labDeviceOptions = computed(() => labs.value.map((lab) => ({ label: lab.name, value: lab.name })))

  const deviceCategoryOptions = computed(() => [...new Set(iotDevices.value.map((device) => device.category).filter(Boolean))])

  const categoryOptions = computed(() => {
    const categories = new Set(eventRecords.value.map((record) => record.category))
    return [...categories].map((category) => ({ label: category, value: category }))
  })

  const filteredRecords = computed(() => {
    const searchText = keyword.value.trim()

    return eventRecords.value.filter((record) => {
      const matchesRoom = roomFilter.value === 'all' || record.room === roomFilter.value
      const matchesCategory = categoryFilter.value === 'all' || record.category === categoryFilter.value
      const matchesKeyword =
        !searchText || Object.values(record).some((value) => String(value).includes(searchText))

      return matchesRoom && matchesCategory && matchesKeyword
    })
  })

  const filteredManagedDevices = computed(() => {
    const searchText = deviceKeyword.value.trim().toLowerCase()

    return iotDevices.value.filter((device) => {
      const matchesKeyword =
        !searchText ||
        [device.code, device.name, device.lab, device.category, device.location, device.owner, device.status, device.health].some((value) =>
          String(value || '').toLowerCase().includes(searchText),
        )
      const matchesLab = deviceLabFilter.value === 'all' || device.lab === deviceLabFilter.value
      const matchesCategory = deviceCategoryFilter.value === 'all' || device.category === deviceCategoryFilter.value
      const matchesStatus = deviceStatusFilter.value === 'all' || device.status === deviceStatusFilter.value

      return matchesKeyword && matchesLab && matchesCategory && matchesStatus
    })
  })

  const alertLabs = computed(() =>
    labs.value.filter(
      (lab) => lab.statusType === 'warning' || lab.door.statusType === 'warning' || lab.camera.statusType === 'warning' || lab.sensor.statusType === 'warning',
    ),
  )

  const roomMetrics = computed(() => {
    const onlineDevices = iotDevices.value.filter((device) => device.online).length
    const deviceTotal = iotDevices.value.length
    const warningEvents = eventRecords.value.filter((record) => ['warning', 'danger'].includes(record.tagType)).length
    const faultDevices = iotDevices.value.filter((device) => device.status === '故障' || device.status === '异常' || !device.online).length
    const modeInfo = gatewayModeInfo.value

    return [
      { label: '接入实验室', value: String(labs.value.length), trend: '按实验室统一管理' },
      { label: '在线设备', value: String(onlineDevices), trend: `物联设备 ${deviceTotal} 个` },
      { label: '异常关注', value: String(alertLabs.value.length + faultDevices), trend: `事件 ${warningEvents} 条，设备 ${faultDevices} 台` },
      { label: '网关模式', value: modeInfo.label, trend: modeInfo.description },
    ]
  })

  const deviceManagementMetrics = computed(() => {
    const total = iotDevices.value.length
    const onlineCount = iotDevices.value.filter((device) => device.online).length
    const faultCount = iotDevices.value.filter((device) => device.status === '故障' || device.status === '异常').length
    const maintenanceCount = iotDevices.value.filter((device) => device.status === '维护中').length

    return {
      total,
      onlineCount,
      faultCount,
      maintenanceCount,
    }
  })

  const controlMetrics = computed(() => {
    const controls = selectedLabControls.value
    const onlineCount = controls.filter((control) => control.enabled).length
    const lightCount = controls.filter((control) => control.controlKind === 'light').length
    const airCount = controls.filter((control) => control.controlKind === 'air').length

    return {
      total: controls.length,
      onlineCount,
      lightCount,
      airCount,
    }
  })

  const deviceCapabilityMetrics = computed(() => {
    const total = selectedLabDevices.value.length
    const onlineCount = selectedLabDevices.value.filter((device) => device.online).length
    const configuredCount = selectedLabDevices.value.filter((device) => device.configured).length
    const categoryCount = new Set(selectedLabDevices.value.map((device) => device.type || device.category).filter(Boolean)).size

    return {
      total,
      onlineCount,
      configuredCount,
      categoryCount,
    }
  })

  const selectedRisk = computed(() => {
    const lab = selectedLab.value
    if (!lab) return { label: '未选择', type: 'info', description: '请选择机房查看态势' }

    if (lab.sensor.statusType === 'warning') {
      return { label: '环境关注', type: 'warning', description: `烟雾浓度 ${lab.sensor.smoke}ppm，建议复核现场` }
    }

    if (lab.door.statusType === 'warning') {
      return { label: '门禁关注', type: 'warning', description: `${lab.door.door}处于${lab.door.doorState}` }
    }

    if (lab.camera.statusType === 'warning') {
      return { label: '视频关注', type: 'warning', description: `${lab.camera.name}状态为${lab.camera.status}` }
    }

    return { label: '运行正常', type: 'success', description: '门禁、视频与环境状态稳定' }
  })

  const gatewayModeInfo = computed(() => {
    const devices = selectedLabDevices.value.length ? selectedLabDevices.value : iotDevices.value
    const hasMockGateway = devices.some((device) => resolveGatewayMode(device) === 'mock_gateway')
    const hasAnyDevice = devices.length > 0
    const copy = getGatewayModeCopy(hasMockGateway ? 'mock_gateway' : 'real_gateway_unconfigured')
    if (!hasAnyDevice) {
      return {
        ...copy,
        label: '待配置真实网关',
        description: '该实验室暂未配置物联设备，可先接入模拟网关演示完整控制闭环。',
      }
    }
    return copy
  })

  const appendEventRecord = (category, event, status = '成功', tagType = 'success') => {
    if (!selectedLab.value) return

    eventRecords.value = [
      {
        time: getCurrentMinute(),
        room: selectedLab.value.name,
        category,
        event,
        status,
        tagType,
      },
      ...eventRecords.value,
    ].slice(0, 30)
  }

  const getCapabilityStateKey = (device, capability) => `${device.code || device.id}::${capability.key}`

  const getDefaultCapabilityValue = (device, capability) => {
    if (capability.value !== undefined && capability.value !== null) return capability.value
    if (capability.type === 'switch') return Boolean(device.online)
    if (capability.type === 'slider') {
      if (capability.key === 'temperature') return 24
      if (capability.key === 'brightness') return 80
      if (capability.key === 'humidity') return 50
      return capability.min ?? 0
    }
    if (capability.type === 'select') return capability.options?.[0] || ''
    return ''
  }

  const inferHardwareControlType = (device = {}) => {
    const type = normalizeIotType(device.type || device.deviceType || '')
    const text = `${device.category || ''}${device.name || ''}${device.iotName || ''}${device.deviceName || ''}${device.specs || ''}`.toLowerCase()

    if (type === 'access-control' || /门禁|access|door/.test(text)) return 'access-control'
    if (type === 'light' || /灯|照明|light|lamp/.test(text)) return 'light'
    if (type === 'air-conditioner' || /空调|温控|新风|除湿|air|hvac/.test(text)) return 'air-conditioner'
    if (type === 'camera' || /摄像|监控|camera|video/.test(text)) return 'camera'
    if (
      ['sensor', 'temperature-sensor', 'humidity-sensor', 'smoke-sensor'].includes(type) ||
      /温湿度|温度|湿度|烟雾|环境|sensor|temperature|humidity|smoke/.test(text)
    ) return 'sensor'
    return ''
  }

  const buildDefaultCapabilities = (device = {}, type) => {
    const enabled = Boolean(device.online ?? device.status !== '故障')

    if (type === 'access-control') {
      return [
        { key: 'open', label: '远程开门', type: 'action', command: 'open', enabled },
        { key: 'lock', label: '锁定门禁', type: 'action', command: 'lock', enabled },
      ]
    }

    if (type === 'light') {
      return [
        { key: 'power', label: '灯光开关', type: 'switch', command: 'set', offCommand: 'off', enabled },
        { key: 'brightness', label: '亮度', type: 'slider', command: 'set', enabled, min: 0, max: 100, unit: '%' },
      ]
    }

    if (type === 'air-conditioner') {
      return [
        { key: 'power', label: '空调开关', type: 'switch', command: 'set', offCommand: 'off', enabled },
        { key: 'temperature', label: '温度', type: 'slider', command: 'set', enabled, min: 16, max: 30, unit: '°C' },
        { key: 'mode', label: '模式', type: 'select', command: 'set', enabled, options: airModeOptions },
      ]
    }

    if (type === 'sensor') {
      return [
        { key: 'temperature', label: '温度', type: 'slider', command: 'read-environment', enabled: false, min: 0, max: 50, unit: '°C' },
        { key: 'humidity', label: '湿度', type: 'slider', command: 'read-environment', enabled: false, min: 0, max: 100, unit: '%' },
        { key: 'read-environment', label: '刷新温湿度', type: 'action', command: 'read-environment', enabled },
      ]
    }

    if (type === 'camera') {
      return [{ key: 'view-camera', label: '查看监控', type: 'media', command: 'view-camera', enabled }]
    }

    return []
  }

  const normalizeCapabilityDevice = (device = {}) => {
    const status = device.status || (device.online ? '正常' : '离线')
    const type = inferHardwareControlType(device)
    const onlineText = String(device.onlineStatus || device.online_status || device.status || '').toLowerCase()
    const online = device.online === undefined
      ? ['online', 'running', '在线', '正常'].includes(onlineText)
      : Boolean(device.online)
    const capabilities = Array.isArray(device.capabilities) && device.capabilities.length
      ? device.capabilities
      : buildDefaultCapabilities(device, type)

    return {
      ...device,
      id: device.id || device.code,
      code: device.iotCode || device.code || device.deviceCode || '',
      iotCode: device.iotCode || device.code || device.deviceCode || '',
      name: device.iotName || device.name || device.deviceName || '未命名设备',
      type,
      category: device.category || device.deviceType || device.type || '未分类设备',
      labId: device.labId ?? device.lab_id ?? '',
      labName: device.labName || device.lab || '',
      location: device.location || '',
      status,
      online,
      configured: Boolean(device.configured ?? device.gatewayConfigured ?? device.gatewayId),
      enabled: device.enabled !== false,
      gatewayMode: resolveGatewayMode(device),
      sourceType: device.sourceType || device.source_type || '',
      protocol: device.protocol || '',
      tagType: online ? getStatusType(status) : 'info',
      streamUrl: device.streamUrl || device.stream_url || '',
      snapshotUrl: device.snapshotUrl || device.snapshot_url || '',
      temperature: device.temperature || device.temperatureValue || '',
      smoke: device.smoke || device.smokeValue || '',
      smokeLevel: device.smokeLevel || device.smoke_level || device.smoke || '',
      flame: device.flame || device.flameStatus || '',
      capabilities,
    }
  }

  const resetCapabilityState = () => {
    Object.keys(capabilityState).forEach((key) => {
      delete capabilityState[key]
    })

    selectedLabDevices.value.forEach((device) => {
      ;(device.capabilities || []).forEach((capability) => {
        const key = getCapabilityStateKey(device, capability)
        const defaultValue = getDefaultCapabilityValue(device, capability)
        capabilityState[key] = {
          enabled: capability.type === 'switch' ? defaultValue : Boolean(device.online),
          value: capability.type === 'switch' ? '' : defaultValue,
        }
      })
    })
  }

  const selectLab = (lab) => {
    selectedLabId.value = lab.id
    roomFilter.value = lab.name
    loadSelectedLabDevices()
    ElMessage.success(`已进入${lab.name}`)
  }

  const resetControlState = () => {
    Object.keys(controlState).forEach((key) => {
      delete controlState[key]
    })

    selectedLabControls.value.forEach((control) => {
      controlState[control.id] = {
        enabled: control.enabled,
        value: control.value,
        mode: control.mode || '',
      }
    })
  }

  const getControlSourceDevice = (control) =>
    selectedLabDevices.value.find(
      (device) =>
        String(device.id) === String(control.sourceDeviceId) ||
        String(device.code || device.iotCode || '') === String(control.sourceDeviceCode || control.code || ''),
    ) || {
      code: control.sourceDeviceCode || control.code,
      name: control.name,
    }

  const normalizeIotDeviceList = (payload = {}) => {
    if (Array.isArray(payload?.devices)) return payload.devices
    if (Array.isArray(payload?.records)) return payload.records
    if (Array.isArray(payload?.data?.devices)) return payload.data.devices
    if (Array.isArray(payload?.data?.records)) return payload.data.records
    if (Array.isArray(payload)) return payload
    return []
  }

  const normalizeIotCommandLogList = (payload = {}) => {
    if (Array.isArray(payload?.records)) return payload.records
    if (Array.isArray(payload?.logs)) return payload.logs
    if (Array.isArray(payload?.data?.records)) return payload.data.records
    if (Array.isArray(payload?.data?.logs)) return payload.data.logs
    if (Array.isArray(payload)) return payload
    return []
  }

  const normalizeIotCommandLog = (record = {}) => {
    const resultStatus = record.resultStatus || record.result_status || record.status || 'pending'
    const tagTypeMap = {
      success: 'success',
      mock_success: 'warning',
      pending: 'info',
      failed: 'danger',
      failure: 'danger',
    }

    return {
      time: record.executedAt || record.executed_at || record.createdAt || record.created_at || record.time || '-',
      room: record.labName || record.lab_name || record.room || '-',
      category: record.commandKey || record.command_key || record.action || '物联控制',
      event: record.responseSummary || record.response_summary || record.action || record.commandKey || '控制命令',
      status: resultStatus,
      tagType: tagTypeMap[resultStatus] || 'info',
    }
  }

  const findSelectedIotDevice = (types = []) => selectedLabDevices.value.find((device) => types.includes(device.type))

  const buildIotControlPayload = (device, action, payload = {}) => ({
    iotDeviceId: Number.isFinite(Number(device?.id)) ? Number(device.id) : undefined,
    iotCode: device?.iotCode || device?.code,
    labId: getLabHardwareId(selectedLab.value),
    commandKey: action,
    action,
    payload,
    gatewayMode: device?.gatewayMode || resolveGatewayMode(device),
    sourceType: resolveGatewayMode(device) === 'mock_gateway' ? 'mock' : 'gateway',
  })

  const applyHardwareStatusToLab = (lab, hardwareStatus = {}) => {
    const hardwareDevices = normalizeIotDeviceList(hardwareStatus).map(normalizeCapabilityDevice)
    const accessStatus = hardwareDevices.find((device) => device.type === 'access-control')
    const cameraStatus = hardwareDevices.find((device) => device.type === 'camera')

    if (accessStatus) {
      lab.door.status = accessStatus.status || (accessStatus.online ? '在线' : '离线')
      lab.door.statusType = accessStatus.online ? 'success' : 'danger'
      lab.door.doorState = accessStatus.online ? '关闭' : '不可用'
      lab.door.lastEvent = `${getCurrentMinute()} 网关状态同步`
    }

    if (cameraStatus) {
      lab.camera.status = cameraStatus.status || (cameraStatus.online ? '在线' : '离线')
      lab.camera.statusType = cameraStatus.online ? 'success' : 'danger'
      lab.camera.lastFrame = `${getCurrentMinute()} 网关状态同步`
    }

    if (accessStatus?.online === false || cameraStatus?.online === false) {
      lab.status = '硬件关注'
      lab.statusType = 'warning'
    }
  }

  const refreshHardwareStatusForLab = async (lab) => {
    const labId = getLabHardwareId(lab)
    if (!labId) return false

    const hardwareStatus = await iotService.listLabIotDevices(labId, SILENT_IOT_REQUEST)
    applyHardwareStatusToLab(lab, hardwareStatus)
    return true
  }

  const loadSelectedLabDevices = async ({ notifyOnError = false } = {}) => {
    const lab = selectedLab.value
    const labId = getLabHardwareId(lab)

    if (!lab) {
      selectedLabDevices.value = []
      resetCapabilityState()
      return
    }

    selectedLabDeviceLoading.value = true
    try {
      const result = labId ? await iotService.listLabIotDevices(labId, SILENT_IOT_REQUEST) : { devices: [] }
      const devices = normalizeIotDeviceList(result)
      const hardwareDevices = devices
        .map(normalizeCapabilityDevice)
        .filter((device) => ['access-control', 'light', 'air-conditioner', 'camera', 'sensor'].includes(device.type))
      selectedLabDevices.value = hardwareDevices
    } catch (error) {
      selectedLabDevices.value = []
      if (notifyOnError) {
        ElMessage.warning('当前实验室暂未接入可访问的物联设备，请先完成物联网关配置')
      }
    } finally {
      selectedLabDeviceLoading.value = false
      resetCapabilityState()
    }
  }

  const loadRoomResources = async () => {
    try {
      const [loadedLabs, loadedIotDevices, loadedCommandLogs] = await Promise.all([
        labResourceService.list(),
        iotService.listIotDevices({}, SILENT_IOT_REQUEST).catch(() => ({})),
        iotService.listIotCommandLogs({ page: 1, pageSize: 30 }, SILENT_IOT_REQUEST).catch(() => ({})),
      ])
      labRecords.value = Array.isArray(loadedLabs) && loadedLabs.length ? loadedLabs : []
      const iotRows = normalizeIotDeviceList(loadedIotDevices).map(normalizeCapabilityDevice)
      iotDevices.value = iotRows
      eventRecords.value = normalizeIotCommandLogList(loadedCommandLogs).map(normalizeIotCommandLog)
      hardwareOverview.value = {
        ...(!Array.isArray(loadedIotDevices) ? loadedIotDevices || {} : {}),
        total: iotRows.length,
        configuredCount: iotRows.filter((device) => device.configured).length,
        enabled: iotRows.length > 0,
      }

      if (labRecords.value.length) {
        labs.value = labRecords.value.map((lab, index) => createRoomLab(lab, iotRows, index))
        if (!labs.value.some((lab) => lab.id === selectedLabId.value)) {
          selectedLabId.value = labs.value[0]?.id || ''
        }
      }

      appendEventRecord('数据同步', '同步实验室与物联设备')
      await loadSelectedLabDevices()
    } catch (error) {
      ElMessage.warning('实验室资源加载失败，已保留当前物联控制页面')
      await loadSelectedLabDevices()
    }
  }

  const refreshAll = async () => {
    await loadRoomResources()
    const results = await Promise.allSettled(labs.value.map((lab) => refreshHardwareStatusForLab(lab)))
    const syncedCount = results.filter((result) => result.status === 'fulfilled' && result.value).length

    if (syncedCount) {
      appendEventRecord('网关状态', `已同步 ${syncedCount} 间实验室网关状态`)
      ElMessage.success(`已同步 ${syncedCount} 间实验室网关状态`)
    } else {
      appendEventRecord('网关状态', '待配置真实网关', '未配置', 'warning')
      ElMessage.warning('物联设备已刷新，当前为模拟网关演示或待配置真实网关')
    }
  }

  const handleAccessCommand = async (command) => {
    const lab = selectedLab.value

    if (!lab) return

    const labId = getLabHardwareId(lab)
    if (!labId) {
      ElMessage.error(`${lab.name}缺少后端实验室编号，无法发送门禁网关指令`)
      return
    }

    const accessDevice = findSelectedIotDevice(['access-control'])
    if (!accessDevice) {
      ElMessage.warning(`${lab.name}暂未接入门禁物联设备`)
      return
    }

    const action = command === '远程开门' ? 'open' : 'lock'

    try {
      const result = await iotService.controlIotDevice(buildIotControlPayload(accessDevice, action, { door: lab.door.door }))
      if (result.success === false) {
        throw new Error(result.response || result.status || '网关返回执行失败')
      }

      if (action === 'open') {
        lab.door.doorState = '短时开启'
        lab.door.status = '待确认'
        lab.door.statusType = 'warning'
        lab.door.lastEvent = `${getCurrentMinute()} 网关开门`
        appendEventRecord('门禁控制', `${lab.door.door}网关开门`, '已下发')
      } else {
        lab.door.doorState = '关闭'
        lab.door.status = '已锁定'
        lab.door.statusType = 'success'
        lab.door.lastEvent = `${getCurrentMinute()} 网关锁定`
        appendEventRecord('门禁控制', `${lab.door.door}网关锁定`, '成功')
      }

      ElMessage.success(`${lab.name}${lab.door.door}${command}网关指令已执行`)
    } catch (error) {
      appendEventRecord('门禁控制', `${lab.door.door}${command}失败`, '失败', 'danger')
      ElMessage.error(error?.response?.data?.message || error?.message || '门禁网关指令执行失败')
    }
  }

  const handleAccessCredential = async (action) => {
    const lab = selectedLab.value
    const config = accessCredentialActions[action]

    if (!lab || !config) return

    const labId = getLabHardwareId(lab)
    if (!labId) {
      ElMessage.error(`${lab.name}缺少后端实验室编号，无法下发门禁功能指令`)
      return
    }

    const accessDevice = findSelectedIotDevice(['access-control'])
    if (!accessDevice) {
      ElMessage.warning(`${lab.name}暂未接入门禁物联设备`)
      return
    }

    let credentialValue = ''

    if (config.promptTitle) {
      try {
        const result = await ElMessageBox.prompt(config.promptMessage, config.promptTitle, {
          confirmButtonText: '下发录入',
          cancelButtonText: '取消',
          inputPlaceholder: config.inputPlaceholder,
          inputPattern: /.+/,
          inputErrorMessage: '请输入有效内容',
        })
        credentialValue = String(result.value || '').trim()
      } catch (error) {
        if (error === 'cancel' || error === 'close') return
        ElMessage.error('门禁录入信息读取失败')
        return
      }
    }

    const payload = {
      door: lab.door.door,
      credentialType: config.credentialType,
      credentialValue,
      operator: '实验室管理员',
      source: 'lab-admin-room-control',
      ...(config.payload || {}),
    }

    if (config.payloadKey && credentialValue) {
      payload[config.payloadKey] = credentialValue
    }

    try {
      const result = await iotService.controlIotDevice(buildIotControlPayload(accessDevice, config.action, payload))
      if (result.success === false) {
        throw new Error(result.response || result.status || '网关返回执行失败')
      }

      lab.door.status = config.credentialType === 'lock' ? result.status || '已同步' : '录入中'
      lab.door.statusType = 'success'
      lab.door.lastEvent = `${getCurrentMinute()} ${config.label}`
      const methodText = action === 'student-enroll' ? '，学生卡/校园码已授权' : ''
      appendEventRecord('门禁控制', `${lab.door.door}${config.label}${credentialValue ? `：${credentialValue}` : ''}${methodText}`, '已下发')
      ElMessage.success(`${lab.name}${config.label}指令已下发`)
    } catch (error) {
      appendEventRecord('门禁控制', `${lab.door.door}${config.label}失败`, '失败', 'danger')
      ElMessage.error(error?.response?.data?.message || error?.message || `${config.label}失败`)
    }
  }

  const handleCameraView = async () => {
    const lab = selectedLab.value
    if (!lab) return

    const labId = getLabHardwareId(lab)
    if (!labId) {
      ElMessage.error(`${lab.name}缺少后端实验室编号，无法读取摄像头`)
      return
    }

    try {
      const camera = findSelectedIotDevice(['camera'])
      if (!camera) {
        ElMessage.warning(`${lab.name}暂未接入摄像头物联设备`)
        return
      }
      const streamUrl = camera.streamUrl || camera.hlsUrl || camera.webrtcUrl || camera.flvUrl || camera.snapshotProxyUrl
      const resolvedUrl = iotService.toBackendUrl(streamUrl)

      lab.camera.code = camera.code || camera.iotCode || lab.camera.code
      lab.camera.name = camera.name || camera.iotName || lab.camera.name
      lab.camera.status = '已连接'
      lab.camera.statusType = 'success'
      lab.camera.lastFrame = `${getCurrentMinute()} 硬件连接`
      appendEventRecord('监控查看', `读取${lab.camera.name}视频源`)

      if (resolvedUrl) {
        window.open(resolvedUrl, '_blank', 'noopener')
        ElMessage.success(`${lab.camera.name}视频源已打开`)
      } else {
        ElMessage.warning(`${lab.camera.name}已配置，但未提供浏览器可打开的视频流或抓拍地址`)
      }
    } catch (error) {
      lab.camera.status = '连接失败'
      lab.camera.statusType = 'danger'
      appendEventRecord('监控查看', `${lab.camera.name}连接失败`, '失败', 'danger')
      ElMessage.error(error?.response?.data?.message || error?.message || '摄像头连接失败')
    }
  }

  const handleSensorCheck = async () => {
    const lab = selectedLab.value

    if (!lab) return

    try {
      const synced = await refreshHardwareStatusForLab(lab)
      if (!synced) {
        throw new Error('实验室缺少后端编号')
      }
      appendEventRecord('环境监测', `${lab.name}网关状态刷新`)
      ElMessage.success(`${lab.name}网关状态已刷新`)
    } catch (error) {
      appendEventRecord('环境监测', `${lab.name}网关状态读取失败`, '失败', 'danger')
      ElMessage.error(error?.response?.data?.message || error?.message || '网关状态读取失败')
    }
  }

  const updateSourceControl = (control) => {
    const state = controlState[control.id]
    const sourceControl = [
      ...(selectedLab.value?.lights || []),
      ...(selectedLab.value?.airConditioners || []),
    ].find((device) => device.id === control.id)

    if (!sourceControl || !state) return

    sourceControl.enabled = state.enabled
    sourceControl.value = state.value
    sourceControl.mode = state.mode || sourceControl.mode
    sourceControl.status = state.enabled ? state.mode || '运行中' : '关闭'
    sourceControl.statusType = state.enabled ? 'success' : 'info'
  }

  const handleControlChange = async (control) => {
    const state = controlState[control.id]

    if (!state) return

    const sourceDevice = getControlSourceDevice(control)
    if (!sourceDevice?.code) {
      resetControlState()
      ElMessage.error(`${control.name}未绑定硬件设备编号，无法下发控制指令`)
      return
    }

    const powerText = state.enabled ? '开启' : '关闭'
    const modeText = state.mode ? `，模式 ${state.mode}` : ''
    const action = state.enabled ? 'set' : 'off'

    try {
      const result = await iotService.controlIotDevice(buildIotControlPayload(sourceDevice, action, {
        enabled: state.enabled,
        value: state.value,
        unit: control.unit,
        mode: state.mode || '',
        controlKind: control.controlKind,
      }))

      if (result.success === false) {
        throw new Error(result.response || result.status || '网关返回执行失败')
      }

      updateSourceControl(control)
      appendEventRecord(control.controlGroup, `${control.name}${powerText}，参数 ${state.value}${control.unit}${modeText}`)
      ElMessage.success(`${control.name}网关指令已执行`)
    } catch (error) {
      resetControlState()
      appendEventRecord(control.controlGroup, `${control.name}${powerText}失败`, '失败', 'danger')
      ElMessage.error(error?.response?.data?.message || error?.message || `${control.name}硬件控制失败`)
    }
  }

  const handleDeviceMediaCapability = (device, capability) => {
    const streamUrl = device.streamUrl || device.hlsUrl || device.webrtcUrl || device.flvUrl || device.snapshotProxyUrl
    const resolvedUrl = iotService.toBackendUrl(streamUrl)

    if (resolvedUrl) {
      window.open(resolvedUrl, '_blank', 'noopener')
      appendEventRecord('设备控制', `${device.name}${capability.label}`)
      ElMessage.success(`${device.name}${capability.label}已打开`)
      return
    }

    ElMessage.warning(`${device.name}未配置可打开的视频流或抓拍地址`)
  }

  const handleDeviceCapability = async (device, capability) => {
    if (!device || !capability) return

    if (!capability.enabled) {
      ElMessage.warning(`${device.name}${capability.label}尚未配置网关点位`)
      return
    }

    if (capability.type === 'media') {
      handleDeviceMediaCapability(device, capability)
      return
    }

    if (capability.type === 'form') {
      await handleAccessCredential(capability.command || capability.key)
      await loadSelectedLabDevices()
      return
    }

    if (device.type === 'sensor' && ['status', 'read-environment'].includes(capability.key)) {
      await handleSensorCheck()
      await loadSelectedLabDevices()
      return
    }

    const lab = selectedLab.value
    const labId = getLabHardwareId(lab)
    const state = capabilityState[getCapabilityStateKey(device, capability)] || {}
    const isAccessDevice = device.type === 'access-control'
    const action =
      capability.type === 'switch'
        ? state.enabled
          ? capability.command || capability.key
          : capability.offCommand || 'off'
        : capability.command || capability.key

    const payload = {
      deviceCode: device.code,
      labId,
      capabilityKey: capability.key,
      enabled: capability.type === 'switch' ? state.enabled : undefined,
      value: capability.type === 'slider' || capability.type === 'select' ? state.value : undefined,
      unit: capability.unit || '',
      mode: capability.key === 'mode' ? state.value : undefined,
    }

    try {
      const result = await iotService.controlIotDevice(buildIotControlPayload(device, action, {
        ...payload,
        door: isAccessDevice && labId ? lab?.door?.door || '前门' : undefined,
      }))

      if (result.success === false) {
        throw new Error(result.response || result.status || '网关返回执行失败')
      }

      appendEventRecord('设备控制', `${device.name}${capability.label}`, '成功')
      ElMessage.success(`${device.name}${capability.label}指令已执行`)

      if (capability.key === 'status') {
        await loadSelectedLabDevices()
      }
    } catch (error) {
      appendEventRecord('设备控制', `${device.name}${capability.label}失败`, '失败', 'danger')
      ElMessage.error(error?.response?.data?.message || error?.message || `${device.name}${capability.label}失败`)
      await loadSelectedLabDevices()
    }
  }

  const updateManagedDevice = async () => {
    ElMessage.warning('物联感知与控制页面不再维护设备资产台账，请到设备资产页面处理资产信息')
  }

  const handleManagedDeviceStatus = (device, status) => {
    updateManagedDevice(device, { status })
  }

  const handleManagedDeviceOnline = (device, online) => {
    updateManagedDevice(device, { online })
  }

  const inventoryManagedDevice = (device) => {
    updateManagedDevice(device, { inventory: true })
  }

  const selectManagedDeviceLab = (device) => {
    const targetLab = labs.value.find((lab) => lab.name === device.lab || String(lab.rawId) === String(device.labId))
    if (targetLab) {
      selectLab(targetLab)
    }
  }

  const applyScenePreset = async (preset) => {
    const lab = selectedLab.value

    if (!lab) return

    const presetMap = {
      class: {
        label: '授课模式',
        lightValue: 88,
        airValue: 24,
        airMode: '制冷',
      },
      energy: {
        label: '节能模式',
        lightValue: 45,
        airValue: 26,
        airMode: '自动',
      },
      security: {
        label: '安防模式',
        lightValue: 20,
        airValue: 25,
        airMode: '自动',
      },
    }
    const nextPreset = presetMap[preset]

    if (!nextPreset) return
    if (!selectedLabControls.value.length) {
      ElMessage.warning(`${lab.name}暂未接入可联动的物联设备`)
      return
    }

    const previousControlState = cloneData(controlState)

    selectedLabControls.value.forEach((control) => {
      const state = controlState[control.id]

      if (!state) return

      state.enabled = true
      state.value = control.controlKind === 'light' ? nextPreset.lightValue : nextPreset.airValue
      state.mode = control.controlKind === 'air' ? nextPreset.airMode : state.mode
    })

    try {
      const hardwareCommands = selectedLabControls.value.map((control) => {
        const state = controlState[control.id]
        const sourceDevice = getControlSourceDevice(control)

        if (!sourceDevice?.code || !state) {
          throw new Error(`${control.name}未绑定硬件设备编号`)
        }

        return iotService.controlIotDevice(buildIotControlPayload(sourceDevice, 'set', {
          enabled: true,
          value: state.value,
          unit: control.unit,
          mode: state.mode || '',
          controlKind: control.controlKind,
          scene: nextPreset.label,
        }))
      })

      const results = await Promise.all(hardwareCommands)
      const failedResult = results.find((result) => result.success === false)
      if (failedResult) {
        throw new Error(failedResult.response || failedResult.status || '网关返回执行失败')
      }

      if (preset === 'security') {
        const accessDevice = findSelectedIotDevice(['access-control'])
        if (accessDevice) {
          await iotService.controlIotDevice(buildIotControlPayload(accessDevice, 'lock', { scene: nextPreset.label }))
        }
      }

      selectedLabControls.value.forEach(updateSourceControl)
    } catch (error) {
      Object.keys(controlState).forEach((key) => {
        delete controlState[key]
      })
      Object.assign(controlState, previousControlState)
      appendEventRecord('场景联动', `切换为${nextPreset.label}失败`, '失败', 'danger')
      ElMessage.error(error?.response?.data?.message || error?.message || `${nextPreset.label}硬件联动失败`)
      return
    }

    lab.door.doorState = '关闭'
    lab.door.status = preset === 'security' ? '已布防' : '已锁定'
    lab.door.statusType = preset === 'security' ? 'primary' : 'success'
    lab.door.lastEvent = `${getCurrentMinute()} ${nextPreset.label}`

    appendEventRecord('场景联动', `切换为${nextPreset.label}`)
    ElMessage.success(`${lab.name}已切换为${nextPreset.label}`)
  }

  const getControlLabel = (control) => (control.controlKind === 'air' ? '目标温度' : '亮度')
  const getControlValue = (control) => `${controlState[control.id]?.value ?? control.value}${control.unit}`

  watch([selectedLabId, selectedLabControls], resetControlState, { immediate: true })
  onMounted(loadRoomResources)

  return {
    airModeOptions,
    applyScenePreset,
    capabilityState,
    categoryFilter,
    categoryOptions,
    deviceCapabilityMetrics,
    controlMetrics,
    controlState,
    deviceCategoryFilter,
    deviceCategoryOptions,
    deviceKeyword,
    deviceLabFilter,
    deviceManagementMetrics,
    deviceStatusFilter,
    deviceStatusOptions,
    filteredRecords,
    filteredManagedDevices,
    gatewayModeInfo,
    getControlLabel,
    getControlValue,
    handleAccessCommand,
    handleAccessCredential,
    handleCameraView,
    handleControlChange,
    handleDeviceCapability,
    handleManagedDeviceOnline,
    handleManagedDeviceStatus,
    handleSensorCheck,
    inventoryManagedDevice,
    keyword,
    labDeviceOptions,
    labs,
    refreshAll,
    roomFilter,
    roomMetrics,
    roomOptions,
    selectedLabDeviceLoading,
    selectedLabDevices,
    selectedLab,
    selectedLabControls,
    selectedRisk,
    selectManagedDeviceLab,
    selectLab,
  }
}
