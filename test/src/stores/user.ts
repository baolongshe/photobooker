import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from 'axios'

export interface User {
  id: number
  username: string
  realName: string
  phone: string
  gender: number
  avatar: string
  openid: string
  role?: number
  token?: string
}

export interface LoginForm {
  username: string
  password: string
}

export interface UserLoginVO {
  id: number
  openid: string
  token: string
  role: number
  avatar: string
}

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const token = ref<string>('')
  const isLoggedIn = ref<boolean>(false)

  // 初始化用户信息
  const initUser = () => {
    const savedToken = localStorage.getItem('token')
    const savedUser = localStorage.getItem('user')
    
    if (savedToken && savedUser) {
      token.value = savedToken
      user.value = JSON.parse(savedUser)
      isLoggedIn.value = true
    }
  }

  // 设置用户信息
  const setUserInfo = (userData: any) => {
    user.value = { ...userData }
    token.value = userData.token
    isLoggedIn.value = true
    
    // 保存到本地存储
    localStorage.setItem('token', userData.token)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  // 登录
  const login = async (loginForm: LoginForm) => {
    try {
      const response = await axios.post('/photo/user/login', loginForm)
      const { data } = response.data
      
      user.value = {
        id: data.id,
        username: data.username,      // 用后端返回的
        realName: data.realName || '',
        phone: data.phone || '',
        gender: data.gender || 0,
        avatar: data.avatar || '',    // 用后端返回的
        openid: data.openid,
        role: data.role,
        token: data.token
      }
      isLoggedIn.value = true
      
      // 保存到本地存储
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(user.value))
      
      return { success: true }
    } catch (error: any) {
      return { 
        success: false, 
        message: error.response?.data?.message || '登录失败' 
      }
    }
  }

  // 登出
  const logout = () => {
    user.value = null
    token.value = ''
    isLoggedIn.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return {
    user,
    token,
    isLoggedIn,
    initUser,
    setUserInfo,
    login,
    logout
  }
}) 