# OpenAPI 接口文档自动生成与转换工具 (Doc Auto Generator)

![Vue3](https://img.shields.io/badge/Vue.js-3.0-green.svg) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-2.7.18-green.svg) ![TypeScript](https://img.shields.io/badge/TypeScript-5.4-blue.svg) ![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)

这是一个**开箱即用**的全栈独立应用，用于将标准的 OpenAPI (Swagger) JSON 规范文件转换为高保真的可视化预览，并支持一键导出为定制化的 Word (.docx) 或 PDF 文档。

## 🌟 核心特性

- **一键打包部署**：前后端完全融合为单体架构。只需一个 JAR 包即可运行，内置 Tomcat 与所有前端静态资源。
- **智能开箱即用**：
  - **端口探活机制**：启动时自动检测 `8080` 端口，若被占用会自动递增寻找可用端口，避免启动失败。
  - **浏览器自动唤醒**：服务完全就绪后，系统会自动调起默认浏览器打开应用主页。
- **高保真高精度渲染**：前端使用原生 HTML Table 并进行严格的 CSS 隔离，像素级还原后端基于 Apache POI 纯代码绘制的 Word 表格样式。
- **多格式导出**：
  - **Word 导出**：纯后端 Java 代码动态构建，无模板依赖，生成可二次编辑的结构化文档。
  - **PDF 导出**：纯前端 JS 直出，所见即所得。
- **极客级架构**：
  - 代码深度严格控制在 3 层以内。
  - 大量运用**策略模式 (Strategy)** 解析复杂树形的 Schema 节点。
  - 使用**建造者模式 (Builder)** 和**工厂模式 (Factory)** 解耦 Word 文档中各区块的渲染逻辑。
  - 自动拦截深层嵌套与循环引用 (`$ref`)。

---

## 🚀 快速启动指南

本项目基于 Maven 多模块管理，包含 `frontend` 和 `backend` 两个子模块。构建时 `frontend-maven-plugin` 会自动为你安装 Node.js 和前端依赖，无需你在本地配置前端环境！

### 1. 环境准备
- **JDK**: Java 11
- **Maven**: 3.8+

### 2. 编译打包 (一键全栈构建)
在项目根目录（`doc-auto-generator/`）下执行：
```bash
mvn clean package
```
> **说明**：该命令会自动执行以下操作：
> 1. 下载内嵌版 Node.js 与 npm。
> 2. 执行 `npm install` 下载前端依赖。
> 3. 执行 Vite 构建，生成前端产物到 `dist/`。
> 4. Maven 自动将 `dist/` 中的文件拷贝到 Spring Boot 的 `static/` 目录。
> 5. 最终在 `backend/target/` 目录下生成一个约数十 MB 的胖包 (Fat JAR)。

### 3. 运行项目
进入 `backend/target/` 目录，找到打包好的 JAR 文件并运行：
```bash
java -jar doc-auto-generator-backend-1.0.0.jar
```
控制台会输出如下信息：
```text
===== OpenAPI Doc Generator 正在启动，端口: 8080 =====
... Spring Boot 启动日志 ...
===== 应用启动成功！访问地址: http://localhost:8080 =====
已自动打开系统默认浏览器
```
此时你的浏览器会自动打开！享受开箱即用的体验。

---

## 👨‍💻 开发环境启动指南

如果你希望对代码进行二次开发，需要分别启动前后端。

### 后端开发启动
1. 使用 IDEA 导入项目。
2. 确保 JDK 为 11。
3. 找到并运行主类：`com.docgen.DocAutoGeneratorApplication`
4. 后端默认监听 `8080` 端口。

### 前端开发启动
1. 进入 `frontend` 目录：
   ```bash
   cd frontend
   ```
2. 安装依赖（确保你本地已安装 Node.js 18+）：
   ```bash
   npm install
   ```
3. 启动 Vite 开发服务器：
   ```bash
   npm run dev
   ```
4. 访问前端地址（通常是 `http://localhost:3000`）。Vite 配置的 proxy 会自动将 `/api` 请求转发给后端的 `8080` 端口。

---

## 📖 使用文档

### 1. 数据源输入
本工具接受标准的 OpenAPI 3.x 或 Swagger 2.0 格式的 JSON。
你可以通过以下三种方式输入：
- **复制粘贴**：直接将 JSON 文本粘贴到左侧文本框。
- **文件拖拽**：将 `.json` 文件拖入虚线框区域上传。
- **加载示例**：点击底部的【加载示例】按钮，系统会填入一份精简版的 Petstore OpenAPI，供你快速预览功能。

**如何从你自己的项目中获取 OpenAPI JSON？**
- 访问你项目的 Swagger UI（如 `http://localhost:8080/swagger-ui.html`）。
- 顶部通常有一个类似 `/v3/api-docs` 的链接，点击后浏览器显示的即是 JSON 数据。
- 也可以通过在线系统获取，例如 Onshape API JSON: `https://cad.onshape.com/api/openapi`

### 2. 预览文档
填入 JSON 后，点击【解析文档】。
系统会将复杂的树形 Schema 扁平化，并在右侧高保真渲染：
- **黄色背景行**：接口 URL 及类型。
- **绿色背景行**：HTTP Headers 等请求头信息。
- **蓝色背景行**：请求参数及其字段结构，支持 `└ ` 的多级嵌套展示。
- **灰色背景行**：返回值结构及字段解析。
- **蓝字高亮**：字段描述中若包含 `非必需`、`默认值` 等关键词，会自动高亮。

### 3. 文档导出
在左侧工具栏中，支持两种导出方式：

| 特性 | 导出 Word | 导出 PDF |
| :--- | :--- | :--- |
| **底层技术** | Java 后端 + Apache POI | Vue 前端 + html2pdf.js |
| **生成方式** | 纯代码实时构建 `XWPFDocument` | 获取 DOM 节点转换为 PDF 画布 |
| **可编辑性** | ✅ 完全可自由二次编辑 | ❌ 不可编辑的快照 |
| **适用场景** | 团队内交付、需要修饰文档或补充额外内容 | 快速分享、只读传阅、归档 |

---

## 🛠 技术细节与架构约束

本项目秉持极致的代码整洁之道 (Clean Code)。
核心复杂逻辑的实现规范：

1. **嵌套深度不超过 3 层**
   - 杜绝深层级的 `if/for` 嵌套，强制采用卫语句 (Guard Clauses)。
2. **Schema 解析的策略模式**
   - `SchemaStrategyFactory` 负责中转。
   - 分别由 `ObjectSchemaStrategy`、`ArraySchemaStrategy`、`PrimitiveSchemaStrategy` 等子类独立负责不同的节点展开。
   - 使用 `visitedRefs` 集合阻断循环引用的死循环。
3. **Word 生成的 Builder 模式**
   - `WordDocumentBuilder` 链式调用：`createDocument().addTitleSection().addAllEndpoints().build()`。
   - `WordSectionFactory` 模板方法基类处理边框、背景色的繁琐 POI 语法，将业务数据渲染交由子类实现。
