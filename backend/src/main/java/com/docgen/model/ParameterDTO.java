package com.docgen.model;

/**
 * 请求头/简单参数的数据传输对象。
 *
 * <h3>适用范围</h3>
 * <p>用于描述 HTTP 请求头（in=header）中的简单键值对参数。
 * 与 {@link SchemaFieldDTO} 不同，ParameterDTO 不涉及嵌套结构，
 * 仅用于头信息等扁平参数的展示。</p>
 */
public class ParameterDTO {

    /** 参数名称 */
    private String name;

    /** 参数位置（header, cookie 等） */
    private String in;

    /** 参数描述 */
    private String description;

    /** 是否必需 */
    private boolean required;

    /** 数据类型（string, integer 等） */
    private String type;

    /** 默认值 */
    private String defaultValue;

    /** 示例值 */
    private String example;

    public ParameterDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIn() {
        return in;
    }

    public void setIn(String in) {
        this.in = in;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
}
