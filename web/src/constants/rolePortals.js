export const LAB_ADMIN_PORTAL = {
  badge: '实验室管理员',
  digitalTwinOnly: true,
  title: '智云实验室数字孪生平台',
  description:
    '基于实验室空间、设备、课程、预约、环境与告警数据，构建实时可视化的实验室运行中心。',
  stats: [
    { label: '待处理报修', value: '9', trend: '高优先级 2 单' },
    { label: '待确认环境', value: '6', trend: '教师待确认' },
    { label: '今日预约', value: '36', trend: '待审批 7 条' },
    { label: '库存预警', value: '5', trend: '需要补货' },
  ],
  digitalTwin: {
    kicker: 'BIM + IoT + AIoT',
    title: '智云实验室数字孪生可视化平台',
    subtitle: '融合空间模型、物联感知、设备运维、预约占用与环境安全数据，形成实验室全生命周期运行视图。',
    sceneTags: ['系统总览', '环境监测', '设备台账', '预约占用', '安全告警', '能耗分析'],
    summary: [
      { label: '纳管空间', value: '13', unit: '间', trend: '2 栋教学楼' },
      { label: '在线设备', value: '512', unit: '台', trend: '在线率 94%' },
      { label: '平均温度', value: '24.1', unit: '℃', trend: '空调联动正常' },
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
      { level: '关注', title: '2-206 高负载运行', desc: '当前占用率 88%，建议关注温度与网络延迟。' },
      { level: '正常', title: '设备在线率稳定', desc: '512 台设备在线，运行监测数据保持稳定。' },
      { level: '正常', title: '环境数据正常', desc: '平均温度 24.1℃，湿度 51%，无烟雾与水浸告警。' },
    ],
    features: ['高质量3D渲染', '实时数据集成', '动态模拟预测', '交互式控制面板', '多层级空间视图', '智能告警联动'],
    scenarios: ['楼宇自控', '视频监控', '门禁管理', '信息发布', '照明控制', '能源管理', '消防报警', '实验室预约'],
    architecture: [
      { layer: '用户界面', items: ['IOC数字大屏', '3D数字孪生模型', '综合管理后台', '移动端'] },
      { layer: '功能系统', items: ['状态监测', '设备监控', '门禁管理', '预约审批', '能耗管理', '告警处置'] },
      { layer: '管理平台', items: ['业务中台', '数据中台', '统一授权', '规则引擎', 'AI分析', '平台监控'] },
      { layer: '边缘计算', items: ['边缘网关', '协议驱动', '数据解析', '实时分析', '告警联动'] },
      { layer: '智能硬件', items: ['摄像头', '门禁', '温湿度', '烟雾', 'RFID', '智能电源'] },
    ],
  },
  actions: [
    { title: '系统总览', tag: 'HOME', description: '查看实验室整体运行态势。' },
    { title: '物联感知', tag: 'IOT', description: '接入环境、门禁、用电等传感数据。' },
    { title: '设备运维', tag: 'OPS', description: '监控设备在线和故障处置。' },
    { title: '预约占用', tag: 'BOOK', description: '联动课表与预约占用状态。' },
    { title: '能耗分析', tag: 'ENERGY', description: '分析电力与空调能耗趋势。' },
    { title: '告警中心', tag: 'ALARM', description: '汇总安全与环境异常事件。' },
  ],
  recordsTitle: '今日重点业务',
  columns: ['业务类型', '对象', '时间', '说明', '状态'],
  records: [
    ['环境确认', 'A101 Java Web 课程', '今日 09:00', '已上传软件清单与截图', '待确认'],
    ['报修工单', 'B203 核心交换机', '今日 10:20', '网络端口异常，已派单', '处理中'],
    ['预约审批', '软件工程 2 班', '今日 11:00', '申请 A102 晚间开放', '待审核'],
  ],
  timeline: [
    { title: '处理当日报修与预约', date: '实时' },
    { title: '同步课程环境确认进度', date: '每日' },
    { title: '更新实验室状态与库存提醒', date: '每日' },
  ],
}
export const SYSTEM_ADMIN_PORTAL = {
  badge: '系统管理员',
  title: '系统管理员工作台',
  description:
    '围绕平台治理，集中处理账号权限、系统设置、通知公告、操作审计和全局统计。',
  stats: [],
  actions: [
    { title: '管理账号权限', tag: '账号', description: '维护系统管理员、实验室管理员、教师和教务人员账号与访问范围。' },
    { title: '配置平台参数', tag: '配置', description: '管理数据字典、日志策略、外部服务和平台基础参数。' },
    { title: '发布通知公告', tag: '通知', description: '向各角色发布维护通知、制度公告和系统提醒。' },
    { title: '查看平台统计', tag: '统计', description: '检查账号、权限、访问和全局运行数据，输出治理报表。' },
  ],
  recordsTitle: '平台治理事项',
  columns: ['事项', '范围', '更新时间', '负责人', '状态'],
  records: [],
  timeline: [
    { title: '审核账号与角色权限', date: '每日' },
    { title: '更新系统配置与日志策略', date: '每周' },
    { title: '输出平台治理统计报告', date: '月度' },
  ],
}

export const TEACHER_PORTAL = {
  badge: '教师',
  title: '教师教学工作台',
  description: '查看个人课程环境准备进度和课表安排，减少课前沟通成本。',
  stats: [
    { label: '本周课程', value: '12', trend: '覆盖 4 门课程' },
    { label: '待确认环境', value: '2', trend: '需远程查看截图' },
    { label: '涉及实验室', value: '3', trend: 'A101 / A102 / C305' },
    { label: '待查看通知', value: '4', trend: '含环境确认提醒' },
  ],
  actions: [
    { title: '查看课程环境', tag: '环境', description: '按课程查看所需软件、版本、机房分配和环境截图。' },
    { title: '查看个人课表', tag: '课表', description: '按周查看本人授课安排、实验室地点和上课班级。' },
    { title: '查看校园课表', tag: '课表', description: '按课程、班级和周次核对校园课表数据。' },
  ],
  recordsTitle: '我的近期安排',
  columns: ['业务', '课程', '时间', '说明', '状态'],
  records: [
    ['环境确认', 'Java Web 程序设计', '周三 08:00', 'A101 已上传配置截图', '待确认'],
    ['授课安排', '人工智能基础', '周四 10:00', 'C305 GPU 实验室', '已排课'],
    ['授课安排', '数据库原理', '周五 14:00', 'A102 机房已排定', '已排课'],
  ],
  timeline: [
    { title: '查看课程环境和截图说明', date: '课前' },
    { title: '核对个人课表与实验室安排', date: '每周' },
    { title: '课前核对实验室安排', date: '按课程' },
  ],
}

