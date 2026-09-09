<template>
  <div class="order-list">
    <!-- Hero Section -->
    <section class="hero-section">
      <div class="hero-container">
        <div class="hero-content">
          <h1 class="hero-title">
            <span class="title-line">📋 我的订单</span>
            <span class="title-line highlight">管理您的预约记录</span>
          </h1>
          <p class="hero-subtitle">查看订单状态、支付、联系摄影师</p>
        </div>
        <div class="hero-visual">
          <div class="floating-card card-1">📸</div>
          <div class="floating-card card-2">💳</div>
          <div class="floating-card card-3">✨</div>
        </div>
      </div>
    </section>

    <!-- 订单筛选 -->
    <div class="filter-section">
      <div class="filter-container">
        <el-select v-model="filters.status" placeholder="🔍 订单状态" clearable @change="handleSearch" class="filter-select">
          <el-option label="待支付" value="PENDING" />
          <el-option label="已确认" value="CONFIRMED" />
          <el-option label="拍摄中" value="IN_PROGRESS" />
          <el-option label="待上传照片" value="PENDING_UPLOAD" />
          <el-option label="待确认完成" value="PENDING_CONFIRM" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELED" />
          <el-option label="已失败" value="FAILED" />
          <el-option label="支付成功" value="TRADE_SUCCESS" />
          <el-option label="退款中" value="REFUNDING" />
          <el-option label="已退款" value="REFUNDED" />
          <el-option label="已拒绝" value="REJECTED" />
        </el-select>
        <el-date-picker
          v-model="filters.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          class="filter-date"
        />
        <div class="filter-buttons">
          <el-button type="primary" @click="handleSearch" class="search-btn">🔍 搜索</el-button>
          <el-button @click="resetFilters" class="reset-btn">🔄 重置</el-button>
        </div>
      </div>
    </div>

    <!-- 订单列表 -->
    <div class="orders-grid">
      <div v-for="order in filteredOrders" :key="order.id" class="order-card">
        <div class="card-header">
          <div class="order-id">#{{ order.id }}</div>
          <el-tag :type="getStatusType(order.status)" class="status-tag">
            {{ getStatusText(order.status) }}
          </el-tag>
        </div>

        <div class="card-body">
          <div class="order-image">
            <img
              v-if="order.status === 'COMPLETED'"
              :src="getOrderPhoto(order)"
              class="order-photo-or-avatar"
              @error="handlePhotoError($event, order)"
            />
            <img
              v-else
              :src="order.userAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
              class="user-avatar"
              @error="handleUserAvatarError($event)"
            />
          </div>
          
          <div class="order-details">
            <div class="detail-item">
              <span class="detail-icon">📦</span>
              <span>{{ order.packageName }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-icon">📅</span>
              <span>{{ formatDateTime(order.shootingTime) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-icon">📍</span>
              <span>{{ order.shootingLocation }}</span>
            </div>
          </div>
          
          <div class="order-price">
            <div class="price-label">总价</div>
            <div class="price-value">¥{{ order.totalPrice }}</div>
          </div>
        </div>

        <div class="card-footer">
          <button class="btn-detail" @click="viewOrderDetail(order.id)">查看详情</button>
          <button 
            v-if="order.status === 'PENDING'" 
            class="btn-pay" 
            @click="handlePay(order)"
          >
            💳 立即支付
          </button>
          <button 
            v-if="order.status === 'CONFIRMED'" 
            class="btn-chat" 
            @click="startChat(order.id)"
          >
            💬 联系摄影师
          </button>
          <button 
            v-if="order.status === 'PENDING'" 
            class="btn-cancel" 
            @click="cancelOrder(order.id)"
          >
            ❌ 取消
          </button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>


  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/api'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()
const isPhotographer = ref(false)

enum OrderStatus {
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELED = 'CANCELED',
  FAILED = 'FAILED',
  REFUNDING = 'REFUNDING',
  REFUNDED = 'REFUNDED',
  REJECTED = 'REJECTED'
}

interface Photographer {
  id: number
  name: string
  style: string
  avatar: string
}

interface Order {
  id: number
  userId: number
  photographerId: number
  packageName: string
  totalPrice: number
  shootingTime: string
  shootingLocation: string
  status: OrderStatus
  createTime: string
  updateTime: string
  photographer?: Photographer
  deliveredPhotos?: any[]
  userAvatar?: string
}

// 数据
const orders = ref<Order[]>([])
const filteredOrders = ref<Order[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

// 筛选条件
const filters = reactive({
  status: '',
  dateRange: []
})



// 获取订单列表
const fetchOrders = async () => {
  try {
    loading.value = true
    const res = await api.get('/orders/user', {
      params: {
        page: currentPage.value,
        pageSize: pageSize.value,
        status: filters.status || undefined // ✅ 将状态筛选传给后端
      }
    })
    
    // 处理后端分页响应
    if (res.data && res.data.records) {
      orders.value = res.data.records
      total.value = res.data.total
    } else if (res.list) {
      orders.value = res.list
      total.value = res.total || res.length
    } else {
      orders.value = res || []
      total.value = orders.value.length
    }
    
    console.log('订单列表数据:', orders.value)
    filteredOrders.value = orders.value // 初始化显示全部
  } catch (error) {
    console.error('获取订单列表失败:', error)
    ElMessage.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1 // ✅ 筛选时重置到第一页
  fetchOrders() // ✅ 重新从后端获取筛选后的数据
}

// 重置筛选
const resetFilters = () => {
  Object.assign(filters, {
    status: '',
    dateRange: []
  })
  filteredOrders.value = orders.value
}

// 分页处理
const handleSizeChange = (val: number) => {
  pageSize.value = val
  currentPage.value = 1 // 重置到第一页
  fetchOrders()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  fetchOrders()
}

// 获取状态类型
const getStatusType = (status: OrderStatus) => {
  const statusMap: Record<OrderStatus, string> = {
    PENDING: 'warning',
    CONFIRMED: 'primary',
    IN_PROGRESS: 'success',
    COMPLETED: 'success',
    CANCELED: 'info',
    FAILED: 'danger',
    REFUNDING: 'warning',
    REFUNDED: 'info',
    REJECTED: 'danger'
  }
  return statusMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: OrderStatus) => {
  const statusMap: Record<OrderStatus, string> = {
    PENDING: '待支付',
    CONFIRMED: '已确认',
    IN_PROGRESS: '拍摄中',
    COMPLETED: '已完成',
    CANCELED: '已取消',
    FAILED: '已失败',
    REFUNDING: '退款中',
    REFUNDED: '已退款',
    REJECTED: '已拒绝'
  }
  return statusMap[status] || '未知状态'
}

// 格式化日期时间
const formatDateTime = (dateTime: string) => {
  return dayjs(dateTime).format('YYYY-MM-DD HH:mm')
}

// 查看订单详情
const viewOrderDetail = (orderId: number) => {
  router.push(`/order/${orderId}`)
}

// 支付处理
const handlePay = (order: Order) => {
  // 直接跳转到支付页面
  const totalAmount = order.totalPrice
  const packageName = order.packageName || '摄影套餐'
  router.push(`/payment?orderId=${order.id}&totalAmount=${totalAmount}&packageName=${encodeURIComponent(packageName)}`)
}

// 取消订单
const cancelOrder = async (orderId: number) => {
  try {
    await ElMessageBox.confirm('确定要取消这个订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.put(`/orders/${orderId}/cancel`, null, {
      params: { userId: userStore.user?.id }
    })
    ElMessage.success('订单已取消')
    fetchOrders()
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

const createOrder = async (orderData: Partial<Order>) => {
  try {
    const res = await api.post('/orders/user/create', orderData)
    ElMessage.success('预约成功！')
    fetchOrders()
    // 跳转或关闭弹窗等
  } catch (error) {
    ElMessage.error('预约失败，请重试')
  }
}

const checkPhotographer = async () => {
  if (!userStore.user || !userStore.user.id) return
  const res = await api.get('/photo/photographer/list')
  console.log('user id:', userStore.user.id)
  console.log('photographer list:', res.data)
  const photographer = (res.data || []).find(
    (p: any) => userStore.user && p.userId === userStore.user.id
  )
  console.log('isPhotographer:', !!photographer)
  isPhotographer.value = !!photographer
}

const goToPhotographerOrders = () => {
  console.log('go to photographer orders')
  router.push('/photographer/orders')
}

// 获取订单照片
const getOrderPhoto = (order: Order) => {
  // 如果有交付的照片，返回第一张
  if (order.deliveredPhotos && order.deliveredPhotos.length > 0) {
    return order.deliveredPhotos[0]
  }
  // 否则返回用户头像
  return order.userAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
}

// 处理照片加载失败
const handlePhotoError = (event: Event, order: Order) => {
  const img = event.target as HTMLImageElement
  // 如果是照片加载失败，切换到用户头像
  if (img.src !== order.userAvatar) {
    img.src = order.userAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
  }
}

// 处理用户头像加载失败
const handleUserAvatarError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.src = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
}

// 获取照片标签
const getPhotoLabel = (order: Order) => {
  if (order.deliveredPhotos && order.deliveredPhotos.length > 0) {
    return `已交付 ${order.deliveredPhotos.length} 张照片`
  }
  return '暂无照片'
}

onMounted(() => {
  checkPhotographer()
  fetchOrders()
})
</script>

<style scoped>
@keyframes float {
  0%, 100% { transform: translateY(0px) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(5deg); }
}

@keyframes pop-in {
  0% { opacity: 0; transform: scale(0.8); }
  100% { opacity: 1; transform: scale(1); }
}

.order-list {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

/* Hero Section */
.hero-section {
  background: linear-gradient(135deg, #ff6b9d 0%, #c44dff 50%, #6c5ce7 100%);
  border-radius: 24px;
  padding: 60px 40px;
  margin-bottom: 40px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(196, 77, 255, 0.2);
}

.hero-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  align-items: center;
}

.hero-title {
  font-size: 3rem;
  font-weight: 900;
  line-height: 1.2;
  margin-bottom: 20px;
  letter-spacing: -1px;
  color: #fff;
}

.title-line {
  display: block;
}

.title-line.highlight {
  background: linear-gradient(135deg, #ffeaa7, #fdcb6e);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 1.2rem;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 30px;
}

.hero-visual {
  position: relative;
  height: 200px;
}

.floating-card {
  position: absolute;
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.5rem;
  animation: float 3s ease-in-out infinite;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.card-1 { top: 10%; left: 10%; animation-delay: 0s; }
.card-2 { top: 40%; right: 20%; animation-delay: 0.5s; }
.card-3 { bottom: 10%; left: 30%; animation-delay: 1s; }

/* Filter Section */
.filter-section {
  background: #fff;
  border-radius: 20px;
  padding: 24px;
  margin-bottom: 30px;
  box-shadow: 0 4px 20px rgba(196, 77, 255, 0.08);
}

.filter-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  align-items: center;
}

.filter-select,
.filter-date {
  width: 100%;
}

.filter-buttons {
  display: flex;
  gap: 12px;
}

.search-btn,
.reset-btn {
  flex: 1;
  border-radius: 12px;
  font-weight: 600;
}

/* Orders Grid */
.orders-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 24px;
  margin-bottom: 40px;
}

.order-card {
  background: #fff;
  border-radius: 24px;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 2px solid transparent;
  box-shadow: 0 4px 20px rgba(196, 77, 255, 0.08);
  animation: pop-in 0.6s ease both;
}

.order-card:hover {
  transform: translateY(-8px) scale(1.02);
  border-color: #c44dff;
  box-shadow: 0 12px 40px rgba(196, 77, 255, 0.2);
}

.card-header {
  padding: 16px 20px;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.05), rgba(196, 77, 255, 0.05));
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(196, 77, 255, 0.1);
}

.order-id {
  font-size: 0.9rem;
  font-weight: 700;
  color: #636e72;
}

.status-tag {
  border-radius: 12px;
  font-weight: 600;
}

.card-body {
  padding: 20px;
}

.order-image {
  text-align: center;
  margin-bottom: 16px;
}

.order-photo-or-avatar,
.user-avatar {
  width: 100px;
  height: 100px;
  border-radius: 16px;
  object-fit: cover;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.user-avatar {
  border-radius: 50%;
}

.order-details {
  margin-bottom: 16px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 0.9rem;
  color: #636e72;
}

.detail-icon {
  font-size: 1.2rem;
}

.order-price {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.05), rgba(196, 77, 255, 0.05));
  padding: 16px;
  border-radius: 16px;
  text-align: center;
}

.price-label {
  font-size: 0.85rem;
  color: #636e72;
  margin-bottom: 8px;
}

.price-value {
  font-size: 2rem;
  font-weight: 900;
  background: linear-gradient(135deg, #ff6b9d, #c44dff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.card-footer {
  padding: 16px 20px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
  gap: 8px;
  border-top: 1px solid rgba(196, 77, 255, 0.1);
}

.btn-detail,
.btn-pay,
.btn-chat,
.btn-cancel {
  padding: 10px 16px;
  border-radius: 12px;
  border: none;
  font-weight: 600;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.btn-detail {
  background: linear-gradient(135deg, #f0e6ff, #ffe0f0);
  color: #2d1b4e;
}

.btn-detail:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 16px rgba(196, 77, 255, 0.2);
}

.btn-pay {
  background: linear-gradient(135deg, #ff6b9d, #c44dff);
  color: #fff;
}

.btn-pay:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(196, 77, 255, 0.4);
}

.btn-chat {
  background: linear-gradient(135deg, #6c5ce7, #a29bfe);
  color: #fff;
}

.btn-chat:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(108, 92, 231, 0.4);
}

.btn-cancel {
  background: rgba(255, 107, 157, 0.1);
  color: #ff6b9d;
}

.btn-cancel:hover {
  background: rgba(255, 107, 157, 0.2);
  transform: scale(1.05);
}

/* Pagination */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

/* Responsive */
@media (max-width: 1024px) {
  .hero-container {
    grid-template-columns: 1fr;
  }
  
  .hero-visual {
    display: none;
  }
  
  .orders-grid {
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  }
}

@media (max-width: 768px) {
  .hero-section {
    padding: 40px 20px;
  }
  
  .hero-title {
    font-size: 2rem;
  }
  
  .filter-container {
    grid-template-columns: 1fr;
  }
  
  .orders-grid {
    grid-template-columns: 1fr;
  }
  
  .card-footer {
    grid-template-columns: 1fr;
  }
}
</style> 