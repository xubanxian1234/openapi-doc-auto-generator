<template>
  <div ref="previewRef" class="doc-table__container" v-if="doc">
    <!-- 封面页 (供 PDF 导出使用) -->
    <div class="doc-table__cover-page">
      <div class="doc-table__cover-title">{{ doc.title || 'API 接口文档' }}</div>
      <div class="doc-table__cover-subtitle">接口集成说明</div>
    </div>

    <!-- 目录区 (TOC) -->
    <div class="doc-table__toc" v-if="groupedEndpoints.length > 0 || doc.description">
      <h2 class="doc-table__toc-title">目录</h2>
      <ul class="doc-table__toc-list">
        <li class="doc-table__toc-item-group doc-table__toc-item-endpoint" v-if="doc.description">
          <a href="#overview" class="doc-table__toc-link">1&nbsp;&nbsp;&nbsp;&nbsp;概述</a>
          <div class="doc-table__toc-leader"></div>
          <span class="doc-table__toc-page" data-target="overview"></span>
        </li>
        <li v-for="(group, gIdx) in groupedEndpoints" :key="'toc-g' + gIdx" class="doc-table__toc-item-group">
          <div class="doc-table__toc-item-endpoint">
            <a :href="'#tag-' + gIdx" class="doc-table__toc-link">{{ gIdx + 2 }}&nbsp;&nbsp;&nbsp;&nbsp;{{ group.tag }}</a>
            <div class="doc-table__toc-leader"></div>
            <span class="doc-table__toc-page" :data-target="'tag-' + gIdx"></span>
          </div>
          <ul class="doc-table__toc-sublist" v-if="group.endpoints.length > 0">
            <li v-for="(endpoint, eIdx) in group.endpoints" :key="'toc-e' + gIdx + '-' + eIdx" class="doc-table__toc-item-endpoint">
              <a :href="'#ep-' + gIdx + '-' + eIdx" class="doc-table__toc-link">
                {{ gIdx + 2 }}.{{ eIdx + 1 }}&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;{{ endpoint.summary || endpoint.path }}
              </a>
              <div class="doc-table__toc-leader"></div>
              <span class="doc-table__toc-page" :data-target="'ep-' + gIdx + '-' + eIdx"></span>
            </li>
          </ul>
        </li>
      </ul>
    </div>

    <!-- 第一章：概述 -->
    <div class="doc-table__group-section" id="overview" v-if="doc.description">
      <div class="doc-table__group-title">1. 概述</div>
      <div class="doc-table__business-desc" style="white-space: pre-wrap;">{{ doc.description }}</div>
    </div>

    <!-- 分组遍历端点 -->
    <div v-for="(group, gIdx) in groupedEndpoints" :key="'g' + gIdx" class="doc-table__group-section">
      <!-- 大分类标题 (例如：2. 项目管理) -->
      <div :id="'tag-' + gIdx" class="doc-table__group-title">{{ gIdx + 2 }}. {{ group.tag }}</div>

      <!-- 遍历组内端点 -->
      <div
        v-for="(endpoint, eIdx) in group.endpoints"
        :key="eIdx"
        :id="'ep-' + gIdx + '-' + eIdx"
        class="doc-table__endpoint-section"
      >
        <!-- 端点标题 (例如：2.1 创建项目) -->
        <div class="doc-table__endpoint-title">
          {{ gIdx + 2 }}.{{ eIdx + 1 }} {{ endpoint.summary || endpoint.path }}
          <span v-if="endpoint.deprecated" class="doc-table__deprecated-tag">[已废弃]</span>
        </div>

        <!-- 业务说明 -->
        <div class="doc-table__section-heading">业务说明</div>
      <div class="doc-table__business-desc">
        {{ endpoint.description || endpoint.summary || '无详细业务说明。' }}
      </div>

      <!-- 接口说明 -->
      <div class="doc-table__section-heading">接口说明</div>

      <!-- 单一的合并表格 -->
      <table class="doc-table__table">
        <colgroup>
          <col style="width: 25%;" />
          <col style="width: 20%;" />
          <col style="width: 55%;" />
        </colgroup>
        <tbody>
          <!-- 请求 URL 区域 -->
          <tr class="doc-table__row--url">
            <td colspan="3" class="doc-table__section-title">请求 URL</td>
          </tr>
          <tr>
            <td class="doc-table__cell-bold">URL</td>
            <td colspan="2">{{ endpoint.path }}</td>
          </tr>
          <tr>
            <td class="doc-table__cell-bold">method</td>
            <td colspan="2">{{ endpoint.method }}</td>
          </tr>
          <tr>
            <td class="doc-table__cell-bold">Content-type</td>
            <td colspan="2">{{ endpoint.contentType || 'application/json' }}</td>
          </tr>

          <!-- 请求头区域 -->
          <template v-if="endpoint.headers && endpoint.headers.length > 0">
            <tr class="doc-table__row--header">
              <td colspan="3" class="doc-table__section-title">请求头</td>
            </tr>
            <tr v-for="(header, hIdx) in endpoint.headers" :key="'h' + hIdx">
              <td class="doc-table__cell-bold">{{ header.name }}</td>
              <td colspan="2">{{ header.example || header.description || header.type || 'string' }}</td>
            </tr>
          </template>

          <!-- 请求参数区域 -->
          <template v-if="endpoint.requestFields && endpoint.requestFields.length > 0">
            <tr class="doc-table__row--request">
              <td colspan="3" class="doc-table__section-title">请求参数</td>
            </tr>
            <tr class="doc-table__row--col-header">
              <td>参数名</td>
              <td>类型</td>
              <td>描述</td>
            </tr>
            <tr
              v-for="(field, fIdx) in flattenFields(endpoint.requestFields)"
              :key="'req' + fIdx"
            >
              <td>
                <span class="doc-table__indent" v-if="field.depth > 0">
                  {{ indent(field.depth) }}└&nbsp;
                </span>
                <span class="doc-table__field-name">{{ field.name || '' }}</span>
              </td>
              <td>{{ field.type || '' }}</td>
              <td>
                <RichText :text="buildDescription(field)" />
              </td>
            </tr>
          </template>

          <!-- 返回参数区域 -->
          <template v-if="endpoint.responseFields && endpoint.responseFields.length > 0">
            <tr class="doc-table__row--response">
              <td colspan="3" class="doc-table__section-title">返回参数</td>
            </tr>
            <!-- 返回参数不带表头，直接列出数据 -->
            <tr
              v-for="(field, fIdx) in flattenFields(endpoint.responseFields)"
              :key="'res' + fIdx"
            >
              <td>
                <span class="doc-table__indent" v-if="field.depth > 0">
                  {{ indent(field.depth) }}└&nbsp;
                </span>
                <span class="doc-table__field-name">{{ field.name || '' }}</span>
              </td>
              <td>{{ field.type || '' }}</td>
              <td>
                <RichText :text="buildDescription(field)" />
              </td>
            </tr>
          </template>
        </tbody>
      </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, h, computed, defineComponent } from 'vue'
import type { ApiDocumentDTO, SchemaFieldDTO } from '../types/api'
import '../styles/doc-table.css'

const props = defineProps<{
  doc: ApiDocumentDTO | null
}>()

const previewRef = ref<HTMLElement | null>(null)

// ==================== 数据分组逻辑 ====================
interface EndpointGroup {
  tag: string
  endpoints: ApiEndpointDTO[]
}

const groupedEndpoints = computed<EndpointGroup[]>(() => {
  if (!props.doc || !props.doc.endpoints) return []
  
  const map = new Map<string, ApiEndpointDTO[]>()
  const groups: EndpointGroup[] = []
  
  for (const ep of props.doc.endpoints) {
    const tag = ep.tag || '默认分组'
    if (!map.has(tag)) {
      const arr: ApiEndpointDTO[] = []
      map.set(tag, arr)
      groups.push({ tag, endpoints: arr })
    }
    map.get(tag)!.push(ep)
  }
  
  return groups
})

const HIGHLIGHT_KEYWORDS = ['非必需', '默认值']

const RichText = defineComponent({
  props: {
    text: { type: String, default: '' },
  },
  setup(props) {
    return () => {
      if (!props.text) return h('span', '')

      const segments = splitByKeywords(props.text)
      const children = segments.map((seg) => {
        if (seg.highlighted) {
          return h('span', { class: 'doc-table__highlight' }, seg.text)
        }
        return seg.text
      })
      return h('span', children)
    }
  },
})

interface TextSegment {
  text: string
  highlighted: boolean
}

function splitByKeywords(text: string): TextSegment[] {
  const segments: TextSegment[] = []
  let remaining = text

  while (remaining.length > 0) {
    const match = findFirstKeyword(remaining)

    if (!match) {
      segments.push({ text: remaining, highlighted: false })
      break
    }

    if (match.index > 0) {
      segments.push({ text: remaining.substring(0, match.index), highlighted: false })
    }

    segments.push({ text: match.keyword, highlighted: true })
    remaining = remaining.substring(match.index + match.keyword.length)
  }

  return segments
}

function findFirstKeyword(text: string): { index: number; keyword: string } | null {
  let earliest: { index: number; keyword: string } | null = null

  for (const keyword of HIGHLIGHT_KEYWORDS) {
    const idx = text.indexOf(keyword)
    if (idx >= 0 && (earliest === null || idx < earliest.index)) {
      earliest = { index: idx, keyword }
    }
  }

  return earliest
}

function flattenFields(fields: SchemaFieldDTO[]): SchemaFieldDTO[] {
  const result: SchemaFieldDTO[] = []
  for (const field of fields) {
    addFieldRecursive(result, field)
  }
  return result
}

function addFieldRecursive(result: SchemaFieldDTO[], field: SchemaFieldDTO): void {
  if (field.name) {
    result.push(field)
  }
  if (field.children) {
    for (const child of field.children) {
      addFieldRecursive(result, child)
    }
  }
}

function indent(depth: number): string {
  return '\u00A0\u00A0\u00A0\u00A0'.repeat(depth)
}

function buildDescription(field: SchemaFieldDTO): string {
  const parts: string[] = []
  if (field.description) parts.push(field.description)
  if (field.enumValues && field.enumValues.length > 0) {
    parts.push('枚举值: ' + field.enumValues.join(', ') + '。')
  }
  
  // 将默认值和是否必需信息追加到描述末尾（配合着色）
  let extraInfo = ''
  if (field.defaultValue) {
    extraInfo += `默认值为 ${field.defaultValue}，`
  }
  extraInfo += field.required ? '' : '非必需'
  
  if (extraInfo) {
    // 确保与前面的描述有间隔
    if (parts.length > 0 && !parts[0].endsWith('。') && !parts[0].endsWith('，')) {
      parts.push('，')
    }
    parts.push(extraInfo + '。')
  }
  
  return parts.join('')
}

defineExpose({ previewRef })
</script>
