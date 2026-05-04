import { ref } from 'vue'
import { downloadWord } from '../api/openapi'
import { ElMessage } from 'element-plus'

/**
 * 导出功能 Composable。
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

  async function exportPdf(element: HTMLElement | null, fileName: string): Promise<void> {
    if (!element) {
      ElMessage.warning('没有可导出的内容')
      return
    }

    exportingPdf.value = true
    const pageSpans = element.querySelectorAll('.doc-table__toc-page')
    const tocLinks = element.querySelectorAll('.doc-table__toc-link')
    
    // 移除 href 防止 PDF 生成原生但错乱的跳转
    const savedHrefs: { el: Element; href: string }[] = []
    tocLinks.forEach((link) => {
      const href = link.getAttribute('href')
      if (href) {
        savedHrefs.push({ el: link, href })
        link.removeAttribute('href')
      }
    })

    const marginTop = 20, marginBottom = 20, marginLeft = 10, marginRight = 10
    const usableWidthMm = 210 - marginLeft - marginRight
    const usableHeightMm = 297 - marginTop - marginBottom

    const pxPerMm = element.offsetWidth / usableWidthMm
    const pageHeightPx = usableHeightMm * pxPerMm

    // --- 模拟 html2pdf 的分页算法 ---
    // 通过遍历容器子元素，精确计算出它们在 PDF 中的绝对页码
    let currentAbsolutePage = 1
    let currentY = 0
    const targetAbsolutePages: Record<string, number> = {}
    
    // 封面页（必定占满 1 页并触发换页）
    const coverEl = element.querySelector('.doc-table__cover-page') as HTMLElement
    if (coverEl) {
      currentAbsolutePage++
      currentY = 0
    }

    // 目录页（由于可能有多个，计算实际高度）
    const tocEl = element.querySelector('.doc-table__toc') as HTMLElement
    let prefacePages = 1
    if (tocEl) {
      const tocHeight = tocEl.offsetHeight
      const tocPagesNeeded = Math.max(1, Math.ceil(tocHeight / pageHeightPx))
      currentAbsolutePage += tocPagesNeeded
      currentY = 0
      prefacePages += tocPagesNeeded
    }

    // 遍历正文，由于我们在 CSS 中对部分元素应用了 page-break-inside: avoid
    // 这里需要模拟这些换页逻辑
    const groupSections = element.querySelectorAll('.doc-table__group-section')
    groupSections.forEach((groupSec) => {
      // 检查 groupSec (如果遇到大章节，记录它的页码)
      const groupId = groupSec.getAttribute('id')
      if (groupId) {
        // html2pdf 不会让大段落整个 avoid，但里面的 endpoint-title 等会 avoid
        targetAbsolutePages[groupId] = currentAbsolutePage
      }
      
      const children = Array.from(groupSec.children) as HTMLElement[]
      children.forEach((child) => {
        const childHeight = child.offsetHeight
        const style = window.getComputedStyle(child)
        const isAvoid = style.pageBreakInside === 'avoid' || style.breakInside === 'avoid'

        if (isAvoid) {
          // 如果当前页剩余空间放不下这个元素，就换到下一页
          if (currentY + childHeight > pageHeightPx && currentY > 0) {
            currentAbsolutePage++
            currentY = 0
          }
        } else {
          // 元素太大，可能会跨多页
          if (currentY + childHeight > pageHeightPx) {
            const overflow = (currentY + childHeight) - pageHeightPx
            const addedPages = Math.ceil(overflow / pageHeightPx)
            currentAbsolutePage += addedPages
            currentY = overflow % pageHeightPx
            // 注意：跨页的元素起始页还是 currentAbsolutePage - addedPages
            // 但计算结束后会留在新页的 currentY
          } else {
            currentY += childHeight
          }
        }

        // 记录子锚点
        const childId = child.getAttribute('id')
        if (childId) {
          targetAbsolutePages[childId] = currentAbsolutePage
        }
      })
    })

    // 回填页码
    pageSpans.forEach((span) => {
      const targetId = span.getAttribute('data-target')
      if (!targetId) return
      const absPage = targetAbsolutePages[targetId]
      if (absPage) {
        const contentPage = Math.max(1, absPage - prefacePages)
        span.textContent = String(contentPage)
      } else {
        span.textContent = "1"
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
            if (i <= prefacePages) continue

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
        })
        .save()

      ElMessage.success('PDF 导出成功')
    } catch (error) {
      console.error('PDF 导出失败:', error)
      ElMessage.error('PDF 导出失败，请重试')
    } finally {
      exportingPdf.value = false
      pageSpans.forEach((span) => { span.textContent = '' })
      savedHrefs.forEach(({ el, href }) => { el.setAttribute('href', href) })
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
