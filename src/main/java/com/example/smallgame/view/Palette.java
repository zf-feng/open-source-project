package com.example.smallgame.view;

import javafx.scene.paint.Color;

/**
 * 主题调色板工具：把 MapTheme 中存储的颜色数据解码为 JavaFX Color。
 * 地图主题在模型层只保存 0xAARRGGBB 形式的颜色数据，
 * 渲染时由本工具统一转换为 JavaFX 颜色对象。
 * <p>
 * 引用文件：无（仅使用 JavaFX 图形 API）。
 * 被 GameMainView（背景与墙体配色）、MapSceneryRenderer（地板纹理派生色）、
 * EntityRenderer（墙体配色）调用。
 */
public final class Palette {

    /** 工具类，禁止实例化。 */
    private Palette() {
    }

    /**
     * 解码 RGBA 颜色数据为 JavaFX Color。
     * 颜色数据按 0xAARRGGBB 布局：最高 8 位为透明度，其后依次为红、绿、蓝，
     * 各通道除以 255 换算为 JavaFX 的 0~1 色值。
     *
     * @param rgba 0xAARRGGBB 颜色数据
     * @return 解码后的 JavaFX 颜色
     */
    public static Color color(int rgba) {
        double r = ((rgba >> 16) & 0xFF) / 255.0;
        double g = ((rgba >> 8) & 0xFF) / 255.0;
        double b = (rgba & 0xFF) / 255.0;
        double a = ((rgba >>> 24) & 0xFF) / 255.0;
        return new Color(r, g, b, a);
    }
}
