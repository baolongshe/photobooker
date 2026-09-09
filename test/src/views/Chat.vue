<template>
  <div class="chat-page">
    <div class="chat-container">
      <!-- 聊天头部 -->
      <div class="chat-header">
        <el-button @click="$router.go(-1)" icon="ArrowLeft">返回</el-button>
        <div class="chat-title">
          <h3>与 {{ chatTargetName }} 的对话</h3>
          <p>订单号：{{ orderId }}</p>
        </div>
      </div>

      <!-- 聊天消息区域 -->
      <div class="chat-messages" ref="messagesContainer">
        <div
            v-for="msg in messages"
            :key="msg.id"
            class="message"
            :class="{ 'message-own': msg.senderId === userStore.user?.id }"
        >
          <div class="message-avatar">
            <img
                :src="msg.senderId === userStore.user?.id ? userStore.user?.avatar : chatTargetAvatar"
                :alt="msg.senderName"
            />
          </div>
          <div class="message-content">
            <div class="message-header">
              <span class="message-sender">{{ msg.senderName }}</span>
              <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
            </div>
            <div class="message-text">
              <div v-if="msg.type === 'text' || msg.type === 'chat'">{{ msg.content }}</div>
              <img v-else-if="msg.type === 'image'" :src="msg.fileUrl" style="max-width:200px;" />
              <a v-else-if="msg.type === 'file'" :href="msg.fileUrl" target="_blank">下载文件</a>
              <div v-else>{{ msg.content }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 聊天输入区域 -->
      <div class="chat-input">
        <el-input
            v-model="newMessage"
            type="textarea"
            :rows="3"
            placeholder="输入消息..."
            @keydown.enter.prevent="sendMessage"
        />
        <el-button
            type="primary"
            @click="sendMessage"
            :loading="sending"
            :disabled="!newMessage.trim()"
        >
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import api from '../utils/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const orderId = route.params.orderId
const messagesContainer = ref<HTMLElement | null>(null)
const newMessage = ref('')
const sending = ref(false)

const chatTargetName = ref('')
const chatTargetAvatar = ref('')
const chatTargetId = ref('')
const chatTargetRole = ref('')

const messages = ref<any[]>([])

let ws: WebSocket | null = null
let isManualClose = false
let heartbeatTimer: number | null = null
let heartbeatInterval = 30000 // 30秒发送一次心跳

const formatTime = (timestamp: string) => {
  return dayjs(timestamp).format('HH:mm')
}

// 启动心跳
const startHeartbeat = () => {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
  }
  heartbeatTimer = setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      const heartbeatMsg = {
        type: 'ping',
        timestamp: Date.now()
      }
      ws.send(JSON.stringify(heartbeatMsg))
      console.log('发送心跳消息')
    }
  }, heartbeatInterval)
}

// 停止心跳
const stopHeartbeat = () => {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

const sendMessage = async () => {
  if (!newMessage.value.trim()) return
  try {
    sending.value = true
    // 直接用已缓存的chatTargetId/chatTargetRole
    if (!chatTargetId.value || !chatTargetRole.value) {
      console.log('sendMessage 前 chatTargetId:', chatTargetId.value, typeof chatTargetId.value)
      console.log('sendMessage 前 chatTargetRole:', chatTargetRole.value, typeof chatTargetRole.value)
      ElMessage.error('聊天对象信息未加载，请稍后重试')
      return
    }

    // 构建基于用户的会话ID：chat_{minUserId}_{maxUserId}
    // 这样无论有多少订单，同一对用户的聊天记录都在同一个会话中
    const currentUserId = Number(userStore.user?.id)
    const otherUserId = Number(chatTargetId.value)
    const minUserId = Math.min(currentUserId, otherUserId)
    const maxUserId = Math.max(currentUserId, otherUserId)
    const sessionId = `chat_${minUserId}_${maxUserId}`

    const msg = {
      type: 'chat',
      fromUserId: userStore.user?.id,
      toUserId: Number(chatTargetId.value),
      orderId: Number(orderId),
      sessionId: sessionId, // 使用基于用户的稳定会话ID
      content: newMessage.value,
      fileUrl: '',
      senderRole: 'user',
      receiverRole: chatTargetRole.value
    }
    ws?.send(JSON.stringify(msg))
    // 本地先显示
    messages.value.push({
      id: Date.now(),
      senderId: userStore.user?.id,
      senderName: userStore.user?.username || '用户',
      content: newMessage.value,
      type: 'chat', // 保持与发送给后端的类型一致
      fileUrl: '',
      timestamp: dayjs().format('YYYY-MM-DD HH:mm:ss')
    })
    newMessage.value = ''
    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送失败，请重试')
  } finally {
    sending.value = false
  }
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

onMounted(async () => {
  console.log('[Chat.vue] 进入聊天页时 token:', localStorage.getItem('token'));
  // 建立 WebSocket 连接，确保用 localStorage 里的 token
  const wsToken = localStorage.getItem('token');
  // 修改后
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${protocol}//${window.location.host}/ws?token=${wsToken}`;


  //修改前console.log('[Chat.vue] WebSocket 用的 token:', wsToken);
  // 修改前const wsUrl = `ws://localhost:8099/ws?token=${wsToken}`;
  ws = new WebSocket(wsUrl)

  ws.onopen = () => {
    console.log('WebSocket 连接已建立')
    startHeartbeat() // 启动心跳
  }

  ws.onmessage = (event) => {
    const data = JSON.parse(event.data)
    console.log('收到WebSocket消息:', data)

    if (data.type === 'ping') {
      // 收到服务器心跳，回复pong
      const pongMsg = {
        type: 'pong',
        timestamp: Date.now()
      }
      ws?.send(JSON.stringify(pongMsg))
      console.log('收到服务器心跳，回复pong')
      return
    } else if (data.type === 'pong') {
      // 收到pong响应
      console.log('收到pong响应')
      return
    } else if (data.isSystem) {
      // 系统消息（如用户上线/下线），可选处理
      return
    }

    // 处理转发的消息
    const newMsg = {
      id: Date.now(),
      senderId: chatTargetId.value, // 转发消息的发送者是聊天对象
      senderName: data.fromName || '对方',
      content: data.message || data.content || '',
      type: data.type || 'text',
      fileUrl: data.fileUrl || '',
      timestamp: data.timestamp || dayjs().format('YYYY-MM-DD HH:mm:ss')
    }
    console.log('添加新消息到列表:', newMsg)
    messages.value.push(newMsg)
    nextTick(scrollToBottom)
  }

  ws.onclose = (event) => {
    console.log('WebSocket 连接关闭', event)
    stopHeartbeat() // 停止心跳
    if (!isManualClose && event.code !== 1000) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      userStore.logout && userStore.logout()
      ElMessage.error('登录已过期，请重新登录')
      router.push({ name: 'login' })
    }
  }

  ws.onerror = (err) => {
    console.error('WebSocket 错误', err)
    stopHeartbeat() // 停止心跳
    if (!isManualClose) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      userStore.logout && userStore.logout()
      ElMessage.error('聊天服务连接失败，请重新登录')
      router.push({ name: 'login' })
    }
  }

  // 拉取订单详情，动态设置聊天对象
  try {
    const res = await api.get(`/orders/${orderId}`)
    console.log('订单详情接口返回: ', res)
    const order = res.data.order
    const photographer = res.data.photographer
    const user = res.data.user
    console.log('order:', order, 'photographer:', photographer, 'user:', user)

    const currentUserId = Number(userStore.user?.id)
    let otherUserId = 0

    if (currentUserId === Number(order.userId)) {
      // 当前用户是下单用户，对方是摄影师
      chatTargetName.value = photographer?.name || '摄影师'
      chatTargetAvatar.value = photographer?.avatar || ''
      chatTargetId.value = photographer?.userId || ''
      chatTargetRole.value = 'photographer'
      otherUserId = Number(photographer?.userId || 0)
      console.log('赋值后 chatTargetId:', chatTargetId.value, typeof chatTargetId.value)
    } else {
      // 当前用户是摄影师，对方是下单用户
      chatTargetName.value = user?.username || '用户'
      chatTargetAvatar.value = user?.avatar || ''
      chatTargetId.value = user?.id || ''
      chatTargetRole.value = 'user'
      otherUserId = Number(user?.id || 0)
      console.log('赋值后 chatTargetId:', chatTargetId.value, typeof chatTargetId.value)
    }

    // 构建基于用户的会话ID：chat_{minUserId}_{maxUserId}
    // 这样无论有多少订单，同一对用户的聊天记录都在同一个会话中
    const minUserId = Math.min(currentUserId, otherUserId)
    const maxUserId = Math.max(currentUserId, otherUserId)
    const sessionId = `chat_${minUserId}_${maxUserId}`
    console.log('生成的会话ID:', sessionId)

    // 拉取历史消息（按会话ID查询）
    try {
      const historyRes = await api.get('/chat/history', {
        params: {
          sessionId: sessionId,
          page: 1,
          size: 200 // 获取更多历史记录
        }
      })
      console.log('历史消息数据:', historyRes.data)

      // 转换数据库格式为前端期望的格式
      const convertedMessages = (historyRes.data || []).map((msg: any) => ({
        id: msg.id,
        senderId: msg.senderId,
        senderName: msg.senderId === currentUserId ? (userStore.user?.username || '用户') : chatTargetName.value,
        content: msg.content,
        type: msg.type,
        fileUrl: msg.fileUrl,
        timestamp: msg.createTime
      }))
      console.log('转换后的历史消息:', convertedMessages)
      messages.value = convertedMessages
    } catch (e) {
      console.error('拉取历史消息失败:', e)
    }
  } catch (e) {
    console.error('catch 拉取订单详情失败:', e)
    chatTargetName.value = '对方'
    chatTargetAvatar.value = ''
    chatTargetId.value = ''
    chatTargetRole.value = ''
  }

  nextTick(() => {
    scrollToBottom()
  })
})

onUnmounted(() => {
  isManualClose = true
  stopHeartbeat() // 停止心跳
  if (ws) {
    ws.close()
    ws = null
  }
})
</script>

<style scoped>
.chat-page {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.chat-container {
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  border-bottom: 1px solid #eee;
  background: #f9f9f9;
  border-radius: 8px 8px 0 0;
}

.chat-title h3 {
  margin: 0 0 5px 0;
  color: #333;
}

.chat-title p {
  margin: 0;
  color: #666;
  font-size: 0.9rem;
}

.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #f5f5f5;
}

.message {
  display: flex;
  margin-bottom: 20px;
  gap: 10px;
}

.message-own {
  flex-direction: row-reverse;
}

.message-avatar img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.message-content {
  max-width: 70%;
}

.message-own .message-content {
  text-align: right;
}

.message-header {
  margin-bottom: 5px;
}

.message-sender {
  font-weight: bold;
  color: #333;
  font-size: 0.9rem;
}

.message-time {
  color: #999;
  font-size: 0.8rem;
  margin-left: 10px;
}

.message-text {
  background: white;
  padding: 12px 16px;
  border-radius: 18px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  word-wrap: break-word;
  line-height: 1.4;
  color: #222;
}

.message-own .message-text {
  background: #409eff;
  color: white;
}

.chat-input {
  padding: 20px;
  border-top: 1px solid #eee;
  background: white;
  border-radius: 0 0 8px 8px;
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.chat-input .el-textarea {
  flex: 1;
}

.chat-input .el-button {
  height: 40px;
  padding: 0 20px;
}
</style> 