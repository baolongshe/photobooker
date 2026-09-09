<template>
  <div class="sys-page">
    <div class="hero">
      <div class="hero-inner">
        <div class="hero-badge">📸 PhotoBooker</div>
        <h1>系统架构总览</h1>
        <p>探索摄影约拍平台背后的技术世界</p>
        <div class="hero-shapes"><div class="shape s1"></div><div class="shape s2"></div><div class="shape s3"></div></div>
      </div>
    </div>
    <div class="tab-container">
      <div class="tab-nav">
        <button v-for="tab in tabs" :key="tab.key" :class="['tab-btn',{active:activeTab===tab.key}]" :style="activeTab===tab.key?{'--btn-hue':tab.hue}:{}" @click="activeTab=tab.key">
          <span class="tab-icon">{{ tab.icon }}</span><span>{{ tab.label }}</span>
        </button>
      </div>
      <div class="tab-content">
        <!-- Agent 调度 -->
        <div v-if="activeTab==='agent'" class="tab-panel">
          <h2 class="panel-title">🤖 Agent 智能调度</h2>
          <p class="panel-desc">基于 ReAct 模式的智能体调度系统，实现推理→行动→观察的循环决策</p>
          <div class="flow-diagram">
            <div class="flow-node" style="--node-hue:330">🧠 推理 Reasoning</div>
            <div class="flow-arrow">→</div>
            <div class="flow-node" style="--node-hue:270">🔧 工具调用 Tool Call</div>
            <div class="flow-arrow">→</div>
            <div class="flow-node" style="--node-hue:200">👁 观察 Observation</div>
            <div class="flow-arrow">→</div>
            <div class="flow-node" style="--node-hue:150">✅ 决策 Decision</div>
          </div>
          <div class="card-grid">
            <div class="info-card" style="--card-hue:330">
              <h4>ReActAgentService</h4>
              <p>手动驱动 tool_calls 执行循环，每轮将用户消息 + 工具结果送入模型</p>
            </div>
            <div class="info-card" style="--card-hue:270">
              <h4>AgentToolRegistry</h4>
              <p>工具注册表，映射模型可见函数名到 AgentTool Bean，支持动态注册</p>
            </div>
            <div class="info-card" style="--card-hue:200">
              <h4>AgentSystemPrompt</h4>
              <p>XML 边界声明的系统提示词，根据登录态动态调整可用工具集</p>
            </div>
            <div class="info-card" style="--card-hue:150">
              <h4>FunctionCallContext</h4>
              <p>AtomicReference 桥接跨线程用户身份，确保并发安全的上下文传递</p>
            </div>
          </div>
        </div>
        <!-- 记忆会话 -->
        <div v-if="activeTab==='memory'" class="tab-panel">
          <h2 class="panel-title">💬 记忆与会话管理</h2>
          <p class="panel-desc">多层记忆系统，让 AI 拥有短期对话记忆和长期用户画像</p>
          <div class="layer-stack">
            <div class="layer" style="--layer-hue:330"><span class="layer-label">短期记忆</span><span class="layer-desc">会话内上下文窗口，滑动窗口保留最近 N 轮对话</span></div>
            <div class="layer" style="--layer-hue:280"><span class="layer-label">Session 管理</span><span class="layer-desc">sessionId 隔离不同会话，支持多轮连续对话</span></div>
            <div class="layer" style="--layer-hue:220"><span class="layer-label">RAG 检索增强</span><span class="layer-desc">Qdrant 向量库 + BGE 嵌入 + Rerank 重排序</span></div>
            <div class="layer" style="--layer-hue:170"><span class="layer-label">长期画像</span><span class="layer-desc">用户偏好、历史交互摘要，持久化存储</span></div>
          </div>
        </div>
        <!-- 数据处理 -->
        <div v-if="activeTab==='data'" class="tab-panel">
          <h2 class="panel-title">📊 数据处理流水线</h2>
          <p class="panel-desc">从用户上传到 AI 处理，完整的数据流转路径</p>
          <div class="flow-diagram">
            <div class="flow-node" style="--node-hue:30">📤 用户上传</div>
            <div class="flow-arrow">→</div>
            <div class="flow-node" style="--node-hue:50">🔄 数据清洗</div>
            <div class="flow-arrow">→</div>
            <div class="flow-node" style="--node-hue:180">🧬 向量化</div>
            <div class="flow-arrow">→</div>
            <div class="flow-node" style="--node-hue:260">💾 Qdrant 存储</div>
          </div>
          <div class="card-grid">
            <div class="info-card" style="--card-hue:30">
              <h4>文件处理</h4>
              <p>支持图片、文档等多种格式上传，自动解析和预处理</p>
            </div>
            <div class="info-card" style="--card-hue:180">
              <h4>BGE 嵌入</h4>
              <p>使用 BGE 模型将文本转换为高维向量，支持语义检索</p>
            </div>
            <div class="info-card" style="--card-hue:260">
              <h4>Qdrant 向量库</h4>
              <p>高性能向量数据库，支持近邻搜索和过滤查询</p>
            </div>
            <div class="info-card" style="--card-hue:120">
              <h4>Rerank 重排</h4>
              <p>对检索结果二次排序，提升最终返回给 LLM 的上下文质量</p>
            </div>
          </div>
        </div>
        <!-- 安全机制 -->
        <div v-if="activeTab==='security'" class="tab-panel">
          <h2 class="panel-title">🔒 安全机制</h2>
          <p class="panel-desc">多层安全防护，保障系统和数据安全</p>
          <div class="card-grid">
            <div class="info-card" style="--card-hue:0">
              <h4>JWT 认证</h4>
              <p>JwtAuthenticationFilter 实现无状态认证，Token 自动刷新</p>
            </div>
            <div class="info-card" style="--card-hue:30">
              <h4>角色权限控制</h4>
              <p>基于角色的访问控制（RBAC），区分用户/摄影师/管理员权限</p>
            </div>
            <div class="info-card" style="--card-hue:270">
              <h4>Prompt 注入防护</h4>
              <p>PromptInjectionGuard 对用户输入清洗，防止恶意 Prompt 注入</p>
            </div>
            <div class="info-card" style="--card-hue:200">
              <h4>接口安全</h4>
              <p>CORS 配置、请求频率限制、敏感数据脱敏处理</p>
            </div>
          </div>
        </div>
        <!-- 订单超时 -->
        <div v-if="activeTab==='order'" class="tab-panel">
          <h2 class="panel-title">⏰ 订单超时机制</h2>
          <p class="panel-desc">基于 Redis ZSet 的高可靠订单超时处理系统</p>
          <div class="flow-diagram">
            <div class="flow-node" style="--node-hue:30">📝 创建订单</div>
            <div class="flow-arrow">→</div>
            <div class="flow-node" style="--node-hue:50">💎 ZSet 写入</div>
            <div class="flow-arrow">→</div>
            <div class="flow-node" style="--node-hue:280">⏱ 定时扫描</div>
            <div class="flow-arrow">→</div>
            <div class="flow-node" style="--node-hue:0">🔒 分布式锁</div>
            <div class="flow-arrow">→</div>
            <div class="flow-node" style="--node-hue:120">✅ 超时处理</div>
          </div>
          <div class="card-grid">
            <div class="info-card" style="--card-hue:50">
              <h4>Redis ZSet</h4>
              <p>score 为超时时间戳，member 为订单ID，天然支持范围扫描</p>
            </div>
            <div class="info-card" style="--card-hue:280">
              <h4>定时扫描</h4>
              <p>@Scheduled 定时任务，周期性扫描已到超时时间的订单</p>
            </div>
            <div class="info-card" style="--card-hue:0">
              <h4>分布式锁</h4>
              <p>多实例部署下防止重复处理，确保订单状态变更的幂等性</p>
            </div>
            <div class="info-card" style="--card-hue:120">
              <h4>级联处理</h4>
              <p>超时后自动取消订单、释放库存、发送通知、触发退款流程</p>
            </div>
          </div>
        </div>
        <!-- 通信模块 -->
        <div v-if="activeTab==='comm'" class="tab-panel">
          <h2 class="panel-title">📡 实时通信模块</h2>
          <p class="panel-desc">基于 Netty WebSocket 的高性能实时通信系统</p>
          <div class="card-grid">
            <div class="info-card" style="--card-hue:200">
              <h4>Netty WebSocket</h4>
              <p>高性能长连接，ConcurrentHashMap 维护用户-Channel 映射</p>
            </div>
            <div class="info-card" style="--card-hue:270">
              <h4>JWT 握手认证</h4>
              <p>WebSocket 握手阶段通过 JWT 验证身份，确保连接安全</p>
            </div>
            <div class="info-card" style="--card-hue:330">
              <h4>心跳机制</h4>
              <p>定时 Ping/Pong 心跳检测，自动清理失效连接</p>
            </div>
            <div class="info-card" style="--card-hue:50">
              <h4>消息投递</h4>
              <p>支持消息投递确认（delivered 状态），保证消息可靠送达</p>
            </div>
          </div>
        </div>
        <!-- 技术栈 -->
        <div v-if="activeTab==='stack'" class="tab-panel">
          <h2 class="panel-title">🛠 技术栈总览</h2>
          <p class="panel-desc">现代全栈技术选型，追求性能与开发效率的平衡</p>
          <div class="card-grid">
            <div class="info-card" style="--card-hue:200">
              <h4>后端框架</h4>
              <p>Spring Boot 3.x + Spring AI + MyBatis-Plus</p>
            </div>
            <div class="info-card" style="--card-hue:270">
              <h4>前端框架</h4>
              <p>Vue 3 + TypeScript + Element Plus + Vite</p>
            </div>
            <div class="info-card" style="--card-hue:330">
              <h4>数据存储</h4>
              <p>MySQL + Redis + Qdrant 向量数据库</p>
            </div>
            <div class="info-card" style="--card-hue:50">
              <h4>通信层</h4>
              <p>Netty WebSocket + RESTful API</p>
            </div>
            <div class="info-card" style="--card-hue:120">
              <h4>AI 能力</h4>
              <p>Spring AI + ReAct Agent + RAG Pipeline</p>
            </div>
            <div class="info-card" style="--card-hue:0">
              <h4>部署运维</h4>
              <p>Docker + Nginx + 日志监控体系</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const activeTab = ref('agent')

const tabs = [
  { key: 'agent', label: 'Agent 调度', icon: '🤖', hue: 330 },
  { key: 'memory', label: '记忆会话', icon: '💬', hue: 270 },
  { key: 'data', label: '数据处理', icon: '📊', hue: 200 },
  { key: 'security', label: '安全机制', icon: '🔒', hue: 0 },
  { key: 'order', label: '订单超时', icon: '⏰', hue: 50 },
  { key: 'comm', label: '通信模块', icon: '📡', hue: 120 },
  { key: 'stack', label: '技术栈', icon: '🛠', hue: 200 }
]
</script>

<style scoped>
.sys-page {
  min-height: 100vh;
  background: #f5f5f7;
  padding-bottom: 60px;
}
.hero {
  background: linear-gradient(135deg, #ff6b9d 0%, #c44dff 40%, #6c5ce7 70%, #4facfe 100%);
  padding: 60px 20px 50px;
  text-align: center;
  position: relative;
  overflow: hidden;
}
.hero-inner { position: relative; z-index: 1; }
.hero-badge {
  display: inline-block;
  background: rgba(255,255,255,0.25);
  backdrop-filter: blur(10px);
  padding: 6px 20px;
  border-radius: 20px;
  color: #fff;
  font-size: 0.9rem;
  font-weight: 600;
  margin-bottom: 16px;
}
.hero h1 {
  color: #fff;
  font-size: 2.6rem;
  margin: 0 0 10px;
  font-weight: 800;
}
.hero p {
  color: rgba(255,255,255,0.9);
  font-size: 1.15rem;
  margin: 0;
}
.hero-shapes { position: absolute; top: 0; left: 0; width: 100%; height: 100%; pointer-events: none; }
.shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.15;
  animation: floatShape 8s ease-in-out infinite;
}
.s1 { width: 200px; height: 200px; background: #fff; top: -40px; left: 10%; animation-delay: 0s; }
.s2 { width: 140px; height: 140px; background: #ffeb3b; bottom: -30px; right: 15%; animation-delay: 2s; }
.s3 { width: 100px; height: 100px; background: #4facfe; top: 30%; right: 8%; animation-delay: 4s; }
@keyframes floatShape {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-20px) scale(1.05); }
}
.tab-container {
  max-width: 1100px;
  margin: -30px auto 0;
  position: relative;
  z-index: 2;
  padding: 0 20px;
}
.tab-nav {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
  margin-bottom: 28px;
}
.tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  border: none;
  border-radius: 16px;
  background: #fff;
  color: #555;
  font-size: 0.92rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 10px rgba(0,0,0,0.06);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.tab-btn:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.tab-btn.active {
  background: linear-gradient(135deg, hsl(var(--btn-hue, 270), 80%, 65%), hsl(calc(var(--btn-hue, 270) + 30), 80%, 55%));
  color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 4px 20px hsla(var(--btn-hue, 270), 80%, 55%, 0.35);
}
.tab-icon { font-size: 1.1rem; }
.tab-content {
  background: #fff;
  border-radius: 20px;
  padding: 36px 32px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.06);
  min-height: 400px;
  animation: fadeUp 0.35s ease;
}
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
.panel-title {
  font-size: 1.6rem;
  font-weight: 700;
  margin: 0 0 8px;
  color: #222;
}
.panel-desc {
  color: #777;
  font-size: 1rem;
  margin: 0 0 28px;
  line-height: 1.6;
}
.flow-diagram {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: center;
  margin-bottom: 32px;
  padding: 24px;
  background: linear-gradient(135deg, #f8f9ff, #f0f4ff);
  border-radius: 16px;
}
.flow-node {
  padding: 12px 20px;
  border-radius: 14px;
  background: linear-gradient(135deg, hsl(var(--node-hue), 75%, 62%), hsl(calc(var(--node-hue) + 20), 75%, 52%));
  color: #fff;
  font-weight: 600;
  font-size: 0.9rem;
  box-shadow: 0 3px 12px hsla(var(--node-hue), 75%, 52%, 0.3);
  white-space: nowrap;
}
.flow-arrow {
  font-size: 1.4rem;
  color: #bbb;
  font-weight: bold;
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 18px;
}
.info-card {
  padding: 22px;
  border-radius: 16px;
  background: linear-gradient(135deg, hsl(var(--card-hue), 80%, 97%), hsl(var(--card-hue), 60%, 94%));
  border: 1px solid hsl(var(--card-hue), 60%, 90%);
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.25s;
}
.info-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 20px hsla(var(--card-hue), 60%, 50%, 0.15);
}
.info-card h4 {
  margin: 0 0 8px;
  font-size: 1.05rem;
  font-weight: 700;
  color: hsl(var(--card-hue), 60%, 35%);
}
.info-card p {
  margin: 0;
  color: hsl(var(--card-hue), 30%, 45%);
  font-size: 0.88rem;
  line-height: 1.6;
}
.layer-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}
.layer {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 24px;
  border-radius: 16px;
  background: linear-gradient(135deg, hsl(var(--layer-hue), 75%, 95%), hsl(var(--layer-hue), 60%, 90%));
  border-left: 4px solid hsl(var(--layer-hue), 70%, 60%);
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.layer:hover { transform: translateX(6px); }
.layer-label {
  font-weight: 700;
  font-size: 1rem;
  color: hsl(var(--layer-hue), 60%, 35%);
  min-width: 100px;
}
.layer-desc {
  color: hsl(var(--layer-hue), 30%, 50%);
  font-size: 0.9rem;
}
@media (max-width: 768px) {
  .hero { padding: 40px 16px 36px; }
  .hero h1 { font-size: 1.8rem; }
  .tab-content { padding: 24px 18px; }
  .card-grid { grid-template-columns: 1fr; }
  .flow-diagram { flex-direction: column; }
  .flow-arrow { transform: rotate(90deg); }
}
</style>
