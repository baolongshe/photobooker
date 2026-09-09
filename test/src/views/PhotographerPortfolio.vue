<template>
  <div class="photographer-portfolio">
    <div class="page-header">
      <h1>我的作品管理</h1>
      <el-button type="primary" @click="showAddDialog = true" icon="Plus">
        添加作品
      </el-button>
    </div>

    <!-- 筛选条件 -->
    <el-card class="filter-card">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-select v-model="filters.category" placeholder="作品分类" clearable>
            <el-option label="婚纱" value="婚纱" />
            <el-option label="写真" value="写真" />
            <el-option label="纪实" value="纪实" />
            <el-option label="商业" value="商业" />
            <el-option label="人像" value="人像" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filters.status" placeholder="作品状态" clearable>
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="已下架" :value="2" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filters.isFeatured" placeholder="精选状态" clearable>
            <el-option label="精选作品" :value="1" />
            <el-option label="普通作品" :value="0" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 作品列表 -->
    <el-card class="portfolio-list">
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>

      <div v-else-if="portfolios.length > 0" class="portfolio-grid">
        <div v-for="portfolio in portfolios" :key="portfolio.id" class="portfolio-card">
          <div class="portfolio-image">
            <img :src="portfolio.coverImage" :alt="portfolio.title" />
            <div class="portfolio-overlay">
              <div class="portfolio-actions">
                <el-button size="small" @click="editPortfolio(portfolio)">编辑</el-button>
                <el-button size="small" type="danger" @click="deletePortfolio(portfolio.id)">删除</el-button>
              </div>
            </div>
            <div class="portfolio-status">
              <el-tag :type="getStatusType(portfolio.status)" size="small">
                {{ getStatusText(portfolio.status) }}
              </el-tag>
              <el-tag v-if="portfolio.isFeatured" type="success" size="small">精选</el-tag>
            </div>
          </div>

          <div class="portfolio-info">
            <h4>{{ portfolio.title }}</h4>
            <p class="description">{{ portfolio.description }}</p>

            <div class="meta-info">
              <el-tag size="small" type="primary">{{ portfolio.category }}</el-tag>
              <span class="stats">
                <el-icon><View /></el-icon> {{ portfolio.viewCount }}
                <el-icon><Star /></el-icon> {{ portfolio.likeCount }}
              </span>
            </div>

            <div class="portfolio-tags" v-if="portfolio.tags">
              <el-tag
                v-for="tag in portfolio.tags.split(',')"
                :key="tag"
                size="small"
                type="info"
                class="tag-item"
              >
                {{ tag.trim() }}
              </el-tag>
            </div>

            <div class="portfolio-actions-bottom">
              <el-button
                size="small"
                :type="portfolio.isFeatured ? 'success' : 'default'"
                @click="toggleFeatured(portfolio)"
              >
                {{ portfolio.isFeatured ? '取消精选' : '设为精选' }}
              </el-button>
              <el-button
                size="small"
                :type="portfolio.status === 1 ? 'warning' : 'success'"
                @click="toggleStatus(portfolio)"
              >
                {{ portfolio.status === 1 ? '下架' : '发布' }}
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无作品" />

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[6, 12, 24, 48]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 添加/编辑作品对话框 -->
    <el-dialog
      v-model="showAddDialog"
      :title="editingPortfolio ? '编辑作品' : '添加作品'"
      width="800px"
    >
      <el-form
        ref="portfolioFormRef"
        :model="portfolioForm"
        :rules="portfolioRules"
        label-width="100px"
      >
        <el-form-item label="作品标题" prop="title">
          <el-input v-model="portfolioForm.title" placeholder="请输入作品标题" />
        </el-form-item>

        <el-form-item label="作品描述" prop="description">
          <el-input
            v-model="portfolioForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入作品描述"
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="作品分类" prop="category">
              <el-select v-model="portfolioForm.category" placeholder="请选择分类">
                <el-option label="婚纱" value="婚纱" />
                <el-option label="写真" value="写真" />
                <el-option label="纪实" value="纪实" />
                <el-option label="商业" value="商业" />
                <el-option label="人像" value="人像" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作品标签">
              <el-input v-model="portfolioForm.tags" placeholder="多个标签用逗号分隔" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="拍摄地点">
              <el-input v-model="portfolioForm.shootingLocation" placeholder="请输入拍摄地点" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="拍摄时间">
              <el-date-picker
                v-model="portfolioForm.shootingDate"
                type="date"
                placeholder="选择拍摄时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="使用设备">
          <el-input v-model="portfolioForm.equipment" placeholder="请输入使用的设备信息" />
        </el-form-item>

        <el-form-item label="封面图片" prop="coverImage">
          <!-- 核心：添加 list-type="picture-card" 对齐作品上传样式 -->
          <el-upload
              class="cover-uploader"
              list-type="picture-card"
          :show-file-list="false"
          :auto-upload="false"
          action="#"
              :on-remove="handleCoverRemove"
          :on-change="handleCoverChange"
          accept="image/*"
          >
          <!-- 无预览图时显示加号（和作品上传的加号布局一致） -->
          <div v-if="!portfolioForm.coverImage" style="width: 100%; height: 100%; display: flex; align-items: center; justify-content: center;">
            <el-icon><Plus /></el-icon>
          </div>
          <!-- 有预览图时显示图片（和作品上传的预览图样式一致） -->
          <img v-else :src="portfolioForm.coverImage" style="width: 100%; height: 100%; object-fit: cover;" />
          </el-upload>
          <div style="margin-top: 10px; font-size: 12px; color: #999;">
            支持jpg/png/jpeg格式，大小不超过10MB
          </div>
        </el-form-item>


        <el-form-item label="作品图片">
          <el-upload
            class="image-uploader"
            list-type="picture-card"
            :auto-upload="false"
            :on-change="handleImageChange"
            :on-remove="handleImageRemove"
            :file-list="imageFileList"
            accept="image/*"
            multiple
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div style="margin-top: 10px; font-size: 12px; color: #999;">
            可上传多张作品图片，支持jpg/png/jpeg格式
          </div>
        </el-form-item>

        <el-form-item label="排序权重">
          <el-input-number v-model="portfolioForm.sortWeight" :min="0" :max="1000" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="savePortfolio" :loading="saving">
          {{ editingPortfolio ? '更新' : '保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import api from '../utils/api'
import { View, Star } from '@element-plus/icons-vue'

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
  sortWeight: number;
  createTime: string;
}

const userStore = useUserStore()

const portfolios = ref<Portfolio[]>([])
const loading = ref(false)
const saving = ref(false)
const showAddDialog = ref(false)
const editingPortfolio = ref<Portfolio | null>(null)
const portfolioFormRef = ref()

// 分页
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

// 筛选条件
const filters = reactive({
  category: '',
  status: null as number | null,
  isFeatured: null as number | null
})

// 表单数据 - 修改imageUrls为文件对象数组
const portfolioForm = reactive({
  title: '',
  description: '',
  category: '',
  tags: '',
  coverImage: '',
  imageUrls: [] as File[], // 改为存储File对象
  shootingDate: '',
  shootingLocation: '',
  equipment: '',
  sortWeight: 0
})

// 添加待上传文件列表
const pendingFiles = ref<File[]>([])
const coverFile = ref<File | null>(null)

// 封面图片选择回调
const handleCoverChange = (uploadFile: any) => {
  // 1. 拿到原生File对象
  const file = uploadFile.raw
  if (!file) return

  // 2. 校验文件类型（仅允许图片）
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('请选择jpg/png/jpeg格式的图片！')
    return
  }

  // 3. 校验文件大小（10MB）
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('封面图片大小不能超过10MB！')
    return
  }

  // 4. 保存文件对象
  coverFile.value = file

  // 5. 生成预览地址并显示
  const reader = new FileReader()
  reader.onload = (e) => {
    portfolioForm.coverImage = e.target?.result as string
  }
  reader.readAsDataURL(file)
}

// 作品图片选择回调
const handleImageChange = (file: any, fileList: any[]) => {
  imageFileList.value = fileList
  // 筛选出待上传的原生File对象
  pendingFiles.value = fileList
      .filter(item => item.raw) // 只保留有原生文件的项
      .map(item => item.raw)   // 提取原生File对象
}

// 封面图片移除回调
const handleCoverRemove = () => {
  coverFile.value = null
  portfolioForm.coverImage = ''
}

// 作品图片移除回调
const handleImageRemove = (file: any, fileList: any[]) => {
  imageFileList.value = fileList
  // 筛选出待上传的原生File对象
  pendingFiles.value = fileList
      .filter(item => item.raw) // 只保留有原生文件的项
      .map(item => item.raw)   // 提取原生File对象

}

// 添加文件列表引用
const imageFileList = ref<{ name: string; url: string }[]>([])

// 封面图片上传成功回调
const handleCoverUploadSuccess = (response: any, file: any) => {
  if (response.code === 1) {
    portfolioForm.coverImage = response.data
    ElMessage.success('封面图片上传成功')
  } else {
    ElMessage.error('封面图片上传失败')
  }
}

// 作品图片上传成功回调
const handleImageUploadSuccess = (response: any, file: any, fileList: any[]) => {
  if (response.code === 1) {
    portfolioForm.imageUrls.push(response.data)
    imageFileList.value = fileList.map((item, index) => ({
      name: item.name,
      url: response.data
    }))
    ElMessage.success('作品图片上传成功')
  } else {
    ElMessage.error('作品图片上传失败')
  }
}

// 封面图片上传前检查
const beforeCoverUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isImage) {
    ElMessage.error('封面图片只能是 JPG/PNG/JPEG 格式!')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('封面图片大小不能超过 10MB!')
    return false
  }
  return true
}

// 作品图片上传前检查
const beforeImageUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isImage) {
    ElMessage.error('作品图片只能是 JPG/PNG/JPEG 格式!')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('作品图片大小不能超过 10MB!')
    return false
  }
  return true
}

// 表单验证规则
const portfolioRules = {
  title: [
    { required: true, message: '请输入作品标题', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入作品描述', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择作品分类', trigger: 'change' }
  ]
}

// 获取摄影师作品列表
const fetchPortfolios = async () => {
  if (!userStore.user?.id) return

  try {
    loading.value = true
    const photographer = await api.get(`/photo/photographer/byUser/${userStore.user.id}`)

    if (!photographer.data) {
      ElMessage.error('获取作品列表失败')
      return
    }
    const res = await api.get(`/portfolio/photographer/${photographer.data.id}`, {
      params: {
        page: currentPage.value,
        size: pageSize.value,
        ...filters
      }
    })

    portfolios.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('获取作品列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchPortfolios()
}

// 重置筛选
const resetFilters = () => {
  Object.assign(filters, {
    category: '',
    status: null,
    isFeatured: null
  })
  handleSearch()
}

// 分页处理
const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  fetchPortfolios()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  fetchPortfolios()
}

// 编辑作品
const editPortfolio = (portfolio: Portfolio) => {
  editingPortfolio.value = portfolio
  // 解析imageUrls JSON字符串
  let parsedImageUrls = [];
  try {
    parsedImageUrls = JSON.parse(portfolio.imageUrls || '[]');
  } catch (e) {
    parsedImageUrls = [];
  }

  Object.assign(portfolioForm, {
    title: portfolio.title,
    description: portfolio.description,
    category: portfolio.category,
    tags: portfolio.tags,
    coverImage: portfolio.coverImage,
    imageUrls: parsedImageUrls,
    shootingDate: portfolio.shootingDate,
    shootingLocation: portfolio.shootingLocation,
    equipment: portfolio.equipment,
    sortWeight: portfolio.sortWeight
  })

  // 更新文件列表显示
  imageFileList.value = parsedImageUrls.map((url: string, index: number) => ({
    name: `image_${index + 1}`,
    url: url
  }))

  showAddDialog.value = true
}

// 批量上传文件到COS
const uploadFilesToCOS = async (): Promise<{ coverUrl: string; imageUrls: string[] }> => {
  console.log('开始上传文件到COS...')
  console.log('封面文件:', coverFile.value)
  console.log('作品文件数量:', pendingFiles.value.length)

  const imageUrls: string[] = []
  let coverUrl = ''

  try {
    // 上传封面图片
    if (coverFile.value) {
      console.log('正在上传封面图片...')
      const formData = new FormData()
      formData.append('file', coverFile.value)

      const response = await fetch('/file/upload', {
        method: 'POST',
        body: formData
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const result = await response.json()
      console.log('封面上传响应:', result)

      if (result.code === 1) {  // 注意：根据你的后端返回码调整
        coverUrl = result.data
        console.log('封面上传成功:', coverUrl)
      } else {
        throw new Error(result.msg || '封面上传失败')
      }
    }

    // 上传作品图片
    for (let i = 0; i < pendingFiles.value.length; i++) {
      console.log(`正在上传第${i + 1}张作品图片...`)
      const formData = new FormData()
      formData.append('file', pendingFiles.value[i])


      const response = await fetch('/file/upload', {
        method: 'POST',
        body: formData
      })

      console.log('作品图片上传响应:', response);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const result = await response.json()
      console.log('作品图片上传响应:', result)
      if (result.code === 1) {
        imageUrls.push(result.data)
        console.log(`第${i + 1}张作品图片上传成功:`, result.data)
      } else {
        throw new Error(result.msg || '作品图片上传失败')
      }

    }

    console.log('所有文件上传完成:', { coverUrl, imageUrls })
    return { coverUrl, imageUrls }

  } catch (error) {
    console.error('文件上传过程中出错:', error)
    throw error
  }
}

// 保存作品时处理imageUrls
const savePortfolio = async () => {
  await portfolioFormRef.value.validate()
  saving.value = true

  try {
    // 批量上传文件到COS
    const { coverUrl, imageUrls } = await uploadFilesToCOS()

    // 获取摄影师ID
    const photographerRes = await api.get(`/photo/photographer/byUser/${userStore.user?.id}`)
    const photographerId = photographerRes.data?.id

    if (!photographerId) {
      throw new Error('您还不是摄影师，无法管理作品')
    }
    console.log('摄影师ID:', photographerId)

    // 准备保存数据
    const data = {
      ...portfolioForm,
      coverImage: coverUrl || portfolioForm.coverImage,
      imageUrls: JSON.stringify(imageUrls.length > 0 ? imageUrls : portfolioForm.imageUrls.map(f => f instanceof File ? null : f).filter(Boolean)),
      shootingDate: portfolioForm.shootingDate ? dayjs(portfolioForm.shootingDate).format('YYYY-MM-DD') : null,
      photographerId
    }

    console.log('准备保存的数据:', data)

    // 保存到数据库
    if (editingPortfolio.value) {
      await api.put(`/portfolio/${editingPortfolio.value.id}`, data)
      ElMessage.success('作品更新成功')
    } else {
      await api.post('/portfolio', data)
      ElMessage.success('作品添加成功')
    }

    console.log('作品保存成功')
    showAddDialog.value = false
    resetForm()
    fetchPortfolios()

  } catch (error) {
    console.error('保存作品失败:', error)
    ElMessage.error(error.message || '保存作品失败')
  } finally {
    saving.value = false
  }
}

// 删除作品
const deletePortfolio = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个作品吗？此操作不可恢复。', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await api.delete(`/portfolio/${id}`)
    //仅在数据库删除
    ElMessage.success('删除成功')
    fetchPortfolios()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除作品失败:', error)
      ElMessage.error('删除作品失败')
    }
  }
}

// 切换精选状态
const toggleFeatured = async (portfolio: Portfolio) => {
  try {
    await api.put(`/portfolio/${portfolio.id}/featured?isFeatured=${!portfolio.isFeatured}`)
    portfolio.isFeatured = !portfolio.isFeatured
    ElMessage.success(portfolio.isFeatured ? '设为精选成功' : '取消精选成功')
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  }
}

// 切换发布状态
const toggleStatus = async (portfolio: Portfolio) => {
  try {
    const newStatus = portfolio.status === 1 ? 2 : 1
    await api.put(`/portfolio/${portfolio.id}/status?status=${newStatus}`)
    portfolio.status = newStatus
    ElMessage.success(newStatus === 1 ? '发布成功' : '下架成功')
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  }
}

// 重置表单
const resetForm = () => {
  editingPortfolio.value = null
  Object.assign(portfolioForm, {
    title: '',
    description: '',
    category: '',
    tags: '',
    coverImage: '',
    imageUrls: [],
    shootingDate: '',
    shootingLocation: '',
    equipment: '',
    sortWeight: 0
  })
  imageFileList.value = []
  pendingFiles.value = []
  coverFile.value = null
  portfolioFormRef.value?.resetFields()
}

// 获取状态类型
const getStatusType = (status: number) => {
  switch (status) {
    case 0: return 'info'
    case 1: return 'success'
    case 2: return 'warning'
    default: return 'info'
  }
}

// 获取状态文本
const getStatusText = (status: number) => {
  switch (status) {
    case 0: return '草稿'
    case 1: return '已发布'
    case 2: return '已下架'
    default: return '未知'
  }
}

onMounted(() => {
  fetchPortfolios()
})
</script>

<style scoped>
.photographer-portfolio {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-header h1 {
  margin: 0;
  color: #333;
}

.filter-card {
  margin-bottom: 20px;
}

.portfolio-list {
  margin-bottom: 40px;
}

.loading-container {
  padding: 40px;
}

.portfolio-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.portfolio-card {
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  transition: box-shadow 0.3s ease;
}

.portfolio-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.portfolio-image {
  position: relative;
  height: 200px;
}

.portfolio-image img {
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
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.portfolio-card:hover .portfolio-overlay {
  opacity: 1;
}

.portfolio-actions {
  display: flex;
  gap: 10px;
}

.portfolio-status {
  position: absolute;
  top: 10px;
  right: 10px;
  display: flex;
  gap: 5px;
}

.portfolio-info {
  padding: 15px;
}

.portfolio-info h4 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 1.1rem;
}

.description {
  color: #666;
  line-height: 1.4;
  margin-bottom: 15px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.stats {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8rem;
  color: #666;
}

.stats .el-icon {
  margin-right: 2px;
}

.portfolio-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 15px;
}

.tag-item {
  margin: 0;
}

.portfolio-actions-bottom {
  display: flex;
  gap: 10px;
}

.portfolio-actions-bottom .el-button {
  flex: 1;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}





.image-uploader :deep(.el-upload--picture-card) {
  width: 120px;
  height: 120px;
}
.cover-uploader:deep(.el-upload--picture-card) {
  width: 120px;
  height: 120px;
}

.image-uploader :deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 120px;
  height: 120px;
}


</style>