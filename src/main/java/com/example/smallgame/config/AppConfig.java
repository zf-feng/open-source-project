package com.example.smallgame.config;

/**
 * 全局配置类，集中定义窗口标题与窗口尺寸等应用级常量。
 * <p>
 * 被 GameApplication（主窗口初始化）引用，
 * 窗口尺寸同时作为游戏内界面布局与坐标计算的基准。
 */
public final class AppConfig {
    /** 窗口标题 */
    public static final String TITLE = "星尘地牢";
    /** 窗口宽度（像素） */
    public static final double WIDTH = 1280;
    /** 窗口高度（像素） */
    public static final double HEIGHT = 760;

    /** 私有构造，禁止实例化纯常量类 */
    private AppConfig() {
    }
}
