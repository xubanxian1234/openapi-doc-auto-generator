import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * Vite 构建配置。
 *
 * - 开发模式: proxy 将 /api 转发到后端 Spring Boot (避免 CORS)
 * - 生产构建: 输出到 dist/，由 Maven 拷贝到 Spring Boot static 目录
 */
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
  },
})
