import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath } from 'url'

// 前端构建与本地开发配置。
// 本地 npm run dev 时，/api 和 /ws 会代理到 Spring Boot；线上部署时由 OpenResty/Nginx 接管同样的路径转发。
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    proxy: {
      // 后端 REST 接口代理，避免本地开发时出现跨域问题。
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // WebSocket 代理预留给消息/通知等实时能力。
      '/ws': {
        target: 'ws://localhost:8080',
        changeOrigin: true,
        ws: true
      }
    },
    cors: true
  },
  optimizeDeps: {
    include: ['echarts']
  },
  build: {
    target: 'es2020'
  }
})
