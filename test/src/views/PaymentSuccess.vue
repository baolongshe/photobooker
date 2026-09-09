<template>
  <div class="payment-success-page">
    <div class="success-container">
      <div class="success-icon">✓</div>
      <h1>支付成功！</h1>
      <p class="message">
        您的订单已支付成功，我们会尽快为您安排拍摄服务。<br>
        如有任何问题，请联系客服。
      </p>
      
      <div class="order-info">
        <div class="info-item">
          <label>订单号：</label>
          <span>{{ orderInfo.orderId }}</span>
        </div>
        <div class="info-item">
          <label>支付金额：</label>
          <span class="amount">¥{{ orderInfo.totalAmount }}</span>
        </div>
        <div class="info-item">
          <label>支付时间：</label>
          <span>{{ orderInfo.payTime }}</span>
        </div>
      </div>
      
      <div class="buttons">
        <button class="btn btn-primary" @click="viewOrder">查看订单</button>
        <button class="btn btn-secondary" @click="goHome">返回首页</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const orderInfo = ref({
  orderId: '',
  totalAmount: '',
  payTime: ''
})

// 查看订单
const viewOrder = () => {
  if (orderInfo.value.orderId) {
    router.push(`/order/${orderInfo.value.orderId}`)
  } else {
    router.push('/orders')
  }
}

// 返回首页
const goHome = () => {
  router.push('/')
}

// 格式化时间
const formatDateTime = (date: Date) => {
  return date.getFullYear() + '-' + 
         String(date.getMonth() + 1).padStart(2, '0') + '-' + 
         String(date.getDate()).padStart(2, '0') + ' ' + 
         String(date.getHours()).padStart(2, '0') + ':' + 
         String(date.getMinutes()).padStart(2, '0') + ':' + 
         String(date.getSeconds()).padStart(2, '0')
}

// 页面加载时处理URL参数
onMounted(() => {
  const outTradeNo = route.query.out_trade_no as string
  const totalAmount = route.query.total_amount as string
  
  if (outTradeNo) {
    orderInfo.value.orderId = outTradeNo
  }
  
  if (totalAmount) {
    orderInfo.value.totalAmount = totalAmount
  }
  
  // 显示当前时间作为支付时间
  orderInfo.value.payTime = formatDateTime(new Date())
  
  // 显示成功消息
  ElMessage.success('支付成功！')
})
</script>

<style scoped>
.payment-success-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.success-container {
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.1);
  padding: 40px;
  width: 90%;
  max-width: 500px;
  text-align: center;
}

.success-icon {
  width: 80px;
  height: 80px;
  background: #52c41a;
  border-radius: 50%;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 40px;
  animation: bounce 0.6s ease-in-out;
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-10px);
  }
  60% {
    transform: translateY(-5px);
  }
}

h1 {
  color: #333;
  margin-bottom: 20px;
  font-size: 28px;
}

.message {
  color: #666;
  margin-bottom: 30px;
  font-size: 16px;
  line-height: 1.6;
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
}

.info-item label {
  font-weight: 500;
  color: #555;
}

.info-item .amount {
  font-weight: bold;
  color: #52c41a;
  font-size: 1.1em;
}

.buttons {
  display: flex;
  gap: 15px;
  justify-content: center;
}

.btn {
  padding: 12px 24px;
  border-radius: 25px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border: none;
}

.btn-primary {
  background: linear-gradient(45deg, #1677ff, #4096ff);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(22, 119, 255, 0.3);
}

.btn-secondary {
  background: white;
  color: #1677ff;
  border: 2px solid #1677ff;
}

.btn-secondary:hover {
  background: #1677ff;
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(22, 119, 255, 0.3);
}
</style> 