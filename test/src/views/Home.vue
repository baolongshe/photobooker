<template>
  <div class="home">
    <AnnouncementBar />

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
            <button class="btn-primary" @click="goToPhotographers">
              <span>立即预约</span>
              <span class="btn-icon">→</span>
            </button>
            <button class="btn-secondary" @click="$router.push('/photographer-nearby')">
              <span>📍 查看附近</span>
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

    <!-- 特色服务 -->
    <section class="features-section">
      <div class="section-header">
        <h2 class="section-title">✨ 为什么选择我们</h2>
        <p class="section-desc">专业、便捷、高品质的摄影约拍体验</p>
      </div>
      <div class="features-grid">
        <div v-for="(feature, idx) in features" :key="feature.id" 
             class="feature-card" 
             :style="{ '--delay': idx * 0.1 + 's' }">
          <div class="feature-icon-wrapper">
            <div class="feature-icon-bg"></div>
            <el-icon class="feature-icon" :size="40">
              <component :is="feature.icon" />
            </el-icon>
          </div>
          <h3>{{ feature.title }}</h3>
          <p>{{ feature.description }}</p>
        </div>
      </div>
    </section>

    <!-- 热门摄影师 -->
    <section v-if="userStore.isLoggedIn" class="photographers-section">
      <div class="section-header">
        <h2 class="section-title">🔥 热门摄影师</h2>
        <p class="section-desc">精选优质摄影师，为你的美好时刻保驾护航</p>
        <router-link to="/photographer-list" class="view-all-btn">
          查看全部 →
        </router-link>
      </div>
      <div class="photographers-grid">
        <div v-for="photographer in hotPhotographers" :key="photographer.id"
             class="photographer-card"
             @click="goToPhotographer(photographer.id)">
          <div class="card-image">
            <img :src="photographer.avatar" alt="摄影师头像" />
            <div class="card-badge">⭐ {{ photographer.orderCount || 0 }}单</div>
          </div>
          <div class="card-content">
            <h3>{{ photographer.name }}</h3>
            <p class="style-tag">{{ photographer.style }}</p>
            <p class="intro-text">{{ photographer.intro }}</p>
            <div class="card-footer">
              <el-rate v-model="photographer.rating" disabled size="small" />
              <span class="price-text">¥{{ photographer.price || 299 }}/起</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- AI 客服入口 -->
    <div id="ai-chat-entry" class="ai-fab">
      <button @click="$router.push('/ai-chat')" title="AI客服">
        <span class="fab-icon">🤖</span>
        <span class="fab-pulse"></span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import AnnouncementBar from '@/components/AnnouncementBar.vue'
import { ChatDotRound } from '@element-plus/icons-vue'
import api from '../utils/api'

const router = useRouter()
const userStore = useUserStore()

const carouselItems = ref([
  {
    id: 1,
    title: '专业摄影服务',
    description: '为您提供高质量的摄影服务，记录美好瞬间',
    image: 'https://images.unsplash.com/photo-1542038784456-1ea8e935640e?w=1200&h=400&fit=crop'
  },
  {
    id: 2,
    title: '多样化拍摄风格',
    description: '婚纱、写真、纪实等多种风格任您选择',
    image: 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=1200&h=400&fit=crop'
  },
  {
    id: 3,
    title: '便捷预约系统',
    description: '在线预约，轻松安排您的拍摄时间',
    image: 'https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=1200&h=400&fit=crop'
  }
])

const features = ref([
  {
    id: 1,
    icon: 'Camera',
    title: '专业摄影',
    description: '经验丰富的摄影师团队，为您提供专业的拍摄服务'
  },
  {
    id: 2,
    icon: 'Calendar',
    title: '灵活预约',
    description: '支持在线预约，时间灵活，满足您的各种需求'
  },
  {
    id: 3,
    icon: 'Star',
    title: '品质保证',
    description: '严格的质量控制，确保每一张照片都完美呈现'
  }
])

const hotPhotographers = ref([])

onMounted(async () => {
  try {
    const res = await api.get('/photo/photographer/list')
    hotPhotographers.value = (res.data || [])
      .sort((a, b) => (b.orderCount || 0) - (a.orderCount || 0))
      .slice(0, 4)
  } catch (e) {
    hotPhotographers.value = []
  }
})

const goToPhotographers = () => {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'login' })
  } else {
    router.push('/photographer-list')
  }
}

const goToPhotographer = (id: number) => {
  router.push(`/photographer/${id}`)
}


</script>

<style scoped>
@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(5deg); }
}
@keyframes gradient-shift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}
@keyframes pop-in {
  0% { transform: scale(0.8) translateY(20px); opacity: 0; }
  70% { transform: scale(1.05); }
  100% { transform: scale(1) translateY(0); opacity: 1; }
}
@keyframes slide-up {
  from { transform: translateY(30px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.home {
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(135deg, #fff5f7 0%, #f0e6ff 50%, #e6f0ff 100%);
  background-size: 200% 200%;
  animation: gradient-shift 12s ease infinite;
  position: relative;
}

/* Hero Section */
.hero-section {
  padding: 80px 24px 60px;
  max-width: 1400px;
  margin: 0 auto;
}

.hero-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 60px;
  align-items: center;
  min-height: 500px;
}

.hero-content {
  animation: slide-up 0.8s ease;
}

.hero-title {
  font-size: 3.5rem;
  font-weight: 900;
  line-height: 1.2;
  margin-bottom: 20px;
  letter-spacing: -1px;
}

.title-line {
  display: block;
}

.title-line.highlight {
  background: linear-gradient(135deg, #ff6b9d, #c44dff, #6c5ce7);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 1.3rem;
  color: #666;
  margin-bottom: 32px;
  font-weight: 500;
}

.hero-actions {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.btn-primary,
.btn-secondary {
  padding: 16px 32px;
  border-radius: 16px;
  font-size: 1.05rem;
  font-weight: 700;
  border: none;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-primary {
  background: linear-gradient(135deg, #ff6b9d, #c44dff);
  color: #fff;
  box-shadow: 0 8px 24px rgba(255, 107, 157, 0.4);
}

.btn-primary:hover {
  transform: translateY(-3px) scale(1.02);
  box-shadow: 0 12px 32px rgba(255, 107, 157, 0.5);
}

.btn-icon {
  font-size: 1.4rem;
  transition: transform 0.3s;
}

.btn-primary:hover .btn-icon {
  transform: translateX(4px);
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  color: #c44dff;
  border: 2px solid rgba(196, 77, 255, 0.2);
}

.btn-secondary:hover {
  background: #fff;
  border-color: #c44dff;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(196, 77, 255, 0.2);
}

/* Hero Visual */
.hero-visual {
  position: relative;
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.floating-card {
  position: absolute;
  width: 120px;
  height: 120px;
  background: linear-gradient(135deg, #fff, #f0e6ff);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3rem;
  box-shadow: 0 12px 40px rgba(196, 77, 255, 0.15);
  animation: float 4s ease-in-out infinite;
}

.card-1 { top: 10%; left: 20%; animation-delay: 0s; }
.card-2 { top: 40%; right: 15%; animation-delay: 1.5s; }
.card-3 { bottom: 15%; left: 30%; animation-delay: 3s; }

/* Features Section */
.features-section {
  padding: 60px 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.section-header {
  text-align: center;
  margin-bottom: 48px;
}

.section-title {
  font-size: 2.5rem;
  font-weight: 800;
  margin-bottom: 12px;
  background: linear-gradient(135deg, #ff6b9d, #c44dff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.section-desc {
  font-size: 1.1rem;
  color: #888;
  font-weight: 500;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
}

.feature-card {
  background: #fff;
  border-radius: 24px;
  padding: 32px 24px;
  text-align: center;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 2px solid transparent;
  box-shadow: 0 4px 20px rgba(196, 77, 255, 0.08);
  animation: pop-in 0.6s ease both;
  animation-delay: var(--delay);
}

.feature-card:hover {
  transform: translateY(-8px) scale(1.02);
  border-color: #c44dff;
  box-shadow: 0 12px 40px rgba(196, 77, 255, 0.2);
}

.feature-icon-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
}

.feature-icon-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #ffe0ec, #e0c3fc);
  border-radius: 20px;
  transform: rotate(6deg);
  transition: transform 0.3s;
}

.feature-card:hover .feature-icon-bg {
  transform: rotate(12deg) scale(1.1);
}

.feature-icon {
  position: relative;
  z-index: 1;
  background: linear-gradient(135deg, #ff6b9d, #c44dff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 2px 8px rgba(196, 77, 255, 0.3));
}

.feature-card h3 {
  font-size: 1.4rem;
  font-weight: 700;
  margin-bottom: 12px;
  color: #2d1b4e;
}

.feature-card p {
  color: #777;
  line-height: 1.6;
}

/* Photographers Section */
.photographers-section {
  padding: 60px 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.view-all-btn {
  display: inline-block;
  margin-top: 16px;
  padding: 12px 24px;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(196, 77, 255, 0.1));
  color: #c44dff;
  border-radius: 12px;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s;
}

.view-all-btn:hover {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.2), rgba(196, 77, 255, 0.2));
  transform: translateX(4px);
}

.photographers-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.photographer-card {
  background: #fff;
  border-radius: 24px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 2px solid transparent;
  box-shadow: 0 4px 20px rgba(196, 77, 255, 0.08);
}

.photographer-card:hover {
  transform: translateY(-8px) scale(1.02);
  border-color: #ff6b9d;
  box-shadow: 0 12px 40px rgba(255, 107, 157, 0.25);
}

.card-image {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s;
}

.photographer-card:hover .card-image img {
  transform: scale(1.1);
}

.card-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 700;
  color: #c44dff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.card-content {
  padding: 20px;
}

.card-content h3 {
  font-size: 1.2rem;
  font-weight: 700;
  margin-bottom: 8px;
  color: #2d1b4e;
}

.style-tag {
  display: inline-block;
  padding: 4px 12px;
  background: linear-gradient(135deg, #ffe0ec, #e0c3fc);
  color: #c44dff;
  border-radius: 12px;
  font-size: 0.85rem;
  font-weight: 600;
  margin-bottom: 12px;
}

.intro-text {
  color: #777;
  font-size: 0.9rem;
  line-height: 1.5;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price-text {
  font-size: 1.1rem;
  font-weight: 800;
  background: linear-gradient(135deg, #ff6b9d, #c44dff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* AI FAB */
.ai-fab {
  position: fixed;
  right: 32px;
  bottom: 32px;
  z-index: 9999;
}

.ai-fab button {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b9d, #c44dff);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 32px rgba(255, 107, 157, 0.4);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
}

.ai-fab button:hover {
  transform: scale(1.1) rotate(-5deg);
  box-shadow: 0 12px 40px rgba(255, 107, 157, 0.5);
}

.fab-icon {
  font-size: 2rem;
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.2));
}

.fab-pulse {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b9d, #c44dff);
  opacity: 0.3;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.3; }
  50% { transform: scale(1.2); opacity: 0; }
}

/* Responsive */
@media (max-width: 1024px) {
  .hero-container {
    grid-template-columns: 1fr;
    gap: 40px;
  }
  
  .hero-visual {
    display: none;
  }
  
  .hero-title {
    font-size: 2.8rem;
  }
}

@media (max-width: 768px) {
  .hero-section {
    padding: 40px 16px 30px;
  }
  
  .hero-title {
    font-size: 2rem;
  }
  
  .hero-subtitle {
    font-size: 1rem;
  }
  
  .hero-actions {
    flex-direction: column;
  }
  
  .btn-primary,
  .btn-secondary {
    width: 100%;
    justify-content: center;
  }
  
  .section-title {
    font-size: 2rem;
  }
  
  .features-grid,
  .photographers-grid {
    grid-template-columns: 1fr;
  }
  
  .ai-fab {
    right: 16px;
    bottom: 16px;
  }
  
  .ai-fab button {
    width: 56px;
    height: 56px;
  }
  
  .fab-icon {
    font-size: 1.7rem;
  }
}
</style> 