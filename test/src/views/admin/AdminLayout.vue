<template>
  <el-container class="admin-layout">
    <el-aside width="240px" class="admin-sidebar">
      <div class="sidebar-header">
        <div class="logo">
          <span class="logo-icon">⚙️</span>
          <span class="logo-text">管理后台</span>
        </div>
      </div>
      
      <el-menu 
        :default-active="$route.path" 
        router
        class="admin-menu"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/photographer-list">
          <el-icon><Avatar /></el-icon>
          <span>摄影师管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><Document /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/refund-management">
          <el-icon><RefreshLeft /></el-icon>
          <span>退款管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/applications">
          <el-icon><Checked /></el-icon>
          <span>申请审核</span>
        </el-menu-item>
        <el-menu-item index="/admin/announcements">
          <el-icon><Bell /></el-icon>
          <span>公告管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/rag-management">
          <el-icon><Reading /></el-icon>
          <span>知识库管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <el-container class="admin-main-container">
      <el-header class="admin-header">
        <div class="header-left">
          <h2 class="page-title">{{ pageTitle }}</h2>
        </div>
        <div class="header-right">
          <el-button type="text" @click="$router.push('/')">
            <el-icon><HomeFilled /></el-icon>
            返回首页
          </el-button>
        </div>
      </el-header>
      
      <el-main class="admin-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  DataAnalysis,
  User,
  Avatar,
  Document,
  RefreshLeft,
  Checked,
  Bell,
  Reading,
  HomeFilled
} from '@element-plus/icons-vue'

const route = useRoute()

const pageTitle = computed(() => {
  const titles: Record<string, string> = {
    '/admin/dashboard': '仪表盘',
    '/admin/users': '用户管理',
    '/admin/photographer-list': '摄影师管理',
    '/admin/orders': '订单管理',
    '/admin/refund-management': '退款管理',
    '/admin/applications': '申请审核',
    '/admin/announcements': '公告管理',
    '/admin/rag-management': '知识库管理'
  }
  return titles[route.path] || '管理后台'
})
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  background: #f5f7fa;
}

/* Sidebar */
.admin-sidebar {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.05);
}

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-icon {
  font-size: 24px;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  letter-spacing: 1px;
}

/* Menu */
.admin-menu {
  border-right: none;
}

.admin-menu .el-menu-item {
  height: 50px;
  line-height: 50px;
  margin: 4px 8px;
  border-radius: 6px;
  transition: all 0.3s;
}

.admin-menu .el-menu-item:hover {
  background-color: #f5f7fa;
}

.admin-menu .el-menu-item.is-active {
  background-color: #ecf5ff;
  color: #409eff;
}

/* Main Container */
.admin-main-container {
  background: #f5f7fa;
}

.admin-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.admin-content {
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}
</style> 