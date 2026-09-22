package com.example.smallgame.model.entity;

import java.util.Random;

/**
 * 地图主题风格：每一层（世界）开始时从该层候选地图中随机选定一个，
 * 整层沿用同一主题，决定游戏背景与墙体的配色。
 * 配色以 0xAARRGGBB 整数保存（AA 为透明度、RR/GG/BB 为红绿蓝分量），
 * 由视图层 Palette.color 解码为 JavaFX 颜色，使本枚举保持纯数据、不依赖 JavaFX。
 * 第一层：森林、草原、冰原、沙漠；第二层：城堡、黑森林、沼泽；
 * 第三层：太空、火山、海岛。DEFAULT 为菜单界面使用的默认配色。
 * <p>
 * 被 DungeonMap（关卡主题选定）、MonsterAppearance（怪物外观风格）、
 * MapSceneryRenderer（背景与墙体渲染）、Monster（外观主题归属）引用。
 */
public enum MapTheme {
    DEFAULT(0, "默认",
            0xFF090C19, 0x0CFFFFFF,
            0xFF4C5B87, 0xFF7F8EC1,
            0xFF202745, 0xFF56608D,
            0x78090C19, 0xFF6D5B8E,
            0xFFA489C0, 0x46D3B3D7),
    FOREST(1, "森林",
            0xFF0C1810, 0x0AC8FFC8,
            0xFF223C24, 0xFF4A6E44,
            0xFF1E3222, 0xFF405C38,
            0x78000000, 0xFF60482C,
            0xFF927044, 0x46C69E66),
    GRASSLAND(1, "草原",
            0xFF142412, 0x0ADCFFD2,
            0xFF345428, 0xFF608446,
            0xFF2C4E2A, 0xFF547C48,
            0x78000000, 0xFF5C6C3A,
            0xFF8A9C58, 0x46BECE82),
    TUNDRA(1, "冰原",
            0xFF121A26, 0x0CD2EBFF,
            0xFF2E4054, 0xFF58728C,
            0xFF344A62, 0xFF627E98,
            0x78000000, 0xFF80A4C4,
            0xFFACCAE2, 0x5ADEF2FC),
    DESERT(1, "沙漠",
            0xFF282014, 0x0AFFF0C8,
            0xFF544428, 0xFF826C42,
            0xFF5C4E32, 0xFF8C7850,
            0x78000000, 0xFF9C7E50,
            0xFFC4A46C, 0x46E8CC96),
    CASTLE(2, "城堡",
            0xFF16141C, 0x0AFFFFFF,
            0xFF2E2C38, 0xFF525060,
            0xFF34323E, 0xFF5A5868,
            0x78000000, 0xFF6E6A7A,
            0xFF9894A6, 0x46C6C2D2),
    DARK_FOREST(2, "黑森林",
            0xFF080C0A, 0x08FFFFFF,
            0xFF141E16, 0xFF2A3A28,
            0xFF18261C, 0xFF2E4030,
            0x8C000000, 0xFF2C3A2A,
            0xFF465840, 0x466C805E),
    SWAMP(2, "沼泽",
            0xFF0E120C, 0x08C8DCB4,
            0xFF1E2618, 0xFF36422A,
            0xFF263220, 0xFF3E4E32,
            0x8C000000, 0xFF3A462C,
            0xFF586842, 0x467E925E),
    SPACE(3, "太空",
            0xFF050712, 0x0C8CA0FF,
            0xFF10142C, 0xFF2E345C,
            0xFF1A1E38, 0xFF3A4066,
            0x8C000000, 0xFF505894,
            0xFF7C86C6, 0x50AEB8EE),
    VOLCANO(3, "火山",
            0xFF1C0C0A, 0x0AFFC8A0,
            0xFF381810, 0xFF5C2C1C,
            0xFF2E1A14, 0xFF4E2E22,
            0x8C000000, 0xFF5C3224,
            0xFF884C34, 0x46B86E4A),
    ISLAND(3, "海岛",
            0xFF0C1A20, 0x0AC8F0FF,
            0xFF1A322E, 0xFF36544E,
            0xFF345450, 0xFF5C8078,
            0x78000000, 0xFF5A6E5E,
            0xFF849880, 0x46B2C6AA);

    /** 主题归属的层（世界）编号，DEFAULT 为 0 不参与随机。 */
    private final int world;
    /** 主题显示名 */
    private final String displayName;
    /** 画布背景色（0xAARRGGBB）。 */
    private final int bgColor;
    /** 网格线颜色（0xAARRGGBB）。 */
    private final int gridColor;
    /** 顶栏底色（0xAARRGGBB）。 */
    private final int topBarColor;
    /** 顶栏描边色（0xAARRGGBB）。 */
    private final int topBarEdgeColor;
    /** 地板填充色（0xAARRGGBB）。 */
    private final int floorColor;
    /** 地板描边色（0xAARRGGBB）。 */
    private final int floorBorderColor;
    /** 墙体阴影色（0xAARRGGBB）。 */
    private final int wallShadow;
    /** 墙体基色（0xAARRGGBB）。 */
    private final int wallBase;
    /** 墙体棱边色（0xAARRGGBB）。 */
    private final int wallEdge;
    /** 墙体高光色（0xAARRGGBB）。 */
    private final int wallHighlight;

    /**
     * 枚举常量构造，保存该主题的完整配色方案（0xAARRGGBB 整数）。
     */
    MapTheme(int world, String displayName, int bgColor, int gridColor,
             int topBarColor, int topBarEdgeColor, int floorColor,
             int floorBorderColor, int wallShadow, int wallBase,
             int wallEdge, int wallHighlight) {
        this.world = world;
        this.displayName = displayName;
        this.bgColor = bgColor;
        this.gridColor = gridColor;
        this.topBarColor = topBarColor;
        this.topBarEdgeColor = topBarEdgeColor;
        this.floorColor = floorColor;
        this.floorBorderColor = floorBorderColor;
        this.wallShadow = wallShadow;
        this.wallBase = wallBase;
        this.wallEdge = wallEdge;
        this.wallHighlight = wallHighlight;
    }

    /** 获取主题显示名 */
    public String getDisplayName() { return displayName; }

    /** 获取画布背景色（0xAARRGGBB，绘制前经 Palette.color 解码）。 */
    public int bgColor() { return bgColor; }

    /** 获取网格线颜色（0xAARRGGBB）。 */
    public int gridColor() { return gridColor; }

    /** 获取顶栏底色（0xAARRGGBB）。 */
    public int topBarColor() { return topBarColor; }

    /** 获取顶栏描边色（0xAARRGGBB）。 */
    public int topBarEdgeColor() { return topBarEdgeColor; }

    /** 获取地板填充色（0xAARRGGBB）。 */
    public int floorColor() { return floorColor; }

    /** 获取地板描边色（0xAARRGGBB）。 */
    public int floorBorderColor() { return floorBorderColor; }

    /** 获取墙体阴影色（0xAARRGGBB）。 */
    public int wallShadow() { return wallShadow; }

    /** 获取墙体基色（0xAARRGGBB）。 */
    public int wallBase() { return wallBase; }

    /** 获取墙体棱边色（0xAARRGGBB）。 */
    public int wallEdge() { return wallEdge; }

    /** 获取墙体高光色（0xAARRGGBB）。 */
    public int wallHighlight() { return wallHighlight; }

    /**
     * 从指定层（世界）的候选地图中随机选一个主题。
     * 先过滤出归属该层的全部主题，再由传入的 Random 实例均匀随机选取，
     * 便于关卡生成时复用同一随机源。
     *
     * @param world  层（世界）编号
     * @param random 随机数生成器
     * @return 随机选中的主题
     */
    public static MapTheme randomForWorld(int world, Random random) {
        MapTheme[] candidates = java.util.Arrays.stream(values())
                .filter(theme -> theme.world == world)
                .toArray(MapTheme[]::new);
        return candidates[random.nextInt(candidates.length)];
    }
}
