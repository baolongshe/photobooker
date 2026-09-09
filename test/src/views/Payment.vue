<template>
  <div class="payment-page">
    <div class="payment-container">
      <div class="logo">📸</div>
      <h1>支付宝支付</h1>
      
      <div class="order-info">
        <div class="info-item">
          <label>订单号：</label>
          <span>{{ orderInfo.orderId }}</span>
        </div>
        <div class="info-item">
          <label>套餐名称：</label>
          <span>{{ orderInfo.packageName }}</span>
        </div>
        <div class="info-item">
          <label>支付金额：</label>
          <span class="amount">¥{{ orderInfo.totalAmount }}</span>
        </div>
      </div>
      
      <div class="payment-form">
        <button 
          class="pay-button" 
          @click="createPayment" 
          :disabled="payLoading"
        >
          <span v-if="payLoading" class="loading"></span>
          {{ payLoading ? '正在创建支付...' : '立即支付' }}
        </button>
      </div>
      
      <div v-if="status.show" :class="['status', status.type]">
        {{ status.message }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../utils/api'
import { getApiUrl } from '../config/api'

const route = useRoute()
const router = useRouter()

const orderInfo = ref({
  orderId: '',
  packageName: '',
  totalAmount: 0
})

const payLoading = ref(false)
const status = ref({
  show: false,
  type: 'success',
  message: ''
})

// 显示状态信息
const showStatus = (type: 'success' | 'error', message: string) => {
  status.value = {
    show: true,
    type,
    message
  }
}

// 创建支付
const createPayment = async () => {
  if (!orderInfo.value.orderId || !orderInfo.value.totalAmount) {
    ElMessage.error('订单信息不完整')
    return
  }
  
  payLoading.value = true
  status.value.show = false
  
  try {
    // 调用后端支付接口
    const formData = new FormData()
    formData.append('orderId', orderInfo.value.orderId)
    formData.append('totalAmount', orderInfo.value.totalAmount.toString())
    
    console.log('发送支付请求，参数:', {
      orderId: orderInfo.value.orderId,
      totalAmount: orderInfo.value.totalAmount
    })
    
    // 使用fetch直接调用后端接口
    const response = await fetch(getApiUrl('/pay/create'), {
      method: 'POST',
      body: formData
    })
    
    console.log('支付响应状态:', response.status)
    
    if (response.ok) {
      const result = await response.text()
      console.log('支付表单内容:', result)
      showStatus('success', '支付表单生成成功！')
      
      // 将支付表单插入到页面中
      const paymentContainer = document.querySelector('.payment-container')
      const formDiv = document.createElement('div')
      formDiv.innerHTML = result
      formDiv.style.display = 'none'
      paymentContainer?.appendChild(formDiv)
      
      // 自动提交表单，跳转到支付宝支付页面
      setTimeout(() => {
        const form = formDiv.querySelector('form')
        if (form) {
          form.submit()
        }
      }, 1000)
      
    } else {
      const errorText = await response.text()
      console.error('支付创建失败，错误信息:', errorText)
      throw new Error('支付创建失败: ' + errorText)
    }
    
  } catch (error) {
    console.error('支付错误:', error)
    showStatus('error', '支付失败：' + (error as Error).message)
  } finally {
    payLoading.value = false
  }
}

// 页面加载时处理URL参数
onMounted(() => {
  const orderId = route.query.orderId as string
  const totalAmount = route.query.totalAmount as string
  const packageName = route.query.packageName as string
  
  if (orderId && totalAmount && packageName) {
    orderInfo.value = {
      orderId,
      packageName: decodeURIComponent(packageName),
      totalAmount: parseFloat(totalAmount)
    }
  } else {
    ElMessage.error('订单信息不完整')
    router.push('/orders')
  }
})
</script>

<style scoped>
.payment-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.payment-container {
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.1);
  padding: 40px;
  width: 90%;
  max-width: 500px;
  text-align: center;
}

.logo {
  width: 80px;
  height: 80px;
  background: #1677ff;
  border-radius: 50%;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  font-weight: bold;
}

h1 {
  color: #333;
  margin-bottom: 30px;
  font-size: 28px;
}

.order-info {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 10px;
  margin-bottom: 30px;
  text-align: left;
}

.info-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  padding: 5px 0;
}

.info-item:last-child {
  margin-bottom: 0;
  border-top: 1px solid #e9ecef;
  padding-top: 15px;
  margin-top: 15px;
}

.info-item label {
  font-weight: 500;
  color: #555;
}

.info-item .amount {
  font-size: 1.2em;
  font-weight: bold;
  color: #1677ff;
}

.pay-button {
  background: linear-gradient(45deg, #1677ff, #4096ff);
  color: white;
  border: none;
  padding: 15px 40px;
  border-radius: 25px;
  font-size: 18px;
  font-weight: bold;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  width: 100%;
  margin-top: 20px;
}

.pay-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(22, 119, 255, 0.3);
}

.pay-button:disabled {
  background: #ccc;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.status {
  margin-top: 20px;
  padding: 15px;
  border-radius: 10px;
}

.status.success {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  color: #52c41a;
}

.status.error {
  background: #fff2f0;
  border: 1px solid #ffccc7;
  color: #ff4d4f;
}

.loading {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #1677ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 10px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style> 