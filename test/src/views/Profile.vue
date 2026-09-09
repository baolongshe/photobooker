<template>
  <div class="profile-page">
    <!-- Hero Section -->
    <section class="hero-section">
      <div class="hero-container">
        <div class="hero-content">
          <h1 class="hero-title">
            <span class="title-line">👤 个人中心</span>
            <span class="title-line highlight">管理您的个人信息</span>
          </h1>
          <p class="hero-subtitle">查看统计、编辑资料、管理账户</p>
        </div>
        <div class="hero-visual">
          <div class="floating-card card-1">✨</div>
          <div class="floating-card card-2">🎨</div>
          <div class="floating-card card-3">💫</div>
        </div>
      </div>
    </section>

    <div class="profile-container">
      <el-row :gutter="30">
        <el-col :span="8">
          <!-- 个人信息卡片 -->
          <el-card class="profile-card modern-card">
            <div class="profile-avatar">
              <img :src="userInfo.avatar || 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200&h=200&fit=crop&crop=face'" />
              <button class="avatar-btn" @click="uploadAvatar">📷 更换头像</button>
            </div>
            <div class="profile-info">
              <h3>{{ userInfo.username }}</h3>
              <p>{{ userInfo.realName }}</p>
              <el-tag type="success" class="status-tag">✓ 正常用户</el-tag>
            </div>
          </el-card>

          <!-- 统计信息 -->
          <el-card class="stats-card modern-card">
            <template #header>
              <h3>📊 我的统计</h3>
            </template>
            <div class="stats-grid">
              <div class="stat-item">
                <div class="stat-number">{{ stats.totalOrders }}</div>
                <div class="stat-label">总订单数</div>
              </div>
              <div class="stat-item">
                <div class="stat-number">{{ stats.completedOrders }}</div>
                <div class="stat-label">已完成</div>
              </div>
              <div class="stat-item">
                <div class="stat-number">{{ stats.pendingOrders }}</div>
                <div class="stat-label">待处理</div>
              </div>
            </div>
          </el-card>
        </el-col>

      <el-col :span="16">
        <!-- 个人信息编辑 -->
        <el-card class="edit-card">
          <template #header>
            <h3>个人信息</h3>
          </template>
          <el-form :model="editUserInfo" label-width="100px" :rules="formRules" ref="formRef">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="用户名" prop="username">
                  <el-input v-model="userInfo.username" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="真实姓名" prop="realName">
                  <el-input v-model="editUserInfo.realName" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="手机号码" prop="phone">
                  <el-input v-model="editUserInfo.phone" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="性别" prop="gender">
                  <el-select v-model="editUserInfo.gender" style="width: 100%;">
                    <el-option label="女" :value="0" />
                    <el-option label="男" :value="1" />
                    <el-option label="其他" :value="2" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="生日" prop="birthday">
                  <el-date-picker
                    v-model="editUserInfo.birthday"
                    type="date"
                    placeholder="选择生日"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    style="width: 100%;"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item>
              <el-button type="primary" @click="saveProfile" :loading="saving">
                保存修改
              </el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 修改密码 -->
        <el-card class="password-card">
          <template #header>
            <h3>修改密码</h3>
          </template>
          <el-form :model="passwordForm" label-width="100px" :rules="passwordRules" ref="passwordFormRef">
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="changePassword" :loading="changingPassword">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 账户安全 -->
        <el-card class="security-card">
          <template #header>
            <h3>账户安全</h3>
          </template>
          <div class="security-items">
            <div class="security-item">
              <div class="security-info">
                <h4>手机绑定</h4>
                <p>已绑定手机：{{ userInfo.phone || '未绑定' }}</p>
              </div>
              <el-button type="primary" size="small" @click="editPhone">修改</el-button>
            </div>
          </div>
          <div class="address-preview" v-if="addressList.length">
            <h4 style="margin: 16px 0 8px 0;">常用地址</h4>
            <div v-for="(addr, idx) in addressList.slice(0,2)" :key="addr.id" class="address-item" style="margin-bottom: 8px;">
              <el-tag v-if="addr.isDefault === 1" type="success" size="small" style="margin-right: 6px;">默认</el-tag>
              {{ addr.receiver }}，{{ addr.phone }}，{{ addr.province + addr.city + addr.district + addr.detail }}
            </div>
            <el-button type="text" @click="showAddressDialog = true">地址管理</el-button>
          </div>
        </el-card>

        <el-button
          v-if="isPhotographer"
          type="primary"
          @click="$router.push('/photographer/orders')"
          style="margin-bottom: 20px"
        >
          管理用户下单关联我的订单
        </el-button>

        <el-button
          v-if="isPhotographer"
          type="primary"
          @click="$router.push('/photographer/packages')"
          style="margin-bottom: 20px"
        >
          套餐管理
        </el-button>

        <div style="display: flex; gap: 12px; margin-bottom: 20px;">
          <el-button v-if="!isPhotographer" type="primary" @click="showApplicationDialog = true">查看申请进度</el-button>
          <el-button type="primary" @click="showAddressDialog = true">地址管理</el-button>
          <el-button type="primary" @click="locateMe">定位</el-button>
        </div>


        <!--  收藏的作品      -->
        <el-card class="favorite-card">
          <template #header>
            <h3>收藏的作品</h3>
          </template>
          <div v-if="favoriteLoading" class="loading-container">
            <el-skeleton :rows="3" animated />
          </div>
          <div v-else-if="favoritePortfolios.length > 0" class="favorite-grid">
            <el-card
                v-for="portfolio in favoritePortfolios"
                :key="portfolio.id"
                class="favorite-item"
                @click="goToPortfolioDetail(portfolio.id)"
            >
              <div class="portfolio-image">
                <img :src="portfolio.coverImage" :alt="portfolio.title" />
                <div class="portfolio-overlay">
                  <el-button type="primary" size="small">查看详情</el-button>
                </div>
              </div>
              <div class="portfolio-info">
                <h4 class="portfolio-title">{{ portfolio.title }}</h4>
                <p class="portfolio-description">{{ portfolio.category }} · 收藏于 {{ formatDateTime(portfolio.createTime) }}</p>
              </div>
            </el-card>
          </div>
          <el-empty v-else description="暂无收藏作品" />
        </el-card>

        <!-- 作品集卡片 -->
        <el-card class="portfolio-card" v-if="isPhotographer" style="margin-top: 24px;">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <h3>我的作品集</h3>
              <el-button type="primary" size="small" @click="$router.push('/photographer/portfolio')">
                管理作品
              </el-button>
            </div>
          </template>

          <div v-if="portfolioLoading" class="loading-container">
            <el-skeleton :rows="3" animated />
          </div>

          <div v-else-if="myPortfolios.length > 0" class="portfolio-grid">
            <div
                v-for="portfolio in myPortfolios.slice(0, 4)"
                :key="portfolio.id"
                class="portfolio-item"
                @click="goToPortfolioDetail(portfolio.id)"
            >
              <div class="portfolio-image-wrapper">
                <img :src="portfolio.coverImage" :alt="portfolio.title" />
                <div class="portfolio-overlay">
                  <el-icon class="view-icon"><View /></el-icon>
                </div>
              </div>
              <div class="portfolio-info">
                <h4 class="portfolio-title">{{ portfolio.title }}</h4>
                <p class="portfolio-category">{{ portfolio.category }}</p>
              </div>
            </div>
          </div>

          <div v-else class="empty-portfolio">
            <el-empty description="暂无作品">
              <el-button type="primary" @click="$router.push('/photographer/portfolio')">
                去上传作品
              </el-button>
            </el-empty>
          </div>
        </el-card>

      </el-col>
    </el-row>
    </div>

    <el-dialog v-model="phoneDialogVisible" title="修改手机号" width="400px">
      <el-form>
        <el-form-item label="新手机号">
          <el-input v-model="newPhone" />
        </el-form-item>
        <el-form-item label="验证码">
          <el-input v-model="phoneCode" style="width: 180px; margin-right: 10px;" />
          <el-button
            :disabled="codeBtnDisabled"
            @click="sendPhoneCode"
            size="small"
            type="primary"
          >
            {{ codeBtnText }}
          </el-button>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="phoneDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePhoneUpdate">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showApplicationDialog" title="我的摄影师申请进度" width="600px">
      <el-table :data="myApplications" style="width: 100%">
        <el-table-column prop="createTime" label="申请时间" />
        <el-table-column prop="status" label="状态" />
        <el-table-column prop="remark" label="审核备注" />
      </el-table>
      <template #footer>
        <el-button @click="showApplicationDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showAddressDialog" title="我的地址管理" width="600px">
      <el-table :data="addressList" style="width: 100%">
        <el-table-column prop="receiver" label="收件人" />
        <el-table-column prop="phone" label="电话" />
        <el-table-column label="详细地址">
          <template #default="{ row }">
            {{ row.province + row.city + row.district + row.detail }}
          </template>
        </el-table-column>
        <el-table-column label="默认">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault === 1" type="success">默认</el-tag>
            <el-button v-else size="small" @click="setDefaultAddress(row.id)">设为默认</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="deleteAddress(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-form :model="newAddress" label-width="70px" style="margin-top: 20px">
        <el-form-item label="收件人"><el-input v-model="newAddress.receiver" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="newAddress.phone" /></el-form-item>
        <el-form-item label="省"><el-input v-model="newAddress.province" /></el-form-item>
        <el-form-item label="市"><el-input v-model="newAddress.city" /></el-form-item>
        <el-form-item label="区"><el-input v-model="newAddress.district" /></el-form-item>
        <el-form-item label="详细"><el-input v-model="newAddress.detail" /></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="addAddress">新增地址</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddressDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="avatarDialogVisible" title="头像管理" width="400px">
      <el-upload
        class="avatar-uploader"
        :show-file-list="false"
        :http-request="handleAvatarUpload"
        accept="image/*"
      >
        <el-button type="primary">上传新头像</el-button>
      </el-upload>
      <div class="avatar-list">
        <div
          v-for="a in avatarList"
          :key="a.id"
          class="avatar-item"
          :class="{ current: a.isCurrent === 1 }"
        >
          <img :src="a.url" class="avatar-img" />
          <el-button v-if="a.isCurrent !== 1" size="small" @click="setCurrentAvatar(a.id)">设为当前</el-button>
          <el-button v-if="a.isCurrent !== 1" size="small" type="danger" @click="deleteAvatar(a.id)">删除</el-button>
          <el-tag v-if="a.isCurrent === 1" type="success">当前头像</el-tag>
        </div>
      </div>
    </el-dialog>

    <div v-if="previewLatLng && previewLatLng.lat !== undefined && previewLatLng.lng !== undefined" style="margin: 16px 0;">
      <div id="previewMap" style="width: 100%; height: 300px;"></div>
      <div style="margin-top: 8px;">
        <span>定位坐标：{{ previewLatLng.lat }}, {{ previewLatLng.lng }}</span>
        <button @click="uploadLocation" style="margin-left: 16px; padding: 6px 18px; background: #67c23a; color: #fff; border: none; border-radius: 4px; cursor: pointer;">确认上传</button>
        <button @click="cancelPreview" style="margin-left: 8px; padding: 6px 18px; background: #f56c6c; color: #fff; border: none; border-radius: 4px; cursor: pointer;">取消</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, watchEffect, watch } from 'vue'
import { useRouter } from 'vue-router' 
import { useUserStore } from '../stores/user'
import { ElMessage, ElDialog } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import api from '../utils/api'
import dayjs from 'dayjs'
import axios from 'axios'
import { nextTick } from 'vue'
import { View, Star } from '@element-plus/icons-vue'


const router = useRouter() 
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const saving = ref(false)
const changingPassword = ref(false)
const phoneDialogVisible = ref(false)
const newPhone = ref('')
const phoneCode = ref('')
const codeBtnText = ref('获取验证码')
const codeBtnDisabled = ref(false)
const codeTimer = ref<number | null>(null)
const isPhotographer = ref(false)
const showApplicationDialog = ref(false)
const myApplications = ref<any[]>([])
const showAddressDialog = ref(false)
const addressList = ref<any[]>([])
const newAddress = reactive({ receiver: '', phone: '', province: '', city: '', district: '', detail: '' })
const previewLatLng = ref<{ lat: number; lng: number } | null>(null)
let previewMap = null
const avatarDialogVisible = ref(false)
const avatarList = ref<any[]>([])
const showUploadDialog = ref(false)
const newWork = ref({
  title: '',
  description: '',
  category: '',
  tags: '',
  coverImage: '',
  imageUrls: [],
  shootingDate: '',
  shootingLocation: '',
  equipment: ''
})
const imageFileList = ref([])
const myPhotographerId = ref(null)

// 用户信息
const userInfo = reactive({
  id: '',
  username: '',
  realName: '',
  phone: '',
  gender: '',
  birthday: '',
  avatar: '',
  // ...其它字段
})

// 统计信息
const stats = reactive({
  totalOrders: 0,
  completedOrders: 0,
  pendingOrders: 0
})

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 表单验证规则
const formRules: FormRules = {
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ]
}

// 密码验证规则
const passwordRules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 同步用户信息到编辑表单
const syncUserInfoToEdit = () => {
  Object.assign(editUserInfo, {
    realName: userInfo.realName || '',
    phone: userInfo.phone || '',
    gender:
      userInfo.gender !== undefined && userInfo.gender !== null && userInfo.gender !== ''
        ? Number(userInfo.gender)
        : '',
    birthday: userInfo.birthday || ''
  })
}

// 重置表单
const resetForm = () => {
  syncUserInfoToEdit()
  formRef.value?.clearValidate()
}

// 修改密码
const changePassword = async () => {
  try {
    await passwordFormRef.value?.validate()
    // 这里添加修改密码的逻辑
    ElMessage.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (error) {
    ElMessage.error('密码修改失败')
  }
}

// 获取用户详细信息
const fetchUserInfo = async () => {
  if (!userStore.user?.id) return
  try {
    const res = await api.get(`/photo/user/${userStore.user.id}`)
    if ( res.data && typeof res.data === 'object' && !Array.isArray(res.data)) {
      Object.assign(userInfo, res.data)
      // 同步到编辑表单
      syncUserInfoToEdit()
    } else {
      ElMessage.error(res.statusText || '服务器异常')
    }
  } catch (error) {
    ElMessage.error('获取用户信息失败')
  }
}

const editPhone = () => {
  phoneDialogVisible.value = true
}

// 发送验证码
const sendPhoneCode = async () => {
  const phone = newPhone.value.trim()
  if (!/^1[3-9]\d{9}$/.test(phone)) {
    ElMessage.error('请输入正确的手机号')
    return
  }
  try {
    const res = await api.post('/photo/user/sendPhoneCode', { phone })
    if (res.data) {
      ElMessage.success('验证码：' + res.data)
    } else {
      ElMessage.success('验证码已发送')
    }
    // 按钮倒计时
    codeBtnDisabled.value = true
    let seconds = 60
    codeBtnText.value = `${seconds}s后重试`
    codeTimer.value = setInterval(() => {
      seconds--
      codeBtnText.value = `${seconds}s后重试`
      if (seconds <= 0) {
        clearInterval(codeTimer.value!)
        codeBtnText.value = '获取验证码'
        codeBtnDisabled.value = false
      }
    }, 1000)
  } catch (e) {
    ElMessage.error('验证码发送失败')
  }
}

const handlePhoneUpdate = async () => {
  if (!/^1[3-9]\d{9}$/.test(newPhone.value)) {
    ElMessage.error('请输入正确的手机号')
    return
  }
  if (!phoneCode.value) {
    ElMessage.error('请输入验证码')
    return
  }
  try {
    // 调用后端接口校验验证码并修改手机号
    await api.post('/photo/user/updatePhone', {
      userId: userInfo.id,
      phone: newPhone.value,
      code: phoneCode.value
    })
    ElMessage.success('手机号修改成功')

    userInfo.phone = newPhone.value
    editUserInfo.phone = newPhone.value

    phoneDialogVisible.value = false
  } catch (e) {
    ElMessage.error('手机号修改失败，请检查验证码')
  }
}

const editUserInfo = reactive({
  realName: '',
  phone: '',
  gender: '',
  birthday: ''
})

// 获取摄影师列表
const checkPhotographer = async () => {
  if (!userStore.user?.id) return
  try {
    const res = await api.get('/photo/photographer/list')
    const photographer = (res.data || []).find(
      (p: any) => p.userId === userStore.user?.id
    )
    isPhotographer.value = !!photographer
  } catch (error) {
    console.error('检查摄影师状态失败:', error)
  }
}

// 获取申请列表
const fetchMyApplications = async () => {
  if (!userStore.user?.id) return
  const res = await api.get(`/application/user/${userStore.user.id}`)
  myApplications.value = res.data || []
}

watch(showApplicationDialog, (val) => {
  if (val) fetchMyApplications()
})

// 获取地址列表
const fetchAddressList = async () => {
  if (!userStore.user?.id) return
  const res = await api.get(`/address/${userStore.user.id}`)
  addressList.value = res.data || []
}

const addAddress = async () => {
  if (!userStore.user?.id) return
  await api.post('/address', { ...newAddress, userId: userStore.user.id })
  ElMessage.success('地址添加成功')
  Object.assign(newAddress, { receiver: '', phone: '', province: '', city: '', district: '', detail: '' })
  fetchAddressList()
}

const deleteAddress = async (id: number) => {
  await api.delete(`/address/${id}`)
  ElMessage.success('地址已删除')
  fetchAddressList()
}

const setDefaultAddress = async (id: number) => {
  if (!userStore.user?.id) return
  await api.post('/address/default', null, { params: { userId: userStore.user.id, addressId: id } })
  ElMessage.success('已设为默认地址')
  fetchAddressList()
}

watch(showAddressDialog, (val) => { if (val) fetchAddressList() })

const locateMe = () => {
  if (!navigator.geolocation) {
    alert('当前浏览器不支持地理定位')
    return
  }
  navigator.geolocation.getCurrentPosition(
    async (pos) => {
      console.log('定位成功', pos)
      const { latitude, longitude } = pos.coords
      previewLatLng.value = { lat: latitude, lng: longitude }
      await nextTick()
      // 这里需要引入腾讯地图SDK
      // eslint-disable-next-line no-undef
      if (typeof TMap !== 'undefined') {
        previewMap = new TMap.Map('previewMap', {
          center: new TMap.LatLng(latitude, longitude),
          zoom: 16
        })
        // eslint-disable-next-line no-undef
        new TMap.MultiMarker({
          map: previewMap,
          geometries: [{
            id: 'me',
            position: new TMap.LatLng(latitude, longitude)
          }]
        })
      }
    },
    (err) => {
      console.log('定位失败', err)
      alert('定位失败，请检查权限设置')
    }
  )
}

const uploadLocation = async () => {
  if (!previewLatLng.value) return
  try {
    await api.post('/photo/photographer/update-location', null, {
      params: {
        id: userStore.user?.id,
        latitude: previewLatLng.value.lat,
        longitude: previewLatLng.value.lng
      }
    })
    ElMessage.success('位置已更新！')
    previewLatLng.value = null
  } catch (error) {
    ElMessage.error('位置更新失败')
  }
}

const cancelPreview = () => {
  previewLatLng.value = null
}

// 获取头像列表
const fetchAvatars = async () => {
  const res = await api.get('/avatar/list', { params: { userId: userInfo.id } })
  avatarList.value = res.data || []
  // 自动刷新当前头像
  const current = avatarList.value.find((a: any) => a.isCurrent === 1)
  if (current) userInfo.avatar = current.url
}

// 模拟图片上传（本地Base64），实际可替换为后端文件上传接口
const handleAvatarUpload = async (option: any) => {
  const file = option.file
  const formData = new FormData()
  formData.append('file', file)
  // 1. 上传到腾讯云COS
  const res = await api.post('/file/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  const url = res.data
  // 2. 存入头像表
  await api.post('/avatar/upload', { userId: userInfo.id, url })
  ElMessage.success('上传成功')
  fetchAvatars()
  await fetchUserInfo() // 上传头像后同步用户信息
}

const setCurrentAvatar = async (id: number) => {
  await api.put(`/avatar/${id}/setCurrent`, null, { params: { userId: userInfo.id } })
  ElMessage.success('已设为当前头像')
  fetchAvatars()
  await fetchUserInfo() // 设为当前头像后同步用户信息
}

const deleteAvatar = async (id: number) => {
  await api.delete(`/avatar/${id}`)
  ElMessage.success('已删除')
  fetchAvatars()
  await fetchUserInfo() // 删除头像后同步用户信息
}

const uploadAvatar = () => {
  avatarDialogVisible.value = true
  fetchAvatars()
}

const handleCoverUploadSuccess = (response: any) => {
  newWork.value.coverImage = response.data
}
const handleImageUploadSuccess = (response: any, file: any, fileListArr: any) => {
  if (response.data) {
    newWork.value.imageUrls.push(response.data)
  }
  imageFileList.value = fileListArr
}

const fetchPhotographerId = async () => {
  const res = await api.get('/photo/photographer/list')
  if (!userInfo.id) return
  const photographer = (res.data || []).find(
    (p: any) => p.userId == userInfo.id
  )
  if (photographer) myPhotographerId.value = photographer.id
}

const submitWork = async () => {
  if (!userInfo.id) {
    ElMessage.error('用户信息未加载，请稍后重试')
    return
  }
  if (!myPhotographerId.value) {
    ElMessage.error('当前账号不是摄影师，无法上传作品')
    return
  }
  if (!newWork.value.title || !newWork.value.coverImage) {
    ElMessage.warning('请填写标题并上传封面')
    return
  }
  // 组装作品数据
  const payload = {
    ...newWork.value,
    photographerId: myPhotographerId.value, // photographerId用photographer表主键
    imageUrls: JSON.stringify(newWork.value.imageUrls),
    shootingDate: newWork.value.shootingDate || null
  }
  await api.post('/portfolio', payload)
  ElMessage.success('作品上传成功')
  showUploadDialog.value = false
  // 清空表单
  newWork.value = { title: '', description: '', category: '', tags: '', coverImage: '', imageUrls: [], shootingDate: '', shootingLocation: '', equipment: '' }
  imageFileList.value = []
}

//保存修改
const saveProfile = async () => {
  saving.value = true
  try {
    // 校验表单
    await formRef.value?.validate()
    // 提交数据到后端
    await api.post('/photo/user/update', [
      {
        id: userInfo.id,
        realName: editUserInfo.realName,
        phone: editUserInfo.phone,
        gender: editUserInfo.gender,
        birthday: editUserInfo.birthday
      }
    ])
    ElMessage.success('保存成功')
    await fetchUserInfo() // 重新拉取用户信息，确保数据同步
  } catch (e) {
    ElMessage.error('保存失败，请检查输入')
  } finally {
    saving.value = false
  }
}

const fetchOrderStats = async () => {
  if (!userStore.user?.id) return
  try {
    console.log('开始获取订单统计，用户ID:', userStore.user.id)
    const res = await api.get('/orders/user')
    console.log('订单数据响应:', res)

    // 后端返回的是 PageResult 格式，需要提取 data.records
    let orders = []
    if (res.data && typeof res.data === 'object') {
      // PageResult 格式：{ code: 1, msg: 'success', data: { total: 10, list: [...] } }
      if (res.data) {
        const pageData = res.data
        orders = pageData.list || pageData.records || []
        console.log('分页数据中的订单列表:', orders, '总数:', pageData.total)
      } else if (Array.isArray(res.data)) {
        // 直接是数组格式
        orders = res.data
        console.log('直接数组格式:', orders)
      }
    }

    stats.totalOrders = orders.length
    stats.completedOrders = orders.filter((o: any) => o.status === 'COMPLETED').length
    stats.pendingOrders = orders.filter((o: any) =>
      ['PENDING', 'CONFIRMED', 'IN_PROGRESS', 'PENDING_CONFIRM'].includes(o.status)
    ).length


  } catch (e) {
    console.error('获取订单统计失败:', e)
    ElMessage.error('获取订单统计失败')
  }
}

// 收藏作品集相关
const favoritePortfolios = ref<any[]>([])
const favoriteLoading = ref(false)

// 获取用户收藏的作品集（Portfolio）
const fetchFavoritePhotos = async () => {
  if (!userStore.user?.id) return

  try {
    favoriteLoading.value = true

    // 获取收藏的作品集（Portfolio）
    const res = await api.get(`/photo/portfolio-like/user/${userStore.user.id}`)
    console.log('收藏的作品集数据:', res.data)
    favoritePortfolios.value = res.data || []

    // 如果有数据，打印第一条记录
    if (res.data && res.data.length > 0) {
      console.log('第一条收藏记录:', res.data[0])
    }
  } catch (error) {
    console.error('获取收藏作品集失败:', error)
  } finally {
    favoriteLoading.value = false
  }
}

// 格式化日期时间
const formatDateTime = (dateTime?: string) => {
  if (!dateTime) return ''
  return dayjs(dateTime).format('YYYY-MM-DD HH:mm')
}

// 跳转到作品集详情页
const goToPortfolioDetail = (portfolioId: number) => {
  router.push(`/portfolio/${portfolioId}`)
}

// 我的作品集相关
const myPortfolios = ref<Portfolio[]>([])
const portfolioLoading = ref(false)

// 获取摄影师自己的作品
const fetchMyPortfolios = async () => {
  if (!myPhotographerId.value) return

  portfolioLoading.value = true
  try {
    console.log("myPhotographerId:"+myPhotographerId.value)
    // 获取精选作品，限制4个用于展示
    const res = await api.get(`/portfolio/photographer/${myPhotographerId.value}/featured`, {
      params: { limit: 4 }
    })

    if (Array.isArray(res.data)) {
      myPortfolios.value = res.data
    } else {
      myPortfolios.value = []
    }
  } catch (error) {
    console.error('获取我的作品集失败:', error)
    myPortfolios.value = []
    // 不显示错误消息，因为这是展示用途
  } finally {
    portfolioLoading.value = false
  }
}




onMounted(() => {
  if (userStore.user && userStore.user.id) {
    fetchUserInfo()
    fetchFavoritePhotos()
  }
  fetchPhotographerId().then(() => {
    if(myPhotographerId.value){
      fetchMyPortfolios()
    }
  })
  fetchOrderStats()
  checkPhotographer()
  fetchAddressList()
})

onUnmounted(() => {
  if (codeTimer.value) clearInterval(codeTimer.value)
})

watchEffect(() => {
  console.log('userInfo.phone:', userInfo.phone)
})

interface Portfolio {
  id: number;
  photographerId: number;
  title: string;
  description: string;
  category: string;
  tags: string;
  coverImage: string;
  imageUrls: string;
  shootingDate: string;
  shootingLocation: string;
  equipment: string;
  status: number;
  viewCount: number;
  likeCount: number;
  isFeatured: number;
  sortWeight: number;
  createTime: string;
  updateTime: string;
}

</script>

<style scoped>
.profile-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h1 {
  margin: 0 0 10px 0;
  color: #333;
}

.page-header p {
  color: #666;
  margin: 0;
}

.profile-card,
.stats-card,
.edit-card,
.password-card,
.security-card,
.favorite-card,
.portfolio-card {
  margin-bottom: 24px;
}

.profile-avatar {
  text-align: center;
  padding: 20px;
}

.profile-avatar img {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  margin-bottom: 15px;
}

.profile-info {
  text-align: center;
}

.profile-info h3 {
  margin: 10px 0 5px;
  font-size: 20px;
  color: #333;
}

.profile-info p {
  color: #666;
  margin-bottom: 10px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  text-align: center;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 15px 10px;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
  line-height: 1;
}

.stat-label {
  color: #666;
  font-size: 14px;
  margin-top: 4px;
  white-space: nowrap;
}

.loading-container {
  padding: 20px;
}

.favorite-grid,
.portfolio-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

.favorite-item,
.portfolio-item {
  cursor: pointer;
  transition: all 0.3s;
}

.favorite-item:hover,
.portfolio-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.portfolio-image,
.portfolio-image-wrapper {
  position: relative;
  overflow: hidden;
  border-radius: 4px;
  aspect-ratio: 100/75;
}

.portfolio-image img,
.portfolio-image-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.portfolio-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.favorite-item:hover .portfolio-overlay,
.portfolio-item:hover .portfolio-overlay {
  opacity: 1;
}

.view-icon {
  font-size: 24px;
  color: white;
}

.portfolio-info {
  padding: 12px;
}

.portfolio-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.portfolio-description,
.portfolio-category {
  margin: 0;
  font-size: 14px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-portfolio {
  padding: 40px 20px;
  text-align: center;
}

.security-items {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
}

.security-info h4 {
  margin: 0 0 5px 0;
  color: #333;
}

.security-info p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.address-preview {
  margin-top: 16px;
}

.address-item {
  font-size: 14px;
  color: #666;
}

.avatar-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 15px;
  margin-top: 20px;
}

.avatar-item {
  position: relative;
  text-align: center;
}

.avatar-img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 50%;
  border: 3px solid #eee;
}

.avatar-item.current .avatar-img {
  border-color: #67c23a;
}

.avatar-item .el-button {
  margin-top: 8px;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .favorite-grid,
  .portfolio-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  }
}

/* Dopamine Style - Hero Section */
@keyframes float {
  0%, 100% { transform: translateY(0px) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(5deg); }
}

@keyframes pop-in {
  0% { opacity: 0; transform: scale(0.8); }
  100% { opacity: 1; transform: scale(1); }
}

.profile-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

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

.profile-container {
  animation: pop-in 0.6s ease both;
}

.modern-card {
  border-radius: 20px;
  border: 2px solid transparent;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 4px 20px rgba(196, 77, 255, 0.08);
}

.modern-card:hover {
  transform: translateY(-4px);
  border-color: #c44dff;
  box-shadow: 0 12px 40px rgba(196, 77, 255, 0.15);
}

.avatar-btn {
  margin-top: 12px;
  padding: 8px 16px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #ff6b9d, #c44dff);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.avatar-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(196, 77, 255, 0.4);
}

.status-tag {
  border-radius: 12px;
  font-weight: 600;
  margin-top: 8px;
}

@media (max-width: 1024px) {
  .hero-container {
    grid-template-columns: 1fr;
  }
  
  .hero-visual {
    display: none;
  }
}

@media (max-width: 768px) {
  .hero-section {
    padding: 40px 20px;
  }
  
  .hero-title {
    font-size: 2rem;
  }
}
</style>
