<script setup lang="ts">
import { useRoute } from 'vue-router'
import NavBar from './components/NavBar.vue'
import Sidebar from './components/Sidebar.vue'

const route = useRoute()
const isAdminPage = route.path.startsWith('/admin')
</script>

<template>
  <div id="app">
    <el-container>
      <el-header>
        <nav-bar />
      </el-header>
      <el-main>
        <router-view />
      </el-main>
      <el-footer>
        <div class="footer-content">
          <p>&copy; {{ new Date().getFullYear() }} 摄影约拍系统. All rights reserved.</p>
        </div>
      </el-footer>
    </el-container>
    <sidebar v-if="!isAdminPage" />
  </div>
</template>

<style scoped>
#app {
  min-height: 100vh;
}

.el-header {
  padding: 0;
  height: auto;
}

.el-main {
  padding: 20px;
  min-height: calc(100vh - 120px);
}

.el-footer {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
}

.footer-content {
  text-align: center;
  color: #666;
}
</style>
