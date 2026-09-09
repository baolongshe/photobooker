<template>
  <div class="ai-chat">
    <!-- 任务进度条 -->
    <div v-if="activeTask" class="task-progress">
      <div class="progress-header">
        <span>📋 {{ activeTask.currentStepName }}</span>
        <span>{{ activeTask.progress }}%</span>
      </div>
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: activeTask.progress + '%' }"></div>
      </div>
    </div>

    <div class="chat-history">
      <div v-for="msg in messages" :key="msg.id" :class="msg.role">
        <span>{{ msg.role === 'user' ? '我' : '小影' }}：</span>{{ msg.content }}
      </div>
    </div>
    <div class="chat-input">
      <input v-model="input" @keyup.enter="send" placeholder="请输入问题..." />
      <button @click="send" :disabled="sending">{{ sending ? '发送中...' : '发送' }}</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { v4 as uuidv4 } from 'uuid'

const sessionId = localStorage.getItem('ai_session_id') || uuidv4()
localStorage.setItem('ai_session_id', sessionId)

const messages = ref([])
const input = ref('')
const sending = ref(false)
const activeTask = ref(null)

onMounted(() => {
  checkTaskStatus()
})

const send = async () => {
  if (!input.value.trim() || sending.value) return
  
  sending.value = true
  messages.value.push({ id: Date.now(), role: 'user', content: input.value })
  const userMsg = input.value
  input.value = ''
  
  try {
    // 获取token并添加到请求头
    const token = localStorage.getItem('token')
    const headers = { 'Content-Type': 'text/plain' }
    if (token) {
      headers['authentication'] = token
    }
    
    const res = await fetch('/ai/stream-chat?sessionId=' + sessionId, {
      method: 'POST',
      headers: headers,
      body: userMsg
    })
    
    if (!res.ok) {
      throw new Error(`HTTP error! status: ${res.status}`)
    }
    
    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let aiReply = ''
    
    // 添加AI消息占位符
    const aiMessageId = Date.now() + 1
    messages.value.push({ 
      id: aiMessageId, 
      role: 'assistant', 
      content: '' 
    })
    
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      
      const chunk = decoder.decode(value, { stream: true })
      aiReply += chunk
      
      // 实时更新AI回复内容
      const aiMessage = messages.value.find(msg => msg.id === aiMessageId)
      if (aiMessage) {
        aiMessage.content = aiReply
      }
    }

    // 检查任务状态更新
    await checkTaskStatus()
  } catch (error) {
    console.error('流式响应错误:', error)
    messages.value.push({ 
      id: Date.now() + 2, 
      role: 'assistant', 
      content: '抱歉，服务暂时不可用，请稍后再试。' 
    })
  } finally {
    sending.value = false
  }
}

const checkTaskStatus = async () => {
  try {
    // 获取token并添加到请求头
    const token = localStorage.getItem('token')
    const headers = {}
    if (token) {
      headers['authentication'] = token
    }
    
    const res = await fetch('/ai/task/status?sessionId=' + sessionId, {
      headers: headers
    })
    const data = await res.json()
    
    if (data.success && data.task) {
      activeTask.value = {
        taskId: data.task.taskId,
        progress: data.progress || 0,
        currentStepName: data.currentStepName || '',
        status: data.task.status
      }
    } else {
      activeTask.value = null
    }
  } catch (error) {
    console.error('获取任务状态失败:', error)
  }
}
</script>

<style scoped>
.ai-chat { 
  width: 350px; 
  border: 1px solid #eee; 
  border-radius: 8px; 
  padding: 12px; 
  background: #fff; 
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.task-progress {
  margin-bottom: 12px;
  padding: 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 6px;
  color: white;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 500;
}

.progress-bar {
  height: 6px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: white;
  transition: width 0.3s ease;
  border-radius: 3px;
}

.chat-history { 
  max-height: 300px; 
  overflow-y: auto; 
  margin-bottom: 8px; 
  padding: 8px;
}

.user { 
  text-align: right; 
  color: #409EFF; 
  margin-bottom: 8px;
}

.assistant { 
  text-align: left; 
  color: #67C23A; 
  margin-bottom: 8px;
  background: #f5f5f5;
  padding: 8px;
  border-radius: 4px;
}

.chat-input { 
  display: flex; 
  gap: 8px; 
}

input { 
  flex: 1; 
  padding: 8px; 
  border-radius: 4px; 
  border: 1px solid #ccc; 
  outline: none;
}

input:focus {
  border-color: #409EFF;
}

button { 
  padding: 8px 16px; 
  background: #409EFF;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s;
}

button:hover:not(:disabled) {
  background: #337ecc;
}

button:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style> 