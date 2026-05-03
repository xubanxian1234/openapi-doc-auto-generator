package com.docgen.word.style;

/**
 * Word 文档样式常量。
 *
 * 集中管理所有颜色、字体、边框等样式配置，
 * 避免魔法值散落在各工厂类中。修改样式只需改此一处。
 */
public final class WordStyleConstants {

    private WordStyleConstants() {}

    // ==================== 背景色（RGB Hex，不带 # 号） ====================
    /** 请求 URL 行背景色：浅黄色 */
    public static final String BG_URL = "FFF2CC";
    /** 请求头行背景色：浅绿色 */
    public static final String BG_HEADER = "E2EFDA";
    /** 请求参数行背景色：浅蓝色 */
    public static final String BG_REQUEST = "D9E1F2";
    /** 返回参数行背景色：浅灰色 */
    public static final String BG_RESPONSE = "D9D9D9";
    /** 表格标题行背景色：深灰色 */
    public static final String BG_TABLE_HEADER = "BDD7EE";
    /** 默认数据行背景色：纯白 */
    public static final String BG_DEFAULT = "FFFFFF";

    // ==================== 字体颜色 ====================
    /** 关键词高亮色：蓝色 */
    public static final String COLOR_HIGHLIGHT = "0000FF";
    /** 默认字体颜色：黑色 */
    public static final String COLOR_DEFAULT = "000000";
    /** 白色字体（用于深色背景标题） */
    public static final String COLOR_WHITE = "FFFFFF";

    // ==================== 字体 ====================
    public static final String FONT_FAMILY = "微软雅黑";
    public static final String FONT_FAMILY_EN = "Arial";
    public static final int FONT_SIZE_TITLE = 16;
    public static final int FONT_SIZE_SUBTITLE = 12;
    public static final int FONT_SIZE_BODY = 9;
    public static final int FONT_SIZE_TABLE = 9;

    // ==================== 边框 ====================
    /** 边框宽度（单位：1/8 磅，4 = 0.5 磅黑色实线） */
    public static final int BORDER_SIZE = 4;
    public static final String BORDER_COLOR = "000000";

    // ==================== 需要蓝色着色的关键词 ====================
    /** 在描述中需要标记为蓝色的关键词列表 */
    public static final String[] HIGHLIGHT_KEYWORDS = {
            "非必需", "默认值"
    };
}
