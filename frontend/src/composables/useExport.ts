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
   * 辅助方法：将包含中文的文本通过 Canvas 渲染为图片后添加到 jsPDF 中
   * 解决 jsPDF 默认不支持中文字体导致的乱码问题
   */
  function addChineseTextToPdf(pdf: any, text: string, x: number, y: number, align: 'left' | 'center' | 'right', fontSize: number, color: string) {
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    // 使用高分辨率渲染保证清晰度
    const scale = 4
    const pxSize = fontSize * scale
    ctx.font = `${pxSize}px "Microsoft YaHei", "PingFang SC", sans-serif`
    
    const metrics = ctx.measureText(text)
    canvas.width = metrics.width + 10 // buffer
    canvas.height = pxSize * 1.5

    // 重新设置 font，因为改变 canvas 宽高会重置 context
    ctx.font = `${pxSize}px "Microsoft YaHei", "PingFang SC", sans-serif`
    ctx.fillStyle = color
    ctx.textBaseline = 'middle'
    ctx.fillText(text, 5, canvas.height / 2)

    const imgData = canvas.toDataURL('image/png')
    
    // 保持与 canvas 真实的物理尺寸比例一致，防止被挤压变扁
    // canvas 高度是 pxSize * 1.5，所以 mmHeight 也要按 1.5 倍的 fontSize 来算
    const mmHeight = (fontSize * 1.5) * 0.352778
    // 宽度根据高度和 canvas 的原始比例计算
    const mmWidth = mmHeight * (canvas.width / canvas.height)

    let finalX = x
    if (align === 'center') {
      finalX = x - mmWidth / 2
    } else if (align === 'right') {
      finalX = x - mmWidth
    }

    pdf.addImage(imgData, 'PNG', finalX, y, mmWidth, mmHeight)
  }

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
          margin: [20, 10, 20, 10],
          filename: `${fileName}.pdf`,
          image: { type: 'jpeg', quality: 0.98 },
          enableLinks: true,
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
        .toPdf()
        .get('pdf')
        .then((pdf: any) => {
          const totalPages = pdf.internal.getNumberOfPages()
          const pageWidth = pdf.internal.pageSize.width || pdf.internal.pageSize.getWidth()
          const pageHeight = pdf.internal.pageSize.height || pdf.internal.pageSize.getHeight()

          for (let i = 1; i <= totalPages; i++) {
            pdf.setPage(i)
            
            // 绘制页眉分割线
            pdf.setDrawColor(220, 220, 220)
            pdf.setLineWidth(0.5)
            pdf.line(15, 12, pageWidth - 15, 12)

            // 添加页眉 (通过 Canvas 解决中文乱码)
            addChineseTextToPdf(pdf, `${fileName} - 接口文档`, pageWidth - 15, 8, 'right', 9, '#888888')

            // 绘制页脚分割线
            pdf.line(15, pageHeight - 12, pageWidth - 15, pageHeight - 12)
            
            // 添加页脚 (居中显示)
            const pageInfo = `第 ${i} 页 / 共 ${totalPages} 页`
            addChineseTextToPdf(pdf, pageInfo, pageWidth / 2, pageHeight - 9, 'center', 9, '#888888')
          }
        })
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
