package com.docgen.service.strategy;

import com.docgen.model.SchemaFieldDTO;
import io.swagger.v3.oas.models.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 策略工厂 — 根据 Schema 的类型特征选择合适的解析策略。
 *
 * <h3>工厂模式 (Factory Pattern) 的应用</h3>
 * <p>此工厂封装了"根据 Schema 类型选择策略"的决策逻辑，
 * 调用方无需了解具体策略类的存在。新增 Schema 类型时，
 * 只需添加新的策略实现类并在此工厂注册即可。</p>
 *
 * <h3>策略选择优先级</h3>
 * <ol>
 *   <li>如果 Schema 含有 $ref → RefSchemaStrategy</li>
 *   <li>如果 type=array → ArraySchemaStrategy</li>
 *   <li>如果 type=object 或含有 properties → ObjectSchemaStrategy</li>
 *   <li>其他情况 → PrimitiveSchemaStrategy</li>
 * </ol>
 *
 * <h3>递归深度控制</h3>
 * <p>提供 {@link #MAX_DEPTH} 常量（默认 10 层）和 {@link #isMaxDepthExceeded} 检查，
 * 防止循环引用或过深嵌套导致栈溢出。</p>
 */
public class SchemaStrategyFactory {

    private static final Logger log = LoggerFactory.getLogger(SchemaStrategyFactory.class);

    /** 递归解析最大深度，防止循环引用或过深嵌套导致 StackOverflow */
    public static final int MAX_DEPTH = 10;

    private final ObjectSchemaStrategy objectStrategy = new ObjectSchemaStrategy(this);
    private final ArraySchemaStrategy arrayStrategy = new ArraySchemaStrategy(this);
    private final PrimitiveSchemaStrategy primitiveStrategy = new PrimitiveSchemaStrategy();
    private final RefSchemaStrategy refStrategy = new RefSchemaStrategy(this);

    /**
     * 根据 Schema 特征选择合适的解析策略。
     *
     * <p>选择逻辑基于 Schema 的结构特征而非仅依赖 type 字段，
     * 因为 OpenAPI 规范中 $ref 和 type 可能同时存在，
     * 且某些 Schema 可能省略 type 但包含 properties。</p>
     *
     * @param schema 待判断的 Schema 对象
     * @return 对应的解析策略实例
     */
    public SchemaParseStrategy getStrategy(Schema<?> schema) {
        if (hasRef(schema)) {
            return refStrategy;
        }
        if (isArrayType(schema)) {
            return arrayStrategy;
        }
        if (isObjectType(schema)) {
            return objectStrategy;
        }
        return primitiveStrategy;
    }

    /**
     * 检查递归深度是否已超过安全限制。
     *
     * @param depth 当前深度
     * @return true 如果超过 {@value MAX_DEPTH}
     */
    public boolean isMaxDepthExceeded(int depth) {
        return depth > MAX_DEPTH;
    }

    /**
     * 便捷方法：直接调用合适的策略解析 Schema。
     * <p>在达到最大深度时返回一个标记字段，而非继续递归。</p>
     *
     * @return 解析后的字段列表
     */
    public List<SchemaFieldDTO> delegateParse(
            String fieldName, Schema<?> schema, Set<String> requiredSet,
            Map<String, Schema> allSchemas, int depth, Set<String> visitedRefs) {

        if (schema == null) {
            return Collections.emptyList();
        }

        if (isMaxDepthExceeded(depth)) {
            log.warn("Schema 解析达到最大深度 {}，字段 '{}' 停止递归", MAX_DEPTH, fieldName);
            return createDepthLimitMarker(fieldName, depth);
        }

        SchemaParseStrategy strategy = getStrategy(schema);
        return strategy.parse(fieldName, schema, requiredSet, allSchemas, depth, visitedRefs);
    }

    // ======================== 私有辅助方法 ========================

    /**
     * 判断 Schema 是否包含 $ref 引用。
     * <p>swagger-parser 可能将 $ref 存储在 get$ref() 方法中。</p>
     */
    private boolean hasRef(Schema<?> schema) {
        return schema.get$ref() != null && !schema.get$ref().isEmpty();
    }

    /** 判断 Schema 是否为数组类型 */
    private boolean isArrayType(Schema<?> schema) {
        return "array".equals(schema.getType());
    }

    /**
     * 判断 Schema 是否为对象类型。
     * <p>除了显式 type=object 外，如果 Schema 含有 properties 但未声明 type，
     * 也应视为对象类型（OpenAPI 规范允许省略 type）。</p>
     */
    private boolean isObjectType(Schema<?> schema) {
        if ("object".equals(schema.getType())) {
            return true;
        }
        return schema.getProperties() != null && !schema.getProperties().isEmpty();
    }

    /**
     * 创建深度限制标记字段。
     * <p>当递归深度达到上限时，不再继续解析，而是生成一个带有提示信息的字段。</p>
     */
    private List<SchemaFieldDTO> createDepthLimitMarker(String fieldName, int depth) {
        SchemaFieldDTO marker = new SchemaFieldDTO();
        marker.setName(fieldName != null ? fieldName : "...");
        marker.setType("object");
        marker.setDescription("（嵌套层级过深，已省略更深层字段）");
        marker.setDepth(depth);
        return Collections.singletonList(marker);
    }
}
