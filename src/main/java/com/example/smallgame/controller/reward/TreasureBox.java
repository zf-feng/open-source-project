package com.example.smallgame.controller.reward;

/**
 * 宝箱实体：玩家靠近自动开启（开启后立即消失，掉落物由 RewardController 散落到地面）。
 * 只保存开启状态与坐标数据，待机立绘动画由 EntityRenderer.drawTreasureBox 绘制。
 * <p>
 * 被 RewardController（宝箱开启与掉落）与 EntityRenderer（宝箱渲染）使用。
 */
public final class TreasureBox {
    /** 玩家靠近多少距离自动开启。 */
    public static final double OPEN_DISTANCE = 45;
    /** 生成保护帧数：生成后短暂延迟响应靠近开启，避免宝箱在玩家脚下瞬间消失。 */
    private static final int SPAWN_PROTECTION_TICKS = 40;

    /** 白色/金色宝箱立绘资源目录（由 EntityRenderer 按品质加载）。 */
    public static final String WHITE_FOLDER = "/sprites/chest_white";
    public static final String GOLD_FOLDER = "/sprites/chest_gold";

    private final double px;
    private final double py;
    private final boolean golden;
    private boolean opened;
    private int spawnProtectionTicks = SPAWN_PROTECTION_TICKS;

    /**
     * 构造宝箱，记录坐标与品质。
     *
     * @param x      坐标 X
     * @param y      坐标 Y
     * @param golden 是否为金宝箱
     */
    public TreasureBox(double x, double y, boolean golden) {
        this.px = x;
        this.py = y;
        this.golden = golden;
    }

    /** 查询是否为金宝箱 */
    public boolean isGolden() {
        return golden;
    }

    /** 获取 X 坐标 */
    public double getPx() {
        return px;
    }

    /** 获取 Y 坐标 */
    public double getPy() {
        return py;
    }

    /** 查询是否已开启 */
    public boolean isOpened() {
        return opened;
    }

    /** 每逻辑帧更新：生成保护结束后，玩家靠近即开启（宝箱消失）。返回 true 表示本帧刚开启。 */
    public boolean update(double playerX, double playerY) {
        if (spawnProtectionTicks > 0) {
            spawnProtectionTicks--;
            return false;
        }
        if (opened || distance(playerX, playerY, px, py) > OPEN_DISTANCE) {
            return false;
        }
        opened = true;
        return true;
    }

    /** 计算两点间欧几里得距离 */
    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }
}
