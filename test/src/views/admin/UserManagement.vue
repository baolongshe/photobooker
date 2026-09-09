<template>
  <div class="user-management">
    <div class="main-card">
      <h2>用户管理</h2>
      <el-row :gutter="20" class="stats-row">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-title">总用户数</div>
            <div class="stat-value">{{ userStats.total }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-title">管理员数</div>
            <div class="stat-value">{{ userStats.admins }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-title">普通用户数</div>
            <div class="stat-value">{{ userStats.users }}</div>
          </el-card>
        </el-col>
      </el-row>
      <el-table :data="users" v-loading="loading" class="user-table" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="头像" prop="avatar" width="80">
          <template #default="{ row }">
            <img
              v-if="row.avatar"
              :src="row.avatar"
              alt="头像"
              style="width: 40px; height: 40px; border-radius: 50%; object-fit: cover;"
            />
            <img
              v-else
              src="https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200&h=200&fit=crop&crop=face"
              alt="默认头像"
              style="width: 40px; height: 40px; border-radius: 50%; object-fit: cover;"
            />
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="gender" label="性别" width="80">
          <template #default="{ row }">
            {{ row.gender === 1 ? '男' : row.gender === 0 ? '女' : '其他' }}
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            {{ row.role === 1 ? '管理员' : '普通用户' }}
          </template>
        </el-table-column>
        <el-table-column prop="registerTime" label="注册时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.registerTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            <el-button size="small" @click="handleResetPwd(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
      <!-- 编辑弹窗 -->
      <el-dialog v-model="editDialogVisible" title="编辑用户" width="400px">
        <el-form :model="editForm" label-width="80px" :rules="editRules" ref="editFormRef">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="editForm.username" disabled />
          </el-form-item>
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="editForm.realName" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="editForm.phone" />
          </el-form-item>
          <el-form-item label="性别" prop="gender">
            <el-select v-model="editForm.gender" style="width: 100%">
              <el-option label="女" :value="0" />
              <el-option label="男" :value="1" />
              <el-option label="其他" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="角色" prop="role">
            <el-select v-model="editForm.role" style="width: 100%">
              <el-option label="普通用户" :value="0" />
              <el-option label="管理员" :value="1" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveEdit" :loading="saveLoading">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/utils/api'
import type { FormInstance, FormRules } from 'element-plus'

const users = ref<any[]>([])
const loading = ref(false)
const editDialogVisible = ref(false)
const saveLoading = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive({
  id: null,
  username: '',
  realName: '',
  phone: '',
  gender: 0,
  role: 0
})
const editRules: FormRules = {
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}
const userStats = ref({ total: 0, admins: 0, users: 0 })

const fetchUsers = async () => {
  loading.value = true
  const res = await api.get('/photo/user/list')
  users.value = res.data || []
  userStats.value.total = users.value.length
  userStats.value.admins = users.value.filter((u: any) => u.role === 1).length
  userStats.value.users = users.value.filter((u: any) => u.role === 0).length
  loading.value = false
}

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString()
}

const handleEdit = (row: any) => {
  Object.assign(editForm, row)
  editDialogVisible.value = true
}
const saveEdit = async () => {
  if (!editFormRef.value) return
  await editFormRef.value.validate()
  saveLoading.value = true
  const { id, username, realName, phone, gender, role } = editForm
  await api.post('/photo/user/update', [{ id, username, realName, phone, gender, role }])
  ElMessage.success('保存成功')
  editDialogVisible.value = false
  saveLoading.value = false
  fetchUsers()
}
const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await api.post('/photo/user/delete', [row.id])
    ElMessage.success('删除成功')
    fetchUsers()
  })
}
const handleResetPwd = (row: any) => {
  ElMessageBox.confirm('确定要重置该用户密码为123456吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await api.post('/photo/user/update', [{ id: row.id, password: '123456' }])
    ElMessage.success('密码已重置为123456')
  })
}
onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.user-management {
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
h2 {
  color: #222;
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 24px;
}
.stats-row {
  margin-top: 0;
  margin-bottom: 24px;
}
.stat-card {
  border-radius: 10px;
  box-shadow: 0 2px 12px #f0f1f2;
  text-align: center;
  padding: 16px 0;
}
.stat-title {
  color: #666;
  font-size: 15px;
  margin-bottom: 8px;
  font-weight: 500;
}
.stat-value {
  color: #409eff;
  font-size: 28px;
  font-weight: bold;
}
.user-table {
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