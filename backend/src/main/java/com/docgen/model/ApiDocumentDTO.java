package com.docgen.model;

import java.util.List;

/**
 * OpenAPI 文档的顶层数据传输对象。
 *
 * <h3>设计意图</h3>
 * <p>swagger-parser 解析后的 {@code OpenAPI} 对象结构极其复杂且深度嵌套，
 * 不适合直接传递给前端。此 DTO 是"展平"后的扁平化视图，
 * 只保留前端渲染和 Word 生成所需的字段。</p>
 *
 * <h3>数据流</h3>
 * <pre>
 * OpenAPI JSON → swagger-parser → OpenAPI 对象 → OpenApiParseService → ApiDocumentDTO → 前端/Word
 * </pre>
 */
public class ApiDocumentDTO {

    /** API 文档标题（来自 info.title） */
    private String title;

    /** API 版本号（来自 info.version） */
    private String version;

    /** API 描述信息（来自 info.description） */
    private String description;

    /** 基础 URL（来自 servers[0].url） */
    private String baseUrl;

    /** OpenAPI 规范版本（如 "3.0.1" 或转换前的 "2.0"） */
    private String specVersion;

    /** 所有解析出的 API 端点列表 */
    private List<ApiEndpointDTO> endpoints;

    public ApiDocumentDTO() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getSpecVersion() {
        return specVersion;
    }

    public void setSpecVersion(String specVersion) {
        this.specVersion = specVersion;
    }

    public List<ApiEndpointDTO> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<ApiEndpointDTO> endpoints) {
        this.endpoints = endpoints;
    }
}
