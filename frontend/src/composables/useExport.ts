import { ref } from 'vue'
import { downloadWord } from '../api/openapi'
import { ElMessage } from 'element-plus'

/**
 * 导出功能 Composable。
 *
 * 封装 PDF（前端 html2pdf.js 直出）和 Word（后端 POI 生成）两种导出逻辑。
 * 将导出状态（loading）和错误处理统一管理。
 */
export function useExport() {
  const exportingPdf = ref(false)
  const exportingWord = ref(false)

  /**
   * PDF 导出：将指定 DOM 元素转换为 PDF 并触发下载。
   *
   * 选择 html2pdf.js 而非 jsPDF 的原因：
   * html2pdf.js 基于 html2canvas + jsPDF，能直接将 DOM 渲染为 PDF，
   * 无需手动绘制每个元素，适合高保真还原复杂表格样式。
   *
   * @param element 要导出的 DOM 元素（高保真表格容器）
   * @param fileName 输出文件名（不含扩展名）
   */
  async function exportPdf(element: HTMLElement | null, fileName: string): Promise<void> {
    if (!element) {
      ElMessage.warning('没有可导出的内容')
      return
    }

    exportingPdf.value = true
    try {
      // 动态导入 html2pdf.js（减少首屏加载体积）
      const html2pdf = (await import('html2pdf.js')).default

      await html2pdf()
        .set({
          margin: [10, 10, 10, 10],
          filename: `${fileName}.pdf`,
          image: { type: 'jpeg', quality: 0.98 },
          html2canvas: {
            scale: 2,         // 2x 渲染保证清晰度
            useCORS: true,
            logging: false,
          },
          jsPDF: {
            unit: 'mm',
            format: 'a4',
            orientation: 'portrait',
          },
          pagebreak: { mode: ['avoid-all', 'css', 'legacy'] },
        })
        .from(element)
        .save()

      ElMessage.success('PDF 导出成功')
    } catch (error) {
      console.error('PDF 导出失败:', error)
      ElMessage.error('PDF 导出失败，请重试')
    } finally {
      exportingPdf.value = false
    }
  }

  /**
   * Word 导出：将 JSON 发送到后端生成 .docx 并下载。
   *
   * @param jsonContent OpenAPI JSON 原始字符串
   * @param fileName 输出文件名（不含扩展名）
   */
  async function exportWord(jsonContent: string, fileName: string): Promise<void> {
    if (!jsonContent) {
      ElMessage.warning('请先输入 OpenAPI JSON')
      return
    }

    exportingWord.value = true
    try {
      await downloadWord(jsonContent, `${fileName}.docx`)
      ElMessage.success('Word 导出成功')
    } catch (error) {
      console.error('Word 导出失败:', error)
      ElMessage.error('Word 导出失败，请检查后端服务是否运行')
    } finally {
      exportingWord.value = false
    }
  }

  return {
    exportingPdf,
    exportingWord,
    exportPdf,
    exportWord,
  }
}
