<!--
  Web 管理平台的全校课表采集视图组件；模板、响应式状态与局部样式在本文件内协同维护。
-->
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Refresh, Search } from '@element-plus/icons-vue'
import { getClassTimetableSemesters, listClassTimetables } from '../../api/services/classTimetable'
import { exportTableToExcel } from '../../utils/exportExcel'

defineOptions({
  name: 'LabAdminClassTimetableData',
})

const weekdayOrder = {
  星期一: 1,
  星期二: 2,
  星期三: 3,
  星期四: 4,
  星期五: 5,
  星期六: 6,
  星期日: 7,
  周一: 1,
  周二: 2,
  周三: 3,
  周四: 4,
  周五: 5,
  周六: 6,
  周日: 7,
}

const filters = reactive({
  semester: '',
  timeMode: 'default',
  classroom: '',
  grade: '',
  major: '',
  className: '',
  weekStart: undefined,
  weekEnd: undefined,
  weekdayStart: '',
  weekdayEnd: '',
  sectionStart: undefined,
  sectionEnd: undefined,
  keyword: '',
})

const pagination = reactive({
  page: 1,
  pageSize: 15,
})

const rows = ref([])
const semesters = ref([])
const isLoading = ref(false)

/**
 * uniqueSorted的处理逻辑。
 * 入参：items（条目集合）；sorter（sorter）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const uniqueSorted = (items, sorter) => [...new Set(items.filter(Boolean))].sort(sorter)

/**
 * 获取Class名称。
 * 入参：record（记录）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getClassName = (record) => record.className || record.rowClassName || ''

/**
 * 解析ClassMeta。
 * 入参：className（class名称）。
 * @returns 组装的键值对象。
 */
const resolveClassMeta = (className = '') => {
  const normalized = String(className).trim()
  const match = normalized.match(/^(.+?)(\d{2})(\d{2})班?$/)
  if (!match) {
    return {
      major: normalized.replace(/\d+班?$/, ''),
      grade: '',
    }
  }
  return {
    major: match[1],
    grade: `${match[2]}级`,
  }
}

/**
 * 解析数值列表。
 * 入参：value（值）。
 * @returns 生成的数组。
 */
const parseNumberList = (value) => {
  const text = String(value || '')
  const numbers = new Set()
  text.split(',').forEach(/** 处理遍历到的当前条目。 入参：part。 */ (part) => {
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
  return [...numbers]
}

/**
 * 获取记录周次。
 * 入参：record（记录）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getRecordWeeks = (record) => {
  const expanded = parseNumberList(record.weekExpanded)
  if (expanded.length) return expanded
  return parseNumberList(record.weekText || record.weekRaw)
}

/**
 * 获取记录开始节次。
 * 入参：record（记录）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getRecordStartSection = (record) => {
  const start = Number(record.startSection || 0)
  if (start > 0) return start
  const parsed = parseNumberList(record.sectionCode || record.sectionText)
  return parsed[0] || 0
}

/**
 * 获取记录结束节次。
 * 入参：record（记录）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getRecordEndSection = (record) => {
  const end = Number(record.endSection || 0)
  if (end > 0) return end
  const parsed = parseNumberList(record.sectionCode || record.sectionText)
  return parsed[parsed.length - 1] || getRecordStartSection(record)
}

/**
 * 比较课表行集合。
 * 入参：left（left）；right（right）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const compareTimetableRows = (left, right) => {
  const weekdayDiff = (weekdayOrder[left.weekday] || 99) - (weekdayOrder[right.weekday] || 99)
  if (weekdayDiff) return weekdayDiff

  const startDiff = getRecordStartSection(left) - getRecordStartSection(right)
  if (startDiff) return startDiff

  const endDiff = getRecordEndSection(left) - getRecordEndSection(right)
  if (endDiff) return endDiff

  const classDiff = getClassName(left).localeCompare(getClassName(right), 'zh-Hans-CN')
  if (classDiff) return classDiff

  return String(left.courseName || '').localeCompare(String(right.courseName || ''), 'zh-Hans-CN')
}

const availableClasses = computed(/** 计算派生状态。 */ () => uniqueSorted(rows.value.map(getClassName), /** 处理 uniqueSorted 调用中的回调逻辑。 入参：a、b。 */ (a, b) => a.localeCompare(b, 'zh-Hans-CN')))

const availableClassrooms = computed(/** 计算派生状态。 */ () =>
  uniqueSorted(rows.value.map(/** 将当前条目转换为目标结果。 入参：record。 */ (record) => record.classroom), /** 处理 uniqueSorted 调用中的回调逻辑。 入参：a、b。 */ (a, b) => a.localeCompare(b, 'zh-Hans-CN')),
)

const availableMajors = computed(/** 计算派生状态。 */ () =>
  uniqueSorted(availableClasses.value.map(/** 将当前条目转换为目标结果。 入参：className。 */ (className) => resolveClassMeta(className).major), /** 处理 uniqueSorted 调用中的回调逻辑。 入参：a、b。 */ (a, b) =>
    a.localeCompare(b, 'zh-Hans-CN'),
  ),
)

const availableGrades = computed(/** 计算派生状态。 */ () =>
  uniqueSorted(availableClasses.value.map(/** 将当前条目转换为目标结果。 入参：className。 */ (className) => resolveClassMeta(className).grade), /** 处理 uniqueSorted 调用中的回调逻辑。 入参：a、b。 */ (a, b) => a.localeCompare(b)),
)

const availableWeekdays = computed(/** 计算派生状态。 */ () =>
  uniqueSorted(rows.value.map(/** 将当前条目转换为目标结果。 入参：record。 */ (record) => record.weekday), /** 处理 uniqueSorted 调用中的回调逻辑。 入参：a、b。 */ (a, b) => (weekdayOrder[a] || 99) - (weekdayOrder[b] || 99)),
)

const availableWeeks = computed(/** 计算派生状态。 */ () =>
  uniqueSorted(rows.value.flatMap(/** 转换并展开当前条目。 入参：record。 */ (record) => getRecordWeeks(record)), /** 处理 uniqueSorted 调用中的回调逻辑。 入参：a、b。 */ (a, b) => Number(a) - Number(b)),
)

const availableSections = computed(/** 计算派生状态。 */ () =>
  uniqueSorted(rows.value.map(/** 将当前条目转换为目标结果。 入参：record。 */ (record) => getRecordStartSection(record)), /** 处理 uniqueSorted 调用中的回调逻辑。 入参：a、b。 */ (a, b) => Number(a) - Number(b)),
)

/**
 * 文本Includes的处理逻辑。
 * 入参：value（值）；keyword（keyword）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const textIncludes = (value, keyword) =>
  !keyword || String(value || '').toLowerCase().includes(String(keyword).trim().toLowerCase())

const filteredRows = computed(/** 计算派生状态。 */ () => {
  const weekStart = filters.weekStart ? Number(filters.weekStart) : undefined
  const weekEnd = filters.weekEnd ? Number(filters.weekEnd) : undefined
  const weekdayStart = filters.weekdayStart ? weekdayOrder[filters.weekdayStart] : undefined
  const weekdayEnd = filters.weekdayEnd ? weekdayOrder[filters.weekdayEnd] : undefined
  const sectionStart = filters.sectionStart ? Number(filters.sectionStart) : undefined
  const sectionEnd = filters.sectionEnd ? Number(filters.sectionEnd) : undefined

  return rows.value.filter(/** 判断当前条目是否符合筛选条件。 入参：record。 */ (record) => {
    const className = getClassName(record)
    const classMeta = resolveClassMeta(className)
    if (!textIncludes(record.classroom, filters.classroom)) return false
    if (filters.grade && classMeta.grade !== filters.grade) return false
    if (filters.major && classMeta.major !== filters.major) return false
    if (filters.className && className !== filters.className) return false

    if (weekStart || weekEnd) {
      const weeks = getRecordWeeks(record)
      const min = weekStart || weekEnd
      const max = weekEnd || weekStart
      if (!weeks.length || !weeks.some(/** 判断当前条目是否满足任一匹配条件。 入参：week。 */ (week) => week >= min && week <= max)) return false
    }

    if (weekdayStart || weekdayEnd) {
      const order = weekdayOrder[record.weekday] || 0
      const min = weekdayStart || weekdayEnd
      const max = weekdayEnd || weekdayStart
      if (!order || order < min || order > max) return false
    }

    if (sectionStart || sectionEnd) {
      const section = getRecordStartSection(record)
      const min = sectionStart || sectionEnd
      const max = sectionEnd || sectionStart
      if (!section || section < min || section > max) return false
    }

    if (filters.keyword) {
      const haystack = [
        record.semester,
        record.className,
        record.rowClassName,
        record.weekday,
        record.sectionText,
        record.courseName,
        record.teacher,
        record.weekText,
        record.classroom,
      ].join(' ')
      if (!textIncludes(haystack, filters.keyword)) return false
    }

    return true
  })
})

const sortedRows = computed(/** 计算派生状态。 */ () => [...filteredRows.value].sort(compareTimetableRows))

const pagedRows = computed(/** 计算派生状态。 */ () => {
  const start = (pagination.page - 1) * pagination.pageSize
  return sortedRows.value.slice(start, start + pagination.pageSize)
})

/**
 * 加载学期。
 */
const loadSemesters = async () => {
  try {
    semesters.value = await getClassTimetableSemesters()
    if (!filters.semester && semesters.value.length) filters.semester = semesters.value[0]
  } catch {
    semesters.value = []
  }
}

/**
 * 加载行集合。
 */
const loadRows = async () => {
  isLoading.value = true
  try {
    rows.value = await listClassTimetables({
      semester: filters.semester || undefined,
      className: filters.className || undefined,
      keyword: filters.keyword || undefined,
      week: filters.weekStart && filters.weekStart === filters.weekEnd ? filters.weekStart : undefined,
      limit: 5000,
    })
    pagination.page = 1
  } catch (error) {
    ElMessage.error(error?.message || '校园课表加载失败')
  } finally {
    isLoading.value = false
  }
}

/**
 * 重置筛选条件。
 */
const resetFilters = () => {
  filters.timeMode = 'default'
  filters.classroom = ''
  filters.grade = ''
  filters.major = ''
  filters.className = ''
  filters.weekStart = undefined
  filters.weekEnd = undefined
  filters.weekdayStart = ''
  filters.weekdayEnd = ''
  filters.sectionStart = undefined
  filters.sectionEnd = undefined
  filters.keyword = ''
  loadRows()
}

/**
 * 处理MajorChange。
 */
const handleMajorChange = () => {
  if (filters.className && resolveClassMeta(filters.className).major !== filters.major) {
    filters.className = ''
  }
}

/**
 * 处理GradeChange。
 */
const handleGradeChange = () => {
  if (filters.className && resolveClassMeta(filters.className).grade !== filters.grade) {
    filters.className = ''
  }
}

const classOptions = computed(/** 计算派生状态。 */ () =>
  availableClasses.value.filter(/** 判断当前条目是否符合筛选条件。 入参：className。 */ (className) => {
    const meta = resolveClassMeta(className)
    return (!filters.major || meta.major === filters.major) && (!filters.grade || meta.grade === filters.grade)
  }),
)

/**
 * 导出行集合。
 */
const exportRows = () => {
  if (!sortedRows.value.length) {
    ElMessage.warning('暂无可导出的校园课表')
    return
  }

  const exportedCount = exportTableToExcel({
    filename: '校园课表',
    sheetName: 'class_timetable',
    rows: sortedRows.value,
    columns: [
      { label: '学期', prop: 'semester' },
      { label: '班级', prop: 'className' },
      { label: '星期', prop: 'weekday' },
      { label: '节次', prop: 'sectionText' },
      { label: '课程', prop: 'courseName' },
      { label: '教师', prop: 'teacher' },
      { label: '周次', prop: 'weekText' },
      { label: '教室 / 机房', prop: 'classroom' },
    ],
  })
  ElMessage.success(`已导出 ${exportedCount} 条校园课表`)
}

onMounted(/** 在组件挂载后初始化相关状态。 */ async () => {
  await loadSemesters()
  await loadRows()
})
</script>

<template>
  <section class="timetable-data-page">
    <el-card class="panel-card filter-card" shadow="never">
      <div class="advanced-filter">
        <label class="filter-field">
          <span>学年学期</span>
          <el-select v-model="filters.semester" clearable placeholder="请选择">
            <el-option v-for="semester in semesters" :key="semester" :label="semester" :value="semester" />
          </el-select>
        </label>
        <label class="filter-field">
          <span>时间模式</span>
          <el-select v-model="filters.timeMode" placeholder="请选择">
            <el-option label="默认节次模式" value="default" />
            <el-option label="连续节次模式" value="continuous" />
          </el-select>
        </label>
        <label class="filter-field">
          <span>上课教室</span>
          <el-select v-model="filters.classroom" clearable filterable placeholder="-请选择-">
            <el-option v-for="classroom in availableClassrooms" :key="classroom" :label="classroom" :value="classroom" />
          </el-select>
        </label>
        <label class="filter-field">
          <span>上课年级</span>
          <el-select v-model="filters.grade" clearable placeholder="-请选择-" @change="handleGradeChange">
            <el-option v-for="grade in availableGrades" :key="grade" :label="grade" :value="grade" />
          </el-select>
        </label>
        <label class="filter-field">
          <span>上课专业</span>
          <el-select v-model="filters.major" clearable placeholder="-请选择-" @change="handleMajorChange">
            <el-option v-for="major in availableMajors" :key="major" :label="major" :value="major" />
          </el-select>
        </label>
        <label class="filter-field">
          <span>上课班级</span>
          <el-select v-model="filters.className" clearable filterable placeholder="-请选择-">
            <el-option v-for="className in classOptions" :key="className" :label="className" :value="className" />
          </el-select>
        </label>
        <label class="filter-field range-field">
          <span>周次</span>
          <el-select v-model="filters.weekStart" clearable placeholder="-请选择-">
            <el-option v-for="week in availableWeeks" :key="`ws-${week}`" :label="`第 ${week} 周`" :value="week" />
          </el-select>
          <em>至</em>
          <el-select v-model="filters.weekEnd" clearable placeholder="-请选择-">
            <el-option v-for="week in availableWeeks" :key="`we-${week}`" :label="`第 ${week} 周`" :value="week" />
          </el-select>
        </label>
        <label class="filter-field range-field">
          <span>星期</span>
          <el-select v-model="filters.weekdayStart" clearable placeholder="-请选择-">
            <el-option v-for="day in availableWeekdays" :key="`ds-${day}`" :label="day" :value="day" />
          </el-select>
          <em>至</em>
          <el-select v-model="filters.weekdayEnd" clearable placeholder="-请选择-">
            <el-option v-for="day in availableWeekdays" :key="`de-${day}`" :label="day" :value="day" />
          </el-select>
        </label>
        <label class="filter-field range-field">
          <span>节次</span>
          <el-select v-model="filters.sectionStart" clearable placeholder="-请选择-">
            <el-option
              v-for="section in availableSections"
              :key="`ss-${section}`"
              :label="`第 ${section} 节`"
              :value="section"
            />
          </el-select>
          <em>至</em>
          <el-select v-model="filters.sectionEnd" clearable placeholder="-请选择-">
            <el-option
              v-for="section in availableSections"
              :key="`se-${section}`"
              :label="`第 ${section} 节`"
              :value="section"
            />
          </el-select>
        </label>
        <el-button :icon="Search" class="query-button" type="primary" @click="loadRows">查询</el-button>
      </div>
    </el-card>

    <el-card class="panel-card" shadow="never">
      <template #header>
        <div class="card-header">
          <strong>校园课表预览</strong>
          <div class="header-actions">
            <el-input v-model="filters.keyword" clearable placeholder="关键词搜索" @keyup.enter="loadRows" />
            <el-button :icon="Search" type="primary" @click="loadRows">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
            <el-button :icon="Refresh" @click="loadRows">刷新</el-button>
            <el-button :icon="Download" @click="exportRows">导出</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="isLoading" :data="pagedRows" border class="management-table">
        <el-table-column label="学期" min-width="120" prop="semester" />
        <el-table-column label="班级" min-width="150" prop="className" show-overflow-tooltip />
        <el-table-column label="星期" min-width="90" prop="weekday" />
        <el-table-column label="节次" min-width="100" prop="sectionText" />
        <el-table-column label="课程" min-width="180" prop="courseName" show-overflow-tooltip />
        <el-table-column label="教师" min-width="110" prop="teacher" show-overflow-tooltip />
        <el-table-column label="周次" min-width="120" prop="weekText" show-overflow-tooltip />
        <el-table-column label="教室 / 机房" min-width="180" prop="classroom" show-overflow-tooltip />
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 15, 20, 50, 100]"
          :total="sortedRows.length"
          background
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </el-card>
  </section>
</template>

<style scoped>
.timetable-data-page {
  display: grid;
  gap: 18px;
}

.panel-card {
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--surface-color);
}

.panel-card :deep(.el-card__header) {
  padding: 18px 22px;
}

.panel-card :deep(.el-card__body) {
  padding: 22px;
}

.filter-card :deep(.el-card__body) {
  padding: 16px 22px;
}

.advanced-filter {
  display: grid;
  grid-template-columns: repeat(3, minmax(300px, 1fr));
  align-items: center;
  gap: 12px 24px;
}

.filter-field {
  display: grid;
  grid-template-columns: 76px 1fr;
  align-items: center;
  gap: 10px;
  min-width: 0;
  color: var(--text-primary);
  font-size: 13px;
}

.range-field {
  grid-template-columns: 52px 96px 20px 96px;
}

.range-field em {
  color: var(--text-secondary);
  font-style: normal;
  text-align: center;
}

.query-button {
  width: 104px;
  justify-self: start;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.header-actions {
  display: grid;
  grid-template-columns: minmax(220px, 360px) repeat(4, auto);
  align-items: center;
  gap: 10px;
}

.management-table {
  width: 100%;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 1300px) {
  .advanced-filter {
    grid-template-columns: repeat(2, minmax(280px, 1fr));
  }

  .card-header,
  .header-actions {
    align-items: stretch;
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .advanced-filter,
  .filter-field,
  .range-field {
    grid-template-columns: 1fr;
  }

  .query-button {
    width: 100%;
  }
}
</style>
