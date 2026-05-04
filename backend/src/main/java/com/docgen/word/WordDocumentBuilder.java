package com.docgen.word;

import com.docgen.model.ApiDocumentDTO;
import com.docgen.model.ApiEndpointDTO;
import com.docgen.word.factory.CombinedEndpointFactory;
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
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Word 文档建造者 — 建造者模式 (Builder Pattern)。
 *
 * 文档结构：
 * - 节1 (封面页)：无页眉页脚，无页码
 * - 节2 (目录页)：有页眉，页码用罗马数字（可选）
 * - 节3 (正文)：有页眉页脚，页码从 1 开始
 *
 * 使用 Word Section Break 实现页码从正文开始计数。
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
     * 添加页眉页脚（带页码，从正文第一页开始编号）。
     * 页眉页脚添加到文档默认 section 中，所有后续 section 会继承。
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

        // 页脚：使用 fldChar 方式插入 PAGE 和 NUMPAGES 域
        XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
        XWPFParagraph fPara = footer.createParagraph();
        fPara.setAlignment(ParagraphAlignment.CENTER);
        fPara.setBorderTop(Borders.SINGLE);
        
        addFooterTextField(fPara, "第 ");
        addFooterPageField(fPara, "PAGE");
        addFooterTextField(fPara, " 页 / 共 ");
        addFooterPageField(fPara, "NUMPAGES");
        addFooterTextField(fPara, " 页");

        return this;
    }

    private void addFooterTextField(XWPFParagraph para, String text) {
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setColor("666666");
        run.setFontSize(9);
        run.setFontFamily(WordStyleConstants.FONT_FAMILY);
    }

    private void addFooterPageField(XWPFParagraph para, String fieldName) {
        CTP ctp = para.getCTP();
        // BEGIN
        CTR beginR = ctp.addNewR();
        CTRPr beginRpr = beginR.addNewRPr();
        beginRpr.addNewColor().setVal("666666");
        beginRpr.addNewSz().setVal(BigInteger.valueOf(18));
        CTFldChar beginChar = beginR.addNewFldChar();
        beginChar.setFldCharType(STFldCharType.BEGIN);
        // INSTR
        CTR instrR = ctp.addNewR();
        CTRPr instrRpr = instrR.addNewRPr();
        instrRpr.addNewColor().setVal("666666");
        instrRpr.addNewSz().setVal(BigInteger.valueOf(18));
        CTText instrText = instrR.addNewInstrText();
        instrText.setStringValue(" " + fieldName + " ");
        // SEPARATE
        CTR sepR = ctp.addNewR();
        CTFldChar sepChar = sepR.addNewFldChar();
        sepChar.setFldCharType(STFldCharType.SEPARATE);
        // Placeholder
        CTR textR = ctp.addNewR();
        CTRPr textRpr = textR.addNewRPr();
        textRpr.addNewColor().setVal("666666");
        textRpr.addNewSz().setVal(BigInteger.valueOf(18));
        CTText pageText = textR.addNewT();
        pageText.setStringValue("1");
        // END
        CTR endR = ctp.addNewR();
        CTFldChar endChar = endR.addNewFldChar();
        endChar.setFldCharType(STFldCharType.END);
    }

    /**
     * 添加封面页，然后插入分节符（下一页），使目录页起新页。
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

        // 插入分节符（下一页），封面页结束
        addSectionBreakNextPage(p2);

        return this;
    }

    /**
     * 添加所有端点的表格，并提前生成目录。
     * 目录和正文之间也插入分节符，正文页码从 1 开始。
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
        TocFactory.TocEntry overviewEntry = tocFactory.addEntry("1    概述", 1);
        
        Map<String, TocFactory.TocEntry> tagTocEntries = new LinkedHashMap<>();
        Map<ApiEndpointDTO, TocFactory.TocEntry> endpointTocEntries = new LinkedHashMap<>();

        int majorIndex = 2;
        for (Map.Entry<String, List<ApiEndpointDTO>> entry : grouped.entrySet()) {
            String tagHeading = majorIndex + "    " + entry.getKey();
            tagTocEntries.put(entry.getKey(), tocFactory.addEntry(tagHeading, 1));

            int minorIndex = 1;
            for (ApiEndpointDTO endpoint : entry.getValue()) {
                String endpointTitle = majorIndex + "." + minorIndex + "      " + (endpoint.getSummary() != null ? endpoint.getSummary() : endpoint.getPath());
                endpointTocEntries.put(endpoint, tocFactory.addEntry(endpointTitle, 2));
                minorIndex++;
            }
            majorIndex++;
        }

        // 3. 渲染目录段落
        tocFactory.render(document);

        // 4. 目录后插入分节符（下一页），正文起新页
        //    获取目录最后一个段落，在其上添加分节符
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        XWPFParagraph lastTocPara = paragraphs.get(paragraphs.size() - 1);
        addSectionBreakNextPage(lastTocPara);
        
        // 设置正文节的页码从 1 开始
        setPageNumberStart(1);

        // 5. 第二遍遍历：实际渲染正文
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

    /**
     * 在指定段落上添加分节符（下一页）。
     * 这会结束当前节并在下一页开始新的节。
     */
    private void addSectionBreakNextPage(XWPFParagraph paragraph) {
        CTPPr ppr = paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : paragraph.getCTP().addNewPPr();
        CTSectPr sectPr = ppr.isSetSectPr() ? ppr.getSectPr() : ppr.addNewSectPr();
        sectPr.addNewType().setVal(STSectionMark.NEXT_PAGE);
    }

    /**
     * 设置文档 body 级别的 sectPr 的页码起始值。
     * body 的 sectPr 控制文档最后一个节（即正文节）的属性。
     */
    private void setPageNumberStart(int startPage) {
        CTBody body = document.getDocument().getBody();
        CTSectPr sectPr = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
        CTPageNumber pgNum = sectPr.isSetPgNumType() ? sectPr.getPgNumType() : sectPr.addNewPgNumType();
        pgNum.setStart(BigInteger.valueOf(startPage));
    }
}
