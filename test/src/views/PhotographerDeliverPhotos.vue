<template>
  <div class="deliver-photos">
    <div class="page-header">
      <el-button @click="goBack" icon="ArrowLeft">返回订单列表</el-button>
      <h1>交付照片</h1>
    </div>

    <el-card class="deliver-card">
      <!-- 订单信息 -->
      <div class="order-info">
        <h3>订单信息</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ order?.id }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ order?.userName }}</el-descriptions-item>
          <el-descriptions-item label="套餐">{{ order?.packageName }}</el-descriptions-item>
          <el-descriptions-item label="拍摄时间">{{ formatDateTime(order?.shootingTime) }}</el-descriptions-item>
          <el-descriptions-item label="拍摄地点">{{ order?.shootingLocation }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getStatusType(order?.status)">
              {{ getStatusText(order?.status) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 交付批次信息 -->
      <div class="delivery-info" v-if="currentDelivery">
        <h3>当前交付批次</h3>
        <el-alert
            :title="`批次号：${currentDelivery.deliveryBatch}`"
            type="info"
            :closable="false"
            style="margin-bottom: 15px"
        />
        <el-row :gutter="20">
          <el-col :span="8">
            <el-statistic title="总照片数" :value="currentDelivery.totalCount" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="已交付" :value="currentDelivery.deliveredCount" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="交付状态">
              <el-tag :type="getDeliveryStatusType(currentDelivery.status)">
                {{ getDeliveryStatusText(currentDelivery.status) }}
              </el-tag>
            </el-statistic>
          </el-col>
        </el-row>
      </div>

      <!-- 创建新交付批次 -->
      <div class="create-batch" v-else>
        <h3>创建交付批次</h3>
        <el-form :model="batchForm" label-width="120px">
          <el-form-item label="预计照片总数">
            <el-input-number v-model="batchForm.totalCount" :min="1" :max="1000" />
          </el-form-item>
          <el-form-item label="备注说明">
            <el-input
                v-model="batchForm.remark"
                type="textarea"
                :rows="3"
                placeholder="请输入备注说明（可选）"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="createBatch" :loading="batchLoading">
              创建批次并上传照片
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 照片上传区域 -->
      <div class="upload-section" v-if="currentDelivery">
        <h3>上传照片</h3>

        <el-upload
          ref="uploadRef"
          drag
          :file-list="fileList"
          :limit="100"
          multiple
          accept="image/*"
          :auto-upload="false"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            拖拽文件到此处或<em>点击选择</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持 jpg/png 格式，单次最多上传 100 张
            </div>
          </template>
        </el-upload>

        <el-button
          type="primary"
          @click="handleBatchUpload"
          :loading="uploading"
          style="margin-top: 20px"
        >
          开始上传
        </el-button>

        <!-- 上传进度 -->
        <div class="upload-progress" v-if="uploading">
          <el-progress
              :percentage="uploadProgress"
              :status="uploadSuccess ? 'success' : undefined"
          />
        </div>

        <!-- 已上传照片列表 -->
        <div class="uploaded-photos" v-if="uploadedPhotos.length > 0">
          <div class="photos-header">
            <h4>已上传照片（{{ uploadedPhotos.length }}张）</h4>
            <el-button
              type="danger"
              size="small"
              @click="clearAllPhotos"
              :disabled="uploadedPhotos.length === 0"
            >
              清空全部
            </el-button>
          </div>
          <div class="photo-grid">
            <el-card
                v-for="(photo, index) in uploadedPhotos"
                :key="index"
                class="photo-item"
            >
              <img :src="photo.url" class="photo-thumb" />
              <div class="photo-info">
                <div class="photo-name">{{ photo.name }}</div>
                <div class="photo-size">{{ formatFileSize(photo.size) }}</div>
              </div>
              <div class="photo-actions">
                <el-button
                  type="danger"
                  size="small"
                  circle
                  @click="removePhoto(index)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </el-card>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button
              type="primary"
              @click="submitDelivery"
              :loading="submitLoading"
              :disabled="uploadedPhotos.length === 0"
          >
            确认交付
          </el-button>
          <el-button @click="continueUpload">继续上传</el-button>
        </div>
      </div>

      <!-- 历史交付记录 -->
      <div class="delivery-history" v-if="deliveryHistory.length > 0">
        <h3>历史交付记录</h3>
        <el-table :data="deliveryHistory" style="width: 100%">
          <el-table-column prop="deliveryBatch" label="批次号" width="180" />
          <el-table-column prop="totalCount" label="总照片数" width="100" />
          <el-table-column prop="deliveredCount" label="已交付" width="100" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getDeliveryStatusType(row.status)">
                {{ getDeliveryStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="deliveryTime" label="交付时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.deliveryTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button
                  type="primary"
                  link
                  @click="viewDeliveryDetail(row)"
              >
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 交付详情对话框 -->
      <el-dialog
          v-model="detailDialogVisible"
          title="交付详情"
          width="900px"
          :close-on-click-modal="false"
      >
        <el-descriptions :column="2" border v-if="currentDetailDelivery">
          <el-descriptions-item label="批次号">{{ currentDetailDelivery.deliveryBatch }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getDeliveryStatusType(currentDetailDelivery.status)">
              {{ getDeliveryStatusText(currentDetailDelivery.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总照片数">{{ currentDetailDelivery.totalCount }}</el-descriptions-item>
          <el-descriptions-item label="已交付数">{{ currentDetailDelivery.deliveredCount }}</el-descriptions-item>
          <el-descriptions-item label="交付时间">{{ formatDateTime(currentDetailDelivery.deliveryTime) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(currentDetailDelivery.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentDetailDelivery.remark || '无' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider>交付照片列表（{{ deliveryPhotos.length }}张）</el-divider>

        <div v-if="photosLoading" style="text-align: center; padding: 40px;">
          <el-skeleton :rows="5" animated />
        </div>

        <div v-else-if="deliveryPhotos.length > 0" class="photo-grid">
          <el-card
              v-for="photo in deliveryPhotos"
              :key="photo.id"
              class="photo-item"
          >
            <img :src="photo.thumbnailUrl || photo.photoUrl" class="photo-thumb" />
            <div class="photo-info">
              <div class="photo-name">{{ photo.photoNumber }}</div>
              <div class="photo-size">{{ formatFileSize(photo.fileSize) }}</div>
              <div class="photo-status">
                <el-tag :type="photo.isDelivered === 1 ? 'success' : 'info'" size="small">
                  {{ photo.isDelivered === 1 ? '已交付' : '未交付' }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </div>

        <el-empty v-else description="该批次暂无照片" />

        <template #footer>
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import api from '../utils/api'
import { UploadFilled, Delete } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

interface Order {
  id: number
  userName: string
  packageName: string
  shootingTime: string
  shootingLocation: string
  status: string
}

interface Delivery {
  id: number
  orderId: number
  photographerId: number
  deliveryBatch: string
  totalCount: number
  deliveredCount: number
  status: string
  deliveryTime: string
  remark: string
  createTime: string
}

interface OrderPhoto {
  id: number
  orderId: number
  deliveryId: number
  photoNumber: string
  photoUrl: string
  thumbnailUrl: string
  isSelected: number
  isDelivered: number
  fileSize: number
  width: number
  height: number
  description: string
  tags: string[]
  sortOrder: number
  createTime: string
  updateTime: string
}

const order = ref<Order | null>(null)
const currentDelivery = ref<Delivery | null>(null)
const deliveryHistory = ref<Delivery[]>([])
const batchForm = reactive({
  totalCount: 20,
  remark: ''
})
const batchLoading = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadSuccess = ref(false)
const submitLoading = ref(false)
const fileList = ref<any[]>([])
const uploadedPhotos = ref<any[]>([])
const uploadRef = ref()

// 新增：详情对话框相关
const detailDialogVisible = ref(false)
const currentDetailDelivery = ref<Delivery | null>(null)
const deliveryPhotos = ref<OrderPhoto[]>([])
const photosLoading = ref(false)

// 自定义上传函数
const customUpload = async (options: any) => {
  const { file, fileList, onSuccess, onError } = options

  try {
    // 收集所有选中的文件
    const filesToUpload = fileList.map((f: any) => f.raw)

    if (filesToUpload.length === 0) {
      ElMessage.warning('请选择要上传的文件')
      onError(new Error('没有选择文件'))
      return
    }

    const formData = new FormData()
    // 添加所有文件，参数名必须是 'files'（复数）
    filesToUpload.forEach((file: File) => {
      formData.append('files', file)
    })

    const token = localStorage.getItem('token')
    const response = await api.post('/file/upload/batch', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': `Bearer ${token}`
      }
    })

    // 后端返回的是 URL 数组
    if (response.data && Array.isArray(response.data)) {
      // 将上传成功的文件信息添加到已上传列表
      response.data.forEach((url: string, index: number) => {
        if (filesToUpload[index]) {
          uploadedPhotos.value.push({
            name: filesToUpload[index].name,
            url: url,
            size: filesToUpload[index].size
          })
        }
      })

      ElMessage.success(`成功上传 ${response.data.length} 张照片`)
      onSuccess(response.data)
    } else {
      ElMessage.error('上传失败')
      onError(new Error('上传失败'))
    }
  } catch (error: any) {
    console.error('上传失败:', error)
    ElMessage.error('上传失败：' + (error.message || '请重试'))
    onError(error)
  }
}

// 获取订单信息
const fetchOrderInfo = async () => {
  const orderId = Number(route.query.orderId)
  if (!orderId) {
    ElMessage.error('订单 ID 无效')
    router.go(-1)
    return
  }

  try {
    const res = await api.get(`/orders/${orderId}`)
    order.value = res.data.order

    // 获取交付批次列表
    fetchDeliveryHistory(orderId)
  } catch (error) {
    console.error('获取订单信息失败:', error)
    ElMessage.error('获取订单信息失败')
  }
}

// 获取交付历史
const fetchDeliveryHistory = async (orderId: number) => {
  try {
    const res = await api.get(`/order-delivery/order/${orderId}`)
    deliveryHistory.value = res.data || []

    // 如果有未完成的交付批次，设置为当前交付
    const pendingDelivery = deliveryHistory.value.find(
        d => d.status === 'PENDING' || d.status === 'PARTIAL'
    )
    if (pendingDelivery) {
      currentDelivery.value = pendingDelivery
    }
  } catch (error) {
    console.error('获取交付历史失败:', error)
  }
}

// 创建交付批次
const createBatch = async () => {
  if (!order.value) return

  try {
    batchLoading.value = true

    const submitData = {
      orderId: order.value.id,
      photographerId: userStore.user?.id, // 需要获取摄影师 ID
      totalCount: batchForm.totalCount,
      deliveredCount: 0,
      status: 'PENDING',
      remark: batchForm.remark
    }

    const res = await api.post('/order-delivery/batch', submitData)
    currentDelivery.value = res.data

    ElMessage.success('批次创建成功，请上传照片')
  } catch (error) {
    console.error('创建批次失败:', error)
    ElMessage.error('创建批次失败，请重试')
  } finally {
    batchLoading.value = false
  }
}

// 上传前校验
const beforeUpload = (file: any) => {
  const isImage = file.type.startsWith('image/')
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
  }
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB！')
  }
  return isImage && isLt10M
}

// 文件变化处理
const handleFileChange = (file: any, uploadFileList: any[]) => {
  // 更新文件列表 - 直接赋值给响应式变量
  fileList.value = uploadFileList
}

// 文件移除处理
const handleFileRemove = (file: any, uploadFileList: any[]) => {
  // 更新文件列表 - 直接赋值给响应式变量
  fileList.value = uploadFileList
}

// 批量上传
const handleBatchUpload = async () => {
  console.log('fileList:', fileList.value.length)

  if (fileList.value.length === 0) {
    ElMessage.warning('请先选择文件')
    return
  }

  uploading.value = true
  uploadProgress.value = 0

  try {
    const filesToUpload = fileList.value.map((f: any) => f.raw || f)
    console.log('准备上传的文件:', filesToUpload)

    const formData = new FormData()

    filesToUpload.forEach((file: File) => {
      formData.append('files', file)
    })

    const token = localStorage.getItem('token')
    const response = await api.post('/file/upload/batch', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': `Bearer ${token}`
      },
      onUploadProgress: (progressEvent: any) => {
        if (progressEvent.total) {
          uploadProgress.value = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        }
      }
    })

    if (response.data && Array.isArray(response.data)) {
      // 准备照片数据，保存到数据库
      const photoData = response.data.map((url: string, index: number) => ({
        orderId: order.value?.id,
        deliveryId: currentDelivery.value?.id,
        photoNumber: filesToUpload[index]?.name || `P${String(index + 1).padStart(4, '0')}`,
        photoUrl: url,
        thumbnailUrl: url,
        isSelected: 0,
        isDelivered: 0,
        fileSize: filesToUpload[index]?.size || 0,
        width: 0,
        height: 0,
        description: '',
        tags: [],
        sortOrder: uploadedPhotos.value.length + index
      }))

      // 批量保存到数据库
      await api.post('/order-photo/batch', photoData)

      // 添加到已上传列表（用于显示）
      response.data.forEach((url: string, index: number) => {
        if (filesToUpload[index]) {
          uploadedPhotos.value.push({
            name: filesToUpload[index].name,
            url: url,
            size: filesToUpload[index].size
          })
        }
      })

      ElMessage.success(`成功上传并保存 ${response.data.length} 张照片`)
      uploadSuccess.value = true
      uploadProgress.value = 100

      // 清空文件列表
      fileList.value = []
      if (uploadRef.value) {
        uploadRef.value.clearFiles()
      }
    } else {
      ElMessage.error('上传失败')
    }
  } catch (error: any) {
    console.error('上传失败:', error)
    ElMessage.error('上传失败：' + (error.message || '请重试'))
  } finally {
    uploading.value = false
    setTimeout(() => {
      uploadSuccess.value = false
    }, 3000)
  }
}

// 提交交付
const submitDelivery = async () => {
  if (!currentDelivery.value || uploadedPhotos.value.length === 0) {
    ElMessage.warning('请先上传照片')
    return
  }

  try {
    submitLoading.value = true

    // 1. 批量更新照片的交付状态为 1（后端会自动更新 deliveredCount）
    const deliveryId = currentDelivery.value.id
    await api.put(`/order-photo/batch-delivery/${deliveryId}`, null, {
      params: {
        isDelivered: true
      }
    })

    // 2. 更新交付批次状态
    await api.put(`/order-delivery/${currentDelivery.value.id}/status`, null, {
      params: {
        status: 'COMPLETED'
      }
    })

    ElMessage.success('照片交付成功！')

    // 3. 重新获取交付历史，确保数据是最新的
    if (order.value) {
      await fetchDeliveryHistory(order.value.id)
    }

    router.go(-1)
  } catch (error) {
    console.error('交付失败:', error)
    ElMessage.error('交付失败，请重试')
  } finally {
    submitLoading.value = false
  }
}

// 继续上传
const continueUpload = () => {
  uploadRef.value?.clearFiles()
}

// 查看交付详情
const viewDeliveryDetail = async (delivery: Delivery) => {
  currentDetailDelivery.value = delivery
  detailDialogVisible.value = true

  // 从数据库获取该批次的照片列表
  try {
    photosLoading.value = true
    const res = await api.get(`/order-photo/delivery/${delivery.id}`)
    deliveryPhotos.value = res.data || []
  } catch (error) {
    console.error('获取照片列表失败:', error)
    ElMessage.error('获取照片列表失败')
    deliveryPhotos.value = []
  } finally {
    photosLoading.value = false
  }
}

// 获取交付状态类型
const getDeliveryStatusType = (status: string) => {
  const statusMap: Record<string, string> = {
    'PENDING': 'info',
    'PARTIAL': 'warning',
    'COMPLETED': 'success'
  }
  return statusMap[status] || 'info'
}

// 获取交付状态文本
const getDeliveryStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'PENDING': '待交付',
    'PARTIAL': '部分交付',
    'COMPLETED': '已完成'
  }
  return statusMap[status] || '未知'
}

// 获取订单状态类型
const getStatusType = (status?: string) => {
  if (!status) return 'info'
  const statusMap: Record<string, string> = {
    'PENDING': 'warning',
    'CONFIRMED': 'primary',
    'IN_PROGRESS': 'success',
    'COMPLETED': 'success',
    'CANCELED': 'info',
    'FAILED': 'danger'
  }
  return statusMap[status] || 'info'
}

// 获取订单状态文本
const getStatusText = (status?: string) => {
  if (!status) return '未知'
  const statusMap: Record<string, string> = {
    'PENDING': '待支付',
    'CONFIRMED': '已确认',
    'IN_PROGRESS': '拍摄中',
    'COMPLETED': '已完成',
    'CANCELED': '已取消',
    'FAILED': '已失败'
  }
  return statusMap[status] || '未知'
}

// 格式化日期时间
const formatDateTime = (dateTime?: string) => {
  if (!dateTime) return ''
  return dayjs(dateTime).format('YYYY-MM-DD HH:mm')
}

// 格式化文件大小
const formatFileSize = (bytes: number) => {
  if (!bytes) return '未知'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

// 删除单张照片
const removePhoto = async (index: number) => {
  const photo = uploadedPhotos.value[index]

  try {
    await ElMessageBox.confirm(
      `确定要删除照片 "${photo.name}" 吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 从数组中移除
    uploadedPhotos.value.splice(index, 1)

    ElMessage.success('删除成功')
  } catch {
    // 用户取消删除，不做任何操作
  }
}

// 清空所有照片
const clearAllPhotos = async () => {
  if (uploadedPhotos.value.length === 0) {
    ElMessage.warning('没有可删除的照片')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要清空所有 ${uploadedPhotos.value.length} 张照片吗？此操作不可恢复！`,
      '清空确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'error'
      }
    )

    // 清空数组
    uploadedPhotos.value = []

    // 同时清空文件列表
    fileList.value = []
    if (uploadRef.value) {
      uploadRef.value.clearFiles()
    }

    ElMessage.success('已清空所有照片')
  } catch {
    // 用户取消操作，不做任何操作
  }
}

// 返回
const goBack = () => {
  router.go(-1)
}

onMounted(() => {
  fetchOrderInfo()
})
</script>

<style scoped>
.deliver-photos {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 30px;
}

.page-header h1 {
  margin: 0;
  color: #333;
}

.deliver-card {
  margin-bottom: 20px;
}

.order-info,
.delivery-info,
.create-batch,
.upload-section,
.delivery-history {
  margin-bottom: 30px;
  padding: 20px 0;
  border-bottom: 1px solid #eee;
}

.order-info:last-child,
.delivery-info:last-child,
.create-batch:last-child,
.upload-section:last-child,
.delivery-history:last-child {
  border-bottom: none;
}

.order-info h3,
.delivery-info h3,
.create-batch h3,
.upload-section h3,
.delivery-history h3 {
  margin-bottom: 20px;
  color: #333;
}

.upload-progress {
  margin: 20px 0;
}

.uploaded-photos {
  margin-top: 20px;
}

.uploaded-photos h4 {
  margin-bottom: 15px;
  color: #333;
}

.photo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 15px;
}

.photo-item {
  cursor: pointer;
  transition: all 0.3s;
}

.photo-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.photo-thumb {
  width: 100%;
  height: 150px;
  object-fit: cover;
}

.photo-info {
  padding: 10px;
}

.photo-name {
  font-size: 14px;
  color: #333;
  margin-bottom: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.photo-size {
  font-size: 12px;
  color: #999;
  margin-bottom: 5px;
}

.photo-status {
  margin-top: 5px;
}

.photo-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.action-buttons {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}
</style>
