package com.docgen.controller;

import com.docgen.model.ApiDocumentDTO;
import com.docgen.service.OpenApiParseService;
import com.docgen.word.WordGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * OpenAPI 文档处理 REST 控制器。
 *
 * 提供两个核心接口：
 * 1. POST /api/parse — 解析 OpenAPI JSON，返回结构化 DTO（前端渲染用）
 * 2. POST /api/parse-to-word — 解析 JSON 并生成 Word 文档流下载
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class OpenApiController {

    private static final Logger log = LoggerFactory.getLogger(OpenApiController.class);

    private final OpenApiParseService parseService;
    private final WordGenerationService wordService;

    public OpenApiController(OpenApiParseService parseService, WordGenerationService wordService) {
        this.parseService = parseService;
        this.wordService = wordService;
    }

    /**
     * 解析 OpenAPI JSON，返回结构化 DTO。
     *
     * @param body 请求体，需包含 "content" 字段（OpenAPI JSON 字符串）
     * @return 解析后的 ApiDocumentDTO
     */
    @PostMapping("/parse")
    public ResponseEntity<ApiDocumentDTO> parseOpenApi(@RequestBody Map<String, String> body) {
        String jsonContent = body.get("content");
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            ApiDocumentDTO result = parseService.parseFromJson(jsonContent);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("OpenAPI 解析失败: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 解析 OpenAPI JSON 并生成 Word 文档下载。
     *
     * @param body 请求体，需包含 "content" 字段（OpenAPI JSON 字符串）
     * @return .docx 文件流
     */
    @PostMapping("/parse-to-word")
    public ResponseEntity<byte[]> parseToWord(@RequestBody Map<String, String> body) throws IOException {
        String jsonContent = body.get("content");
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ApiDocumentDTO docDTO = parseService.parseFromJson(jsonContent);
        byte[] wordBytes = wordService.generate(docDTO);

        String fileName = buildFileName(docDTO.getTitle());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .contentLength(wordBytes.length)
                .body(wordBytes);
    }

    /**
     * 构建下载文件名（URL 编码处理中文）。
     */
    private String buildFileName(String title) {
        String baseName = (title != null && !title.isEmpty()) ? title : "api-doc";
        // 移除文件名中的非法字符
        baseName = baseName.replaceAll("[\\\\/:*?\"<>|]", "_");
        return URLEncoder.encode(baseName + ".docx", StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
