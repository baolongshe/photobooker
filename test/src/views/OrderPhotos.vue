<template>
  <div class="order-photos">
    <div class="page-header">
      <el-button @click="goBack" icon="ArrowLeft">返回订单详情</el-button>
      <h1>照片查看</h1>
    </div>

    <el-card class="photos-card">
      <!-- 照片统计信息 -->
      <div class="statistics">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-statistic title="照片总数" :value="statistics.totalCount" />
          </el-col>
          <el-col :span="12">
            <el-statistic title="精选照片" :value="statistics.selectedCount" />
          </el-col>
        </el-row>
      </div>

      <!-- 筛选和排序 -->
      <div class="filter-bar">
        <el-radio-group v-model="filterType" size="large" @change="filterPhotos">
          <el-radio-button label="all">全部照片</el-radio-button>
          <el-radio-button label="selected">精选照片</el-radio-button>
        </el-radio-group>

        <el-input
            v-model="searchKeyword"
            placeholder="搜索照片编号或描述..."
            prefix-icon="Search"
            style="width: 300px"
            clearable
            @input="searchPhotos"
        />
      </div>

      <!-- 照片网格 -->
      <div class="photo-grid">
        <el-card
            v-for="photo in filteredPhotos"
            :key="photo.id"
            class="photo-item"
            :body-style="{ padding: '0px' }"
            @click="viewPhotoDetail(photo)"
        >
          <div class="photo-wrapper">
            <img :src="photo.thumbnailUrl || photo.photoUrl" class="photo-image" />

            <!-- 照片标签 -->
            <div class="photo-tags">
              <el-tag
                  v-for="(tag, index) in photo.tags"
                  :key="index"
                  size="small"
                  :type="tag === '精修' ? 'success' : 'info'"
                  class="photo-tag"
              >
                {{ tag }}
              </el-tag>
            </div>

            <!-- 照片状态标识 -->
            <div class="photo-status">
              <el-tag
                  v-if="photo.isSelected === 1"
                  type="warning"
                  size="small"
                  effect="dark"
                  class="status-tag"
              >
                精选
              </el-tag>
              <!-- 收藏按钮 -->
              <el-button
                  v-if="isLoggedIn"
                  type="danger"
                  size="small"
                  circle
                  @click.stop="toggleFavorite(photo)"
                  class="favorite-btn"
              >
                <el-icon>
                  <Star v-if="!photo.isFavorited" />
                  <StarFilled v-else />
                </el-icon>
              </el-button>
            </div>

            <!-- 照片编号 -->
            <div class="photo-number">{{ photo.photoNumber }}</div>
          </div>
        </el-card>
      </div>

      <!-- 空状态 -->
      <el-empty
          v-if="filteredPhotos.length === 0"
          description="暂无照片"
      />
    </el-card>

    <!-- 照片详情对话框 -->
    <el-dialog
        v-model="detailDialogVisible"
        title="照片详情"
        width="80%"
        :close-on-click-modal="false"
    >
      <div class="photo-detail" v-if="currentPhoto">
        <el-row :gutter="20">
          <el-col :span="16">
            <div class="photo-viewer">
              <img :src="currentPhoto.photoUrl" class="detail-image" />
            </div>
          </el-col>
          <el-col :span="8">
            <div class="photo-info-panel">
              <h3>照片信息</h3>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="照片编号">
                  {{ currentPhoto.photoNumber }}
                </el-descriptions-item>
                <el-descriptions-item label="尺寸">
                  {{ currentPhoto.width }} x {{ currentPhoto.height }} px
                </el-descriptions-item>
                <el-descriptions-item label="文件大小">
                  {{ formatFileSize(currentPhoto.fileSize) }}
                </el-descriptions-item>
                <el-descriptions-item label="标签">
                  <el-tag
                      v-for="(tag, index) in currentPhoto.tags"
                      :key="index"
                      size="small"
                      style="margin-right: 5px"
                  >
                    {{ tag }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag :type="currentPhoto.isDelivered === 1 ? 'success' : 'info'">
                    {{ currentPhoto.isDelivered === 1 ? '已交付' : '待交付' }}
                  </el-tag>
                  <el-tag type="warning" v-if="currentPhoto.isSelected === 1" style="margin-left: 5px">
                    精选
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="描述">
                  {{ currentPhoto.description || '无' }}
                </el-descriptions-item>
              </el-descriptions>

              <div class="action-buttons" style="margin-top: 20px">
                <el-button
                    type="primary"
                    @click="downloadPhoto(currentPhoto)"
                    :disabled="currentPhoto.isDelivered !== 1"
                    :loading="downloadLoading"
                >
                  下载照片
                </el-button>
                <el-button @click="copyPhotoLink(currentPhoto)">
                  复制链接
                </el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import api from '../utils/api'
import { Star, StarFilled } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

interface OrderPhoto {
  id: number
  deliveryId: number
  orderId: number
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
  isFavorited?: boolean  // 新增：是否已收藏
}

const order = ref<any>(null)
const photos = ref<OrderPhoto[]>([])
const filteredPhotos = ref<OrderPhoto[]>([])
const statistics = reactive({
  totalCount: 0,
  selectedCount: 0
})

const filterType = ref('all')
const searchKeyword = ref('')
const detailDialogVisible = ref(false)
const currentPhoto = ref<OrderPhoto | null>(null)
const downloadLoading = ref(false)

// 新增：登录状态
const isLoggedIn = computed(() => !!userStore.user?.id)

// 获取订单照片
const fetchPhotos = async () => {
  const orderId = Number(route.query.orderId)
  if (!orderId) {
    ElMessage.error('订单 ID 无效')
    router.go(-1)
    return
  }

  try {
    const res = await api.get(`/order-photo/order/${orderId}`)
    photos.value = res.data || []

    // 如果用户已登录，查询用户的收藏状态
    if (userStore.user?.id) {
      // 批量查询收藏状态
      const favoritePromises = photos.value.map(async (photo) => {
        try {
          const favoriteRes = await api.get(`/portfolio/like/check`, {
            params: {
              userId: userStore.user.id,
              portfolioId: photo.id
            }
          })
          photo.isFavorited = favoriteRes.data?.favorited || false
        } catch (error) {
          photo.isFavorited = false
        }
      })
      await Promise.all(favoritePromises)
    }

    filteredPhotos.value = photos.value

    // 更新统计信息
    statistics.totalCount = photos.value.length
    statistics.selectedCount = photos.value.filter(p => p.isSelected === 1).length
  } catch (error) {
    console.error('获取照片失败:', error)
    ElMessage.error('获取照片失败')
  }
}

// 筛选照片
const filterPhotos = () => {
  let result = [...photos.value]

  if (filterType.value === 'selected') {
    result = result.filter(p => p.isSelected === 1)
  }

  // 应用搜索筛选
  if (searchKeyword.value) {
    result = result.filter(p =>
        p.photoNumber.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
        (p.description && p.description.toLowerCase().includes(searchKeyword.value.toLowerCase()))
    )
  }

  filteredPhotos.value = result
}

// 搜索照片
const searchPhotos = () => {
  filterPhotos()
}

// 查看照片详情
const viewPhotoDetail = (photo: OrderPhoto) => {
  currentPhoto.value = photo
  detailDialogVisible.value = true
}

// 下载照片
const downloadPhoto = async (photo: OrderPhoto) => {
  if (!photo || photo.isDelivered !== 1) {
    ElMessage.warning('该照片尚未交付，无法下载')
    return
  }

  try {
    downloadLoading.value = true
    // 创建下载链接
    const link = document.createElement('a')
    link.href = photo.photoUrl
    link.download = `${photo.photoNumber}.jpg`
    link.target = '_blank'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    ElMessage.success('下载已开始')
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败，请重试')
  } finally {
    downloadLoading.value = false
  }
}

// 复制照片链接
const copyPhotoLink = (photo: OrderPhoto) => {
  navigator.clipboard.writeText(photo.photoUrl)
  ElMessage.success('链接已复制到剪贴板')
}

// 格式化文件大小
const formatFileSize = (bytes: number) => {
  if (!bytes) return '未知'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

// 收藏照片
const favoritePhoto = async (photo: OrderPhoto) => {
  if (!userStore.user?.id) {
    ElMessage.warning('请先登录')
    return
  }

  try {
    // 使用 /portfolio/order-photo/like 接口，传递完整的照片信息
    const response = await api.post(`/portfolio/order-photo/like`, {
      userId: userStore.user.id,
      orderPhotoId: photo.id,
      photoUrl: photo.photoUrl,
      thumbnailUrl: photo.thumbnailUrl,
      photoNumber: photo.photoNumber,
      orderId: photo.orderId  // 新增：传递订单ID
    })

    if (response.code === 1) {
      photo.isFavorited = true
      ElMessage.success('收藏成功')
    } else {
      ElMessage.warning(response.msg || '收藏失败')
    }
  } catch (error: any) {
    console.error('收藏失败:', error)
    ElMessage.error(error.response?.data?.msg || '收藏失败，请重试')
  }
}

// 取消收藏
const unfavoritePhoto = async (photo: OrderPhoto) => {
  if (!userStore.user?.id) {
    ElMessage.warning('请先登录')
    return
  }

  try {
    // 使用 /portfolio/order-photo/like 接口
    const response = await api.delete(`/portfolio/order-photo/like`, {
      params: {
        userId: userStore.user.id,
        orderPhotoId: photo.id
      }
    })

    if (response.code === 1) {
      // 更新本地状态
      photo.isFavorited = false
      ElMessage.success('已取消收藏')
    } else {
      ElMessage.warning(response.msg || '取消收藏失败')
    }
  } catch (error: any) {
    console.error('取消收藏失败:', error)
    ElMessage.error(error.response?.data?.msg || '取消收藏失败，请重试')
  }
}

// 切换收藏状态
const toggleFavorite = (photo: OrderPhoto) => {
  if (photo.isFavorited) {
    unfavoritePhoto(photo)
  } else {
    favoritePhoto(photo)
  }
}

// 返回
const goBack = () => {
  router.go(-1)
}

onMounted(() => {
  fetchPhotos()
})
</script>

<style scoped>
.order-photos {
  max-width: 1400px;
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

.photos-card {
  margin-bottom: 20px;
}

.statistics {
  margin-bottom: 30px;
  padding: 20px 0;
  border-bottom: 1px solid #eee;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  gap: 20px;
}

.photo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.photo-item {
  cursor: pointer;
  transition: all 0.3s;
}

.photo-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.photo-wrapper {
  position: relative;
  overflow: hidden;
  aspect-ratio: 1;
}

.photo-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.photo-item:hover .photo-image {
  transform: scale(1.1);
}

.photo-tags {
  position: absolute;
  top: 10px;
  left: 10px;
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}

.photo-tag {
  background: rgba(255, 255, 255, 0.9);
}

.photo-status {
  position: absolute;
  top: 10px;
  right: 10px;
  display: flex;
  gap: 5px;
  flex-direction: column;
}

.favorite-btn {
  background: rgba(255, 255, 255, 0.8);
  border: none;
  padding: 5px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.favorite-btn:hover {
  background: rgba(255, 255, 255, 1);
  transform: scale(1.1);
}

.photo-number {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.7), transparent);
  color: white;
  padding: 20px 10px 8px;
  font-size: 14px;
  font-weight: bold;
}

.photo-detail {
  padding: 20px;
}

.photo-viewer {
  text-align: center;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}

.detail-image {
  max-width: 100%;
  max-height: 600px;
  object-fit: contain;
}

.photo-info-panel {
  padding: 10px;
}

.photo-info-panel h3 {
  margin-bottom: 20px;
  color: #333;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

@media (max-width: 768px) {
  .photo-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 10px;
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-bar .el-radio-group {
    width: 100%;
  }

  .filter-bar .el-input {
    width: 100%;
  }
}
</style>
