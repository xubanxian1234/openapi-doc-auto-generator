package com.docgen.word.factory;

import com.docgen.model.SchemaFieldDTO;
import com.docgen.word.style.WordStyleConstants;
import org.apache.poi.xwpf.usermodel.*;

import java.util.List;

/**
 * 参数表格工厂 — 渲染请求参数和响应参数区域。
 *
 * 职责：
 * - 请求参数区域：蓝色背景 (#D9E1F2)
 * - 响应参数区域：灰色背景 (#D9D9D9)
 * - 支持树形字段的缩进渲染（通过 SchemaFieldDTO.depth 控制前缀缩进）
 * - 描述字段中的"非必需"、"默认值"等关键词蓝色着色
 */
public class ParameterTableFactory extends WordSectionFactory {

    /** 参数表格列定义：名称、类型、必需、默认值、描述 */
    private static final String[] COLUMN_HEADERS = {"参数名称", "类型", "是否必需", "默认值", "描述"};
    private static final int COL_COUNT = COLUMN_HEADERS.length;

    /**
     * 渲染请求参数表格。
     */
    public void renderRequestParams(XWPFDocument document, List<SchemaFieldDTO> fields) {
        renderParamSection(document, "请求参数", WordStyleConstants.BG_REQUEST, fields);
    }

    /**
     * 渲染响应参数表格。
     */
    public void renderResponseParams(XWPFDocument document, List<SchemaFieldDTO> fields) {
        renderParamSection(document, "返回参数", WordStyleConstants.BG_RESPONSE, fields);
    }

    /**
     * 通用参数区域渲染逻辑。
     */
    private void renderParamSection(
            XWPFDocument document, String sectionTitle, String bgColor, List<SchemaFieldDTO> fields) {

        if (fields == null || fields.isEmpty()) {
            return;
        }

        // 展平树形结构为列表（带缩进信息）
        List<SchemaFieldDTO> flatFields = flattenFields(fields);

        // 行数 = 区域标题行(1) + 列标题行(1) + 数据行(N)
        int totalRows = 2 + flatFields.size();
        XWPFTable table = createTable(document, totalRows, COL_COUNT);

        int rowIndex = 0;
        rowIndex = renderSectionTitle(table, rowIndex, sectionTitle, bgColor);
        rowIndex = renderColumnHeaders(table, rowIndex, bgColor);
        renderDataRows(table, rowIndex, flatFields);
    }

    /**
     * 渲染区域标题行（如"请求参数"、"返回参数"）。
     */
    private int renderSectionTitle(XWPFTable table, int rowIndex, String title, String bgColor) {
        XWPFTableRow row = table.getRow(rowIndex);
        styleRow(row, bgColor);
        setCellText(row.getCell(0), title);
        mergeCellsHorizontally(table, rowIndex, 0, COL_COUNT - 1);
        return rowIndex + 1;
    }

    /**
     * 渲染列标题行。
     */
    private int renderColumnHeaders(XWPFTable table, int rowIndex, String bgColor) {
        XWPFTableRow row = table.getRow(rowIndex);
        styleRow(row, bgColor);
        for (int i = 0; i < COLUMN_HEADERS.length; i++) {
            XWPFTableCell cell = row.getCell(i);
            setCellText(cell, COLUMN_HEADERS[i]);
        }
        return rowIndex + 1;
    }

    /**
     * 渲染数据行。每行对应一个展平后的 SchemaFieldDTO。
     */
    private void renderDataRows(XWPFTable table, int startRow, List<SchemaFieldDTO> fields) {
        for (int i = 0; i < fields.size(); i++) {
            SchemaFieldDTO field = fields.get(i);
            XWPFTableRow row = table.getRow(startRow + i);

            // 设置边框
            for (XWPFTableCell cell : row.getTableCells()) {
                setCellBorders(cell);
            }

            renderFieldRow(row, field);
        }
    }

    /**
     * 渲染单个字段行。
     */
    private void renderFieldRow(XWPFTableRow row, SchemaFieldDTO field) {
        // 名称列：根据 depth 添加缩进前缀
        String indentedName = buildIndentedName(field);
        setCellText(row.getCell(0), indentedName);

        // 类型列
        setCellText(row.getCell(1), orDefault(field.getType(), ""));

        // 必需列：使用富文本着色
        String requiredText = field.isRequired() ? "必需" : "非必需";
        setCellRichText(row.getCell(2), requiredText, false);

        // 默认值列
        setCellText(row.getCell(3), orDefault(field.getDefaultValue(), ""));

        // 描述列：使用富文本着色（可能包含"默认值"等关键词）
        String description = buildDescription(field);
        setCellRichText(row.getCell(4), description, false);
    }

    /**
     * 根据字段深度生成带缩进的名称。
     * 每层缩进用 "└ " 前缀表示层级关系。
     */
    private String buildIndentedName(SchemaFieldDTO field) {
        if (field.getDepth() <= 0 || field.getName() == null) {
            return field.getName() != null ? field.getName() : "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < field.getDepth(); i++) {
            sb.append("  ");
        }
        sb.append("└ ").append(field.getName());
        return sb.toString();
    }

    /**
     * 构建描述文本。如果字段有枚举值，附加枚举信息。
     */
    private String buildDescription(SchemaFieldDTO field) {
        StringBuilder sb = new StringBuilder();
        if (field.getDescription() != null) {
            sb.append(field.getDescription());
        }
        if (field.getEnumValues() != null && !field.getEnumValues().isEmpty()) {
            sb.append(" 枚举值: ").append(String.join(", ", field.getEnumValues()));
        }
        if (field.getDefaultValue() != null && !field.getDefaultValue().isEmpty()) {
            sb.append(" 默认值: ").append(field.getDefaultValue());
        }
        return sb.toString();
    }

    /**
     * 递归展平树形字段结构为有序列表。
     * 保留 depth 信息用于渲染时的缩进控制。
     */
    private List<SchemaFieldDTO> flattenFields(List<SchemaFieldDTO> fields) {
        List<SchemaFieldDTO> result = new java.util.ArrayList<>();
        for (SchemaFieldDTO field : fields) {
            addFieldAndChildren(result, field);
        }
        return result;
    }

    private void addFieldAndChildren(List<SchemaFieldDTO> result, SchemaFieldDTO field) {
        // 只添加有名称的字段（匿名顶层对象不添加自身，只展开其子级）
        if (field.getName() != null) {
            result.add(field);
        }
        if (field.getChildren() != null) {
            for (SchemaFieldDTO child : field.getChildren()) {
                addFieldAndChildren(result, child);
            }
        }
    }

    private String orDefault(String value, String defaultValue) {
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
}
