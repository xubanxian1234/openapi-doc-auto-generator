package com.docgen.service.strategy;

import com.docgen.model.SchemaFieldDTO;
import io.swagger.v3.oas.models.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * $ref 引用类型 Schema 解析策略。
 *
 * 处理含 $ref 的 Schema。解引用后委托给工厂重新分发，
 * 通过 visitedRefs 集合检测并阻断循环引用。
 */
public class RefSchemaStrategy implements SchemaParseStrategy {

    private static final Logger log = LoggerFactory.getLogger(RefSchemaStrategy.class);

    private final SchemaStrategyFactory factory;

    public RefSchemaStrategy(SchemaStrategyFactory factory) {
        this.factory = factory;
    }

    @Override
    public List<SchemaFieldDTO> parse(
            String fieldName, Schema<?> schema, Set<String> requiredSet,
            Map<String, Schema> allSchemas, int depth, Set<String> visitedRefs) {

        String ref = schema.get$ref();
        String refName = extractRefName(ref);

        // 循环引用检测：如果该 ref 已在访问链上，立即终止递归
        if (visitedRefs.contains(refName)) {
            log.debug("检测到循环引用: {}，停止递归", refName);
            return createCircularRefMarker(fieldName, refName, depth);
        }

        Schema<?> resolvedSchema = allSchemas.get(refName);
        if (resolvedSchema == null) {
            log.warn("无法解析引用: {}，Schema 未在 components/schemas 中找到", ref);
            return createUnresolvedRefMarker(fieldName, refName, depth);
        }

        // 将当前 ref 加入访问链，解析完成后移除（回溯）
        Set<String> newVisited = new HashSet<>(visitedRefs);
        newVisited.add(refName);

        return factory.delegateParse(
                fieldName, resolvedSchema, requiredSet,
                allSchemas, depth, newVisited
        );
    }

    private String extractRefName(String ref) {
        if (ref == null) return "unknown";
        int lastSlash = ref.lastIndexOf('/');
        return lastSlash >= 0 ? ref.substring(lastSlash + 1) : ref;
    }

    private List<SchemaFieldDTO> createCircularRefMarker(String fieldName, String refName, int depth) {
        SchemaFieldDTO marker = new SchemaFieldDTO();
        marker.setName(fieldName != null ? fieldName : refName);
        marker.setType("object");
        marker.setSchemaRef(refName);
        marker.setDescription("（循环引用: " + refName + "）");
        marker.setDepth(depth);
        return Collections.singletonList(marker);
    }

    private List<SchemaFieldDTO> createUnresolvedRefMarker(String fieldName, String refName, int depth) {
        SchemaFieldDTO marker = new SchemaFieldDTO();
        marker.setName(fieldName != null ? fieldName : refName);
        marker.setType("object");
        marker.setSchemaRef(refName);
        marker.setDescription("（未解析的引用: " + refName + "）");
        marker.setDepth(depth);
        return Collections.singletonList(marker);
    }
}
