<template>
  <div class="photographer-nearby">
    <!-- Hero Section -->
    <section class="hero-section">
      <div class="hero-container">
        <div class="hero-content">
          <h1 class="hero-title">
            <span class="title-line">📍 附近摄影师</span>
            <span class="title-line highlight">发现身边的专业摄影服务</span>
          </h1>
          <p class="hero-subtitle">基于位置智能推荐，快速找到最近的摄影师</p>
          <div class="hero-controls">
            <el-select v-model="radius" class="radius-select" @change="reSearch">
              <el-option v-for="r in radiusOptions" :key="r" :label="`${r} km`" :value="r" />
            </el-select>
            <button class="btn-primary" @click="locate" :disabled="loading">
              <span v-if="!loading">🔄 重新定位</span>
              <span v-else>⏳ 定位中...</span>
            </button>
          </div>
        </div>
        <div class="hero-visual">
          <div class="floating-card card-1">🗺️</div>
          <div class="floating-card card-2">📍</div>
          <div class="floating-card card-3">📸</div>
        </div>
      </div>
    </section>

    <!-- Loading State -->
    <div v-if="loading" class="nearby-loading">
      <div class="loading-spinner">⏳</div>
      <p>正在定位并查找附近的摄影师...</p>
    </div>

    <!-- Empty States -->
    <el-empty v-else-if="!located" description="未获取到位置，无法检索附近摄影师" />
    <el-empty v-else-if="nearbyList.length === 0" description="附近暂无摄影师" />

    <!-- Photographers Grid -->
    <div v-else class="nearby-grid">
      <div v-for="p in nearbyList" :key="p.id" 
           class="nearby-card" @click="goToDetail(p.id)">
        <div class="card-image">
          <img :src="p.avatar || defaultAvatar" class="photographer-avatar" />
          <div class="status-badge" :class="p.working === 1 ? 'status-working' : 'status-resting'">
            {{ p.working === 1 ? '✓ 接单中' : '休息中' }}
          </div>
        </div>
        
        <div class="card-content">
          <h3 class="photographer-name">{{ p.name }}</h3>
          
          <div class="photographer-details">
            <div class="detail-item distance-item">
              <span class="detail-icon">📍</span>
              <span class="distance-text">距离 {{ formatDistance(p.distanceKm) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-icon">🏆</span>
              <span>{{ p.orderCount || 0 }} 订单</span>
            </div>
            <div class="detail-item">
              <span class="detail-icon">📱</span>
              <span>{{ p.phone }}</span>
            </div>
          </div>
          
          <div class="card-footer">
            <button class="view-btn">查看详情 →</button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="manualVisible" title="手动输入位置" width="420px">
      <el-form label-width="80px">
        <el-form-item label="纬度">
          <el-input v-model="manualLat" placeholder="纬度 [-90, 90]，如 23.1289" />
        </el-form-item>
        <el-form-item label="经度">
          <el-input v-model="manualLng" placeholder="经度 [-180, 180]，如 113.2771" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmManual">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../utils/api'

const router = useRouter()

const radiusOptions = [1, 3, 5, 10, 20]
const radius = ref(5)
const loading = ref(false)
const located = ref(false)
const locationLabel = ref('')
const nearbyList = ref<any[]>([])
const defaultAvatar =
  'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200&h=200&fit=crop&crop=face'

const manualVisible = ref(false)
const manualLat = ref('23.1289')
const manualLng = ref('113.2771')

const formatDistance = (km: number) => (km != null ? km.toFixed(2) : '-') + ' km'

const goToDetail = (id: number) => router.push(`/photographer/${id}`)

const searchNearby = async (lat: number, lng: number) => {
  loading.value = true
  try {
    const res = await api.get('/photo/photographer/nearby', {
      params: { latitude: lat, longitude: lng, radius: radius.value, limit: 20 }
    })
    nearbyList.value = res.data || []
    located.value = true
  } catch (e) {
    // api.ts 响应拦截器已统一 ElMessage 提示
  } finally {
    loading.value = false
  }
}

const reSearch = async () => {
  if (located.value && locationLabel.value) {
    const [lat, lng] = locationLabel.value.split(', ').map(Number)
    await searchNearby(lat, lng)
  }
}

const locate = () => {
  loading.value = true
  if (!navigator.geolocation) {
    loading.value = false
    manualVisible.value = true
    return
  }
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      const lat = pos.coords.latitude
      const lng = pos.coords.longitude
      locationLabel.value = `${lat.toFixed(4)}, ${lng.toFixed(4)}`
      searchNearby(lat, lng)
    },
    () => {
      loading.value = false
      manualVisible.value = true
    },
    { timeout: 10000, maximumAge: 300000 }
  )
}

const confirmManual = () => {
  const lat = parseFloat(manualLat.value)
  const lng = parseFloat(manualLng.value)
  if (Number.isNaN(lat) || Number.isNaN(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
    ElMessage.error('经纬度不合法')
    return
  }
  manualVisible.value = false
  locationLabel.value = `${lat.toFixed(4)}, ${lng.toFixed(4)}`
  searchNearby(lat, lng)
}

onMounted(() => locate())
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

.photographer-nearby {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

/* Hero Section */
.hero-section {
  background: linear-gradient(135deg, #6c5ce7 0%, #c44dff 50%, #ff6b9d 100%);
  border-radius: 24px;
  padding: 60px 40px;
  margin-bottom: 40px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(108, 92, 231, 0.2);
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

.hero-controls {
  display: flex;
  gap: 16px;
  align-items: center;
}

.radius-select {
  width: 140px;
}

.btn-primary {
  padding: 14px 28px;
  border-radius: 16px;
  border: none;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  background: #fff;
  color: #6c5ce7;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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

/* Loading State */
.nearby-loading {
  text-align: center;
  padding: 60px 20px;
  color: #636e72;
}

.loading-spinner {
  font-size: 4rem;
  margin-bottom: 20px;
  animation: float 2s ease-in-out infinite;
}

/* Nearby Grid */
.nearby-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  margin-top: 30px;
}

.nearby-card {
  background: #fff;
  border-radius: 24px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 2px solid transparent;
  box-shadow: 0 4px 20px rgba(196, 77, 255, 0.08);
  animation: pop-in 0.6s ease both;
}

.nearby-card:hover {
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

.nearby-card:hover .photographer-avatar {
  transform: scale(1.1);
}

.status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 6px 12px;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 700;
  backdrop-filter: blur(10px);
}

.status-working {
  background: rgba(103, 194, 58, 0.9);
  color: #fff;
}

.status-resting {
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

.distance-item {
  color: #6c5ce7;
  font-weight: 700;
}

.distance-text {
  color: #6c5ce7;
}

.card-footer {
  text-align: center;
}

.view-btn {
  width: 100%;
  padding: 12px;
  border-radius: 12px;
  font-weight: 700;
  font-size: 1rem;
  background: linear-gradient(135deg, #6c5ce7, #c44dff);
  border: none;
  color: #fff;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.view-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(108, 92, 231, 0.4);
}

/* Responsive */
@media (max-width: 1024px) {
  .hero-container {
    grid-template-columns: 1fr;
  }
  
  .hero-visual {
    display: none;
  }
  
  .nearby-grid {
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
  
  .hero-controls {
    flex-direction: column;
    align-items: stretch;
  }
  
  .radius-select {
    width: 100%;
  }
  
  .nearby-grid {
    grid-template-columns: 1fr;
  }
}
</style>
