<template>
  <div class="photographer-detail" v-loading="!photographer" element-loading-text="加载中...">
    <div v-if="photographer">
      <div class="photographer-header">
        <el-card>
          <el-row :gutter="30">
            <el-col :span="6">
              <img :src="photographer.avatar" class="photographer-avatar" />
            </el-col>
            <el-col :span="18">
              <div class="photographer-info">
                <h1>{{ photographer.name }}</h1>
                <el-tag :type="photographer.authStatus === 1 ? 'success' : 'info'" size="large">
                  {{ photographer.authStatus === 1 ? '已认证' : '未认证' }}
                </el-tag>
                <p class="photographer-style">{{ photographer.style }}</p>
                <p class="photographer-intro">{{ photographer.intro }}</p>
                <div class="photographer-stats">
                  <div class="stat-item">
                    <span class="stat-number">{{ photographer.workYears }}</span>
                    <span class="stat-label">年经验</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-number">{{ photographer.rating || '暂无' }}</span>
                    <span class="stat-label">评分</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-number">{{ photographer.orderCount || 0 }}</span>
                    <span class="stat-label">完成订单</span>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </div>

      <div class="photographer-content">
        <el-row :gutter="20">
          <el-col :span="16">
            <!-- 作品展示 -->
            <el-card class="portfolio-section">
              <template #header>
                <h3>作品展示</h3>
              </template>
              <div v-if="portfolioLoading" class="loading-container">
                <el-skeleton :rows="3" animated />
              </div>
              <div v-else-if="portfolioItems.length > 0" class="portfolio-grid">
                <div v-for="work in portfolioItems" :key="work.id" class="portfolio-item" @click="goToPortfolioDetail(work.id)">
                  <img :src="work.coverImage" :alt="work.title" />
                  <div class="portfolio-overlay">
                    <div class="portfolio-info">
                      <h4>{{ work.title }}</h4>
                      <p class="portfolio-description">{{ work.description }}</p>
                      <div class="portfolio-meta">
                        <el-tag size="small" type="primary">{{ work.category }}</el-tag>
                        <span class="portfolio-stats">
                          <el-icon><View /></el-icon> {{ work.viewCount }}
                          <el-icon><Star /></el-icon> {{ work.likeCount }}
                        </span>
                      </div>
                      <div v-if="work.tags" class="portfolio-tags">
                        <el-tag 
                          v-for="tag in work.tags.split(',')" 
                          :key="tag" 
                          size="small" 
                          type="info"
                          class="tag-item"
                        >
                          {{ tag.trim() }}
                        </el-tag>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无作品" />
            </el-card>

            <!-- 服务套餐 -->
            <el-card class="packages-section">
              <template #header>
                <h3>服务套餐</h3>
              </template>
              <el-row v-if="packages.length > 0" :gutter="20">
                <el-col :span="12" v-for="pkg in packages" :key="pkg.id">
                  <div class="package-card" :class="{ 'package-recommended': pkg.recommended }">
                    <div class="package-header">
                      <h4>{{ pkg.name }}</h4>
                      <div class="package-price">¥{{ pkg.price }}</div>
                    </div>
                    <ul class="package-features">
                      <li v-for="feature in pkg.features || []" :key="feature">{{ feature }}</li>
                    </ul>
                    <el-button 
                      type="primary" 
                      size="large" 
                      class="package-button"
                      @click="selectPackage(pkg)"
                    >
                      选择此套餐
                    </el-button>
                  </div>
                </el-col>
              </el-row>
              <el-empty v-else description="暂无服务套餐" />
            </el-card>

            <!-- 客户评价 -->
            <el-card class="reviews-section">
              <template #header>
                <div class="reviews-header">
                  <h3>客户评价</h3>
                  <div class="reviews-summary" v-if="reviews.length > 0">
                    <div class="rating-display">
                      <el-rate
                        v-model="averageRating"
                        disabled
                        :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
                      />
                      <span class="rating-number">{{ averageRating.toFixed(1) }}</span>
                    </div>
                    <span class="review-count">共{{ reviews.length }}条评价</span>
                  </div>
                </div>
              </template>

              <div v-if="reviewsLoading" class="loading-container">
                <el-skeleton :rows="3" animated />
              </div>

              <div v-else-if="reviews.length > 0" class="reviews-list">
                <div v-for="review in reviews" :key="review.id" class="review-item">
                  <div class="review-header">
                    <div class="reviewer-info">
                      <img
                        :src="review.userAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                        class="reviewer-avatar"
                      />
                      <div class="reviewer-name">
                        {{ review.isAnonymous ? '匿名用户' : (review.userName || '用户') }}
                      </div>
                    </div>
                    <div class="review-rating">
                      <el-rate
                        v-model="review.rating"
                        disabled
                        :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
                      />
                    </div>
                  </div>
                  <div class="review-content">
                    {{ review.content }}
                  </div>
                  <div class="review-meta">
                    <span class="review-time">{{ formatDateTime(review.createTime) }}</span>
                    <span class="review-order" v-if="review.orderId">订单号：{{ review.orderId }}</span>
                  </div>
                </div>

                <!-- 加载更多 -->
                <div class="load-more" v-if="hasMoreReviews">
                  <el-button @click="loadMoreReviews" :loading="loadingMore">
                    加载更多
                  </el-button>
                </div>
              </div>

              <el-empty v-else description="暂无评价" />
            </el-card>
          </el-col>

          <el-col :span="8">
            <!-- 预约表单 -->
            <el-card class="booking-section">
              <template #header>
                <h3>立即预约</h3>
              </template>
              <el-form :model="bookingForm" label-position="top">
                <el-form-item label="选择套餐">
                  <el-select v-model="bookingForm.packageId" placeholder="请选择套餐">
                    <el-option 
                      v-for="pkg in packages"
                      :key="pkg.id"
                      :label="`${pkg.name} - ¥${pkg.price}`"
                      :value="pkg.id"
                    />
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
                  <el-input v-model="bookingForm.shootingLocation" placeholder="请输入拍摄地点" />
                </el-form-item>
                <el-form-item>
                  <el-button 
                    type="primary" 
                    size="large" 
                    class="booking-button"
                    @click="submitBooking"
                    :loading="bookingLoading"
                  >
                    确认预约
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>

            <!-- 联系方式 -->
            <el-card class="contact-section">
              <template #header>
                <h3>联系方式</h3>
              </template>
              <div class="contact-info">
                <p><el-icon><Phone /></el-icon> {{ photographer.phone }}</p>
                <p><el-icon><Location /></el-icon> {{ photographer.location || '暂无地址' }}</p>
                <p><el-icon><Clock /></el-icon> {{ photographer.availableSchedule }}</p>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 预约确认对话框 -->
      <el-dialog v-model="bookingDialogVisible" title="请确认预约信息" width="500px">
        <div v-if="selectedPackage" class="booking-confirm">
          <p><strong>摄影师：</strong>{{ photographer.name }}</p>
          <p><strong>套餐：</strong>{{ selectedPackage.name }} - ¥{{ selectedPackage.price }}</p>
          <p><strong>拍摄时间：</strong>{{ formatDateTime(bookingForm.shootingTime) }}</p>
          <p><strong>拍摄地点：</strong>{{ bookingForm.shootingLocation }}</p>
        </div>
        <template #footer>
          <el-button @click="bookingDialogVisible = false">返回修改</el-button>
          <el-button type="primary" @click="confirmBooking" :loading="bookingLoading">
            确认并提交订单
          </el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import api from '@/utils/api'
import { Phone, Location, Clock, View, Star } from '@element-plus/icons-vue'

// Define interfaces for better type safety
interface PortfolioItem {
  id: number;
  title: string;
  description: string;
  coverImage: string;
  category: string;
  tags: string;
  viewCount: number;
  likeCount: number;
  isFeatured: boolean;
  createTime: string;
}

interface Package {
  id: number;
  name: string;
  price: number;
  features: string[];
  recommended: boolean;
}

interface Photographer {
  id: number;
  name: string;
  style: string;
  intro: string;
  workYears: number;
  authStatus: number;
  rating: number;
  orderCount: number;
  phone: string;
  location: string;
  availableSchedule: string;
  avatar: string;
  packages: Package[];
}

interface Review {
  id: number;
  orderId: number;
  userId: number;
  photographerId: number;
  rating: number;
  content: string;
  isAnonymous: number;
  userName?: string;
  userAvatar?: string;
  createTime: string;
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const photographer = ref<Photographer | null>(null)
const portfolioItems = ref<PortfolioItem[]>([])
const loading = ref(false)
const portfolioLoading = ref(false)
const bookingLoading = ref(false)
const reviewsLoading = ref(false)
const loadingMore = ref(false)

const packages = ref<Package[]>([])
const reviews = ref<Review[]>([])
const averageRating = ref<number>(0)
const hasMoreReviews = ref<boolean>(true)
const reviewPage = ref<number>(1)
const reviewPageSize = ref<number>(10)

const bookingForm = reactive({
  packageId: '',
  shootingTime: '',
  shootingLocation: ''
})

const bookingDialogVisible = ref(false)
const selectedPackage = ref<Package | null>(null)

// Fetch photographer details from the backend
const fetchPhotographerDetails = async (id: string | string[]) => {
  try {
    loading.value = true
    const res = await api.get(`/photo/photographer/${id}`)
    const photographerData = res.data
    if (!photographerData) {
      throw new Error("Photographer data is null or undefined from API");
    }
    photographer.value = photographerData
    // 获取套餐列表
    await fetchPackages(photographerData.id)
    // 获取摄影师作品
    await fetchPhotographerPortfolio(Number(id))
    // 获取摄影师评价
    await fetchPhotographerReviews(photographerData.id)
  } catch (error) {
    console.error('获取摄影师详情失败:', error)
    ElMessage.error('获取摄影师详情失败')
  } finally {
    loading.value = false
  }
}

const fetchPackages = async (photographerId: number) => {
  try {
    const res = await api.get(`/photo/photographer/package/list/${photographerId}`)
    packages.value = res.data || []
  } catch (error) {
    packages.value = []
  }
}

// 获取摄影师作品
const fetchPhotographerPortfolio = async (photographerId: number) => {
  try {
    portfolioLoading.value = true
    const res = await api.get(`/portfolio/photographer/${photographerId}/featured?limit=6`)
    if (Array.isArray(res.data)) {
      portfolioItems.value = res.data
    } else if (res.data) {
      portfolioItems.value = [res.data]
    } else {
      portfolioItems.value = []
    }
  } catch (error) {
    console.error('获取摄影师作品失败:', error)
    // 如果获取失败，不显示错误，因为作品不是必需的
  } finally {
    portfolioLoading.value = false
  }
}

// 获取摄影师评价
const fetchPhotographerReviews = async (photographerId: number, append = false) => {
  try {
    if (!append) {
      reviewsLoading.value = true
    } else {
      loadingMore.value = true
    }

    const res = await api.get(`/order-comment/photographer/${photographerId}/rating`, {
      params: {
        page: reviewPage.value,
        pageSize: reviewPageSize.value
      }
    })

    if (res.data) {
      // 设置平均评分
      averageRating.value = res.data.average || 0

      // 处理评价列表
      const newReviews = (res.data.comments || []).map((comment: any) => ({
        ...comment,
        rating: comment.rating || 0
      }))

      if (append) {
        reviews.value = [...reviews.value, ...newReviews]
      } else {
        reviews.value = newReviews
      }

      // 判断是否还有更多数据
      hasMoreReviews.value = newReviews.length === reviewPageSize.value
    }
  } catch (error) {
    console.error('获取摄影师评价失败:', error)
    // 如果获取失败，不显示错误
  } finally {
    if (!append) {
      reviewsLoading.value = false
    } else {
      loadingMore.value = false
    }
  }
}

// 加载更多评价
const loadMoreReviews = () => {
  reviewPage.value++
  fetchPhotographerReviews(photographer.value!.id, true)
}

onMounted(() => {
  const photographerId = route.params.id
  if (photographerId) {
    fetchPhotographerDetails(photographerId)
  }
})

// 格式化日期时间
const formatDateTime = (dateTime: string) => {
  if (!dateTime) return ''
  return dayjs(dateTime).format('YYYY-MM-DD HH:mm')
}

// 选择套餐 - 更新表单值
const selectPackage = (pkg: Package) => {
  bookingForm.packageId = pkg.id
  ElMessage.success(`已选择套餐: ${pkg.name}`)
}

// 提交预约 - 显示确认对话框
const submitBooking = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push({ name: 'login' })
    return
  }
  if (!bookingForm.packageId || !bookingForm.shootingTime || !bookingForm.shootingLocation) {
    ElMessage.warning('请填写完整的预约信息')
    return
  }
  const foundPackage = packages.value.find(p => p.id === bookingForm.packageId)
  if (foundPackage) {
    selectedPackage.value = foundPackage
    bookingDialogVisible.value = true
  } else {
    ElMessage.error('未找到所选套餐，请重新选择')
  }
}

// 确认预约 - 发送API请求
const confirmBooking = async () => {
  if (!selectedPackage.value || !photographer.value) {
    ElMessage.error('预约信息不完整，请重试')
    return
  }
  try {
    bookingLoading.value = true
    const orderData = {
      userId: userStore.user?.id,
      photographerId: photographer.value.id,
      packageName: selectedPackage.value.name,
      totalPrice: selectedPackage.value.price,
      shootingTime: bookingForm.shootingTime,
      shootingLocation: bookingForm.shootingLocation,
      status: 'PENDING'
    }
    const response = await api.post('/orders/user/create', orderData)
    ElMessage.success('预约成功！正在跳转至支付页面...')
    bookingDialogVisible.value = false
    
    const orderId = response.data.id
    const totalAmount = selectedPackage.value.price
    const packageName = selectedPackage.value.name
    router.push(`/payment?orderId=${orderId}&totalAmount=${totalAmount}&packageName=${encodeURIComponent(packageName)}`)
  } catch (error) {
    console.error('预约失败:', error)
    ElMessage.error('预约失败，请检查后重试')
  } finally {
    bookingLoading.value = false
  }
}

// 跳转到作品详情页
const goToPortfolioDetail = (workId: number) => {
  router.push({ name: 'PortfolioDetail', params: { id: workId } })
}
</script>

<style scoped>
.photographer-detail {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.photographer-header {
  margin-bottom: 30px;
}

.photographer-avatar {
  width: 200px;
  height: 200px;
  border-radius: 50%;
  object-fit: cover;
}

.photographer-info h1 {
  margin: 0 0 15px 0;
  color: #333;
}

.photographer-style {
  font-size: 1.2rem;
  color: #409eff;
  margin: 15px 0;
}

.photographer-intro {
  color: #666;
  line-height: 1.6;
  margin-bottom: 20px;
}

.photographer-stats {
  display: flex;
  gap: 30px;
}

.stat-item {
  text-align: center;
}

.stat-number {
  display: block;
  font-size: 2rem;
  font-weight: bold;
  color: #409eff;
}

.stat-label {
  color: #666;
  font-size: 0.9rem;
}

.photographer-content {
  margin-bottom: 40px;
}

.portfolio-section,
.packages-section,
.reviews-section {
  margin-bottom: 30px;
}

.reviews-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.reviews-summary {
  display: flex;
  align-items: center;
  gap: 15px;
}

.rating-display {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rating-number {
  font-size: 1.5rem;
  font-weight: bold;
  color: #FF9900;
}

.review-count {
  color: #999;
  font-size: 0.9rem;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.review-item {
  padding: 20px;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #fafafa;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.reviewer-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.reviewer-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.reviewer-name {
  font-size: 1rem;
  color: #333;
  font-weight: 500;
}

.review-content {
  color: #666;
  line-height: 1.6;
  margin-bottom: 15px;
  font-size: 0.95rem;
}

.review-meta {
  display: flex;
  justify-content: space-between;
  color: #999;
  font-size: 0.85rem;
}

.review-time {
  margin-right: 15px;
}

.review-order {
  color: #666;
}

.load-more {
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.loading-container {
  padding: 20px;
}

.portfolio-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.portfolio-item {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  cursor: pointer;
}

.portfolio-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.portfolio-item img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  display: block;
}

.portfolio-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.8));
  color: white;
  padding: 20px;
  transform: translateY(100%);
  transition: transform 0.3s ease;
}

.portfolio-item:hover .portfolio-overlay {
  transform: translateY(0);
}

.portfolio-info h4 {
  margin: 0 0 8px 0;
  font-size: 1.1rem;
  font-weight: 600;
}

.portfolio-description {
  margin: 0 0 12px 0;
  font-size: 0.9rem;
  line-height: 1.4;
  opacity: 0.9;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.portfolio-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.portfolio-stats {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8rem;
  opacity: 0.8;
}

.portfolio-stats .el-icon {
  margin-right: 2px;
}

.portfolio-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tag-item {
  margin: 0;
}

.package-card {
  border: 2px solid #eee;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  transition: all 0.3s;
  margin-bottom: 20px;
}

.package-card:hover {
  border-color: #409eff;
  transform: translateY(-5px);
}

.package-recommended {
  border-color: #409eff;
  background: linear-gradient(135deg, #f0f8ff 0%, #e6f3ff 100%);
}

.package-header {
  margin-bottom: 20px;
}

.package-header h4 {
  margin: 0 0 10px 0;
  color: #333;
}

.package-price {
  font-size: 2rem;
  font-weight: bold;
  color: #e74c3c;
}

.package-features {
  list-style: none;
  padding: 0;
  margin: 0 0 20px 0;
}

.package-features li {
  padding: 8px 0;
  color: #666;
  border-bottom: 1px solid #eee;
}

.package-features li:last-child {
  border-bottom: none;
}

.package-button {
  width: 100%;
}

.booking-section,
.contact-section {
  margin-bottom: 20px;
}

.booking-button {
  width: 100%;
}

.contact-info p {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
  color: #666;
}

.booking-confirm p {
  margin-bottom: 10px;
  color: #666;
}

.booking-confirm strong {
  color: #333;
}

.booking-section .el-form-item {
  margin-bottom: 20px;
}

.booking-section .el-select {
  width: 100%;
}
</style> 