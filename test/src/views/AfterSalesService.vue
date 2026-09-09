
<template>
  <div class="after-sales-service">
    <div class="page-header">
      <el-button @click="goBack" icon="ArrowLeft">返回订单详情</el-button>
      <h1>售后服务</h1>
    </div>

    <el-card class="service-card">
      <!-- 订单信息概览 -->
      <div class="order-summary">
        <h3>订单信息</h3>
        <div class="summary-content">
          <div class="info-row">
            <span class="label">订单号：</span>
            <span class="value">{{ order?.id }}</span>
          </div>
          <div class="info-row">
            <span class="label">订单状态：</span>
            <el-tag :type="getStatusType(order?.status)">
              {{ getStatusText(order?.status) }}
            </el-tag>
          </div>
          <div class="info-row">
            <span class="label">拍摄套餐：</span>
            <span class="value">{{ order?.packageName }}</span>
          </div>
          <div class="info-row">
            <span class="label">拍摄时间：</span>
            <span class="value">{{ formatDateTime(order?.shootingTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 售后服务选项 -->
      <div class="service-options">
        <h3>请选择售后服务类型</h3>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card
                class="service-item"
                :class="{ active: selectedService === 'refine' }"
                @click="selectService('refine')"
            >
              <div class="service-icon">📸</div>
              <h4>精修照片</h4>
              <p>对已交付的照片提出精修要求</p>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card
                class="service-item"
                :class="{ active: selectedService === 'reschedule' }"
                @click="selectService('reschedule')"
            >
              <div class="service-icon"></div>
              <h4>重新预约</h4>
              <p>因天气等原因需要重新安排拍摄时间</p>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card
                class="service-item"
                :class="{ active: selectedService === 'refund' }"
                @click="selectService('refund')"
            >
              <div class="service-icon">💰</div>
              <h4>退款申请</h4>
              <p>符合退款条件的订单可申请退款</p>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card
                class="service-item"
                :class="{ active: selectedService === 'complaint' }"
                @click="selectService('complaint')"
            >
              <div class="service-icon">️</div>
              <h4>投诉建议</h4>
              <p>对服务不满意或有改进建议</p>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card
                class="service-item"
                :class="{ active: selectedService === 'additional' }"
                @click="selectService('additional')"
            >
              <div class="service-icon">➕</div>
              <h4>加选服务</h4>
              <p>额外购买精修照片、相册等产品</p>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card
                class="service-item"
                :class="{ active: selectedService === 'other' }"
                @click="selectService('other')"
            >
              <div class="service-icon">💬</div>
              <h4>其他问题</h4>
              <p>其他需要帮助的问题</p>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 服务申请表单 -->
      <div v-if="selectedService" class="service-form">
        <h3>申请详情</h3>
        <el-form :model="serviceForm" label-width="100px" :rules="formRules" ref="formRef">
          <!-- 精修照片 -->
          <div v-if="selectedService === 'refine'">
            <el-form-item label="照片编号" prop="photoNumbers">
              <el-input
                  v-model="serviceForm.photoNumbers"
                  placeholder="请输入需要精修的照片编号，多个用逗号分隔"
              />
            </el-form-item>
            <el-form-item label="精修要求" prop="description">
              <el-input
                  v-model="serviceForm.description"
                  type="textarea"
                  :rows="4"
                  placeholder="请描述您的精修要求，如：调色、磨皮、瘦身等..."
              />
            </el-form-item>
          </div>

          <!-- 重新预约 -->
          <div v-else-if="selectedService === 'reschedule'">
            <el-form-item label="期望时间" prop="expectedTime">
              <el-date-picker
                  v-model="serviceForm.expectedTime"
                  type="datetime"
                  placeholder="选择期望的拍摄时间"
                  style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="改期原因" prop="description">
              <el-input
                  v-model="serviceForm.description"
                  type="textarea"
                  :rows="4"
                  placeholder="请说明需要改期的原因..."
              />
            </el-form-item>
          </div>

          <!-- 退款申请 -->
          <div v-else-if="selectedService === 'refund'">
            <el-form-item label="退款原因" prop="refundReason">
              <el-select v-model="serviceForm.refundReason" placeholder="请选择退款原因" style="width: 100%">
                <el-option label="拍摄前取消" value="cancel_before_shoot" />
                <el-option label="摄影师原因" value="photographer_reason" />
                <el-option label="服务质量问题" value="quality_issue" />
                <el-option label="其他原因" value="other" />
              </el-select>
            </el-form-item>
            <el-form-item label="退款说明" prop="description">
              <el-input
                  v-model="serviceForm.description"
                  type="textarea"
                  :rows="4"
                  placeholder="请详细说明退款原因..."
              />
            </el-form-item>
            <el-form-item label="上传凭证" prop="evidence">
              <el-upload
                  action="#"
                  list-type="picture-card"
                  :auto-upload="false"
                  :on-change="handleFileChange"
                  :limit="5"
              >
                <el-icon><Plus /></el-icon>
              </el-upload>
              <div class="form-tip">可上传相关凭证照片，最多 5 张</div>
            </el-form-item>
          </div>

          <!-- 投诉建议 -->
          <div v-else-if="selectedService === 'complaint'">
            <el-form-item label="投诉类型" prop="complaintType">
              <el-select v-model="serviceForm.complaintType" placeholder="请选择投诉类型" style="width: 100%">
                <el-option label="服务态度" value="attitude" />
                <el-option label="服务质量" value="quality" />
                <el-option label="时间延误" value="delay" />
                <el-option label="其他问题" value="other" />
              </el-select>
            </el-form-item>
            <el-form-item label="投诉内容" prop="description">
              <el-input
                  v-model="serviceForm.description"
                  type="textarea"
                  :rows="6"
                  placeholder="请详细描述您的问题或建议..."
              />
            </el-form-item>
          </div>

          <!-- 加选服务 -->
          <div v-else-if="selectedService === 'additional'">
            <el-form-item label="加选项目" prop="additionalItems">
              <div style="display: flex; flex-direction: column; gap: 10px;">
                <el-checkbox
                  v-model="serviceForm.additionalItems.extraRefine"
                  label="额外精修（¥50/张）"
                />
                <el-checkbox
                  v-model="serviceForm.additionalItems.photoAlbum"
                  label="定制相册（¥200/本）"
                />
                <el-checkbox
                  v-model="serviceForm.additionalItems.photoFrame"
                  label="精美相框（¥150/个）"
                />
                <el-checkbox
                  v-model="serviceForm.additionalItems.allPhotos"
                  label="全底片赠送（¥100）"
                />
              </div>
            </el-form-item>
            <el-form-item label="精修数量" prop="refineCount" v-if="serviceForm.additionalItems.extraRefine">
              <el-input-number
                v-model="serviceForm.additionalItems.refineCount"
                :min="1"
                :max="50"
                placeholder="请输入精修数量"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="备注说明" prop="description">
              <el-input
                  v-model="serviceForm.description"
                  type="textarea"
                  :rows="3"
                  placeholder="如有其他要求请在此说明..."
              />
            </el-form-item>
          </div>

          <!-- 其他问题 -->
          <div v-else-if="selectedService === 'other'">
            <el-form-item label="问题类型" prop="issueType">
              <el-input
                  v-model="serviceForm.issueType"
                  placeholder="请简要描述问题类型"
              />
            </el-form-item>
            <el-form-item label="详细描述" prop="description">
              <el-input
                  v-model="serviceForm.description"
                  type="textarea"
                  :rows="6"
                  placeholder="请详细描述您遇到的问题..."
              />
            </el-form-item>
          </div>

          <!-- 联系方式 -->
          <el-form-item label="联系电话" prop="contactPhone">
            <el-input
                v-model="serviceForm.contactPhone"
                placeholder="请输入您的联系电话"
                maxlength="11"
            />
          </el-form-item>

          <!-- 提交按钮 -->
          <el-form-item>
            <el-button type="primary" @click="submitService" :loading="submitLoading">
              提交申请
            </el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 未选择服务时的提示 -->
      <div v-else class="service-prompt">
        <el-empty description="请从上方选择您需要的售后服务类型" />
      </div>

      <!-- 历史记录 -->
      <div class="service-history">
        <h3>我的售后记录</h3>
        <el-table :data="serviceRecords" style="width: 100%">
          <el-table-column prop="serviceType" label="服务类型" width="120">
            <template #default="{ row }">
              {{ getServiceTypeName(row.serviceType || row.type) }}
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" />
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button type="primary" link @click="viewRecord(row)">
                查看
              </el-button>
              <el-button
                v-if="row.status === 'PENDING'"
                type="danger"
                link
                @click="cancelService(row.id)"
              >
                撤销申请
              </el-button>
              <el-button
                v-else-if="row.status === 'IN_PROGRESS'"
                type="warning"
                link
                @click="expediteService(row.id)"
              >
                催办
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import api from '../utils/api'
import { Plus } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

interface Order {
  id: number;
  status: string;
  packageName: string;
  shootingTime: string;
  shootingLocation: string;
  totalPrice: number;
  createTime: string;
}

interface ServiceRecord {
  id: number;
  orderId: number;
  type: string;
  status: string;
  description: string;
  createTime: string;
}

const order = ref<Order | null>(null)
const selectedService = ref('')
const submitLoading = ref(false)
const formRef = ref()

const serviceForm = reactive({
  photoNumbers: '',
  expectedTime: '',
  refundReason: '',
  complaintType: '',
  additionalItems: {
    extraRefine: false,
    photoAlbum: false,
    photoFrame: false,
    allPhotos: false,
    refineCount: 0
  },
  issueType: '',
  description: '',
  contactPhone: ''
})

const formRules = {
  description: [
    { required: true, message: '请填写详细描述', trigger: 'blur' }
  ],
  contactPhone: [
    { required: true, message: '请填写联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const serviceRecords = ref<ServiceRecord[]>([
  // 示例数据，实际应从后端获取
  {
    id: 1,
    orderId: 123,
    type: 'refine',
    status: 'COMPLETED',
    description: '精修 3 张照片，要求美白磨皮',
    createTime: '2024-01-10 14:30:00'
  },
  {
    id: 2,
    orderId: 123,
    type: 'reschedule',
    status: 'PENDING',
    description: '因天气原因申请改期',
    createTime: '2024-02-15 09:20:00'
  }
])

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
    // 同时获取该订单的售后记录
    fetchServiceRecords(orderId)
  } catch (error) {
    console.error('获取订单信息失败:', error)
    ElMessage.error('获取订单信息失败')
  }
}
// 获取售后记录
const fetchServiceRecords = async (orderId: number) => {
  try {
    const res = await api.get(`/after-sales/order/${orderId}`)
    serviceRecords.value = res.data || []
  } catch (error) {
    console.error('获取售后记录失败:', error)
  }
}


// 选择服务类型
const selectService = (service: string) => {
  console.log('选择服务类型:', service)
  selectedService.value = service

  // 滚动到表单区域
  setTimeout(() => {
    const formElement = document.querySelector('.service-form')
    if (formElement) {
      formElement.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }, 100)
}

// 文件上传处理
const handleFileChange = (file: any) => {
  console.log('文件选择:', file)
}

// 提交申请
const submitService = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    if (!userStore.user) {
      ElMessage.error('请先登录')
      router.push({ name: 'login' })
      return
    }

    try {
      submitLoading.value = true

      const submitData: any = {
        orderId: order.value?.id,
        userId: userStore.user.id,
        photographerId: order.value?.photographerId,
        serviceType: selectedService.value,
        description: serviceForm.description,
        contactPhone: serviceForm.contactPhone
      }

      // 根据不同的服务类型传递不同的数据
      if (selectedService.value === 'refine') {
        submitData.photoNumbers = serviceForm.photoNumbers
        submitData.refineRequirements = serviceForm.description
      } else if (selectedService.value === 'reschedule') {
        submitData.expectedTime = serviceForm.expectedTime
        submitData.rescheduleReason = serviceForm.description
      } else if (selectedService.value === 'refund') {
        submitData.refundReason = serviceForm.refundReason
        submitData.refundAmount = order.value?.totalPrice || 0
        submitData.description = serviceForm.description
      } else if (selectedService.value === 'complaint') {
        submitData.complaintType = serviceForm.complaintType
        submitData.description = serviceForm.description
      } else if (selectedService.value === 'additional') {
        // 直接传递 AdditionalItems 对象
        submitData.additionalItems = serviceForm.additionalItems
        submitData.description = serviceForm.description
      } else if (selectedService.value === 'other') {
        submitData.issueType = serviceForm.issueType
        submitData.description = serviceForm.description
      }

      // 调用后端 API 提交售后申请
      await api.post('/after-sales', submitData)

      ElMessage.success('申请提交成功，我们会尽快处理')

      // 刷新售后记录
      fetchServiceRecords(order.value!.id)
      resetForm()

    } catch (error) {
      console.error('提交申请失败:', error)
      ElMessage.error('提交申请失败，请重试')
    } finally {
      submitLoading.value = false
    }
  })
}

// 重置表单
const resetForm = () => {
  selectedService.value = ''
  serviceForm.photoNumbers = ''
  serviceForm.expectedTime = ''
  serviceForm.refundReason = ''
  serviceForm.complaintType = ''
  serviceForm.additionalItems = {
    extraRefine: false,
    photoAlbum: false,
    photoFrame: false,
    allPhotos: false,
    refineCount: 0
  }
  serviceForm.issueType = ''
  serviceForm.description = ''
  serviceForm.contactPhone = ''
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

// 获取服务类型名称
const getServiceTypeName = (type: string) => {
  const typeMap: Record<string, string> = {
    'refine': '精修照片',
    'reschedule': '重新预约',
    'refund': '退款申请',
    'complaint': '投诉建议',
    'additional': '加选服务',
    'other': '其他问题'
  }
  return typeMap[type] || type
}

// 获取状态文本
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

const getStatusText = (status?: string) => {
  if (!status) return '未知'
  const statusMap: Record<string, string> = {
    'PENDING': '待处理',
    'CONFIRMED': '已确认',
    'IN_PROGRESS': '处理中',
    'COMPLETED': '已完成',
    'CANCELED': '已取消',
    'FAILED': '已失败'
  }
  return statusMap[status] || '未知'
}

const getStatusTagType = (status?: string) => {
  return getStatusType(status)
}

// 格式化日期时间
const formatDateTime = (dateTime?: string) => {
  if (!dateTime) return ''
  return dayjs(dateTime).format('YYYY-MM-DD HH:mm')
}

// 返回
const goBack = () => {
  router.go(-1)
}

// 查看记录详情
const viewRecord = (record: ServiceRecord) => {
  console.log('查看售后记录:', record)
  const serviceType = record.serviceType || record.type
  ElMessageBox.alert(
      `
    <div style="text-align: left;">
      <p><strong>服务类型：</strong>${getServiceTypeName(serviceType || '')}</p>
      <p><strong>申请时间：</strong>${formatDateTime(record.createTime)}</p>
      <p><strong>状态：</strong>${getStatusText(record.status)}</p>
      <p><strong>描述：</strong>${record.description || ''}</p>
    </div>
    `,
      '售后记录详情',
      {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '关闭'
      }
  )
}

// 撤销申请
const cancelService = async (recordId: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要撤销这个售后申请吗？撤销后无法恢复。',
      '确认撤销',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 调用后端 API 撤销申请
    await api.post(`/after-sales/${recordId}/cancel`)

    ElMessage.success('申请已撤销')
    // 刷新售后记录
    if (order.value?.id) {
      fetchServiceRecords(order.value.id)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('撤销申请失败:', error)
      ElMessage.error('撤销申请失败')
    }
  }
}

// 催办
const expediteService = async (recordId: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要催办这个售后申请吗？我们将尽快处理。',
      '确认催办',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'question'
      }
    )

    // 调用后端 API 发送催办通知（这里只是提示，实际应该调用催办接口）
    ElMessage.success('催办成功，我们会尽快处理您的申请')
    // TODO: 如果有催办接口，可以在这里调用
    // await api.post(`/after-sales/${recordId}/expedite`)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('催办失败:', error)
      ElMessage.error('催办失败')
    }
  }
}

onMounted(() => {
  fetchOrderInfo()
})
</script>

<style scoped>
.after-sales-service {
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

.service-card {
  margin-bottom: 20px;
}

.order-summary {
  margin-bottom: 30px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.order-summary h3 {
  margin: 0 0 15px 0;
  color: #333;
}

.summary-content {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.info-row {
  display: flex;
  align-items: center;
}

.info-row .label {
  width: 100px;
  color: #666;
  font-weight: bold;
}

.info-row .value {
  color: #333;
}

.service-options {
  margin-bottom: 30px;
}

.service-options h3 {
  margin: 0 0 20px 0;
  color: #333;
}

.service-item {
  cursor: pointer;
  transition: all 0.3s;
  text-align: center;
  height: 180px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border: 2px solid transparent;
}

.service-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.service-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.service-icon {
  font-size: 3rem;
  margin-bottom: 15px;
}

.service-item h4 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 1.1rem;
}

.service-item p {
  margin: 0;
  color: #666;
  font-size: 0.9rem;
  line-height: 1.5;
}

.service-form {
  margin-top: 30px;
  padding: 20px;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #fafafa;
}

.service-form h3 {
  margin: 0 0 20px 0;
  color: #333;
}

.form-tip {
  font-size: 0.85rem;
  color: #999;
  margin-top: 5px;
}

.service-prompt {
  padding: 40px 0;
}

.service-history {
  margin-top: 40px;
  border-top: 1px solid #eee;
  padding-top: 30px;
}

.service-history h3 {
  margin: 0 0 20px 0;
  color: #333;
}

@media (max-width: 768px) {
  .summary-content {
    grid-template-columns: 1fr;
  }

  .service-item {
    height: auto;
    padding: 20px;
  }
}
</style>
