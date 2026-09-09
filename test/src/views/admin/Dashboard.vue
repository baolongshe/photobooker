<template>
  <div>
    <h2 style="color: #222; font-size: 20px; font-weight: bold;">仪表盘</h2>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card>
          <div>总用户数：{{ userStats.total }}</div>
          <div>摄影师数：{{ userStats.photographers }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div>订单总数：{{ orderStats.total }}</div>
          <div>完成订单数：{{ orderStats.completed }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span style="color: #222; font-size: 18px; font-weight: bold;">最近订单</span>
          </template>
          <el-table :data="recentOrders" style="width: 100%">
            <el-table-column prop="id" label="订单号" width="100" />
            <el-table-column prop="userName" label="用户" />
            <el-table-column prop="photographerName" label="摄影师" />
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="下单时间">
              <template #default="{ row }">
                {{ formatDateTime(row.createTime) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span style="color: #222; font-size: 18px; font-weight: bold;">系统公告</span>
          </template>
          <div v-for="notice in announcements" :key="notice.id" class="announcement-item">
            <h4>{{ notice.title }}</h4>
            <p>{{ notice.content }}</p>
            <span class="notice-time">{{ notice.time }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/utils/api'
import dayjs from 'dayjs'

const userStats = ref({ total: 0, photographers: 0 })
const orderStats = ref({ total: 0, completed: 0 })
const recentOrders = ref<any[]>([])
const announcements = ref<any[]>([])

const fetchUserStats = async () => {
  const res = await api.get('/photo/user/list')
  userStats.value.total = (res.data || []).length
}
const fetchPhotographerStats = async () => {
  const res = await api.get('/photo/photographer/list')
  userStats.value.photographers = (res.data || []).length
}
const fetchOrderStats = async () => {
  const res = await api.get('/orders/list')
  orderStats.value.total = (res.data || []).length
  orderStats.value.completed = (res.data || []).filter((o: any) => o.status === 'COMPLETED').length
}
const fetchRecentOrders = async () => {
  const res = await api.get('/orders/recent', { params: { limit: 5 } })
  recentOrders.value = (res.data || []).map((order: any) => ({
    ...order,
    userName: order.userName || order.user || '-',
    photographerName: order.photographerName || order.photographer || '-',
  }))
}
const fetchAnnouncements = async () => {
  const res = await api.get('/announcement/list')
  announcements.value = (res.data || []).map((notice: any) => ({
    ...notice,
    time: dayjs(notice.createTime).format('YYYY-MM-DD HH:mm')
  }))
}
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
const formatDateTime = (val: string) => val ? dayjs(val).format('YYYY-MM-DD HH:mm') : ''

onMounted(() => {
  fetchUserStats()
  fetchPhotographerStats()
  fetchOrderStats()
  fetchRecentOrders()
  fetchAnnouncements()
})
</script>

<style scoped>
.announcement-item { padding: 10px 0; border-bottom: 1px solid #f0f0f0; }
.announcement-item:last-child { border-bottom: none; }
.notice-time { color: #909399; font-size: 12px; }
</style> 