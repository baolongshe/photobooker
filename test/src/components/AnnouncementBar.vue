<template>
  <el-alert
    v-if="announcement"
    :title="announcement.title"
    type="info"
    show-icon
    :closable="false"
    style="margin-bottom: 12px; cursor: pointer;"
    @click="showDetail"
  />
  <el-dialog v-model="dialogVisible" title="公告详情" width="400px">
    <div v-if="announcement">
      <h3>{{ announcement.title }}</h3>
      <div style="white-space: pre-line;">{{ announcement.content }}</div>
      <div style="color: #888; font-size: 13px; margin-top: 12px;">发布时间：{{ formatTime(announcement.time) }}</div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/utils/api'
import dayjs from 'dayjs'

const announcement = ref<any>(null)
const dialogVisible = ref(false)

const showDetail = () => {
  dialogVisible.value = true
}
const formatTime = (t: string) => t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '-'

const token = localStorage.getItem('token')
if (token) {
  const fetchLatest = async () => {
    const res = await api.get('/announcement/latest')
    announcement.value = res.data
  }
  onMounted(fetchLatest)
}
</script>

<style scoped>
.el-alert {
  font-size: 16px;
  font-weight: 500;
}
</style> 