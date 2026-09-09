import { fileURLToPath, URL } from 'node:url'
import { resolve } from 'path'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    },
  },
  server: {
    port: 3000,
    host: '0.0.0.0',
    allowedHosts: [
      'straining-express-hatchet.ngrok-free.dev'
    ],
    proxy: {
      '/api': {
        target: 'http://localhost:8099',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/api/, '')
      },
      '/photo': {
        target: 'http://localhost:8099',
        changeOrigin: true
      },
      '/file': {
        target: 'http://localhost:8099',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://localhost:8081',
        ws: true,
        changeOrigin: true
      },
      '/ai': {
        target: 'http://localhost:8099',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/ai/, '/ai')
      }
    }
  }
})
