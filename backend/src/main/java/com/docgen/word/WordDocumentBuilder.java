package com.docgen.word;

import com.docgen.model.ApiDocumentDTO;
import com.docgen.model.ApiEndpointDTO;
import com.docgen.word.factory.CombinedEndpointFactory;
import com.docgen.word.factory.TitleSectionFactory;
import com.docgen.word.factory.TocFactory;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.Borders;
import com.docgen.word.style.WordStyleConstants;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSimpleField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 *     .addHeaderFooter(docDTO)
 *     .addTitleSection(docDTO)
 *     .addAllEndpoints(docDTO)
 *     .build();
 */
public class WordDocumentBuilder {

    private XWPFDocument document;
    private final CombinedEndpointFactory endpointFactory = new CombinedEndpointFactory();
    private final TocFactory tocFactory = new TocFactory();

    /**
     * 创建空白文档。
     */
    public WordDocumentBuilder createDocument() {
        this.document = new XWPFDocument();
        return this;
    }

    /**
     * 添加页眉页脚（带页码）。
     */
    public WordDocumentBuilder addHeaderFooter(ApiDocumentDTO docDTO) {
        // 页眉
        XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
        XWPFParagraph hPara = header.createParagraph();
        hPara.setAlignment(ParagraphAlignment.RIGHT);
        hPara.setBorderBottom(Borders.SINGLE);
        
        XWPFRun hRun = hPara.createRun();
        hRun.setText((docDTO.getTitle() != null ? docDTO.getTitle() : "API 文档") + " - 接口文档");
        hRun.setColor("666666");
        hRun.setFontSize(9);
        hRun.setFontFamily(WordStyleConstants.FONT_FAMILY);

        // 页脚
        XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
        XWPFParagraph fPara = footer.createParagraph();
        fPara.setAlignment(ParagraphAlignment.CENTER);
        fPara.setBorderTop(Borders.SINGLE);
        
        XWPFRun fRun = fPara.createRun();
        fRun.setText("第 ");
        fRun.setColor("666666");
        fRun.setFontSize(9);
        fRun.setFontFamily(WordStyleConstants.FONT_FAMILY);
        
        CTSimpleField pageField = fPara.getCTP().addNewFldSimple();
        pageField.setInstr("PAGE \\* MERGEFORMAT");
        
        XWPFRun fRun2 = fPara.createRun();
        fRun2.setText(" 页 / 共 ");
        fRun2.setColor("666666");
        fRun2.setFontSize(9);
        fRun2.setFontFamily(WordStyleConstants.FONT_FAMILY);
        
        CTSimpleField numPagesField = fPara.getCTP().addNewFldSimple();
        numPagesField.setInstr("NUMPAGES \\* MERGEFORMAT");
        
        XWPFRun fRun3 = fPara.createRun();
        fRun3.setText(" 页");
        fRun3.setColor("666666");
        fRun3.setFontSize(9);
        fRun3.setFontFamily(WordStyleConstants.FONT_FAMILY);

        return this;
    }

    /**
     * 添加封面页。
     */
    public WordDocumentBuilder addCoverPage(ApiDocumentDTO docDTO) {
        XWPFParagraph p = document.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        p.setSpacingBefore(4000);
        
        XWPFRun titleRun = p.createRun();
        titleRun.setText(docDTO.getTitle() != null ? docDTO.getTitle() : "API 接口文档");
        titleRun.setBold(true);
        titleRun.setFontSize(36);
        titleRun.setFontFamily(WordStyleConstants.FONT_FAMILY);
        
        XWPFParagraph p2 = document.createParagraph();
        p2.setAlignment(ParagraphAlignment.CENTER);
        p2.setSpacingBefore(1000);
        
        XWPFRun subtitleRun = p2.createRun();
        subtitleRun.setText("接口集成说明");
        subtitleRun.setBold(true);
        subtitleRun.setFontSize(24);
        subtitleRun.setFontFamily(WordStyleConstants.FONT_FAMILY);
        
        XWPFParagraph pb = document.createParagraph();
        pb.setPageBreak(true);
        
        return this;
    }

    /**
     * 添加所有端点的表格，并提前生成目录。
     */
    public WordDocumentBuilder addAllEndpoints(ApiDocumentDTO docDTO) {
        if (docDTO.getEndpoints() == null) {
            return this;
        }

        // 1. 按 tag 分组
        Map<String, List<ApiEndpointDTO>> grouped = new LinkedHashMap<>();
        for (ApiEndpointDTO endpoint : docDTO.getEndpoints()) {
            String tag = endpoint.getTag() != null ? endpoint.getTag() : "默认分组";
            grouped.computeIfAbsent(tag, k -> new ArrayList<>()).add(endpoint);
        }

        // 2. 第一遍遍历：生成 TOC 条目和对应的 Bookmark
        // 为“1. 概述”追加目录项
        TocFactory.TocEntry overviewEntry = tocFactory.addEntry("1. 概述", 1);
        
        Map<String, TocFactory.TocEntry> tagTocEntries = new LinkedHashMap<>();
        Map<ApiEndpointDTO, TocFactory.TocEntry> endpointTocEntries = new LinkedHashMap<>();

        int majorIndex = 2; // API 接口从第 2 章开始
        for (Map.Entry<String, List<ApiEndpointDTO>> entry : grouped.entrySet()) {
            String tagHeading = majorIndex + ". " + entry.getKey();
            tagTocEntries.put(entry.getKey(), tocFactory.addEntry(tagHeading, 1));

            int minorIndex = 1;
            for (ApiEndpointDTO endpoint : entry.getValue()) {
                String endpointTitle = majorIndex + "." + minorIndex + " " + (endpoint.getSummary() != null ? endpoint.getSummary() : endpoint.getPath());
                endpointTocEntries.put(endpoint, tocFactory.addEntry(endpointTitle, 2));
                minorIndex++;
            }
            majorIndex++;
        }

        // 3. 渲染目录段落
        tocFactory.render(document);

        // 分页符 (目录后起新页)
        XWPFParagraph pageBreakPara = document.createParagraph();
        pageBreakPara.setPageBreak(true);

        // 4. 第二遍遍历：实际渲染正文
        // 渲染第一章：概述
        endpointFactory.renderTagHeading(document, 1, "概述", overviewEntry.bookmarkId, overviewEntry.bookmarkName);
        
        XWPFParagraph descPara = document.createParagraph();
        descPara.setSpacingAfter(400);
        descPara.setSpacingBefore(200);
        descPara.setIndentationLeft(400);
        XWPFRun descRun = descPara.createRun();
        descRun.setText(docDTO.getDescription() != null ? docDTO.getDescription() : "本文档包含接口的详细信息与集成说明。");
        descRun.setFontSize(10);
        descRun.setColor("333333");
        descRun.setFontFamily(WordStyleConstants.FONT_FAMILY);

        // 渲染后续接口
        majorIndex = 2;
        for (Map.Entry<String, List<ApiEndpointDTO>> entry : grouped.entrySet()) {
            TocFactory.TocEntry tagEntry = tagTocEntries.get(entry.getKey());
            endpointFactory.renderTagHeading(document, majorIndex, entry.getKey(), tagEntry.bookmarkId, tagEntry.bookmarkName);

            int minorIndex = 1;
            for (ApiEndpointDTO endpoint : entry.getValue()) {
                TocFactory.TocEntry endpointEntry = endpointTocEntries.get(endpoint);
                endpointFactory.render(document, endpoint, majorIndex + "." + minorIndex, endpointEntry.bookmarkId, endpointEntry.bookmarkName);
                minorIndex++;
            }
            majorIndex++;
        }
        return this;
    }

    /**
     * 完成构建，返回最终文档。
     */
    public XWPFDocument build() {
        document.enforceUpdateFields();
        return document;
    }
}
