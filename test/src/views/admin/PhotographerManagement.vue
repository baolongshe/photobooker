<template>
  <div class="photographer-management">
    <div class="main-card">
      <h2 style="color: #222; font-size: 20px; font-weight: bold;">摄影师管理</h2>
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
        <el-input v-model="searchKeyword" placeholder="搜索姓名/电话/风格" style="width: 260px;" clearable @input="handleSearch" />
        <el-button type="primary" @click="fetchPhotographers">刷新</el-button>
      </div>
     
      <el-table :data="filteredPhotographers" v-loading="loading" class="photographer-table" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="style" label="擅长风格" width="120" />
        <el-table-column prop="intro" label="简介" width="150" />
        <el-table-column prop="workYears" label="工作年限" width="100" />
        <el-table-column prop="authStatus" label="认证状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getAuthStatusType(row.authStatus)">{{ getAuthStatusText(row.authStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderCount" label="完成订单数" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="editPhotographer(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deletePhotographer(row.id)">删除</el-button>
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <!-- 编辑弹窗、详情弹窗等可后续补充 -->
    </div>
    <el-dialog v-model="editDialogVisible" title="编辑摄影师" width="400px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="editForm.name" disabled />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="editForm.phone" />
        </el-form-item>
        <el-form-item label="风格">
          <el-input v-model="editForm.style" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="editForm.intro" />
        </el-form-item>
        <el-form-item label="工作年限">
          <el-input v-model="editForm.workYears" type="number" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit" :loading="saveLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'

const photographers = ref<any[]>([])
const filteredPhotographers = ref<any[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const router = useRouter()
const editDialogVisible = ref(false)
const editForm = ref<any>({})
const saveLoading = ref(false)

const fetchPhotographers = async () => {
  loading.value = true
  const res = await api.get('/photo/photographer/list')
  photographers.value = res.data || []
  filteredPhotographers.value = [...photographers.value]
  loading.value = false
}

const handleSearch = () => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) {
    filteredPhotographers.value = [...photographers.value]
    return
  }
  filteredPhotographers.value = photographers.value.filter((p: any) =>
    (p.name && p.name.toLowerCase().includes(keyword)) ||
    (p.phone && p.phone.includes(keyword)) ||
    (p.style && p.style.toLowerCase().includes(keyword))
  )
}

const getAuthStatusText = (status: number) => {
  switch (status) {
    case 0: return '未认证'
    case 1: return '已认证'
    case 2: return '认证失败'
    default: return '未知'
  }
}
const getAuthStatusType = (status: number) => {
  switch (status) {
    case 0: return 'info'
    case 1: return 'success'
    case 2: return 'danger'
    default: return 'info'
  }
}

const editPhotographer = (row: any) => {
  editForm.value = { ...row }//拷贝当行数据
  editDialogVisible.value = true
}

const saveEdit = async () => {
  saveLoading.value = true
  await api.post('/photo/photographer/update', editForm.value)
  ElMessage.success('保存成功')
  editDialogVisible.value = false
  saveLoading.value = false
  fetchPhotographers()
}

const deletePhotographer = async (id: number) => {
  await ElMessageBox.confirm('确定要删除该摄影师吗？', '提示', { type: 'warning' })
  await api.post('/photo/photographer/deleteBatch', [id])
  ElMessage.success('删除成功')
  fetchPhotographers()
}
const viewDetail = (row: any) => {
  router.push({ name: 'photographer-detail', params: { id: row.id } })
}

onMounted(() => {
  fetchPhotographers()
})
</script>

<style scoped>
.photographer-management {
  padding: 0;
  background: #fff;
  min-height: 100vh;
}
.main-card {
  background: #fff;
  border-radius: 0;
  box-shadow: none;
  padding: 24px 32px 24px 32px;
  width: 100%;
  max-width: 100%;
  margin: 0;
}
.photographer-table {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 12px #f0f1f2;
  font-size: 15px;
}
.el-table th {
  background: #f7f8fa;
  color: #333;
  font-weight: 600;
}
.el-table .el-table__row:hover {
  background: #f0f7ff;
}
</style> 