<template>
  <div class="order-detail">
    <div class="page-header">
      <el-button @click="$router.go(-1)" icon="ArrowLeft">返回</el-button>
      <h1>订单详情</h1>
    </div>

    <el-card v-if="order" class="order-card">
      <!-- 订单状态 -->
      <div class="order-status">
        <el-steps :active="getStatusStep(order.status)" finish-status="success">
          <el-step title="已预约" description="等待支付"></el-step>
          <el-step title="已确认" description="等待拍摄"></el-step>
          <el-step title="拍摄中" description="进行中"></el-step>
          <el-step title="待确认" description="等待用户确认"></el-step>
          <el-step title="已完成" description="订单完成"></el-step>
        </el-steps>
      </div>

      <!-- 订单信息 -->
      <div class="order-info">
        <el-row :gutter="30">
          <el-col :span="12">
            <h3>订单信息</h3>
            <div class="info-item">
              <span class="label">订单号：</span>
              <span class="value">{{ order.id }}</span>
            </div>
            <div class="info-item">
              <span class="label">订单状态：</span>
              <el-tag :type="getStatusType(order.status)">
                {{ getStatusText(order.status) }}
              </el-tag>
            </div>
            <div class="info-item">
              <span class="label">拍摄套餐：</span>
              <span class="value">{{ order.packageName }}</span>
            </div>
            <div class="info-item">
              <span class="label">拍摄时间：</span>
              <span class="value">{{ formatDateTime(order.shootingTime) }}</span>
            </div>
            <div class="info-item">
              <span class="label">拍摄地点：</span>
              <span class="value">{{ order.shootingLocation }}</span>
            </div>
            <div class="info-item">
              <span class="label">订单金额：</span>
              <span class="value price">¥{{ order.totalPrice }}</span>
            </div>
            <div class="info-item">
              <span class="label">创建时间：</span>
              <span class="value">{{ formatDateTime(order.createTime) }}</span>
            </div>
          </el-col>
          
          <el-col :span="12">
            <h3>摄影师信息</h3>
            <div class="photographer-card">
              <img :src="order.photographer?.avatar" class="photographer-avatar" />
              <div class="photographer-info">
                <h4>{{ order.photographer?.name }}</h4>
                <p>{{ order.photographer?.style }}</p>
                <p>{{ order.photographer?.phone }}</p>
                <el-button 
                  v-if="['CONFIRMED', 'IN_PROGRESS', 'COMPLETED','PENDING'].includes(order.status)" 
                  type="primary" 
                  @click="startChat(order.id)"
                >
                  联系摄影师
                </el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 订单操作 -->
      <div class="order-actions">
        <h3>订单操作</h3>
        <div class="action-buttons">
          <el-button 
            v-if="order.status === 'PENDING'" 
            type="primary" 
            size="large"
            @click="handlePay"
          >
            立即支付
          </el-button>
          <el-button 
            v-if="order.status === 'PENDING'" 
            type="danger" 
            size="large"
            @click="cancelOrder"
          >
            取消订单
          </el-button>
          <el-button 
            v-if="['CONFIRMED', 'IN_PROGRESS', 'COMPLETED'].includes(order.status)"
            type="warning"
            size="large"
            @click="goToAfterSales"
          >
            售后服务
          </el-button>
          <el-button
            v-if="order.status === 'PENDING_CONFIRM'"
            type="success"
            size="large"
            @click="confirmCompleteOrder"
            :loading="confirmLoading"
          >
            确认完成订单
          </el-button>
          <el-button
            v-if="order.status === 'IN_PROGRESS' && isPhotographer"
            type="success" 
            size="large"
            @click="completeOrder"
            :loading="completeLoading"
          >
            完成订单
          </el-button>
          <el-button 
            v-if="order.status === 'COMPLETED'" 
            type="success" 
            size="large"
            @click="viewPhotos"
          >
            查看照片
          </el-button>
          <el-button 
            v-if="order.status === 'COMPLETED' && showRateButton && !hasCommented"
            type="warning" 
            size="large"
            @click="rateOrder"
          >
            评价订单
          </el-button>
        </div>
      </div>
    </el-card>



    <!-- 评价对话框 -->
    <el-dialog v-model="rateDialogVisible" title="评价订单" width="500px">
      <el-form :model="rateForm" label-width="80px" :rules="rateRules" ref="rateFormRef">
        <el-form-item label="评分" prop="rating">
          <el-rate v-model="rateForm.rating" :colors="['#99A9BF', '#F7BA2A', '#FF9900']" />
        </el-form-item>
        <el-form-item label="评价内容" prop="content">
          <el-input 
            v-model="rateForm.content"
            type="textarea" 
            :rows="4"
            placeholder="请分享您的拍摄体验，对其他用户会有很大帮助哦~"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="匿名评价">
          <el-switch v-model="rateForm.isAnonymous" />
          <span style="margin-left: 10px; color: #999; font-size: 12px;">匿名后他人将无法看到您的评价</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRate" :loading="rateLoading">
          提交评价
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import api from '../utils/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

interface Photographer {
  id: number;
  name: string;
  style: string;
  phone: string;
  avatar: string;
  // ... 其他字段
}

interface Order {
  id: number;
  status: string;
  packageName: string;
  shootingTime: string;
  shootingLocation: string;
  totalPrice: number;
  createTime: string;
  photographer: Photographer;
  // ... 其他字段
}

const order = ref<Order | null>(null)
const rateDialogVisible = ref(false)
const rateLoading = ref(false)
const completeLoading = ref(false)
const confirmLoading = ref(false)

const isPhotographer = ref(false)
const hasCommented = ref(false)
const showRateButton = ref(false)

const rateForm = reactive({
  rating: 5,
  content: '',
  isAnonymous: false
})

const rateFormRef = ref()

const rateRules = {
  rating: [
    { required: true, message: '请选择评分', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请填写评价内容', trigger: 'blur' },
    { min: 10, message: '评价内容不能少于 10 个字', trigger: 'blur' }
  ]
}

// 获取订单详情
const fetchOrderDetail = async (orderId: number) => {
  try {
    const res = await api.get(`/orders/${orderId}`)
    console.log('订单API返回数据:', res)
    console.log('res.data:', res.data)

    order.value = res.data.order;
    console.log('order.value:', order.value)

    if (order.value) {
      order.value.photographer = res.data.photographer;
      // 只有已完成的订单才检查是否已评价
      if (order.value.status === 'COMPLETED') {
        checkIfCommented(orderId)
      } else {
        hasCommented.value = false
        showRateButton.value = false
      }
    } else {
      ElMessage.error('订单数据加载失败')
    }
  } catch (error) {
    console.error('获取订单详情失败:', error)
    ElMessage.error('获取订单详情失败')
  }
}

// 检查是否已评价
const checkIfCommented = async (orderId: number) => {
  try {
    // 使用 axios 直接调用，绕过 api 拦截器的全局错误提示
    const response = await fetch('http://localhost:8099/order-comment/order/' + orderId, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    const result = await response.json()
    hasCommented.value = !!result.data
    showRateButton.value = !hasCommented.value
  } catch (error) {
    // 接口返回错误（如：该订单暂无评价）是正常的，不处理
    hasCommented.value = false
    showRateButton.value = true
  }
}

// 获取状态步骤
const getStatusStep = (status: string) => {
  const statusMap = {
    'PENDING': 0,
    'CONFIRMED': 1,
    'IN_PROGRESS': 2,
    'PENDING_UPLOAD': 2,
    'PENDING_CONFIRM': 3,
    'COMPLETED': 4,
    'CANCELED': -1,
    'FAILED': -1,
    'REFUNDING': -1,
    'REFUNDED': -1,
    'REJECTED': -1
  }
  return statusMap[status as keyof typeof statusMap] ?? 0
}

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
    'REJECTED': '已拒绝'
  }
  return statusMap[status as keyof typeof statusMap] || '未知状态'
}

// 格式化日期时间
const formatDateTime = (dateTime: string) => {
  return dayjs(dateTime).format('YYYY-MM-DD HH:mm')
}

// 支付处理
const handlePay = () => {
  // 直接跳转到支付页面
  const totalAmount = order.value?.totalPrice
  const packageName = order.value?.packageName || '摄影套餐'
  router.push(`/payment?orderId=${order.value?.id}&totalAmount=${totalAmount}&packageName=${encodeURIComponent(packageName)}`)
}

// 取消订单
const cancelOrder = async () => {
  if (!userStore.user) {
    ElMessage.error('请先登录');
    router.push({ name: 'login' })
    return;
  }
  try {
    await ElMessageBox.confirm('确定要取消这个订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    // 调用后端取消订单接口
    await api.put(`/orders/${order.value!.id}/cancel`, {})

    ElMessage.success('订单已取消')
    fetchOrderDetail(order.value!.id) // 刷新订单详情
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消订单失败:', error)
      ElMessage.error('取消订单失败，请重试')
    }
  }
}

// 开始聊天
const startChat = (orderId: number) => {
  router.push(`/chat/${orderId}`)
}

// 查看照片
const viewPhotos = () => {
  if (!order.value) return
  router.push({
    path: '/order-photos',
    query: { orderId: order.value.id }
  })
}

// 完成订单（摄影师操作）
const completeOrder = async () => {
  try {
    completeLoading.value = true
    await api.put(`/orders/${order.value!.id}/pending-confirm`, {})

    ElMessage.success('订单已提交，等待用户确认')
    fetchOrderDetail(order.value!.id)
  } catch (error) {
    console.error('完成订单失败:', error)
    ElMessage.error('完成订单失败，请重试')
  } finally {
    completeLoading.value = false
  }
}

// 确认完成订单（用户操作）
const confirmCompleteOrder = async () => {
  try {
    confirmLoading.value = true
    await ElMessageBox.confirm('确定要确认完成这个订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await api.put(`/orders/${order.value!.id}/confirm-complete`, {})
    ElMessage.success('订单已完成')
    fetchOrderDetail(order.value!.id)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('确认完成失败:', error)
      ElMessage.error('确认完成失败，请重试')
    }
  } finally {
    confirmLoading.value = false
  }
}

// 评价订单
const rateOrder = async () => {
  // 直接打开对话框，不再检查
  rateDialogVisible.value = true
}

// 检查是否已存在评价
const checkCommentExists = async () => {
  if (!order.value) return

  try {
    const res = await api.get(`/order-comment/order/${order.value.id}`)
    // 检查响应是否成功且有数据
    if (res.data && res.code === 1) {
      ElMessage.info('您已经评价过该订单了')
      // 显示已评价的内容
      showExistingComment(res.data)
    } else {
      // 没有评价，可以新建
      rateDialogVisible.value = true
    }
  } catch (error) {
    console.log('查询评价结果:', error)
    // 接口返回错误（如：该订单暂无评价），说明可以新建评价
    // 但需要检查订单状态是否允许评价
    if (order.value && order.value.status === 'COMPLETED') {
      rateDialogVisible.value = true
    } else {
      ElMessage.warning('只有已完成的订单才能评价')
    }
  }
}

// 显示已存在的评价
const showExistingComment = (comment) => {
  ElMessageBox.alert(
    `
    <div style="text-align: left;">
      <div style="margin-bottom: 15px;">
        <el-rate
          v-model="${comment.rating}"
          disabled
          :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
        />
      </div>
      <p style="margin-bottom: 10px;"><strong>评价内容：</strong>${comment.content}</p>
      <p style="color: #999; font-size: 12px;">评价时间：${formatDateTime(comment.createTime)}</p>
    </div>
    `,
    '您的评价',
    {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '关闭'
    }
  )
}

// 提交评价
const submitRate = async () => {
  if (!rateFormRef.value) return
  
  await rateFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    if (!userStore.user) {
      ElMessage.error('请先登录')
      router.push({ name: 'login' })
      return
    }

    try {
      rateLoading.value = true

      const submitData = {
        orderId: order.value?.id,
        userId: userStore.user.id,
        photographerId: order.value?.photographer?.id,
        rating: rateForm.rating,
        content: rateForm.content,
        isAnonymous: rateForm.isAnonymous ? 1 : 0
      }

      await api.post('/order-comment', submitData)

      ElMessage.success('评价提交成功！感谢您的反馈')
      rateDialogVisible.value = false

      // 重置表单
      rateForm.rating = 5
      rateForm.content = ''
      rateForm.isAnonymous = false
      if (rateFormRef.value) {
        rateFormRef.value.clearValidate()
      }

      // 刷新订单详情（更新按钮状态）
      fetchOrderDetail(order.value!.id)
    } catch (error) {
      console.error('评价提交失败:', error)
      ElMessage.error('评价提交失败，请重试')
    } finally {
      rateLoading.value = false
    }
  })
}

// 前往售后服务页面
const goToAfterSales = () => {
  router.push({
    path: '/after-sales',
    query: { orderId: order.value?.id }
  })
}

onMounted(() => {
  // 兼容路由参数名为 id
  const orderId = Number(route.params.id);
  console.log('route.params:', route.params, 'orderId:', orderId);
  if (!orderId) {
    ElMessage.error('订单ID无效');
    // 跳转到订单列表，路由 name 应为 'orders'
    router.push({ name: 'orders' });
    return;
  }
  fetchOrderDetail(orderId);
})

onUnmounted(() => {
})
</script>

<style scoped>
.order-detail {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 30px;
}

.page-header h1 {
  margin: 0;
  color: #333;
}

.order-card {
  margin-bottom: 30px;
}

.order-status {
  margin-bottom: 40px;
  padding: 20px 0;
  border-bottom: 1px solid #eee;
}

.order-info {
  margin-bottom: 40px;
}

.order-info h3 {
  margin-bottom: 20px;
  color: #333;
  border-bottom: 2px solid #409eff;
  padding-bottom: 10px;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.info-item .label {
  width: 100px;
  color: #666;
  font-weight: bold;
}

.info-item .value {
  color: #333;
}

.info-item .price {
  color: #e74c3c;
  font-size: 1.2rem;
  font-weight: bold;
}

.photographer-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #f9f9f9;
}

.photographer-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
}

.photographer-info h4 {
  margin: 0 0 10px 0;
  color: #333;
}

.photographer-info p {
  margin: 0 0 5px 0;
  color: #666;
}

.order-actions {
  border-top: 1px solid #eee;
  padding-top: 30px;
}

.order-actions h3 {
  margin-bottom: 20px;
  color: #333;
}

.action-buttons {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.pay-info p {
  margin-bottom: 10px;
  color: #666;
}

.pay-info strong {
  color: #333;
}
</style> 