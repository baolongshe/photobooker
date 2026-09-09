<template>
  <div class="application-management">
    <div class="main-card">
      <h2 style="color: #222; font-size: 20px; font-weight: bold;">申请审核</h2>
      <el-table :data="applications" v-loading="loading" class="application-table" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="intro" label="简介" width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="审核备注" width="150" />
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column prop="updateTime" label="审核时间" width="160" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="approve(row)" :disabled="row.status!=='PENDING'">通过</el-button>
            <el-button size="small" type="danger" @click="reject(row)" :disabled="row.status!=='PENDING'">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const applications = ref<any[]>([])
const loading = ref(false)

const fetchApplications = async () => {
  loading.value = true
  const res = await api.get('/application/list')
  applications.value = res.data || []
  loading.value = false
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'APPROVED': return '已通过'
    case 'REJECTED': return '已拒绝'
    case 'PENDING': return '待审核'
    default: return status
  }
}
const getStatusType = (status: string) => {
  switch (status) {
    case 'APPROVED': return 'success'
    case 'REJECTED': return 'danger'
    case 'PENDING': return 'info'
    default: return 'info'
  }
}

const approve = async (row: any) => {
  const { value: remark } = await ElMessageBox.prompt('请输入审核备注（可选）', '通过申请', { inputValue: '' })
  await api.put(`/application/${row.id}/review`, null, {
    params: { status: 'APPROVED', remark }
  })
  ElMessage.success('已通过')
  fetchApplications()
}
const reject = async (row: any) => {
  const { value: remark } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝申请', { inputValue: '', inputPattern: /.+/, inputErrorMessage: '请填写拒绝原因' })
  await api.put(`/application/${row.id}/review`, null, {
    params: { status: 'REJECTED', remark }
  })
  ElMessage.success('已拒绝')
  fetchApplications()
}

onMounted(() => {
  fetchApplications()
})
</script>

<style scoped>
.application-management {
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
.application-table {
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