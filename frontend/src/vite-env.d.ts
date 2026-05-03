/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

// html2pdf.js 没有官方类型声明，手动声明模块
declare module 'html2pdf.js' {
  interface Html2PdfOptions {
    margin?: number | number[]
    filename?: string
    image?: { type?: string; quality?: number }
    html2canvas?: { scale?: number; useCORS?: boolean; logging?: boolean }
    jsPDF?: { unit?: string; format?: string; orientation?: string }
    pagebreak?: { mode?: string | string[] }
  }

  interface Html2PdfInstance {
    from(element: HTMLElement): Html2PdfInstance
    set(options: Html2PdfOptions): Html2PdfInstance
    save(): Promise<void>
    toPdf(): Html2PdfInstance
    output(type: string): Promise<any>
  }

  function html2pdf(): Html2PdfInstance
  export default html2pdf
}
