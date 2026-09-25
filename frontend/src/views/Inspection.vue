<template>
  <div class="inspection-container">
    <!-- 批次列表 -->
    <el-card v-show="mode === 'list'">
      <div class="card-header">
        <el-button type="primary" :icon="Plus" @click="openCreate">创建巡检批次</el-button>
        <el-button type="warning" plain @click="goException">进入异常处置</el-button>
      </div>

      <div class="search-bar">
        <el-form :model="filterForm" inline>
          <el-form-item label="批次号">
            <el-input v-model="filterForm.batchNo" placeholder="请输入批次号" clearable />
          </el-form-item>
          <el-form-item label="巡检人">
            <el-input v-model="filterForm.inspector" placeholder="请输入巡检人" clearable />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 120px">
              <el-option label="暂存中" :value="0" />
              <el-option label="已提交" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="tableData" stripe>
        <el-table-column prop="batchNo" label="批次号" width="190" />
        <el-table-column prop="inspector" label="巡检人/上报人" width="120" />
        <el-table-column prop="zoneNames" label="巡检分区" min-width="180" show-overflow-tooltip />
        <el-table-column label="登记进度" width="110">
          <template #default="{ row }">{{ row.recordedCount }}/{{ row.totalCount }}</template>
        </el-table-column>
        <el-table-column prop="abnormalCount" label="异常数" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="batchStatusType(row.status)">{{ batchStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column prop="submittedAt" label="提交时间" width="170">
          <template #default="{ row }">{{ row.submittedAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              type="primary"
              link
              @click="openRegister(row)"
            >继续登记/补录</el-button>
            <el-button type="info" link @click="openRegister(row, true)">查看</el-button>
            <el-button
              v-if="row.status === 0"
              type="success"
              link
              :disabled="row.recordedCount < row.totalCount"
              @click="handleSubmit(row)"
            >提交批次</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- 创建批次对话框 -->
    <el-dialog v-model="createVisible" title="创建巡检批次" width="560px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="巡检人" prop="inspector">
          <el-input v-model="createForm.inspector" placeholder="请输入巡检人(上报人)" />
        </el-form-item>
        <el-form-item label="巡检分区" prop="zoneIds">
          <el-select
            v-model="createForm.zoneIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="可选择一个或多个分区"
            style="width: 100%"
          >
            <el-option
              v-for="zone in zoneList"
              :key="zone.id"
              :value="zone.id"
              :label="zone.zoneName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="confirmCreate">创建并开始登记</el-button>
      </template>
    </el-dialog>

    <!-- 登记页 -->
    <el-card v-if="mode === 'register' && currentBatch" class="register-card">
      <template #header>
        <div class="register-header">
          <el-button :icon="Back" link @click="backToList">返回批次列表</el-button>
          <el-divider direction="vertical" />
          <span class="register-title">
            巡检批次 {{ currentBatch.batchNo }}
            <el-tag :type="batchStatusType(currentBatch.status)" size="small" class="ml8">
              {{ batchStatusLabel(currentBatch.status) }}
            </el-tag>
          </span>
          <div class="register-meta">
            <span>分区：{{ currentBatch.zoneNames }}</span>
            <span>巡检人：{{ currentBatch.inspector }}</span>
            <span>进度：{{ recordedCount }}/{{ itemRows.length }}</span>
          </div>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="逐个登记灯组结果；标记为熄灭/频闪/亮度不足时必须填写现场说明。可随时“暂存”后继续补录；提交后原始结果将固化、不可再修改。"
        class="mb16"
      />

      <el-table :data="itemRows" stripe border max-height="520">
        <el-table-column prop="groupCode" label="灯组编号" width="140" />
        <el-table-column prop="zoneName" label="巡检时分区(快照)" width="150" />
        <el-table-column prop="installationLocation" label="安装位置" min-width="160" show-overflow-tooltip />
        <el-table-column label="巡检结果" width="280">
          <template #default="{ row }">
            <el-radio-group
              v-model="row.inspectResult"
              :disabled="readonly"
              @change="(val) => handleResultChange(row, val)"
            >
              <el-radio-button v-for="opt in RESULT_OPTIONS" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </el-radio-button>
            </el-radio-group>
          </template>
        </el-table-column>
        <el-table-column label="现场说明（异常必填）" min-width="240">
          <template #default="{ row }">
            <el-input
              v-model="row.siteDescription"
              type="textarea"
              :rows="2"
              :disabled="readonly || !isAbnormal(row.inspectResult)"
              :placeholder="isAbnormal(row.inspectResult) ? '请描述现场情况' : '正常无需填写'"
            />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.inspectResult" :type="resultTagType(row.inspectResult)" size="small">
              {{ resultLabel(row.inspectResult) }}
            </el-tag>
            <el-tag v-else type="info" size="small">未登记</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="register-actions" v-if="!readonly">
        <el-button type="primary" :loading="saving" @click="handleSave">暂存（可继续补录）</el-button>
        <el-button type="success" :loading="submitting" @click="handleSubmitFromRegister">
          提交并生成异常清单
        </el-button>
        <span class="tip">已暂存 {{ savedCount }} 项，全部登记后才能提交</span>
      </div>
      <div class="register-actions" v-else>
        <el-button type="warning" plain @click="goException">前往异常处置查看</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Back } from '@element-plus/icons-vue'
import { zoneApi, inspectionApi } from '@/api'
import {
  RESULT_OPTIONS,
  isAbnormal,
  resultLabel,
  resultTagType,
  batchStatusLabel,
  batchStatusType
} from '@/utils/inspection'

const router = useRouter()

const mode = ref('list') // list | register
const tableData = ref([])
const zoneList = ref([])
const currentBatch = ref(null)
const itemRows = ref([])
const savedCount = ref(0)

const filterForm = reactive({ batchNo: '', inspector: '', status: null })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const createVisible = ref(false)
const creating = ref(false)
const createFormRef = ref(null)
const createForm = reactive({ inspector: '', zoneIds: [], remark: '' })
const createRules = {
  inspector: [{ required: true, message: '请输入巡检人', trigger: 'blur' }],
  zoneIds: [{ required: true, type: 'array', min: 1, message: '请至少选择一个分区', trigger: 'change' }]
}

const saving = ref(false)
const submitting = ref(false)

const readonly = computed(() => currentBatch.value && currentBatch.value.status === 1)
const recordedCount = computed(() => itemRows.value.filter(r => r.inspectResult).length)

const loadZones = async () => {
  try {
    const res = await zoneApi.getAll()
    zoneList.value = res.data || []
  } catch (e) {
    ElMessage.error('加载分区失败')
  }
}

const loadList = async () => {
  try {
    const res = await inspectionApi.getBatchPage({
      ...filterForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.data || []
    pagination.total = res.total || 0
  } catch (e) {
    ElMessage.error(e.message || '加载批次失败')
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadList()
}

const handleReset = () => {
  filterForm.batchNo = ''
  filterForm.inspector = ''
  filterForm.status = null
  pagination.pageNum = 1
  loadList()
}

const openCreate = () => {
  createForm.inspector = ''
  createForm.zoneIds = []
  createForm.remark = ''
  createVisible.value = true
}

const confirmCreate = async () => {
  if (!createFormRef.value) return
  await createFormRef.value.validate(async (valid) => {
    if (!valid) return
    creating.value = true
    try {
      const res = await inspectionApi.createBatch({ ...createForm })
      ElMessage.success('批次已创建，请逐个登记灯组结果')
      createVisible.value = false
      await enterRegister(res.data.id)
    } catch (e) {
      ElMessage.error(e.message || '创建批次失败')
    } finally {
      creating.value = false
    }
  })
}

const openRegister = async (row, viewOnly = false) => {
  await enterRegister(row.id, viewOnly)
}

const enterRegister = async (batchId) => {
  try {
    const res = await inspectionApi.getBatchDetail(batchId)
    currentBatch.value = res.data.batch
    itemRows.value = (res.data.items || []).map(it => ({
      id: it.id,
      lightGroupId: it.lightGroupId,
      groupCode: it.groupCode,
      zoneName: it.zoneName,
      installationLocation: it.installationLocation,
      inspectResult: it.inspectResult || null,
      siteDescription: it.siteDescription || ''
    }))
    savedCount.value = itemRows.value.filter(r => r.inspectResult).length
    mode.value = 'register'
  } catch (e) {
    ElMessage.error(e.message || '加载登记页失败')
  }
}

const backToList = () => {
  mode.value = 'list'
  currentBatch.value = null
  itemRows.value = []
  loadList()
}

const handleResultChange = (row, val) => {
  if (!isAbnormal(val)) {
    row.siteDescription = ''
  }
}

// 收集当前已填写结果的灯组
const collectFilled = () => {
  return itemRows.value
    .filter(r => r.inspectResult)
    .map(r => ({
      lightGroupId: r.lightGroupId,
      inspectResult: r.inspectResult,
      siteDescription: isAbnormal(r.inspectResult) ? r.siteDescription : null
    }))
}

const validateFilled = () => {
  for (const r of itemRows.value) {
    if (r.inspectResult && isAbnormal(r.inspectResult) && !(r.siteDescription || '').trim()) {
      ElMessage.error(`灯组 ${r.groupCode} 标记为「${resultLabel(r.inspectResult)}」，必须填写现场说明`)
      return false
    }
  }
  return true
}

const handleSave = async () => {
  if (!validateFilled()) return
  const items = collectFilled()
  if (items.length === 0) {
    ElMessage.warning('请至少登记一个灯组结果再暂存')
    return
  }
  saving.value = true
  try {
    const res = await inspectionApi.saveItems({ batchId: currentBatch.value.id, items })
    ElMessage.success(`已暂存 ${items.length} 项，可继续补录`)
    currentBatch.value = res.data.batch
    savedCount.value = res.data.batch.recordedCount
  } catch (e) {
    ElMessage.error(e.message || '暂存失败')
  } finally {
    saving.value = false
  }
}

const handleSubmitFromRegister = async () => {
  if (recordedCount.value < itemRows.value.length) {
    ElMessage.warning(`还有 ${itemRows.value.length - recordedCount.value} 个灯组未登记，全部登记后才能提交`)
    return
  }
  if (!validateFilled()) return
  try {
    await ElMessageBox.confirm(
      '提交后原始巡检结果将固化、不能再修改，并生成异常清单。确认提交？',
      '提交确认',
      { type: 'warning', confirmButtonText: '确认提交', cancelButtonText: '再检查一下' }
    )
  } catch {
    return
  }
  submitting.value = true
  try {
    // 提交前先把当前页全部结果暂存一次, 避免漏存
    await inspectionApi.saveItems({ batchId: currentBatch.value.id, items: collectFilled() })
    const res = await inspectionApi.submitBatch(currentBatch.value.id)
    ElMessage.success(`提交成功，共形成 ${res.data.abnormalCount} 条异常`)
    await enterRegister(currentBatch.value.id)
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

const handleSubmit = async (row) => {
  try {
    await ElMessageBox.confirm(`确认提交批次 ${row.batchNo}？提交后生成异常清单且结果不可改。`, '提交确认', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    const res = await inspectionApi.submitBatch(row.id)
    ElMessage.success(`提交成功，共形成 ${res.data.abnormalCount} 条异常`)
    loadList()
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  }
}

const goException = () => {
  router.push('/inspection-exception')
}

onMounted(() => {
  loadZones()
  loadList()
})
</script>

<style lang="scss" scoped>
.card-header {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
.search-bar {
  margin-bottom: 12px;
}
.pagination {
  margin-top: 16px;
  text-align: right;
}
.register-card {
  .register-header {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 6px;

    .register-title {
      font-weight: bold;
      font-size: 15px;
    }
    .register-meta {
      display: flex;
      gap: 18px;
      margin-left: auto;
      color: #606266;
      font-size: 13px;
    }
  }
  .register-actions {
    margin-top: 16px;
    display: flex;
    align-items: center;
    gap: 12px;

    .tip {
      color: #909399;
      font-size: 13px;
    }
  }
}
.mb16 {
  margin-bottom: 16px;
}
.ml8 {
  margin-left: 8px;
}
</style>
