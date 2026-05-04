package com.docgen.word.factory;

import com.docgen.word.style.WordStyleConstants;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * 目录工厂 — 用于生成静态的、带内部书签跳转链接的目录。
 *
 * 生成的目录格式参考商业文档（如 CrownCAD 第三方集成解决方案），
 * 采用 "序号 + Tab + 标题 + 虚线引导 + 页码" 的排版方式。
 *
 * 页码通过 PAGEREF 域引用书签位置，打开文档后需更新域（Ctrl+A, F9）
 * 以显示真实页码。enforceUpdateFields() 会在文件首次打开时自动触发更新。
 */
public class TocFactory {

    private final List<TocEntry> entries = new ArrayList<>();
    private int bookmarkIdCounter = 1000;

    public static class TocEntry {
        public String title;
        public int level; // 1 for major tag, 2 for endpoint
        public String bookmarkName;
        public String bookmarkId;

        public TocEntry(String title, int level, String bookmarkName, String bookmarkId) {
            this.title = title;
            this.level = level;
            this.bookmarkName = bookmarkName;
            this.bookmarkId = bookmarkId;
        }
    }

    /**
     * 添加目录项并返回生成的书签ID和名称。
     */
    public TocEntry addEntry(String title, int level) {
        String bId = String.valueOf(bookmarkIdCounter++);
        String bName = "bookmark_" + bId;
        TocEntry entry = new TocEntry(title, level, bName, bId);
        entries.add(entry);
        return entry;
    }

    /**
     * 渲染整个目录到文档中。
     */
    public void render(XWPFDocument document) {
        if (entries.isEmpty()) {
            return;
        }

        // 目录标题（居中，加大字号和字间距）
        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        titlePara.setSpacingAfter(400);
        org.apache.poi.xwpf.usermodel.XWPFRun titleRun = titlePara.createRun();
        titleRun.setText("目    录");
        titleRun.setBold(true);
        titleRun.setFontSize(20);
        titleRun.setFontFamily(WordStyleConstants.FONT_FAMILY);

        for (TocEntry entry : entries) {
            XWPFParagraph p = document.createParagraph();
            p.setSpacingAfter(60);
            p.setSpacingBefore(60);

            // 设置制表位：右对齐 + 虚线引导
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr ppr = p.getCTP().isSetPPr()
                    ? p.getCTP().getPPr()
                    : p.getCTP().addNewPPr();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTabs tabs = ppr.isSetTabs()
                    ? ppr.getTabs()
                    : ppr.addNewTabs();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTabStop tab = tabs.addNewTab();
            tab.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTabJc.RIGHT);
            tab.setLeader(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTabTlc.DOT);
            tab.setPos(BigInteger.valueOf(8500)); // 右侧对齐位置

            // 缩进处理
            if (entry.level == 1) {
                p.setIndentationLeft(0);
            } else if (entry.level == 2) {
                p.setIndentationLeft(400);
            }

            // 创建超链接指向书签
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink hyperlink = p.getCTP().addNewHyperlink();
            hyperlink.setAnchor(entry.bookmarkName);

            // --- 标题文本 Run ---
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR titleR = hyperlink.addNewR();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr titleRpr = titleR.addNewRPr();

            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTColor titleColor = titleRpr.addNewColor();
            titleColor.setVal("333333");

            if (entry.level == 1) {
                titleRpr.addNewB();
            }

            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts titleFonts = titleRpr.addNewRFonts();
            titleFonts.setAscii(WordStyleConstants.FONT_FAMILY);
            titleFonts.setEastAsia(WordStyleConstants.FONT_FAMILY);
            titleFonts.setHAnsi(WordStyleConstants.FONT_FAMILY);

            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHpsMeasure titleSz = titleRpr.addNewSz();
            titleSz.setVal(BigInteger.valueOf(entry.level == 1 ? 24 : 20)); // 12pt / 10pt (half-points)

            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText titleText = titleR.addNewT();
            titleText.setStringValue(entry.title);

            // --- Tab Run（虚线引导）---
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR tabR = hyperlink.addNewR();
            // 复制同样的字体属性
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr tabRpr = tabR.addNewRPr();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts tabFonts = tabRpr.addNewRFonts();
            tabFonts.setAscii(WordStyleConstants.FONT_FAMILY);
            tabFonts.setEastAsia(WordStyleConstants.FONT_FAMILY);
            tabRpr.addNewSz().setVal(BigInteger.valueOf(entry.level == 1 ? 24 : 20));
            tabRpr.addNewColor().setVal("333333");
            tabR.addNewTab();

            // --- PAGEREF 域（页码）---
            // 将 PAGEREF 放在超链接内部，使得页码也是可点击的
            // 使用 fldChar 方式代替 fldSimple，兼容性更好
            addPageRefField(hyperlink, entry.bookmarkName, entry.level);
        }
    }

    /**
     * 使用 fldChar (begin/separate/end) 方式插入 PAGEREF 域。
     * 相比 fldSimple 方式，此方法兼容性更好，在 WPS 和 Office 中都能正确更新。
     */
    private void addPageRefField(
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink hyperlink,
            String bookmarkName, int level) {

        BigInteger fontSize = BigInteger.valueOf(level == 1 ? 24 : 20);

        // 1. fldChar BEGIN
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR beginR = hyperlink.addNewR();
        applyRunProps(beginR, fontSize);
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFldChar beginChar = beginR.addNewFldChar();
        beginChar.setFldCharType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType.BEGIN);

        // 2. instrText
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR instrR = hyperlink.addNewR();
        applyRunProps(instrR, fontSize);
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText instrText = instrR.addNewInstrText();
        instrText.setStringValue(" PAGEREF " + bookmarkName + " \\h ");

        // 3. fldChar SEPARATE
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR sepR = hyperlink.addNewR();
        applyRunProps(sepR, fontSize);
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFldChar sepChar = sepR.addNewFldChar();
        sepChar.setFldCharType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType.SEPARATE);

        // 4. 占位文本（更新前显示，更新后自动替换为真实页码）
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR textR = hyperlink.addNewR();
        applyRunProps(textR, fontSize);
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText pageText = textR.addNewT();
        pageText.setStringValue("0");

        // 5. fldChar END
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR endR = hyperlink.addNewR();
        applyRunProps(endR, fontSize);
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFldChar endChar = endR.addNewFldChar();
        endChar.setFldCharType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType.END);
    }

    /**
     * 统一设置 Run 的字体属性。
     */
    private void applyRunProps(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR r, BigInteger fontSize) {
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr rpr = r.addNewRPr();
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts fonts = rpr.addNewRFonts();
        fonts.setAscii(WordStyleConstants.FONT_FAMILY);
        fonts.setEastAsia(WordStyleConstants.FONT_FAMILY);
        fonts.setHAnsi(WordStyleConstants.FONT_FAMILY);
        rpr.addNewSz().setVal(fontSize);
        rpr.addNewColor().setVal("333333");
    }
}
