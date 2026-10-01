<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, Download, Refresh, Search } from '@element-plus/icons-vue'
import { AUTH_ROLE_DEFAULT_PATH } from '../../constants/auth'
import PageHero from '../../components/common/PageHero.vue'
import MetricGrid from '../../components/common/MetricGrid.vue'
import { getStoredRole, getStoredUser } from '../../api/services/auth'
import { getClassTimetableSemesters, listClassTimetables } from '../../api/services/classTimetable'
import { exportTableToExcel } from '../../utils/exportExcel'

defineOptions({
  name: 'TeacherSchedule',
})

const router = useRouter()
const currentUser = computed(() => getStoredUser() || {})
const roleHomePath = computed(() => AUTH_ROLE_DEFAULT_PATH[getStoredRole()] || AUTH_ROLE_DEFAULT_PATH.teacher)
const semesterStartDate = '2026-03-02'

const bindingStorageKey = computed(() => `teacher-schedule-binding:${currentUser.value.id || currentUser.value.username || 'default'}`)

const resolveDefaultTeacherName = () =>
  currentUser.value.realName || currentUser.value.name || currentUser.value.username || ''

const getCurrentAcademicWeek = () => {
  const start = new Date(`${semesterStartDate}T00:00:00`)
  const today = new Date()
  const diffDays = Math.floor((today.getTime() - start.getTime()) / 86400000)
  return Math.min(25, Math.max(1, Math.floor(diffDays / 7) + 1))
}

const formatMonthDay = (date) =>
  `${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`

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

const dayLabels = [
  { label: '一', value: 'mon' },
  { label: '二', value: 'tue' },
  { label: '三', value: 'wed' },
  { label: '四', value: 'thu' },
  { label: '五', value: 'fri' },
  { label: '六', value: 'sat' },
  { label: '日', value: 'sun' },
]

const periods = [
  { label: '第1节', time: '8:30-10:00', start: 1 },
  { label: '第2节', time: '10:20-11:50', start: 3 },
  { label: '第3节', time: '14:00-15:30', start: 5 },
  { label: '第4节', time: '15:50-17:20', start: 7 },
  { label: '第5节', time: '18:40-20:10', start: 9 },
  { label: '第6节', time: '20:20-21:50', start: 11 },
]

const weekOptions = Array.from({ length: 25 }, (_, index) => {
  const week = index + 1
  return { label: `第 ${week} 周`, value: week }
})

const filters = reactive({
  teacherName: '',
  semester: '',
  week: getCurrentAcademicWeek(),
  className: '',
  classroom: '',
  courseName: '',
  keyword: '',
})

const pagination = reactive({
  page: 1,
  pageSize: 15,
})

const rows = ref([])
const semesters = ref([])
const latestLoadedAt = ref('')
const isLoading = ref(false)

const weekStartDate = computed(() => {
  const start = new Date(`${semesterStartDate}T00:00:00`)
  start.setDate(start.getDate() + (filters.week - 1) * 7)
  return start
})

const weekDays = computed(() =>
  dayLabels.map((day, index) => {
    const date = new Date(weekStartDate.value)
    date.setDate(date.getDate() + index)
    return {
      ...day,
      date,
      dateText: formatMonthDay(date),
    }
  }),
)

const weekMonthLabel = computed(() => `${weekStartDate.value.getMonth() + 1}月`)

const pagedRows = computed(() => {
  const start = (pagination.page - 1) * pagination.pageSize
  return rows.value.slice(start, start + pagination.pageSize)
})

const metrics = computed(() => {
  const classCount = new Set(rows.value.map((item) => item.className || item.rowClassName).filter(Boolean)).size
  const classroomCount = new Set(rows.value.map((item) => item.classroom).filter(Boolean)).size
  const courseCount = new Set(rows.value.map((item) => item.courseName).filter(Boolean)).size

  return [
    { label: '本周课表', value: String(rows.value.length), trend: `第 ${filters.week} 周` },
    { label: '课程数量', value: String(courseCount), trend: filters.semester || '当前学期' },
    { label: '授课班级', value: String(classCount), trend: '按班级去重' },
    { label: '涉及教室', value: String(classroomCount), trend: '来自抓取课表' },
  ]
})

const resolvePeriodStart = (record = {}) => {
  const start = Number(record.startSection || 0)
  if (start > 0) return start % 2 === 0 ? start - 1 : start

  const match = String(record.sectionCode || record.sectionText || '').match(/(\d{1,2})/)
  const parsed = match ? Number(match[1]) : 1
  return parsed % 2 === 0 ? parsed - 1 : parsed
}

const normalizeCourse = (record = {}, index = 0) => {
  const weekday = record.weekday || ''
  const classroom = record.classroom || '未标注教室'
  const className = record.className || record.rowClassName || '未标注班级'
  const sectionText = record.sectionText || `${record.startSection || ''}-${record.endSection || ''}节`

  return {
    id: record.id ? `timetable-${record.id}` : `timetable-${index}`,
    day: weekdayValueMap[weekday] || 'mon',
    periodStart: resolvePeriodStart(record),
    title: record.courseName || '未命名课程',
    classroom,
    className,
    sectionText,
    weekText: record.weekText || record.weekRaw || record.weekExpanded || '',
    teacher: record.teacher || filters.teacherName,
    raw: record,
  }
}

const normalizedCourses = computed(() => rows.value.map((record, index) => normalizeCourse(record, index)))

const getCellCourses = (dayValue, periodStart) =>
  normalizedCourses.value.filter((course) => course.day === dayValue && course.periodStart === periodStart)

const courseColorClass = (course) => {
  const seed = String(course.title || '').charCodeAt(0) || 0
  return `course-color-${(seed % 5) + 1}`
}

const loadSemesters = async () => {
  try {
    semesters.value = await getClassTimetableSemesters()
    if (!filters.semester && semesters.value.length) {
      filters.semester = semesters.value[0]
    }
  } catch {
    semesters.value = []
  }
}

const loadTeacherSchedule = async () => {
  if (!filters.teacherName.trim()) {
    ElMessage.warning('请先绑定教师姓名')
    return false
  }

  isLoading.value = true
  try {
    rows.value = await listClassTimetables({
      semester: filters.semester || undefined,
      teacher: filters.teacherName.trim(),
      className: filters.className || undefined,
      classroom: filters.classroom || undefined,
      courseName: filters.courseName || undefined,
      keyword: filters.keyword || undefined,
      week: filters.week || undefined,
      limit: 5000,
    })
    latestLoadedAt.value = new Date().toLocaleString()
    pagination.page = 1
    if (!rows.value.length) {
      ElMessage.warning(`未匹配到教师「${filters.teacherName}」第 ${filters.week} 周课表`)
    }
    return true
  } catch (error) {
    ElMessage.error(error?.message || '教师课表加载失败')
    return false
  } finally {
    isLoading.value = false
  }
}

const changeWeek = async (offset) => {
  filters.week = Math.min(25, Math.max(1, filters.week + offset))
  await loadTeacherSchedule()
}

const saveTeacherBinding = async () => {
  if (!filters.teacherName.trim()) {
    ElMessage.warning('请填写教师姓名')
    return
  }
  window.localStorage?.setItem(bindingStorageKey.value, filters.teacherName.trim())
  ElMessage.success('教师姓名已绑定')
  await loadTeacherSchedule()
}

const resetFilters = () => {
  filters.className = ''
  filters.classroom = ''
  filters.courseName = ''
  filters.keyword = ''
  loadTeacherSchedule()
}

const exportTeacherSchedule = () => {
  if (!rows.value.length) {
    ElMessage.warning('暂无可导出的教师课表')
    return
  }

  const exportedCount = exportTableToExcel({
    filename: `${filters.teacherName || '教师'}第${filters.week}周课表`,
    sheetName: '教师课表',
    rows: rows.value,
    columns: [
      { label: '学期', prop: 'semester' },
      { label: '教师', prop: 'teacher' },
      { label: '课程', prop: 'courseName' },
      { label: '班级', prop: 'className' },
      { label: '星期', prop: 'weekday' },
      { label: '节次', prop: 'sectionText' },
      { label: '周次', prop: 'weekText' },
      { label: '教室', prop: 'classroom' },
    ],
  })
  ElMessage.success(`已导出 ${exportedCount} 条教师课表`)
}

const handleHeroAction = async (action) => {
  if (action.command === 'sync') await loadTeacherSchedule()
  if (action.command === 'export') exportTeacherSchedule()
}

const handleBack = () => {
  router.push(roleHomePath.value)
}

onMounted(async () => {
  const storedBinding = window.localStorage?.getItem(bindingStorageKey.value)
  filters.teacherName = storedBinding || resolveDefaultTeacherName()
  filters.week = getCurrentAcademicWeek()
  await loadSemesters()
  await loadTeacherSchedule()
})
</script>

<template>
  <section class="teacher-page">
    <PageHero
      :actions="[
        { label: '同步课表', type: 'primary', command: 'sync' },
        { label: '导出课表', type: 'default', command: 'export' },
      ]"
      badge="教师课表"
      page-title="我的课表"
      page-description="系统会根据学期起始日期自动计算当前教学周，并按绑定教师姓名展示对应周课表。"
      :page-icon="Calendar"
      @action="handleHeroAction"
      @back="handleBack"
    />

    <MetricGrid :metrics="metrics" />

    <el-card class="toolbar-card" shadow="never">
      <div class="binding-row">
        <div>
          <h2>教师绑定</h2>
        </div>
        <div class="binding-controls">
          <el-input v-model="filters.teacherName" clearable placeholder="教师姓名，例如：张明" />
          <el-button type="primary" @click="saveTeacherBinding">保存绑定</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="toolbar-card" shadow="never">
      <div class="filter-grid">
        <el-select v-model="filters.semester" clearable placeholder="学期" @change="loadTeacherSchedule">
          <el-option v-for="semester in semesters" :key="semester" :label="semester" :value="semester" />
        </el-select>
        <el-select v-model="filters.week" placeholder="周次" @change="loadTeacherSchedule">
          <el-option v-for="item in weekOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
<!--        <el-input v-model="filters.className" clearable placeholder="班级" @keyup.enter="loadTeacherSchedule" />-->
<!--        <el-input v-model="filters.classroom" clearable placeholder="教室 / 机房" @keyup.enter="loadTeacherSchedule" />-->
<!--        <el-input v-model="filters.courseName" clearable placeholder="课程名称" @keyup.enter="loadTeacherSchedule" />-->
        <el-input v-model="filters.keyword" clearable placeholder="关键词搜索" @keyup.enter="loadTeacherSchedule" />
        <div class="filter-actions">
          <el-button :icon="Search" type="primary" @click="loadTeacherSchedule">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <el-button :icon="Refresh" @click="loadTeacherSchedule">刷新</el-button>
          <el-button :icon="Download" @click="exportTeacherSchedule">导出</el-button>
        </div>
      </div>
    </el-card>

    <el-card v-loading="isLoading" class="week-card" shadow="never">
      <div class="week-toolbar">
        <el-button :disabled="filters.week <= 1" @click="changeWeek(-1)">上一周</el-button>
        <strong>{{ weekMonthLabel }} 第{{ filters.week }}周</strong>
        <el-button :disabled="filters.week >= 25" @click="changeWeek(1)">下一周</el-button>
      </div>

      <div class="schedule-grid">
        <div class="month-cell">{{ weekMonthLabel }}</div>
        <div v-for="day in weekDays" :key="day.value" class="day-head">
          <strong>{{ day.label }}</strong>
          <span>{{ day.dateText }}</span>
        </div>

        <template v-for="period in periods" :key="period.start">
          <div class="period-cell">
            <strong>{{ period.label }}</strong>
            <span>{{ period.time }}</span>
          </div>
          <div v-for="day in weekDays" :key="`${period.start}-${day.value}`" class="course-cell">
            <el-popover
              v-for="course in getCellCourses(day.value, period.start)"
              :key="course.id"
              placement="right"
              trigger="hover"
              width="280"
            >
              <template #reference>
                <button class="week-course" :class="courseColorClass(course)" type="button">
                  <strong>{{ course.title }}</strong>
                  <span>{{ course.classroom }}</span>
                  <small>{{ course.className }}</small>
                </button>
              </template>
              <div class="course-popover-content">
                <h3>{{ course.title }}</h3>
                <p>教师：{{ course.teacher }}</p>
                <p>节次：{{ course.sectionText }}</p>
                <p>教室 / 机房：{{ course.classroom }}</p>
                <p>班级：{{ course.className }}</p>
                <p>周次：{{ course.weekText || `第 ${filters.week} 周` }}</p>
              </div>
            </el-popover>
          </div>
        </template>
      </div>

      <div class="week-footer">更新时间：{{ latestLoadedAt || '暂未同步' }}</div>
    </el-card>

    <el-card class="detail-card" shadow="never">
      <template #header>
        <div class="section-title">
          <div>
            <h2>课表数据</h2>
            <span>当前展示 {{ filters.teacherName || '未绑定教师' }} 第 {{ filters.week }} 周的抓取课表</span>
          </div>
        </div>
      </template>

      <el-table :data="pagedRows" border>
        <el-table-column label="学期" min-width="120" prop="semester" />
        <el-table-column label="星期" min-width="90" prop="weekday" />
        <el-table-column label="节次" min-width="100" prop="sectionText" />
        <el-table-column label="课程" min-width="180" prop="courseName" show-overflow-tooltip />
        <el-table-column label="班级" min-width="160" prop="className" show-overflow-tooltip />
        <el-table-column label="周次" min-width="120" prop="weekText" show-overflow-tooltip />
        <el-table-column label="教室 / 机房" min-width="180" prop="classroom" show-overflow-tooltip />
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 15, 20, 50, 100]"
          :total="rows.length"
          background
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </el-card>
  </section>
</template>

<style scoped>
.teacher-page {
  display: grid;
  gap: 18px;
}

.toolbar-card,
.week-card,
.detail-card {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
}

.toolbar-card :deep(.el-card__body),
.week-card :deep(.el-card__body),
.detail-card :deep(.el-card__body) {
  padding: 18px;
}

.binding-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.binding-row h2,
.section-title h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 700;
}

.section-title span {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.binding-controls {
  display: grid;
  grid-template-columns: 240px auto;
  gap: 10px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.week-toolbar {
  display: grid;
  grid-template-columns: 100px 1fr 100px;
  align-items: center;
  margin-bottom: 14px;
}

.week-toolbar strong {
  color: var(--text-primary);
  font-size: 20px;
  text-align: center;
}

.schedule-grid {
  display: grid;
  grid-template-columns: 68px repeat(7, minmax(92px, 1fr));
  overflow-x: auto;
  border-top: 1px solid var(--border-color);
  border-left: 1px solid var(--border-color);
}

.month-cell,
.day-head,
.period-cell,
.course-cell {
  border-right: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
  background: #fff;
}

.month-cell,
.day-head {
  min-height: 56px;
}

.month-cell {
  display: grid;
  place-items: center;
  color: var(--text-primary);
  font-weight: 700;
}

.day-head {
  display: grid;
  place-items: center;
  gap: 3px;
  color: var(--text-primary);
  font-size: 14px;
}

.day-head span {
  color: var(--text-secondary);
  font-size: 13px;
}

.period-cell {
  display: grid;
  place-items: center;
  min-height: 108px;
  padding: 8px 4px;
  text-align: center;
}

.period-cell strong {
  color: var(--text-primary);
  font-size: 13px;
}

.period-cell span {
  color: var(--text-secondary);
  font-size: 11px;
  line-height: 1.4;
}

.course-cell {
  min-height: 108px;
  padding: 4px;
}

.week-course {
  display: grid;
  width: 100%;
  min-height: 78px;
  margin-bottom: 4px;
  border: 1px solid currentColor;
  border-radius: 5px;
  padding: 6px;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.week-course strong,
.week-course span,
.week-course small {
  overflow-wrap: anywhere;
  font-size: 13px;
  line-height: 1.35;
}

.week-course:hover {
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.16);
}

.course-popover-content h3 {
  margin: 0 0 10px;
  color: var(--text-primary);
  font-size: 15px;
}

.course-popover-content p {
  margin: 5px 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.course-color-1 {
  color: #ef4444;
  background: #fff1f2;
}

.course-color-2 {
  color: #16a34a;
  background: #f0fdf4;
}

.course-color-3 {
  color: #2563eb;
  background: #eff6ff;
}

.course-color-4 {
  color: #9333ea;
  background: #faf5ff;
}

.course-color-5 {
  color: #f97316;
  background: #fff7ed;
}

.week-footer {
  margin-top: 10px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 13px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 980px) {
  .binding-row {
    align-items: stretch;
    flex-direction: column;
  }

  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .binding-controls,
  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
