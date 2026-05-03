package com.docgen.service.strategy;

import com.docgen.model.SchemaFieldDTO;
import io.swagger.v3.oas.models.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 数组类型 Schema 解析策略。
 *
 * <h3>职责</h3>
 * <p>处理 type=array 的 Schema。数组的核心在于 items 属性，
 * 它定义了数组元素的类型。解析逻辑：
 * <ol>
 *   <li>创建当前数组字段的 DTO（type 标注为 "array[元素类型]"）</li>
 *   <li>递归解析 items Schema，将结果作为子字段</li>
 * </ol>
 * </p>
 *
 * <h3>类型展示格式</h3>
 * <p>为了让前端和 Word 文档更直观地展示数组类型，
 * type 字段格式化为 "array[string]"、"array[object]" 等。</p>
 */
public class ArraySchemaStrategy implements SchemaParseStrategy {

    private static final Logger log = LoggerFactory.getLogger(ArraySchemaStrategy.class);

    private final SchemaStrategyFactory factory;

    public ArraySchemaStrategy(SchemaStrategyFactory factory) {
        this.factory = factory;
    }

    @Override
    public List<SchemaFieldDTO> parse(
            String fieldName, Schema<?> schema, Set<String> requiredSet,
            Map<String, Schema> allSchemas, int depth, Set<String> visitedRefs) {

        List<SchemaFieldDTO> result = new ArrayList<>();

        Schema<?> itemsSchema = schema.getItems();
        String itemType = resolveItemType(itemsSchema);

        // 构建数组字段本身
        SchemaFieldDTO arrayField = buildArrayField(fieldName, schema, requiredSet, depth, itemType);
        result.add(arrayField);

        // 如果 items 为复合类型（object/array），递归解析其子结构
        if (itemsSchema == null) {
            return result;
        }

        List<SchemaFieldDTO> itemFields = factory.delegateParse(
                null, itemsSchema, Collections.emptySet(),
                allSchemas, depth + 1, visitedRefs
        );
        result.addAll(itemFields);

        return result;
    }

    // ======================== 私有辅助方法 ========================

    /**
     * 解析数组元素的类型名称。
     * <p>如果 items 有 $ref，则提取引用名称；否则使用 items 的 type。</p>
     */
    private String resolveItemType(Schema<?> itemsSchema) {
        if (itemsSchema == null) {
            return "object";
        }
        if (itemsSchema.get$ref() != null) {
            return extractRefName(itemsSchema.get$ref());
        }
        return itemsSchema.getType() != null ? itemsSchema.getType() : "object";
    }

    /**
     * 构建数组字段的 DTO。
     */
    private SchemaFieldDTO buildArrayField(
            String fieldName, Schema<?> schema, Set<String> requiredSet,
            int depth, String itemType) {

        SchemaFieldDTO field = new SchemaFieldDTO();
        field.setName(fieldName);
        field.setType("array[" + itemType + "]");
        field.setDescription(schema.getDescription());
        field.setDepth(depth);
        field.setRequired(requiredSet != null && requiredSet.contains(fieldName));
        return field;
    }

    /**
     * 从 $ref 路径中提取 Schema 名称。
     * <p>例如 "#/components/schemas/User" → "User"</p>
     */
    private String extractRefName(String ref) {
        if (ref == null) {
            return "object";
        }
        int lastSlash = ref.lastIndexOf('/');
        return lastSlash >= 0 ? ref.substring(lastSlash + 1) : ref;
    }
}
