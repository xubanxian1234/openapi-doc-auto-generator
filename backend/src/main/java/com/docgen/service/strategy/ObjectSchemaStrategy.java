package com.docgen.service.strategy;

import com.docgen.model.SchemaFieldDTO;
import io.swagger.v3.oas.models.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 对象类型 Schema 解析策略。
 *
 * <h3>职责</h3>
 * <p>处理 type=object 的 Schema，遍历其 properties 字典，
 * 将每个属性递归解析为 {@link SchemaFieldDTO}。</p>
 *
 * <h3>required 字段的提取逻辑</h3>
 * <p>OpenAPI 规范中，required 声明在父级 Schema 上（而非属性自身）：
 * <pre>
 * UserSchema:
 *   type: object
 *   required: [name, email]    ← required 在这里
 *   properties:
 *     name: { type: string }   ← name 本身没有 required 标记
 *     email: { type: string }
 *     age: { type: integer }   ← 不在 required 列表中，为非必需
 * </pre>
 * 因此解析时需从父 Schema 的 required 列表构建 Set，
 * 再在遍历 properties 时逐一判断。</p>
 */
public class ObjectSchemaStrategy implements SchemaParseStrategy {

    private static final Logger log = LoggerFactory.getLogger(ObjectSchemaStrategy.class);

    private final SchemaStrategyFactory factory;

    public ObjectSchemaStrategy(SchemaStrategyFactory factory) {
        this.factory = factory;
    }

    @Override
    public List<SchemaFieldDTO> parse(
            String fieldName, Schema<?> schema, Set<String> requiredSet,
            Map<String, Schema> allSchemas, int depth, Set<String> visitedRefs) {

        List<SchemaFieldDTO> result = new ArrayList<>();

        // 如果有字段名（非顶层调用），先创建当前对象字段本身
        SchemaFieldDTO objectField = buildObjectField(fieldName, schema, requiredSet, depth);
        if (fieldName != null) {
            result.add(objectField);
        }

        // 提取当前 Schema 的 required 列表，供子属性判断是否必需
        Set<String> childRequired = extractRequiredSet(schema);
        Map<String, Schema> properties = schema.getProperties();

        if (properties == null || properties.isEmpty()) {
            return result;
        }

        // 遍历每个属性，递归调用工厂的 delegateParse
        int childDepth = (fieldName != null) ? depth + 1 : depth;
        for (Map.Entry<String, Schema> entry : properties.entrySet()) {
            List<SchemaFieldDTO> childFields = factory.delegateParse(
                    entry.getKey(), entry.getValue(), childRequired,
                    allSchemas, childDepth, visitedRefs
            );
            result.addAll(childFields);
        }

        return result;
    }

    // ======================== 私有辅助方法 ========================

    /**
     * 构建对象类型字段自身的 DTO（不含子属性）。
     */
    private SchemaFieldDTO buildObjectField(
            String fieldName, Schema<?> schema, Set<String> requiredSet, int depth) {

        SchemaFieldDTO field = new SchemaFieldDTO();
        field.setName(fieldName);
        field.setType("object");
        field.setDescription(schema.getDescription());
        field.setDepth(depth);
        field.setRequired(requiredSet != null && requiredSet.contains(fieldName));
        return field;
    }

    /**
     * 从 Schema 的 required 列表构建 Set，用于快速判断子属性是否必需。
     * <p>如果 required 为 null，返回空 Set 而非 null（防御性编程）。</p>
     */
    private Set<String> extractRequiredSet(Schema<?> schema) {
        List<String> requiredList = schema.getRequired();
        if (requiredList == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(requiredList);
    }
}
