// API配置文件
export const API_CONFIG = {
  // 开发环境
  development: {
    baseURL: 'http://localhost:8099',
    timeout: 60000
  }
}

// 根据当前环境获取配置
const env = import.meta.env.MODE || 'development'
export const currentConfig = API_CONFIG[env as keyof typeof API_CONFIG]

// 获取完整的API URL
export const getApiUrl = (path: string) => {
  return `${currentConfig.baseURL}${path}`
} 