package com.docgen.word.factory;

import com.docgen.word.style.WordStyleConstants;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.math.BigInteger;

/**
 * Word 区块工厂的抽象基类 — 模板方法模式 (Template Method)。
 *
 * 封装了所有工厂共用的 POI 底层操作（创建单元格、设置背景色、设置边框、
 * 富文本着色等），子类只需关注业务逻辑。
 *
 * 设计意图：POI 的 API 极其冗长（设置一个单元格背景色需要 5+ 行代码），
 * 如果每个工厂都写一遍，代码重复率极高。抽取到基类后，
 * 子类通过调用 setCellBackground / addStyledRun 等方法即可完成样式设置。
 */
public abstract class WordSectionFactory {

    /**
     * 在文档中创建一个新表格。
     */
    protected XWPFTable createTable(XWPFDocument document, int rows, int cols) {
        XWPFTable table = document.createTable(rows, cols);
        setTableWidth(table);
        return table;
    }

    /**
     * 设置表格宽度为页面 100%。
     */
    protected void setTableWidth(XWPFTable table) {
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        if (tblPr == null) {
            tblPr = table.getCTTbl().addNewTblPr();
        }
        CTTblWidth width = tblPr.addNewTblW();
        width.setType(STTblWidth.PCT);
        width.setW(BigInteger.valueOf(5000)); // 5000 = 100% in PCT units
    }

    /**
     * 设置表格每列的宽度 (PCT)
     */
    protected void setTableColWidths(XWPFTable table, int[] widths) {
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid grid = table.getCTTbl().addNewTblGrid();
        for (int w : widths) {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGridCol col = grid.addNewGridCol();
            col.setW(BigInteger.valueOf(w));
        }
    }

    /**
     * 设置单元格背景色。
     *
     * @param cell  目标单元格
     * @param color RGB 颜色值（不带 #，如 "FFF2CC"）
     */
    protected void setCellBackground(XWPFTableCell cell, String color) {
        CTTcPr tcPr = getCellProperties(cell);
        CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
        shd.setVal(STShd.CLEAR);
        shd.setColor("auto");
        shd.setFill(color);
    }

    /**
     * 为单元格设置黑色实线四边框。
     */
    protected void setCellBorders(XWPFTableCell cell) {
        CTTcPr tcPr = getCellProperties(cell);
        CTTcBorders borders = tcPr.isSetTcBorders() ? tcPr.getTcBorders() : tcPr.addNewTcBorders();

        setBorder(borders.isSetTop() ? borders.getTop() : borders.addNewTop());
        setBorder(borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom());
        setBorder(borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft());
        setBorder(borders.isSetRight() ? borders.getRight() : borders.addNewRight());
    }

    private void setBorder(CTBorder border) {
        border.setVal(STBorder.SINGLE);
        border.setSz(BigInteger.valueOf(WordStyleConstants.BORDER_SIZE));
        border.setColor(WordStyleConstants.BORDER_COLOR);
        border.setSpace(BigInteger.ZERO);
    }

    /**
     * 设置整行所有单元格的背景色和边框。
     */
    protected void styleRow(XWPFTableRow row, String bgColor) {
        for (XWPFTableCell cell : row.getTableCells()) {
            setCellBackground(cell, bgColor);
            setCellBorders(cell);
        }
    }

    /**
     * 向单元格写入纯文本（带字体样式）。
     */
    protected void setCellText(XWPFTableCell cell, String text) {
        cell.removeParagraph(0);
        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setSpacingBefore(0);
        paragraph.setSpacingAfter(0);

        XWPFRun run = paragraph.createRun();
        run.setText(text != null ? text : "");
        run.setFontFamily(WordStyleConstants.FONT_FAMILY);
        run.setFontSize(WordStyleConstants.FONT_SIZE_TABLE);
        run.setColor(WordStyleConstants.COLOR_DEFAULT);
    }

    /**
     * 向单元格写入富文本（支持关键词蓝色着色）。
     *
     * 将描述文本中的关键词（如"非必需"、"默认值"）拆分为多个 XWPFRun，
     * 关键词 Run 设置蓝色字体，其他部分保持黑色。
     *
     * @param cell        目标单元格
     * @param text        原始文本
     * @param isBold      是否加粗
     */
    protected void setCellRichText(XWPFTableCell cell, String text, boolean isBold) {
        if (text == null || text.isEmpty()) {
            setCellText(cell, "");
            return;
        }

        cell.removeParagraph(0);
        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setSpacingBefore(0);
        paragraph.setSpacingAfter(0);

        // 扫描并拆分关键词
        renderRichText(paragraph, text, isBold);
    }

    /**
     * 渲染富文本：扫描文本中的关键词，将其着色为蓝色。
     *
     * 算法：从左到右扫描文本，找到最近的关键词位置，
     * 将关键词前的文本作为普通 Run，关键词本身作为蓝色 Run，
     * 然后继续扫描剩余文本。
     */
    protected void renderRichText(XWPFParagraph paragraph, String text, boolean isBold) {
        String remaining = text;

        while (!remaining.isEmpty()) {
            int[] match = findFirstKeyword(remaining);

            // 没有找到关键词，剩余全部作为普通文本
            if (match == null) {
                addStyledRun(paragraph, remaining, WordStyleConstants.COLOR_DEFAULT, isBold);
                break;
            }

            // 关键词前的普通文本
            if (match[0] > 0) {
                addStyledRun(paragraph, remaining.substring(0, match[0]),
                        WordStyleConstants.COLOR_DEFAULT, isBold);
            }

            // 关键词本身（蓝色）
            addStyledRun(paragraph, remaining.substring(match[0], match[1]),
                    WordStyleConstants.COLOR_HIGHLIGHT, isBold);

            remaining = remaining.substring(match[1]);
        }
    }

    /**
     * 在文本中找到第一个出现的关键词。
     *
     * @return [startIndex, endIndex] 或 null（未找到）
     */
    private int[] findFirstKeyword(String text) {
        int earliestStart = Integer.MAX_VALUE;
        int earliestEnd = -1;

        for (String keyword : WordStyleConstants.HIGHLIGHT_KEYWORDS) {
            int idx = text.indexOf(keyword);
            if (idx >= 0 && idx < earliestStart) {
                earliestStart = idx;
                earliestEnd = idx + keyword.length();
            }
        }

        return earliestEnd > 0 ? new int[]{earliestStart, earliestEnd} : null;
    }

    /**
     * 添加带样式的 XWPFRun。
     */
    protected void addStyledRun(XWPFParagraph paragraph, String text, String color, boolean isBold) {
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontFamily(WordStyleConstants.FONT_FAMILY);
        run.setFontSize(WordStyleConstants.FONT_SIZE_TABLE);
        run.setColor(color);
        run.setBold(isBold);
    }

    /**
     * 合并单元格（同一行内跨列合并）。
     *
     * @param table    表格对象
     * @param row      行索引
     * @param fromCol  起始列
     * @param toCol    结束列（包含）
     */
    protected void mergeCellsHorizontally(XWPFTable table, int row, int fromCol, int toCol) {
        XWPFTableRow tableRow = table.getRow(row);
        for (int col = fromCol; col <= toCol; col++) {
            XWPFTableCell cell = tableRow.getCell(col);
            CTTcPr tcPr = getCellProperties(cell);
            CTHMerge hMerge = tcPr.isSetHMerge() ? tcPr.getHMerge() : tcPr.addNewHMerge();
            hMerge.setVal(col == fromCol ? STMerge.RESTART : STMerge.CONTINUE);
        }
    }

    /**
     * 获取单元格属性（确保非 null）。
     */
    private CTTcPr getCellProperties(XWPFTableCell cell) {
        CTTcPr tcPr = cell.getCTTc().getTcPr();
        if (tcPr == null) {
            tcPr = cell.getCTTc().addNewTcPr();
        }
        return tcPr;
    }
}
