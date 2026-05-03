/**
 * 前端 TypeScript 类型定义。
 * 与后端 DTO 一一对应，确保前后端数据契约一致。
 */

/** 顶层文档数据 */
export interface ApiDocumentDTO {
  title: string
  version: string
  description: string
  baseUrl: string
  specVersion: string
  endpoints: ApiEndpointDTO[]
}

/** 单个 API 端点 */
export interface ApiEndpointDTO {
  tag: string
  method: string
  path: string
  summary: string
  description: string
  operationId: string
  deprecated: boolean
  contentType: string | null
  headers: ParameterDTO[]
  requestFields: SchemaFieldDTO[]
  responseFields: SchemaFieldDTO[]
}

/** 请求头/简单参数 */
export interface ParameterDTO {
  name: string
  in: string
  description: string
  required: boolean
  type: string
  defaultValue: string | null
  example: string | null
}

/** Schema 字段（树形结构，支持递归嵌套） */
export interface SchemaFieldDTO {
  name: string | null
  type: string
  format: string | null
  description: string | null
  required: boolean
  defaultValue: string | null
  example: string | null
  enumValues: string[] | null
  depth: number
  children: SchemaFieldDTO[] | null
  schemaRef: string | null
}
