package com.docgen.word.factory;

import com.docgen.word.style.WordStyleConstants;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.util.ArrayList;
import java.util.List;

/**
 * 目录工厂 — 用于生成静态的、带内部书签跳转链接的目录。
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
     * 在指定位置渲染整个目录。
     * 必须在文档内容生成完毕后，或者预留的空段落处进行渲染。
     * 但因为 POI 的游标限制，通常我们在开头就生成目录，这就需要我们使用类似延迟写入，
     * 或者先建空段落，再通过段落游标插入内容的方式。
     * 实际上，最简单的做法是在最后把生成的段落移到前面，或者在 Document 层面按顺序渲染。
     */
    public void render(XWPFDocument document) {
        if (entries.isEmpty()) {
            return;
        }

        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.LEFT);
        titlePara.setSpacingAfter(300);
        org.apache.poi.xwpf.usermodel.XWPFRun titleRun = titlePara.createRun();
        titleRun.setText("目录");
        titleRun.setBold(true);
        titleRun.setFontSize(18);
        titleRun.setFontFamily(WordStyleConstants.FONT_FAMILY);

        for (TocEntry entry : entries) {
            XWPFParagraph p = document.createParagraph();
            p.setSpacingAfter(100);
            p.setSpacingBefore(100);
            
            // 缩进处理
            if (entry.level == 1) {
                p.setIndentationLeft(0);
            } else if (entry.level == 2) {
                p.setIndentationLeft(400); // indent 20 points
            }

            // 在 POI 5.x 中，创建指向书签的内部链接
            // CTHyperlink 方式
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink hyperlink = p.getCTP().addNewHyperlink();
            hyperlink.setAnchor(entry.bookmarkName);
            
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR ctr = hyperlink.addNewR();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr rpr = ctr.addNewRPr();
            
            // 使用深灰色，避免刺眼的纯蓝色，显得更优雅
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTColor color = rpr.addNewColor();
            color.setVal("333333");
            
            // 级别 1 加粗
            if (entry.level == 1) {
                rpr.addNewB();
            }
            
            // 设置字体和大小
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts fonts = rpr.addNewRFonts();
            fonts.setAscii(WordStyleConstants.FONT_FAMILY);
            fonts.setEastAsia(WordStyleConstants.FONT_FAMILY);
            fonts.setHAnsi(WordStyleConstants.FONT_FAMILY);
            
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHpsMeasure sz = rpr.addNewSz();
            sz.setVal(java.math.BigInteger.valueOf(entry.level == 1 ? 24 : 20)); // 12pt or 10pt (value is half-points)
            
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText text = ctr.addNewT();
            text.setStringValue(entry.title);

            // 增加制表符用于生成虚线引导 (Dotted Leader)
            ctr.addNewTab();

            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr ppr = p.getCTP().getPPr();
            if (ppr == null) ppr = p.getCTP().addNewPPr();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTabs tabs = ppr.isSetTabs() ? ppr.getTabs() : ppr.addNewTabs();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTabStop tab = tabs.addNewTab();
            tab.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTabJc.RIGHT);
            tab.setLeader(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTabTlc.DOT);
            tab.setPos(java.math.BigInteger.valueOf(8500)); // 右侧对齐位置

            // 追加页码域 (PAGEREF)，打开文档并更新域时会自动显示真实页码
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSimpleField pageRef = p.getCTP().addNewFldSimple();
            pageRef.setInstr(" PAGEREF " + entry.bookmarkName + " \\h ");
            
            // 预留一个默认页码文本，防止更新前没有内容
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR pageRefR = pageRef.addNewR();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr pageRefRpr = pageRefR.addNewRPr();
            pageRefRpr.addNewColor().setVal("333333");
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts pageFonts = pageRefRpr.addNewRFonts();
            pageFonts.setAscii(WordStyleConstants.FONT_FAMILY);
            pageRefRpr.addNewSz().setVal(java.math.BigInteger.valueOf(entry.level == 1 ? 24 : 20));
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText pageRefText = pageRefR.addNewT();
            pageRefText.setStringValue("1");

            // 虽然创建了底层 CTHyperlink，但为了段落高度等排版兼容性，也可以使用 XWPFHyperlinkRun（如果版本支持）
            // 注意：直接操作 CT 元素时，不需要再通过 p.createRun() 添加文本，否则会重复
        }
    }
}
