package com.docgen.model;

import java.util.List;

/**
 * 单个 API 端点（接口）的数据传输对象。
 *
 * <h3>业务含义</h3>
 * <p>对应 OpenAPI paths 下的一个具体操作（如 GET /users/{id}）。
 * 一个 path 可能有多个 HTTP 方法，每个方法对应一个 ApiEndpointDTO。</p>
 *
 * <h3>字段来源映射</h3>
 * <ul>
 *   <li>tag → Operation.tags[0]（用于分组显示）</li>
 *   <li>method → 从 PathItem 的方法枚举获取（GET/POST/PUT/DELETE 等）</li>
 *   <li>path → paths 的 key（如 "/users/{id}"）</li>
 *   <li>summary → Operation.summary（接口摘要）</li>
 *   <li>headers → 从 parameters 中筛选 in=header 的参数</li>
 *   <li>requestParams → 从 parameters 中筛选 in=query/path 的参数 + requestBody 展开</li>
 *   <li>responseFields → 解析 responses 中默认/200 响应的 Schema</li>
 * </ul>
 */
public class ApiEndpointDTO {

    /** 接口所属标签/分组 */
    private String tag;

    /** HTTP 方法（GET, POST, PUT, DELETE 等） */
    private String method;

    /** 请求路径（如 /api/users/{id}） */
    private String path;

    /** 接口摘要描述 */
    private String summary;

    /** 接口详细描述 */
    private String description;

    /** 操作 ID（来自 operationId） */
    private String operationId;

    /** 是否已废弃 */
    private boolean deprecated;

    /** Content-Type（如 application/json） */
    private String contentType;

    /** 请求头参数列表 */
    private List<ParameterDTO> headers;

    /** 请求参数列表（path + query + requestBody 展开） */
    private List<SchemaFieldDTO> requestFields;

    /** 响应参数列表 */
    private List<SchemaFieldDTO> responseFields;

    public ApiEndpointDTO() {
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public List<ParameterDTO> getHeaders() {
        return headers;
    }

    public void setHeaders(List<ParameterDTO> headers) {
        this.headers = headers;
    }

    public List<SchemaFieldDTO> getRequestFields() {
        return requestFields;
    }

    public void setRequestFields(List<SchemaFieldDTO> requestFields) {
        this.requestFields = requestFields;
    }

    public List<SchemaFieldDTO> getResponseFields() {
        return responseFields;
    }

    public void setResponseFields(List<SchemaFieldDTO> responseFields) {
        this.responseFields = responseFields;
    }
}
