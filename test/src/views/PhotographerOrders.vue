<template>
    <div class="photographer-orders">
      <h2 class="order-title">我的订单</h2>
      <el-table :data="orders" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="订单号" width="100" />
        <el-table-column prop="userName" label="用户" />
        <el-table-column prop="packageName" label="套餐">
          <template #default="{ row }">
            <span>{{ row.packageName || '未知套餐' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="shootingTime" label="拍摄时间" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="startChat(row.id)">联系用户</el-button>
            <el-button 
              v-if="row.status === 'CONFIRMED'"
              type="success" 
              size="small"
              style="margin-left: 8px;"
              @click="startShooting(row.id)"
            >
              开始拍摄
            </el-button>
            <el-button
              v-if="row.status === 'CONFIRMED'"
              type="danger"
              size="small"
              style="margin-left: 8px;"
              @click="rejectOrder(row.id)"
            >
              拒绝订单
            </el-button>
            <el-button
              v-if="row.status === 'IN_PROGRESS'"
              type="success"
              size="small"
              style="margin-left: 8px;"
              @click="deliverPhotos(row.id)"
            >
              交付照片
            </el-button>
            <el-button
              v-if="row.status === 'IN_PROGRESS'"
              type="warning"
              size="small"
              style="margin-left: 8px;"
              @click="completeOrder(row.id)"
              :loading="row.completing"
            >
              完成订单
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </template>
  
  <script setup lang="ts">
  import { ref, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import api from '../utils/api'
  import { useUserStore } from '../stores/user'
  
  const router = useRouter()
  const userStore = useUserStore()
  const orders = ref<any[]>([])
  const loading = ref(false)
  const photographerId = ref<number>()

  const getStatusType = (status: string) => {
    const statusMap = {
      'PENDING': 'warning',
      'CONFIRMED': 'primary',
      'IN_PROGRESS': 'success',
      'PENDING_UPLOAD': 'warning',
      'PENDING_CONFIRM': 'warning',
      'COMPLETED': 'success',
      'CANCELED': 'info',
      'FAILED': 'danger',
      'REFUNDING': 'warning',
      'REFUNDED': 'info',
      'REJECTED': 'danger'
    }
    return statusMap[status as keyof typeof statusMap] || 'info'
  }
  
  const getStatusText = (status: string) => {
    const statusMap = {
      'PENDING': '待支付',
      'CONFIRMED': '已确认',
      'IN_PROGRESS': '拍摄中',
      'PENDING_UPLOAD': '待上传照片',
      'PENDING_CONFIRM': '待确认完成',
      'COMPLETED': '已完成',
      'CANCELED': '已取消',
      'FAILED': '已失败',
      'REFUNDING': '退款中',
      'REFUNDED': '已退款',
      'REJECTED': '摄影师拒绝'
    }
    return statusMap[status as keyof typeof statusMap] || '未知状态'
  }
  
  const fetchPhotographerOrders = async () => {
    try {
      loading.value = true
      // 获取当前用户的摄影师信息
      if (!userStore.user?.id) {
        ElMessage.error('请先登录')
        return
      }
      const photographerRes = await api.get(`/photo/photographer/byUser/${userStore.user.id}`)
      const photographer = photographerRes.data
      photographerId.value = photographer.id
      const ordersRes = await api.get(`/photo/photographer/orders/${photographer.id}`)
      orders.value = ordersRes.data || []
    } catch (error) {
      console.error('获取订单失败:', error)
      ElMessage.error('获取订单失败')
    } finally {
      loading.value = false
    }
  }
  
  const startChat = (orderId: number) => {
    router.push(`/chat/${orderId}`)
  }
  
  const startShooting = async (orderId: number) => {
    try {
      await ElMessageBox.confirm('确定要开始拍摄吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })

      await api.put(`/orders/${orderId}/start-shooting`, {})
      ElMessage.success('订单已开始拍摄')
      fetchPhotographerOrders()
    } catch (error) {
      console.error('开始拍摄失败:', error)
      ElMessage.error('开始拍摄失败')
    }
  }

  const deliverPhotos = (orderId: number) => {
    router.push({
      path: '/photographer/deliver-photos',
      query: { orderId }
    })
  }

  const completeOrder = async (orderId: number) => {
    try {
      await ElMessageBox.confirm('确定要完成这个订单吗？请确保照片已交付。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      
      const order = orders.value.find(o => o.id === orderId)
      if (order) {
        order.completing = true
      }
      
      await api.put(`/orders/${orderId}/pending-confirm`, {})
      ElMessage.success('订单已提交，等待用户确认')
      fetchPhotographerOrders()
    } catch (error) {
      if (error !== 'cancel') {
        console.error('完成订单失败:', error)
        ElMessage.error('完成订单失败，请重试')
      }
    } finally {
      const order = orders.value.find(o => o.id === orderId)
      if (order) {
        order.completing = false
      }
    }
  }

  const rejectOrder = async (orderId: number) => {
    try {
      await ElMessageBox.prompt('请输入拒绝原因：', '拒绝订单', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /.+/,
        inputErrorMessage: '请输入拒绝原因',
        type: 'warning'
      }).then(async ({ value }) => {
        await api.put(`/orders/${orderId}/reject`, null, {
          params: {
            photographerId: photographerId.value,
            reason: value
          }
        })
        ElMessage.success('订单已拒绝')
        fetchPhotographerOrders()
      })
    } catch (error) {
      if (error !== 'cancel') {
        console.error('拒绝订单失败:', error)
        ElMessage.error('拒绝订单失败')
      }
    }
  }

  onMounted(() => {
    console.log('PhotographerOrders.vue mounted')
    fetchPhotographerOrders()
  })
  </script>
  
  <style scoped>
  .photographer-orders {
    max-width: 900px;
    margin: 40px auto;
    background: #fff;
    padding: 32px;
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  }
  .order-title {
    font-size: 2rem;
    font-weight: bold;
    color: #409EFF;
    letter-spacing: 2px;
    margin-bottom: 24px;
  }
  </style>