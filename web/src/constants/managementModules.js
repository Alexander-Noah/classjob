/*
 * managementModules共享常量与映射，修改取值时同步检查使用方。
 * 主要导出：LAB_MANAGEMENT_MODULE、DEVICE_MANAGEMENT_MODULE、RESERVATION_MANAGEMENT_MODULE、REPAIR_MANAGEMENT_MODULE、USAGE_RECORDS_MODULE 等。
 */
  import { API_ENDPOINTS, getBusinessModuleEndpoints } from '../api/endpoints'

  export const LAB_MANAGEMENT_MODULE = {
    badge: '空间资源',
    recordName: '实验室',
    primaryKey: 'name',
    tableTitle: '实验室台账',
    actions: [
      { label: '新增实验室', type: 'primary', command: 'create' },
      { label: '导出清单', type: 'default', command: 'export' },
    ],
    statusOptions: [
      { label: '开放', type: 'success' },
      { label: '预约中', type: 'primary' },
      { label: '使用中', type: 'warning' },
      { label: '维护中', type: 'warning' },
      { label: '停用', type: 'info' },
    ],
    defaultRecord: {
      name: '新建实验室',
      location: '待分配',
      capacity: '48 人',
      manager: '待指派',
      status: '预约中',
      tagType: 'primary',
    },
    metrics: [
      { label: '实验室总数', value: '18', trend: '开放 15 间' },
      { label: '总工位', value: '1,024', trend: '可用 936 个' },
      { label: '运行状态', value: '3 类', trend: '开放、预约、维护' },
    ],
    featureCards: [
      { title: '基础信息登记', tag: '台账', type: 'primary', description: '维护实验室名称、编号、楼栋楼层、容量、类型、管理员和开放状态。' },
      { title: '状态维护', tag: '状态', type: 'success', description: '支持开放、使用中、维护中、已预约、暂停开放等状态流转。' },
      { title: '实验室详情', tag: '详情', type: 'warning', description: '集中查看设备列表、软件环境、预约记录、故障记录和负责人信息。' },
    ],
    tableColumns: [
      { prop: 'name', label: '实验室名称', minWidth: 150 },
      { prop: 'location', label: '位置', minWidth: 120 },
      { prop: 'capacity', label: '容量', minWidth: 90 },
      { prop: 'manager', label: '负责人', minWidth: 100 },
      { prop: 'status', label: '状态', minWidth: 90, type: 'tag' },
    ],
    records: [
      { name: 'A101 云计算实验室', location: '信息楼 A 区', capacity: '72 人', manager: '张明', status: '开放', tagType: 'success' },
      { name: 'B203 网络安全实验室', location: '实训楼 B 区', capacity: '56 人', manager: '李婷', status: '预约中', tagType: 'primary' },
      { name: 'C305 人工智能实验室', location: '创新楼 C 区', capacity: '64 人', manager: '王磊', status: '维护中', tagType: 'warning' },
    ],
    timeline: [
      { title: '核验实验室容量与位置', date: '本周', type: 'primary' },
      { title: '同步开放时段与负责人', date: '本周', type: 'success' },
      { title: '接入门禁与能耗数据', date: '下周', type: 'info' },
    ],
  }

  export const DEVICE_MANAGEMENT_MODULE = {
    badge: '资产设备',
    recordName: '设备',
    primaryKey: 'code',
    tableTitle: '设备清单',
    actions: [
      { label: '新增设备', type: 'primary', command: 'create' },
      { label: '批量盘点', type: 'default', command: 'batchStatus', targetStatus: '已盘点' },
    ],
    statusOptions: [
      { label: '正常', type: 'success' },
      { label: '借用中', type: 'primary' },
      { label: '维护中', type: 'warning' },
      { label: '故障', type: 'danger' },
      { label: '已盘点', type: 'success' },
    ],
    defaultRecord: {
      code: 'DEV-NEW-001',
      name: '新建设备',
      lab: '待分配实验室',
      owner: '待指派',
      status: '正常',
      tagType: 'success',
    },
    metrics: [
      { label: '设备总数', value: '642', trend: '在线 598 台' },
      { label: '故障设备', value: '44', trend: '待处理 9 台' },
      { label: '本月新增', value: '24', trend: '完成验收 18 台' },
    ],
    featureCards: [
      { title: '资产台账', tag: '登记', type: 'primary', description: '登记设备编号、名称、类型、所属实验室、品牌型号、负责人和备注。' },
      { title: '生命周期状态', tag: '状态', type: 'success', description: '覆盖正常、故障、维修中、报废、闲置和借用中的设备状态。' },
      { title: '二维码报修', tag: '扫码', type: 'warning', description: '每台设备可生成唯一二维码，扫码查看详情或提交故障报修。' },
    ],
    tableColumns: [
      { prop: 'code', label: '资产编号', minWidth: 130 },
      { prop: 'name', label: '设备名称', minWidth: 150 },
      { prop: 'lab', label: '所属实验室', minWidth: 130 },
      { prop: 'owner', label: '责任人', minWidth: 100 },
      { prop: 'status', label: '状态', minWidth: 90, type: 'tag' },
    ],
    records: [
      { code: 'PC-A101-018', name: '图形工作站', lab: 'A101 云计算实验室', owner: '张明', status: '正常', tagType: 'success' },
      { code: 'NET-B203-006', name: '核心交换机', lab: 'B203 网络安全实验室', owner: '李婷', status: '维护中', tagType: 'warning' },
      { code: 'AI-C305-012', name: 'GPU 训练服务器', lab: 'C305 人工智能实验室', owner: '王磊', status: '借用中', tagType: 'primary' },
    ],
    timeline: [
      { title: '完成资产编号校验', date: '今日', type: 'success' },
      { title: '生成设备巡检计划', date: '明日', type: 'primary' },
      { title: '同步维修记录归档', date: '本周', type: 'info' },
    ],
  }

  export const RESERVATION_MANAGEMENT_MODULE = {
    badge: '预约调度',
    recordName: '预约',
    primaryKey: 'applicant',
    tableTitle: '预约申请',
    actions: [
      { label: '新建预约', type: 'primary', command: 'create' },
      { label: '冲突检测', type: 'default', command: 'detectConflicts' },
    ],
    statusOptions: [
      { label: '待审核', type: 'warning' },
      { label: '已通过', type: 'success' },
      { label: '冲突', type: 'danger' },
      { label: '已取消', type: 'info' },
    ],
    defaultRecord: {
      applicant: '新增申请人',
      lab: 'A101 云计算实验室',
      time: '04-23 16:00',
      scene: '自主',
      status: '待审核',
      tagType: 'warning',
    },
    metrics: [
      { label: '今日预约', value: '36', trend: '已审核 29 条' },
      { label: '待审核', value: '7', trend: '课程 4 条' },
      { label: '冲突预警', value: '3', trend: '需人工确认' },
    ],
    featureCards: [
      { title: '预约申请', tag: '申请', type: 'primary', description: '填写申请人、使用时间、使用目的、人数、实验室和所需环境。' },
      { title: '审批与驳回', tag: '审批', type: 'success', description: '管理员可通过或驳回预约，驳回时记录原因并通知申请人。' },
      { title: '冲突检测', tag: '校验', type: 'danger', description: '同一实验室同一时间不可重复预约，系统自动提示时间冲突。' },
    ],
    tableColumns: [
      { prop: 'applicant', label: '申请人', minWidth: 100 },
      { prop: 'lab', label: '预约实验室', minWidth: 150 },
      { prop: 'time', label: '预约时间', minWidth: 150 },
      { prop: 'scene', label: '类型', minWidth: 90 },
      { prop: 'status', label: '状态', minWidth: 90, type: 'tag' },
    ],
    records: [
      { applicant: '软件工程 2 班', lab: 'A101 云计算实验室', time: '04-23 08:00', scene: '课程', status: '已通过', tagType: 'success' },
      { applicant: '李同学', lab: 'C305 人工智能实验室', time: '04-23 14:00', scene: '自主', status: '待审核', tagType: 'warning' },
      { applicant: '数据科学 1 班', lab: 'B203 网络安全实验室', time: '04-24 10:00', scene: '课程', status: '冲突', tagType: 'danger' },
    ],
    timeline: [
      { title: '自动校验时间冲突', date: '实时', type: 'primary' },
      { title: '推送预约审核提醒', date: '今日', type: 'success' },
      { title: '生成周课表视图', date: '本周', type: 'info' },
    ],
  }

  export const REPAIR_MANAGEMENT_MODULE = {
    badge: '运维工单',
    recordName: '报修工单',
    primaryKey: 'ticket',
    tableTitle: '报修工单',
    actions: [
      { label: '登记报修', type: 'primary', command: 'create' },
      { label: '派单处理', type: 'default', command: 'batchStatus', targetStatus: '处理中' },
    ],
    statusOptions: [
      { label: '待派单', type: 'danger' },
      { label: '处理中', type: 'warning' },
      { label: '已完成', type: 'success' },
      { label: '已关闭', type: 'info' },
    ],
    defaultRecord: {
      ticket: 'R20260423099',
      device: '待确认设备',
      lab: '待确认实验室',
      owner: '待分配',
      status: '待派单',
      tagType: 'danger',
    },
    metrics: [
      { label: '报修总数', value: '214', trend: '本月 32 单' },
      { label: '待处理', value: '9', trend: '高优先级 2 单' },
      { label: '平均响应', value: '18m', trend: '较上周 -6m' },
    ],
    featureCards: [
      { title: '故障上报', tag: '报修', type: 'primary', description: '教师、学生和管理员可选择故障设备，填写描述并上传图片。' },
      { title: '受理派单', tag: '派单', type: 'warning', description: '管理员判断故障等级、分配维修人员并设置处理状态。' },
      { title: '闭环维护', tag: '闭环', type: 'success', description: '维修人员回填处理过程和结果，同步更新设备状态和维修记录。' },
    ],
    tableColumns: [
      { prop: 'ticket', label: '工单号', minWidth: 120 },
      { prop: 'device', label: '故障设备', minWidth: 150 },
      { prop: 'lab', label: '实验室', minWidth: 130 },
      { prop: 'owner', label: '处理人', minWidth: 100 },
      { prop: 'status', label: '状态', minWidth: 90, type: 'tag' },
    ],
    records: [
      { ticket: 'R20260423001', device: '核心交换机', lab: 'B203 网络安全实验室', owner: '陈工', status: '处理中', tagType: 'warning' },
      { ticket: 'R20260422012', device: '显示器', lab: 'A101 云计算实验室', owner: '周工', status: '已完成', tagType: 'success' },
      { ticket: 'R20260421008', device: 'GPU 训练服务器', lab: 'C305 人工智能实验室', owner: '待分配', status: '待派单', tagType: 'danger' },
    ],
    timeline: [
      { title: '派发高优先级工单', date: '今日', type: 'danger' },
      { title: '回填维修结果', date: '今日', type: 'success' },
      { title: '统计设备故障排行', date: '本周', type: 'info' },
    ],
  }

  export const USAGE_RECORDS_MODULE = {
    badge: '过程留痕',
    recordName: '使用记录',
    primaryKey: 'person',
    tableTitle: '使用流水',
    actions: [
      { label: '导出记录', type: 'primary', command: 'export' },
      { label: '筛选异常', type: 'default', command: 'filterAbnormal' },
    ],
    statusOptions: [
      { label: '正常', type: 'success' },
      { label: '借用中', type: 'primary' },
      { label: '待复核', type: 'warning' },
      { label: '异常', type: 'danger' },
    ],
    defaultRecord: {
      person: '新增人员',
      resource: '待选择资源',
      scene: '自主使用',
      time: '04-23 16:30',
      status: '正常',
      tagType: 'success',
    },
    metrics: [
      { label: '今日记录', value: '386', trend: '上机 322 条' },
      { label: '借用记录', value: '42', trend: '归还 31 条' },
      { label: '异常记录', value: '5', trend: '待复核 2 条' },
    ],
    featureCards: [
      { title: '使用记录生成', tag: '留痕', type: 'primary', description: '预约通过后生成使用记录，记录实际使用时间、人员和用途。' },
      { title: '设备借用流水', tag: '资产', type: 'success', description: '记录设备借出、归还、负责人、使用场景和异常复核。' },
      { title: '审计报表', tag: '导出', type: 'warning', description: '按人员、班级、实验室和时间范围筛选并导出使用记录。' },
    ],
    tableColumns: [
      { prop: 'person', label: '人员/班级', minWidth: 130 },
      { prop: 'resource', label: '资源', minWidth: 150 },
      { prop: 'scene', label: '使用场景', minWidth: 110 },
      { prop: 'time', label: '时间', minWidth: 140 },
      { prop: 'status', label: '状态', minWidth: 90, type: 'tag' },
    ],
    records: [
      { person: '软件工程 2 班', resource: 'A101 云计算实验室', scene: '课程上机', time: '04-23 08:00', status: '正常', tagType: 'success' },
      { person: '李同学', resource: 'GPU 训练服务器', scene: '设备借用', time: '04-23 10:30', status: '借用中', tagType: 'primary' },
      { person: '巡检员 陈工', resource: 'B203 网络安全实验室', scene: '安全巡检', time: '04-23 11:20', status: '待复核', tagType: 'warning' },
    ],
    timeline: [
      { title: '归档今日上机流水', date: '今晚', type: 'primary' },
      { title: '复核异常使用记录', date: '今日', type: 'warning' },
      { title: '生成月度审计报表', date: '月底', type: 'info' },
    ],
  }

  export const CONSUMABLE_INVENTORY_MODULE = {
    apiEndpoints: API_ENDPOINTS.consumables,
    persistMode: 'single',
    badge: '耗材库存',
    recordName: '耗材',
    primaryKey: 'name',
    tableTitle: '耗材与库存清单',
    actions: [
      { label: '新增耗材', type: 'primary', command: 'create' },
      { label: '导出库存', type: 'default', command: 'export' },
    ],
    statusOptions: [
      { label: '库存充足', type: 'success' },
      { label: '低库存', type: 'warning' },
      { label: '待补货', type: 'danger' },
      { label: '已停用', type: 'info' },
    ],
    defaultRecord: {
      name: '新耗材',
      category: '教学耗材',
      stock: '0',
      unit: '件',
      location: '待分配',
      status: '低库存',
      tagType: 'warning',
    },
    metrics: [
      { label: '耗材种类', value: '46', trend: '覆盖 6 类物资' },
      { label: '低库存', value: '5', trend: '低于预警数量' },
      { label: '本月出库', value: '128', trend: '教学领取 96 件' },
    ],
    featureCards: [
      { title: '耗材信息管理', tag: '台账', type: 'primary', description: '维护耗材名称、类型、库存、单位、存放位置和预警数量。' },
      { title: '入库出库', tag: '流转', type: 'success', description: '记录经办人、供应商、领取人、领取数量、用途和出入库时间。' },
      { title: '库存预警', tag: '提醒', type: 'warning', description: '库存低于预警值时自动提醒实验室管理员及时补货。' },
    ],
    tableColumns: [
      { prop: 'name', label: '耗材名称', minWidth: 140 },
      { prop: 'category', label: '类型', minWidth: 110 },
      { prop: 'stock', label: '当前库存', minWidth: 100 },
      { prop: 'unit', label: '单位', minWidth: 80 },
      { prop: 'location', label: '存放位置', minWidth: 130 },
      { prop: 'status', label: '状态', minWidth: 90, type: 'tag' },
    ],
    records: [
      { name: '网线', category: '网络耗材', stock: '86', unit: '根', location: 'A 区库房', status: '库存充足', tagType: 'success' },
      { name: '鼠标', category: '外设', stock: '12', unit: '个', location: 'B 区库房', status: '低库存', tagType: 'warning' },
      { name: '固态硬盘', category: '维修备件', stock: '3', unit: '块', location: '维修柜', status: '待补货', tagType: 'danger' },
    ],
    timeline: [
      { title: '登记耗材入库与供应商信息', date: '入库时', type: 'primary' },
      { title: '记录领取用途和出库数量', date: '出库时', type: 'success' },
      { title: '生成低库存预警通知', date: '每日', type: 'warning' },
    ],
  }

  export const DATA_STATISTICS_MODULE = {
    badge: '统计分析',
    recordName: '统计项',
    primaryKey: 'dimension',
    tableTitle: '统计看板',
    actions: [
      { label: '生成报表', type: 'primary', command: 'export' },
      { label: '查看课表', type: 'default', command: 'showSchedule' },
    ],
    statusOptions: [
      { label: '上升', type: 'success' },
      { label: '稳定', type: 'primary' },
      { label: '优化', type: 'success' },
      { label: '关注', type: 'warning' },
    ],
    defaultRecord: {
      dimension: '新增统计维度',
      current: '0',
      compare: '+0%',
      owner: '待关注',
      status: '稳定',
      tagType: 'primary',
    },
    metrics: [
      { label: '周使用率', value: '82%', trend: '较上周 +5%' },
      { label: '课程排课', value: '146', trend: '覆盖 12 门课' },
      { label: '设备在线率', value: '93%', trend: '稳定运行' },
    ],
    featureCards: [
      { title: '实验室使用率', tag: '图表', type: 'primary', description: '按日、周、月统计各实验室使用频率，识别空闲时段和高峰时段。' },
      { title: '设备状态统计', tag: '资产', type: 'success', description: '汇总正常、故障、维修中和报废设备数量，为维护计划提供依据。' },
      { title: '故障数据分析', tag: '运维', type: 'warning', description: '统计高频故障设备、故障类型和平均维修时间，定位薄弱环节。' },
      { title: '报表导出', tag: '导出', type: 'info', description: '支持生成月度管理报告、Excel 清单和 PDF 汇总材料。' },
    ],
    tableColumns: [
      { prop: 'dimension', label: '统计维度', minWidth: 130 },
      { prop: 'current', label: '当前值', minWidth: 110 },
      { prop: 'compare', label: '环比', minWidth: 100 },
      { prop: 'owner', label: '关注人', minWidth: 100 },
      { prop: 'status', label: '状态', minWidth: 90, type: 'tag' },
    ],
    records: [
      { dimension: '实验室使用率', current: '82%', compare: '+5%', owner: '教务处', status: '上升', tagType: 'success' },
      { dimension: '设备在线率', current: '93%', compare: '+1%', owner: '信息中心', status: '稳定', tagType: 'primary' },
      { dimension: '故障响应时长', current: '18 分钟', compare: '-6 分钟', owner: '运维组', status: '优化', tagType: 'success' },
    ],
    timeline: [
      { title: '更新周课表与预约数据', date: '每日', type: 'primary' },
      { title: '汇总设备运行指标', date: '每日', type: 'success' },
      { title: '输出月度运营报表', date: '月底', type: 'info' },
    ],
  }

  export const SYSTEM_SETTINGS_MODULE = {
    badge: '平台配置',
    recordName: '配置项',
    primaryKey: 'name',
    tableTitle: '配置项',
    actions: [
      { label: '新增配置', type: 'primary', command: 'create' },
      { label: '权限校验', type: 'default', command: 'validatePermissions' },
    ],
    statusOptions: [
      { label: '启用', type: 'success' },
      { label: '停用', type: 'info' },
      { label: '待复核', type: 'warning' },
    ],
    defaultRecord: {
      name: '新增配置项',
      scope: '待配置',
      owner: '系统管理员',
      updatedAt: '04-23 16:00',
      status: '待复核',
      tagType: 'warning',
    },
    metrics: [
      { label: '基础配置', value: '36', trend: '已上线 32 项' },
      { label: '审计策略', value: '8', trend: '日志规则已启用' },
      { label: '服务策略', value: '6', trend: '统一后端配置' },
    ],
    featureCards: [
      { title: '基础数据配置', tag: '系统', type: 'primary', description: '维护实验室类型、设备类型、故障类型、软件类型和状态字典。' },
      { title: '操作日志审计', tag: '审计', type: 'success', description: '记录登录、权限变更、通知发布、导出等关键平台操作。' },
      { title: '接口与服务', tag: '服务', type: 'warning', description: '统一维护单点登录、AI 服务、门禁、监控和 IoT 设备接入配置。' },
    ],
    tableColumns: [
      { prop: 'name', label: '配置名称', minWidth: 150 },
      { prop: 'scope', label: '作用范围', minWidth: 120 },
      { prop: 'owner', label: '维护人', minWidth: 100 },
      { prop: 'updatedAt', label: '更新时间', minWidth: 140 },
      { prop: 'status', label: '状态', minWidth: 90, type: 'tag' },
    ],
    records: [
      { name: '登录认证策略', scope: '全平台', owner: '系统管理员', updatedAt: '04-22 18:10', status: '启用', tagType: 'success' },
      { name: '实验室状态字典', scope: '实验室管理', owner: '系统管理员', updatedAt: '04-21 15:30', status: '启用', tagType: 'success' },
      { name: 'AI 接口参数', scope: '扩展接入', owner: '系统管理员', updatedAt: '04-20 09:00', status: '待复核', tagType: 'warning' },
    ],
    timeline: [
      { title: '校验平台配置有效性', date: '本周', type: 'primary' },
      { title: '复核日志与权限规则', date: '本周', type: 'warning' },
      { title: '推进统一认证接入', date: '下周', type: 'info' },
    ],
  }

  export const SOFTWARE_ENVIRONMENT_MODULE = {
    apiEndpoints: {
      list: '/lab-software',
      create: '/lab-software',
      update: '/lab-software',
      delete: '/lab-software',
    },
    storageKey: 'smart-lab:software-environments:records',
    persistMode: 'single',
    badge: '软件环境',
    recordName: '软件',
    primaryKey: 'name',
    tableTitle: '软件与实验室环境清单',
    actions: [
      { label: '新增软件', type: 'primary', command: 'create' },
      { label: '导出环境清单', type: 'default', command: 'export' },
    ],
    statusOptions: [
      { label: '已安装', type: 'success' },
      { label: '待安装', type: 'warning' },
      { label: '需升级', type: 'primary' },
      { label: '授权异常', type: 'danger' },
    ],
    defaultRecord: {
      name: '新教学软件',
      version: '最新版',
      type: '开发工具',

      // 关键：这里不要默认写“待分配实验室”，必须让用户从下拉框选择真实实验室
      labId: undefined,

      // 表格展示用，选择下拉框后会自动回填实验室名称
      lab: '',

      license: '校内授权',
      status: '待安装',
      tagType: 'warning',
    },
    metrics: [
      { label: '软件条目', value: '126', trend: '覆盖 18 间实验室' },
      { label: '待安装任务', value: '9', trend: '课程环境关联 6 项' },
      { label: '授权异常', value: '3', trend: '需管理员复核' },
    ],
    featureCards: [
      {
        title: '软件清单维护',
        tag: '基础',
        type: 'primary',
        description: '登记软件名称、版本、类型、安装位置、授权情况和所属实验室。',
      },
      {
        title: '安装记录',
        tag: '留痕',
        type: 'success',
        description: '记录安装人、安装时间、安装结果和截图说明，方便教师远程核验。',
      },
      {
        title: '版本管理',
        tag: '版本',
        type: 'warning',
        description: '标记当前使用版本，保留历史版本，提醒授权异常和升级任务。',
      },
    ],
    tableColumns: [
      {
        prop: 'name',
        label: '软件名称',
        minWidth: 140,
        required: true,
      },
      {
        prop: 'version',
        label: '版本',
        minWidth: 110,
        required: true,
      },
      {
        prop: 'type',
        label: '软件类型',
        minWidth: 110,
        formType: 'select',
        required: true,
        options: [
          { label: '开发工具', value: '开发工具' },
          { label: '运行环境', value: '运行环境' },
          { label: '网络仿真', value: '网络仿真' },
          { label: '数据库工具', value: '数据库工具' },
          { label: '教学软件', value: '教学软件' },
          { label: '设计软件', value: '设计软件' },
          { label: '其他', value: '其他' },
        ],
      },

      // 关键改动：
      // 表格显示 lab，但是弹窗真正提交 labId
      // 这样后端 lab_software.lab_id 才能对应 lab.id
      {
        prop: 'lab',
        label: '所属实验室',
        minWidth: 150,
        formProp: 'labId',
        formType: 'select',
        required: true,
        optionLabel: 'labName',
        optionValue: 'id',
      },

      {
        prop: 'license',
        label: '授权情况',
        minWidth: 120,
        formType: 'select',
        required: true,
        options: [
          { label: '校内授权', value: '校内授权' },
          { label: '免费授权', value: '免费授权' },
          { label: '开源', value: '开源' },
          { label: '教学授权', value: '教学授权' },
          { label: '试用授权', value: '试用授权' },
          { label: '授权异常', value: '授权异常' },
          {label: '插件破解', value: '插件破解'},
        ],
      },
      {
        prop: 'status',
        label: '状态',
        minWidth: 90,
        type: 'tag',
        formType: 'select',
        required: true,
      },
    ],
    records: [
      {
        name: 'Visual Studio Code',
        version: '1.98',
        type: '开发工具',
        labId: 1,
        lab: 'A101 云计算实验室',
        license: '免费授权',
        status: '已安装',
        tagType: 'success',
      },
      {
        name: 'Python',
        version: '3.12',
        type: '运行环境',
        labId: 2,
        lab: 'C305 人工智能实验室',
        license: '开源',
        status: '需升级',
        tagType: 'primary',
      },
      {
        name: 'Packet Tracer',
        version: '8.2',
        type: '网络仿真',
        labId: 3,
        lab: 'B203 网络安全实验室',
        license: '教学授权',
        status: '待安装',
        tagType: 'warning',
      },
    ],
    timeline: [
      { title: '核对课程所需软件版本', date: '今日', type: 'primary' },
      { title: '上传关键软件安装截图', date: '本周', type: 'success' },
      { title: '复核授权到期软件', date: '本周', type: 'warning' },
    ],
  }

  export const COURSE_ENVIRONMENT_MODULE = {
    apiEndpoints: API_ENDPOINTS.courseEnvironmentRequests,
    storageKey: 'smart-lab:course-environment-requests:records',
    persistMode: 'single',
    badge: '课程环境',
    recordName: '环境需求',
    primaryKey: 'course',
    tableTitle: '课程环境需求与确认',
    actions: [
      { label: '提交需求', type: 'primary', command: 'create' },
      { label: '标记待确认', type: 'default', command: 'batchStatus', targetStatus: '待教师确认' },
    ],
    statusOptions: [
      { label: '待配置', type: 'warning' },
      { label: '配置中', type: 'primary' },
      { label: '待教师确认', type: 'warning' },
      { label: '已确认', type: 'success' },
      { label: '已退回', type: 'danger' },
    ],
    defaultRecord: {
      course: '',
      teacher: '',
      className: '',
      labType: '',
      software: '',
      status: '待配置',
      tagType: 'warning',
    },
    metrics: [
      { label: '环境需求', value: '42', trend: '本周新增 8 条' },
      { label: '待教师确认', value: '6', trend: '含截图 4 条' },
      { label: '已复用模板', value: '27', trend: '复用率 64%' },
    ],
    featureCards: [
      {
        title: '教师提交需求',
        tag: '提交',
        type: 'primary',
        description: '填写课程、班级、使用时间、实验室类型、软件版本和特殊配置说明。',
      },
      {
        title: '管理员配置处理',
        tag: '配置',
        type: 'warning',
        description: '分配实验室、引用相似模板、上传配置说明并标记处理状态。',
      },
      {
        title: '教师远程确认',
        tag: '确认',
        type: 'success',
        description: '教师查看软件清单、环境截图和配置说明，可确认可用或退回修改。',
      },
    ],
    tableColumns: [
      {
        prop: 'course',
        label: '课程名称',
        minWidth: 150,
        formType: 'select',
        required: true,
        optionLabel: 'label',
        optionValue: 'value',
      },
      {
        prop: 'teacher',
        label: '任课教师',
        minWidth: 100,
        formType: 'select',
        required: true,
        optionLabel: 'label',
        optionValue: 'value',
      },
      {
        prop: 'className',
        label: '上课班级',
        minWidth: 130,
        formType: 'select',
        required: true,
        optionLabel: 'label',
        optionValue: 'value',
      },
      {
        prop: 'labType',
        label: '实验室类型',
        minWidth: 120,
        formType: 'select',
        required: true,
        optionLabel: 'label',
        optionValue: 'value',
        options: [
          { label: '计算机实验室', value: '计算机实验室' },
          { label: '软件开发实验室', value: '软件开发实验室' },
          { label: '网络安全实验室', value: '网络安全实验室' },
          { label: '人工智能实验室', value: '人工智能实验室' },
          { label: '移动互联实训室', value: '移动互联实训室' },
          { label: '云计算实验室', value: '云计算实验室' },
          { label: '普通多媒体实验室', value: '普通多媒体实验室' },
        ],
      },
      {
        prop: 'software',
        label: '所需软件',
        minWidth: 170,
        required: true,
        formType: 'textarea',
      },
      {
        prop: 'status',
        label: '状态',
        minWidth: 110,
        type: 'tag',
        formType: 'select',
        required: true,
      },
    ],
    records: [
      {
        course: 'Java Web 程序设计',
        teacher: '李娜',
        className: '软件工程 2 班',
        labType: '软件开发实验室',
        software: 'JDK 21、IDEA、MySQL',
        status: '待教师确认',
        tagType: 'warning',
      },
      {
        course: '网络安全攻防实训',
        teacher: '周强',
        className: '网安 1 班',
        labType: '网络安全实验室',
        software: 'Kali、Wireshark、靶机镜像',
        status: '配置中',
        tagType: 'primary',
      },
      {
        course: '人工智能基础',
        teacher: '王璐',
        className: '数据科学 1 班',
        labType: '人工智能实验室',
        software: 'Python、PyTorch、CUDA',
        status: '已确认',
        tagType: 'success',
      },
    ],
    timeline: [
      { title: '管理员完成软件安装并上传截图', date: '今日', type: 'primary' },
      { title: '教师远程确认课程环境', date: '明日', type: 'warning' },
      { title: '归档已确认环境用于模板推荐', date: '本周', type: 'success' },
    ],
  }

  export const ENVIRONMENT_TEMPLATE_MODULE = {
    apiEndpoints: API_ENDPOINTS.environmentTemplates,
    badge: '模板复用',
    recordName: '环境模板',
    persistMode: 'single',
    primaryKey: 'name',
    tableTitle: '常用课程环境模板',
    actions: [
      { label: '新增模板', type: 'primary', command: 'create' },
      { label: '导出模板', type: 'default', command: 'export' },
    ],
    statusOptions: [
      { label: '可复用', type: 'success' },
      { label: '待更新', type: 'warning' },
      { label: '试运行', type: 'primary' },
    ],
    defaultRecord: {
      name: '新环境模板',
      course: '适用课程',
      os: 'Windows 11',
      softwareList: '待维护软件清单',
      labs: '待选择实验室',
      status: '试运行',
      tagType: 'primary',
    },
    metrics: [
      { label: '模板总数', value: '18', trend: '覆盖 12 门课程' },
      { label: '本月复用', value: '31', trend: '减少重复配置' },
      { label: '待更新模板', value: '4', trend: '版本需核验' },
    ],
    featureCards: [
      { title: '模板沉淀', tag: '复用', type: 'primary', description: '将常用操作系统、软件清单、依赖环境和配置说明保存为模板。' },
      { title: '智能推荐基础', tag: 'AI', type: 'success', description: '记录模板使用次数和适用课程，为课程环境智能推荐提供业务依据。' },
      { title: '一键引用', tag: '效率', type: 'warning', description: '管理员可根据模板快速生成课程环境配置任务，减少重复沟通。' },
    ],
    tableColumns: [
      { prop: 'name', label: '模板名称', minWidth: 150 },
      { prop: 'course', label: '适用课程', minWidth: 130 },
      { prop: 'os', label: '操作系统', minWidth: 110 },
      { prop: 'softwareList', label: '软件清单', minWidth: 180 },
      { prop: 'labs',
        label: '适用实验室',
        minWidth: 140 ,
        formProp: 'labId',
        formType: 'select',
        required: true,
        optionLabel: 'labName',
        optionValue: 'id',},
      { prop: 'status', label: '状态', minWidth: 90, type: 'tag' },
    ],
    records: [
      { name: 'Java Web 标准环境', course: 'Java Web 程序设计', os: 'Windows 11', softwareList: 'JDK、IDEA、Tomcat、MySQL', labs: 'A101、A102', status: '可复用', tagType: 'success' },
      { name: '网络攻防隔离环境', course: '网络安全攻防实训', os: 'Ubuntu/Kali', softwareList: 'Kali、靶场镜像、Wireshark', labs: 'B203', status: '待更新', tagType: 'warning' },
      { name: 'AI GPU 训练环境', course: '人工智能基础', os: 'Ubuntu 22.04', softwareList: 'Python、CUDA、PyTorch', labs: 'C305', status: '可复用', tagType: 'success' },
    ],
    timeline: [
      { title: '根据历史需求推荐相似模板', date: '提交时', type: 'primary' },
      { title: '统计模板使用次数', date: '每日', type: 'success' },
      { title: '更新过期软件版本', date: '本周', type: 'warning' },
    ],
  }

  export const AI_ASSISTANT_MODULE = {
    badge: 'AI辅助',
    recordName: '智能任务',
    primaryKey: 'scenario',
    tableTitle: 'AI智能辅助场景',
    actions: [
      { label: '新增问答场景', type: 'primary', command: 'create' },
      { label: '导出建议记录', type: 'default', command: 'export' },
    ],
    statusOptions: [
      { label: '可用', type: 'success' },
      { label: '运行中', type: 'success' },
      { label: '优化中', type: 'primary' },
    ],
    defaultRecord: {
      scenario: '新智能场景',
      input: '用户输入问题',
      output: '大模型返回建议',
      owner: '实验室管理员',
      status: '运行中',
      tagType: 'success',
    },
    metrics: [
      { label: '助手场景', value: '4', trend: '问答、推荐、排障、分析' },
      { label: '建议记录', value: '86', trend: '本周新增 19 条' },
      { label: '智能服务', value: '1', trend: '企业接口已配置' },
    ],
    tableColumns: [
      { prop: 'scenario', label: '应用场景', minWidth: 150 },
      { prop: 'input', label: '输入内容', minWidth: 170 },
      { prop: 'output', label: '输出建议', minWidth: 190 },
      { prop: 'owner', label: '使用角色', minWidth: 110 },
      { prop: 'status', label: '状态', minWidth: 110, type: 'tag' },
    ],
    records: [
      { scenario: '环境配置助手', input: 'Python 深度学习课程环境', output: '推荐 CUDA、PyTorch、Jupyter 配置', owner: '实验室管理员', status: '可用', tagType: 'success' },
      { scenario: '故障排查助手', input: '交换机端口无法连接', output: '检查链路、VLAN、端口状态与日志', owner: '维修人员', status: '优化中', tagType: 'primary' },
      { scenario: '数据分析建议', input: 'C305 使用率持续偏高', output: '建议分流至 A102 并扩展 GPU 预约规则', owner: '管理者', status: '运行中', tagType: 'success' },
    ],
    timeline: [
      { title: '智能助手服务接入', date: '已完成', type: 'primary' },
      { title: '业务数据联动建议', date: '已接入', type: 'warning' },
      { title: '结合运行数据输出调度建议', date: '持续优化', type: 'success' },
    ],
  }

  export const NOTICE_CENTER_MODULE = {
    apiEndpoints: API_ENDPOINTS.notices,
    persistMode: 'single',
    badge: '通知公告',
    recordName: '通知',
    primaryKey: 'title',
    tableTitle: '通知公告与系统提醒',
    actions: [
      { label: '发布通知', type: 'primary', command: 'create' },
      { label: '导出通知', type: 'default', command: 'export' },
    ],
    statusOptions: [
      { label: '已发布', type: 'success' },
      { label: '定时发布', type: 'primary' },
      { label: '草稿', type: 'info' },
      { label: '需处理', type: 'warning' },
    ],
    defaultRecord: {
      title: '新通知',
      target: '全部用户',
      type: '系统提醒',
      publishTime: '待设置',
      status: '草稿',
      tagType: 'info',
    },
    metrics: [
      { label: '本月通知', value: '28', trend: '维护 9 条' },
      { label: '未读提醒', value: '64', trend: '环境确认 12 条' },
      { label: '库存预警', value: '5', trend: '低于阈值' },
    ],
    featureCards: [
      { title: '公告发布', tag: '发布', type: 'primary', description: '实验室管理员可发布开放安排、维护通知和环境确认提醒。' },
      { title: '对象分发', tag: '角色', type: 'success', description: '按实验室管理员、预约申请人和处理责任人等对象定向发送提醒。' },
      { title: '已读未读', tag: '提醒', type: 'warning', description: '记录用户通知状态，突出待确认环境、报修处理和库存预警。' },
    ],
    tableColumns: [
      { prop: 'title', label: '通知标题', minWidth: 170 },
      { prop: 'target', label: '通知对象', minWidth: 110 },
      { prop: 'type', label: '通知类型', minWidth: 110 },
      { prop: 'publishTime', label: '发布时间', minWidth: 140, readonly: true },
      { prop: 'status', label: '状态', minWidth: 90, type: 'tag' },
    ],
    records: [
      { title: 'A101 课程环境待确认', target: '实验室管理员', type: '环境确认', publishTime: '2026-04-24 09:00', status: '需处理', tagType: 'warning' },
      { title: 'B203 网络维护通知', target: '实验室管理员', type: '维护通知', publishTime: '2026-04-25 18:00', status: '定时发布', tagType: 'primary' },
      { title: '期末实验室预约开放', target: '业务使用方', type: '开放安排', publishTime: '2026-04-20 10:00', status: '已发布', tagType: 'success' },
    ],
    timeline: [
      { title: '发送课程环境确认提醒', date: '实时', type: 'warning' },
      { title: '同步预约审批结果', date: '实时', type: 'primary' },
      { title: '推送低库存预警', date: '每日', type: 'success' },
    ],
  }

  export const USER_PERMISSION_MODULE = {
    badge: '账号权限',
    recordName: '用户',
    primaryKey: 'name',
    tableTitle: '用户角色与权限配置',
    actions: [
      { label: '新增用户', type: 'primary', command: 'create' },
      { label: '权限校验', type: 'default', command: 'validatePermissions' },
    ],
    statusOptions: [
      { label: '启用', type: 'success' },
      { label: '待审核', type: 'warning' },
      { label: '停用', type: 'info' },
    ],
    defaultRecord: {
      name: '新用户',
      role: '实验室管理员',
      department: '信息中心',
      contact: '待填写',
      status: '待审核',
      tagType: 'warning',
    },
    metrics: [
      { label: '用户账号', value: '128', trend: '启用 121 个' },
      { label: '系统角色', value: '2', trend: '系统管理员、实验室管理员' },
      { label: '待审核账号', value: '7', trend: '实验室管理员 5 个' },
    ],
    featureCards: [
      { title: '账号创建', tag: '用户', type: 'primary', description: '系统管理员创建系统管理员和实验室管理员账号，并维护登录状态。' },
      { title: '角色权限', tag: '权限', type: 'success', description: '按系统管理员与实验室管理员两类角色控制访问范围和菜单能力。' },
      { title: '个人信息', tag: '资料', type: 'info', description: '支持维护姓名、手机号、邮箱、密码和个人操作记录。' },
    ],
    tableColumns: [
      { prop: 'name', label: '姓名', minWidth: 110 },
      { prop: 'role', label: '角色', minWidth: 130 },
      { prop: 'department', label: '所属部门', minWidth: 140 },
      { prop: 'contact', label: '联系方式', minWidth: 140 },
      { prop: 'status', label: '状态', minWidth: 90, type: 'tag' },
    ],
    records: [
      { name: '张明', role: '实验室管理员', department: '信息中心', contact: '13800000001', status: '启用', tagType: 'success' },
      { name: '陈倩', role: '系统管理员', department: '信息中心', contact: '13900000002', status: '启用', tagType: 'success' },
      { name: '王磊', role: '实验室管理员', department: '实验中心', contact: 'labadmin003', status: '待审核', tagType: 'warning' },
    ],
    timeline: [
      { title: '系统管理员创建实验室管理员账号', date: '按需', type: 'primary' },
      { title: '复核待启用账号', date: '每日', type: 'warning' },
      { title: '记录用户操作日志', date: '实时', type: 'success' },
    ],
  }

  export const SYSTEM_ADMIN_NOTICE_MODULE = {
    apiEndpoints: API_ENDPOINTS.notices,
    persistMode: 'single',
    badge: '平台通知',
    recordName: '通知',
    primaryKey: 'title',
    tableTitle: '平台通知公告',
    actions: [
      { label: '发布公告', type: 'primary', command: 'create' },
      { label: '导出通知', type: 'default', command: 'export' },
    ],
    statusOptions: [
      { label: '已发布', type: 'success' },
      { label: '定时发布', type: 'primary' },
      { label: '草稿', type: 'info' },
      { label: '待复核', type: 'warning' },
    ],
    defaultRecord: {
      title: '新平台公告',
      target: '实验室管理员',
      type: '系统公告',
      publishTime: '待设置',
      status: '草稿',
      tagType: 'info',
    },
    metrics: [
      { label: '本月公告', value: '18', trend: '系统治理 6 条' },
      { label: '待发布', value: '4', trend: '含维护通知 2 条' },
      { label: '触达对象', value: '15', trend: '实验室管理员' },
    ],
    featureCards: [
      { title: '平台制度公告', tag: '制度', type: 'primary', description: '发布平台制度更新、账号治理要求和系统维护计划。' },
      { title: '分角色触达', tag: '触达', type: 'success', description: '面向实验室管理员定向发送系统公告、权限调整和配置提醒。' },
      { title: '发布留痕', tag: '审计', type: 'warning', description: '记录公告发布时间、发布人、状态和阅读范围，便于审计追踪。' },
    ],
    tableColumns: [
      { prop: 'title', label: '公告标题', minWidth: 180 },
      { prop: 'target', label: '触达对象', minWidth: 120 },
      { prop: 'type', label: '公告类型', minWidth: 110 },
      { prop: 'publishTime', label: '发布时间', minWidth: 140, readonly: true },
      { prop: 'status', label: '状态', minWidth: 90, type: 'tag' },
    ],
    records: [
      { title: '五月平台维护窗口通知', target: '实验室管理员', type: '维护公告', publishTime: '2026-04-25 18:00', status: '定时发布', tagType: 'primary' },
      { title: '账号权限收敛要求', target: '实验室管理员', type: '制度公告', publishTime: '2026-04-22 09:00', status: '已发布', tagType: 'success' },
      { title: '统一认证接入说明', target: '系统管理员', type: '配置通知', publishTime: '2026-04-24 14:00', status: '待复核', tagType: 'warning' },
    ],
    timeline: [
      { title: '起草平台公告', date: '按需', type: 'primary' },
      { title: '复核发布内容', date: '发布前', type: 'warning' },
      { title: '归档公告记录', date: '发布后', type: 'success' },
    ],
  }

  export const SYSTEM_ADMIN_STATISTICS_MODULE = {
    badge: '平台统计',
    recordName: '平台指标',
    primaryKey: 'dimension',
    tableTitle: '平台治理统计',
    actions: [
      { label: '导出报表', type: 'primary', command: 'export' },
      { label: '查看配置', type: 'default', command: 'showSchedule' },
    ],
    statusOptions: [
      { label: '稳定', type: 'success' },
      { label: '关注', type: 'warning' },
      { label: '优化中', type: 'primary' },
    ],
    defaultRecord: {
      dimension: '新增平台指标',
      current: '0',
      compare: '+0%',
      owner: '系统管理员',
      status: '稳定',
      tagType: 'success',
    },
    metrics: [
      { label: '账号启用率', value: '94%', trend: '较上月 +2%' },
      { label: '权限命中率', value: '98%', trend: '无越权异常' },
      { label: '公告发布数', value: '18', trend: '本月' },
    ],
    featureCards: [
      { title: '账号治理', tag: '账号', type: 'primary', description: '统计账号数量、启用率、待审核账号和角色覆盖情况。' },
      { title: '权限与访问', tag: '权限', type: 'success', description: '跟踪权限配置、菜单绑定、访问命中率和潜在越权风险。' },
      { title: '平台运营', tag: '运营', type: 'warning', description: '查看公告发布、配置变更、日志审计和系统稳定性指标。' },
    ],
    tableColumns: [
      { prop: 'dimension', label: '统计维度', minWidth: 140 },
      { prop: 'current', label: '当前值', minWidth: 110 },
      { prop: 'compare', label: '环比', minWidth: 100 },
      { prop: 'owner', label: '责任方', minWidth: 120 },
      { prop: 'status', label: '状态', minWidth: 90, type: 'tag' },
    ],
    records: [
      { dimension: '账号启用率', current: '94%', compare: '+2%', owner: '系统管理员', status: '稳定', tagType: 'success' },
      { dimension: '待审核账号', current: '7', compare: '-1', owner: '系统管理员', status: '关注', tagType: 'warning' },
      { dimension: '配置变更成功率', current: '99%', compare: '+1%', owner: '平台配置中心', status: '优化中', tagType: 'primary' },
    ],
    timeline: [
      { title: '同步账号与权限数据', date: '每日', type: 'primary' },
      { title: '输出周治理简报', date: '每周', type: 'success' },
      { title: '汇总月度平台报告', date: '月底', type: 'warning' },
    ],
  }
