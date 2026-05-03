package com.docgen.word.factory;

import com.docgen.model.ApiEndpointDTO;
import com.docgen.model.ParameterDTO;
import com.docgen.model.SchemaFieldDTO;
import com.docgen.word.style.WordStyleConstants;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.util.ArrayList;
import java.util.List;

/**
 * 完整端点工厂 — 负责渲染类似 "3.1 创建项目" 的大区域，
 * 包括业务说明、接口说明，以及一个合并的三列表格（URL、请求头、请求参数、响应参数）。
 */
public class CombinedEndpointFactory extends WordSectionFactory {

    public void render(XWPFDocument document, ApiEndpointDTO endpoint, int endpointIndex) {
        // 1. 端点标题
        renderEndpointTitle(document, endpoint, endpointIndex);

        // 2. 业务说明
        renderSectionHeading(document, "业务说明");
        renderBusinessDescription(document, endpoint);

        // 3. 接口说明
        renderSectionHeading(document, "接口说明");

        // 4. 接口表格 (3列)
        renderEndpointTable(document, endpoint);
    }

    private void renderEndpointTitle(XWPFDocument document, ApiEndpointDTO endpoint, int index) {
        XWPFParagraph p = document.createParagraph();
        p.setSpacingBefore(400);
        p.setSpacingAfter(200);

        XWPFRun run = p.createRun();
        String titleText = "3." + index + " " + (endpoint.getSummary() != null ? endpoint.getSummary() : endpoint.getPath());
        run.setText(titleText);
        run.setFontFamily(WordStyleConstants.FONT_FAMILY);
        run.setFontSize(14);
        run.setBold(true);

        if (endpoint.isDeprecated()) {
            XWPFRun deprecatedRun = p.createRun();
            deprecatedRun.setText(" [已废弃]");
            deprecatedRun.setFontFamily(WordStyleConstants.FONT_FAMILY);
            deprecatedRun.setFontSize(12);
            deprecatedRun.setColor("FF0000");
            deprecatedRun.setBold(true);
        }
    }

    private void renderSectionHeading(XWPFDocument document, String text) {
        XWPFParagraph p = document.createParagraph();
        p.setSpacingBefore(200);
        p.setSpacingAfter(100);

        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontFamily(WordStyleConstants.FONT_FAMILY);
        run.setFontSize(12);
        run.setBold(true);
    }

    private void renderBusinessDescription(XWPFDocument document, ApiEndpointDTO endpoint) {
        XWPFParagraph p = document.createParagraph();
        p.setSpacingAfter(200);
        XWPFRun run = p.createRun();
        String desc = endpoint.getDescription() != null ? endpoint.getDescription() :
                (endpoint.getSummary() != null ? endpoint.getSummary() : "无详细业务说明。");
        run.setText(desc);
        run.setFontFamily(WordStyleConstants.FONT_FAMILY);
        run.setFontSize(10);
    }

    private void renderEndpointTable(XWPFDocument document, ApiEndpointDTO endpoint) {
        // 先计算需要的行数
        int rowCount = 4; // URL区至少4行: 标题, URL, method, Content-type
        if (endpoint.getHeaders() != null && !endpoint.getHeaders().isEmpty()) {
            rowCount += 1 + endpoint.getHeaders().size(); // 标题 + 列表
        }
        
        List<SchemaFieldDTO> reqFields = flattenFields(endpoint.getRequestFields());
        if (!reqFields.isEmpty()) {
            rowCount += 2 + reqFields.size(); // 标题 + 表头 + 列表
        }

        List<SchemaFieldDTO> resFields = flattenFields(endpoint.getResponseFields());
        if (!resFields.isEmpty()) {
            rowCount += 1 + resFields.size(); // 标题 + 列表 (无表头)
        }

        XWPFTable table = createTable(document, rowCount, 3);
        setTableColWidths(table, new int[]{1250, 1000, 2750}); // 25%, 20%, 55% of 5000
        int currentRow = 0;

        // 渲染 URL 区
        currentRow = renderUrlSection(table, currentRow, endpoint);

        // 渲染请求头
        if (endpoint.getHeaders() != null && !endpoint.getHeaders().isEmpty()) {
            currentRow = renderHeaderSection(table, currentRow, endpoint.getHeaders());
        }

        // 渲染请求参数
        if (!reqFields.isEmpty()) {
            currentRow = renderRequestSection(table, currentRow, reqFields);
        }

        // 渲染响应参数
        if (!resFields.isEmpty()) {
            renderResponseSection(table, currentRow, resFields);
        }
    }

    private int renderUrlSection(XWPFTable table, int startRow, ApiEndpointDTO endpoint) {
        // 标题行
        XWPFTableRow titleRow = table.getRow(startRow);
        styleRow(titleRow, WordStyleConstants.BG_URL);
        setCellText(titleRow.getCell(0), "请求 URL");
        mergeCellsHorizontally(table, startRow, 0, 2);

        // URL 行
        XWPFTableRow urlRow = table.getRow(startRow + 1);
        styleRow(urlRow, WordStyleConstants.BG_DEFAULT);
        setCellText(urlRow.getCell(0), "URL");
        setCellText(urlRow.getCell(1), endpoint.getPath());
        mergeCellsHorizontally(table, startRow + 1, 1, 2);

        // method 行
        XWPFTableRow methodRow = table.getRow(startRow + 2);
        styleRow(methodRow, WordStyleConstants.BG_DEFAULT);
        setCellText(methodRow.getCell(0), "method");
        setCellText(methodRow.getCell(1), endpoint.getMethod());
        mergeCellsHorizontally(table, startRow + 2, 1, 2);

        // Content-type 行
        XWPFTableRow ctRow = table.getRow(startRow + 3);
        styleRow(ctRow, WordStyleConstants.BG_DEFAULT);
        setCellText(ctRow.getCell(0), "Content-type");
        setCellText(ctRow.getCell(1), endpoint.getContentType() != null ? endpoint.getContentType() : "application/json");
        mergeCellsHorizontally(table, startRow + 3, 1, 2);

        return startRow + 4;
    }

    private int renderHeaderSection(XWPFTable table, int startRow, List<ParameterDTO> headers) {
        XWPFTableRow titleRow = table.getRow(startRow);
        styleRow(titleRow, WordStyleConstants.BG_HEADER);
        setCellText(titleRow.getCell(0), "请求头");
        mergeCellsHorizontally(table, startRow, 0, 2);

        int currentRow = startRow + 1;
        for (ParameterDTO header : headers) {
            XWPFTableRow row = table.getRow(currentRow++);
            styleRow(row, WordStyleConstants.BG_DEFAULT);
            setCellText(row.getCell(0), header.getName());
            
            String desc = header.getExample() != null ? header.getExample() : 
                         (header.getDescription() != null ? header.getDescription() : header.getType());
            setCellText(row.getCell(1), desc);
            mergeCellsHorizontally(table, currentRow - 1, 1, 2);
        }
        return currentRow;
    }

    private int renderRequestSection(XWPFTable table, int startRow, List<SchemaFieldDTO> fields) {
        XWPFTableRow titleRow = table.getRow(startRow);
        styleRow(titleRow, WordStyleConstants.BG_REQUEST);
        setCellText(titleRow.getCell(0), "请求参数");
        mergeCellsHorizontally(table, startRow, 0, 2);

        XWPFTableRow headerRow = table.getRow(startRow + 1);
        styleRow(headerRow, WordStyleConstants.BG_DEFAULT);
        setCellRichText(headerRow.getCell(0), "参数名", true);
        setCellRichText(headerRow.getCell(1), "类型", true);
        setCellRichText(headerRow.getCell(2), "描述", true);

        int currentRow = startRow + 2;
        for (SchemaFieldDTO field : fields) {
            XWPFTableRow row = table.getRow(currentRow++);
            styleRow(row, WordStyleConstants.BG_DEFAULT);
            
            String indent = "    ".repeat(field.getDepth());
            String prefix = field.getDepth() > 0 ? indent + "└ " : "";
            setCellText(row.getCell(0), prefix + field.getName());
            setCellText(row.getCell(1), field.getType());
            setCellRichText(row.getCell(2), buildDescription(field), false);
        }
        return currentRow;
    }

    private int renderResponseSection(XWPFTable table, int startRow, List<SchemaFieldDTO> fields) {
        XWPFTableRow titleRow = table.getRow(startRow);
        styleRow(titleRow, WordStyleConstants.BG_RESPONSE);
        setCellText(titleRow.getCell(0), "返回参数");
        mergeCellsHorizontally(table, startRow, 0, 2);

        int currentRow = startRow + 1;
        for (SchemaFieldDTO field : fields) {
            XWPFTableRow row = table.getRow(currentRow++);
            styleRow(row, WordStyleConstants.BG_DEFAULT);
            
            String indent = "    ".repeat(field.getDepth());
            String prefix = field.getDepth() > 0 ? indent + "└ " : "";
            setCellText(row.getCell(0), prefix + field.getName());
            setCellText(row.getCell(1), field.getType());
            setCellRichText(row.getCell(2), buildDescription(field), false);
        }
        return currentRow;
    }

    private List<SchemaFieldDTO> flattenFields(List<SchemaFieldDTO> fields) {
        List<SchemaFieldDTO> result = new ArrayList<>();
        if (fields == null) return result;
        for (SchemaFieldDTO field : fields) {
            flattenRecursive(result, field);
        }
        return result;
    }

    private void flattenRecursive(List<SchemaFieldDTO> result, SchemaFieldDTO field) {
        if (field.getName() != null) {
            result.add(field);
        }
        if (field.getChildren() != null) {
            for (SchemaFieldDTO child : field.getChildren()) {
                flattenRecursive(result, child);
            }
        }
    }

    private String buildDescription(SchemaFieldDTO field) {
        StringBuilder sb = new StringBuilder();
        if (field.getDescription() != null) {
            sb.append(field.getDescription());
        }
        if (field.getEnumValues() != null && !field.getEnumValues().isEmpty()) {
            sb.append(" 枚举值: ").append(String.join(", ", field.getEnumValues())).append("。");
        }
        
        StringBuilder extra = new StringBuilder();
        if (field.getDefaultValue() != null) {
            extra.append("默认值为 ").append(field.getDefaultValue()).append("，");
        }
        extra.append(field.isRequired() ? "" : "非必需");
        
        if (extra.length() > 0) {
            if (sb.length() > 0 && !sb.toString().endsWith("。") && !sb.toString().endsWith("，")) {
                sb.append("，");
            }
            sb.append(extra).append("。");
        }
        
        return sb.toString();
    }
}
