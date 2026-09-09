<template>
  <div class="photographer-list">
    <!-- Hero Section -->
    <section class="hero-section">
      <div class="hero-container">
        <div class="hero-content">
          <h1 class="hero-title">
            <span class="title-line">📸 发现专业摄影师</span>
            <span class="title-line highlight">记录你的精彩瞬间</span>
          </h1>
          <p class="hero-subtitle">连接优秀摄影师与热爱生活的你</p>
          <div class="hero-actions">
            <button class="btn-primary" @click="$router.push('/photographer-map')">
              <span>🗺️ 地图查看</span>
            </button>
            <button class="btn-secondary" @click="$router.push('/photographer-nearby')">
              <span>📍 附近摄影师</span>
            </button>
          </div>
        </div>
        <div class="hero-visual">
          <div class="floating-card card-1">🎨</div>
          <div class="floating-card card-2">📷</div>
          <div class="floating-card card-3">✨</div>
        </div>
      </div>
    </section>

    <!-- 热门摄影师展示区 -->
    <div v-if="hotPhotographers.length" class="hot-photographers">
      <h2 class="section-title">🔥 热门摄影师</h2>
      <div class="hot-grid">
        <div v-for="photographer in hotPhotographers" :key="photographer.id" 
             class="hot-card" @click="goToDetail(photographer.id)">
          <img :src="photographer.avatar || 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200&h=200&fit=crop&crop=face'" 
               class="hot-avatar" />
          <div class="hot-name">{{ photographer.name }}</div>
          <div class="hot-stats">
            <span class="stat-item">🏆 {{ photographer.orderCount }} 订单</span>
            <span class="stat-item">🎨 {{ photographer.style }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选条件 -->
    <div class="filter-section">
      <div class="filter-container">
        <el-select v-model="filters.style" placeholder="🎨 拍摄风格" clearable class="filter-select">
          <el-option label="婚纱" value="婚纱" />
          <el-option label="写真" value="写真" />
          <el-option label="纪实" value="纪实" />
          <el-option label="人像" value="人像" />
          <el-option label="商业" value="商业" />
          <el-option label="广告" value="广告" />
        </el-select>
        <el-select v-model="filters.workYears" placeholder="⏱️ 工作年限" clearable class="filter-select">
          <el-option label="1-3年" value="1-3" />
          <el-option label="3-5年" value="3-5" />
          <el-option label="5年以上" value="5+" />
        </el-select>
        <el-select v-model="filters.authStatus" placeholder="✅ 认证状态" clearable class="filter-select">
          <el-option label="已认证" :value="1" />
          <el-option label="未认证" :value="0" />
        </el-select>
        <div class="filter-buttons">
          <el-button type="primary" @click="handleSearch" class="search-btn">🔍 搜索</el-button>
          <el-button @click="resetFilters" class="reset-btn">🔄 重置</el-button>
        </div>
      </div>
    </div>

    <!-- 摄影师列表 -->
    <div class="photographers-grid">
      <div v-for="photographer in filteredPhotographers" :key="photographer.id" 
           class="photographer-card" @click="goToDetail(photographer.id)">
        <div class="card-image">
          <img :src="photographer.avatar || 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200&h=200&fit=crop&crop=face'" 
               class="photographer-avatar" />
          <div class="card-badge" :class="photographer.authStatus === 1 ? 'badge-verified' : 'badge-unverified'">
            {{ photographer.authStatus === 1 ? '✓ 已认证' : '未认证' }}
          </div>
        </div>
        
        <div class="card-content">
          <h3 class="photographer-name">{{ photographer.name }}</h3>
          
          <div class="photographer-details">
            <div class="detail-item">
              <span class="detail-icon">🎨</span>
              <span>{{ photographer.style }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-icon">⏱️</span>
              <span>{{ photographer.workYears }}年经验</span>
            </div>
            <div class="detail-item">
              <span class="detail-icon">📱</span>
              <span>{{ photographer.phone }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-icon">📅</span>
              <span>{{ photographer.availableSchedule }}</span>
            </div>
          </div>
          
          <p class="photographer-intro">{{ photographer.intro }}</p>
          
          <div class="card-footer">
            <el-button type="primary" class="book-btn" @click.stop="handleBook(photographer)">
              📸 立即预约
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[9, 18, 27, 36]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 预约对话框 -->
    <el-dialog v-model="bookingDialogVisible" title="预约摄影师" width="500px">
      <el-form :model="bookingForm" label-width="100px">
        <el-form-item label="摄影师">
          <span>{{ selectedPhotographer?.name }}</span>
        </el-form-item>
        <el-form-item label="拍摄套餐">
          <el-select v-model="bookingForm.packageName" placeholder="请选择拍摄套餐">
            <el-option label="基础套餐 - ¥299" value="基础套餐" />
            <el-option label="标准套餐 - ¥599" value="标准套餐" />
            <el-option label="高级套餐 - ¥999" value="高级套餐" />
            <el-option label="豪华套餐 - ¥1999" value="豪华套餐" />
          </el-select>
        </el-form-item>
        <el-form-item label="拍摄时间">
          <el-date-picker
            v-model="bookingForm.shootingTime"
            type="datetime"
            placeholder="选择拍摄时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="拍摄地点">
          <el-select v-model="bookingForm.shootingLocation" placeholder="请选择地址" filterable allow-create style="width: 100%;">
            <el-option v-for="addr in addressList" :key="addr.id" :label="addr.province + addr.city + addr.district + addr.detail" :value="addr.province + addr.city + addr.district + addr.detail" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bookingDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBooking" :loading="bookingLoading">
          确认预约
        </el-button>
      </template>
    </el-dialog>

    <el-button type="success" @click="becomeDialogVisible = true" v-if="userStore.isLoggedIn && !isPhotographer">
      成为摄影师
    </el-button>

    <el-dialog v-model="becomeDialogVisible" title="申请成为摄影师" width="500px">
      <el-form :model="becomeForm" label-width="100px">
        <el-form-item label="真实姓名">
          <el-input v-model="becomeForm.realName" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="becomeForm.phone" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="becomeForm.intro" type="textarea" />
        </el-form-item>
        
      </el-form>
      <template #footer>
        <el-button @click="becomeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBecome" :loading="becoming">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import api from '../utils/api'

const router = useRouter()
const userStore = useUserStore()

interface Photographer {
  id: number
  name: string
  phone: string
  style: string
  intro: string
  workYears: number
  avatar: string
  authStatus: number
  availableSchedule: string
  createTime: string
  updateTime: string
  orderCount?: number
}

// 数据
const photographers = ref<Photographer[]>([])
const currentPage = ref(1)
const pageSize = ref(9)
const total = ref(0)
const loading = ref(false)

// 筛选条件
const filters = reactive({
  style: '',
  workYears: '',
  authStatus: null
})

// 预约相关
const bookingDialogVisible = ref(false)
const bookingLoading = ref(false)
const selectedPhotographer = ref<Photographer | null>(null)
const bookingForm = reactive({
  packageName: '',
  shootingTime: '',
  shootingLocation: ''
})

const becomeDialogVisible = ref(false)
const becoming = ref(false)
const becomeForm = reactive({
  realName: userStore.user?.realName || '',
  phone: userStore.user?.phone || '',
  intro: ''
})

const isPhotographer = ref(false)

const addressList = ref<any[]>([])

const fetchAddressList = async () => {
  if (!userStore.user?.id) return
  const res = await api.get(`/address/${userStore.user.id}`)
  addressList.value = res.data || []
}

const filteredPhotographers = computed(() => {
  let list = photographers.value
  if (filters.style) {
    list = list.filter(p => p.style?.includes(filters.style))
  }
  if (filters.workYears) {
    if (filters.workYears === '1-3') list = list.filter(p => p.workYears >= 1 && p.workYears <= 3)
    if (filters.workYears === '3-5') list = list.filter(p => p.workYears > 3 && p.workYears <= 5)
    if (filters.workYears === '5+') list = list.filter(p => p.workYears > 5)
  }
  if (filters.authStatus !== null && filters.authStatus !== undefined && filters.authStatus !== '') {
    list = list.filter(p => p.authStatus === filters.authStatus)
  }
  total.value = list.length
  // 分页
  const start = (currentPage.value - 1) * pageSize.value
  return list.slice(start, start + pageSize.value)
})

// 获取摄影师列表
const fetchPhotographers = async () => {
  try {
    loading.value = true
    // 这里调用后端API
    const res = await api.get('/photo/photographer/list')
    photographers.value = res.data || []
    total.value = photographers.value.length
  } catch (error) {
    console.error('获取摄影师列表失败:', error)
    ElMessage.error('获取摄影师列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchPhotographers()
}

// 重置筛选
const resetFilters = () => {
  Object.assign(filters, {
    style: '',
    workYears: '',
    authStatus: null
  })
  handleSearch()
}

// 分页处理
const handleSizeChange = (val: number) => {
  pageSize.value = val
  fetchPhotographers()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  fetchPhotographers()
}

// 跳转到详情页
const goToDetail = (id: number) => {
  router.push(`/photographer/${id}`)
}

// 预约处理
const handleBook = (photographer: any) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push({ name: 'login' })
    return
  }
  selectedPhotographer.value = photographer
  bookingDialogVisible.value = true
  fetchAddressList()
}

// 提交预约
const submitBooking = async () => {
  if (!bookingForm.packageName || !bookingForm.shootingTime || !bookingForm.shootingLocation) {
    ElMessage.warning('请填写完整的预约信息')
    return
  }
  
  try {
    bookingLoading.value = true
    
    // 根据套餐名称确定价格
    const prices: Record<string, number> = {
      '基础套餐': 299,
      '标准套餐': 599,
      '高级套餐': 999,
      '豪华套餐': 1999
    }
    const totalPrice = prices[bookingForm.packageName] || 0

    const orderData = {
      userId: userStore.user?.id,
      photographerId: selectedPhotographer.value?.id,
      packageName: bookingForm.packageName,
      totalPrice: totalPrice,
      shootingTime: bookingForm.shootingTime,
      shootingLocation: bookingForm.shootingLocation,
      status: 'PENDING'
    }
    


    // 调用后端API创建订单
    const response = await api.post('/orders/user/create', orderData, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    })
    
    ElMessage.success('预约成功！正在跳转至支付页面...')
    bookingDialogVisible.value = false
    
    // 跳转到支付页面，传递订单信息
    const orderId = response.data.id
    const totalAmount = totalPrice
    const packageName = bookingForm.packageName
    router.push(`/payment?orderId=${orderId}&totalAmount=${totalAmount}&packageName=${encodeURIComponent(packageName)}`)
  } catch (error) {
    console.error('预约失败:', error)
    ElMessage.error('预约失败，请重试')
  } finally {
    bookingLoading.value = false
  }
}

const getAuthStatusText = (status: number) => {
  switch (status) {
    case 0: return '未认证'
    case 1: return '已认证'
    case 2: return '认证失败'
    default: return '未知'
  }
}

const getAuthStatusType = (status: number) => {
  switch (status) {
    case 0: return 'info'
    case 1: return 'success'
    case 2: return 'danger'
    default: return 'info'
  }
}

const submitBecome = async () => {
  if (!becomeForm.realName || !becomeForm.phone) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    becoming.value = true
    await api.post('/application', {
      userId: userStore.user.id,
      realName: becomeForm.realName,
      phone: becomeForm.phone,
      intro: becomeForm.intro
    })
    ElMessage.success('申请已提交，请等待管理员审核')
    becomeDialogVisible.value = false
  } catch (e) {
    ElMessage.error('申请失败，请重试')
  } finally {
    becoming.value = false
  }
}

// 热门摄影师（按订单完成数排序，取前4名）
const hotPhotographers = computed(() => {
  return [...photographers.value]
    .sort((a, b) => (b.orderCount || 0) - (a.orderCount || 0))
    .slice(0, 4)
})

onMounted(async () => {
  fetchPhotographers()
  // // 判断当前用户是否为摄影师
  // if (!userStore.isLoggedIn || !userStore.user || !userStore.user.id) {
  //   isPhotographer.value = false
  //   return
  // }
  try {
    const res = await api.get(`/photo/photographer/byUser/${(userStore.user as any).id}`)
    isPhotographer.value = !!(res.data && res.data)
  } catch (e) {
    isPhotographer.value = false
  }
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

.photographer-list {
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

.hero-actions {
  display: flex;
  gap: 16px;
}

.btn-primary,
.btn-secondary {
  padding: 14px 28px;
  border-radius: 16px;
  border: none;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.btn-primary {
  background: #fff;
  color: #c44dff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.btn-primary:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  color: #fff;
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.btn-secondary:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-3px);
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

/* Hot Photographers */
.hot-photographers {
  margin-bottom: 40px;
}

.section-title {
  font-size: 2rem;
  font-weight: 800;
  margin-bottom: 24px;
  color: #2d3436;
}

.hot-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
}

.hot-card {
  background: #fff;
  border-radius: 20px;
  padding: 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 2px solid transparent;
  box-shadow: 0 4px 20px rgba(196, 77, 255, 0.08);
  animation: pop-in 0.6s ease both;
}

.hot-card:hover {
  transform: translateY(-8px) scale(1.02);
  border-color: #c44dff;
  box-shadow: 0 12px 40px rgba(196, 77, 255, 0.2);
}

.hot-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  margin-bottom: 12px;
  border: 4px solid #f0e6ff;
}

.hot-name {
  font-size: 1.2rem;
  font-weight: 700;
  color: #2d3436;
  margin-bottom: 12px;
}

.hot-stats {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.stat-item {
  font-size: 0.85rem;
  color: #636e72;
}

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

.filter-select {
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

/* Photographers Grid */
.photographers-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  margin-bottom: 40px;
}

.photographer-card {
  background: #fff;
  border-radius: 24px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 2px solid transparent;
  box-shadow: 0 4px 20px rgba(196, 77, 255, 0.08);
  animation: pop-in 0.6s ease both;
}

.photographer-card:hover {
  transform: translateY(-8px) scale(1.02);
  border-color: #c44dff;
  box-shadow: 0 12px 40px rgba(196, 77, 255, 0.2);
}

.card-image {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.photographer-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s;
}

.photographer-card:hover .photographer-avatar {
  transform: scale(1.1);
}

.card-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 6px 12px;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 700;
  backdrop-filter: blur(10px);
}

.badge-verified {
  background: rgba(103, 194, 58, 0.9);
  color: #fff;
}

.badge-unverified {
  background: rgba(144, 147, 153, 0.9);
  color: #fff;
}

.card-content {
  padding: 20px;
}

.photographer-name {
  font-size: 1.4rem;
  font-weight: 800;
  color: #2d3436;
  margin-bottom: 16px;
}

.photographer-details {
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

.photographer-intro {
  color: #636e72;
  font-size: 0.9rem;
  line-height: 1.6;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  text-align: center;
}

.book-btn {
  width: 100%;
  padding: 12px;
  border-radius: 12px;
  font-weight: 700;
  font-size: 1rem;
  background: linear-gradient(135deg, #ff6b9d, #c44dff);
  border: none;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.book-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(196, 77, 255, 0.4);
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
  
  .photographers-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  }
}

@media (max-width: 768px) {
  .hero-section {
    padding: 40px 20px;
  }
  
  .hero-title {
    font-size: 2rem;
  }
  
  .hero-actions {
    flex-direction: column;
  }
  
  .filter-container {
    grid-template-columns: 1fr;
  }
  
  .photographers-grid {
    grid-template-columns: 1fr;
  }
}
</style> 