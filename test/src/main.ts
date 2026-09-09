import './assets/main.css'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import router from './router'
import App from './App.vue'
import { ref, onMounted, onUnmounted } from 'vue'
import { useUserStore } from './stores/user'

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

const sidebarWidth = ref(300)
let isDragging = false

const startDrag = (e) => {
  isDragging = true
  document.body.style.cursor = 'col-resize'
}

const onDrag = (e) => {
  if (isDragging) {
    // 限制最小宽度为200，最大为500
    sidebarWidth.value = Math.min(500, Math.max(200, e.clientX))
  }
}

const stopDrag = () => {
  isDragging = false
  document.body.style.cursor = ''
}

const toggleTop = ref(20)
const toggleLeft = ref(20)
let isToggleDragging = false
let dragOffsetX = 0
let dragOffsetY = 0

const startToggleDrag = (e) => {
  // 只允许鼠标左键拖动
  if (e.button !== 0) return
  isToggleDragging = true
  dragOffsetX = e.clientX - toggleLeft.value
  dragOffsetY = e.clientY - toggleTop.value
  document.body.style.cursor = 'move'
  window.addEventListener('mousemove', onToggleDrag)
  window.addEventListener('mouseup', stopToggleDrag)
}

const onToggleDrag = (e) => {
  if (isToggleDragging) {
    toggleLeft.value = Math.max(0, Math.min(window.innerWidth - 50, e.clientX - dragOffsetX))
    toggleTop.value = Math.max(0, Math.min(window.innerHeight - 50, e.clientY - dragOffsetY))
  }
}

const stopToggleDrag = () => {
  isToggleDragging = false
  document.body.style.cursor = ''
  window.removeEventListener('mousemove', onToggleDrag)
  window.removeEventListener('mouseup', stopToggleDrag)
}

const userStore = useUserStore()
userStore.initUser()

onMounted(() => {
  window.addEventListener('mousemove', onDrag)
  window.addEventListener('mouseup', stopDrag)
})

onUnmounted(() => {
  window.removeEventListener('mousemove', onDrag)
  window.removeEventListener('mouseup', stopDrag)
})

app.mount('#app')
