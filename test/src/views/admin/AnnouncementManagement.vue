<template>
  <div class="announcement-management">
    <div class="main-card">
      <h2 style="color: #222; font-size: 20px; font-weight: bold;">公告管理</h2>
      <div style="margin-bottom: 16px;">
        <el-button type="primary" @click="showAddDialog = true">新增公告</el-button>
      </div>
      <el-table :data="announcements" v-loading="loading" class="announcement-table" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" width="200" />
        <el-table-column prop="content" label="内容" width="400" />
        <el-table-column prop="time" label="发布时间" width="160" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="edit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <!-- 新增/编辑弹窗 -->
      <el-dialog v-model="showAddDialog" title="新增公告" width="400px">
        <el-form :model="form" label-width="60px">
          <el-form-item label="标题">
            <el-input v-model="form.title" maxlength="50" show-word-limit />
          </el-form-item>
          <el-form-item label="内容">
            <el-input v-model="form.content" type="textarea" rows="4" maxlength="500" show-word-limit />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showAddDialog = false">取消</el-button>
          <el-button type="primary" @click="add">确定</el-button>
        </template>
      </el-dialog>
      <el-dialog v-model="showEditDialog" title="编辑公告" width="400px">
        <el-form :model="form" label-width="60px">
          <el-form-item label="标题">
            <el-input v-model="form.title" maxlength="50" show-word-limit />
          </el-form-item>
          <el-form-item label="内容">
            <el-input v-model="form.content" type="textarea" rows="4" maxlength="500" show-word-limit />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showEditDialog = false">取消</el-button>
          <el-button type="primary" @click="update">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const announcements = ref<any[]>([])
const loading = ref(false)
const showAddDialog = ref(false)
const showEditDialog = ref(false)
const form = ref({ id: null, title: '', content: '' })

const fetchAnnouncements = async () => {
  loading.value = true
  const res = await api.get('/announcement/list')
  announcements.value = res.data || []
  loading.value = false
}

const add = async () => {
  if (!form.value.title || !form.value.content) {
    ElMessage.warning('标题和内容不能为空')
    return
  }
  await api.post('/announcement/add', { title: form.value.title, content: form.value.content })
  ElMessage.success('新增成功')
  showAddDialog.value = false
  form.value = { id: null, title: '', content: '' }
  fetchAnnouncements()
}

const edit = (row: any) => {
  form.value = { ...row }
  showEditDialog.value = true
}

const update = async () => {
  if (!form.value.title || !form.value.content) {
    ElMessage.warning('标题和内容不能为空')
    return
  }
  await api.put(`/announcement/${form.value.id}`, { title: form.value.title, content: form.value.content })
  ElMessage.success('修改成功')
  showEditDialog.value = false
  form.value = { id: null, title: '', content: '' }
  fetchAnnouncements()
}

const remove = async (id: number) => {
  await ElMessageBox.confirm('确定要删除该公告吗？', '提示', { type: 'warning' })
  await api.delete(`/announcement/${id}`)
  ElMessage.success('删除成功')
  fetchAnnouncements()
}

onMounted(() => {
  fetchAnnouncements()
})
</script>

<style scoped>
.announcement-management {
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
.announcement-table {
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