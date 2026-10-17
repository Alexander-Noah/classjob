<!--
  Web 管理平台的耗材借还视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  业务状态复用：../../../composables/useTablePagination。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../../api/http'
import TablePagination from '../../../components/common/TablePagination.vue'
import { useTablePagination } from '../../../composables/useTablePagination'

const props = defineProps({ consumables: { type: Array, default: () => [] } })
const emit = defineEmits(['changed'])
const rows = ref([])
const loading = ref(false)
const loadError = ref('')
const saving = ref(false)
const borrowVisible = ref(false)
const returnVisible = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const selectedLoan = ref(null)
const borrowRef = ref()
const returnRef = ref()
const filters = reactive({ keyword: '', status: '' })
const borrowForm = reactive({ consumableId: '', borrower: '', contact: '', quantity: 1, purpose: '', dueDate: '', remark: '', requestId: '' })
const returnForm = reactive({ quantity: 1, remark: '', requestId: '' })
const selectedItem = computed(() => props.consumables.find(item => String(item.id) === String(borrowForm.consumableId)))
const filtered = computed(() => rows.value.filter(row =>
  (!filters.status || row.status === filters.status) &&
  [row.consumableName, row.borrower, row.contact, row.purpose, row.operator, String(row.id)].join(' ').toLowerCase().includes(filters.keyword.trim().toLowerCase()),
))
const outstanding = computed(() => rows.value.filter(row => row.remainingQuantity > 0).length)
const overdue = computed(() => rows.value.filter(row => row.status === '已逾期').length)
const { currentPage, pageSize, total, pagedRecords, handleCurrentPageChange, handlePageSizeChange } = useTablePagination(filtered)
const borrowRules = {
  consumableId: [{ required: true, message: '请选择耗材', trigger: 'change' }],
  borrower: [{ required: true, whitespace: true, message: '请填写借用人', trigger: 'blur' }],
  quantity: [{ validator: (_rule, value, callback) => callback(Number.isSafeInteger(value) && value > 0 && value <= (selectedItem.value?.stock || 0) ? undefined : new Error('借用数量须为正整数，且不能超过可用库存')), trigger: 'change' }],
  purpose: [{ required: true, whitespace: true, message: '请填写借用用途', trigger: 'blur' }],
  dueDate: [{ required: true, message: '请选择应还日期', trigger: 'change' }],
}
const returnRules = {
  quantity: [{ validator: (_rule, value, callback) => callback(Number.isSafeInteger(value) && value > 0 && value <= (selectedLoan.value?.remainingQuantity || 0) ? undefined : new Error('归还数量须为正整数，且不能超过待还数量')), trigger: 'change' }],
}
const statusType = status => ({ 已归还: 'success', 已逾期: 'danger', 部分归还: 'warning', 借用中: 'primary' }[status] || 'info')
const showError = (error, fallback) => { if (!error?.__handled) ElMessage.error(error?.userMessage || error?.message || fallback) }
const load = async () => {
  loading.value = true
  loadError.value = ''
  try { rows.value = (await http.get('/consumables/loans')).data?.data || [] }
  catch { loadError.value = '借用记录加载失败，请重新加载' }
  finally { loading.value = false }
}
const openBorrow = item => {
  Object.assign(borrowForm, { consumableId: item?.id ? String(item.id) : '', borrower: '', contact: '', quantity: 1, purpose: '', dueDate: '', remark: '', requestId: crypto.randomUUID() })
  borrowVisible.value = true
}
const openReturn = async row => {
  try {
    selectedLoan.value = (await http.get(`/consumables/loans/${row.id}`)).data.data
    if (!selectedLoan.value.remainingQuantity) { ElMessage.info('该借用记录已全部归还'); await load(); return }
    Object.assign(returnForm, { quantity: selectedLoan.value.remainingQuantity, remark: '', requestId: crypto.randomUUID() })
    returnVisible.value = true
  } catch (error) { showError(error, '借用详情加载失败') }
}
const openDetail = async row => {
  try { detail.value = (await http.get(`/consumables/loans/${row.id}`)).data.data; detailVisible.value = true }
  catch (error) { showError(error, '借用详情加载失败') }
}
const saveBorrow = async () => {
  if (saving.value || !(await borrowRef.value?.validate().catch(() => false))) return
  saving.value = true
  try {
    await http.post('/consumables/loans', { ...borrowForm, consumableId: Number(borrowForm.consumableId) })
    borrowVisible.value = false
    ElMessage.success('借用登记成功，库存已扣减')
    emit('changed')
    await load()
  } catch (error) { showError(error, '借用登记失败') }
  finally { saving.value = false }
}
const saveReturn = async () => {
  if (saving.value || !(await returnRef.value?.validate().catch(() => false))) return
  saving.value = true
  try {
    await http.post(`/consumables/loans/${selectedLoan.value.id}/return`, { ...returnForm })
    returnVisible.value = false
    ElMessage.success('归还登记成功，库存已补回')
    emit('changed')
    await load()
  } catch (error) { showError(error, '归还登记失败') }
  finally { saving.value = false }
}
const disabledDueDate = date => { const today = new Date(); today.setHours(0, 0, 0, 0); return date < today }
defineExpose({ openBorrow })
onMounted(load)
</script>

<template>
  <section class="loan-panel">
    <div class="loan-heading">
      <div><h2>耗材借用记录</h2><p>借出扣减可用库存，归还补回库存；支持分次归还。</p></div>
      <div class="loan-actions">
        <el-tag>待归还 {{ outstanding }} 笔</el-tag>
        <el-tag v-if="overdue" type="danger">逾期 {{ overdue }} 笔</el-tag>
        <el-button :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" @click="openBorrow()">借用登记</el-button>
      </div>
    </div>
    <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon><el-button link @click="load">重新加载</el-button></el-alert>
    <div class="loan-filters">
      <el-input v-model="filters.keyword" clearable placeholder="搜索耗材、借用人、用途" @input="currentPage = 1" />
      <el-select v-model="filters.status" clearable placeholder="全部借用状态" @change="currentPage = 1">
        <el-option v-for="status in ['借用中', '部分归还', '已逾期', '已归还']" :key="status" :label="status" :value="status" />
      </el-select>
    </div>
    <el-table v-loading="loading" :data="pagedRecords" empty-text="暂无借用记录">
      <el-table-column prop="id" label="编号" width="75" />
      <el-table-column prop="consumableName" label="耗材" min-width="140" />
      <el-table-column prop="borrower" label="借用人" min-width="100" />
      <el-table-column label="借用 / 已还 / 待还" min-width="165"><template #default="{ row }">{{ row.quantity }} / {{ row.returnedQuantity }} / {{ row.remainingQuantity }} {{ row.unit }}</template></el-table-column>
      <el-table-column prop="borrowedAt" label="借用时间" min-width="165" />
      <el-table-column prop="dueDate" label="应还日期" min-width="115" />
      <el-table-column label="状态" min-width="100"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ row.status }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="145" fixed="right"><template #default="{ row }">
        <el-button link type="primary" @click="openDetail(row)">详情</el-button>
        <el-button v-if="row.remainingQuantity > 0" link type="success" :disabled="saving" @click="openReturn(row)">归还</el-button>
      </template></el-table-column>
    </el-table>
    <TablePagination :current-page="currentPage" :page-size="pageSize" :total="total" @page-change="handleCurrentPageChange" @page-size-change="handlePageSizeChange" />

    <el-dialog v-model="borrowVisible" title="耗材借用登记" width="min(620px, calc(100vw - 32px))" align-center destroy-on-close :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving" append-to-body>
      <el-form ref="borrowRef" :model="borrowForm" :rules="borrowRules" label-position="top" :disabled="saving">
        <el-form-item label="耗材" prop="consumableId"><el-select v-model="borrowForm.consumableId" filterable placeholder="请选择耗材" style="width: 100%">
          <el-option v-for="item in consumables" :key="item.id" :value="String(item.id)" :label="`${item.name}（可用 ${item.stock} ${item.unit}）`" :disabled="item.stock <= 0" />
        </el-select></el-form-item>
        <div class="loan-form-grid">
          <el-form-item label="借用人" prop="borrower"><el-input v-model="borrowForm.borrower" maxlength="100" placeholder="填写实际借用人姓名" /></el-form-item>
          <el-form-item label="联系方式（选填）"><el-input v-model="borrowForm.contact" maxlength="100" placeholder="电话或其他联系方式" /></el-form-item>
          <el-form-item label="借用数量" prop="quantity"><el-input-number v-model="borrowForm.quantity" :min="1" :max="Math.max(1, selectedItem?.stock || 0)" :precision="0" controls-position="right" style="width: 100%" /></el-form-item>
          <el-form-item label="应还日期" prop="dueDate"><el-date-picker v-model="borrowForm.dueDate" value-format="YYYY-MM-DD" type="date" :disabled-date="disabledDueDate" placeholder="请选择应还日期" style="width: 100%" /></el-form-item>
        </div>
        <el-form-item label="借用用途" prop="purpose"><el-input v-model="borrowForm.purpose" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="填写课程、实验或其他用途" /></el-form-item>
        <el-form-item label="备注（选填）"><el-input v-model="borrowForm.remark" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button :disabled="saving" @click="borrowVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveBorrow">确认借用</el-button></template>
    </el-dialog>

    <el-dialog v-model="returnVisible" title="耗材归还登记" width="min(500px, calc(100vw - 32px))" align-center destroy-on-close :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving" append-to-body>
      <p v-if="selectedLoan">{{ selectedLoan.consumableName }} · 借用人：{{ selectedLoan.borrower }} · 尚需归还 {{ selectedLoan.remainingQuantity }} {{ selectedLoan.unit }}</p>
      <el-form ref="returnRef" :model="returnForm" :rules="returnRules" label-position="top" :disabled="saving">
        <el-form-item label="本次归还数量" prop="quantity"><el-input-number v-model="returnForm.quantity" :min="1" :max="selectedLoan?.remainingQuantity || 1" :precision="0" /></el-form-item>
        <el-form-item label="归还备注（选填）"><el-input v-model="returnForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="可记录归还状态或补充说明" /></el-form-item>
      </el-form>
      <template #footer><el-button :disabled="saving" @click="returnVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveReturn">确认归还</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="耗材借用详情" width="min(620px, calc(100vw - 32px))" align-center append-to-body>
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="耗材">{{ detail.consumableName }}</el-descriptions-item>
        <el-descriptions-item label="借用人 / 联系方式">{{ detail.borrower }} / {{ detail.contact || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="借用 / 已还 / 待还">{{ detail.quantity }} / {{ detail.returnedQuantity }} / {{ detail.remainingQuantity }} {{ detail.unit }}</el-descriptions-item>
        <el-descriptions-item label="借用时间 / 应还日期">{{ detail.borrowedAt }} / {{ detail.dueDate }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="statusType(detail.status)">{{ detail.status }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="用途">{{ detail.purpose }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '无' }}</el-descriptions-item>
        <el-descriptions-item label="经办人">{{ detail.operator }}</el-descriptions-item>
        <el-descriptions-item label="最近归还时间">{{ detail.lastReturnedAt || '尚未归还' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer><el-button @click="detailVisible = false">关闭</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.loan-panel { padding: 22px; border: 1px solid var(--border-color); border-radius: 8px; background: var(--surface-color); }
.loan-heading, .loan-actions, .loan-filters { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.loan-heading { justify-content: space-between; margin-bottom: 18px; }
.loan-heading h2 { margin: 0; font-size: 20px; }
.loan-heading p { margin: 8px 0 0; color: var(--text-secondary); font-size: 14px; }
.loan-filters { margin: 16px 0; }
.loan-filters .el-input { width: 260px; }
.loan-filters .el-select { width: 180px; }
.loan-form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 18px; }
@media (max-width: 600px) { .loan-form-grid { grid-template-columns: 1fr; } .loan-filters .el-input, .loan-filters .el-select { width: 100%; } }
</style>
