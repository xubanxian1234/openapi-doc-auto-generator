import { ref } from 'vue'
import { downloadWord } from '../api/openapi'
import { ElMessage } from 'element-plus'

/**
 * 导出功能 Composable。
 *
 * PDF 页码规则：
 * - 封面页和目录页不计入页码
 * - 页码从正文第一页开始，编号为 1
 * - 目录中的页码也是正文页码（不含封面/目录）
 */
export function useExport() {
  const exportingPdf = ref(false)
  const exportingWord = ref(false)

  function addChineseTextToPdf(pdf: any, text: string, x: number, y: number, align: 'left' | 'center' | 'right', fontSize: number, color: string) {
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    const scale = 4
    const pxSize = fontSize * scale
    ctx.font = `${pxSize}px "Microsoft YaHei", "PingFang SC", sans-serif`
    const metrics = ctx.measureText(text)
    canvas.width = metrics.width + 10
    canvas.height = pxSize * 1.5
    ctx.font = `${pxSize}px "Microsoft YaHei", "PingFang SC", sans-serif`
    ctx.fillStyle = color
    ctx.textBaseline = 'middle'
    ctx.fillText(text, 5, canvas.height / 2)
    const imgData = canvas.toDataURL('image/png')
    const mmHeight = (fontSize * 1.5) * 0.352778
    const mmWidth = mmHeight * (canvas.width / canvas.height)
    let finalX = x
    if (align === 'center') finalX = x - mmWidth / 2
    else if (align === 'right') finalX = x - mmWidth
    pdf.addImage(imgData, 'PNG', finalX, y, mmWidth, mmHeight)
  }

  /**
   * 获取元素相对于指定容器的绝对 offsetTop。
   */
  function getAbsoluteTop(el: HTMLElement, container: HTMLElement): number {
    let top = 0
    let curr: HTMLElement | null = el
    while (curr && curr !== container && curr !== document.body) {
      top += curr.offsetTop
      curr = curr.offsetParent as HTMLElement | null
    }
    return top
  }

  async function exportPdf(element: HTMLElement | null, fileName: string): Promise<void> {
    if (!element) {
      ElMessage.warning('没有可导出的内容')
      return
    }

    exportingPdf.value = true
    const pageSpans = element.querySelectorAll('.doc-table__toc-page')

    const marginTop = 20, marginBottom = 20, marginLeft = 10, marginRight = 10
    const usableWidthMm = 210 - marginLeft - marginRight   // 190mm
    const usableHeightMm = 297 - marginTop - marginBottom   // 257mm

    // html2pdf 将 element 的 offsetWidth 映射到 usableWidthMm
    const pxPerMm = element.offsetWidth / usableWidthMm
    const pageHeightPx = usableHeightMm * pxPerMm

    // 封面始终占 1 页 (page-break-after: always)
    // 目录占 ceil(tocHeight/pageHeight) 页 (page-break-after: always)
    const tocEl = element.querySelector('.doc-table__toc') as HTMLElement
    const tocPages = tocEl ? Math.max(1, Math.ceil(tocEl.scrollHeight / pageHeightPx)) : 0
    const prefacePages = 1 + tocPages  // 1(封面) + N(目录)

    // 正文的第一个元素，用作基准偏移
    const firstContent = element.querySelector('.doc-table__group-section') as HTMLElement
    const contentStartTop = firstContent ? getAbsoluteTop(firstContent, element) : 0

    // 计算并注入每个目录项的正文页码
    // 收集 TOC 项信息（稍后用于创建 PDF 内部链接）
    const tocItemInfos: { spanEl: Element; targetPage: number; yInToc: number }[] = []
    const tocStartTop = tocEl ? getAbsoluteTop(tocEl, element) : 0

    pageSpans.forEach((span) => {
      const targetId = span.getAttribute('data-target')
      if (!targetId) return
      const targetEl = document.getElementById(targetId)
      if (!targetEl) return

      // 正文页码：目标元素相对于正文起始位置的偏移
      const relativeOffset = getAbsoluteTop(targetEl, element) - contentStartTop
      const contentPage = Math.floor(Math.max(0, relativeOffset) / pageHeightPx) + 1
      span.textContent = String(contentPage)

      // 收集 TOC 项在目录区域内的 Y 位置（用于内部链接）
      const parentLi = span.closest('.doc-table__toc-item-endpoint') as HTMLElement
      if (parentLi && tocEl) {
        const yInToc = getAbsoluteTop(parentLi, element) - tocStartTop
        tocItemInfos.push({
          spanEl: span,
          targetPage: prefacePages + contentPage, // PDF 中的绝对页码
          yInToc,
        })
      }
    })

    try {
      const html2pdf = (await import('html2pdf.js')).default

      await html2pdf()
        .set({
          margin: [marginTop, marginRight, marginBottom, marginLeft],
          filename: `${fileName}.pdf`,
          image: { type: 'jpeg', quality: 0.98 },
          enableLinks: false,
          html2canvas: { scale: 2, useCORS: true, logging: false },
          jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
          pagebreak: { mode: ['css', 'legacy'] },
        })
        .from(element)
        .toPdf()
        .get('pdf')
        .then((pdf: any) => {
          const totalPages = pdf.internal.getNumberOfPages()
          const pageWidth = pdf.internal.pageSize.getWidth()
          const pageHeight = pdf.internal.pageSize.getHeight()
          const contentTotalPages = Math.max(1, totalPages - prefacePages)

          // --- 添加页眉页脚（仅正文页） ---
          for (let i = 1; i <= totalPages; i++) {
            pdf.setPage(i)
            if (i <= prefacePages) continue // 封面和目录页不加页眉页脚

            const contentPageNum = i - prefacePages
            pdf.setDrawColor(200, 200, 200)
            pdf.setLineWidth(0.3)
            // 页眉
            pdf.line(15, 14, pageWidth - 15, 14)
            addChineseTextToPdf(pdf, `${fileName} - 接口文档`, pageWidth - 15, 9, 'right', 9, '#888888')
            // 页脚
            pdf.line(15, pageHeight - 14, pageWidth - 15, pageHeight - 14)
            addChineseTextToPdf(pdf, `第 ${contentPageNum} 页 / 共 ${contentTotalPages} 页`, pageWidth / 2, pageHeight - 10, 'center', 9, '#888888')
          }

          // --- 添加 TOC 内部跳转链接 ---
          tocItemInfos.forEach((info) => {
            // 计算该 TOC 项在 PDF 第几页（目录从第 2 页开始）
            const yMm = info.yInToc / pxPerMm
            const tocPageOffset = Math.floor(yMm / usableHeightMm)
            const pdfTocPage = 2 + tocPageOffset  // 封面占第 1 页
            const yOnPage = marginTop + (yMm - tocPageOffset * usableHeightMm)

            if (pdfTocPage >= 1 && pdfTocPage <= totalPages && info.targetPage <= totalPages) {
              pdf.setPage(pdfTocPage)
              // 创建可点击区域，跳转到目标页
              pdf.link(marginLeft, yOnPage, usableWidthMm, 6, { pageNumber: info.targetPage })
            }
          })
        })
        .save()

      ElMessage.success('PDF 导出成功')
    } catch (error) {
      console.error('PDF 导出失败:', error)
      ElMessage.error('PDF 导出失败，请重试')
    } finally {
      exportingPdf.value = false
      pageSpans.forEach((span) => { span.textContent = '' })
    }
  }

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

  return { exportingPdf, exportingWord, exportPdf, exportWord }
}
