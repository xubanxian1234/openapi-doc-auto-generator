<template>
  <el-drawer
    v-model="visible"
    title="📖 使用指南"
    direction="rtl"
    size="480px"
    :append-to-body="true"
  >
    <div class="help-drawer">
      <!-- 系统简介 -->
      <section class="help-drawer__section">
        <h3 class="help-drawer__heading">🎯 系统简介</h3>
        <p>
          OpenAPI 文档生成器是一款<strong>开箱即用</strong>的接口文档工具。
          它可以将标准的 OpenAPI 3.x 或 Swagger 2.0 规范 JSON 自动解析，
          生成可视化的文档预览，并支持一键导出为 <strong>Word (.docx)</strong>
          或 <strong>PDF</strong> 格式。
        </p>
        <p>
          整个应用打包为单个 JAR 文件，双击运行即可自动启动浏览器，
          无需任何额外配置或依赖。
        </p>
      </section>

      <el-divider />

      <!-- 数据源获取 -->
      <section class="help-drawer__section">
        <h3 class="help-drawer__heading">📋 数据源获取指导</h3>
        <h4>方式一：从 Swagger UI 获取</h4>
        <ol>
          <li>打开你的项目 Swagger UI 页面（通常是 <code>/swagger-ui.html</code>）</li>
          <li>在页面顶部找到 OpenAPI 规范链接（如 <code>/v3/api-docs</code>）</li>
          <li>点击链接，浏览器会显示 JSON 内容</li>
          <li>全选复制（<kbd>Ctrl+A</kbd>, <kbd>Ctrl+C</kbd>）</li>
          <li>粘贴到本工具的输入区域</li>
        </ol>
        <h4>方式二：从文件上传</h4>
        <ol>
          <li>如果你有 <code>openapi.json</code> 或 <code>swagger.json</code> 文件</li>
          <li>直接拖拽到上传区域，或点击上传按钮选择文件</li>
        </ol>
        <h4>方式三：从 URL 下载</h4>
        <p>
          在浏览器中访问 API 的 OpenAPI JSON 端点地址，
          将返回的 JSON 内容复制到本工具即可。
          例如 Onshape API 的地址为：
          <code>https://cad.onshape.com/api/openapi</code>
        </p>
      </section>

      <el-divider />

      <!-- 操作流程 -->
      <section class="help-drawer__section">
        <h3 class="help-drawer__heading">🔧 操作流程</h3>
        <ol>
          <li><strong>输入 JSON</strong> — 粘贴、上传文件，或点击「加载示例」快速体验</li>
          <li><strong>解析文档</strong> — 点击「解析文档」按钮，系统将解析 JSON 并渲染预览</li>
          <li><strong>查看预览</strong> — 右侧会展示高保真的文档表格预览</li>
          <li><strong>导出文档</strong> — 选择导出 Word 或 PDF</li>
        </ol>
      </section>

      <el-divider />

      <!-- Word vs PDF 差异 -->
      <section class="help-drawer__section">
        <h3 class="help-drawer__heading">📄 Word / PDF 产物差异说明</h3>
        <el-table :data="comparisonData" border style="width: 100%">
          <el-table-column prop="feature" label="特性" width="120" />
          <el-table-column prop="word" label="Word (.docx)" />
          <el-table-column prop="pdf" label="PDF" />
        </el-table>
      </section>

      <el-divider />

      <!-- 技术说明 -->
      <section class="help-drawer__section">
        <h3 class="help-drawer__heading">⚙️ 技术说明</h3>
        <ul>
          <li><strong>Word 生成</strong>：后端使用 Apache POI 纯 Java 代码渲染，无预设模板</li>
          <li><strong>PDF 生成</strong>：前端使用 html2pdf.js 将预览区 DOM 直接转换</li>
          <li><strong>Schema 解析</strong>：支持 <code>$ref</code> 引用、嵌套对象、数组等复杂结构</li>
          <li><strong>循环引用保护</strong>：自动检测并阻断循环引用，最大递归深度 10 层</li>
        </ul>
      </section>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const visible = ref(false)

const comparisonData = ref([
  { feature: '生成方式', word: '后端 Apache POI 生成', pdf: '前端 html2pdf.js 直出' },
  { feature: '样式精度', word: '高（POI 原生控制）', pdf: '极高（DOM 截图）' },
  { feature: '可编辑性', word: '✅ 可编辑', pdf: '❌ 不可编辑' },
  { feature: '文件大小', word: '较小（10-100KB）', pdf: '较大（含图片渲染）' },
  { feature: '适用场景', word: '需要二次编辑、团队协作', pdf: '归档、打印、分享' },
  { feature: '网络依赖', word: '需要后端服务运行', pdf: '纯前端，无需网络' },
])

function open() {
  visible.value = true
}

defineExpose({ open })
</script>

<style scoped>
.help-drawer {
  padding: 0 8px;
  color: var(--text-primary);
  line-height: 1.7;
}

.help-drawer__section {
  margin-bottom: 8px;
}

.help-drawer__heading {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--color-primary-light);
}

.help-drawer h4 {
  font-size: 14px;
  font-weight: 500;
  margin: 12px 0 6px 0;
  color: var(--text-primary);
}

.help-drawer p {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.help-drawer ol, .help-drawer ul {
  font-size: 13px;
  color: var(--text-secondary);
  padding-left: 20px;
  margin-bottom: 8px;
}

.help-drawer li {
  margin-bottom: 4px;
}

.help-drawer code {
  background: var(--bg-input);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: var(--color-primary-light);
}

.help-drawer kbd {
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  border-radius: 3px;
  padding: 1px 5px;
  font-size: 11px;
  font-family: inherit;
}
</style>
