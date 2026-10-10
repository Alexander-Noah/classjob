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

