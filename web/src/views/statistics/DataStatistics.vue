<script setup>
import { computed, markRaw, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Bell,
  DataAnalysis,
  DocumentChecked,
  Monitor,
  Setting,
  TrendCharts,
  User,
  WarningFilled,
} from '@element-plus/icons-vue'
import { AUTH_ROLE_DEFAULT_PATH, DEFAULT_AUTH_ROLE } from '../../constants/auth'
import PageHero from '../../components/common/PageHero.vue'
import { useECharts } from '../../composables/useECharts'
import { echarts } from '../../utils/echartsCore'
import http from '../../api/http'
import { fetchDashboardOverview } from '../../api/dashboard'
import { API_ENDPOINTS } from '../../api/endpoints'
import { exportTableToExcel } from '../../utils/exportExcel'

defineOptions({
  name: 'DataStatistics',
})

const PERIOD_OPTIONS = [
  { label: '近 7 天', value: 'week' },
  { label: '本月', value: 'month' },
  { label: '本学期', value: 'term' },
]

const SUMMARY_ICON_MAP = {
  usage: markRaw(TrendCharts),
  schedule: markRaw(DocumentChecked),
  device: markRaw(Monitor),
  repair: markRaw(WarningFilled),
  account: markRaw(User),
  config: markRaw(Setting),
  notice: markRaw(Bell),
  governance: markRaw(DataAnalysis),
}

const LAB_ADMIN_PAGE = {
  badge: '统计分析',
  title: '统计分析',
  description: '通过图表查看实验室使用率、设备状态、故障趋势与课程环境需求变化。',
  pageIcon: markRaw(DataAnalysis),
  actions: [
    { label: '生成报表', type: 'primary', command: 'report' },
    { label: '导出图表', type: 'default', command: 'export' },
  ],
  toolbarTitle: '实验室运营视图',
  toolbarDescription: '按时间维度观察实验室运行波动、环境需求变化和故障热点。',
  charts: {
    trend: { title: '使用率趋势', subtitle: '预约与上机利用率变化', tag: '折线图' },
    compare: { title: '实验室使用对比', subtitle: '使用记录前五实验室横向对比', tag: '柱状图' },
    distribution: { title: '设备状态分布', subtitle: '当前资产运行状态汇总', tag: '环形图' },
    ranking: { title: '高频故障类型', subtitle: '近阶段报修问题排行', tag: '排行图' },
  },
  detailTitle: '关键明细',
  periods: {
    week: {
      summary: [
        { label: '周使用率', value: '82%', trend: '较上周 +5%', iconKey: 'usage' },
        { label: '预约排课', value: '146', trend: '覆盖 12 门课', iconKey: 'schedule' },
        { label: '设备在线率', value: '93%', trend: '稳定运行', iconKey: 'device' },
        { label: '报修闭环率', value: '89%', trend: '较上周 +4%', iconKey: 'repair' },
      ],
      trend: {
        name: '使用率',
        unit: '%',
        categories: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
        values: [68, 74, 86, 83, 88, 61, 49],
        color: '#2563eb',
      },
      compare: {
        name: '使用率',
        unit: '%',
        categories: ['A101', 'A102', 'B203', 'C305', 'D102'],
        values: [88, 81, 76, 92, 67],
        color: '#3b82f6',
      },
      distribution: {
        name: '设备状态',
        data: [
          { name: '正常', value: 598 },
          { name: '维修中', value: 24 },
          { name: '故障', value: 18 },
          { name: '闲置', value: 12 },
        ],
      },
      ranking: {
        name: '故障次数',
        unit: '次',
        categories: ['网络端口异常', '系统无法启动', '显卡驱动冲突', '显示器无信号'],
        values: [12, 9, 6, 4],
        color: '#f59e0b',
      },
      details: [
        { metric: 'A101 晚间开放利用率', value: '88%', change: '+8%', owner: '实验室管理员', status: '上升', tagType: 'success' },
        { metric: 'GPU 环境需求量', value: '18 次', change: '+3', owner: '课程环境', status: '关注', tagType: 'warning' },
        { metric: '平均故障响应时长', value: '18 分钟', change: '-6 分钟', owner: '维修工单', status: '优化', tagType: 'primary' },
        { metric: '课程环境确认通过率', value: '91%', change: '+4%', owner: '教师确认', status: '稳定', tagType: 'info' },
      ],
      insights: [
        { title: 'C305 持续高负载', description: 'GPU 课程集中在周四和周五，建议将部分训练课程分流到 A102。', type: 'warning' },
        { title: '故障集中在网络与启动', description: '本周端口异常和系统启动问题占比最高，适合增加交换机巡检与系统镜像校验。', type: 'primary' },
        { title: '环境模板复用效果明显', description: 'Java Web 与数据库类课程复用模板后，环境准备平均缩短约 30%。', type: 'success' },
      ],
    },
    month: {
      summary: [
        { label: '月使用率', value: '84%', trend: '较上月 +3%', iconKey: 'usage' },
        { label: '排课总数', value: '528', trend: '覆盖 38 门课', iconKey: 'schedule' },
        { label: '设备在线率', value: '95%', trend: '较上月 +2%', iconKey: 'device' },
        { label: '报修闭环率', value: '91%', trend: '较上月 +5%', iconKey: 'repair' },
      ],
      trend: {
        name: '使用率',
        unit: '%',
        categories: ['第 1 周', '第 2 周', '第 3 周', '第 4 周'],
        values: [79, 82, 85, 84],
        color: '#2563eb',
      },
      compare: {
        name: '使用率',
        unit: '%',
        categories: ['A101', 'A102', 'B203', 'C305', 'D102'],
        values: [85, 79, 73, 90, 69],
        color: '#3b82f6',
      },
      distribution: {
        name: '设备状态',
        data: [
          { name: '正常', value: 606 },
          { name: '维修中', value: 20 },
          { name: '故障', value: 14 },
          { name: '闲置', value: 12 },
        ],
      },
      ranking: {
        name: '故障次数',
        unit: '次',
        categories: ['网络端口异常', '系统无法启动', '镜像损坏', '教师机外设异常'],
        values: [36, 24, 17, 11],
        color: '#f59e0b',
      },
      details: [
        { metric: '晚间开放预约量', value: '96 次', change: '+12', owner: '预约管理', status: '上升', tagType: 'success' },
        { metric: '环境确认退回率', value: '6%', change: '-2%', owner: '教师确认', status: '优化', tagType: 'primary' },
        { metric: '高频报修实验室', value: 'B203', change: '+4 单', owner: '维修工单', status: '关注', tagType: 'warning' },
        { metric: '模板复用次数', value: '31 次', change: '+8', owner: '课程环境', status: '稳定', tagType: 'info' },
      ],
      insights: [
        { title: 'B203 故障率偏高', description: '网络实验室工单数连续两周偏高，建议安排专项巡检窗口。', type: 'warning' },
        { title: '模板沉淀已见成效', description: '复用模板最多的课程来自 Java Web 与数据库方向，适合继续扩充 AI 课程模板。', type: 'success' },
        { title: '晚间时段更适合开放自习', description: '18:00 后预约更多集中在 A101 与 A102，可做差异化开放策略。', type: 'primary' },
      ],
    },
    term: {
      summary: [
        { label: '学期使用率', value: '79%', trend: '较上学期 +6%', iconKey: 'usage' },
        { label: '学期排课', value: '1836', trend: '覆盖 86 门课', iconKey: 'schedule' },
        { label: '设备在线率', value: '92%', trend: '维持稳定', iconKey: 'device' },
        { label: '报修闭环率', value: '87%', trend: '较上学期 +7%', iconKey: 'repair' },
      ],
      trend: {
        name: '使用率',
        unit: '%',
        categories: ['2 月', '3 月', '4 月', '5 月', '6 月'],
        values: [63, 75, 82, 86, 79],
        color: '#2563eb',
      },
      compare: {
        name: '使用率',
        unit: '%',
        categories: ['A101', 'A102', 'B203', 'C305', 'D102'],
        values: [81, 74, 72, 88, 63],
        color: '#3b82f6',
      },
      distribution: {
        name: '设备状态',
        data: [
          { name: '正常', value: 592 },
          { name: '维修中', value: 26 },
          { name: '故障', value: 22 },
          { name: '闲置', value: 12 },
        ],
      },
      ranking: {
        name: '故障次数',
        unit: '次',
        categories: ['系统无法启动', '网络端口异常', '显卡驱动冲突', '镜像损坏'],
        values: [62, 57, 28, 19],
        color: '#f59e0b',
      },
      details: [
        { metric: '学期课程环境需求', value: '164 条', change: '+22', owner: '课程环境', status: '上升', tagType: 'success' },
        { metric: '学期模板复用率', value: '64%', change: '+11%', owner: '模板管理', status: '优化', tagType: 'primary' },
        { metric: '平均维修闭环时长', value: '1.8 天', change: '-0.4 天', owner: '维修工单', status: '稳定', tagType: 'info' },
        { metric: '设备报废建议数', value: '7 台', change: '+2', owner: '设备资产', status: '关注', tagType: 'warning' },
      ],
      insights: [
        { title: 'GPU 类课程增长明显', description: '本学期 GPU 环境需求增长快，建议系统管理员提前储备镜像和驱动版本。', type: 'warning' },
        { title: '维护效率整体提升', description: '维修工单平均闭环时长较上学期缩短，说明派单和回填流程已经稳定。', type: 'success' },
        { title: 'A101/A102 仍是核心承载机房', description: '两间机房承担了多数基础课程，可优先投入更新预算。', type: 'primary' },
      ],
    },
  },
}

const SYSTEM_ADMIN_PAGE = {
  badge: '平台统计',
  title: '平台统计',
  description: '从账号治理、权限命中、配置变更和通知触达四个维度查看平台运行状态。',
  pageIcon: markRaw(DataAnalysis),
  actions: [
    { label: '导出月报', type: 'primary', command: 'report' },
    { label: '导出数据', type: 'default', command: 'export' },
  ],
  toolbarTitle: '平台治理视图',
  toolbarDescription: '按时间口径查看账号启用率、权限策略执行情况和平台运营变化。',
  charts: {
    trend: { title: '账号活跃趋势', subtitle: '登录与访问活跃度变化', tag: '折线图' },
    compare: { title: '治理模块完成度', subtitle: '各平台模块运行质量对比', tag: '柱状图' },
    distribution: { title: '角色分布', subtitle: '当前平台账号角色占比', tag: '环形图' },
    ranking: { title: '配置变更排行', subtitle: '近阶段高频治理动作', tag: '排行图' },
  },
  detailTitle: '治理明细',
  periods: {
    week: {
      summary: [
        { label: '账号启用率', value: '94%', trend: '较上周 +2%', iconKey: 'account' },
        { label: '权限命中率', value: '98%', trend: '无越权异常', iconKey: 'governance' },
        { label: '配置变更', value: '12', trend: '本周新增', iconKey: 'config' },
        { label: '通知触达率', value: '91%', trend: '较上周 +3%', iconKey: 'notice' },
      ],
      trend: {
        name: '活跃账号',
        unit: '个',
        categories: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
        values: [84, 88, 91, 89, 93, 58, 42],
        color: '#4f46e5',
      },
      compare: {
        name: '完成度',
        unit: '%',
        categories: ['用户权限', '系统设置', '通知公告', '统计服务'],
        values: [98, 95, 91, 87],
        color: '#6366f1',
      },
      distribution: {
        name: '角色分布',
        data: [
          { name: '实验室管理员', value: 18 },
          { name: '教师', value: 76 },
          { name: '维修人员', value: 12 },
          { name: '系统管理员', value: 6 },
        ],
      },
      ranking: {
        name: '变更次数',
        unit: '次',
        categories: ['权限调整', '系统参数', '通知发布', '服务配置'],
        values: [12, 9, 7, 3],
        color: '#8b5cf6',
      },
      details: [
        { metric: '待审核账号', value: '7 个', change: '-1', owner: '用户权限', status: '关注', tagType: 'warning' },
        { metric: '通知发送成功率', value: '97%', change: '+2%', owner: '通知公告', status: '稳定', tagType: 'info' },
        { metric: '权限策略命中率', value: '98%', change: '+1%', owner: '角色权限', status: '优化', tagType: 'primary' },
        { metric: '配置变更成功率', value: '99%', change: '+1%', owner: '系统设置', status: '上升', tagType: 'success' },
      ],
      insights: [
        { title: '教师账号活跃度较高', description: '工作日 08:00 至 16:00 登录最频繁，可优先投放教学相关公告。', type: 'primary' },
        { title: '权限调整集中发生在实验室管理员', description: '近期新增环境管理能力较多，建议复核实验室管理员菜单授权边界。', type: 'warning' },
        { title: '公告触达率保持平稳', description: '触达率维持在 90% 以上，通知机制可继续沿用当前分发策略。', type: 'success' },
      ],
    },
    month: {
      summary: [
        { label: '账号启用率', value: '95%', trend: '较上月 +1%', iconKey: 'account' },
        { label: '权限命中率', value: '98%', trend: '稳定无异常', iconKey: 'governance' },
        { label: '配置变更', value: '37', trend: '本月累计', iconKey: 'config' },
        { label: '通知触达率', value: '92%', trend: '较上月 +4%', iconKey: 'notice' },
      ],
      trend: {
        name: '活跃账号',
        unit: '个',
        categories: ['第 1 周', '第 2 周', '第 3 周', '第 4 周'],
        values: [86, 89, 92, 94],
        color: '#4f46e5',
      },
      compare: {
        name: '完成度',
        unit: '%',
        categories: ['用户权限', '系统设置', '通知公告', '统计服务'],
        values: [99, 96, 92, 89],
        color: '#6366f1',
      },
      distribution: {
        name: '角色分布',
        data: [
          { name: '实验室管理员', value: 18 },
          { name: '教师', value: 77 },
          { name: '维修人员', value: 13 },
          { name: '系统管理员', value: 6 },
        ],
      },
      ranking: {
        name: '变更次数',
        unit: '次',
        categories: ['权限调整', '系统参数', '通知发布', '日志策略'],
        values: [28, 19, 14, 9],
        color: '#8b5cf6',
      },
      details: [
        { metric: '新建账号', value: '16 个', change: '+4', owner: '用户权限', status: '上升', tagType: 'success' },
        { metric: '待发布公告', value: '4 条', change: '+1', owner: '通知公告', status: '关注', tagType: 'warning' },
        { metric: '配置审计覆盖率', value: '100%', change: '持平', owner: '系统设置', status: '稳定', tagType: 'info' },
        { metric: '越权访问告警', value: '0', change: '持平', owner: '角色权限', status: '优化', tagType: 'primary' },
      ],
      insights: [
        { title: '本月权限调整频次较高', description: '建议在月底前梳理一次角色矩阵，避免权限项持续扩张。', type: 'warning' },
        { title: '配置审计已形成闭环', description: '关键参数调整均有记录，可继续沉淀标准化变更模板。', type: 'success' },
        { title: '通知分发更加稳定', description: '维护公告与制度公告的点击和阅读情况都优于上月。', type: 'primary' },
      ],
    },
    term: {
      summary: [
        { label: '账号启用率', value: '93%', trend: '较上学期 +5%', iconKey: 'account' },
        { label: '权限命中率', value: '97%', trend: '风险持续下降', iconKey: 'governance' },
        { label: '配置变更', value: '126', trend: '学期累计', iconKey: 'config' },
        { label: '通知触达率', value: '90%', trend: '较上学期 +6%', iconKey: 'notice' },
      ],
      trend: {
        name: '活跃账号',
        unit: '个',
        categories: ['2 月', '3 月', '4 月', '5 月', '6 月'],
        values: [58, 83, 91, 95, 88],
        color: '#4f46e5',
      },
      compare: {
        name: '完成度',
        unit: '%',
        categories: ['用户权限', '系统设置', '通知公告', '统计服务'],
        values: [97, 94, 90, 86],
        color: '#6366f1',
      },
      distribution: {
        name: '角色分布',
        data: [
          { name: '实验室管理员', value: 17 },
          { name: '教师', value: 74 },
          { name: '维修人员', value: 11 },
          { name: '系统管理员', value: 6 },
        ],
      },
      ranking: {
        name: '变更次数',
        unit: '次',
        categories: ['权限调整', '系统参数', '日志策略', '通知发布'],
        values: [66, 34, 18, 15],
        color: '#8b5cf6',
      },
      details: [
        { metric: '学期新增账号', value: '42 个', change: '+9', owner: '用户权限', status: '上升', tagType: 'success' },
        { metric: '角色矩阵复核次数', value: '5 次', change: '+2', owner: '角色权限', status: '稳定', tagType: 'info' },
        { metric: '配置灰度发布次数', value: '11 次', change: '+3', owner: '系统设置', status: '优化', tagType: 'primary' },
        { metric: '通知阅读率', value: '90%', change: '+6%', owner: '通知公告', status: '关注', tagType: 'warning' },
      ],
      insights: [
        { title: '教师角色成为主要活跃用户', description: '教师账号的活跃度持续提升，平台后续公告与功能文案应优先面向教学场景优化。', type: 'primary' },
        { title: '权限治理压力集中在学期初', description: '3 月和 4 月配置变更最多，建议将角色和菜单初始化流程提前标准化。', type: 'warning' },
        { title: '平台治理整体趋稳', description: '启用率、命中率和触达率都较上学期更平稳，说明治理策略已经生效。', type: 'success' },
      ],
    },
  },
}

const route = useRoute()
const router = useRouter()
const activePeriod = ref('week')
const dashboardOverview = ref(null)
const linkedStatistics = ref(null)

const roleScope = computed(() => route.meta?.roleScope || DEFAULT_AUTH_ROLE)
const pageConfig = computed(() => (roleScope.value === 'systemAdmin' ? SYSTEM_ADMIN_PAGE : LAB_ADMIN_PAGE))
const basePeriodData = computed(() => pageConfig.value.periods[activePeriod.value] || pageConfig.value.periods.week)
const activePeriodLabel = computed(
  () => PERIOD_OPTIONS.find((item) => item.value === activePeriod.value)?.label || PERIOD_OPTIONS[0].label,
)
const dataSourceLabel = computed(() => {
  if (roleScope.value === 'systemAdmin') {
    return dashboardOverview.value ? '平台概览接口' : '预置治理口径'
  }

  return linkedStatistics.value ? '业务接口联动' : '本地统计口径'
})
const toolbarMetaItems = computed(() => [
  { label: '统计口径', value: activePeriodLabel.value },
  { label: '数据来源', value: dataSourceLabel.value },
  { label: '分析模块', value: '4 组图表' },
])
const LOOP_ICON_KEYS = ['schedule', 'repair', 'device', 'usage', 'config', 'notice']
const LOOP_STATUS_TYPE = {
  success: 'success',
  warning: 'warning',
  danger: 'danger',
  primary: 'primary',
  info: 'info',
}

const normalizeLoopNumber = (value) => Number(value || 0)
const getLoopData = () => dashboardOverview.value?.businessLoop || {}
const buildLoopCards = (cards = []) =>
  cards.slice(0, 4).map((card, index) => ({
    label: card.label,
    value: String(card.value ?? '-'),
    trend: card.trend || '\u540e\u7aef\u5b9e\u65f6\u95ed\u73af',
    iconKey: LOOP_ICON_KEYS[index] || 'usage',
  }))

const buildLoopDetails = (chains = []) =>
  chains.map((chain) => ({
    metric: chain.title,
    value: `${normalizeLoopNumber(chain.completed)}/${normalizeLoopNumber(chain.total)}`,
    change: chain.rate || '-',
    owner: chain.owner || '\u4e1a\u52a1\u95ed\u73af',
    status: chain.status || '\u5df2\u540c\u6b65',
    tagType: LOOP_STATUS_TYPE[chain.tagType] || 'primary',
  }))

const buildLoopInsights = (actions = []) =>
  actions.length
    ? actions.slice(0, 3).map((action) => ({
        title: `${action.title} ${action.count ?? ''}`,
        description: action.description,
        type: 'warning',
      }))
    : [
        {
          title: '\u95ed\u73af\u72b6\u6001\u7a33\u5b9a',
          description: '\u5404\u4e3b\u8981\u4e1a\u52a1\u94fe\u8def\u5df2\u5f62\u6210\u64cd\u4f5c\u7559\u75d5\u548c\u6570\u636e\u56de\u6d41\u3002',
          type: 'success',
        },
      ]

const buildLoopTrend = (usageTrend = [], fallbackTrend) => {
  if (!Array.isArray(usageTrend) || usageTrend.length === 0) return fallbackTrend
  return {
    ...fallbackTrend,
    name: '\u4f7f\u7528\u8bb0\u5f55',
    unit: '\u6761',
    categories: usageTrend.map((item) => item.name),
    values: usageTrend.map((item) => normalizeLoopNumber(item.value)),
  }
}

const buildLoopRanking = (repairFaultRanking = [], fallbackRanking) => {
  if (!Array.isArray(repairFaultRanking) || repairFaultRanking.length === 0) return fallbackRanking
  return {
    ...fallbackRanking,
    name: '\u6545\u969c\u6b21\u6570',
    unit: '\u6b21',
    categories: repairFaultRanking.map((item) => item.name),
    values: repairFaultRanking.map((item) => normalizeLoopNumber(item.value)),
  }
}

const normalizeResponseList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.records)) return payload.data.records
  if (Array.isArray(payload?.data?.items)) return payload.data.items
  if (Array.isArray(payload?.records)) return payload.records
  if (Array.isArray(payload?.items)) return payload.items
  if (Array.isArray(payload?.list)) return payload.list
  return []
}

const loadLinkedRecords = async (endpoint) => {
  try {
    const { data } = await http.get(endpoint)
    return normalizeResponseList(data)
  } catch (error) {
    console.warn(`统计联动数据加载失败：${endpoint}`, error)
    return []
  }
}

const loadLinkedPayload = async (endpoint) => {
  try {
    const { data } = await http.get(endpoint)
    return data?.data || data || {}
  } catch (error) {
    console.warn(`统计联动数据加载失败：${endpoint}`, error)
    return {}
  }
}

const toText = (value) => String(value ?? '').trim()
const percentText = (value, total) => `${total ? Math.round((value / total) * 100) : 0}%`

const hasStatus = (record, keywords) => {
  const status = [
    record.status,
    record.processStatus,
    record.confirmStatus,
    record.assetStatus,
    record.deviceStatus,
    record.onlineStatus,
    record.resultStatus,
  ]
    .map(toText)
    .filter(Boolean)
    .join(' ')
  return keywords.some((keyword) => status.includes(keyword))
}

const getLabNameFromRecord = (record) =>
  toText(
    record.lab ||
      record.labName ||
      record.assignedLabName ||
      record.resource ||
      record.classroom ||
      record.roomName ||
      record.location ||
      record.name,
  )

const normalizeLabText = (value) =>
  toText(value)
    .toLowerCase()
    .replace(/\s+/g, '')
    .replace(/[（）()【】\[\]_-]/g, '')

const getLabDisplayName = (lab) => {
  const code = toText(lab.labCode || lab.code)
  const name = toText(lab.labName || lab.name)
  if (code && name && !name.includes(code)) return `${code} ${name}`
  return name || code
}

const createLabResolver = (labs = []) => {
  const exactMap = new Map()
  const idMap = new Map()
  const labEntries = labs
    .map((lab) => {
      const displayName = getLabDisplayName(lab)
      if (displayName && lab.id != null) idMap.set(String(lab.id), displayName)
      const aliases = [
        displayName,
        lab.labName,
        lab.name,
        lab.labCode,
        lab.code,
        lab.roomNo,
        lab.roomName,
        lab.location,
        lab.building && lab.roomNo ? `${lab.building}${lab.roomNo}` : '',
      ]
        .map(normalizeLabText)
        .filter(Boolean)

      aliases.forEach((alias) => exactMap.set(alias, displayName))
      return { displayName, aliases }
    })
    .filter((entry) => entry.displayName && entry.aliases.length)

  return (record) => {
    const labId = record.labId ?? record.assignedLabId
    if (labId != null && idMap.has(String(labId))) return idMap.get(String(labId))

    const rawName = getLabNameFromRecord(record)
    const normalizedName = normalizeLabText(rawName)
    if (!normalizedName) return ''

    if (exactMap.has(normalizedName)) return exactMap.get(normalizedName)

    const matchedLab = labEntries.find(({ aliases }) =>
      aliases.some((alias) => alias.length >= 2 && normalizedName.includes(alias)),
    )
    return matchedLab?.displayName || ''
  }
}

const createLabUsageRows = (labs = [], records = [], resolver) => {
  const counter = new Map()
  records.forEach((record) => {
    const labName = resolver(record)
    if (!labName) return
    counter.set(labName, (counter.get(labName) || 0) + 1)
  })

  return labs
    .map(getLabDisplayName)
    .filter(Boolean)
    .map((name) => ({ name, value: counter.get(name) || 0 }))
    .sort((left, right) => right.value - left.value || left.name.localeCompare(right.name, 'zh-Hans-CN'))
}

const getRepairName = (record) =>
  toText(record.faultType || record.problemType || record.reason || record.type || record.device || record.deviceName || record.status)

const getPeriodCategories = (period) => {
  if (period === 'month') return ['第 1 周', '第 2 周', '第 3 周', '第 4 周', '第 5 周']
  if (period === 'term') return getTermMonths().map((month) => `${month} 月`)
  return ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
}

const WEEKDAY_ORDER = {
  星期一: 1,
  星期二: 2,
  星期三: 3,
  星期四: 4,
  星期五: 5,
  星期六: 6,
  星期日: 7,
  星期天: 7,
  周一: 1,
  周二: 2,
  周三: 3,
  周四: 4,
  周五: 5,
  周六: 6,
  周日: 7,
  周天: 7,
}

const parseNumberList = (value) => {
  const numbers = new Set()
  String(value || '')
    .split(',')
    .forEach((part) => {
      const range = part.match(/(\d{1,2})\s*[-~至]\s*(\d{1,2})/)
      if (range) {
        const start = Number(range[1])
        const end = Number(range[2])
        for (let number = Math.min(start, end); number <= Math.max(start, end); number += 1) {
          numbers.add(number)
        }
        return
      }

      const single = part.match(/\d{1,2}/)
      if (single) numbers.add(Number(single[0]))
    })
  return [...numbers].filter((number) => number > 0)
}

const getTimetableWeeks = (record) => {
  const expanded = parseNumberList(record.weekExpanded)
  if (expanded.length) return expanded
  return parseNumberList(record.weekText || record.weekRaw)
}

const getTimetableWeekdayIndex = (record) => {
  const weekday = toText(record.weekday)
  if (WEEKDAY_ORDER[weekday]) return WEEKDAY_ORDER[weekday]
  const match = weekday.match(/[1-7一二三四五六日天]/)
  const weekdayMap = { 一: 1, 二: 2, 三: 3, 四: 4, 五: 5, 六: 6, 日: 7, 天: 7 }
  return match ? Number(match[0]) || weekdayMap[match[0]] || 0 : 0
}

const parseDateText = (value) => {
  const text = toText(value).replace(/\//g, '-')
  const match = text.match(/(\d{4})-(\d{1,2})-(\d{1,2})/)
  if (!match) return null

  const date = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]))
  return Number.isNaN(date.getTime()) ? null : date
}

const getDefaultSemesterStartDate = () => {
  const now = new Date()
  const month = now.getMonth()
  if (month >= 7) return new Date(now.getFullYear(), 7, 1)
  if (month === 0) return new Date(now.getFullYear() - 1, 7, 1)
  return new Date(now.getFullYear(), 1, 1)
}

const formatDateKey = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getRecordTimeText = (record) =>
  toText(
    record.entryTime ||
      record.scheduleDate ||
      record.date ||
      record.applyDate ||
      record.reservationDate ||
      record.time ||
      record.useTime ||
      record.createdAt ||
      record.createdTime ||
      record.updatedAt ||
      record.updatedTime ||
      record.startTime ||
      record.reservationTime ||
      record.inspectedAt ||
      record.reportedAt,
  )

const parseRecordDate = (record) => {
  const text = getRecordTimeText(record).replace(/\//g, '-').replace('T', ' ')
  if (!text || text === '-') return null

  const fullMatch = text.match(/(\d{4})-(\d{1,2})-(\d{1,2})/)
  const shortMatch = text.match(/(^|\D)(\d{1,2})-(\d{1,2})(?=\D|$)/)
  const match = fullMatch || shortMatch
  if (!match) return null

  const year = fullMatch ? Number(match[1]) : new Date().getFullYear()
  const month = fullMatch ? Number(match[2]) : Number(match[2])
  const day = fullMatch ? Number(match[3]) : Number(match[3])
  const timeMatch = text.match(/(\d{1,2}):(\d{2})/)
  const hour = timeMatch ? Number(timeMatch[1]) : 0
  const minute = timeMatch ? Number(timeMatch[2]) : 0
  const date = new Date(year, month - 1, day, hour, minute, 0)
  return Number.isNaN(date.getTime()) ? null : date
}

const getPeriodRange = (period) => {
  const now = new Date()
  const end = new Date(now)
  end.setHours(23, 59, 59, 999)

  if (period === 'month') {
    return {
      start: new Date(now.getFullYear(), now.getMonth(), 1),
      end,
    }
  }

  if (period === 'term') {
    const currentMonth = now.getMonth() + 1
    const startMonth = currentMonth >= 8 || currentMonth === 1 ? 8 : 2
    const startYear = currentMonth === 1 && startMonth === 8 ? now.getFullYear() - 1 : now.getFullYear()
    return {
      start: new Date(startYear, startMonth - 1, 1),
      end,
    }
  }

  const start = new Date(end)
  start.setDate(end.getDate() - 6)
  start.setHours(0, 0, 0, 0)
  return { start, end }
}

const filterRecordsByPeriod = (records, period) => {
  const { start, end } = getPeriodRange(period)
  return records.filter((record) => {
    const date = parseRecordDate(record)
    return date && date >= start && date <= end
  })
}

const expandTimetableOccurrences = (records, period, semesterStartDateText) => {
  const { start, end } = getPeriodRange(period)
  const semesterStart = parseDateText(semesterStartDateText) || getDefaultSemesterStartDate()
  const occurrences = []

  records.forEach((record) => {
    const weekdayIndex = getTimetableWeekdayIndex(record)
    if (!weekdayIndex) return

    getTimetableWeeks(record).forEach((week) => {
      const scheduleDate = new Date(semesterStart)
      scheduleDate.setDate(semesterStart.getDate() + (week - 1) * 7 + weekdayIndex - 1)
      scheduleDate.setHours(0, 0, 0, 0)
      if (scheduleDate < start || scheduleDate > end) return

      occurrences.push({
        ...record,
        sourceType: '课表',
        scheduleDate: formatDateKey(scheduleDate),
      })
    })
  })

  return occurrences
}

const getTermMonths = () => {
  const currentMonth = new Date().getMonth() + 1
  return currentMonth >= 8 || currentMonth === 1 ? [8, 9, 10, 11, 12, 1] : [2, 3, 4, 5, 6]
}

const inferBucketIndex = (record, period, categories) => {
  const date = parseRecordDate(record)
  if (!date) return -1

  if (period === 'month') {
    return Math.min(categories.length - 1, Math.max(0, Math.floor((date.getDate() - 1) / 7)))
  }

  if (period === 'term') {
    return getTermMonths().indexOf(date.getMonth() + 1)
  }

  return (date.getDay() + 6) % 7
}

const buildPeriodSeries = (records, period) => {
  const categories = getPeriodCategories(period)
  const values = Array(categories.length).fill(0)

  records.forEach((record) => {
    const index = inferBucketIndex(record, period, categories)
    if (index >= 0) values[index] += 1
  })

  return {
    name: '实验室使用',
    unit: '条',
    categories,
    values,
    color: '#2563eb',
  }
}

const topCountRows = (records, resolver, limit = 5) => {
  const counter = new Map()
  records.forEach((record) => {
    const key = resolver(record)
    if (!key) return
    counter.set(key, (counter.get(key) || 0) + 1)
  })
  return Array.from(counter.entries())
    .sort((a, b) => b[1] - a[1])
    .slice(0, limit)
    .map(([name, value]) => ({ name, value }))
}

const getDeviceStatus = (record) => toText(record.status || record.deviceStatus || record.assetStatus || '未标记状态')

const isDeviceDeleted = (record) =>
  record.deleted === true || record.isDeleted === true || hasStatus(record, ['已删除'])

const isDeviceOnline = (record) => {
  const rawOnline = record.online ?? record.isOnline ?? record.onlineStatus ?? record.networkStatus ?? record.connectStatus

  if (typeof rawOnline === 'boolean') return rawOnline

  const onlineText = toText(rawOnline).toLowerCase()
  if (['true', '1', 'yes', 'y', 'online'].includes(onlineText) || onlineText.includes('在线')) return true
  if (['false', '0', 'no', 'n', 'offline'].includes(onlineText) || onlineText.includes('离线')) return false

  const status = getDeviceStatus(record)
  if (['故障', '维修', '维护', '停用', '报废', '删除', '缺失', '损坏'].some((keyword) => status.includes(keyword))) {
    return false
  }

  return ['正常', '可用', '借用中', '已盘点'].some((keyword) => status.includes(keyword))
}

const buildLinkedStatistics = (records, period) => {
  const labs = records.labs || []
  const devices = (records.devices || []).filter((record) => !isDeviceDeleted(record))
  const reservations = records.reservations || []
  const usageRecords = records.usageRecords || []
  const classTimetables = records.classTimetables || []
  const timetableConfig = records.timetableConfig || {}
  const repairs = records.repairs || []
  const courseEnvironments = records.courseEnvironments || []
  const templates = records.templates || []
  const periodUsageRecords = filterRecordsByPeriod(usageRecords, period)
  const periodReservations = filterRecordsByPeriod(reservations, period)
  const periodTimetableOccurrences = expandTimetableOccurrences(
    classTimetables,
    period,
    timetableConfig.semesterStartDate,
  )
  const periodRepairs = filterRecordsByPeriod(repairs, period)
  const periodCourseEnvironments = filterRecordsByPeriod(courseEnvironments, period)

  const approvedReservations = periodReservations.filter((record) =>
    hasStatus(record, ['已通过', '通过', '已确认', '预约成功', '正常']),
  )
  const pendingReservations = periodReservations.filter((record) => hasStatus(record, ['待审', '待审核', '待确认']))
  const pendingEnvironments = periodCourseEnvironments.filter((record) => !hasStatus(record, ['已确认', '可复用', '已完成']))
  const resolveLabName = createLabResolver(labs)
  const labUsageSourceRecords = [...periodUsageRecords, ...approvedReservations, ...periodTimetableOccurrences]
  const labUsageRecords = labUsageSourceRecords.filter(resolveLabName)
  const labTimetableOccurrences = periodTimetableOccurrences.filter(resolveLabName)
  const labUsageRecordCount = periodUsageRecords.filter(resolveLabName).length
  const labApprovedReservationCount = approvedReservations.filter(resolveLabName).length
  const labRepairs = periodRepairs.filter(resolveLabName)
  const activeLabRepairs = labRepairs.filter((record) => !hasStatus(record, ['已完成', '已关闭', '完成', '关闭', '已归档']))
  const labDevices = devices.filter(resolveLabName)
  const onlineLabDevices = labDevices.filter(isDeviceOnline)
  const abnormalLabDevices = labDevices.filter((record) =>
    hasStatus(record, ['故障', '维修', '维护', '停用', '报废', '盘点异常', '设备缺失', '设备损坏']),
  )
  const labUsageRows = createLabUsageRows(labs, labUsageRecords, resolveLabName)
  const activeLabUsageRows = labUsageRows.filter((item) => item.value > 0)
  const labUsageCompareRows = labUsageRows.slice(0, 5)
  const repairRows = topCountRows(labRepairs, getRepairName, 4)
  const deviceStatusRows = topCountRows(labDevices, getDeviceStatus, 6)
  const labCategories = labUsageCompareRows.map((item) => item.name)

  return {
    summary: [
      {
        label: '实验室使用',
        value: String(labUsageRecords.length),
        trend: `含课表 ${labTimetableOccurrences.length} 节`,
        iconKey: 'usage',
      },
      {
        label: '预约/课表',
        value: String(labApprovedReservationCount + labTimetableOccurrences.length),
        trend: `预约通过 ${labApprovedReservationCount} 条`,
        iconKey: 'schedule',
      },
      {
        label: '设备在线率',
        value: percentText(onlineLabDevices.length, labDevices.length),
        trend: `异常 ${abnormalLabDevices.length} 台`,
        iconKey: 'device',
      },
      {
        label: '环境需求',
        value: String(periodCourseEnvironments.length),
        trend: `模板 ${templates.length} 个`,
        iconKey: 'config',
      },
    ],
    trend: buildPeriodSeries(labUsageRecords, period),
    compare: {
      name: '实验室使用',
      unit: '条',
      categories: labCategories,
      values: labUsageCompareRows.length ? labUsageCompareRows.map((item) => item.value) : labCategories.map(() => 0),
      color: '#3b82f6',
    },
    distribution: {
      name: '设备状态',
      data: deviceStatusRows,
    },
    ranking: {
      name: '故障/工单数',
      unit: '单',
      categories: repairRows.map((item) => item.name),
      values: repairRows.map((item) => item.value),
      color: '#f59e0b',
    },
    details: [
      {
        metric: '期间实验室使用',
        value: `${labUsageRecords.length} 条`,
        change: `使用 ${labUsageRecordCount} / 预约 ${labApprovedReservationCount} / 课表 ${labTimetableOccurrences.length}`,
        owner: '使用记录',
        status: labUsageRecords.length ? '有数据' : '暂无',
        tagType: labUsageRecords.length ? 'success' : 'info',
      },
      {
        metric: '期间预约记录',
        value: `${periodReservations.length} 条`,
        change: `${approvedReservations.length} 条已通过`,
        owner: '预约管理',
        status: pendingReservations.length ? '待审' : '稳定',
        tagType: pendingReservations.length ? 'warning' : 'success',
      },
      {
        metric: '设备资产总数',
        value: `${labDevices.length} 台`,
        change: `${onlineLabDevices.length} 台在线`,
        owner: '设备资产',
        status: abnormalLabDevices.length ? '关注' : '稳定',
        tagType: abnormalLabDevices.length ? 'warning' : 'success',
      },
      {
        metric: '实验室覆盖数量',
        value: `${labs.length} 间`,
        change: `${activeLabUsageRows.length} 间有记录`,
        owner: '实验室管理',
        status: labs.length ? '已联动' : '待同步',
        tagType: labs.length ? 'success' : 'warning',
      },
      {
        metric: '期间课表数据',
        value: `${labTimetableOccurrences.length} 节`,
        change: `课表源 ${classTimetables.length} 条`,
        owner: '课表数据',
        status: labTimetableOccurrences.length ? '已纳入' : '暂无',
        tagType: labTimetableOccurrences.length ? 'success' : 'info',
      },
      {
        metric: '处理中工单',
        value: `${activeLabRepairs.length} 单`,
        change: `期间 ${labRepairs.length} 单`,
        owner: '故障报修',
        status: activeLabRepairs.length ? '处理中' : '稳定',
        tagType: activeLabRepairs.length ? 'primary' : 'success',
      },
      {
        metric: '待处理环境需求',
        value: `${pendingEnvironments.length} 条`,
        change: `${templates.length} 个模板可复用`,
        owner: '环境运维',
        status: pendingEnvironments.length ? '关注' : '稳定',
        tagType: pendingEnvironments.length ? 'warning' : 'info',
      },
    ],
    insights: [
      {
        title: activeLabUsageRows[0] ? `${activeLabUsageRows[0].name} 使用最集中` : '暂无期间使用热点',
        description: activeLabUsageRows[0]
          ? `该实验室本时间范围内关联 ${activeLabUsageRows[0].value} 条使用/通过预约/课表记录。`
          : '当前时间范围内接口未返回可匹配实验室基础数据的使用、预约或课表记录。',
        type: activeLabUsageRows[0] ? 'primary' : 'info',
      },
      {
        title: activeLabRepairs.length ? '仍有工单待闭环' : '当前无待闭环工单',
        description: activeLabRepairs.length
          ? `本时间范围内存在 ${activeLabRepairs.length} 单实验室未闭环报修。`
          : '当前时间范围内接口未返回待处理维修工单。',
        type: activeLabRepairs.length ? 'warning' : 'success',
      },
      {
        title: pendingEnvironments.length ? '课程环境需继续确认' : '暂无待处理环境需求',
        description: pendingEnvironments.length
          ? `本时间范围内 ${pendingEnvironments.length} 条课程环境需求尚未完成确认，当前模板 ${templates.length} 个。`
          : `当前时间范围内接口未返回待处理环境需求，系统模板数 ${templates.length} 个。`,
        type: pendingEnvironments.length ? 'warning' : 'info',
      },
    ],
  }
}

const periodData = computed(() => {
  if (roleScope.value !== 'labAdmin') return basePeriodData.value

  return linkedStatistics.value || buildLinkedStatistics({}, activePeriod.value)
})
const summaryCards = computed(() =>
  periodData.value.summary.map((item) => ({
    ...item,
    icon: SUMMARY_ICON_MAP[item.iconKey] || DataAnalysis,
  })),
)

const trendChartRef = ref(null)
const compareChartRef = ref(null)
const distributionChartRef = ref(null)
const rankingChartRef = ref(null)

const formatValue = (value, unit) => `${value}${unit || ''}`
const wrapAxisLabel = (value, lineLength = 8) => {
  const text = String(value ?? '')
  if (text.length <= lineLength) return text

  const lines = []
  for (let index = 0; index < text.length; index += lineLength) {
    lines.push(text.slice(index, index + lineLength))
  }
  return lines.join('\n')
}

const buildTrendOption = () => ({
  color: [periodData.value.trend.color],
  tooltip: {
    trigger: 'axis',
    valueFormatter: (value) => formatValue(value, periodData.value.trend.unit),
  },
  grid: {
    left: 32,
    right: 20,
    top: 34,
    bottom: 26,
    containLabel: true,
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: periodData.value.trend.categories,
    axisLine: { lineStyle: { color: '#d8e0ec' } },
    axisLabel: { color: '#64748b' },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { type: 'dashed', color: '#e2e8f0' } },
    axisLabel: {
      color: '#64748b',
      formatter: (value) => formatValue(value, periodData.value.trend.unit),
    },
  },
  series: [
    {
      name: periodData.value.trend.name,
      type: 'line',
      smooth: true,
      symbolSize: 8,
      data: periodData.value.trend.values,
      lineStyle: {
        width: 3,
        color: periodData.value.trend.color,
      },
      itemStyle: {
        color: periodData.value.trend.color,
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: `${periodData.value.trend.color}55` },
          { offset: 1, color: `${periodData.value.trend.color}08` },
        ]),
      },
    },
  ],
})

const buildCompareOption = () => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    valueFormatter: (value) => formatValue(value, periodData.value.compare.unit),
  },
  grid: {
    left: 34,
    right: 18,
    top: 34,
    bottom: 58,
    containLabel: true,
  },
  xAxis: {
    type: 'category',
    data: periodData.value.compare.categories,
    axisLine: { lineStyle: { color: '#d8e0ec' } },
    axisLabel: {
      color: '#64748b',
      interval: 0,
      lineHeight: 16,
      hideOverlap: false,
      formatter: (value) => wrapAxisLabel(value, 8),
    },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { type: 'dashed', color: '#e2e8f0' } },
    axisLabel: {
      color: '#64748b',
      formatter: (value) => formatValue(value, periodData.value.compare.unit),
    },
  },
  series: [
    {
      name: periodData.value.compare.name,
      type: 'bar',
      barWidth: 28,
      data: periodData.value.compare.values,
      itemStyle: {
        borderRadius: [8, 8, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: periodData.value.compare.color },
          { offset: 1, color: '#c7d2fe' },
        ]),
      },
    },
  ],
})

const buildDistributionOption = () => ({
  color: ['#16a34a', '#f59e0b', '#ef4444', '#60a5fa'],
  tooltip: {
    trigger: 'item',
    formatter: '{b}<br />{c}（{d}%）',
  },
  legend: {
    bottom: 0,
    left: 'center',
    itemWidth: 10,
    itemHeight: 10,
    textStyle: {
      color: '#64748b',
      fontSize: 12,
    },
  },
  series: [
    {
      name: periodData.value.distribution.name,
      type: 'pie',
      radius: ['44%', '68%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderColor: '#ffffff',
        borderWidth: 3,
      },
      label: {
        show: false,
      },
      labelLine: {
        show: false,
      },
      data: periodData.value.distribution.data,
    },
  ],
})

const buildRankingOption = () => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    valueFormatter: (value) => formatValue(value, periodData.value.ranking.unit),
  },
  grid: {
    left: 90,
    right: 20,
    top: 28,
    bottom: 20,
    containLabel: true,
  },
  xAxis: {
    type: 'value',
    splitLine: { lineStyle: { type: 'dashed', color: '#e2e8f0' } },
    axisLabel: {
      color: '#64748b',
      formatter: (value) => formatValue(value, periodData.value.ranking.unit),
    },
  },
  yAxis: {
    type: 'category',
    data: periodData.value.ranking.categories,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#475569' },
  },
  series: [
    {
      name: periodData.value.ranking.name,
      type: 'bar',
      data: periodData.value.ranking.values,
      barWidth: 18,
      itemStyle: {
        borderRadius: [0, 8, 8, 0],
        color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
          { offset: 0, color: periodData.value.ranking.color },
          { offset: 1, color: '#fde68a' },
        ]),
      },
    },
  ],
})

const { renderChart: renderTrendChart, resizeChart: resizeTrendChart } = useECharts(
  trendChartRef,
  buildTrendOption,
)
const { renderChart: renderCompareChart, resizeChart: resizeCompareChart } = useECharts(
  compareChartRef,
  buildCompareOption,
)
const { renderChart: renderDistributionChart, resizeChart: resizeDistributionChart } = useECharts(
  distributionChartRef,
  buildDistributionOption,
)
const { renderChart: renderRankingChart, resizeChart: resizeRankingChart } = useECharts(
  rankingChartRef,
  buildRankingOption,
)

const renderAllCharts = async () => {
  await Promise.all([
    renderTrendChart(),
    renderCompareChart(),
    renderDistributionChart(),
    renderRankingChart(),
  ])
}

const handleWindowResize = () => {
  resizeTrendChart()
  resizeCompareChart()
  resizeDistributionChart()
  resizeRankingChart()
}

const createChartRows = (chart, categoryLabel) =>
  (chart.categories || []).map((category, index) => ({
    section: categoryLabel,
    name: category,
    value: formatValue(chart.values?.[index] ?? '', chart.unit),
    description: chart.name,
  }))

const exportStatisticsData = () => {
  const data = periodData.value
  const rows = [
    ...data.summary.map((item) => ({
      section: '核心指标',
      name: item.label,
      value: item.value,
      description: item.trend,
    })),
    ...createChartRows(data.trend, pageConfig.value.charts.trend.title),
    ...createChartRows(data.compare, pageConfig.value.charts.compare.title),
    ...(data.distribution.data || []).map((item) => ({
      section: pageConfig.value.charts.distribution.title,
      name: item.name,
      value: item.value,
      description: data.distribution.name,
    })),
    ...createChartRows(data.ranking, pageConfig.value.charts.ranking.title),
    ...(data.details || []).map((item) => ({
      section: pageConfig.value.detailTitle,
      name: item.metric,
      value: item.value,
      description: `${item.change || ''} ${item.owner || ''} ${item.status || ''}`.trim(),
    })),
  ]

  if (!rows.length) {
    ElMessage.warning('暂无可导出的统计数据')
    return
  }

  const exportedCount = exportTableToExcel({
    filename: `${pageConfig.value.title}-统计数据`,
    sheetName: '统计数据',
    rows,
    columns: [
      { label: '数据区块', prop: 'section' },
      { label: '指标/维度', prop: 'name' },
      { label: '数值', prop: 'value' },
      { label: '说明', prop: 'description' },
    ],
  })
  ElMessage.success(`已导出 ${exportedCount} 条统计数据`)
}

const handleHeroAction = (action) => {
  if (action.command === 'report') {
    loadStatisticsData()
    ElMessage.success('统计报表已加入生成队列')
    return
  }

  if (action.command === 'export') {
    exportStatisticsData()
    return
  }

  ElMessage.success(`${action.label}已处理`)
}

const handleBack = () => {
  const target = AUTH_ROLE_DEFAULT_PATH[roleScope.value] || AUTH_ROLE_DEFAULT_PATH[DEFAULT_AUTH_ROLE]
  router.push(target)
}

const loadDashboardOverview = async () => {
  try {
    dashboardOverview.value = await fetchDashboardOverview()
  } catch (error) {
    ElMessage.warning(error?.message || '实时统计加载失败，已使用本地统计口径')
  }
}

const loadBusinessStatistics = async () => {
  if (roleScope.value !== 'labAdmin') {
    linkedStatistics.value = null
    return
  }

  const [
    labs,
    devices,
    reservations,
    usageRecords,
    classTimetables,
    timetableConfig,
    repairs,
    courseEnvironments,
    templates,
  ] = await Promise.all([
    loadLinkedRecords(API_ENDPOINTS.labs.list),
    loadLinkedRecords(API_ENDPOINTS.devices.list),
    loadLinkedRecords(API_ENDPOINTS.reservations.list),
    loadLinkedRecords(API_ENDPOINTS.usageRecords.list),
    loadLinkedRecords(`${API_ENDPOINTS.classTimetables.list}?limit=5000`),
    loadLinkedPayload(API_ENDPOINTS.classTimetables.crawlerConfig),
    loadLinkedRecords(API_ENDPOINTS.repairs.list),
    loadLinkedRecords(API_ENDPOINTS.courseEnvironmentRequests.list),
    loadLinkedRecords(API_ENDPOINTS.environmentTemplates.list),
  ])

  linkedStatistics.value = buildLinkedStatistics(
    {
      labs,
      devices,
      reservations,
      usageRecords,
      classTimetables,
      timetableConfig,
      repairs,
      courseEnvironments,
      templates,
    },
    activePeriod.value,
  )
}

const loadStatisticsData = async () => {
  if (roleScope.value === 'labAdmin') {
    await loadBusinessStatistics()
  } else {
    await loadDashboardOverview()
  }
  await renderAllCharts()
}

watch([roleScope, activePeriod], async () => {
  await loadBusinessStatistics()
  await renderAllCharts()
}, { flush: 'post' })

watch(linkedStatistics, () => {
  renderAllCharts()
}, { flush: 'post' })

onMounted(() => {
  loadStatisticsData()
  window.addEventListener('resize', handleWindowResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleWindowResize)
})
</script>

<template>
  <section class="statistics-page">
    <PageHero
      :actions="pageConfig.actions"
      :badge="pageConfig.badge"
      :page-description="pageConfig.description"
      :page-icon="pageConfig.pageIcon"
      :page-title="pageConfig.title"
      @action="handleHeroAction"
      @back="handleBack"
    />

    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar-content">
        <div class="toolbar-copy">
          <h2>{{ pageConfig.toolbarTitle }}</h2>
        </div>
        <div class="toolbar-controls">
          <div class="toolbar-meta" aria-label="统计口径信息">
            <span v-for="item in toolbarMetaItems" :key="item.label">
              <small>{{ item.label }}</small>
              <strong>{{ item.value }}</strong>
            </span>
          </div>
          <el-radio-group v-model="activePeriod" size="small">
            <el-radio-button v-for="item in PERIOD_OPTIONS" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </div>
      </div>
    </el-card>

    <div class="summary-grid">
      <article v-for="item in summaryCards" :key="item.label" class="summary-card">
        <span class="summary-icon" aria-hidden="true">
          <component :is="item.icon" />
        </span>
        <div class="summary-copy">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.trend }}</small>
        </div>
      </article>
    </div>

    <div class="chart-grid">
      <el-card class="chart-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <h3>{{ pageConfig.charts.trend.title }}</h3>
            </div>
            <el-tag effect="light" type="primary">{{ pageConfig.charts.trend.tag }}</el-tag>
          </div>
        </template>
        <div class="chart-shell">
          <div ref="trendChartRef" class="chart-box"></div>
        </div>
      </el-card>

      <el-card class="chart-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <h3>{{ pageConfig.charts.compare.title }}</h3>
            </div>
            <el-tag effect="light" type="success">{{ pageConfig.charts.compare.tag }}</el-tag>
          </div>
        </template>
        <div class="chart-shell">
          <div ref="compareChartRef" class="chart-box"></div>
        </div>
      </el-card>

      <el-card class="chart-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <h3>{{ pageConfig.charts.distribution.title }}</h3>
            </div>
            <el-tag effect="light" type="warning">{{ pageConfig.charts.distribution.tag }}</el-tag>
          </div>
        </template>
        <div class="chart-shell">
          <div ref="distributionChartRef" class="chart-box chart-box-pie"></div>
        </div>
      </el-card>

      <el-card class="chart-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <h3>{{ pageConfig.charts.ranking.title }}</h3>
            </div>
            <el-tag effect="light">{{ pageConfig.charts.ranking.tag }}</el-tag>
          </div>
        </template>
        <div class="chart-shell">
          <div ref="rankingChartRef" class="chart-box"></div>
        </div>
      </el-card>
    </div>

    <el-row class="detail-row" :gutter="18">
      <el-col :xs="24" :xl="24">
        <el-card class="detail-card" shadow="never">
          <template #header>
            <div class="card-header">
              <div>
                <h3>{{ pageConfig.detailTitle }}</h3>
              </div>
            </div>
          </template>
          <el-table :data="periodData.details" class="detail-table" empty-text="暂无统计明细" stripe>
            <el-table-column prop="metric" label="指标" min-width="180" />
            <el-table-column prop="value" label="当前值" min-width="110" />
            <el-table-column prop="change" label="环比" min-width="110" />
            <el-table-column prop="owner" label="关注域" min-width="130" />
            <el-table-column label="状态" min-width="100">
              <template #default="{ row }">
                <el-tag :type="row.tagType || 'info'" effect="light">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>

<style scoped>
.statistics-page {
  display: grid;
  gap: 20px;
  min-width: 0;
}

.toolbar-card,
.summary-card,
.chart-card,
.detail-card {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
}

.toolbar-card,
.chart-card,
.detail-card {
  overflow: hidden;
}

.toolbar-card :deep(.el-card__body) {
  padding: 0;
}

.toolbar-content {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 20px;
  background:
    linear-gradient(90deg, rgba(37, 99, 235, 0.08), rgba(37, 99, 235, 0) 58%),
    #ffffff;
}

.toolbar-content::after {
  position: absolute;
  right: 20px;
  bottom: 0;
  left: 20px;
  height: 1px;
  background: var(--border-light);
  content: '';
}

.toolbar-copy {
  min-width: 0;
}

.toolbar-copy h2,
.card-header h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 700;
}

.summary-copy span,
.summary-copy small {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.toolbar-controls {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 16px;
}

.toolbar-meta {
  display: grid;
  grid-template-columns: repeat(3, max-content);
  gap: 8px;
}

.toolbar-meta span {
  display: grid;
  min-width: 86px;
  gap: 3px;
  padding: 9px 11px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}

.toolbar-meta small {
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1;
}

.toolbar-meta strong {
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.2;
}

.toolbar-controls :deep(.el-radio-button__inner) {
  min-width: 72px;
  font-weight: 600;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 240px), 1fr));
  gap: 16px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 104px;
  padding: 16px 18px;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.summary-card:hover {
  border-color: #bfdbfe;
  background: #fbfdff;
}

.summary-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 8px;
  color: var(--primary-color);
  background: #eff6ff;
  font-size: 24px;
}

.summary-copy {
  min-width: 0;
}

.summary-copy strong {
  display: block;
  margin-top: 8px;
  color: var(--text-primary);
  font-size: 30px;
  line-height: 1;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(360px, 1fr));
  gap: 16px;
  min-width: 0;
}

.chart-card :deep(.el-card__header),
.detail-card :deep(.el-card__header) {
  min-height: 76px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--border-light);
  background: #fbfdff;
}

.chart-card :deep(.el-card__body) {
  padding: 16px;
}

.chart-card {
  min-width: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-header > div {
  min-width: 0;
}

.card-header h3 {
  line-height: 1.35;
}

.chart-shell {
  min-width: 0;
  padding: 10px;
  overflow: hidden;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background:
    linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.chart-box {
  width: 100%;
  min-width: 0;
  height: 316px;
  min-height: 316px;
}

.chart-box-pie {
  height: 316px;
  min-height: 316px;
}

.detail-row :deep(.el-col) {
  display: flex;
  min-width: 0;
  margin-bottom: 16px;
}

.detail-card {
  width: 100%;
}

.detail-card :deep(.el-card__body) {
  padding: 16px 18px 18px;
}

.detail-table {
  --el-table-border-color: var(--border-light);
  --el-table-header-bg-color: #f8fafc;
  --el-table-header-text-color: #334155;
  border: 1px solid var(--border-light);
  border-radius: 8px;
}

.detail-table :deep(.el-table__cell) {
  padding: 12px 0;
}

@media (max-width: 1080px) {
  .chart-grid,
  .toolbar-meta {
    grid-template-columns: 1fr;
  }

  .toolbar-controls {
    align-items: flex-end;
    flex-direction: column;
  }
}

@media (max-width: 760px) {
  .toolbar-content,
  .toolbar-controls,
  .card-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-controls {
    width: 100%;
  }

  .toolbar-meta {
    width: 100%;
  }

  .toolbar-meta span {
    min-width: 0;
  }

  .toolbar-controls :deep(.el-radio-group) {
    width: 100%;
  }

  .toolbar-controls :deep(.el-radio-button) {
    flex: 1;
  }

  .toolbar-controls :deep(.el-radio-button__inner) {
    width: 100%;
    min-width: 0;
  }

  .summary-card {
    min-height: 112px;
  }

  .chart-box,
  .chart-box-pie {
    height: 280px;
    min-height: 280px;
  }
}
</style>
