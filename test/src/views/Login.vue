<template>
  <div class="login-container">
    <div class="login-background">
      <div class="background-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
      </div>
    </div>
    
    <div class="login-content">
      <el-card class="login-card" shadow="hover">
        <div class="login-header">
          <div class="logo-container">
            <el-icon class="login-icon" :size="50"><Camera /></el-icon>
          </div>
          <h2>摄影约拍系统</h2>
          <p>Professional Photography Booking</p>
        </div>
        
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-width="0"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              size="large"
              clearable
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>
          
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-button"
              :loading="loading"
              @click="handleLogin"
            >
              <el-icon v-if="!loading"><Right /></el-icon>
              {{ loading ? '登录中...' : '立即登录' }}
            </el-button>
          </el-form-item>
        </el-form>
        
        <div class="login-footer">
          <p>还没有账号？ <el-link type="primary" @click="goToRegister">立即注册</el-link></p>
          <p class="demo-info">演示账号：xhxi / 123123</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import api from '../utils/api'

const router = useRouter()
const userStore = useUserStore()
const loginFormRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, message: '用户名长度不能少于2位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    await loginFormRef.value.validate()
    loading.value = true

    // 调用后端登录API
    const response: any = await api.post('/photo/user/login', {
      username: loginForm.username,
      password: loginForm.password
    })

    if (response.code === 1 && response.data) {
      const userData = response.data
      userStore.setUserInfo(userData)
      ElMessage.success('登录成功')
      if (userData.role === 1) {
        router.push('/admin')
        ElMessage.info('欢迎管理员登录')
      } else {
        router.push('/')
      }
    } else {
      ElMessage.error('登录失败')
    }
  } catch (error: any) {
    console.error('登录失败:', error)
    ElMessage.error(error.response?.data?.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

const goToRegister = () => {
  router.push('/register')
}

onMounted(() => {
  // 如果已经登录，直接跳转
  if (userStore.isLoggedIn) {
    if (userStore.user?.role === 1) {
      router.push('/admin')
    } else {
      router.push('/')
    }
  }
})
</script>

<style scoped>
@keyframes float {
  0%, 100% { transform: translateY(0px) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(5deg); }
}
@keyframes gradient-shift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}
@keyframes pop-in {
  0% { transform: scale(0.8); opacity: 0; }
  70% { transform: scale(1.05); }
  100% { transform: scale(1); opacity: 1; }
}

.login-container {
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(135deg, #ff6b9d 0%, #c44dff 40%, #6c5ce7 70%, #4facfe 100%);
  background-size: 300% 300%;
  animation: gradient-shift 10s ease infinite;
  display: flex;
  justify-content: center;
  align-items: center;
  position: absolute;
  left: 0;
  top: 0;
  overflow: hidden;
}

.login-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.background-shapes {
  position: relative;
  width: 100%;
  height: 100%;
}

.shape {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  animation: float 8s ease-in-out infinite;
}

.shape-1 {
  width: 120px;
  height: 120px;
  top: 15%;
  left: 8%;
  animation-delay: 0s;
}

.shape-2 {
  width: 180px;
  height: 180px;
  top: 60%;
  right: 8%;
  animation-delay: 2s;
}

.shape-3 {
  width: 100px;
  height: 100px;
  bottom: 15%;
  left: 15%;
  animation-delay: 4s;
}

.login-content {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 450px;
  padding: 20px;
  animation: pop-in 0.6s ease;
}

.login-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15), 0 0 0 1px rgba(255, 255, 255, 0.2) inset;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo-container {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 90px;
  height: 90px;
  background: linear-gradient(135deg, #ff6b9d, #c44dff);
  border-radius: 24px;
  margin-bottom: 20px;
  box-shadow: 0 12px 32px rgba(255, 107, 157, 0.4);
  animation: float 4s ease-in-out infinite;
}

.login-icon {
  color: white;
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.2));
}

.login-header h2 {
  margin-bottom: 10px;
  font-size: 30px;
  font-weight: 800;
  background: linear-gradient(135deg, #ff6b9d, #c44dff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-header p {
  color: #999;
  font-size: 15px;
  margin: 0;
  font-weight: 500;
}

.login-form {
  margin-bottom: 30px;
}

.login-form .el-form-item {
  margin-bottom: 25px;
}

.login-form .el-input {
  --el-input-border-radius: 16px;
  --el-input-hover-border-color: #c44dff;
  --el-input-focus-border-color: #ff6b9d;
}

.login-form .el-input :deep(.el-input__wrapper) {
  box-shadow: 0 2px 8px rgba(196, 77, 255, 0.08);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.login-form .el-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 16px rgba(196, 77, 255, 0.15);
}

.login-form .el-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 4px 20px rgba(255, 107, 157, 0.25);
}

.login-button {
  width: 100%;
  height: 52px;
  font-size: 17px;
  font-weight: 700;
  border-radius: 16px;
  background: linear-gradient(135deg, #ff6b9d 0%, #c44dff 100%);
  border: none;
  box-shadow: 0 8px 24px rgba(255, 107, 157, 0.4);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  letter-spacing: 1px;
}

.login-button:hover {
  transform: translateY(-3px) scale(1.02);
  box-shadow: 0 12px 32px rgba(255, 107, 157, 0.5);
}

.login-button:active {
  transform: translateY(-1px) scale(1);
}

.login-footer {
  text-align: center;
  color: #666;
}

.login-footer p {
  margin: 8px 0;
  font-size: 14px;
}

.demo-info {
  color: #999;
  font-size: 12px;
  margin-top: 15px;
  padding: 12px;
  background: linear-gradient(135deg, #fff5f7, #f0e6ff);
  border-radius: 12px;
  border: 1px solid rgba(196, 77, 255, 0.1);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-content {
    padding: 10px;
  }
  
  .login-card {
    padding: 30px 20px;
  }
  
  .login-header h2 {
    font-size: 26px;
  }
}
</style> 