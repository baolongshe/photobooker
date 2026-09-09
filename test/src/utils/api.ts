import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from "@/router";

// 创建 axios 实例
const api = axios.create({
  baseURL: "/api",
  //baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8099',
  timeout: 60000 // 60 秒
})

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    console.log('[api.ts] 请求拦截器 token:', token)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    console.log('[api.ts] 请求拦截器 config:', config)
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    const res = response.data;
    console.log('[api.ts] 响应拦截器 response:', response)
    // 检查配置项，如果设置了 silentErrors 则不显示全局错误提示
    const silentErrors = (response.config as any).silentErrors
    if (res.code !== 1) {
      console.error('[api.ts] 接口异常:', res);
      // 统一错误提示
      if (!silentErrors) {
        ElMessage.error(res.msg || '服务器异常');
      }
      // 401 未登录处理
      if (res.code === 401) {
        console.warn('[api.ts] 响应拦截器检测到 401，清除 token 并准备跳转登录')
        localStorage.removeItem('token');
        localStorage.removeItem('user');
          router.push({ name: 'login' })
      }

      return Promise.reject(res);
    }
    console.log('[api.ts] 响应拦截器返回数据：', res.data);
    return res;
  },
  (error) => {
    if (error.response?.status === 401) {
      console.warn('[api.ts] 响应拦截器 error 检测到401，清除token并准备跳转登录')
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      // TODO: 此处应使用 router.push({ name: 'login' }) 进行跳转，需确保 router 实例可用
      // window.location.href = '/login';
    }
    ElMessage.error(error.response?.data?.msg || error.message || '请求失败');
    return Promise.reject(error);
  }
)

export default api 