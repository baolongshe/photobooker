<template>
  <div class="rag-management">
    <h2 style="color: #222; font-size: 20px; font-weight: bold; margin-bottom: 20px;">知识库管理（RAG）</h2>

    <!-- Section 1: 系统状态面板 -->
    <el-card class="section-card" shadow="hover">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between;">
          <span style="font-size: 16px; font-weight: bold;">系统状态</span>
          <el-button size="small" :icon="Refresh" @click="fetchStatus" :loading="statusLoading">刷新</el-button>
        </div>
      </template>
      <el-descriptions :column="2" border v-loading="statusLoading">
        <el-descriptions-item label="Qdrant 状态">
          <el-tag :type="status.qdrant?.connected ? 'success' : 'danger'">
            {{ status.qdrant?.connected ? '已连接' : '未连接' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Collection 名称">{{ status.qdrant?.collectionName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="向量数量">{{ status.qdrant?.vectorCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="Elasticsearch 状态">
          <el-tag :type="status.elasticsearch?.connected ? 'success' : 'danger'">
            {{ status.elasticsearch?.connected ? '已连接' : '未连接' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="索引名称">{{ status.elasticsearch?.indexName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="文档数量">{{ status.elasticsearch?.docCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="上次索引时间">{{ status.lastIndexTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="重建任务状态">
          <el-tag :type="status.rebuildStatus === 'IDLE' ? 'info' : 'warning'">
            {{ status.rebuildStatus === 'IDLE' ? '空闲' : '进行中' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- Section 2: 索引操作面板 -->
    <el-card class="section-card" shadow="hover">
      <template #header>
        <span style="font-size: 16px; font-weight: bold;">索引操作</span>
      </template>
      <el-space>
        <el-button type="danger" @click="handleFullRebuild" :loading="indexLoading">
          全量重建索引
        </el-button>
        <el-button type="primary" @click="handleIncrementalIndex" :loading="indexLoading">
          增量索引
        </el-button>
      </el-space>
      <div v-if="indexMessage" style="margin-top: 12px;">
        <el-alert :title="indexMessage" :type="indexMessageType" show-icon :closable="false" />
      </div>
    </el-card>

    <!-- Section 3: 文档浏览 -->
    <el-card class="section-card" shadow="hover">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between;">
          <span style="font-size: 16px; font-weight: bold;">文档浏览</span>
          <el-button size="small" :icon="Refresh" @click="fetchDocuments" :loading="docLoading">刷新</el-button>
        </div>
      </template>
      <el-table :data="documents" v-loading="docLoading" stripe style="width: 100%;">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="内容" min-width="300">
          <template #default="{ row }">
            <el-tooltip :content="row.content" placement="top" :disabled="row.content?.length <= 80">
              <span>{{ row.content?.substring(0, 80) }}{{ row.content?.length > 80 ? '...' : '' }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="120" />
        <el-table-column prop="sourceId" label="来源ID" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="handleDeleteDoc(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="docPage"
          v-model:page-size="docPageSize"
          :total="docTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchDocuments"
          @current-change="fetchDocuments"
        />
      </div>
    </el-card>

    <!-- Section 4: 查询测试面板 -->
    <el-card class="section-card" shadow="hover">
      <template #header>
        <span style="font-size: 16px; font-weight: bold;">查询测试</span>
      </template>
      <el-form label-width="80px">
        <el-form-item label="问题">
          <el-input v-model="testQuestion" type="textarea" rows="3" placeholder="请输入要测试的问题" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleTestQuery" :loading="queryLoading">测试查询</el-button>
        </el-form-item>
      </el-form>

      <!-- 查询结果展示 -->
      <div v-if="queryResult" class="query-result-area">
        <el-collapse v-model="activeCollapsePanels">
          <!-- 查询重写结果 -->
          <el-collapse-item title="1. 查询重写结果" name="rewrite">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="原始查询">{{ queryResult.originalQuery || '-' }}</el-descriptions-item>
              <el-descriptions-item label="意图补全查询">{{ queryResult.intentQuery || '-' }}</el-descriptions-item>
              <el-descriptions-item label="同义扩写列表">
                <div v-if="queryResult.synonymQueries?.length">
                  <el-tag v-for="(q, i) in queryResult.synonymQueries" :key="i" style="margin-right: 6px; margin-bottom: 4px;">{{ q }}</el-tag>
                </div>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="子查询列表">
                <div v-if="queryResult.subQueries?.length">
                  <el-tag v-for="(q, i) in queryResult.subQueries" :key="i" type="info" style="margin-right: 6px; margin-bottom: 4px;">{{ q }}</el-tag>
                </div>
                <span v-else>-</span>
              </el-descriptions-item>
            </el-descriptions>
          </el-collapse-item>

          <!-- 向量检索结果 -->
          <el-collapse-item title="2. 向量检索结果" name="vector">
            <div v-if="queryResult.vectorResults?.length">
              <div v-for="(item, i) in queryResult.vectorResults" :key="i" class="result-item">
                <el-tag type="success" size="small">分数: {{ item.score?.toFixed(4) }}</el-tag>
                <span style="margin-left: 8px;">{{ item.content }}</span>
              </div>
            </div>
            <el-empty v-else description="无结果" :image-size="40" />
          </el-collapse-item>

          <!-- 关键词检索结果 -->
          <el-collapse-item title="3. 关键词检索结果" name="keyword">
            <div v-if="queryResult.keywordResults?.length">
              <div v-for="(item, i) in queryResult.keywordResults" :key="i" class="result-item">
                <el-tag type="warning" size="small">分数: {{ item.score?.toFixed(4) }}</el-tag>
                <span style="margin-left: 8px;">{{ item.content }}</span>
              </div>
            </div>
            <el-empty v-else description="无结果" :image-size="40" />
          </el-collapse-item>

          <!-- 融合后结果 -->
          <el-collapse-item title="4. 融合后结果" name="fused">
            <div v-if="queryResult.fusedResults?.length">
              <div v-for="(item, i) in queryResult.fusedResults" :key="i" class="result-item">
                <el-tag size="small">分数: {{ item.score?.toFixed(4) }}</el-tag>
                <span style="margin-left: 8px;">{{ item.content }}</span>
              </div>
            </div>
            <el-empty v-else description="无结果" :image-size="40" />
          </el-collapse-item>

          <!-- Rerank 后结果 -->
          <el-collapse-item title="5. Rerank 后结果" name="rerank">
            <div v-if="queryResult.rerankResults?.length">
              <div v-for="(item, i) in queryResult.rerankResults" :key="i" class="result-item">
                <el-tag type="danger" size="small">分数: {{ item.score?.toFixed(4) }}</el-tag>
                <span style="margin-left: 8px;">{{ item.content }}</span>
              </div>
            </div>
            <el-empty v-else description="无结果" :image-size="40" />
          </el-collapse-item>

          <!-- 最终回答 -->
          <el-collapse-item title="6. 最终回答" name="answer">
            <div v-if="queryResult.answer" class="final-answer">
              {{ queryResult.answer }}
            </div>
            <el-empty v-else description="无回答" :image-size="40" />
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-card>

    <!-- Section 5: 参数配置 -->
    <el-card class="section-card" shadow="hover">
      <template #header>
        <span style="font-size: 16px; font-weight: bold;">参数配置</span>
      </template>
      <el-form label-width="140px" style="max-width: 500px;" v-loading="configLoading">
        <el-form-item label="Top-K (返回数量)">
          <el-slider v-model="configTopK" :min="1" :max="20" :step="1" show-input />
        </el-form-item>
        <el-form-item label="Score Threshold">
          <el-slider v-model="configScoreThreshold" :min="0" :max="1" :step="0.05" show-input />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSaveConfig" :loading="configSaving">保存配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import api from '@/utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

// ===== Section 1: 系统状态 =====
const status = ref<any>({})
const statusLoading = ref(false)
let statusTimer: ReturnType<typeof setInterval> | null = null

const fetchStatus = async () => {
  statusLoading.value = true
  try {
    const res = await api.get('/photo/admin/rag/status')
    const raw = res.data || {}
    // Transform backend response to match template expectations
    const qdrantRaw = raw.qdrant || {}
    const esRaw = raw.elasticsearch || {}
    const collection = qdrantRaw.collection || {}
    const esIndex = esRaw.index || {}
    status.value = {
      qdrant: {
        connected: qdrantRaw.status === 'healthy',
        collectionName: collection.name || '-',
        vectorCount: collection.vectors_count ?? collection.points_count ?? '-'
      },
      elasticsearch: {
        connected: esRaw.status === 'healthy',
        indexName: esIndex.indexName || '-',
        docCount: esIndex.totalDocs ?? '-'
      },
      lastIndexTime: raw.lastIndexTime || '-',
      rebuildStatus: raw.rebuildTask?.status === 'running' ? 'RUNNING' : 'IDLE'
    }
  } catch (e) {
    console.error('获取RAG状态失败', e)
  } finally {
    statusLoading.value = false
  }
}

// ===== Section 2: 索引操作 =====
const indexLoading = ref(false)
const indexMessage = ref('')
const indexMessageType = ref<'success' | 'error' | 'info' | 'warning'>('info')

const handleFullRebuild = async () => {
  try {
    await ElMessageBox.confirm('确定要全量重建索引吗？此操作可能需要较长时间。', '确认', { type: 'warning' })
  } catch {
    return
  }
  indexLoading.value = true
  indexMessage.value = ''
  try {
    const res = await api.post('/photo/admin/rag/index')
    indexMessage.value = (res.data as any)?.message || '全量重建索引任务已启动'
    indexMessageType.value = 'success'
    ElMessage.success(indexMessage.value)
    // 延迟刷新状态
    setTimeout(fetchStatus, 2000)
  } catch (e) {
    indexMessage.value = '全量重建索引失败'
    indexMessageType.value = 'error'
    ElMessage.error('全量重建索引失败')
  } finally {
    indexLoading.value = false
  }
}

const handleIncrementalIndex = async () => {
  indexLoading.value = true
  indexMessage.value = ''
  try {
    const res = await api.post('/photo/admin/rag/index/incremental')
    indexMessage.value = (res.data as any)?.message || '增量索引任务已启动'
    indexMessageType.value = 'success'
    ElMessage.success(indexMessage.value)
    setTimeout(fetchStatus, 2000)
  } catch (e) {
    indexMessage.value = '增量索引失败'
    indexMessageType.value = 'error'
    ElMessage.error('增量索引失败')
  } finally {
    indexLoading.value = false
  }
}

// ===== Section 3: 文档浏览 =====
const documents = ref<any[]>([])
const docLoading = ref(false)
const docPage = ref(1)
const docPageSize = ref(20)
const docTotal = ref(0)

const fetchDocuments = async () => {
  docLoading.value = true
  try {
    const res = await api.get('/photo/admin/rag/documents', {
      params: { page: docPage.value, size: docPageSize.value }
    })
    const data = res.data || {}
    documents.value = (data.documents || []).map((doc: any) => ({
      ...doc,
      createTime: doc.createdAt
    }))
    docTotal.value = data.total || 0
  } catch (e) {
    console.error('获取文档列表失败', e)
  } finally {
    docLoading.value = false
  }
}

const handleDeleteDoc = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该文档吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await api.delete(`/photo/admin/rag/documents/${id}`)
    ElMessage.success('删除成功')
    fetchDocuments()
    fetchStatus()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

// ===== Section 4: 查询测试 =====
const testQuestion = ref('')
const queryLoading = ref(false)
const queryResult = ref<any>(null)
const activeCollapsePanels = ref<string[]>(['rewrite', 'vector', 'keyword', 'fused', 'rerank', 'answer'])

const handleTestQuery = async () => {
  if (!testQuestion.value.trim()) {
    ElMessage.warning('请输入测试问题')
    return
  }
  queryLoading.value = true
  queryResult.value = null
  try {
    const res = await api.post('/photo/admin/rag/test-query', {
      question: testQuestion.value,
      chatHistory: ''
    })
    const data = res.data || {}
    // Transform: backend returns nested rewrite object
    const rewrite = data.rewrite || {}
    queryResult.value = {
      originalQuery: rewrite.originalQuery || '',
      intentQuery: rewrite.compensatedQuery || '',
      synonymQueries: rewrite.expandedQueries || [],
      subQueries: rewrite.subQueries || [],
      vectorResults: data.vectorResults || [],
      keywordResults: data.esResults || [],
      fusedResults: data.mergedResults || [],
      rerankResults: [],
      answer: data.answer || ''
    }
    // 默认展开所有面板
    activeCollapsePanels.value = ['rewrite', 'vector', 'keyword', 'fused', 'rerank', 'answer']
  } catch (e) {
    ElMessage.error('查询测试失败')
  } finally {
    queryLoading.value = false
  }
}

// ===== Section 5: 参数配置 =====
const configTopK = ref(5)
const configScoreThreshold = ref(0.3)
const configLoading = ref(false)
const configSaving = ref(false)

const fetchConfig = async () => {
  configLoading.value = true
  try {
    const res = await api.get('/photo/admin/rag/config')
    const data = res.data || {}
    // Transform: backend returns retrieval.topK and retrieval.scoreThreshold
    const retrieval = data.retrieval || {}
    configTopK.value = retrieval.topK ?? 5
    configScoreThreshold.value = retrieval.scoreThreshold ?? 0.3
  } catch (e) {
    console.error('获取配置失败', e)
  } finally {
    configLoading.value = false
  }
}

const handleSaveConfig = async () => {
  configSaving.value = true
  try {
    await api.put('/photo/admin/rag/config', {
      topK: configTopK.value,
      scoreThreshold: configScoreThreshold.value
    })
    ElMessage.success('配置保存成功')
  } catch (e) {
    ElMessage.error('配置保存失败')
  } finally {
    configSaving.value = false
  }
}

// ===== 工具函数 =====
const formatDateTime = (val: string) => val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'

// ===== 生命周期 =====
onMounted(() => {
  fetchStatus()
  fetchDocuments()
  fetchConfig()
  // 每30秒自动刷新状态
  statusTimer = setInterval(fetchStatus, 30000)
})

onUnmounted(() => {
  if (statusTimer) {
    clearInterval(statusTimer)
  }
})
</script>

<style scoped>
.rag-management {
  padding: 0;
  background: #fff;
  min-height: 100vh;
}
.section-card {
  margin-bottom: 20px;
}
.query-result-area {
  margin-top: 20px;
}
.result-item {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  line-height: 1.6;
}
.result-item:last-child {
  border-bottom: none;
}
.final-answer {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 6px;
  line-height: 1.8;
  font-size: 14px;
  white-space: pre-wrap;
}
</style>
