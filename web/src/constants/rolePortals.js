export const LAB_ADMIN_PORTAL = {
  badge: '实验室管理员',
  digitalTwinOnly: true,
  title: '智云实验室数字孪生平台',
  description:
    '基于实验室空间、设备、课程、预约和维护记录，构建可视化的实验室运行中心。',
  stats: [
    { label: '待处理报修', value: '9', trend: '高优先级 2 单' },
    { label: '待处理维护', value: '6', trend: '管理员待跟进' },
    { label: '今日预约', value: '36', trend: '待审批 7 条' },
    { label: '课表数据', value: '36', trend: '今日已同步' },
  ],
  digitalTwin: {
    kicker: 'BIM + CORE DATA',
    title: '智云实验室可视化运行平台',
    subtitle: '融合空间模型、设备资产、预约占用、课表和维护记录，形成实验室运行视图。',
    sceneTags: ['系统总览', '设备台账', '预约占用', '课表数据', '维护记录', '统计分析'],
    summary: [
      { label: '纳管空间', value: '13', unit: '间', trend: '2 栋教学楼' },
      { label: '在线设备', value: '512', unit: '台', trend: '在线率 94%' },
      { label: '维护记录', value: '24', unit: '条', trend: '本周已归档' },
      { label: '今日课表', value: '36', unit: '节', trend: '课表与预约联动' },
    ],
    labs: [
      { code: '2-206', name: '移动互联实训室', floor: '2F', status: '使用中', usage: 88, color: '#f59e0b' },
      { code: '2-204', name: '软件开发实验实训室', floor: '2F', status: '开放', usage: 42, color: '#16a34a' },
      { code: '2-207', name: '鸿蒙应用开发实训室', floor: '2F', status: '使用中', usage: 76, color: '#f59e0b' },
      { code: '2-209', name: 'WEB后端开发实训室', floor: '2F', status: '开放', usage: 35, color: '#16a34a' },
      { code: '2-212', name: '数据库设计实训室', floor: '2F', status: '待预约', usage: 58, color: '#2563eb' },
      { code: '2-210', name: '软件工程试训室', floor: '2F', status: '维护中', usage: 12, color: '#ef4444' },
    ],
    events: [
      { level: '关注', title: '2-206 使用率偏高', desc: '当前占用率 88%，建议结合预约和课表安排进行分流。' },
      { level: '正常', title: '设备资产状态稳定', desc: '512 台设备已纳入资产台账，运行状态保持稳定。' },
      { level: '正常', title: '维护记录按期归档', desc: '本周维护记录已完成归档，异常事项均有处理结果。' },
    ],
    features: ['高质量3D渲染', '实时数据集成', '预约课表协同', '交互式资产视图', '多层级空间视图', '维护记录归档'],
    scenarios: ['实验室管理', '设备资产', '预约审批', '课表数据', '维护记录', '通知公告', '运行统计'],
    architecture: [
      { layer: '用户界面', items: ['IOC数字大屏', '3D数字孪生模型', '综合管理后台', '移动端'] },
      { layer: '功能系统', items: ['设备资产', '实验室管理', '预约审批', '课表数据', '维护记录', '统计分析'] },
      { layer: '管理平台', items: ['业务中台', '数据中台', '统一授权', '规则引擎', '操作审计', '平台统计'] },
    ],
  },
  actions: [
    { title: '系统总览', tag: 'HOME', description: '查看实验室整体运行态势。' },
    { title: '设备运维', tag: 'OPS', description: '监控设备在线和故障处置。' },
    { title: '预约占用', tag: 'BOOK', description: '联动课表与预约占用状态。' },
    { title: '维护记录', tag: 'MAINT', description: '登记、查看和导出设备维护记录。' },
    { title: '统计分析', tag: 'DATA', description: '查看实验室运行指标和趋势。' },
  ],
  recordsTitle: '今日重点业务',
  columns: ['业务类型', '对象', '时间', '说明', '状态'],
  records: [
    ['维护记录', 'B203 核心交换机', '今日 10:20', '网络端口异常，已登记维护记录', '处理中'],
    ['预约审批', '软件工程 2 班', '今日 11:00', '申请 A102 晚间开放', '待审核'],
  ],
  timeline: [
    { title: '处理当日报修与预约', date: '实时' },
    { title: '同步课表与预约数据', date: '每日' },
    { title: '更新维护记录与通知', date: '每日' },
  ],
}
export const SYSTEM_ADMIN_PORTAL = {
  badge: '系统管理员',
  title: '系统管理员工作台',
  description:
    '围绕平台治理，集中处理账号权限、系统设置、通知公告、操作审计和全局统计。',
  stats: [
    { label: '账号总数', value: '128', trend: '启用 121 个' },
    { label: '待审核账号', value: '7', trend: '待管理员复核' },
    { label: '系统通知', value: '28', trend: '本月发布' },
    { label: '配置项', value: '36', trend: '已绑定权限' },
  ],
  actions: [
    { title: '管理账号权限', tag: '账号', description: '维护系统管理员、实验室管理员、教师和教务人员账号与访问范围。' },
    { title: '复核操作审计', tag: '审计', description: '查看登录、权限变更、通知发布和导出等关键操作。' },
    { title: '发布通知公告', tag: '通知', description: '向各角色发布维护通知、制度公告和系统提醒。' },
    { title: '查看平台统计', tag: '统计', description: '检查账号、权限、访问和全局运行数据，输出治理报表。' },
  ],
  recordsTitle: '平台治理事项',
  columns: ['事项', '范围', '更新时间', '负责人', '状态'],
  records: [
    ['角色权限复核', '全平台', '今日 09:30', '陈倩', '待处理'],
    ['操作审计复核', '平台治理', '今日 10:40', '系统管理员', '进行中'],
    ['平台维护公告发布', '全角色', '今日 11:10', '系统管理员', '已发布'],
  ],
  timeline: [
    { title: '审核账号与角色权限', date: '每日' },
    { title: '更新系统配置与日志策略', date: '每周' },
    { title: '输出平台治理统计报告', date: '月度' },
  ],
}

export const TEACHER_PORTAL = {
  badge: '教师',
  title: '教师教学工作台',
  description: '查看个人课表安排和实验室地点，减少课前沟通成本。',
  stats: [
    { label: '本周课程', value: '12', trend: '覆盖 4 门课程' },
    { label: '课表记录', value: '12', trend: '覆盖 4 门课程' },
    { label: '涉及实验室', value: '3', trend: 'A101 / A102 / C305' },
    { label: '待查看通知', value: '4', trend: '含实验室安排提醒' },
  ],
  actions: [
    { title: '查看个人课表', tag: '课表', description: '按周查看本人授课安排、实验室地点和上课班级。' },
    { title: '查看校园课表', tag: '课表', description: '按课程、班级和周次核对校园课表数据。' },
  ],
  recordsTitle: '我的近期安排',
  columns: ['业务', '课程', '时间', '说明', '状态'],
  records: [
    ['课表安排', 'Java Web 程序设计', '周三 08:00', 'A101 已完成排课', '已排课'],
    ['授课安排', '人工智能基础', '周四 10:00', 'C305 GPU 实验室', '已排课'],
    ['授课安排', '数据库原理', '周五 14:00', 'A102 机房已排定', '已排课'],
  ],
  timeline: [
    { title: '核对个人课表与实验室安排', date: '每周' },
    { title: '课前核对实验室安排', date: '按课程' },
  ],
}

