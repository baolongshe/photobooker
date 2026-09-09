<template>
    <div class="package-page">
      <h2>我的服务套餐</h2>
      <el-button type="primary" @click="showAddDialog = true" style="margin-bottom: 16px;">新增套餐</el-button>
      <el-table :data="packageList" style="width: 100%;">
        <el-table-column prop="name" label="套餐名称" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="price" label="价格" />
        <el-table-column prop="isDefault" label="默认">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault === 1" type="success">默认</el-tag>
            <el-button v-else size="small" @click="setDefault(row)">设为默认</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button size="small" @click="editPackage(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deletePackage(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
  
      <!-- 新增/编辑套餐弹窗 -->
      <el-dialog v-model="showAddDialog" :title="editingPackage ? '编辑套餐' : '新增套餐'">
        <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
          <el-form-item label="名称" prop="name">
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="form.description" />
          </el-form-item>
          <el-form-item label="价格" prop="price">
            <el-input v-model="form.price" type="number" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showAddDialog = false">取消</el-button>
          <el-button type="primary" @click="savePackage">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </template>
  
  <script setup lang="ts">
  import { ref, reactive, onMounted } from 'vue'
  import { ElMessage } from 'element-plus'
  import api from '../utils/api'
  import { useUserStore } from '../stores/user'
  
  const userStore = useUserStore()
  const packageList = ref<any[]>([])
  const showAddDialog = ref(false)
  const editingPackage = ref<any>(null)
  const formRef = ref()
  const form = reactive({
    name: '',
    description: '',
    price: '',
  })
  const rules = {
    name: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
    price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
  }
  
  const fetchPackages = async () => {
    // 获取当前摄影师ID
    const res = await api.get(`/photo/photographer/byUser/${userStore.user.id}`)
    const photographer = res.data
    if (!photographer) {
      ElMessage.error('您还不是摄影师，无法管理套餐')
      return
    }
    const pkgRes = await api.get(`/photo/photographer/package/list/${photographer.id}`)
    packageList.value = pkgRes.data || []
  }
  
  const savePackage = async () => {
    await formRef.value.validate()
    // 获取摄影师ID
    const res = await api.get(`/photo/photographer/byUser/${userStore.user.id}`)
    const photographer = res.data
    if (!photographer) {
      ElMessage.error('您还不是摄影师，无法管理套餐')
      return
    }
    const payload = {
      ...form,
      price: Number(form.price),
      photographerId: photographer.id
    }
    if (editingPackage.value) {
      payload.id = editingPackage.value.id
      await api.put('/photo/photographer/package/update', payload)
      ElMessage.success('套餐更新成功')
    } else {
      await api.post('/photo/photographer/package/add', payload)
      ElMessage.success('套餐添加成功')
    }
    showAddDialog.value = false
    editingPackage.value = null
    fetchPackages()
  }
  
  const editPackage = (row: any) => {
    editingPackage.value = row
    Object.assign(form, {
      name: row.name,
      description: row.description,
      price: row.price
    })
    showAddDialog.value = true
  }
  
  const deletePackage = async (row: any) => {
    // 获取摄影师ID
    const res = await api.get(`/photo/photographer/byUser/${userStore.user.id}`)
    const photographer = res.data
    if (!photographer) {
      ElMessage.error('您还不是摄影师，无法管理套餐')
      return
    }
    await api.delete(`/photo/photographer/package/delete/${row.id}/${photographer.id}`)
    ElMessage.success('删除成功')
    fetchPackages()
  }
  
  const setDefault = async (row: any) => {
    // 获取摄影师ID
    const res = await api.get(`/photo/photographer/byUser/${userStore.user.id}`)
    const photographer = res.data
    if (!photographer) {
      ElMessage.error('您还不是摄影师，无法管理套餐')
      return
    }
    await api.post(`/photo/photographer/package/set-default/${row.id}/${photographer.id}`)
    ElMessage.success('设置为默认套餐成功')
    fetchPackages()
  }
  
  onMounted(() => {
    fetchPackages()
  })
  </script>
  
  <style scoped>
  .package-page {
    max-width: 800px;
    margin: 0 auto;
    padding: 32px 0;
  }
  </style>