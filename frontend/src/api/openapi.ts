import axios from 'axios'
import type { ApiDocumentDTO } from '../types/api'

/**
 * Axios 实例。
 * 开发环境通过 Vite proxy 转发 /api → localhost:8080，
 * 生产环境前后端同源（Fat JAR 托管），无需额外配置。
 */
const http = axios.create({
  baseURL: '/',
  timeout: 60000, // OpenAPI JSON 可能很大，给足超时时间
})

/**
 * 解析 OpenAPI JSON，返回结构化 DTO（前端渲染用）。
 */
export async function parseOpenApi(jsonContent: string): Promise<ApiDocumentDTO> {
  const response = await http.post<ApiDocumentDTO>('/api/parse', {
    content: jsonContent,
  })
  return response.data
}

/**
 * 解析 OpenAPI JSON 并下载 Word 文档。
 * 后端返回 .docx 文件流，前端通过 Blob + URL.createObjectURL 触发下载。
 */
export async function downloadWord(jsonContent: string, fileName: string): Promise<void> {
  const response = await http.post('/api/parse-to-word', {
    content: jsonContent,
  }, {
    responseType: 'blob',
  })

  const blob = new Blob([response.data], {
    type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  })

  triggerDownload(blob, fileName)
}

/**
 * 通过创建隐藏的 <a> 标签触发浏览器下载。
 */
function triggerDownload(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
