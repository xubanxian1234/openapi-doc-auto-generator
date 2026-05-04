package com.docgen.word.factory;

import com.docgen.word.style.WordStyleConstants;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFldChar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;

/**
 * 目录工厂 — 生成 Word 原生 TOC 域
 * 
 * 借助 fldChar 插入标准的 TOC 域代码 (TOC \o "1-3" \h \z \\u)，
 * 依赖文档中已设置的大纲级别 (outlineLvl) 来自动抓取目录项。
 */
public class TocFactory {

    /**
     * 渲染原生 TOC 目录域。
     */
    public void renderNativeTOC(XWPFDocument document) {
        // 目录标题
        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        titlePara.setSpacingAfter(400);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText("目    录");
        titleRun.setBold(true);
        titleRun.setFontSize(20);
        titleRun.setFontFamily(WordStyleConstants.FONT_FAMILY);

        // 插入包含 TOC 域的段落
        XWPFParagraph tocPara = document.createParagraph();
        CTP ctp = tocPara.getCTP();

        // 1. fldChar begin
        CTR beginRun = ctp.addNewR();
        CTFldChar beginFldChar = beginRun.addNewFldChar();
        beginFldChar.setFldCharType(STFldCharType.BEGIN);

        // 2. instrText
        CTR instrRun = ctp.addNewR();
        CTText instrText = instrRun.addNewInstrText();
        // 设置 field 文本，前后加空格确保隔离
        instrText.setStringValue(" TOC \\o \"1-3\" \\h \\z \\u \\* MERGEFORMAT ");

        // 3. fldChar separate
        CTR sepRun = ctp.addNewR();
        CTFldChar sepFldChar = sepRun.addNewFldChar();
        sepFldChar.setFldCharType(STFldCharType.SEPARATE);

        // （可选）插入占位提示文字
        CTR textRun = ctp.addNewR();
        XWPFRun infoRun = new XWPFRun(textRun, (org.apache.poi.xwpf.usermodel.IRunBody) tocPara);
        infoRun.setText("请右键点击此处，选择“更新域”以生成目录...");
        infoRun.setColor("888888");
        infoRun.setItalic(true);

        // 4. fldChar end
        CTR endRun = ctp.addNewR();
        CTFldChar endFldChar = endRun.addNewFldChar();
        endFldChar.setFldCharType(STFldCharType.END);
    }
}
