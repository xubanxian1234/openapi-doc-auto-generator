<template>
  <div class="app">
    <!-- ==================== 顶部导航栏 ==================== -->
    <header class="app__header">
      <div class="app__header-left">
        <div class="app__logo">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/>
            <polyline points="10 9 9 9 8 9"/>
          </svg>
        </div>
        <div class="app__title-group">
          <h1 class="app__title">OpenAPI 文档生成器</h1>
          <span class="app__subtitle">接口文档自动生成与转换工具</span>
        </div>
      </div>

      <div class="app__header-right">
        <el-button
          text
          :icon="QuestionFilled"
          @click="openHelp"
          class="app__help-btn"
        >
          使用指南
        </el-button>
      </div>
    </header>

    <!-- ==================== 主内容区 ==================== -->
    <main class="app__main">
      <!-- 左侧：输入区 + 导出工具栏 -->
      <aside class="app__sidebar fade-in-up">
        <div class="app__card">
          <JsonUploader ref="uploaderRef" @parse="handleParse" />
        </div>

        <div class="app__card app__export-card" v-if="docData">
          <div class="app__card-header">
            <el-icon :size="18" color="var(--color-primary-light)"><Download /></el-icon>
            <span>导出文档</span>
          </div>
          <ExportToolbar
            :disabled="!docData"
            :exporting-word="exportingWord"
            :exporting-pdf="exportingPdf"
            @export-word="handleExportWord"
            @export-pdf="handleExportPdf"
          />
        </div>

        <!-- 解析统计信息 -->
        <div class="app__card app__stats-card" v-if="docData">
          <div class="app__stats">
            <div class="app__stat-item">
              <span class="app__stat-value">{{ docData.endpoints?.length || 0 }}</span>
              <span class="app__stat-label">接口总数</span>
            </div>
            <div class="app__stat-item">
              <span class="app__stat-value">{{ tagCount }}</span>
              <span class="app__stat-label">分组数</span>
            </div>
            <div class="app__stat-item">
              <span class="app__stat-value">{{ docData.specVersion || '-' }}</span>
              <span class="app__stat-label">规范版本</span>
            </div>
          </div>
        </div>
      </aside>

      <!-- 右侧：预览区 -->
      <section class="app__preview">
        <!-- 空状态 -->
        <div v-if="!docData" class="app__empty-state fade-in-up">
          <div class="app__empty-icon">
            <svg viewBox="0 0 24 24" width="64" height="64" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round" opacity="0.3">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
            </svg>
          </div>
          <h2 class="app__empty-title">等待输入数据</h2>
          <p class="app__empty-desc">
            请在左侧粘贴 OpenAPI JSON 或上传文件，<br/>
            然后点击「解析文档」查看预览
          </p>
        </div>

        <!-- 文档预览 -->
        <div v-else class="app__preview-content fade-in-up">
          <div class="app__preview-header">
            <h2 class="app__preview-title">
              <el-icon><View /></el-icon>
              文档预览
            </h2>
            <el-tag type="success" size="small">已解析</el-tag>
          </div>

          <div class="app__preview-scroll">
            <ApiDocPreview ref="previewRef" :doc="docData" />
          </div>
        </div>
      </section>
    </main>

    <!-- 帮助抽屉 -->
    <HelpDrawer ref="helpRef" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { QuestionFilled, Download, View } from '@element-plus/icons-vue'
import JsonUploader from './components/JsonUploader.vue'
import ApiDocPreview from './components/ApiDocPreview.vue'
import ExportToolbar from './components/ExportToolbar.vue'
import HelpDrawer from './components/HelpDrawer.vue'
import { parseOpenApi } from './api/openapi'
import { useExport } from './composables/useExport'
import type { ApiDocumentDTO } from './types/api'

// ==================== 状态 ====================
const docData = ref<ApiDocumentDTO | null>(null)
const uploaderRef = ref<InstanceType<typeof JsonUploader> | null>(null)
const previewRef = ref<InstanceType<typeof ApiDocPreview> | null>(null)
const helpRef = ref<InstanceType<typeof HelpDrawer> | null>(null)

const { exportingPdf, exportingWord, exportPdf, exportWord } = useExport()

/** 统计分组数（去重的 tag 数量） */
const tagCount = computed(() => {
  if (!docData.value?.endpoints) return 0
  const tags = new Set(docData.value.endpoints.map((e) => e.tag))
  return tags.size
})

// ==================== 事件处理 ====================

/** 解析 JSON */
async function handleParse(jsonContent: string) {
  const uploader = uploaderRef.value
  if (!uploader) return

  uploader.loading = true
  try {
    docData.value = await parseOpenApi(jsonContent)
    ElMessage.success(`解析成功！共 ${docData.value.endpoints?.length || 0} 个接口`)
  } catch (error: any) {
    const msg = error.response?.data?.message || error.message || '解析失败'
    ElMessage.error('解析失败: ' + msg)
    console.error('Parse error:', error)
  } finally {
    uploader.loading = false
  }
}

/** 导出 Word */
function handleExportWord() {
  const jsonContent = uploaderRef.value?.jsonContent
  if (!jsonContent) return

  const fileName = docData.value?.title || 'api-doc'
  exportWord(jsonContent, fileName)
}

/** 导出 PDF */
function handleExportPdf() {
  const element = previewRef.value?.previewRef
  const fileName = docData.value?.title || 'api-doc'
  exportPdf(element ?? null, fileName)
}

/** 打开帮助 */
function openHelp() {
  helpRef.value?.open()
}
</script>

<style scoped>
/* ==================== 布局 ==================== */
.app {
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* ==================== 顶部栏 ==================== */
.app__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: var(--gradient-card);
  border-bottom: 1px solid var(--border-color);
  backdrop-filter: blur(10px);
  position: sticky;
  top: 0;
  z-index: 100;
}

.app__header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app__logo {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-primary);
  border-radius: 10px;
  color: white;
  box-shadow: var(--shadow-glow);
}

.app__title-group {
  display: flex;
  flex-direction: column;
}

.app__title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
  margin: 0;
}

.app__subtitle {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 1px;
}

.app__help-btn {
  color: var(--text-secondary) !important;
  font-size: 13px;
}

.app__help-btn:hover {
  color: var(--color-primary-light) !important;
}

/* ==================== 主内容区 ==================== */
.app__main {
  flex: 1;
  display: flex;
  gap: 0;
  overflow: hidden;
}

/* ==================== 左侧边栏 ==================== */
.app__sidebar {
  width: 420px;
  min-width: 380px;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  border-right: 1px solid var(--border-color);
  background: rgba(26, 29, 39, 0.5);
}

.app__card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  padding: 20px;
  transition: all var(--transition-normal);
}

.app__card:hover {
  border-color: rgba(99, 102, 241, 0.3);
  box-shadow: var(--shadow-sm);
}

.app__card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 14px;
}

/* ==================== 统计卡片 ==================== */
.app__stats {
  display: flex;
  gap: 12px;
}

.app__stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 8px;
  background: var(--bg-surface);
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.app__stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary-light);
}

.app__stat-label {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}

/* ==================== 预览区 ==================== */
.app__preview {
  flex: 1;
  overflow-y: auto;
  background: var(--bg-app);
  display: flex;
  flex-direction: column;
}

.app__empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  padding: 40px;
}

.app__empty-icon {
  margin-bottom: 20px;
  animation: pulse-glow 3s ease-in-out infinite;
  padding: 20px;
  border-radius: 50%;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
}

.app__empty-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.app__empty-desc {
  font-size: 14px;
  text-align: center;
  line-height: 1.7;
  color: var(--text-muted);
}

.app__preview-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.app__preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-card);
}

.app__preview-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.app__preview-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f5f5;
}

/* ==================== 响应式 ==================== */
@media (max-width: 900px) {
  .app__main {
    flex-direction: column;
  }

  .app__sidebar {
    width: 100%;
    min-width: auto;
    border-right: none;
    border-bottom: 1px solid var(--border-color);
  }
}
</style>
