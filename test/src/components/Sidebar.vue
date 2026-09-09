<template>
  <div class="sidebar-container">
    <!-- 侧边栏 -->
    <div class="sidebar" :class="{ 'sidebar-open': isOpen }">
      <div class="sidebar-header">
        <div class="logo">
          <i class="fa fa-bolt text-primary text-2xl"></i>
          <span class="logo-text">Superman</span>
        </div>
        <button @click="toggleSidebar" class="close-btn">
          <i class="fa fa-times"></i>
        </button>
      </div>
      
      <nav class="sidebar-nav">
        <div class="nav-section">
          <h3 class="nav-title">主要导航</h3>
          <ul class="nav-list">
            <li>
              <router-link to="/" class="nav-link" :class="{ 'active': currentRoute === '/' }">
                <i class="fa fa-home"></i>
                <span>首页</span>
              </router-link>
            </li>
            <li>
              <router-link to="/photographer-list" class="nav-link" :class="{ 'active': currentRoute === '/photographer-list' }">
                <i class="fa fa-camera"></i>
                <span>摄影师</span>
              </router-link>
            </li>
            <!-- 需要登录才能访问的菜单 -->
            <template v-if="userStore.user && userStore.user.token">
              <li>
                <router-link to="/orders" class="nav-link" :class="{ 'active': currentRoute === '/orders' }">
                  <i class="fa fa-list"></i>
                  <span>订单管理</span>
                </router-link>
              </li>
              <li>
                <router-link to="/chat" class="nav-link" :class="{ 'active': currentRoute === '/chat' }">
                  <i class="fa fa-comments"></i>
                  <span>聊天</span>
                </router-link>
              </li>
              <li>
                <router-link to="/profile" class="nav-link" :class="{ 'active': currentRoute === '/profile' }">
                  <i class="fa fa-user"></i>
                  <span>个人中心</span>
                </router-link>
              </li>
              <!-- 技术笔记也只允许登录后访问 -->
              <div class="nav-section">
                <h3 class="nav-title">技术笔记</h3>
                <ul class="nav-list">
                  <li v-for="item in techNotes" :key="item.id">
                    <router-link :to="item.route" class="nav-link" :class="{ 'active': currentRoute === item.route }">
                      <i :class="item.icon"></i>
                      <span>{{ item.label }}</span>
                    </router-link>
                  </li>
                </ul>
              </div>
            </template>
          </ul>
        </div>
      </nav>
      
      <div class="sidebar-footer">
        <div class="user-info">
          <div class="avatar">
            <i class="fa fa-user"></i>
          </div>
          <div class="user-details">
            <p class="username">Superman</p>
            <p class="user-role">Developer</p>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 遮罩层 -->
    <div v-if="isOpen" class="sidebar-overlay" @click="closeSidebar"></div>
    
    <!-- 侧边栏切换按钮 -->
    <button
      @click="toggleSidebar"
      class="sidebar-toggle"
      :style="{ top: toggleTop + 'px', left: toggleLeft + 'px' }"
      @mousedown="startToggleDrag"
    >
      <i class="fa fa-bars"></i>
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const isOpen = ref(false)
const userStore = useUserStore()

const toggleTop = ref(20)
const toggleLeft = ref(20)
let isToggleDragging = false
let dragOffsetX = 0
let dragOffsetY = 0

const startToggleDrag = (e: MouseEvent) => {
  if (e.button !== 0) return
  isToggleDragging = true
  dragOffsetX = e.clientX - toggleLeft.value
  dragOffsetY = e.clientY - toggleTop.value
  document.body.style.cursor = 'move'
  window.addEventListener('mousemove', onToggleDrag)
  window.addEventListener('mouseup', stopToggleDrag)
}

const onToggleDrag = (e: MouseEvent) => {
  if (isToggleDragging) {
    toggleLeft.value = Math.max(0, Math.min(window.innerWidth - 50, e.clientX - dragOffsetX))
    toggleTop.value = Math.max(0, Math.min(window.innerHeight - 50, e.clientY - dragOffsetY))
  }
}

const stopToggleDrag = () => {
  isToggleDragging = false
  document.body.style.cursor = ''
  window.removeEventListener('mousemove', onToggleDrag)
  window.removeEventListener('mouseup', stopToggleDrag)
}

onMounted(() => {
  // 侧边栏初始化逻辑
})

onUnmounted(() => {
  // 清理逻辑
})

const currentRoute = computed(() => route.path)

const mainNavItems = [
  { id: 'home', label: '首页', route: '/', icon: 'fa fa-home' },
  { id: 'photographers', label: '摄影师', route: '/photographer-list', icon: 'fa fa-camera' },
  { id: 'orders', label: '订单管理', route: '/orders', icon: 'fa fa-list' },
  { id: 'chat', label: '聊天', route: '/chat', icon: 'fa fa-comments' },
  { id: 'profile', label: '个人中心', route: '/profile', icon: 'fa fa-user' }
]

const techNotes = [
  { id: 'cors-solution', label: 'Java跨域解决方案', route: '/technical-notes#cors-solution', icon: 'fa fa-code' },
  { id: 'spring-boot', label: 'Spring Boot 笔记', route: '/technical-notes', icon: 'fa fa-leaf' },
  { id: 'vue-notes', label: 'Vue.js 技巧', route: '/technical-notes', icon: 'fa fa-js' },
  { id: 'database', label: '数据库优化', route: '/technical-notes', icon: 'fa fa-database' }
]

const toggleSidebar = () => {
  isOpen.value = !isOpen.value
}

const closeSidebar = () => {
  isOpen.value = false
}
</script>

<style scoped>
.sidebar-container {
  position: relative;
}

.sidebar {
  position: fixed;
  top: 0;
  left: -300px;
  width: 300px;
  height: 100vh;
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
  color: white;
  z-index: 1000;
  transition: left 0.3s ease;
  display: flex;
  flex-direction: column;
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.3);
}

.sidebar-open {
  left: 0;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-text {
  font-size: 1.5rem;
  font-weight: bold;
  color: #3b82f6;
}

.close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 1.2rem;
  cursor: pointer;
  padding: 5px;
  border-radius: 5px;
  transition: background-color 0.3s;
}

.close-btn:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.sidebar-nav {
  flex: 1;
  padding: 20px 0;
  overflow-y: auto;
}

.nav-section {
  margin-bottom: 30px;
}

.nav-title {
  padding: 0 20px 10px;
  font-size: 0.875rem;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.nav-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  color: #cbd5e1;
  text-decoration: none;
  transition: all 0.3s ease;
  border-left: 3px solid transparent;
}

.nav-link:hover {
  background-color: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  border-left-color: #3b82f6;
}

.nav-link.active {
  background-color: rgba(59, 130, 246, 0.2);
  color: #3b82f6;
  border-left-color: #3b82f6;
}

.nav-link i {
  width: 20px;
  text-align: center;
}

.sidebar-footer {
  padding: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.user-details {
  flex: 1;
}

.username {
  font-weight: 600;
  margin: 0;
  font-size: 0.9rem;
}

.user-role {
  margin: 0;
  font-size: 0.8rem;
  color: #94a3b8;
}

.sidebar-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 999;
}

.sidebar-toggle {
  position: fixed;
  top: 20px;
  left: 20px;
  z-index: 998;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  color: white;
  border: none;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
  transition: all 0.3s ease;
}

.sidebar-toggle:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.4);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    width: 280px;
    left: -280px;
  }
  
  .sidebar-toggle {
    top: 15px;
    left: 15px;
    width: 45px;
    height: 45px;
  }
}

/* 滚动条样式 */
.sidebar-nav::-webkit-scrollbar {
  width: 4px;
}

.sidebar-nav::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
}

.sidebar-nav::-webkit-scrollbar-thumb {
  background: rgba(59, 130, 246, 0.5);
  border-radius: 2px;
}

.sidebar-nav::-webkit-scrollbar-thumb:hover {
  background: rgba(59, 130, 246, 0.7);
}
</style> 