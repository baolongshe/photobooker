<template>
  <div class="register-container">
    <div class="register-background">
      <div class="background-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
      </div>
    </div>
    
    <div class="register-content">
      <el-card class="register-card" shadow="hover">
        <div class="register-header">
          <div class="logo-container">
            <el-icon class="register-icon" :size="50"><Camera /></el-icon>
          </div>
          <h2>用户注册</h2>
          <p>Join Our Photography Community</p>
        </div>
        
        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          label-width="0"
          class="register-form"
          @keyup.enter="handleRegister"
        >
          <el-form-item prop="username">
            <el-input
              v-model="registerForm.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              size="large"
              clearable
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
              clearable
            />
          </el-form-item>
          
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请确认密码"
              prefix-icon="Lock"
              size="large"
              show-password
              clearable
            />
          </el-form-item>
          
          <el-form-item prop="realName">
            <el-input
              v-model="registerForm.realName"
              placeholder="请输入真实姓名"
              prefix-icon="UserFilled"
              size="large"
              clearable
            />
          </el-form-item>
          
          <el-form-item prop="phone">
            <el-input
              v-model="registerForm.phone"
              placeholder="请输入手机号码"
              prefix-icon="Phone"
              size="large"
              clearable
            />
          </el-form-item>
          
          <el-form-item prop="gender">
            <el-select
              v-model="registerForm.gender"
              placeholder="请选择性别"
              size="large"
              style="width: 100%"
            >
              <el-option label="女" :value="0" />
              <el-option label="男" :value="1" />
              <el-option label="其他" :value="2" />
            </el-select>
          </el-form-item>
          
          <el-form-item prop="birthday">
            <el-date-picker
              v-model="registerForm.birthday"
              type="date"
              placeholder="请选择生日"
              size="large"
              style="width: 100%"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
          
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="register-button"
              :loading="loading"
              @click="handleRegister"
            >
              <el-icon v-if="!loading"><Check /></el-icon>
              {{ loading ? '注册中...' : '立即注册' }}
            </el-button>
          </el-form-item>
        </el-form>
        
        <div class="register-footer">
          <p>已有账号？ <el-link type="primary" @click="goToLogin">立即登录</el-link></p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import api from '../utils/api'

const router = useRouter()
const registerFormRef = ref<FormInstance>()
const loading = ref(false)

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  gender: undefined as number | undefined,
  birthday: ''
})

// 自定义验证规则
const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const validatePhone = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请输入手机号码'))
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的手机号码'))
  } else {
    callback()
  }
}

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度在2到20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 10, message: '姓名长度在2到10个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, validator: validatePhone, trigger: 'blur' }
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  birthday: [
    { required: true, message: '请选择生日', trigger: 'change' }
  ]
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  try {
    await registerFormRef.value.validate()
    loading.value = true
    
    // 准备注册数据
    const registerData = {
      username: registerForm.username,
      password: registerForm.password,
      realName: registerForm.realName,
      phone: registerForm.phone,
      gender: registerForm.gender,
      birthday: registerForm.birthday ? new Date(registerForm.birthday) : null,
      status: 1, // 默认正常状态
      role: 0 // 默认普通用户角色
    }
    
    // 调用后端注册API
    const response = await api.post('/photo/user/sign', registerData)
    const result = response.data
    console.log('注册结果:', response)
    if ( result === true&&response.code === 1) {
      ElMessage.success('注册成功！请登录')
      // 注册成功后跳转到登录页面
      router.push({ name: 'login' })
    } else {
      ElMessage.error(result || '注册失败1')
    }
  } catch (error: any) {
    console.error('注册失败:', error)
    ElMessage.error(error.response?.data?.msg || '注册失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push({ name: 'login' })
}
</script>

<style scoped>
.register-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #ff6b9d 0%, #c44dff 40%, #6c5ce7 70%, #4facfe 100%);
  background-size: 300% 300%;
  animation: gradient-shift 10s ease infinite;
  overflow: hidden;
  padding: 20px 0;
}

.register-background {
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

.register-content {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 500px;
  padding: 20px;
  animation: pop-in 0.6s ease;
}

.register-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15), 0 0 0 1px rgba(255, 255, 255, 0.2) inset;
}

.register-header {
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

.register-icon {
  color: white;
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.2));
}

.register-header h2 {
  margin-bottom: 10px;
  font-size: 30px;
  font-weight: 800;
  background: linear-gradient(135deg, #ff6b9d, #c44dff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.register-header p {
  color: #999;
  font-size: 15px;
  margin: 0;
  font-weight: 500;
}

.register-form {
  margin-bottom: 30px;
}

.register-form .el-form-item {
  margin-bottom: 20px;
}

.register-form .el-input,
.register-form .el-select,
.register-form .el-date-picker {
  --el-input-border-radius: 16px;
  --el-input-hover-border-color: #c44dff;
  --el-input-focus-border-color: #ff6b9d;
}

.register-form .el-input :deep(.el-input__wrapper),
.register-form .el-select :deep(.el-select__wrapper),
.register-form .el-date-picker :deep(.el-input__wrapper) {
  box-shadow: 0 2px 8px rgba(196, 77, 255, 0.08);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.register-form .el-input :deep(.el-input__wrapper:hover),
.register-form .el-select :deep(.el-select__wrapper:hover),
.register-form .el-date-picker :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 16px rgba(196, 77, 255, 0.15);
}

.register-form .el-input :deep(.el-input__wrapper.is-focus),
.register-form .el-select :deep(.el-select__wrapper.is-focused),
.register-form .el-date-picker :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 4px 20px rgba(255, 107, 157, 0.25);
}

.register-button {
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

.register-button:hover {
  transform: translateY(-3px) scale(1.02);
  box-shadow: 0 12px 32px rgba(255, 107, 157, 0.5);
}

.register-button:active {
  transform: translateY(-1px) scale(1);
}

.register-footer {
  text-align: center;
  color: #666;
}

.register-footer p {
  margin: 8px 0;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .register-content {
    padding: 10px;
  }
  
  .register-card {
    padding: 30px 20px;
  }
  
  .register-header h2 {
    font-size: 26px;
  }
  
  .register-form .el-form-item {
    margin-bottom: 15px;
  }
}
</style> 