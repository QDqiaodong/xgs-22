<template>
  <div class="ledger-container">
    <el-card>
      <div class="search-bar">
        <el-form :model="filterForm" inline>
          <el-form-item label="批次号">
            <el-input v-model="filterForm.batchNo" placeholder="请输入批次号" clearable />
          </el-form-item>
          <el-form-item label="操作人">
            <el-input v-model="filterForm.operator" placeholder="请输入操作人" clearable />
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <el-table 
        :data="tableData" 
        :row-key="(row) => row.id"
        @row-click="handleRowClick"
        stripe
      >
        <el-table-column prop="batchNo" label="批次号" />
        <el-table-column prop="operator" label="操作人" />
        <el-table-column prop="transferTime" label="划转时间" />
        <el-table-column prop="sourceZoneIds" label="源分区ID" />
        <el-table-column prop="targetZoneId" label="目标分区ID" />
        <el-table-column prop="lightCount" label="划转数量" />
        <el-table-column prop="description" label="操作描述" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button type="text" @click="handleViewDetails(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePageChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
    
    <el-dialog v-model="detailDialogVisible" :title="`批次 ${currentLedger?.batchNo} 详情`" width="700px">
      <div class="ledger-summary">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="操作人">{{ currentLedger?.operator }}</el-descriptions-item>
          <el-descriptions-item label="划转时间">{{ currentLedger?.transferTime }}</el-descriptions-item>
          <el-descriptions-item label="源分区ID">{{ currentLedger?.sourceZoneIds }}</el-descriptions-item>
          <el-descriptions-item label="目标分区ID">{{ currentLedger?.targetZoneId }}</el-descriptions-item>
          <el-descriptions-item label="划转数量">{{ currentLedger?.lightCount }}</el-descriptions-item>
          <el-descriptions-item label="操作描述">{{ currentLedger?.description || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      
      <div class="detail-table">
        <h4>划转明细</h4>
        <el-table :data="detailList" stripe>
          <el-table-column prop="lightGroupId" label="灯组ID" />
          <el-table-column prop="sourceZoneId" label="原分区ID" />
          <el-table-column prop="targetZoneId" label="新分区ID" />
        </el-table>
      </div>
      
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { transferApi, zoneApi } from '@/api'

const tableData = ref([])
const detailList = ref([])
const detailDialogVisible = ref(false)
const currentLedger = ref(null)
const dateRange = ref([])

const filterForm = reactive({
  batchNo: '',
  operator: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const loadTable = async () => {
  try {
    const params = {
      ...filterForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }
    
    if (dateRange.value.length === 2) {
      params.startTime = dateRange.value[0] + ' 00:00:00'
      params.endTime = dateRange.value[1] + ' 23:59:59'
    }
    
    const res = await transferApi.getLedgerPage(params)
    tableData.value = res.data
    pagination.total = res.total
  } catch (error) {
    ElMessage.error('加载数据失败')
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadTable()
}

const handleReset = () => {
  Object.assign(filterForm, {
    batchNo: '',
    operator: ''
  })
  dateRange.value = []
  pagination.pageNum = 1
  loadTable()
}

const handlePageChange = () => {
  loadTable()
}

const handleRowClick = (row) => {
  handleViewDetails(row)
}

const handleViewDetails = async (row) => {
  currentLedger.value = row
  detailDialogVisible.value = true
  
  try {
    const res = await transferApi.getLedgerDetails(row.id)
    detailList.value = res.data
  } catch (error) {
    ElMessage.error('加载明细失败')
  }
}

onMounted(() => {
  loadTable()
})
</script>

<style lang="scss" scoped>
.ledger-container {
  height: 100%;
  
  .search-bar {
    margin-bottom: 20px;
  }
  
  .pagination {
    margin-top: 20px;
    text-align: right;
  }
  
  .ledger-summary {
    margin-bottom: 20px;
  }
  
  .detail-table {
    h4 {
      margin-bottom: 15px;
      font-size: 14px;
      font-weight: bold;
    }
  }
}
</style>