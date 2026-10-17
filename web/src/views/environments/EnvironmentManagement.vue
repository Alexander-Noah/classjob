<!--
  Web 管理平台的实验环境视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  业务状态复用：../../composables/useBusinessModuleRecords。
-->
<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../../api/http'
import SoftwareEnvironmentDialog from './components/SoftwareEnvironmentDialog.vue'
import CourseEnvironmentDialog from './components/CourseEnvironmentDialog.vue'
import EnvironmentTemplateDialog from './components/EnvironmentTemplateDialog.vue'
import BusinessRecordTable from '../../components/business-management/BusinessRecordTable.vue'
import { useBusinessModuleRecords } from '../../composables/useBusinessModuleRecords'
import { createLabSelectOption } from '../../utils/labDisplay'
import {
  COURSE_ENVIRONMENT_MODULE,
  ENVIRONMENT_TEMPLATE_MODULE,
  SOFTWARE_ENVIRONMENT_MODULE,
} from '../../constants/managementModules'

defineOptions({
  name: 'EnvironmentManagement',
})

const route = useRoute()
const router = useRouter()

const tabConfigs = [
  {
    name: 'software',
    label: '软件环境',
    config: {
      ...SOFTWARE_ENVIRONMENT_MODULE,
      pageTitle: '软件环境管理',
      pageDescription: '维护实验室操作系统、开发工具、教学软件、版本授权、安装记录和截图说明。',
    },
  },
  {
    name: 'course',
    label: '课程环境需求',
    config: {
      ...COURSE_ENVIRONMENT_MODULE,
      pageTitle: '课程环境需求管理',
      pageDescription: '处理教师提交的课程环境需求，跟踪管理员配置任务、环境截图上传和教师远程确认。',
    },
  },
  {
    name: 'template',
    label: '环境模板',
    config: {
      ...ENVIRONMENT_TEMPLATE_MODULE,
      pageTitle: '环境模板管理',
      pageDescription: '沉淀常用课程环境模板，支持模板复用、软件版本维护、适用实验室和使用记录统计。',
    },
  },
]

const activeTab = computed({
  get() {
    const tab = String(route.query.tab || 'software')
    return tabConfigs.some((item) => item.name === tab) ? tab : 'software'
  },
  set(tab) {
    router.replace({
      path: route.path,
      query: {
        ...route.query,
        tab,
      },
    })
  },
})

const activeTabConfig = computed(() => tabConfigs.find((item) => item.name === activeTab.value) || tabConfigs[0])
const activeModuleConfig = computed(() => activeTabConfig.value.config)
const pageTitle = computed(() => activeModuleConfig.value.pageTitle || '环境管理')

const business = useBusinessModuleRecords({
  route,
  moduleConfig: activeModuleConfig,
  pageTitle,
})

const getResponseList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.data)) return payload.data.data
  if (Array.isArray(payload?.records)) return payload.records
  if (Array.isArray(payload?.data?.records)) return payload.data.records
  if (Array.isArray(payload?.data?.list)) return payload.data.list
  if (Array.isArray(payload?.list)) return payload.list
  return []
}

const normalizeText = (value) => String(value ?? '').trim()
const buildOption = (value) => ({ label: value, value, name: value })
const toOptionValue = (value) => {
  const numberValue = Number(value)
  return Number.isNaN(numberValue) ? value : numberValue
}

const courseOptions = ref([])
const teacherOptions = ref([])
const classOptions = ref([])
const allTeacherOptions = ref([])
const allClassOptions = ref([])
const courseTeacherMap = ref({})
const courseClassMap = ref({})
const labOptions = ref([])
const softwareRecords = ref([])
const templateRecords = ref([])
const selectedEnvironmentRecord = ref(null)

const getCourseName = (item) =>
  normalizeText(item.courseName || item.course || item.name || item.subjectName || item.lessonName)

const getTeacherName = (item) =>
  normalizeText(item.teacher || item.teacherName || item.realName || item.username)

const getClassName = (item) =>
  normalizeText(item.className || item.rowClassName || item.class_name || item.row_class_name || item.clazz || item.class)

const loadCourseEnvironmentOptions = async () => {
  try {
    const res = await http.get('/class-timetables', {
      params: {
        limit: 5000,
      },
    })

    const list = getResponseList(res.data ?? res)
    const courseSet = new Set()
    const teacherSet = new Set()
    const classSet = new Set()
    const teacherMap = {}
    const classMap = {}

    list.forEach((item) => {
      const courseName = getCourseName(item)
      const teacherName = getTeacherName(item)
      const className = getClassName(item)

      if (courseName) courseSet.add(courseName)
      if (teacherName) teacherSet.add(teacherName)
      if (className) classSet.add(className)

      if (courseName && teacherName) {
        teacherMap[courseName] ||= new Set()
        teacherMap[courseName].add(teacherName)
      }

      if (courseName && className) {
        classMap[courseName] ||= new Set()
        classMap[courseName].add(className)
      }
    })

    courseOptions.value = Array.from(courseSet).map(buildOption)
    allTeacherOptions.value = Array.from(teacherSet).map(buildOption)
    allClassOptions.value = Array.from(classSet).map(buildOption)
    courseTeacherMap.value = Object.fromEntries(
      Object.entries(teacherMap).map(([courseName, teachers]) => [courseName, Array.from(teachers)]),
    )
    courseClassMap.value = Object.fromEntries(
      Object.entries(classMap).map(([courseName, classNames]) => [courseName, Array.from(classNames)]),
    )
    updateCourseLinkedOptions(business.formModel.course)
  } catch (error) {
    console.error('加载课程环境联动下拉数据失败：', error)
    courseOptions.value = []
    allTeacherOptions.value = []
    allClassOptions.value = []
    teacherOptions.value = allTeacherOptions.value
    classOptions.value = allClassOptions.value
  }
}

const updateCourseLinkedOptions = (courseName) => {
  const course = normalizeText(courseName)

  if (!course) {
    teacherOptions.value = allTeacherOptions.value
    classOptions.value = allClassOptions.value
    return
  }

  teacherOptions.value = (courseTeacherMap.value[course] || []).map(buildOption)
  classOptions.value = (courseClassMap.value[course] || []).map(buildOption)

  if (!teacherOptions.value.some((item) => item.value === business.formModel.teacher)) {
    business.formModel.teacher = ''
  }

  if (!classOptions.value.some((item) => item.value === business.formModel.className)) {
    business.formModel.className = ''
  }

  if (teacherOptions.value.length === 1) business.formModel.teacher = teacherOptions.value[0].value
  if (classOptions.value.length === 1) business.formModel.className = classOptions.value[0].value
}

const loadLabOptions = async () => {
  try {
    const res = await http.get('/labs')
    const list = getResponseList(res.data ?? res)

    labOptions.value = list
      .map((item) => {
        const id = item.id ?? item.labId
        const name = item.labName ?? item.name ?? item.lab ?? item.roomName ?? item.classroomName ?? item.location

        return createLabSelectOption({
          ...item,
          id: toOptionValue(id),
          value: toOptionValue(id),
          name,
          labName: name,
        })
      })
      .filter((item) => item.value !== undefined && item.value !== null && item.value !== '' && item.label)
  } catch (error) {
    console.error('加载实验室下拉列表失败：', error)
  }
}

const loadLinkedEnvironmentRecords = async () => {
  const loadRecords = async (url) => {
    try {
      const res = await http.get(url)
      const list = getResponseList(res.data ?? res)
      return list
    } catch (error) {
      console.error(`加载${url}联动数据失败：`, error)
      return []
    }
  }

  const [softwareList, templateList] = await Promise.all([
    loadRecords('/lab-software'),
    loadRecords('/environment-templates'),
  ])

  softwareRecords.value = softwareList
  templateRecords.value = templateList
}

const uniqueOptions = (records, getter) => {
  const values = records.map(getter).map(normalizeText).filter(Boolean)
  return Array.from(new Set(values)).map(buildOption)
}

const getSoftwareName = (record) => normalizeText(record.name || record.softwareName)
const getSoftwareVersion = (record) => normalizeText(record.version || record.softwareVersion)
const getSoftwareLicense = (record) => normalizeText(record.license || record.licenseInfo)
const getTemplateName = (record) => normalizeText(record.name)
const getTemplateCourse = (record) => normalizeText(record.course)

const softwareNameOptions = computed(() => uniqueOptions(softwareRecords.value, getSoftwareName))
const templateNameOptions = computed(() => uniqueOptions(templateRecords.value, getTemplateName))
const softwareVersionOptions = computed(() => uniqueOptions(softwareRecords.value, getSoftwareVersion))
const softwareListOptions = computed(() => uniqueOptions(templateRecords.value, (record) => record.softwareList))
const templateCourseOptions = computed(() => {
  const options = [...courseOptions.value, ...uniqueOptions(templateRecords.value, getTemplateCourse)]
  const optionMap = new Map()
  options.forEach((option) => {
    if (option.value && !optionMap.has(option.value)) {
      optionMap.set(option.value, option)
    }
  })
  return Array.from(optionMap.values())
})

const findSoftwareByName = (name) => {
  const targetName = normalizeText(name)
  return softwareRecords.value.find((record) => getSoftwareName(record) === targetName)
}

const findTemplateByName = (name) => {
  const targetName = normalizeText(name)
  return templateRecords.value.find((record) => getTemplateName(record) === targetName)
}

const findTemplateByCourse = (courseName) => {
  const targetCourse = normalizeText(courseName)
  if (!targetCourse) return null
  const recordsWithCourse = templateRecords.value.filter((record) => getTemplateCourse(record))

  return (
    recordsWithCourse.find((record) => getTemplateCourse(record) === targetCourse) ||
    recordsWithCourse.find((record) => targetCourse.includes(getTemplateCourse(record)) || getTemplateCourse(record).includes(targetCourse))
  )
}

const setIfPresent = (prop, value, { overwrite = false } = {}) => {
  const normalizedValue = normalizeText(value)
  if (!normalizedValue) return
  if (!overwrite && normalizeText(business.formModel[prop])) return
  business.formModel[prop] = normalizedValue
}

const applySoftwareLinkage = (softwareName, { overwrite = false } = {}) => {
  const software = findSoftwareByName(softwareName)
  if (!software) return

  setIfPresent('version', getSoftwareVersion(software), { overwrite })
  setIfPresent('type', software.type, { overwrite })
  setIfPresent('license', getSoftwareLicense(software), { overwrite })
  setIfPresent('status', software.status, { overwrite: false })

  if (software.labId && (overwrite || !business.formModel.labId)) {
    business.formModel.labId = software.labId
  }

  setIfPresent('lab', software.labName || software.lab, { overwrite })
}

const applyTemplateLinkage = (template, { overwrite = false } = {}) => {
  if (!template) return

  setIfPresent('name', template.name, { overwrite: false })
  setIfPresent('course', template.course, { overwrite })
  setIfPresent('os', template.os, { overwrite })
  setIfPresent('softwareList', template.softwareList, { overwrite })
  setIfPresent('labs', template.labs, { overwrite })
  setIfPresent('status', template.status, { overwrite: false })
}

const applyCourseTemplateSuggestion = (courseName, { overwrite = false } = {}) => {
  const template = findTemplateByCourse(courseName)
  if (!template) return

  setIfPresent('software', template.softwareList, { overwrite })
  setIfPresent('labType', template.labs, { overwrite: false })
}

const dialogFieldOptions = computed(() => ({
  ...business.formFieldOptions.value,
  labId: labOptions.value,
  lab: labOptions.value,
  labName: labOptions.value,
  labs: labOptions.value,
  name: activeTab.value === 'software' ? softwareNameOptions.value : templateNameOptions.value,
  version: softwareVersionOptions.value,
  softwareList: softwareListOptions.value,
  course: activeTab.value === 'template' ? templateCourseOptions.value : courseOptions.value,
  courseName: courseOptions.value,
  teacher: teacherOptions.value,
  teacherName: teacherOptions.value,
  className: classOptions.value,
}))

const primaryKey = computed(
  () => activeModuleConfig.value.primaryKey || activeModuleConfig.value.tableColumns?.[0]?.prop || 'name',
)

const statusColumn = computed(
  () =>
    activeModuleConfig.value.tableColumns?.find((column) => column.type === 'tag') ||
    activeModuleConfig.value.tableColumns?.find((column) => column.prop === 'status'),
)

const detailFields = computed(() => activeModuleConfig.value.tableColumns || [])
const detailTitle = computed(
  () => selectedEnvironmentRecord.value?.[primaryKey.value] || activeModuleConfig.value.recordName || '环境记录',
)
const detailStatus = computed(() => selectedEnvironmentRecord.value?.[statusColumn.value?.prop])

const detailHighlights = computed(() =>
  detailFields.value
    .filter((column) => column.prop !== primaryKey.value && column.prop !== statusColumn.value?.prop)
    .slice(0, 4)
    .map((column) => ({
      label: column.label,
      value: selectedEnvironmentRecord.value?.[column.prop] || '-',
    })),
)

const statusOverview = computed(() => business.statusSummary.value.filter((item) => item.count > 0 || item.percentage > 0))

const metrics = computed(() => {
  const records = business.records.value
  const countStatus = (status) => records.filter((record) => record.status === status).length
  if (activeTab.value === 'software') {
    const labs = new Set(records.map((record) => record.labId || record.lab).filter(Boolean))
    return [
      { label: '软件条目', value: records.length, trend: `覆盖 ${labs.size} 间实验室` },
      { label: '待安装任务', value: countStatus('待安装'), trend: '按软件安装状态统计' },
      { label: '授权异常', value: records.filter((record) => record.status === '授权异常' || record.license === '授权异常').length, trend: '需管理员复核' },
    ]
  }
  if (activeTab.value === 'course') {
    return [
      { label: '环境需求', value: records.length, trend: '已登记的课程环境需求' },
      { label: '待教师确认', value: countStatus('待教师确认'), trend: '等待教师核验' },
      { label: '已确认', value: countStatus('已确认'), trend: '已完成教师确认' },
    ]
  }
  return [
    { label: '记录总数', value: business.totalRecordCount.value, trend: `当前筛选 ${business.filteredRecordCount.value} 条` },
    { label: '状态类型', value: business.statusOptions.value.length, trend: '按安装、确认、复用状态管理' },
    { label: '可用实验室', value: labOptions.value.length, trend: '用于环境归属选择' },
  ]
})

const workflowSteps = computed(() => {
  const map = {
    software: [
      { title: '登记清单', desc: '录入软件名称、版本、类型和授权情况' },
      { title: '分配实验室', desc: '绑定真实实验室，形成安装范围' },
      { title: '安装核验', desc: '标记安装、升级或授权异常状态' },
    ],
    course: [
      { title: '提交需求', desc: '从课表联动课程、教师和上课班级' },
      { title: '配置处理', desc: '补充实验室类型、所需软件和处理状态' },
      { title: '教师确认', desc: '将配置结果流转为待确认或已确认' },
    ],
    template: [
      { title: '沉淀模板', desc: '维护操作系统、软件清单和适用课程' },
      { title: '关联实验室', desc: '选择可复用模板覆盖的实验室' },
      { title: '持续更新', desc: '跟踪待更新、试运行和可复用状态' },
    ],
  }

  return map[activeTab.value] || map.software
})

const checklistItems = computed(() => {
  const record = selectedEnvironmentRecord.value || {}
  const map = {
    software: [
      { label: '基础信息', done: Boolean(record.name && record.version && record.type) },
      { label: '实验室归属', done: Boolean(record.lab || record.labId) },
      { label: '授权状态', done: Boolean(record.license) },
      { label: '安装状态', done: Boolean(record.status) },
    ],
    course: [
      { label: '课程信息', done: Boolean(record.course && record.teacher && record.className) },
      { label: '实验室类型', done: Boolean(record.labType) },
      { label: '软件需求', done: Boolean(record.software) },
      { label: '确认流程', done: ['待教师确认', '已确认'].includes(record.status) },
    ],
    template: [
      { label: '模板名称', done: Boolean(record.name) },
      { label: '系统与软件', done: Boolean(record.os && record.softwareList) },
      { label: '适用范围', done: Boolean(record.course && record.labs) },
      { label: '复用状态', done: Boolean(record.status) },
    ],
  }

  return map[activeTab.value] || map.software
})

const selectEnvironmentRecord = (record) => {
  selectedEnvironmentRecord.value = record || null
}

const handleDeleteRecord = (record) => {
  if (record?._recordId === selectedEnvironmentRecord.value?._recordId) {
    selectedEnvironmentRecord.value = null
  }

  business.deleteRecord(record)
}

const handleSubmitRecord = async () => {
  await business.submitRecord()

  if (!business.dialogVisible.value) {
    await loadLinkedEnvironmentRecords()
  }
}

onMounted(() => {
  loadLabOptions()
  loadCourseEnvironmentOptions()
  loadLinkedEnvironmentRecords()
})

watch(
  () => business.formModel.course,
  (courseName) => {
    if (activeTab.value === 'course') {
      updateCourseLinkedOptions(courseName)
      applyCourseTemplateSuggestion(courseName)
    }

    if (activeTab.value === 'template') {
      const template = findTemplateByCourse(courseName)
      applyTemplateLinkage(template)
    }
  },
)

watch(
  () => business.formModel.name,
  (name) => {
    if (!business.dialogVisible.value) return

    if (activeTab.value === 'software') {
      applySoftwareLinkage(name)
    }

    if (activeTab.value === 'template') {
      applyTemplateLinkage(findTemplateByName(name))
    }
  },
)

watch(
  () => business.dialogVisible.value,
  (visible) => {
    if (!visible) return

    if (activeTab.value === 'course') {
      updateCourseLinkedOptions(business.formModel.course)
      applyCourseTemplateSuggestion(business.formModel.course)
    }

    if ((activeTab.value === 'software' || activeTab.value === 'template') && labOptions.value.length === 0) {
      loadLabOptions()
    }

    if (softwareRecords.value.length === 0 || templateRecords.value.length === 0) {
      loadLinkedEnvironmentRecords()
    }
  },
)

watch(activeTab, () => {
  business.keyword.value = ''
  business.statusFilter.value = 'all'
  selectedEnvironmentRecord.value = null
})

watch(
  () => business.pagedRecords.value,
  (records) => {
    if (!records.length) {
      selectedEnvironmentRecord.value = null
      return
    }

    if (
      !selectedEnvironmentRecord.value ||
      !records.some((record) => record._recordId === selectedEnvironmentRecord.value?._recordId)
    ) {
      selectedEnvironmentRecord.value = records[0]
    }
  },
  { immediate: true },
)
</script>

<template>
  <section class="environment-page">
    <el-card class="environment-header-card" shadow="never">
      <div class="environment-header">
        <div class="header-main">
          <div>
            <span class="header-kicker">{{ activeModuleConfig.badge }}</span>
            <h1>环境管理</h1>
          </div>

          <div class="header-actions">
            <el-button type="primary" @click="business.openCreateDialog">
              新增{{ business.recordName.value }}
            </el-button>
            <el-button @click="business.exportRecords">导出</el-button>
          </div>
        </div>

        <el-tabs v-model="activeTab" class="environment-tabs">
          <el-tab-pane v-for="tab in tabConfigs" :key="tab.name" :label="tab.label" :name="tab.name" />
        </el-tabs>
      </div>
    </el-card>

    <el-card class="environment-summary-card" shadow="never">
      <div class="summary-section metrics-section">
        <div v-for="metric in metrics" :key="metric.label" class="metric-item">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
          <p>{{ metric.trend }}</p>
        </div>
      </div>

      <div class="summary-section status-section">
        <div class="section-heading compact">
          <h2>状态分布</h2>
          <span>{{ business.totalRecordCount.value }} 条记录</span>
        </div>

        <div class="status-chips">
          <button
            v-for="status in statusOverview"
            :key="status.label"
            class="status-chip"
            type="button"
            @click="business.statusFilter.value = status.label"
          >
            <el-tag :type="status.type" effect="light" size="small">{{ status.label }}</el-tag>
            <strong>{{ status.count }}</strong>
          </button>

          <span v-if="!statusOverview.length" class="empty-hint">暂无状态数据</span>
        </div>
      </div>

      <div class="summary-section workflow-section">
        <div class="section-heading compact">
          <h2>处理流程</h2>
          <span>{{ activeTabConfig.label }}</span>
        </div>

        <div class="workflow-strip">
          <div v-for="(step, index) in workflowSteps" :key="step.title" class="workflow-step">
            <i>{{ index + 1 }}</i>
            <div>
              <strong>{{ step.title }}</strong>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <div class="environment-workbench">
      <BusinessRecordTable
        v-model:keyword="business.keyword.value"
        v-model:status-filter="business.statusFilter.value"
        :active-record-id="selectedEnvironmentRecord?._recordId"
        :batch-target-status="business.batchTargetStatus.value"
        :current-page="business.currentPage.value"
        :filtered-record-count="business.filteredRecordCount.value"
        :filtered-records="business.pagedRecords.value"
        :get-row-tag-type="business.getRowTagType"
        :page-size="business.pageSize.value"
        :record-name="business.recordName.value"
        :selected-count="business.selectedRows.value.length"
        :status-options="business.statusOptions.value"
        :table-columns="business.tableColumns.value"
        :table-title="activeModuleConfig.tableTitle"
        :total-record-count="business.totalRecordCount.value"
        @batch-status="business.applyBatchStatus()"
        @create="business.openCreateDialog"
        @delete="handleDeleteRecord"
        @delete-selected="business.deleteSelectedRecords"
        @edit="business.openEditDialog"
        @export="business.exportRecords"
        @page-size-change="business.handlePageSizeChange"
        @page-change="business.handleCurrentPageChange"
        @row-click="selectEnvironmentRecord"
        @selection-change="business.handleSelectionChange"
      />

      <el-card class="environment-detail-card" shadow="never">
        <template v-if="selectedEnvironmentRecord">
          <div class="detail-heading">
            <div>
              <h2>{{ detailTitle }}</h2>
            </div>

            <el-tag v-if="detailStatus" :type="business.getRowTagType(selectedEnvironmentRecord)" effect="light">
              {{ detailStatus }}
            </el-tag>
          </div>

          <div class="detail-actions">
            <el-button size="small" type="primary" @click="business.openEditDialog(selectedEnvironmentRecord)">
              编辑
            </el-button>
            <el-button size="small" type="danger" plain @click="handleDeleteRecord(selectedEnvironmentRecord)">
              删除
            </el-button>
          </div>

          <div class="detail-highlight-grid">
            <div v-for="item in detailHighlights" :key="item.label" class="detail-highlight">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>

          <h3>配置核验</h3>
          <div class="checklist">
            <div v-for="item in checklistItems" :key="item.label" class="check-item" :class="{ 'is-done': item.done }">
              <span>{{ item.done ? '已完成' : '待完善' }}</span>
              <strong>{{ item.label }}</strong>
            </div>
          </div>

          <h3>详细信息</h3>
          <el-descriptions :column="1" class="detail-descriptions">
            <el-descriptions-item v-for="field in detailFields" :key="field.prop" :label="field.label">
              {{ selectedEnvironmentRecord[field.prop] || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </template>

        <el-empty v-else description="点击左侧任意记录查看详细信息" />
      </el-card>
    </div>

    <SoftwareEnvironmentDialog
      v-if="activeTab === 'software'"
      v-model="business.dialogVisible.value"
      :dialog-title="business.dialogTitle.value"
      :editable-columns="business.editableColumns.value"
      :field-options="dialogFieldOptions"
      :form-model="business.formModel"
      :form-submitting="business.formSubmitting.value"
      :status-options="business.statusOptions.value"
      :status-prop="business.statusProp.value"
      @submit="handleSubmitRecord"
    />

    <CourseEnvironmentDialog
      v-if="activeTab === 'course'"
      v-model="business.dialogVisible.value"
      :dialog-title="business.dialogTitle.value"
      :editable-columns="business.editableColumns.value"
      :field-options="dialogFieldOptions"
      :form-model="business.formModel"
      :form-submitting="business.formSubmitting.value"
      :status-options="business.statusOptions.value"
      :status-prop="business.statusProp.value"
      @submit="handleSubmitRecord"
    />

    <EnvironmentTemplateDialog
      v-if="activeTab === 'template'"
      v-model="business.dialogVisible.value"
      :dialog-title="business.dialogTitle.value"
      :editable-columns="business.editableColumns.value"
      :field-options="dialogFieldOptions"
      :form-model="business.formModel"
      :form-submitting="business.formSubmitting.value"
      :status-options="business.statusOptions.value"
      :status-prop="business.statusProp.value"
      @submit="handleSubmitRecord"
    />
  </section>
</template>

<style scoped>
.environment-page {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.environment-header-card,
.environment-summary-card,
.environment-detail-card {
  overflow: hidden;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: var(--surface-color);
}

.environment-header-card :deep(.el-card__body) {
  height: auto;
  max-height: none;
  overflow: visible;
  flex: none;
  padding: 20px 22px 0;
  background:
    linear-gradient(135deg, #ffffff 0%, #f8fbff 56%, #eef6ff 100%);
}

.environment-header {
  display: grid;
  gap: 14px;
}

.environment-header-card {
  height: auto;
  max-height: none;
  align-self: start;
}

.header-main,
.header-actions,
.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.header-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.header-kicker {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  background: #ffffff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.environment-header h1 {
  margin: 10px 0 0;
  color: var(--text-primary);
  font-size: 25px;
  font-weight: 700;
  letter-spacing: 0;
  line-height: 1.28;
}

.environment-header p {
  max-width: 76ch;
  margin: 8px 0 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.65;
}

.header-actions :deep(.el-button) {
  margin-left: 0;
  min-width: 86px;
  border-radius: 8px;
  font-weight: 700;
}

.environment-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.environment-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.environment-tabs :deep(.el-tabs__item) {
  height: 46px;
  padding: 0 16px;
  color: #475569;
  font-weight: 600;
}

.environment-tabs :deep(.el-tabs__item.is-active) {
  color: var(--primary-color);
}

.environment-summary-card :deep(.el-card__body) {
  display: grid;
  grid-template-columns: minmax(280px, 0.9fr) minmax(220px, 0.58fr) minmax(420px, 1.45fr);
  gap: 14px;
  padding: 14px;
  align-items: stretch;
  background: #ffffff;
}

.summary-section {
  min-width: 0;
}

.metrics-section {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.metric-item {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 12px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #f8fbff;
}

.metric-item:nth-child(2) {
  background: #fffaf0;
  border-color: #fde4b4;
}

.metric-item:nth-child(3) {
  background: #f8fafc;
  border-color: #e2e8f0;
}

.metric-item span,
.section-heading span,
.detail-highlight span,
.detail-heading p {
  color: #64748b;
  font-size: 13px;
}

.metric-item strong {
  color: var(--text-primary);
  font-size: 24px;
  line-height: 1;
}

.metric-item p {
  margin: 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.45;
}

.status-section,
.workflow-section {
  display: grid;
  gap: 8px;
  align-content: start;
  min-height: 84px;
  padding: 10px 12px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #fbfdff;
}

.section-heading.compact {
  align-items: center;
}

.section-heading h2,
.detail-heading h2,
.environment-detail-card h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 700;
}

.status-chips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 9px;
  border: 1px solid #dbe7f5;
  border-radius: 999px;
  background: #ffffff;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease;
}

.status-chip:hover {
  border-color: #93c5fd;
  background: #eff6ff;
}

.status-chip strong {
  color: var(--text-primary);
  font-size: 13px;
}

.empty-hint {
  color: var(--text-secondary);
  font-size: 13px;
}

.workflow-strip {
  display: flex;
  gap: 8px;
}

.workflow-step {
  display: flex;
  flex: 1 1 0;
  min-width: 0;
  gap: 8px;
  padding: 9px 10px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #ffffff;
}

.workflow-step i {
  display: inline-flex;
  flex: 0 0 24px;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #eaf1ff;
  color: var(--primary-color);
  font-style: normal;
  font-size: 12px;
  font-weight: 700;
}

.workflow-step strong {
  color: var(--text-primary);
  font-size: 13px;
}

.workflow-step p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.4;
}

.environment-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(340px, 390px);
  gap: 18px;
  align-items: start;
}

.environment-workbench :deep(.module-card),
.environment-workbench > * {
  min-width: 0;
}

.environment-workbench :deep(.record-table) {
  width: 100%;
}

.environment-workbench :deep(.module-card) {
  overflow: hidden;
  border-color: #dbe7f5;
  border-radius: 8px;
}

.environment-workbench :deep(.module-card .el-card__header) {
  min-height: 60px;
  padding: 14px 16px;
  background: #fbfdff;
}

.environment-workbench :deep(.card-header h2) {
  font-size: 15px;
}

.environment-workbench :deep(.batch-bar) {
  padding: 12px 16px;
  background: #f8fbff;
}

.environment-workbench :deep(.batch-actions) {
  gap: 8px 10px;
}

.environment-workbench :deep(.selection-action-hint) {
  color: #64748b;
}

.environment-workbench :deep(.el-table th.el-table__cell) {
  background: #f8fbff;
  color: #475569;
  font-weight: 700;
}

.environment-workbench :deep(.el-table__row:hover > td.el-table__cell) {
  background: #f6faff;
}

.environment-workbench :deep(.table-pagination) {
  padding: 12px 14px;
}

.environment-detail-card {
  position: sticky;
  top: 14px;
  min-width: 0;
  max-height: calc(100vh - 108px);
}

.environment-detail-card :deep(.el-card__body) {
  max-height: calc(100vh - 108px);
  overflow: auto;
  padding: 18px;
}

.detail-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}

.detail-heading p {
  margin: 6px 0 0;
}

.detail-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-light);
}

.detail-actions :deep(.el-button) {
  width: 100%;
  min-height: 32px;
  margin-left: 0;
  border-radius: 6px;
  font-weight: 700;
}

.detail-highlight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin: 14px 0 16px;
}

.detail-highlight {
  display: grid;
  gap: 6px;
  min-height: 58px;
  padding: 11px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #f8fbff;
}

.detail-highlight strong {
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.4;
  word-break: break-word;
}

.checklist {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin: 10px 0 16px;
}

.check-item {
  display: grid;
  gap: 5px;
  padding: 9px 11px;
  border: 1px solid #fed7aa;
  border-radius: 8px;
  background: #fff7ed;
}

.check-item.is-done {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.check-item span {
  color: #d97706;
  font-size: 12px;
}

.check-item.is-done span {
  color: #059669;
}

.check-item strong {
  color: var(--text-primary);
  font-size: 13px;
}

.detail-descriptions {
  margin-top: 8px;
}

.detail-descriptions :deep(.el-descriptions__body) {
  background: transparent;
}

.detail-descriptions :deep(.el-descriptions__label) {
  color: var(--text-secondary);
}

.detail-descriptions :deep(.el-descriptions__content) {
  color: var(--text-primary);
  font-weight: 500;
}

@media (max-width: 1180px) {
  .environment-workbench {
    grid-template-columns: 1fr;
  }

  .environment-summary-card :deep(.el-card__body) {
    grid-template-columns: 1fr;
  }

  .environment-detail-card {
    position: static;
    max-height: none;
  }

  .environment-detail-card :deep(.el-card__body) {
    max-height: none;
  }
}

@media (max-width: 860px) {
  .header-main {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions {
    justify-content: flex-start;
  }

  .metrics-section {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .metrics-section,
  .detail-highlight-grid,
  .checklist {
    grid-template-columns: 1fr;
  }

  .workflow-strip {
    flex-direction: column;
  }

  .environment-header-card :deep(.el-card__body) {
    padding: 16px 16px 0;
  }

  .environment-header h1 {
    font-size: 21px;
  }

  .header-actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
    width: 100%;
  }

  .header-actions :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }
}
</style>
