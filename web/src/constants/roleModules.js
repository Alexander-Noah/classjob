export const TEACHER_COURSE_ENVIRONMENT_MODULE = {
  badge: '教学环境',
  recordName: '课程环境',
  primaryKey: 'course',
  tableTitle: '我的课程环境',
  actions: [
    { label: '远程确认', type: 'primary', command: 'batchStatus', targetStatus: '已确认' },
    { label: '导出清单', type: 'default', command: 'export' },
  ],
  statusOptions: [
    { label: '待确认', type: 'warning' },
    { label: '配置中', type: 'primary' },
    { label: '已确认', type: 'success' },
    { label: '需调整', type: 'danger' },
  ],
  defaultRecord: {
    course: '新课程环境',
    lab: '待分配实验室',
    software: '待确认软件',
    schedule: '待安排时间',
    status: '待确认',
    tagType: 'warning',
  },
  metrics: [
    { label: '待确认环境', value: '2', trend: '课前需确认' },
    { label: '本周课程', value: '12', trend: '4 门课程' },
    { label: '已复用模板', value: '5', trend: '减少重复沟通' },
  ],
  featureCards: [
    { title: '查看软件清单', tag: '清单', type: 'primary', description: '按课程查看软件名称、版本、依赖环境和截图说明。' },
    { title: '远程确认环境', tag: '确认', type: 'success', description: '教师可在线确认环境是否满足上课要求，避免课前到场反复核验。' },
    { title: '退回调整', tag: '反馈', type: 'warning', description: '如配置不满足授课需求，可附带说明退回给实验室管理员修改。' },
  ],
  tableColumns: [
    { prop: 'course', label: '课程名称', minWidth: 150 },
    { prop: 'lab', label: '实验室', minWidth: 130 },
    { prop: 'software', label: '环境清单', minWidth: 200 },
    { prop: 'schedule', label: '上课时间', minWidth: 150 },
    { prop: 'status', label: '状态', minWidth: 100, type: 'tag' },
  ],
  records: [
    { course: 'Java Web 程序设计', lab: 'A101 开发实验室', software: 'JDK 21、IDEA、MySQL', schedule: '周三 08:00', status: '待确认', tagType: 'warning' },
    { course: '人工智能基础', lab: 'C305 GPU 实验室', software: 'Python、PyTorch、CUDA', schedule: '周四 10:00', status: '已确认', tagType: 'success' },
    { course: '数据库原理', lab: 'A102 机房', software: 'MySQL、Navicat、ER 建模工具', schedule: '周五 14:00', status: '配置中', tagType: 'primary' },
  ],
  timeline: [
    { title: '查看管理员上传的环境截图', date: '课前', type: 'primary' },
    { title: '在线确认课程环境', date: '课前', type: 'success' },
    { title: '不满足时退回调整', date: '按需', type: 'warning' },
  ],
}

export const TEACHER_SCHEDULE_MODULE = {
  badge: '个人课表',
  recordName: '授课安排',
  primaryKey: 'course',
  tableTitle: '我的授课课表',
  actions: [
    { label: '导出课表', type: 'primary', command: 'export' },
    { label: '查看周次', type: 'default', command: 'showSchedule' },
  ],
  statusOptions: [
    { label: '已排课', type: 'success' },
    { label: '待确认', type: 'warning' },
    { label: '环境待确认', type: 'primary' },
  ],
  defaultRecord: {
    course: '新课程',
    className: '待安排班级',
    lab: '待安排实验室',
    time: '待安排时间',
    status: '待确认',
    tagType: 'warning',
  },
  metrics: [
    { label: '本周排课', value: '12', trend: '4 门课程' },
    { label: '涉及实验室', value: '3', trend: 'A101 / A102 / C305' },
    { label: '环境待确认', value: '2', trend: '课前完成核对' },
  ],
  featureCards: [
    { title: '按周查看课表', tag: '周视图', type: 'primary', description: '查看本周授课安排、实验室地点和班级信息。' },
    { title: '关联环境信息', tag: '环境', type: 'success', description: '课表可直接对应到课程环境准备情况和实验室安排。' },
    { title: '冲突提示', tag: '提醒', type: 'warning', description: '当实验室变更或课程时间冲突时，页面会突出提示。' },
  ],
  tableColumns: [
    { prop: 'course', label: '课程名称', minWidth: 150 },
    { prop: 'className', label: '授课班级', minWidth: 130 },
    { prop: 'lab', label: '实验室', minWidth: 130 },
    { prop: 'time', label: '授课时间', minWidth: 150 },
    { prop: 'status', label: '状态', minWidth: 100, type: 'tag' },
  ],
  records: [
    { course: 'Java Web 程序设计', className: '软件工程 2 班', lab: 'A101', time: '周三 08:00-09:40', status: '已排课', tagType: 'success' },
    { course: '人工智能基础', className: '数据科学 1 班', lab: 'C305', time: '周四 10:00-11:40', status: '已排课', tagType: 'success' },
    { course: '数据库原理', className: '软件工程 3 班', lab: 'A102', time: '周五 14:00-15:40', status: '已排课', tagType: 'success' },
  ],
  timeline: [
    { title: '每周核对授课安排', date: '每周', type: 'primary' },
    { title: '同步环境准备状态', date: '课前', type: 'success' },
    { title: '课前核对实验室安排', date: '按课程', type: 'warning' },
  ],
}

export const MAINTENANCE_WORK_ORDER_MODULE = {
  badge: '维修工单',
  recordName: '工单',
  primaryKey: 'ticket',
  tableTitle: '我的维修工单',
  actions: [
    { label: '开始处理', type: 'primary', command: 'batchStatus', targetStatus: '处理中' },
    { label: '导出工单', type: 'default', command: 'export' },
  ],
  statusOptions: [
    { label: '待接单', type: 'danger' },
    { label: '处理中', type: 'warning' },
    { label: '待复核', type: 'primary' },
    { label: '已完成', type: 'success' },
  ],
  defaultRecord: {
    ticket: 'R20260424099',
    device: '待确认设备',
    lab: '待确认实验室',
    fault: '待填写故障',
    status: '待接单',
    tagType: 'danger',
  },
  metrics: [
    { label: '待接单', value: '3', trend: '高优先级 1 单' },
    { label: '处理中', value: '6', trend: '今日需回填' },
    { label: '已完成', value: '14', trend: '本周累计' },
  ],
  featureCards: [
    { title: '查看分派工单', tag: '工单', type: 'primary', description: '仅展示分派给当前维修人员的待接单、处理中和已完成工单。' },
    { title: '回填维修过程', tag: '过程', type: 'warning', description: '记录故障原因、处理步骤、更换配件和处理结果。' },
    { title: '同步设备状态', tag: '设备', type: 'success', description: '维修后同步设备恢复、继续观察或停用状态，便于管理员闭环。' },
  ],
  tableColumns: [
    { prop: 'ticket', label: '工单号', minWidth: 140 },
    { prop: 'device', label: '设备', minWidth: 150 },
    { prop: 'lab', label: '实验室', minWidth: 120 },
    { prop: 'fault', label: '故障描述', minWidth: 180 },
    { prop: 'status', label: '状态', minWidth: 100, type: 'tag' },
  ],
  records: [
    { ticket: 'R20260424001', device: '核心交换机', lab: 'B203', fault: '端口掉线，网络不通', status: '处理中', tagType: 'warning' },
    { ticket: 'R20260424005', device: '教师机', lab: 'A101', fault: '系统无法启动', status: '待接单', tagType: 'danger' },
    { ticket: 'R20260423018', device: 'GPU 服务器', lab: 'C305', fault: '驱动异常，训练任务中断', status: '待复核', tagType: 'primary' },
    { ticket: 'R20260422011', device: '显示器', lab: 'A102', fault: '屏幕无信号', status: '已完成', tagType: 'success' },
  ],
  timeline: [
    { title: '接收管理员派单', date: '实时', type: 'primary' },
    { title: '到场排查并回填过程', date: '处理时', type: 'warning' },
    { title: '提交结果等待复核', date: '处理后', type: 'success' },
  ],
}
