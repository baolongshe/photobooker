import { createRouter, createWebHashHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import PhotographerList from '../views/PhotographerList.vue'
import PhotographerNearby from '../views/PhotographerNearby.vue'
import PhotographerDetail from '../views/PhotographerDetail.vue'
import PortfolioDetail from '../views/PortfolioDetail.vue'
import OrderList from '../views/OrderList.vue'
import OrderDetail from '../views/OrderDetail.vue'
import Chat from '../views/Chat.vue'
import Profile from '../views/Profile.vue'
import TechnicalNotes from '../views/TechnicalNotes.vue'
import AiChatPage from '../views/AiChatPage.vue'
import SystemIntroduction from '../views/SystemIntroduction.vue'
import api from '../utils/api'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/',
    name: 'home',
    component: Home
  },
  {
    path: '/login',
    name: 'login',
    component: Login
  },
  {
    path: '/register',
    name: 'register',
    component: Register
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'users', component: () => import('@/views/admin/UserManagement.vue') },
      { path: 'photographer/list', component: () => import('@/views/admin/PhotographerManagement.vue') },
      { path: 'photographer-list', redirect: '/admin/photographer/list' },
      { path: 'orders', component: () => import('@/views/admin/OrderManagement.vue') },
      { path: 'applications', component: () => import('@/views/admin/ApplicationManagement.vue') },
      { path: 'announcements', component: () => import('@/views/admin/AnnouncementManagement.vue') },
      { path: 'refund-management', component: () => import('@/views/admin/RefundManagement.vue') },
      { path: 'rag-management', name: 'RagManagement', component: () => import('@/views/admin/RagManagement.vue'), meta: { title: '知识库管理' } }
    ]
  },
  {
    path: '/photographer-list',
    name: 'photographer-list',
    component: PhotographerList
  },
  {
    path: '/photographer-nearby',
    name: 'photographer-nearby',
    component: PhotographerNearby
  },
  {
    path: '/photographer/:id',
    name: 'photographer-detail',
    component: PhotographerDetail
  },
  {
    path: '/portfolio/:id',
    name: 'PortfolioDetail',
    component: PortfolioDetail
  },
  {
    path: '/orders',
    name: 'orders',
    component: OrderList
  },
  {
    path: '/order/:id',
    name: 'order-detail',
    component: OrderDetail
  },
  {
    path: '/chat/:orderId',
    name: 'chat',
    component: Chat
  },
  {
    path: '/profile',
    name: 'profile',
    component: Profile
  },
  {
    path: '/technical-notes',
    name: 'technical-notes',
    component: TechnicalNotes
  },
  {
    path: '/photographer/orders',
    name: 'photographer-orders',
    component: () => import('../views/PhotographerOrders.vue')
  },
  {
    path: '/photographer/portfolio',
    name: 'photographer-portfolio',
    component: () => import('../views/PhotographerPortfolio.vue')
  },
  {
    path: '/photographer-map',
    name: 'photographer-map',
    component: () => import('../views/PhotographerMap.vue')
  },
  {
    path: '/photographer/packages',
    name: 'photographer-packages',
    component: () => import('../views/PhotographerPackages.vue')
  },
  {
    path: '/photographer/deliver-photos',
    name: 'photographer-deliver-photos',
    component: () => import('../views/PhotographerDeliverPhotos.vue')
  },
  {
    path: '/payment',
    name: 'payment',
    component: () => import('../views/Payment.vue')
  },
  {
    path: '/payment-success',
    name: 'payment-success',
    component: () => import('../views/PaymentSuccess.vue')
  },
  {
    path: '/order-photos',
    name: 'order-photos',
    component: () => import('../views/OrderPhotos.vue')
  },
  {
    path: '/after-sales',
    name: 'after-sales',
    component: () => import('../views/AfterSalesService.vue')
  },
  {
    path: '/ai-chat',
    name: 'ai-chat',
    component: AiChatPage
  },
  {
    path: '/system-intro',
    name: 'system-intro',
    component: SystemIntroduction
  }
]

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes
})

const publicPages = ['home', 'login', 'register', 'photographer-list', 'photographer-nearby', 'photographer-detail', 'PortfolioDetail', 'technical-notes', 'ai-chat', 'system-intro']

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const user = localStorage.getItem('user')
  const isPublic = publicPages.includes((to.name as string))
  const isAdminRoute = to.path.startsWith('/admin')

  if (!isPublic && !token) {
    next({ name: 'login' })
  } else if (isAdminRoute) {
    if (user) {
      const userData = JSON.parse(user)
      if (userData.role === 1) {
        next()
      } else {
        ElMessage.error('没有权限，禁止访问')
        next(false) // 阻止跳转
      }
    } else {
      next({ name: 'login' })
    }
  } else {
    next()
  }
})

const goToPhotographers = () => {
  // 假设你用 localStorage 存储 token 或 user 信息
  const token = localStorage.getItem('token')
  if (!token) {
    router.push({ name: 'login' })
  } else {
    router.push('/photographers')
  }
}

export default router