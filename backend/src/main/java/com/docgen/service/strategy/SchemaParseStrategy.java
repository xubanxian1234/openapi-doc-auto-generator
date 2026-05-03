package com.docgen.service.strategy;

import com.docgen.model.SchemaFieldDTO;
import io.swagger.v3.oas.models.media.Schema;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Schema 解析策略接口 — 策略模式 (Strategy Pattern) 的核心抽象。
 *
 * <h3>设计动机</h3>
 * <p>OpenAPI Schema 有多种类型（object、array、基本类型、$ref 引用等），
 * 每种类型的解析逻辑截然不同：
 * <ul>
 *   <li>object → 递归遍历 properties</li>
 *   <li>array → 解析 items，标注为数组类型</li>
 *   <li>primitive → 直接提取 type/format/enum 等元信息</li>
 *   <li>$ref → 解引用后委托给对应策略重新解析</li>
 * </ul>
 * 如果使用 if-else 或 switch 集中处理，代码会迅速膨胀且难以维护。
 * 策略模式将每种类型的解析逻辑封装为独立类，符合开闭原则（OCP）。</p>
 *
 * @see SchemaStrategyFactory 根据 Schema 类型选择具体策略
 */
public interface SchemaParseStrategy {

    /**
     * 解析给定的 Schema，将其展平为 SchemaFieldDTO 列表。
     *
     * @param fieldName     当前字段名称（顶层调用时可为 null）
     * @param schema        待解析的 OpenAPI Schema 对象
     * @param requiredSet   父级 Schema 声明的 required 字段名集合
     * @param allSchemas    整个文档的 components/schemas 映射（用于解引用 $ref）
     * @param depth         当前递归深度（从 0 开始，用于缩进控制）
     * @param visitedRefs   已访问的 $ref 集合（防止循环引用导致栈溢出）
     * @return 解析得到的字段 DTO 列表
     */
    List<SchemaFieldDTO> parse(
            String fieldName,
            Schema<?> schema,
            Set<String> requiredSet,
            Map<String, Schema> allSchemas,
            int depth,
            Set<String> visitedRefs
    );
}
