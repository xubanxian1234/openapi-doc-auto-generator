package com.docgen.model;

import java.util.List;

/**
 * Schema 字段的数据传输对象 — 支持递归嵌套的树形结构。
 *
 * <h3>设计决策：为什么用树形而非完全展平</h3>
 * <p>OpenAPI 的 Schema 天然是树形结构（对象嵌套对象、数组包含对象等）。
 * 如果完全展平为列表，会丢失层级关系信息，导致：
 * <ul>
 *   <li>前端无法正确渲染缩进层级</li>
 *   <li>Word 表格无法体现字段的父子从属关系</li>
 * </ul>
 * 因此采用树形 DTO，每个字段持有 children 列表，前端和 Word 生成时
 * 通过 depth 字段控制缩进层级。</p>
 *
 * <h3>富文本着色标记</h3>
 * <p>description 字段中若包含"非必需"或"默认值"等状态信息，
 * 前端和 Word 生成时需要将这些关键词标记为蓝色 (#0000FF)。
 * 着色逻辑在渲染层处理，DTO 层仅提供原始数据。</p>
 */
public class SchemaFieldDTO {

    /** 字段名称 */
    private String name;

    /** 数据类型（string, integer, object, array 等） */
    private String type;

    /** 数据格式（int32, int64, date-time 等） */
    private String format;

    /** 字段描述 */
    private String description;

    /** 是否必需 */
    private boolean required;

    /** 默认值（字符串化表示） */
    private String defaultValue;

    /** 示例值（字符串化表示） */
    private String example;

    /** 枚举值列表（如果是枚举类型） */
    private List<String> enumValues;

    /**
     * 嵌套深度（从 0 开始）。
     * <p>用于前端缩进渲染和 Word 表格中名称列的缩进控制。</p>
     */
    private int depth;

    /**
     * 子字段列表。
     * <p>当 type=object 时，children 为其 properties 展开的字段列表；
     * 当 type=array 且 items 为 object 时，children 为 items 的 properties。</p>
     */
    private List<SchemaFieldDTO> children;

    /** 原始 Schema 引用名（如 "#/components/schemas/User" 中的 "User"） */
    private String schemaRef;

    public SchemaFieldDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public List<String> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(List<String> enumValues) {
        this.enumValues = enumValues;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public List<SchemaFieldDTO> getChildren() {
        return children;
    }

    public void setChildren(List<SchemaFieldDTO> children) {
        this.children = children;
    }

    public String getSchemaRef() {
        return schemaRef;
    }

    public void setSchemaRef(String schemaRef) {
        this.schemaRef = schemaRef;
    }
}
