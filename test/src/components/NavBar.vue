<template>
  <nav class="modern-nav">
    <div class="nav-container">
      <!-- Logo -->
      <div class="nav-logo" @click="$router.push('/')">
        <div class="logo-icon">📸</div>
        <span class="logo-text">PhotoBooker</span>
      </div>

      <!-- 导航菜单 -->
      <div class="nav-links">
        <router-link to="/" class="nav-link" active-class="active">
          <span class="link-icon">🏠</span>
          <span>首页</span>
        </router-link>
        <router-link to="/photographer-list" class="nav-link" active-class="active">
          <span class="link-icon">👨‍🎨</span>
          <span>摄影师</span>
        </router-link>
        <router-link to="/photographer-nearby" class="nav-link" active-class="active">
          <span class="link-icon">📍</span>
          <span>附近</span>
        </router-link>
        <router-link v-if="userStore.isLoggedIn" to="/orders" class="nav-link" active-class="active">
          <span class="link-icon">📋</span>
          <span>订单</span>
        </router-link>
        <router-link to="/ai-chat" class="nav-link ai-link" active-class="active">
          <span class="link-icon">🤖</span>
          <span>AI客服</span>
        </router-link>
        <router-link to="/system-intro" class="nav-link intro-link" active-class="active">
          <span class="link-icon">💡</span>
          <span>系统介绍</span>
        </router-link>
      </div>

      <!-- 用户操作区 -->
      <div class="nav-actions">
        <template v-if="userStore.isLoggedIn">
          <button v-if="userStore.user?.role === 1" class="action-btn admin-btn" @click="goAdmin">
            <span>⚙️</span>
            <span>管理</span>
          </button>
          <router-link to="/profile" class="action-btn profile-btn">
            <img v-if="userStore.user?.avatar" :src="userStore.user.avatar" class="user-avatar" />
            <span v-else class="user-avatar-placeholder">{{ userStore.user?.username?.charAt(0)?.toUpperCase() }}</span>
            <span class="user-name">{{ userStore.user?.username }}</span>
          </router-link>
        </template>
        <router-link v-else to="/login" class="action-btn login-btn">
          <span>登录</span>
        </router-link>
      </div>

      <!-- 移动端菜单按钮 -->
      <button class="mobile-menu-btn" @click="mobileMenuOpen = !mobileMenuOpen">
        <span class="hamburger" :class="{ open: mobileMenuOpen }"></span>
      </button>
    </div>

    <!-- 移动端下拉菜单 -->
    <div v-show="mobileMenuOpen" class="mobile-menu">
      <div class="mobile-menu-grid">
        <router-link to="/" class="mobile-link" @click="mobileMenuOpen = false">
          <span class="link-emoji">🏠</span>
          <span>首页</span>
        </router-link>
        <router-link to="/photographer-list" class="mobile-link" @click="mobileMenuOpen = false">
          <span class="link-emoji">👨‍🎨</span>
          <span>摄影师</span>
        </router-link>
        <router-link to="/photographer-nearby" class="mobile-link" @click="mobileMenuOpen = false">
          <span class="link-emoji">📍</span>
          <span>附近</span>
        </router-link>
        <router-link v-if="userStore.isLoggedIn" to="/orders" class="mobile-link" @click="mobileMenuOpen = false">
          <span class="link-emoji">📋</span>
          <span>订单</span>
        </router-link>
        <router-link to="/ai-chat" class="mobile-link" @click="mobileMenuOpen = false">
          <span class="link-emoji">🤖</span>
          <span>AI客服</span>
        </router-link>
        <router-link to="/system-intro" class="mobile-link" @click="mobileMenuOpen = false">
          <span class="link-emoji">💡</span>
          <span>介绍</span>
        </router-link>
        <template v-if="userStore.isLoggedIn">
          <router-link to="/profile" class="mobile-link" @click="mobileMenuOpen = false">
            <span class="link-emoji">👤</span>
            <span>个人</span>
          </router-link>
          <button class="mobile-link logout" @click="handleLogout; mobileMenuOpen = false">
            <span class="link-emoji">🚪</span>
            <span>退出</span>
          </button>
        </template>
        <router-link v-else to="/login" class="mobile-link" @click="mobileMenuOpen = false">
          <span class="link-emoji">🔑</span>
          <span>登录</span>
        </router-link>
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const mobileMenuOpen = ref(false)

const handleLogout = () => {
  userStore.logout()
  ElMessage.success('退出登录成功')
  router.push('/')
}

const goAdmin = () => {
  router.push('/admin/dashboard')
}

onMounted(() => {
  userStore.initUser()
})
</script>

<style scoped>
@keyframes slideDown {
  from { transform: translateY(-10px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.modern-nav {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(196, 77, 255, 0.1);
  box-shadow: 0 2px 20px rgba(196, 77, 255, 0.08);
  position: sticky;
  top: 0;
  z-index: 1000;
  animation: slideDown 0.4s ease;
}

.nav-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
  height: 70px;
  display: flex;
  align-items: center;
  gap: 32px;
}

/* Logo */
.nav-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.nav-logo:hover {
  transform: scale(1.05);
}

.logo-icon {
  font-size: 32px;
  filter: drop-shadow(0 2px 8px rgba(255, 107, 157, 0.3));
}

.logo-text {
  font-size: 1.4rem;
  font-weight: 800;
  background: linear-gradient(135deg, #ff6b9d, #c44dff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.5px;
}

/* 导航链接 */
.nav-links {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 12px;
  color: #555;
  text-decoration: none;
  font-weight: 600;
  font-size: 0.92rem;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
}

.nav-link:hover {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.08), rgba(196, 77, 255, 0.08));
  color: #c44dff;
  transform: translateY(-2px);
}

.nav-link.active {
  background: linear-gradient(135deg, #ff6b9d, #c44dff);
  color: #fff;
  box-shadow: 0 4px 16px rgba(196, 77, 255, 0.3);
}

.link-icon {
  font-size: 1.2rem;
}

.ai-link {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(196, 77, 255, 0.1));
  border: 2px solid transparent;
}

.ai-link:hover {
  border-color: #c44dff;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.15), rgba(196, 77, 255, 0.15));
}

.intro-link {
  background: linear-gradient(135deg, rgba(108, 92, 231, 0.1), rgba(79, 172, 254, 0.1));
  border: 2px solid transparent;
}

.intro-link:hover {
  border-color: #6c5ce7;
  background: linear-gradient(135deg, rgba(108, 92, 231, 0.15), rgba(79, 172, 254, 0.15));
}

/* 用户操作区 */
.nav-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 12px;
  border: none;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  text-decoration: none;
}

.admin-btn {
  background: linear-gradient(135deg, #ffeaa7, #fdcb6e);
  color: #2d3436;
}

.admin-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(253, 203, 110, 0.4);
}

.profile-btn {
  background: linear-gradient(135deg, #f0e6ff, #ffe0f0);
  color: #2d1b4e;
}

.profile-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(196, 77, 255, 0.2);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #c44dff;
}

.user-avatar-placeholder {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b9d, #c44dff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.9rem;
}

.user-name {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.login-btn {
  background: linear-gradient(135deg, #ff6b9d, #c44dff);
  color: #fff;
  padding: 10px 24px;
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.3);
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(255, 107, 157, 0.4);
}

/* 移动端菜单按钮 */
.mobile-menu-btn {
  display: none;
  width: 40px;
  height: 40px;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 8px;
}

.hamburger {
  display: block;
  width: 24px;
  height: 2px;
  background: #c44dff;
  position: relative;
  transition: all 0.3s;
}

.hamburger::before,
.hamburger::after {
  content: '';
  position: absolute;
  width: 24px;
  height: 2px;
  background: #c44dff;
  transition: all 0.3s;
}

.hamburger::before { top: -8px; }
.hamburger::after { bottom: -8px; }

.hamburger.open {
  background: transparent;
}

.hamburger.open::before {
  top: 0;
  transform: rotate(45deg);
}

.hamburger.open::after {
  bottom: 0;
  transform: rotate(-45deg);
}

/* 移动端下拉菜单 */
.mobile-menu {
  display: none;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  border-top: 1px solid rgba(196, 77, 255, 0.1);
  padding: 16px;
  animation: slideDown 0.3s ease;
}

.mobile-menu-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.mobile-link {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 8px;
  border-radius: 12px;
  color: #555;
  text-decoration: none;
  font-weight: 600;
  font-size: 0.75rem;
  transition: all 0.3s;
  text-align: center;
}

.mobile-link .link-emoji {
  font-size: 1.5rem;
}

.mobile-link:hover,
.mobile-link.router-link-active {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(196, 77, 255, 0.1));
  color: #c44dff;
  transform: scale(1.05);
}

.mobile-link.logout {
  background: rgba(255, 107, 157, 0.1);
  color: #ff6b9d;
  border: none;
  cursor: pointer;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .nav-links {
    display: none;
  }
  
  .mobile-menu-btn {
    display: block;
  }
  
  .mobile-menu {
    display: block;
  }
  
  .user-name {
    display: none;
  }
}

@media (max-width: 768px) {
  .nav-container {
    padding: 0 16px;
    height: 60px;
  }
  
  .logo-text {
    font-size: 1.2rem;
  }
  
  .logo-icon {
    font-size: 28px;
  }
  
  .admin-btn span:not(:first-child) {
    display: none;
  }
}
</style> 