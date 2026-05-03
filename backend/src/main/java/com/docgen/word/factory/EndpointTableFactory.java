package com.docgen.word.factory;

import com.docgen.model.ApiEndpointDTO;
import com.docgen.model.ParameterDTO;
import com.docgen.word.style.WordStyleConstants;
import org.apache.poi.xwpf.usermodel.*;

import java.util.List;

/**
 * 接口端点表格工厂 — 渲染 URL 行和请求头行。
 *
 * 每个接口生成一个独立的表格区块，包含：
 * 1. 接口名称标题行
 * 2. URL 行（黄色背景 #FFF2CC）
 * 3. 请求头行（绿色背景 #E2EFDA）
 */
public class EndpointTableFactory extends WordSectionFactory {

    /** 表格固定列数 */
    private static final int COL_COUNT = 5;

    /**
     * 渲染单个端点的头部表格（名称 + URL + 请求头）。
     */
    public XWPFTable render(XWPFDocument document, ApiEndpointDTO endpoint) {
        // 先添加接口标题
        renderEndpointTitle(document, endpoint);

        // 计算需要的行数
        int headerRows = (endpoint.getHeaders() != null) ? endpoint.getHeaders().size() : 0;
        int totalRows = 2 + (headerRows > 0 ? 1 + headerRows : 0); // URL行 + 可能的header标题行 + header数据行

        XWPFTable table = createTable(document, totalRows, COL_COUNT);

        int currentRow = 0;
        currentRow = renderUrlRow(table, currentRow, endpoint);
        renderHeaderRows(table, currentRow, endpoint.getHeaders());

        return table;
    }

    private void renderEndpointTitle(XWPFDocument document, ApiEndpointDTO endpoint) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(300);
        paragraph.setSpacingAfter(100);

        String title = String.format("[%s] %s", endpoint.getMethod(), endpoint.getPath());
        if (endpoint.getSummary() != null) {
            title += " — " + endpoint.getSummary();
        }

        XWPFRun run = paragraph.createRun();
        run.setText(title);
        run.setFontFamily(WordStyleConstants.FONT_FAMILY);
        run.setFontSize(WordStyleConstants.FONT_SIZE_SUBTITLE);
        run.setBold(true);
        run.setColor(WordStyleConstants.COLOR_DEFAULT);

        // 废弃标记
        if (endpoint.isDeprecated()) {
            XWPFRun deprecatedRun = paragraph.createRun();
            deprecatedRun.setText(" [已废弃]");
            deprecatedRun.setFontFamily(WordStyleConstants.FONT_FAMILY);
            deprecatedRun.setFontSize(WordStyleConstants.FONT_SIZE_SUBTITLE);
            deprecatedRun.setColor("FF0000");
            deprecatedRun.setBold(true);
        }
    }

    /**
     * 渲染 URL 行（合并为 2 列：标签 + URL 值）。
     * 返回下一行的索引。
     */
    private int renderUrlRow(XWPFTable table, int rowIndex, ApiEndpointDTO endpoint) {
        XWPFTableRow urlRow = table.getRow(rowIndex);
        styleRow(urlRow, WordStyleConstants.BG_URL);

        setCellText(urlRow.getCell(0), "请求地址");
        String urlText = endpoint.getMethod() + " " + endpoint.getPath();
        setCellText(urlRow.getCell(1), urlText);
        mergeCellsHorizontally(table, rowIndex, 1, COL_COUNT - 1);

        // Content-Type 行
        XWPFTableRow ctRow = table.getRow(rowIndex + 1);
        styleRow(ctRow, WordStyleConstants.BG_URL);
        setCellText(ctRow.getCell(0), "Content-Type");
        String contentType = endpoint.getContentType() != null ? endpoint.getContentType() : "N/A";
        setCellText(ctRow.getCell(1), contentType);
        mergeCellsHorizontally(table, rowIndex + 1, 1, COL_COUNT - 1);

        return rowIndex + 2;
    }

    /**
     * 渲染请求头行（如果有请求头）。
     */
    private void renderHeaderRows(XWPFTable table, int startRow, List<ParameterDTO> headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }

        // 标题行
        XWPFTableRow titleRow = table.getRow(startRow);
        styleRow(titleRow, WordStyleConstants.BG_HEADER);
        setCellText(titleRow.getCell(0), "请求头");
        mergeCellsHorizontally(table, startRow, 0, COL_COUNT - 1);

        // 暂不逐行展开请求头参数（在 ParameterTableFactory 中统一处理）
    }
}
