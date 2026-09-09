<template>
  <div class="portfolio-detail">
    <div class="page-header">
      <el-button @click="$router.go(-1)" icon="ArrowLeft">返回</el-button>
      <h1>作品详情</h1>
    </div>

    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="10" animated />
    </div>

    <div v-else-if="portfolio" class="portfolio-content">
      <el-row :gutter="40">
        <!-- 作品图片展示 -->
        <el-col :span="16">
          <el-card class="image-gallery">
            <template #header>
              <h3>{{ portfolio.title }}</h3>
            </template>
            
            <!-- 主图展示 -->
            <div class="main-image">
              <img :src="currentImage" :alt="portfolio.title" />
            </div>  
            
            <!-- 缩略图列表 -->
            <div v-if="imageList.length > 1" class="thumbnail-list">
              <div 
                v-for="(image, index) in imageList" 
                :key="index"
                class="thumbnail-item"
                :class="{ active: currentImageIndex === index }"
                @click="currentImageIndex = index"
              >
                <img :src="image" :alt="`${portfolio.title} - 图片${index + 1}`" />
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 作品信息 -->
        <el-col :span="8">
          <el-card class="portfolio-info">
            <template #header>
              <h3>作品信息</h3>
            </template>
            
            <div class="info-section">
              <h4>{{ portfolio.title }}</h4>
              <p class="description">{{ portfolio.description }}</p>
              
              <div class="meta-info">
                <div class="meta-item">
                  <el-icon><Camera /></el-icon>
                  <span>分类：{{ portfolio.category }}</span>
                </div>
                
                <div v-if="portfolio.shootingLocation" class="meta-item">
                  <el-icon><Location /></el-icon>
                  <span>拍摄地点：{{ portfolio.shootingLocation }}</span>
                </div>
                
                <div v-if="portfolio.shootingDate" class="meta-item">
                  <el-icon><Calendar /></el-icon>
                  <span>拍摄时间：{{ formatDate(portfolio.shootingDate) }}</span>
                </div>
                
                <div v-if="portfolio.equipment" class="meta-item">
                  <el-icon><Tools /></el-icon>
                  <span>设备：{{ portfolio.equipment }}</span>
                </div>
              </div>
              
              <div class="stats">
                <div class="stat-item">
                  <el-icon><View /></el-icon>
                  <span>{{ portfolio.viewCount }} 浏览</span>
                </div>
                <div class="stat-item">
                  <el-icon><Star /></el-icon>
                  <span>{{ portfolio.likeCount }} 点赞</span>
                </div>
              </div>
              
              <div v-if="portfolio.tags" class="tags-section">
                <h5>标签</h5>
                <div class="tags">
                  <el-tag 
                    v-for="tag in portfolio.tags.split(',')" 
                    :key="tag" 
                    size="small"
                    class="tag-item"
                  >
                    {{ tag.trim() }}
                  </el-tag>
                </div>
              </div>
              
              <div class="actions">
                <el-button 
                  type="primary" 
                  @click="handleLike"
                  :loading="likeLoading"
                  :class="{ 'is-liked': isLiked }"
                >
                  <el-icon><Star /></el-icon>
                  {{ isLiked ? '取消点赞' : '点赞' }}
                </el-button>
                
                <el-button @click="handleDownload">
                  <el-icon><Download /></el-icon>
                  下载
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-empty v-else description="作品不存在" />

    <!-- 评论区入口 -->
    <div class="comments-section">
      <h3>评论区</h3>
      <el-form v-if="userStore.user" @submit.prevent="submitComment">
        <el-input v-model="newComment" type="textarea" :rows="2" placeholder="说点什么..." />
        <el-button type="primary" @click="submitComment" :disabled="!newComment.trim() && !replyingTo">发表评论</el-button>
      </el-form>
      <el-empty v-else description="请登录后评论" />
      <div class="comment-list">
        <CommentItem
          v-for="comment in rootComments"
          :key="comment.id"
          :comment="comment"
          @submit-reply="submitReply"
          @delete-comment="deleteComment"
          @show-reply="showReply"
          @cancel-reply="cancelReply"
          @on-avatar-error="onAvatarError"
        />
      </div>
      <el-pagination
        v-if="total > pageSize"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
        style="margin-top: 16px;"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import api from '../utils/api'
import { Camera, Location, Calendar, Tools, View, Star, Download } from '@element-plus/icons-vue'
import CommentItem from './CommentItem.vue'

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
  isFeatured: boolean;
  createTime: string;
  isLiked?: boolean; // 新增字段
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const portfolio = ref<Portfolio | null>(null)
const loading = ref(false)
const likeLoading = ref(false)
const isLiked = ref(false)

// 检查是否已点赞（建议后端提供接口，暂用作品详情接口返回的likeCount和用户是否已点赞字段）
// 这里假设后端未来会返回 isLiked 字段

// 点赞/取消点赞
const handleLike = async () => {
  if (!portfolio.value || !userStore.user) return
  try {
    likeLoading.value = true
    if (isLiked.value) {
      await api.delete(`/portfolio/${portfolio.value.id}/like?userId=${userStore.user.id}`)
      await fetchPortfolioDetail()
      ElMessage.success('取消点赞成功')
    } else {
      await api.post(`/portfolio/${portfolio.value.id}/like`, {
        userId: userStore.user.id
      })
      await fetchPortfolioDetail()
      ElMessage.success('点赞成功')
    }
  } catch (error: any) {
    // 如果后端返回“你已经点过赞了”，也刷新详情，防止按钮卡死
    await fetchPortfolioDetail()
    if (error?.response?.data?.msg) {
      ElMessage.warning(error.response.data.msg)
    } else {
      ElMessage.error('操作失败，请重试')
    }
  } finally {
    likeLoading.value = false
  }
}



// 格式化日期
const formatDate = (dateStr: string) => {
  return dayjs(dateStr).format('YYYY年MM月DD日')
}

const currentImageIndex = ref(0)
const comments = ref<any[]>([])
const rootComments = ref<any[]>([])
const newComment = ref('')
const replyContent = ref('')
const replyingTo = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
// 默认头像改为本地图片
const defaultAvatar = '/src/assets/default-avatar.png'

// 计算属性
const imageList = computed(() => {
  if (!portfolio.value?.imageUrls) return [portfolio.value?.coverImage]
  
  try {
    const urls = JSON.parse(portfolio.value.imageUrls)
    return Array.isArray(urls) ? urls : [portfolio.value.coverImage]
  } catch (e) {
    return [portfolio.value.coverImage]
  }
})

const currentImage = computed(() => {
  return imageList.value[currentImageIndex.value] || portfolio.value?.coverImage
})

// 获取作品详情
const fetchPortfolioDetail = async () => {
  const id = route.params.id
  if (!id) return
  
  try {
    loading.value = true
    // 请求作品详情时带上userId参数，后端返回isLiked字段
    const res = await api.get(`/portfolio/${id}?userId=${userStore.user?.id || ''}`)
    portfolio.value = res.data
    isLiked.value = !!res.data.isLiked // 关键：同步后端的点赞状态
  } catch (error) {
    console.error('获取作品详情失败:', error)
    ElMessage.error('获取作品详情失败')
  } finally {
    loading.value = false
  }
}

const fetchComments = async () => {
  const res = await api.get(`/portfolio/${route.params.id}/comments?page=${currentPage.value}&size=${pageSize.value}`)
  comments.value = res.data.comments || []
  total.value = res.data.total || 0
  // 构建楼中楼结构
  const map = new Map()
  comments.value.forEach(c => { c.children = []; map.set(c.id, c) })
  rootComments.value = []
  comments.value.forEach(c => {
    if (c.parentId) {
      const parent = map.get(c.parentId)
      if (parent) parent.children.push(c)
    } else {
      rootComments.value.push(c)
    }
  })
  console.log(rootComments.value.map(c => c.id))
}

const submitComment = async () => {
  if (!userStore.user) {
    ElMessage.warning('请先登录')
    return
  }
  console.log({
  userId: userStore.user.id,
  userName: userStore.user.username,
  content: newComment.value
})
  console.log('userStore.user:', userStore.user)
  if (!newComment.value.trim()) return
  await api.post(`/portfolio/${route.params.id}/comment`, {
    userId: userStore.user.id!,
    userName: userStore.user.username!,
    content: newComment.value,
    avatar: userStore.user.avatar
  }
)

  newComment.value = ''
  fetchComments()
}

const showReply = (comment: any) => {
  replyingTo.value = comment.id
  replyContent.value = `@${comment.userName} `
}
const cancelReply = () => {
  replyingTo.value = null
  replyContent.value = ''
}
const submitReply = async (parentComment: any) => {
  if (!userStore.user) {
    ElMessage.warning('请先登录')
    return
  }
  if (!replyContent.value.trim()) return
  await api.post(`/portfolio/${route.params.id}/comment`, {
    userId: userStore.user.id!,
    userName: userStore.user.username!,
    content: replyContent.value,
    parentId: parentComment.id,
    parentUserName: parentComment.userName,
    avatar: userStore.user.avatar // 修复：补充头像字段
  })
  replyContent.value = ''
  replyingTo.value = null
  fetchComments()
}

const deleteComment = async (commentId: number) => {
  if (!userStore.user) {
    ElMessage.warning('请先登录')
    return
  }
  await api.delete(`/portfolio/comment/${commentId}?userId=${userStore.user.id!}`)
  fetchComments()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchComments()
}

// @高亮
const highlightAt = (content: string) => {
  return content.replace(/@([\w\u4e00-\u9fa5]+)/g, '<span class="at-user">@$1</span>')
}

function onAvatarError(e: Event) {
  const target = e.target as HTMLImageElement | null
  if (target && target.src !== defaultAvatar) {
    target.src = defaultAvatar
  }
}

// 下载图片
const handleDownload = async () => {
  if (!portfolio.value) return

  const imageUrl = currentImage.value
  if (!imageUrl) {
    ElMessage.error('没有可下载的图片')
    return
  }

  try {
    ElMessage.info('正在准备下载...')

    // 方案：通过新标签页打开图片，触发浏览器下载
    // 这样可以绕过 CORS 限制
    const link = document.createElement('a')
    link.href = imageUrl
    link.target = '_blank'
    link.download = `${portfolio.value.title}_${portfolio.value.photographerId}_${new Date().getTime()}.jpg`

    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    ElMessage.success('图片下载成功')
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败，请重试')
  }
}

onMounted(() => {
  fetchPortfolioDetail()
  fetchComments()
  // checkLiked() // 移除 localStorage 检查
})
</script>

<style scoped>
.portfolio-detail {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
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

.loading-container {
  padding: 40px;
}

.portfolio-content {
  margin-bottom: 40px;
}

.image-gallery {
  margin-bottom: 30px;
}

.main-image {
  margin-bottom: 20px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.main-image img {
  width: 100%;
  height: 500px;
  object-fit: cover;
  display: block;
}

.thumbnail-list {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding: 10px 0;
}

.thumbnail-item {
  flex-shrink: 0;
  width: 80px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.3s ease;
}

.thumbnail-item.active {
  border-color: #409eff;
}

.thumbnail-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.portfolio-info {
  position: sticky;
  top: 20px;
}

.info-section h4 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 1.3rem;
}

.description {
  color: #666;
  line-height: 1.6;
  margin-bottom: 20px;
}

.meta-info {
  margin-bottom: 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  color: #666;
}

.meta-item .el-icon {
  color: #409eff;
}

.stats {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #666;
}

.stat-item .el-icon {
  color: #409eff;
}

.tags-section {
  margin-bottom: 20px;
}

.tags-section h5 {
  margin: 0 0 10px 0;
  color: #333;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  margin: 0;
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.actions .el-button {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.actions .el-button:first-child {
  /* 点赞按钮 */
}

.actions .el-button:last-child {
  /* 下载按钮 - 使用默认样式 */
  background-color: #fff;
  border-color: #dcdfe6;
  color: #606266;
}

.actions .el-button:last-child:hover {
  color: #67c23a;
  border-color: #c2e7b0;
  background-color: #f0f9eb;
}

.is-liked {
  background-color: #f56c6c;
  border-color: #f56c6c;
}

.is-liked:hover {
  background-color: #f78989;
  border-color: #f78989;
}

.comments-section {
  margin-top: 32px;
  background: #fff;
  border-radius: 8px;
  padding: 24px 32px 32px 32px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  font-size: 15px;
}
.comment-list {
  margin-top: 18px;
}
.comment-item {
  position: relative;
  margin-bottom: 24px;
  padding: 16px 16px 8px 56px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(64,158,255,0.04);
  border: 1px solid #f0f0f0;
  min-height: 56px;
}
.comment-avatar {
  position: absolute;
  left: 16px;
  top: 16px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e6f0fa;
  object-fit: cover;
  border: 1px solid #dbeafe;
}
.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  color: #222;
  margin-bottom: 2px;
}
.comment-user {
  font-weight: bold;
  color: #409EFF;
  font-size: 16px;
}
.comment-time {
  color: #b0b0b0;
  font-size: 13px;
  margin-left: 4px;
}
.comment-content {
  margin: 8px 0 10px 0;
  font-size: 16px;
  color: #222;
  line-height: 1.7;
  word-break: break-all;
}
.comment-children {
  margin-left: 32px;
  border-left: 3px solid #e6f0fa;
  padding-left: 16px;
  margin-top: 12px;
  background: #f8fbff;
  border-radius: 6px;
}
.comment-child {
  margin-bottom: 8px;
}
.comment-reply {
  color: #67c23a;
  font-size: 14px;
  margin-left: 4px;
}
.at-user {
  color: #409EFF;
  font-weight: bold;
  background: #e6f0fa;
  padding: 0 2px;
  border-radius: 2px;
}
.reply-box {
  margin: 10px 0 0 0;
  display: flex;
  gap: 8px;
}
.el-button {
  font-size: 13px;
  padding: 2px 10px;
}
.el-button[type='text'] {
  color: #409EFF;
  font-size: 13px;
  padding: 2px 8px;
  border-radius: 4px;
}
.el-button[type='text']:hover {
  color: #fff;
  background: #409EFF;
}
.el-form {
  margin-bottom: 10px;
}
.el-input__inner, .el-textarea__inner {
  font-size: 15px;
}
</style>