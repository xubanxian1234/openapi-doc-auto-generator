<template>
  <div class="json-uploader">
    <!-- 标题区 -->
    <div class="json-uploader__header">
      <el-icon :size="20" color="var(--color-primary-light)"><Document /></el-icon>
      <span class="json-uploader__title">数据源输入</span>
    </div>

    <!-- 上传区 -->
    <el-upload
      class="json-uploader__drop-zone"
      drag
      accept=".json"
      :auto-upload="false"
      :show-file-list="false"
      :on-change="handleFileChange"
    >
      <el-icon class="json-uploader__upload-icon" :size="40"><UploadFilled /></el-icon>
      <div class="json-uploader__upload-text">
        拖拽 JSON 文件到此处，或 <em>点击上传</em>
      </div>
      <div class="json-uploader__upload-hint">
        支持 OpenAPI 3.x 和 Swagger 2.0 规范的 JSON 文件
      </div>
    </el-upload>

    <!-- 分割线 -->
    <el-divider>
      <el-icon><Edit /></el-icon>
      <span style="margin-left: 6px;">或粘贴 JSON</span>
    </el-divider>

    <!-- JSON 文本区 -->
    <div class="json-uploader__textarea-wrapper">
      <el-input
        v-model="jsonContent"
        type="textarea"
        :rows="14"
        placeholder="在此粘贴 OpenAPI / Swagger JSON 内容..."
        resize="vertical"
        class="json-uploader__textarea"
      />
    </div>

    <!-- 操作按钮 -->
    <div class="json-uploader__actions">
      <el-button
        type="primary"
        :icon="Search"
        :loading="loading"
        :disabled="!jsonContent.trim()"
        size="large"
        @click="handleParse"
      >
        {{ loading ? '解析中...' : '解析文档' }}
      </el-button>

      <el-button
        :icon="Delete"
        size="large"
        @click="handleClear"
        :disabled="!jsonContent.trim()"
      >
        清空
      </el-button>

      <el-button
        :icon="Link"
        size="large"
        @click="loadSampleData"
      >
        加载示例
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Search, Delete, Link, Document, UploadFilled, Edit } from '@element-plus/icons-vue'
import type { UploadFile } from 'element-plus'

const emit = defineEmits<{
  (e: 'parse', jsonContent: string): void
}>()

const jsonContent = ref('')
const loading = ref(false)

/** 文件上传处理：读取文件内容到 textarea */
function handleFileChange(file: UploadFile) {
  if (!file.raw) return

  const reader = new FileReader()
  reader.onload = (e) => {
    const content = e.target?.result as string
    jsonContent.value = content
  }
  reader.readAsText(file.raw)
}

/** 触发解析 */
function handleParse() {
  if (!jsonContent.value.trim()) return
  emit('parse', jsonContent.value)
}

/** 清空输入 */
function handleClear() {
  jsonContent.value = ''
}

/** 加载示例数据（Petstore 精简版，方便快速体验） */
function loadSampleData() {
  jsonContent.value = JSON.stringify({
    "openapi": "3.0.1",
    "info": {
      "title": "Petstore 示例 API",
      "description": "这是一个 OpenAPI 3.0 示例文档，用于演示文档生成工具的功能。",
      "version": "1.0.0"
    },
    "servers": [{ "url": "https://petstore.example.com/api/v1" }],
    "paths": {
      "/pets": {
        "get": {
          "tags": ["Pets"],
          "summary": "获取所有宠物列表",
          "operationId": "listPets",
          "parameters": [
            { "name": "limit", "in": "query", "description": "返回的最大数量", "required": false, "schema": { "type": "integer", "format": "int32", "default": 20 } },
            { "name": "status", "in": "query", "description": "按状态筛选", "schema": { "type": "string", "enum": ["available", "sold", "pending"] } }
          ],
          "responses": {
            "200": {
              "description": "成功返回宠物列表",
              "content": {
                "application/json": {
                  "schema": { "type": "array", "items": { "$ref": "#/components/schemas/Pet" } }
                }
              }
            }
          }
        },
        "post": {
          "tags": ["Pets"],
          "summary": "新增一只宠物",
          "operationId": "createPet",
          "requestBody": {
            "required": true,
            "content": {
              "application/json": {
                "schema": { "$ref": "#/components/schemas/Pet" }
              }
            }
          },
          "responses": {
            "200": {
              "description": "创建成功",
              "content": {
                "application/json": {
                  "schema": { "$ref": "#/components/schemas/Pet" }
                }
              }
            }
          }
        }
      },
      "/pets/{petId}": {
        "get": {
          "tags": ["Pets"],
          "summary": "根据 ID 查询单只宠物",
          "operationId": "getPetById",
          "parameters": [
            { "name": "petId", "in": "path", "required": true, "description": "宠物的唯一标识符", "schema": { "type": "string" } }
          ],
          "responses": {
            "200": {
              "description": "成功返回宠物详情",
              "content": {
                "application/json": {
                  "schema": { "$ref": "#/components/schemas/Pet" }
                }
              }
            }
          }
        }
      }
    },
    "components": {
      "schemas": {
        "Pet": {
          "type": "object",
          "required": ["name", "species"],
          "properties": {
            "id": { "type": "integer", "format": "int64", "description": "宠物唯一 ID，系统自动生成" },
            "name": { "type": "string", "description": "宠物名称" },
            "species": { "type": "string", "description": "物种", "enum": ["dog", "cat", "bird", "fish"] },
            "age": { "type": "integer", "description": "年龄（岁）", "default": 1 },
            "owner": { "$ref": "#/components/schemas/Owner" },
            "tags": { "type": "array", "items": { "type": "string" }, "description": "标签列表" }
          }
        },
        "Owner": {
          "type": "object",
          "required": ["name"],
          "properties": {
            "name": { "type": "string", "description": "主人姓名" },
            "email": { "type": "string", "format": "email", "description": "联系邮箱" },
            "phone": { "type": "string", "description": "联系电话" }
          }
        }
      }
    }
  }, null, 2)
}

/** 暴露 loading 和 jsonContent 供父组件使用 */
defineExpose({ loading, jsonContent })
</script>

<style scoped>
.json-uploader {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.json-uploader__header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.json-uploader__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.json-uploader__drop-zone :deep(.el-upload-dragger) {
  background: var(--bg-input);
  border: 2px dashed var(--border-color);
  border-radius: var(--border-radius);
  transition: all var(--transition-normal);
  padding: 24px;
}

.json-uploader__drop-zone :deep(.el-upload-dragger:hover) {
  border-color: var(--color-primary);
  background: rgba(99, 102, 241, 0.05);
}

.json-uploader__upload-icon {
  color: var(--text-muted);
  margin-bottom: 8px;
}

.json-uploader__upload-text {
  color: var(--text-secondary);
  font-size: 14px;
}

.json-uploader__upload-text em {
  color: var(--color-primary-light);
  font-style: normal;
}

.json-uploader__upload-hint {
  color: var(--text-muted);
  font-size: 12px;
  margin-top: 4px;
}

.json-uploader__textarea :deep(.el-textarea__inner) {
  background: var(--bg-input);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  transition: border-color var(--transition-normal);
}

.json-uploader__textarea :deep(.el-textarea__inner:focus) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.15);
}

.json-uploader__actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.json-uploader__actions .el-button {
  margin-left: 0 !important;
  width: 100%;
}

:deep(.el-divider__text) {
  background: var(--bg-card);
  color: var(--text-muted);
  display: flex;
  align-items: center;
  font-size: 13px;
}

:deep(.el-divider) {
  border-color: var(--border-color);
}
</style>
