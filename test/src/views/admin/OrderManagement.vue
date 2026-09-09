<template>
  <div class="order-management">
    <div class="main-card">
      <h2 style="color: #222; font-size: 20px; font-weight: bold;">订单管理</h2>

      <!-- 筛选和操作栏 -->
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
        <div style="display: flex; gap: 12px;">
          <el-select
            v-model="statusFilter"
            placeholder="订单状态"
            clearable
            style="width: 150px;"
            @change="filterOrders"
          >
            <el-option label="待支付" value="PENDING" />
            <el-option label="已确认" value="CONFIRMED" />
            <el-option label="拍摄中" value="IN_PROGRESS" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELED" />
            <el-option label="退款中" value="REFUNDING" />
            <el-option label="已退款" value="REFUNDED" />
          </el-select>
        </div>
        <el-button type="primary" @click="fetchOrders">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <!-- 订单表格 -->
      <el-table
        :data="filteredOrders"
        v-loading="loading"
        class="order-table"
        stripe
        max-height="600"
      >
        <el-table-column prop="id" label="订单号" width="100" />
        <el-table-column prop="userName" label="用户" width="120" />
        <el-table-column prop="photographerName" label="摄影师" width="120" />
        <el-table-column prop="packageName" label="套餐" width="120" />
        <el-table-column prop="totalPrice" label="金额" width="100">
          <template #default="{ row }">¥{{ row.totalPrice }}</template>
        </el-table-column>
        <el-table-column prop="shootingTime" label="拍摄时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.shootingTime) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              @click="viewDetail(row)"
            >
              查看详情
            </el-button>
            <el-button
              v-if="row.status === 'IN_PROGRESS'"
              size="small"
              type="success"
              @click="completeOrder(row)"
            >
              完成订单
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/utils/api'
import { useRouter } from 'vue-router'

const router = useRouter()
const orders = ref<any[]>([])
const filteredOrders = ref<any[]>([])
const loading = ref(false)
const statusFilter = ref('')

// 获取订单列表
const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await api.get('/orders/list')
    orders.value = res.data || []
    filteredOrders.value = [...orders.value]
  } catch (error) {
    console.error('获取订单列表失败:', error)
    ElMessage.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

// 筛选订单
const filterOrders = () => {
  if (!statusFilter.value) {
    filteredOrders.value = [...orders.value]
  } else {
    filteredOrders.value = orders.value.filter((o: any) => o.status === statusFilter.value)
  }
}

// 格式化日期时间
const formatDateTime = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString()
}

// 获取订单状态类型
const getStatusType = (status: string) => {
  const statusMap: Record<string, string> = {
    'PENDING': 'warning',
    'CONFIRMED': 'primary',
    'IN_PROGRESS': 'success',
    'COMPLETED': 'success',
    'CANCELED': 'info',
    'FAILED': 'danger',
    'REFUNDING': 'warning',
    'REFUNDED': 'info'
  }
  return statusMap[status] || 'info'
}

// 获取订单状态文本
const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'PENDING': '待支付',
    'CONFIRMED': '已确认',
    'IN_PROGRESS': '拍摄中',
    'COMPLETED': '已完成',
    'CANCELED': '已取消',
    'FAILED': '失败',
    'REFUNDING': '退款中',
    'REFUNDED': '已退款'
  }
  return map[status] || status
}

// 查看详情
const viewDetail = (row: any) => {
  router.push(`/order/${row.id}`)
}

// 完成订单
const completeOrder = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要完成订单 "${row.id}" 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await api.put(`/orders/${row.id}/complete`, {})
    ElMessage.success('订单已完成')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('完成订单失败:', error)
      ElMessage.error('完成订单失败')
    }
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.order-management {
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

.order-table {
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
</style>
