package com.docgen.service;

import com.docgen.model.*;
import com.docgen.service.strategy.SchemaStrategyFactory;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OpenAPI 解析主服务。
 *
 * 将原始 OpenAPI JSON 字符串（支持 2.0 和 3.x）解析为扁平化的 {@link ApiDocumentDTO}。
 * 内部使用 swagger-parser 完成原始解析，再通过策略模式进行 Schema 递归展开。
 *
 * 设计要点：
 * - swagger-parser 会自动将 Swagger 2.0 转为 OpenAPI 3.x 内部模型
 * - Schema 解析委托给 {@link SchemaStrategyFactory}（策略模式）
 * - 方法嵌套深度严格控制在 3 层以内
 */
@Service
public class OpenApiParseService {

    private static final Logger log = LoggerFactory.getLogger(OpenApiParseService.class);

    private final SchemaStrategyFactory strategyFactory = new SchemaStrategyFactory();

    /**
     * 解析 OpenAPI JSON 字符串为文档 DTO。
     *
     * @param jsonContent OpenAPI JSON 字符串（支持 Swagger 2.0 和 OpenAPI 3.x）
     * @return 解析后的文档 DTO
     * @throws IllegalArgumentException 如果 JSON 解析失败
     */
    public ApiDocumentDTO parseFromJson(String jsonContent) {
        OpenAPI openAPI = doParse(jsonContent);
        return buildDocumentDTO(openAPI);
    }

    // ======================== 第一层：解析入口 ========================

    /**
     * 调用 swagger-parser 解析 JSON 字符串。
     * 开启 resolveFully 以自动解析所有 $ref 引用。
     */
    private OpenAPI doParse(String jsonContent) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        // 注意：不用 resolveFully，因为我们需要保留 $ref 信息用于循环引用检测
        // options.setResolveFully(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(jsonContent, null, options);

        if (result.getOpenAPI() == null) {
            String errors = result.getMessages() != null
                    ? String.join("; ", result.getMessages()) : "未知错误";
            throw new IllegalArgumentException("OpenAPI 解析失败: " + errors);
        }

        logParseWarnings(result);
        return result.getOpenAPI();
    }

    /**
     * 记录解析过程中的警告信息（非致命错误）。
     */
    private void logParseWarnings(SwaggerParseResult result) {
        if (result.getMessages() == null || result.getMessages().isEmpty()) {
            return;
        }
        result.getMessages().forEach(msg -> log.debug("解析警告: {}", msg));
    }

    // ======================== 第二层：构建顶层 DTO ========================

    private ApiDocumentDTO buildDocumentDTO(OpenAPI openAPI) {
        ApiDocumentDTO doc = new ApiDocumentDTO();
        doc.setTitle(safeGet(() -> openAPI.getInfo().getTitle(), "Untitled API"));
        doc.setVersion(safeGet(() -> openAPI.getInfo().getVersion(), "1.0.0"));
        doc.setDescription(safeGet(() -> openAPI.getInfo().getDescription(), ""));
        doc.setBaseUrl(extractBaseUrl(openAPI));
        doc.setSpecVersion(openAPI.getOpenapi());

        Map<String, Schema> allSchemas = extractAllSchemas(openAPI);
        List<ApiEndpointDTO> endpoints = extractEndpoints(openAPI, allSchemas);
        doc.setEndpoints(endpoints);

        log.info("解析完成: {} 个端点, {} 个 Schema 定义",
                endpoints.size(), allSchemas.size());
        return doc;
    }

    private String extractBaseUrl(OpenAPI openAPI) {
        if (openAPI.getServers() == null || openAPI.getServers().isEmpty()) {
            return "";
        }
        return openAPI.getServers().get(0).getUrl();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Schema> extractAllSchemas(OpenAPI openAPI) {
        if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            return Collections.emptyMap();
        }
        return openAPI.getComponents().getSchemas();
    }

    // ======================== 第三层：提取端点 ========================

    private List<ApiEndpointDTO> extractEndpoints(OpenAPI openAPI, Map<String, Schema> allSchemas) {
        if (openAPI.getPaths() == null) {
            return Collections.emptyList();
        }

        List<ApiEndpointDTO> endpoints = new ArrayList<>();
        for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
            List<ApiEndpointDTO> pathEndpoints = extractPathEndpoints(
                    pathEntry.getKey(), pathEntry.getValue(), allSchemas);
            endpoints.addAll(pathEndpoints);
        }
        return endpoints;
    }

    /**
     * 从单个 PathItem 中提取所有 HTTP 方法对应的端点。
     * PathItem 可能包含 GET/POST/PUT/DELETE 等多个操作。
     */
    private List<ApiEndpointDTO> extractPathEndpoints(
            String path, PathItem pathItem, Map<String, Schema> allSchemas) {

        List<ApiEndpointDTO> results = new ArrayList<>();
        Map<String, Operation> operations = getOperationMap(pathItem);

        for (Map.Entry<String, Operation> opEntry : operations.entrySet()) {
            ApiEndpointDTO endpoint = buildEndpoint(
                    path, opEntry.getKey(), opEntry.getValue(), allSchemas);
            results.add(endpoint);
        }
        return results;
    }

    /**
     * 将 PathItem 的各 HTTP 方法提取为统一的 Map 结构。
     * 避免对每个方法写重复的 null 检查。
     */
    private Map<String, Operation> getOperationMap(PathItem pathItem) {
        Map<String, Operation> map = new LinkedHashMap<>();
        addIfPresent(map, "GET", pathItem.getGet());
        addIfPresent(map, "POST", pathItem.getPost());
        addIfPresent(map, "PUT", pathItem.getPut());
        addIfPresent(map, "DELETE", pathItem.getDelete());
        addIfPresent(map, "PATCH", pathItem.getPatch());
        addIfPresent(map, "HEAD", pathItem.getHead());
        addIfPresent(map, "OPTIONS", pathItem.getOptions());
        return map;
    }

    private void addIfPresent(Map<String, Operation> map, String method, Operation op) {
        if (op != null) {
            map.put(method, op);
        }
    }

    // ======================== 构建单个端点 ========================

    private ApiEndpointDTO buildEndpoint(
            String path, String method, Operation operation, Map<String, Schema> allSchemas) {

        ApiEndpointDTO endpoint = new ApiEndpointDTO();
        endpoint.setPath(path);
        endpoint.setMethod(method);
        endpoint.setTag(extractFirstTag(operation));
        endpoint.setSummary(operation.getSummary());
        endpoint.setDescription(operation.getDescription());
        endpoint.setOperationId(operation.getOperationId());
        endpoint.setDeprecated(Boolean.TRUE.equals(operation.getDeprecated()));

        // 分别解析请求头、请求参数、响应参数
        endpoint.setHeaders(extractHeaders(operation));
        endpoint.setContentType(extractContentType(operation));
        endpoint.setRequestFields(extractRequestFields(operation, allSchemas));
        endpoint.setResponseFields(extractResponseFields(operation, allSchemas));

        return endpoint;
    }

    private String extractFirstTag(Operation operation) {
        if (operation.getTags() == null || operation.getTags().isEmpty()) {
            return "Default";
        }
        return operation.getTags().get(0);
    }

    // ======================== 请求头提取 ========================

    private List<ParameterDTO> extractHeaders(Operation operation) {
        if (operation.getParameters() == null) {
            return Collections.emptyList();
        }
        return operation.getParameters().stream()
                .filter(p -> "header".equalsIgnoreCase(p.getIn()))
                .map(this::toParameterDTO)
                .collect(Collectors.toList());
    }

    private ParameterDTO toParameterDTO(Parameter parameter) {
        ParameterDTO dto = new ParameterDTO();
        dto.setName(parameter.getName());
        dto.setIn(parameter.getIn());
        dto.setDescription(parameter.getDescription());
        dto.setRequired(Boolean.TRUE.equals(parameter.getRequired()));

        if (parameter.getSchema() != null) {
            dto.setType(parameter.getSchema().getType());
            dto.setDefaultValue(parameter.getSchema().getDefault() != null
                    ? String.valueOf(parameter.getSchema().getDefault()) : null);
        }
        return dto;
    }

    // ======================== Content-Type 提取 ========================

    private String extractContentType(Operation operation) {
        RequestBody requestBody = operation.getRequestBody();
        if (requestBody == null || requestBody.getContent() == null) {
            return null;
        }
        // 取第一个 Content-Type
        return requestBody.getContent().keySet().stream()
                .findFirst()
                .map(ct -> ct.contains(";") ? ct.substring(0, ct.indexOf(';')).trim() : ct)
                .orElse(null);
    }

    // ======================== 请求参数提取 ========================

    /**
     * 提取请求参数：包括 path/query 参数 + requestBody 展开的字段。
     */
    private List<SchemaFieldDTO> extractRequestFields(
            Operation operation, Map<String, Schema> allSchemas) {

        List<SchemaFieldDTO> fields = new ArrayList<>();

        // 1. path 和 query 参数转为 SchemaFieldDTO
        fields.addAll(extractPathQueryParams(operation));

        // 2. requestBody 的 Schema 递归展开
        fields.addAll(extractRequestBodyFields(operation, allSchemas));

        return fields;
    }

    private List<SchemaFieldDTO> extractPathQueryParams(Operation operation) {
        if (operation.getParameters() == null) {
            return Collections.emptyList();
        }
        return operation.getParameters().stream()
                .filter(p -> "path".equalsIgnoreCase(p.getIn()) || "query".equalsIgnoreCase(p.getIn()))
                .map(this::parameterToSchemaField)
                .collect(Collectors.toList());
    }

    private SchemaFieldDTO parameterToSchemaField(Parameter parameter) {
        SchemaFieldDTO field = new SchemaFieldDTO();
        field.setName(parameter.getName());
        field.setRequired(Boolean.TRUE.equals(parameter.getRequired()));
        field.setDescription(buildParamDescription(parameter));
        field.setDepth(0);

        if (parameter.getSchema() != null) {
            Schema<?> schema = parameter.getSchema();
            field.setType(schema.getType() != null ? schema.getType() : "string");
            field.setFormat(schema.getFormat());
            field.setDefaultValue(schema.getDefault() != null ? String.valueOf(schema.getDefault()) : null);
        } else {
            field.setType("string");
        }
        return field;
    }

    /**
     * 构建参数描述，包含位置信息（[path] 或 [query]）。
     */
    private String buildParamDescription(Parameter parameter) {
        String desc = parameter.getDescription() != null ? parameter.getDescription() : "";
        String location = "[" + parameter.getIn() + "] ";
        return location + desc;
    }

    private List<SchemaFieldDTO> extractRequestBodyFields(
            Operation operation, Map<String, Schema> allSchemas) {

        RequestBody requestBody = operation.getRequestBody();
        if (requestBody == null || requestBody.getContent() == null) {
            return Collections.emptyList();
        }
        
        String contentType = extractContentType(operation);
        String inLocation = "body";
        if (contentType != null && (contentType.contains("form-data") || contentType.contains("x-www-form-urlencoded"))) {
            inLocation = "form";
        }

        Schema<?> bodySchema = extractFirstSchema(requestBody.getContent());
        if (bodySchema == null) {
            return Collections.emptyList();
        }
        List<SchemaFieldDTO> fields = strategyFactory.delegateParse(
                null, bodySchema, Collections.emptySet(),
                allSchemas, 0, new HashSet<>());
                
        // 给根级参数增加位置标识
        for (SchemaFieldDTO field : fields) {
            if (field.getDepth() == 0) {
                String desc = field.getDescription() != null ? field.getDescription() : "";
                field.setDescription("[" + inLocation + "] " + desc);
            }
        }
        
        return fields;
    }

    // ======================== 响应参数提取 ========================

    private List<SchemaFieldDTO> extractResponseFields(
            Operation operation, Map<String, Schema> allSchemas) {

        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            return Collections.emptyList();
        }

        // 优先取 200 响应，其次取 default 响应
        ApiResponse response = responses.get("200");
        if (response == null) {
            response = responses.get("default");
        }
        if (response == null) {
            return Collections.emptyList();
        }
        return extractResponseSchemaFields(response, allSchemas);
    }

    private List<SchemaFieldDTO> extractResponseSchemaFields(
            ApiResponse response, Map<String, Schema> allSchemas) {

        if (response.getContent() == null) {
            return Collections.emptyList();
        }
        Schema<?> schema = extractFirstSchema(response.getContent());
        if (schema == null) {
            return Collections.emptyList();
        }
        return strategyFactory.delegateParse(
                null, schema, Collections.emptySet(),
                allSchemas, 0, new HashSet<>());
    }

    // ======================== 公共工具方法 ========================

    /**
     * 从 Content 映射中提取第一个 MediaType 的 Schema。
     */
    private Schema<?> extractFirstSchema(Content content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        MediaType mediaType = content.values().iterator().next();
        return mediaType != null ? mediaType.getSchema() : null;
    }

    /**
     * 安全获取值，如果 lambda 执行抛异常则返回默认值。
     * 避免多级 null 检查导致代码嵌套过深。
     */
    private <T> T safeGet(java.util.function.Supplier<T> supplier, T defaultValue) {
        try {
            T value = supplier.get();
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
