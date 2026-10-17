<!--
  Web 管理平台的教师工作台视图组件；模板、响应式状态与局部样式在本文件内协同维护。
-->
<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar } from '@element-plus/icons-vue'
import MetricGrid from '../../components/common/MetricGrid.vue'
import TeacherScheduleBoard from '../teacher/components/TeacherScheduleBoard.vue'
import http from '../../api/http'
import { API_ENDPOINTS } from '../../api/endpoints'
import { getStoredUser } from '../../api/services/auth'
import { parseUserPermissions } from '../../constants/permissions'
import { getClassTimetableSemesters, listClassTimetables } from '../../api/services/classTimetable'
import { getAcademicWeek, getTeachingWeekStartDate, loadTeachingCalendar } from '../../modules/teaching-calendar/teachingCalendar'

defineOptions({
  name: 'TeacherPortal',
})

const router = useRouter()
const currentUser = computed(() => getStoredUser() || {})
const currentUserPermissions = computed(() => parseUserPermissions(currentUser.value))
const canViewTimetableData = computed(() => currentUserPermissions.value.includes('课表数据'))
const teachingCalendar = ref(null)
const bindingStorageKey = computed(() => `teacher-schedule-binding:${currentUser.value.id || currentUser.value.username || 'default'}`)

const resolveDefaultTeacherName = () =>
  currentUser.value.realName || currentUser.value.name || currentUser.value.username || ''

const formatMonthDay = (date) =>
  `${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`

const weekOptions = Array.from({ length: 25 }, (_, index) => {
  const week = index + 1
  return { label: `第 ${week} 周`, value: week }
})

const dayLabels = [
  { label: '周一', value: 'mon' },
  { label: '周二', value: 'tue' },
  { label: '周三', value: 'wed' },
  { label: '周四', value: 'thu' },
  { label: '周五', value: 'fri' },
  { label: '周六', value: 'sat' },
  { label: '周日', value: 'sun' },
]

const weekdayValueMap = {
  星期一: 'mon',
  星期二: 'tue',
  星期三: 'wed',
  星期四: 'thu',
  星期五: 'fri',
  星期六: 'sat',
  星期日: 'sun',
  周一: 'mon',
  周二: 'tue',
  周三: 'wed',
  周四: 'thu',
  周五: 'fri',
  周六: 'sat',
  周日: 'sun',
}

const DEFAULT_SCHEDULE_RECORDS = [
  {
    id: 'teacher-course-1',
    week: 9,
    day: 'wed',
    title: 'Java Web 程序设计',
    time: '08:00-09:40',
    lab: 'A101',
    className: '软件工程 2 班',
    status: '待确认',
    tagType: 'warning',
    environment: 'JDK 21、IDEA、MySQL',
    note: '已上传环境截图，建议课前完成远程确认。',
    students: 48,
  },
  {
    id: 'teacher-course-2',
    week: 9,
    day: 'thu',
    title: '人工智能基础',
    time: '10:00-11:40',
    lab: 'C305',
    className: '数据科学 1 班',
    status: '已确认',
    tagType: 'success',
    environment: 'Python、PyTorch、CUDA',
    note: 'GPU 环境已确认，可直接授课。',
    students: 42,
  },
  {
    id: 'teacher-course-3',
    week: 9,
    day: 'fri',
    title: '数据库原理',
    time: '14:00-15:40',
    lab: 'A102',
    className: '软件工程 3 班',
    status: '已排课',
    tagType: 'success',
    environment: 'MySQL、Navicat、ER 建模工具',
    note: '课表已排定，环境准备中。',
    students: 50,
  },
  {
    id: 'teacher-course-4',
    week: 9,
    day: 'tue',
    title: '软件测试',
    time: '16:00-17:40',
    lab: 'A102',
    className: '软件工程 1 班',
    status: '已排课',
    tagType: 'info',
    environment: 'JUnit、Selenium、测试样例库',
    note: '课表已排定，环境准备中。',
    students: 44,
  },
]

const normalizeListResponse = (data) => {
  if (Array.isArray(data?.data)) return data.data
  if (Array.isArray(data)) return data
  return []
}

const resolveSectionTime = (record = {}) => {
  const section = String(record.sectionText || record.sectionCode || '').trim()
  if (section) return section
  const start = record.startSection || ''
  const end = record.endSection || ''
  return start || end ? `${start}-${end}节` : '待确认'
}

const resolveCourseTimeText = (record = {}) => {
  const section = resolveSectionTime(record)
  const weekday = record.weekday || ''
  return [weekday, section].filter(Boolean).join(' ') || '待确认'
}

const resolvePeriodStart = (record = {}) => {
  const start = Number(record.startSection || 0)
  if (start > 0) return start % 2 === 0 ? start - 1 : start

  const match = String(record.sectionCode || record.sectionText || record.time || '').match(/\d{1,2}/)
  const parsed = match ? Number(match[0]) : 1
  return parsed % 2 === 0 ? parsed - 1 : parsed
}

const findEnvironmentRecord = (record = {}) => {
  const courseName = record.courseName || record.course || record.title || ''
  return environmentRecords.value.find((item) => {
    const envCourse = item.course || item.courseName || item.title || ''
    return envCourse && courseName && envCourse === courseName
  })
}

const normalizePortalCourse = (record = {}, index = 0) => {
  const environmentRecord = findEnvironmentRecord(record)
  const title = record.title || record.course || record.courseName || '未命名课程'
  const lab = record.lab || record.classroom || record.assignedLabCode || record.assignedLabName || record.labType || '待分配'
  const environment = record.software || record.softwareRequirements || record.environment || '按课程要求配置'

  return {
    id: record.id ? `teacher-course-${record.id}` : `teacher-course-${Date.now()}-${index}`,
    week: Number(record.week || currentWeek.value),
    day: record.day || weekdayValueMap[record.weekday] || 'mon',
    periodStart: resolvePeriodStart(record),
    title,
    time: record.time || record.useTime || record.schedule || resolveCourseTimeText(record),
    lab,
    className: record.className || record.rowClassName || '待同步班级',
    environment: environmentRecord?.software || environmentRecord?.softwareRequirements || environment,
    note:
      environmentRecord?.specialRequirements ||
      record.note ||
      resolveCourseTimeText(record),
    students: Number(record.students || record.capacity || 45),
    teacher: record.teacher || teacherName.value,
    weekText: record.weekText || record.weekRaw || '',
  }
}

const environmentRecords = ref([])
const isSyncing = ref(false)
const semesters = ref([])
const semester = ref('')
const teacherName = ref('')
const currentWeek = ref(1)

const scheduleRecords = ref(DEFAULT_SCHEDULE_RECORDS.map((item, index) => normalizePortalCourse(item, index)))
const selectedCourse = ref(scheduleRecords.value[0] || null)

const weekStartDate = computed(() => {
  return getTeachingWeekStartDate(teachingCalendar.value, currentWeek.value) || new Date()
})

const weekDays = computed(() =>
  dayLabels.map((day, index) => {
    const date = new Date(weekStartDate.value)
    date.setDate(date.getDate() + index)
    return {
      ...day,
      date: formatMonthDay(date),
    }
  }),
)

const metrics = computed(() => {
  const courses = scheduleRecords.value.filter((item) => item.week === currentWeek.value)
  const labCount = new Set(courses.map((item) => item.lab).filter(Boolean)).size
  const classCount = new Set(courses.map((item) => item.className).filter(Boolean)).size
  const pendingEnvironmentCount = environmentRecords.value.filter(
    (item) => !['已确认', '已完成'].includes(item.status || item.confirmStatus),
  ).length

  return [
    { label: '本周课程', value: String(courses.length), trend: `第 ${currentWeek.value} 周课表` },
    { label: '授课班级', value: String(classCount), trend: teacherName.value || '当前教师' },
    { label: '涉及实验室', value: String(labCount), trend: courses.map((item) => item.lab).slice(0, 3).join(' / ') || '暂无实验室' },
    { label: '待确认环境', value: String(pendingEnvironmentCount), trend: '课前完成确认' },
  ]
})

const visibleCourses = computed(() =>
  scheduleRecords.value.filter((item) => item.week === currentWeek.value),
)

const selectCourse = (course) => {
  selectedCourse.value = course
}

const loadSemesters = async () => {
  try {
    semesters.value = await getClassTimetableSemesters()
    if (!semester.value && semesters.value.length) {
      semester.value = semesters.value[0]
    }
  } catch {
    semesters.value = []
  }
}

watch(visibleCourses, (courses) => {
  if (!courses.some((item) => item.id === selectedCourse.value?.id)) {
    selectedCourse.value = courses[0] || null
  }
})

const syncTeacherData = async ({ silent = false } = {}) => {
  isSyncing.value = true
  try {
    const [{ data }, timetableRecords] = await Promise.all([
      http.get(API_ENDPOINTS.courseEnvironmentRequests.list),
      listClassTimetables({
        semester: semester.value || undefined,
        teacher: teacherName.value || undefined,
        week: currentWeek.value || undefined,
        limit: 5000,
      }),
    ])
    environmentRecords.value = normalizeListResponse(data)
    if (Array.isArray(timetableRecords) && timetableRecords.length) {
      scheduleRecords.value = timetableRecords.map((item, index) => normalizePortalCourse(item, index))
      selectedCourse.value = scheduleRecords.value[0] || null
    } else {
      scheduleRecords.value = []
      selectedCourse.value = null
      if (!silent) ElMessage.warning(`未匹配到${teacherName.value ? `「${teacherName.value}」` : ''}第 ${currentWeek.value} 周课表`)
    }
    if (!silent) {
      ElMessage.success('教师周课表已同步')
    }
  } catch (error) {
    if (!silent) {
      ElMessage.warning(error?.message || '教师工作台同步失败，已保留当前数据')
    }
  } finally {
    isSyncing.value = false
  }
}

const goToSchedule = () => {
  router.push('/teacher/schedule')
}

const goToTimetableData = () => {
  router.push('/teacher/class-timetable-data')
}

onMounted(async () => {
  const storedBinding = window.localStorage?.getItem(bindingStorageKey.value)
  teacherName.value = storedBinding || resolveDefaultTeacherName()
  try {
    teachingCalendar.value = await loadTeachingCalendar()
    semester.value = teachingCalendar.value.semester
    currentWeek.value = getAcademicWeek(teachingCalendar.value)
  } catch (error) {
    ElMessage.error(error?.message || '教学日历加载失败')
    return
  }
  await loadSemesters()
  await syncTeacherData({ silent: true })
})
</script>

<template>
  <section class="teacher-portal">
    <el-card class="hero-card" shadow="never">
      <div class="hero-content">
        <div class="hero-copy">
          <span class="hero-icon" aria-hidden="true">
            <el-icon><Calendar /></el-icon>
          </span>
          <div>
            <el-tag effect="light">教师工作台</el-tag>
            <h1>我的课表</h1>
          </div>
        </div>

        <div class="hero-actions">
          <el-button :loading="isSyncing" @click="syncTeacherData()">同步数据</el-button>
          <el-button type="primary" @click="goToSchedule">进入完整课表</el-button>
          <el-button v-if="canViewTimetableData" @click="goToTimetableData">课表数据</el-button>
        </div>
      </div>
    </el-card>

    <MetricGrid :metrics="metrics" />

    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar-content">
        <div>
          <h2>本周授课安排</h2>
        </div>
        <el-select v-model="currentWeek" class="week-select" placeholder="选择周次" @change="syncTeacherData()">
          <el-option v-for="item in weekOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </div>
    </el-card>

    <el-row :gutter="18">
      <el-col :xs="24" :xl="16">
        <TeacherScheduleBoard
          :active-id="selectedCourse?.id || ''"
          :courses="visibleCourses"
          :week-days="weekDays"
          @select="selectCourse"
        />
      </el-col>

      <el-col :xs="24" :xl="8">
        <el-card class="detail-card" shadow="never">
          <template #header>
            <div class="section-title">
              <div>
                <h2>当前课程详情</h2>
                <span>首页直接展示当前课表项，减少来回切页面。</span>
              </div>
            </div>
          </template>

          <template v-if="selectedCourse">
            <h3 class="detail-title">{{ selectedCourse.title }}</h3>
            <p class="detail-copy">{{ selectedCourse.note }}</p>

            <el-descriptions :column="1" class="detail-descriptions">
              <el-descriptions-item label="授课时间">{{ selectedCourse.time }}</el-descriptions-item>
              <el-descriptions-item label="实验室">{{ selectedCourse.lab }}</el-descriptions-item>
              <el-descriptions-item label="班级">{{ selectedCourse.className }}</el-descriptions-item>
              <el-descriptions-item label="学生人数">{{ selectedCourse.students }} 人</el-descriptions-item>
            </el-descriptions>
          </template>

          <el-empty v-else description="本周暂无课表数据" :image-size="88" />
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>

<style scoped>
.teacher-portal {
  display: grid;
  gap: 18px;
}

.hero-card,
.toolbar-card,
.detail-card {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
}

.hero-card :deep(.el-card__body),
.toolbar-card :deep(.el-card__body),
.detail-card :deep(.el-card__body) {
  padding: 18px;
}

.hero-content,
.toolbar-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.hero-copy {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.hero-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  border-radius: 8px;
  color: var(--primary-color);
  background: #eff6ff;
  font-size: 30px;
}

.hero-copy h1,
.toolbar-content h2,
.section-title h2,
.detail-title {
  margin: 8px 0 0;
  color: var(--text-primary);
  font-size: 22px;
  font-weight: 700;
  line-height: 1.3;
}

.hero-copy p,
.toolbar-content p,
.section-title span,
.detail-copy {
  margin: 8px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.week-select {
  width: 180px;
}

.detail-descriptions {
  margin: 16px 0;
}

.quick-grid {
  display: grid;
  gap: 12px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: #fbfdff;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.quick-card:hover {
  border-color: #93c5fd;
  box-shadow: 0 10px 24px rgba(59, 130, 246, 0.12);
}

.quick-icon {
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

.quick-card strong {
  display: block;
  color: var(--text-primary);
  font-size: 14px;
}

.quick-card small {
  display: block;
  margin-top: 5px;
  color: var(--text-secondary);
  font-size: 12px;
}

@media (max-width: 760px) {
  .hero-content,
  .toolbar-content {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-copy {
    align-items: flex-start;
    flex-direction: column;
  }

  .week-select {
    width: 100%;
  }
}
</style>
