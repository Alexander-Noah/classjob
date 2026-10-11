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

