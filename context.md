# 项目上下文 — OpenAPI 接口文档自动生成与转换工具

## 技术选型

| 层次 | 技术 | 版本 |
|------|------|------|
| JDK | Java | 11 |
| 构建 | Maven 多模块 | 3.8+ |
| 后端 | Spring Boot | 2.7.18 |
| OpenAPI 解析 | swagger-parser | 2.1.22 |
| Word 生成 | Apache POI (poi-ooxml) | 5.2.5 |
| 前端框架 | Vue 3 (Composition API + TS) | 3.x |
| 构建工具 | Vite | 5.x |
| UI 库 | Element Plus | 2.x |
| PDF 导出 | html2pdf.js | 0.10.x |
| 打包插件 | frontend-maven-plugin | 1.15.1 |
| 包管理 | npm | — |

## OpenAPI 版本支持

- OpenAPI 3.0.x ✅
- Swagger 2.0 ✅ (swagger-parser 自动检测并统一转换为 OAS3 内部模型)

## 当前开发进度

| 阶段 | 状态 | 说明 |
|------|------|------|
| 第一步：初始化 AI 大脑 (context.md) | ✅ 完成 | 当前文件 |
| 第二步：工程化基石 (pom.xml + 启动类) | ✅ 完成 | 3个 pom.xml + 启动类 + 端口探测 + application.yml |
| 第三步：后端核心逻辑 | ✅ 完成 | DTO模型 + Strategy解析 + Builder/Factory Word生成 + Controller |
| 第四步：前端与交互 | ✅ 完成 | Vue3+TS 全套组件 + CSS + 导出逻辑 + 帮助手册 |

## 核心模块职责分配

### 后端模块 (backend) — 已完成
- **controller/OpenApiController**: REST 接口 /api/parse + /api/parse-to-word
- **model**: ApiDocumentDTO, ApiEndpointDTO, ParameterDTO, SchemaFieldDTO
- **service/OpenApiParseService**: swagger-parser 解析主服务
- **service/strategy**: SchemaParseStrategy + 4个策略实现 + SchemaStrategyFactory
- **word/WordDocumentBuilder**: Builder 模式链式组装 Word
- **word/WordGenerationService**: DTO → byte[] 转换服务
- **word/factory**: TitleSectionFactory, EndpointTableFactory, ParameterTableFactory
- **word/style/WordStyleConstants**: 集中管理所有样式常量
- **config/SmartPortConfig**: 端口探测工具类

### 前端模块 (frontend) — 已完成
- **JsonUploader.vue**: JSON 粘贴 / 文件上传 / 示例数据加载
- **ApiDocPreview.vue**: 原生 table 高保真渲染 + RichText 关键词着色
- **ExportToolbar.vue**: Word/PDF 导出按钮
- **HelpDrawer.vue**: Element Plus 抽屉 + Markdown 使用指南
- **App.vue**: 主页面布局 (暗色主题 + 响应式)
- **useExport.ts**: PDF(html2pdf.js) / Word(Blob下载) 组合式函数
- **api/openapi.ts**: Axios API 封装
- **types/api.ts**: TS 类型定义
- **styles/global.css**: 暗色主题设计系统
- **styles/doc-table.css**: CSS 隔离的高保真表格样式

## 关键设计决策

1. **策略模式 (Strategy)** — Schema 解析：SchemaParseStrategy 接口 + 4 个实现 (Object/Array/Primitive/Ref)
2. **建造者模式 (Builder)** — Word 构建：WordDocumentBuilder 链式 API
3. **工厂模式 (Factory)** — Word 区块：TitleSectionFactory / EndpointTableFactory / ParameterTableFactory
4. **模板方法 (Template Method)** — WordSectionFactory 抽象基类封装 POI 公共操作
5. **嵌套深度 ≤ 3 层**：严格使用卫语句 + 方法抽取控制
6. **循环引用保护**：递归解析时维护 visitedRefs Set，最大深度 10 层

## 测试数据

- Onshape OpenAPI JSON: `https://cad.onshape.com/api/openapi`
- 内置示例: Petstore 精简版（前端 JsonUploader 内置）

## TODO

- [x] 创建 Maven 多模块 pom.xml
- [x] 创建 Spring Boot 启动类 (端口探测 + 浏览器唤醒)
- [x] 创建后端 DTO 模型
- [x] 实现 Schema 解析策略模式
- [x] 实现 Word 生成 Builder + Factory
- [x] 创建 REST Controller
- [x] 初始化 Vue 3 + Vite + TS 前端
- [x] 实现高保真表格渲染组件
- [x] 实现 PDF / Word 导出
- [x] 实现帮助手册抽屉
- [ ] npm install 安装依赖
- [ ] 后端编译验证
- [ ] 端到端测试
