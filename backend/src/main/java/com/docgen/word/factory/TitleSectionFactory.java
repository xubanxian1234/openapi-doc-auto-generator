package com.docgen.word.factory;

import com.docgen.model.ApiDocumentDTO;
import com.docgen.word.style.WordStyleConstants;
import org.apache.poi.xwpf.usermodel.*;

/**
 * 标题区域工厂 — 负责渲染文档标题、版本、描述等头部信息。
 */
public class TitleSectionFactory extends WordSectionFactory {

    /**
     * 在文档头部渲染 API 标题和基本信息。
     */
    public void render(XWPFDocument document, ApiDocumentDTO docDTO) {
        renderMainTitle(document, docDTO.getTitle());
        renderMetaInfo(document, docDTO);
        addBlankLine(document);
    }

    private void renderMainTitle(XWPFDocument document, String title) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setSpacingAfter(200);

        XWPFRun run = paragraph.createRun();
        run.setText(title != null ? title : "API Documentation");
        run.setFontFamily(WordStyleConstants.FONT_FAMILY);
        run.setFontSize(WordStyleConstants.FONT_SIZE_TITLE);
        run.setBold(true);
        run.setColor(WordStyleConstants.COLOR_DEFAULT);
    }

    private void renderMetaInfo(XWPFDocument document, ApiDocumentDTO docDTO) {
        addInfoLine(document, "版本: " + orDefault(docDTO.getVersion(), "1.0.0"));
        addInfoLine(document, "基础地址: " + orDefault(docDTO.getBaseUrl(), "N/A"));
        addInfoLine(document, "规范版本: " + orDefault(docDTO.getSpecVersion(), "N/A"));

        if (docDTO.getDescription() != null && !docDTO.getDescription().isEmpty()) {
            // 只取描述的前 200 字符，避免过长影响文档布局
            String desc = docDTO.getDescription().length() > 200
                    ? docDTO.getDescription().substring(0, 200) + "..."
                    : docDTO.getDescription();
            addInfoLine(document, "描述: " + desc);
        }
    }

    private void addInfoLine(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(50);

        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontFamily(WordStyleConstants.FONT_FAMILY);
        run.setFontSize(WordStyleConstants.FONT_SIZE_BODY);
        run.setColor(WordStyleConstants.COLOR_DEFAULT);
    }

    private void addBlankLine(XWPFDocument document) {
        document.createParagraph();
    }

    private String orDefault(String value, String defaultValue) {
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
}
