package com.docgen.service.strategy;

import com.docgen.model.SchemaFieldDTO;
import io.swagger.v3.oas.models.media.Schema;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基本类型 Schema 解析策略。
 * 处理 string/integer/number/boolean 等非复合类型。
 * 当 format 不为空时，type 格式化为 "type(format)"，如 "integer(int64)"。
 */
public class PrimitiveSchemaStrategy implements SchemaParseStrategy {

    @Override
    public List<SchemaFieldDTO> parse(
            String fieldName, Schema<?> schema, Set<String> requiredSet,
            Map<String, Schema> allSchemas, int depth, Set<String> visitedRefs) {

        SchemaFieldDTO field = new SchemaFieldDTO();
        field.setName(fieldName);
        field.setType(formatType(schema));
        field.setFormat(schema.getFormat());
        field.setDescription(schema.getDescription());
        field.setDepth(depth);
        field.setRequired(requiredSet != null && requiredSet.contains(fieldName));
        field.setDefaultValue(schema.getDefault() != null ? String.valueOf(schema.getDefault()) : null);
        field.setExample(schema.getExample() != null ? String.valueOf(schema.getExample()) : null);
        field.setEnumValues(extractEnumValues(schema));

        return Collections.singletonList(field);
    }

    private String formatType(Schema<?> schema) {
        String type = schema.getType() != null ? schema.getType() : "string";
        String format = schema.getFormat();
        if (format != null && !format.isEmpty()) {
            return type + "(" + format + ")";
        }
        return type;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractEnumValues(Schema<?> schema) {
        List<?> enumList = schema.getEnum();
        if (enumList == null || enumList.isEmpty()) {
            return null;
        }
        return enumList.stream().map(String::valueOf).collect(Collectors.toList());
    }
}
