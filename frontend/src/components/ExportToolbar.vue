<template>
  <div class="export-toolbar">
    <el-button
      type="primary"
      :icon="DocumentIcon"
      :loading="exportingWord"
      :disabled="disabled"
      @click="$emit('export-word')"
      class="export-toolbar__btn"
    >
      <span class="export-toolbar__btn-text">
        {{ exportingWord ? '生成中...' : '导出 Word' }}
      </span>
      <span class="export-toolbar__btn-badge">后端生成</span>
    </el-button>

    <el-button
      type="success"
      :icon="Printer"
      :loading="exportingPdf"
      :disabled="disabled"
      @click="$emit('export-pdf')"
      class="export-toolbar__btn export-toolbar__btn--pdf"
    >
      <span class="export-toolbar__btn-text">
        {{ exportingPdf ? '导出中...' : '导出 PDF' }}
      </span>
      <span class="export-toolbar__btn-badge">前端直出</span>
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { Document as DocumentIcon, Printer } from '@element-plus/icons-vue'

defineProps<{
  disabled: boolean
  exportingWord: boolean
  exportingPdf: boolean
}>()

defineEmits<{
  (e: 'export-word'): void
  (e: 'export-pdf'): void
}>()
</script>

<style scoped>
.export-toolbar {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.export-toolbar__btn {
  position: relative;
  margin-left: 0 !important;
  width: 100%;
  padding: 12px 24px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 10px;
  overflow: hidden;
  transition: all var(--transition-normal);
}

.export-toolbar__btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.export-toolbar__btn--pdf {
  background: linear-gradient(135deg, #10b981, #059669) !important;
  border: none !important;
}

.export-toolbar__btn-text {
  margin-right: 6px;
}

.export-toolbar__btn-badge {
  font-size: 10px;
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 400;
}
</style>
