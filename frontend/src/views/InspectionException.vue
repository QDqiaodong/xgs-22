<template>
  <div class="exception-container">
    <el-card>
      <div class="card-header">
        <el-button plain @click="goBatches">返回巡检批次</el-button>
        <span class="header-title">巡检异常处置清单</span>
      </div>

      <div class="search-bar">
        <el-form :model="filterForm" inline>
          <el-form-item label="批次号">
            <el-input v-model="filterForm.batchNo" placeholder="批次号" clearable style="width: 150px" />
          </el-form-item>
          <el-form-item label="灯组编号">
            <el-input v-model="filterForm.groupCode" placeholder="灯组编号" clearable style="width: 150px" />
          </el-form-item>
          <el-form-item label="异常类型">
            <el-select v-model="filterForm.exceptionType" placeholder="全部" clearable style="width: 120px">
              <el-option
                v-for="opt in ABNORMAL_TYPE_OPTIONS"
                :key="opt.value"
                :value="opt.value"
                :label="opt.label"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 140px">
              <el-option v-for="s in STATUS_OPTIONS" :key="s.value" :value="s.value" :label="s.label" />
            </el-select>
          </el-form-item>
          <el-form-item label="上报人">
            <el-input v-model="filterForm.reporter" placeholder="上报人" clearable style="width: 120px" />
          </el-form-item>
          <el-form-item label="分区变动">
            <el-select v-model="filterForm.zoneChanged" placeholder="全部" clearable style="width: 130px">
              <el-option label="仅看已变化" :value="1" />
              <el-option label="仅看未变化" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="tableData" stripe>
        <el-table-column prop="groupCode" label="灯组编号" width="130" />
        <el-table-column label="所属分区(巡检快照)" min-width="150">
          <template #default="{ row }">
            <span>{{ row.snapshotZoneName }}</span>
            <el-tag v-if="row.zoneChanged === 1" type="danger" size="small" class="ml6">
              当前已划转
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前分区" min-width="140">
          <template #default="{ row }">
            <span v-if="row.currentZoneName">{{ row.currentZoneName }}</span>
            <el-tag v-else type="info" size="small">灯组已不存在</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="异常类型" width="100">
          <template #default="{ row }">
            <el-tag :type="resultTagType(row.exceptionType)">{{ resultLabel(row.exceptionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="siteDescription" label="现场说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="reporter" label="上报人" width="90" />
        <el-table-column prop="reportedAt" label="上报时间" width="160" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row.id)">处置</el-button>
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

    <!-- 处置抽屉 -->
    <el-drawer
      v-model="detailVisible"
      size="62%"
      :title="`异常处置 - ${detail?.exception?.groupCode || ''}`"
      destroy-on-close
    >
      <div v-if="detail" class="detail-wrap">
        <!-- 基本信息 -->
        <el-descriptions :column="2" border size="small" class="mb16">
          <el-descriptions-item label="灯组编号">{{ detail.exception.groupCode }}</el-descriptions-item>
          <el-descriptions-item label="安装位置">{{ detail.exception.installationLocation || '-' }}</el-descriptions-item>
          <el-descriptions-item label="异常类型">
            <el-tag :type="resultTagType(detail.exception.exceptionType)" size="small">
              {{ resultLabel(detail.exception.exceptionType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="statusTagType(detail.exception.status)" size="small">
              {{ statusLabel(detail.exception.status) }}
            </el-tag>
            <span class="version-tag">v{{ detail.exception.version }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="巡检时分区(快照)">{{ detail.exception.snapshotZoneName }}</el-descriptions-item>
          <el-descriptions-item label="当前分区">
            <span>{{ detail.exception.currentZoneName || '灯组已不存在' }}</span>
            <el-tag v-if="detail.exception.zoneChanged === 1" type="danger" size="small" class="ml6">
              分区已变化
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="上报人">{{ detail.exception.reporter }}</el-descriptions-item>
          <el-descriptions-item label="上报时间">{{ detail.exception.reportedAt }}</el-descriptions-item>
          <el-descriptions-item label="原始现场说明" :span="2">
            {{ detail.exception.originalDescription }}
          </el-descriptions-item>
          <el-descriptions-item label="当前现场说明" :span="2">
            {{ detail.exception.siteDescription }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.exception.reviewOpinion" label="处理意见" :span="2">
            {{ detail.exception.reviewOpinion }}（确认人：{{ detail.exception.reviewer }}，
            处理时限：{{ detail.exception.handleDeadline }}）
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.exception.handleResult" label="处理结果" :span="2">
            {{ detail.exception.handleResult }}（处理人：{{ detail.exception.handler }}，
            {{ detail.exception.handledAt }}）
          </el-descriptions-item>
        </el-descriptions>

        <!-- 操作区(按状态) -->
        <el-alert
          v-if="detail.exception.status === 'CLOSED'"
          type="info"
          :closable="false"
          show-icon
          title="该异常已关闭，记录只能查看，不能再改回处理中。"
          class="mb16"
        />

        <!-- 待复核: 确认 / 退回 -->
        <el-card v-if="detail.exception.status === 'PENDING'" shadow="never" class="action-card">
          <template #header>复核操作</template>
          <el-form label-width="90px">
            <el-form-item label="复核人">
              <el-input v-model="actionOperator" placeholder="请输入复核人" style="width: 220px" />
            </el-form-item>
            <el-form-item label="处理意见">
              <el-input v-model="confirmForm.reviewOpinion" type="textarea" :rows="2" placeholder="确认异常时必填" />
            </el-form-item>
            <el-form-item label="处理时限">
              <el-date-picker
                v-model="confirmForm.handleDeadline"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="选择处理时限"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="acting" @click="doConfirm">确认异常</el-button>
            </el-form-item>
            <el-divider>或 退回补充</el-divider>
            <el-form-item label="退回原因">
              <el-input v-model="returnForm.returnReason" type="textarea" :rows="2" placeholder="退回后巡检人需补充说明" />
            </el-form-item>
            <el-form-item>
              <el-button type="danger" plain :loading="acting" @click="doReturn">退回巡检人补充</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 已退回: 巡检人补充 -->
        <el-card v-if="detail.exception.status === 'RETURNED'" shadow="never" class="action-card">
          <template #header>补充现场说明（补充后重新进入待复核）</template>
          <el-form label-width="90px">
            <el-form-item label="巡检人">
              <el-input v-model="actionOperator" placeholder="请输入巡检人" style="width: 220px" />
            </el-form-item>
            <el-form-item label="补充说明">
              <el-input
                v-model="supplementForm.supplementDescription"
                type="textarea"
                :rows="3"
                placeholder="请补充现场说明"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="warning" :loading="acting" @click="doSupplement">提交补充</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 已确认: 登记处理结果 / 关闭 -->
        <el-card v-if="detail.exception.status === 'CONFIRMED'" shadow="never" class="action-card">
          <template #header>处理收口</template>
          <el-form label-width="90px">
            <el-form-item label="处理人">
              <el-input v-model="actionOperator" placeholder="请输入处理人" style="width: 220px" />
            </el-form-item>
            <el-form-item label="处理结果">
              <el-input v-model="handleForm.handleResult" type="textarea" :rows="3" placeholder="请登记处理结果" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="acting" @click="doHandle">登记处理结果</el-button>
              <el-button type="success" :loading="acting" @click="doClose">关闭异常</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 处置时间线 -->
        <el-divider content-position="left">处置记录（退回 / 补充 / 确认 / 处理 / 关闭 全过程）</el-divider>
        <el-timeline>
          <el-timeline-item
            v-for="rec in detail.records"
            :key="rec.id"
            :type="actionTagType(rec.action)"
            :timestamp="rec.operatedAt"
            placement="top"
          >
            <el-tag :type="actionTagType(rec.action)" size="small" class="mr6">
              {{ actionLabel(rec.action) }}
            </el-tag>
            <span class="rec-operator">{{ rec.operator }}</span>
            <div class="rec-content">{{ rec.content }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { inspectionApi } from '@/api'
import {
  RESULT_OPTIONS,
  STATUS_OPTIONS,
  isAbnormal,
  resultLabel,
  resultTagType,
  statusLabel,
  statusTagType,
  actionLabel,
  actionTagType
} from '@/utils/inspection'

const router = useRouter()

const tableData = ref([])
const filterForm = reactive({
  batchNo: '',
  groupCode: '',
  exceptionType: '',
  status: '',
  reporter: '',
  zoneChanged: null
})
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const detailVisible = ref(false)
const detail = ref(null)
const acting = ref(false)
const actionOperator = ref('')
const confirmForm = reactive({ reviewOpinion: '', handleDeadline: '' })
const returnForm = reactive({ returnReason: '' })
const supplementForm = reactive({ supplementDescription: '' })
const handleForm = reactive({ handleResult: '' })

const ABNORMAL_TYPE_OPTIONS = RESULT_OPTIONS.filter(o => isAbnormal(o.value))

const loadList = async () => {
  try {
    const res = await inspectionApi.getExceptionPage({
      ...filterForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.data || []
    pagination.total = res.total || 0
  } catch (e) {
    ElMessage.error(e.message || '加载异常清单失败')
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadList()
}

const handleReset = () => {
  Object.assign(filterForm, {
    batchNo: '',
    groupCode: '',
    exceptionType: '',
    status: '',
    reporter: '',
    zoneChanged: null
  })
  pagination.pageNum = 1
  loadList()
}

const openDetail = async (id) => {
  detailVisible.value = true
  await loadDetail(id)
}

const loadDetail = async (id) => {
  try {
    const res = await inspectionApi.getExceptionDetail(id)
    detail.value = res.data
    // 预填, 便于操作
    actionOperator.value = ''
    confirmForm.reviewOpinion = detail.value.exception.reviewOpinion || ''
    confirmForm.handleDeadline = detail.value.exception.handleDeadline || ''
    returnForm.returnReason = ''
    supplementForm.supplementDescription = detail.value.exception.siteDescription || ''
    handleForm.handleResult = detail.value.exception.handleResult || ''
  } catch (e) {
    ElMessage.error(e.message || '加载详情失败')
  }
}

const requireOperator = () => {
  if (!actionOperator.value.trim()) {
    ElMessage.warning('请填写操作人')
    return false
  }
  return true
}

// 统一处理动作: 409 并发冲突时用后端返回的最新内容刷新, 不覆盖前次结论
const runAction = async (apiCall, payload, successMsg) => {
  if (!requireOperator()) return
  acting.value = true
  try {
    const res = await apiCall({ ...payload, operator: actionOperator.value.trim() })
    detail.value = res.data
    ElMessage.success(successMsg)
    loadList()
  } catch (e) {
    if (e.code === 409) {
      // 记录已被其他人先处理: 明确提示 + 展示最新内容, 本次操作不生效、未重复生成记录
      try {
        await ElMessageBox.alert(e.message || '记录已变化', '操作冲突', {
          type: 'warning',
          confirmButtonText: '查看最新记录'
        })
      } catch { /* 用户关闭 */ }
      await loadDetail(payload.exceptionId)
      loadList()
    } else {
      ElMessage.error(e.message || '操作失败')
    }
  } finally {
    acting.value = false
  }
}

const doConfirm = () => {
  if (!confirmForm.reviewOpinion.trim()) return ElMessage.warning('请填写处理意见')
  if (!confirmForm.handleDeadline) return ElMessage.warning('请选择处理时限')
  runAction(
    inspectionApi.confirm,
    {
      exceptionId: detail.value.exception.id,
      version: detail.value.exception.version,
      reviewOpinion: confirmForm.reviewOpinion,
      handleDeadline: confirmForm.handleDeadline
    },
    '已确认异常'
  )
}

const doReturn = () => {
  if (!returnForm.returnReason.trim()) return ElMessage.warning('请填写退回原因')
  runAction(
    inspectionApi.returnException,
    {
      exceptionId: detail.value.exception.id,
      version: detail.value.exception.version,
      returnReason: returnForm.returnReason
    },
    '已退回巡检人补充'
  )
}

const doSupplement = () => {
  if (!supplementForm.supplementDescription.trim()) return ElMessage.warning('请填写补充说明')
  runAction(
    inspectionApi.supplement,
    {
      exceptionId: detail.value.exception.id,
      version: detail.value.exception.version,
      supplementDescription: supplementForm.supplementDescription
    },
    '补充已提交，重新进入待复核'
  )
}

const doHandle = () => {
  if (!handleForm.handleResult.trim()) return ElMessage.warning('请填写处理结果')
  runAction(
    inspectionApi.registerHandleResult,
    {
      exceptionId: detail.value.exception.id,
      version: detail.value.exception.version,
      handleResult: handleForm.handleResult
    },
    '已登记处理结果'
  )
}

const doClose = () => {
  ElMessageBox.confirm('关闭后记录只能查看、不能再改回处理中。确认关闭？', '关闭确认', {
    type: 'warning'
  })
    .then(() => {
      runAction(
        inspectionApi.close,
        {
          exceptionId: detail.value.exception.id,
          version: detail.value.exception.version
        },
        '异常已关闭'
      )
    })
    .catch(() => {})
}

const goBatches = () => router.push('/inspection')

onMounted(loadList)
</script>

<style lang="scss" scoped>
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;

  .header-title {
    font-size: 16px;
    font-weight: bold;
  }
}
.search-bar {
  margin-bottom: 12px;
}
.pagination {
  margin-top: 16px;
  text-align: right;
}
.detail-wrap {
  padding: 0 8px 24px;
}
.mb16 {
  margin-bottom: 16px;
}
.ml6 {
  margin-left: 6px;
}
.mr6 {
  margin-right: 6px;
}
.version-tag {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
.action-card {
  margin-bottom: 8px;
}
.rec-operator {
  color: #409eff;
  font-size: 13px;
}
.rec-content {
  margin-top: 4px;
  color: #303133;
  font-size: 13px;
  white-space: pre-wrap;
}
</style>
