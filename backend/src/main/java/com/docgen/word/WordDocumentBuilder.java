package com.docgen.word;

import com.docgen.model.ApiDocumentDTO;
import com.docgen.model.ApiEndpointDTO;
import com.docgen.word.factory.CombinedEndpointFactory;
import com.docgen.word.factory.TitleSectionFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

/**
 * Word 文档建造者 — 建造者模式 (Builder Pattern)。
 *
 * 提供链式 API 将文档的各区域按顺序组装，
 * 将"构建什么"与"如何构建"彻底解耦。
 * 各区域的渲染细节由对应的 Factory 负责。
 *
 * 使用方式：
 * XWPFDocument doc = new WordDocumentBuilder()
 *     .createDocument()
 *     .addTitleSection(docDTO)
 *     .addAllEndpoints(docDTO)
 *     .build();
 */
public class WordDocumentBuilder {

    private XWPFDocument document;
    private final TitleSectionFactory titleFactory = new TitleSectionFactory();
    private final CombinedEndpointFactory endpointFactory = new CombinedEndpointFactory();

    /**
     * 创建空白文档。
     */
    public WordDocumentBuilder createDocument() {
        this.document = new XWPFDocument();
        return this;
    }

    /**
     * 添加标题区域（标题、版本、描述）。
     */
    public WordDocumentBuilder addTitleSection(ApiDocumentDTO docDTO) {
        titleFactory.render(document, docDTO);
        return this;
    }

    /**
     * 添加所有端点的表格。
     */
    public WordDocumentBuilder addAllEndpoints(ApiDocumentDTO docDTO) {
        if (docDTO.getEndpoints() == null) {
            return this;
        }
        int index = 1;
        for (ApiEndpointDTO endpoint : docDTO.getEndpoints()) {
            addEndpoint(endpoint, index++);
        }
        return this;
    }

    /**
     * 添加单个端点的完整渲染（标题 + 业务说明 + 接口表格）。
     */
    public WordDocumentBuilder addEndpoint(ApiEndpointDTO endpoint, int index) {
        endpointFactory.render(document, endpoint, index);
        return this;
    }

    /**
     * 完成构建，返回最终文档。
     */
    public XWPFDocument build() {
        return document;
    }
}
