<template>
  <div class="refund-management">
    <div class="main-card">
      <h2 style="color: #222; font-size: 20px; font-weight: bold;">退款管理</h2>

      <!-- 筛选和操作栏 -->
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
        <div style="display: flex; gap: 12px;">
          <el-select
            v-model="statusFilter"
            placeholder="退款状态"
            clearable
            style="width: 150px;"
            @change="filterRefunds"
          >
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已批准" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已撤销" value="CANCELLED" />
          </el-select>
        </div>
        <el-button type="primary" @click="fetchRefundList">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <!-- 退款列表表格 -->
      <el-table
        :data="filteredRefunds"
        v-loading="loading"
        class="refund-table"
        stripe
        max-height="600"
      >
        <el-table-column prop="id" label="申请ID" width="100" />
        <el-table-column prop="orderId" label="订单号" width="120" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="photographerName" label="摄影师" width="120" />
        <el-table-column prop="refundAmount" label="退款金额" width="120">
          <template #default="{ row }">
            <span style="color: #e74c3c; font-weight: bold;">¥{{ row.refundAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="refundReason" label="退款原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button 
              size="small" 
              type="success" 
              @click="approveRefund(row)"
              :disabled="row.status !== 'PENDING'"
            >
              批准
            </el-button>
            <el-button 
              size="small" 
              type="danger" 
              @click="showRejectDialog(row)"
              :disabled="row.status !== 'PENDING' && row.status !== 'PROCESSING'"
            >
              拒绝
            </el-button>
            <el-button 
              size="small" 
              @click="viewDetail(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 拒绝原因对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝退款申请" width="500px">
      <el-form :model="rejectForm" label-width="100px">
        <el-form-item label="拒绝原因" required>
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝原因..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject" :loading="rejectLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="退款申请详情" width="700px">
      <div v-if="currentRefund" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请ID">{{ currentRefund.id }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ currentRefund.orderId }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ currentRefund.userId }}</el-descriptions-item>
          <el-descriptions-item label="摄影师ID">{{ currentRefund.photographerId }}</el-descriptions-item>
          <el-descriptions-item label="摄影师">{{ currentRefund.photographerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="退款金额" :span="2">
            <span style="color: #e74c3c; font-weight: bold; font-size: 18px;">¥{{ currentRefund.refundAmount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="退款原因" :span="2">{{ getRefundReasonText(currentRefund.refundReason) }}</el-descriptions-item>
          <el-descriptions-item label="详细描述" :span="2">{{ currentRefund.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentRefund.contactPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentRefund.status)">
              {{ getStatusText(currentRefund.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请时间" :span="2">{{ formatDateTime(currentRefund.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="处理时间" :span="2">{{ currentRefund.handleTime ? formatDateTime(currentRefund.handleTime) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理人">{{ currentRefund.handlerId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理回复" :span="2">{{ currentRefund.handlerResponse || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 凭证图片 -->
        <div v-if="currentRefund.evidenceImages && currentRefund.evidenceImages.length > 0" style="margin-top: 20px;">
          <h4>凭证图片</h4>
          <el-image
            v-for="(img, index) in currentRefund.evidenceImages"
            :key="index"
            :src="img"
            :preview-src-list="currentRefund.evidenceImages"
            fit="cover"
            style="width: 100px; height: 100px; margin-right: 10px; cursor: pointer;"
          />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import api from '../../utils/api'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()

interface RefundRecord {
  id: number
  orderId: number
  userId: number
  userName?: string
  photographerId: number
  photographerName?: string
  refundAmount: number
  refundReason: string
  description?: string
  contactPhone?: string
  status: string
  handlerId?: number
  handlerResponse?: string
  evidenceImages?: string[]
  createTime: string
  handleTime?: string
}

const loading = ref(false)
const refundList = ref<RefundRecord[]>([])
const statusFilter = ref('')
const rejectDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const rejectLoading = ref(false)
const currentRefund = ref<RefundRecord | null>(null)

const rejectForm = ref({
  reason: ''
})

// 过滤后的退款列表
const filteredRefunds = computed(() => {
  if (!statusFilter.value) {
    return refundList.value
  }
  return refundList.value.filter(item => item.status === statusFilter.value)
})

// 获取退款列表
const fetchRefundList = async () => {
  try {
    loading.value = true
    const res = await api.get('/after-sales/list', {
      params: { serviceType: 'REFUND' }
    })
    refundList.value = res.data || []
    
    // 补充摄影师名称
    for (const refund of refundList.value) {
      if (refund.photographerId) {
        try {
          const photoRes = await api.get(`/photo/photographer/${refund.photographerId}`)
          refund.photographerName = photoRes.data?.name || '-'
        } catch (e) {
          refund.photographerName = '-'
        }
      }
    }
  } catch (error) {
    console.error('获取退款列表失败:', error)
    ElMessage.error('获取退款列表失败')
  } finally {
    loading.value = false
  }
}

// 过滤退款
const filterRefunds = () => {
  // 由 computed 自动处理
}

// 批准退款
const approveRefund = async (row: RefundRecord) => {
  try {
    await ElMessageBox.confirm(
      `确定要批准订单 ${row.orderId} 的退款申请吗？退款金额：¥${row.refundAmount}`,
      '确认批准',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await api.post(`/after-sales/${row.id}/approve-refund`)

    ElMessage.success('退款申请已批准')
    fetchRefundList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批准失败:', error)
      ElMessage.error('批准失败')
    }
  }
}

// 显示拒绝对话框
const showRejectDialog = (row: RefundRecord) => {
  currentRefund.value = row
  rejectForm.value.reason = ''
  rejectDialogVisible.value = true
}

// 确认拒绝
const confirmReject = async () => {
  if (!rejectForm.value.reason.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }

  try {
    rejectLoading.value = true
    await api.post(`/after-sales/${currentRefund.value?.id}/reject-refund`, null, {
      params: {
        reason: rejectForm.value.reason
      }
    })

    ElMessage.success('退款申请已拒绝')
    rejectDialogVisible.value = false
    fetchRefundList()
  } catch (error) {
    console.error('拒绝失败:', error)
    ElMessage.error('拒绝失败')
  } finally {
    rejectLoading.value = false
  }
}

// 查看详情
const viewDetail = (row: RefundRecord) => {
  currentRefund.value = row
  detailDialogVisible.value = true
}

// 获取状态类型
const getStatusType = (status: string) => {
  const statusMap: Record<string, string> = {
    'PENDING': 'warning',
    'PROCESSING': 'primary',
    'APPROVED': 'success',
    'REJECTED': 'danger',
    'COMPLETED': 'success',
    'CANCELLED': 'info'
  }
  return statusMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'PENDING': '待处理',
    'PROCESSING': '处理中',
    'APPROVED': '已批准',
    'REJECTED': '已拒绝',
    'COMPLETED': '已完成',
    'CANCELLED': '已撤销'
  }
  return statusMap[status] || '未知状态'
}

// 格式化日期时间
const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 退款原因中文映射
const getRefundReasonText = (reason: string) => {
  const reasonMap: Record<string, string> = {
    'cancel_before_shoot': '拍摄前取消',
    'photographer_reason': '摄影师原因',
    'quality_issue': '服务质量问题',
    'other': '其他原因'
  }
  return reasonMap[reason] || reason
}

onMounted(() => {
  fetchRefundList()
})
</script>

<style scoped>
.refund-management {
  padding: 0;
  background: #fff;
  min-height: 100vh;
}

.main-card {
  background: #fff;
  border-radius: 0;
  box-shadow: none;
  padding: 24px 32px 24px 32px;
  width: 100%;
  max-width: 100%;
  margin: 0;
}

h2 {
  color: #222;
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 24px;
}

.refund-table {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 12px #f0f1f2;
  font-size: 15px;
}

.el-table th {
  background: #f7f8fa;
  color: #333;
  font-weight: 600;
}

.el-table .el-table__row:hover {
  background: #f0f7ff;
}

.detail-content {
  max-height: 600px;
  overflow-y: auto;
}

.detail-content h4 {
  margin: 20px 0 10px 0;
  color: #333;
}
</style>
