<template>
  <div class="dopa-chat">
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <div class="logo-blob"><span class="logo-emoji">📸</span></div>
        <h2 class="logo-title">小影 AI</h2>
        <p class="logo-sub">你的智能摄影助手 ✨</p>
      </div>
      <div class="quick-section">
        <h4 class="quick-title">💡 试试这些</h4>
        <div
          v-for="(action, idx) in quickActions" :key="idx"
          class="quick-btn" :style="{ '--delay': idx * 0.06 + 's' }"
          @click="sendQuick(action.text)"
        >
          <span class="quick-icon">{{ action.icon }}</span>
          <span class="quick-label">{{ action.label }}</span>
        </div>
      </div>
      <div class="sidebar-bottom">
        <div class="tech-pill"><span class="pill-dot pulse-pink"></span>ReAct Agent</div>
        <div class="tech-pill"><span class="pill-dot pulse-green"></span>Spring AI</div>
      </div>
    </div>
    <div class="chat-main">
      <!-- 任务进度条 -->
      <div v-if="activeTask" class="task-progress-bar">
        <div class="progress-info">
          <span>📋 {{ activeTask.currentStepName }}</span>
          <span>{{ activeTask.progress }}%</span>
        </div>
        <div class="progress-track">
          <div class="progress-fill" :style="{ width: activeTask.progress + '%' }"></div>
        </div>
      </div>
      <div class="chat-topbar">
        <div class="topbar-left">
          <div class="ai-avatar-blob"><span>🤖</span></div>
          <div>
            <h3 class="ai-name">小影</h3>
            <span class="ai-status"><span class="status-dot"></span>在线 · 随时为你服务</span>
          </div>
        </div>
        <button class="clear-btn" @click="clearChat" title="清空记录">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
        </button>
      </div>
      <div class="chat-body" ref="messagesContainer">
        <div v-if="messages.length === 0" class="welcome">
          <div class="welcome-blob"><span class="welcome-emoji">👋</span></div>
          <h2>Hey，我是小影！</h2>
          <p class="welcome-desc">摄影约拍平台 AI 客服，有什么可以帮你的？</p>
          <div class="cap-grid">
            <div class="cap-card" v-for="(c, i) in capabilities" :key="i" :style="{ '--card-hue': c.hue }">
              <span class="cap-emoji">{{ c.icon }}</span>
              <span class="cap-text">{{ c.text }}</span>
            </div>
          </div>
        </div>
        <div v-for="msg in messages" :key="msg.id" :class="['bubble-row', msg.role]">
          <div class="bubble-avatar">
            <span v-if="msg.role === 'assistant'">🤖</span>
            <span v-else>😊</span>
          </div>
          <div class="bubble-content">
            <div class="bubble-name">{{ msg.role === 'assistant' ? '小影' : '我' }}</div>
            <div :class="['bubble', msg.role]"><span v-html="formatContent(msg.content)"></span></div>
            <div class="bubble-time">{{ msg.time }}</div>
          </div>
        </div>
        <div v-if="sending" class="bubble-row assistant">
          <div class="bubble-avatar"><span>🤖</span></div>
          <div class="bubble-content">
            <div class="bubble-name">小影</div>
            <div class="bubble assistant thinking"><div class="dots"><span></span><span></span><span></span></div></div>
          </div>
        </div>
      </div>
      <div class="chat-footer">
        <div class="input-box">
          <textarea v-model="input" @keydown.enter.exact.prevent="send" placeholder="问我任何问题，按 Enter 发送 ✨" rows="1" ref="inputRef"></textarea>
          <button class="send-btn" @click="send" :disabled="sending || !input.trim()">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/></svg>
          </button>
        </div>
        <p class="footer-hint">🔒 基于实时数据回答 · 不会编造信息</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { v4 as uuidv4 } from 'uuid'
import dayjs from 'dayjs'

const sessionId = localStorage.getItem('ai_session_id') || uuidv4()
localStorage.setItem('ai_session_id', sessionId)

const messages = ref<{ id: number; role: string; content: string; time: string }[]>([])
const input = ref('')
const sending = ref(false)
const activeTask = ref(null)
const messagesContainer = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)

const quickActions = [
  { icon: '🔍', label: '搜索摄影师', text: '帮我推荐一位擅长婚纱风格的摄影师' },
  { icon: '📦', label: '查看套餐', text: '有哪些摄影套餐可以选择？' },
  { icon: '📍', label: '附近摄影师', text: '帮我找一下附近的摄影师' },
  { icon: '📅', label: '预约拍摄', text: '我想预约一次拍摄' },
  { icon: '💰', label: '价格查询', text: '摄影套餐的价格范围是多少？' },
  { icon: '🎨', label: '作品集推荐', text: '有没有优秀的作品集推荐？' }
]
const capabilities = [
  { icon: '🔍', text: '搜索摄影师', hue: 330 },
  { icon: '📦', text: '查询套餐', hue: 260 },
  { icon: '📅', text: '预约档期', hue: 170 },
  { icon: '📍', text: '附近找摄影师', hue: 30 },
  { icon: '📝', text: '创建订单', hue: 50 },
  { icon: '💬', text: '摄影问答', hue: 290 }
]

const formatContent = (content: string) =>
  content.replace(/\n/g, '<br>').replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>').replace(/`(.*?)`/g, '<code>$1</code>')
const getTime = () => dayjs().format('HH:mm')
const scrollToBottom = () => { nextTick(() => { if (messagesContainer.value) messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight }) }
const sendQuick = (text: string) => { input.value = text; send() }

const send = async () => {
  if (!input.value.trim() || sending.value) return
  sending.value = true
  messages.value.push({ id: Date.now(), role: 'user', content: input.value, time: getTime() })
  const userMsg = input.value
  input.value = ''
  scrollToBottom()
  try {
    const token = localStorage.getItem('token')
    const headers: Record<string, string> = { 'Content-Type': 'text/plain' }
    if (token) headers['authentication'] = token
    const res = await fetch('/ai/stream-chat?sessionId=' + sessionId, { method: 'POST', headers, body: userMsg })
    if (!res.ok) throw new Error('HTTP ' + res.status)
    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let aiReply = ''
    const aiId = Date.now() + 1
    messages.value.push({ id: aiId, role: 'assistant', content: '', time: getTime() })
    scrollToBottom()
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      aiReply += decoder.decode(value, { stream: true })
      const m = messages.value.find(msg => msg.id === aiId)
      if (m) { m.content = aiReply; scrollToBottom() }
    }

    // 检查任务状态更新
    await checkTaskStatus()
  } catch (e) {
    console.error('流式响应错误:', e)
    messages.value.push({ id: Date.now() + 2, role: 'assistant', content: '抱歉，服务暂时不可用，请稍后再试 😢', time: getTime() })
  } finally { sending.value = false }
}

const clearChat = () => {
  messages.value = []
  localStorage.removeItem('ai_session_id')
  localStorage.setItem('ai_session_id', uuidv4())
  window.location.reload()
}
onMounted(() => {
  checkTaskStatus()
  inputRef.value?.focus()
})

const checkTaskStatus = async () => {
  try {
    const token = localStorage.getItem('token')
    const headers: Record<string, string> = {}
    if (token) headers['authentication'] = token
    
    const res = await fetch('/ai/task/status?sessionId=' + sessionId, { headers })
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
@keyframes float{0%,100%{transform:translateY(0)}50%{transform:translateY(-8px)}}
@keyframes pop-in{0%{transform:scale(.6);opacity:0}70%{transform:scale(1.08)}100%{transform:scale(1);opacity:1}}
@keyframes slide-up{from{transform:translateY(20px);opacity:0}to{transform:translateY(0);opacity:1}}
@keyframes dot-bounce{0%,80%,100%{transform:scale(.5)}40%{transform:scale(1)}}
@keyframes gradient-shift{0%{background-position:0% 50%}50%{background-position:100% 50%}100%{background-position:0% 50%}}
.dopa-chat{display:flex;height:calc(100vh - 60px);max-width:1400px;margin:16px auto;border-radius:24px;overflow:hidden;box-shadow:0 12px 48px rgba(255,100,150,.15),0 4px 16px rgba(100,100,255,.08)}
.chat-sidebar{width:280px;background:linear-gradient(160deg,#ff6b9d 0%,#c44dff 40%,#6c5ce7 70%,#4facfe 100%);background-size:200% 200%;animation:gradient-shift 8s ease infinite;color:#fff;display:flex;flex-direction:column;padding:28px 20px;position:relative;overflow:hidden}
.chat-sidebar::before{content:'';position:absolute;width:200px;height:200px;background:rgba(255,255,255,.06);border-radius:50%;top:-60px;right:-60px}
.sidebar-header{text-align:center;margin-bottom:28px;position:relative;z-index:1}
.logo-blob{width:64px;height:64px;background:rgba(255,255,255,.2);backdrop-filter:blur(10px);border-radius:20px;display:flex;align-items:center;justify-content:center;margin:0 auto 12px;animation:float 3s ease-in-out infinite}
.logo-emoji{font-size:32px}.logo-title{font-size:1.5rem;font-weight:800;margin:0}.logo-sub{font-size:.82rem;opacity:.8;margin:4px 0 0}
.quick-section{flex:1;position:relative;z-index:1}.quick-title{font-size:.72rem;text-transform:uppercase;letter-spacing:1.5px;opacity:.6;margin-bottom:12px}
.quick-btn{display:flex;align-items:center;gap:10px;padding:10px 14px;border-radius:14px;cursor:pointer;transition:all .25s cubic-bezier(.34,1.56,.64,1);margin-bottom:6px;font-size:.88rem;background:rgba(255,255,255,.08);animation:slide-up .4s ease both;animation-delay:var(--delay)}
.quick-btn:hover{background:rgba(255,255,255,.22);transform:translateX(6px) scale(1.02)}.quick-icon{font-size:1.15rem}
.sidebar-bottom{display:flex;gap:8px;flex-wrap:wrap;padding-top:16px;border-top:1px solid rgba(255,255,255,.15);position:relative;z-index:1}
.tech-pill{display:flex;align-items:center;gap:6px;background:rgba(255,255,255,.12);padding:5px 12px;border-radius:20px;font-size:.75rem}
.pill-dot{width:6px;height:6px;border-radius:50%}.pulse-pink{background:#ff6b9d;box-shadow:0 0 6px #ff6b9d}.pulse-green{background:#2ecc71;box-shadow:0 0 6px #2ecc71}
.chat-main{flex:1;display:flex;flex-direction:column;background:#fafbff}
.task-progress-bar{padding:12px 24px;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);color:#fff}
.progress-info{display:flex;justify-content:space-between;font-size:.85rem;font-weight:600;margin-bottom:8px}
.progress-track{height:6px;background:rgba(255,255,255,.3);border-radius:3px;overflow:hidden}
.progress-fill{height:100%;background:#fff;border-radius:3px;transition:width .3s ease}
.chat-topbar{display:flex;align-items:center;justify-content:space-between;padding:14px 24px;background:#fff;border-bottom:1px solid #f0e6f6}
.topbar-left{display:flex;align-items:center;gap:12px}
.ai-avatar-blob{width:44px;height:44px;background:linear-gradient(135deg,#ff6b9d,#c44dff);border-radius:14px;display:flex;align-items:center;justify-content:center;font-size:22px;box-shadow:0 4px 14px rgba(196,77,255,.3)}
.ai-name{margin:0;font-size:1.05rem;color:#2d1b4e;font-weight:700}.ai-status{font-size:.78rem;color:#999;display:flex;align-items:center;gap:4px}
.status-dot{width:7px;height:7px;border-radius:50%;background:#2ecc71;box-shadow:0 0 6px #2ecc71}
.clear-btn{width:36px;height:36px;border-radius:12px;border:none;background:#f5f0ff;color:#c44dff;cursor:pointer;display:flex;align-items:center;justify-content:center;transition:all .2s}
.clear-btn:hover{background:#ffe0ec;color:#ff6b9d;transform:scale(1.1)}
.chat-body{flex:1;overflow-y:auto;padding:24px}
.welcome{text-align:center;padding:48px 16px;animation:pop-in .5s ease}
.welcome-blob{width:80px;height:80px;margin:0 auto 16px;background:linear-gradient(135deg,#ffe0ec,#e0c3fc);border-radius:50%;display:flex;align-items:center;justify-content:center;animation:float 3s ease-in-out infinite}
.welcome-emoji{font-size:40px}.welcome h2{color:#2d1b4e;font-size:1.6rem;margin-bottom:8px}.welcome-desc{color:#888;margin-bottom:24px}
.cap-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;max-width:480px;margin:0 auto}
.cap-card{background:#fff;border:2px solid #f0e6f6;border-radius:16px;padding:14px 10px;display:flex;flex-direction:column;align-items:center;gap:6px;font-size:.82rem;color:#555;transition:all .25s cubic-bezier(.34,1.56,.64,1);cursor:default}
.cap-card:hover{border-color:hsl(var(--card-hue),80%,70%);background:hsl(var(--card-hue),100%,97%);transform:translateY(-4px) scale(1.04);box-shadow:0 8px 20px hsla(var(--card-hue),80%,60%,.15)}
.cap-emoji{font-size:1.5rem}
.bubble-row{display:flex;gap:10px;margin-bottom:18px;max-width:72%;animation:slide-up .3s ease}
.bubble-row.user{margin-left:auto;flex-direction:row-reverse}
.bubble-avatar{width:36px;height:36px;border-radius:12px;display:flex;align-items:center;justify-content:center;font-size:18px;flex-shrink:0}
.bubble-row.assistant .bubble-avatar{background:linear-gradient(135deg,#ffe0ec,#e0c3fc)}
.bubble-row.user .bubble-avatar{background:linear-gradient(135deg,#c3feef,#d0e0ff)}
.bubble-content{display:flex;flex-direction:column}.user .bubble-content{align-items:flex-end}
.bubble-name{font-size:.72rem;color:#bbb;margin-bottom:3px}
.bubble{padding:12px 16px;border-radius:18px;line-height:1.65;font-size:.92rem;word-break:break-word}
.bubble.assistant{background:#fff;color:#333;border:1.5px solid #f0e6f6;border-top-left-radius:4px;box-shadow:0 2px 8px rgba(196,77,255,.06)}
.bubble.user{background:linear-gradient(135deg,#ff6b9d,#c44dff);color:#fff;border-top-right-radius:4px;box-shadow:0 4px 14px rgba(255,107,157,.25)}
.bubble-time{font-size:.68rem;color:#ccc;margin-top:3px}
.thinking{padding:14px 20px}.dots{display:flex;gap:5px}
.dots span{width:9px;height:9px;border-radius:50%;background:linear-gradient(135deg,#ff6b9d,#c44dff);animation:dot-bounce 1.4s infinite ease-in-out}
.dots span:nth-child(2){animation-delay:.16s}.dots span:nth-child(3){animation-delay:.32s}
.chat-footer{padding:14px 24px;background:#fff;border-top:1px solid #f0e6f6}
.input-box{display:flex;align-items:flex-end;gap:10px;background:#f8f5ff;border:2px solid #ece4f8;border-radius:20px;padding:8px 8px 8px 18px;transition:all .25s}
.input-box:focus-within{border-color:#c44dff;box-shadow:0 0 0 4px rgba(196,77,255,.1);background:#fff}
.input-box textarea{flex:1;border:none;outline:none;background:transparent;font-size:.92rem;resize:none;line-height:1.5;font-family:inherit;max-height:100px;color:#333}
.input-box textarea::placeholder{color:#bbb}
.send-btn{width:42px;height:42px;border-radius:14px;border:none;background:linear-gradient(135deg,#ff6b9d,#c44dff);color:#fff;cursor:pointer;display:flex;align-items:center;justify-content:center;transition:all .25s cubic-bezier(.34,1.56,.64,1);flex-shrink:0;box-shadow:0 4px 14px rgba(196,77,255,.3)}
.send-btn:disabled{opacity:.35;cursor:not-allowed;box-shadow:none}.send-btn:hover:not(:disabled){transform:scale(1.12) rotate(-5deg)}
.footer-hint{font-size:.72rem;color:#ccc;text-align:center;margin-top:8px}
@media(max-width:768px){.chat-sidebar{display:none}.dopa-chat{border-radius:0;margin:0;height:calc(100vh - 60px)}.bubble-row{max-width:88%}.cap-grid{grid-template-columns:repeat(2,1fr)}}
</style>
