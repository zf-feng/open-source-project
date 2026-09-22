package com.example.smallgame.model.entity;

/**
 * 传送门实体：竖直椭圆造型（左右方向短半轴、上下方向长半轴），
 * 生成时由小到大生长显现（生长进度由本类维护，绘制由视图层完成）。
 * <p>
 * 被 LevelController（玩家踩点与传送判定）、EntityRenderer（传送门渲染）使用。
 */
public class Portal {
    /** 椭圆短半轴（左右方向）。 */
    public static final double RADIUS_X = 26;
    /** 椭圆长半轴（上下方向）。 */
    public static final double RADIUS_Y = 42;
    /** 生长动画帧数（约 0.6 秒内由小到大）。 */
    private static final int GROW_TICKS = 36;

    /** 中心坐标（浮点）。 */
    private double px;
    private double py;
    /** 已生长帧数（每次绘制前进一帧，满后保持全尺寸）。 */
    private int age;

    /**
     * 构造传送门。
     *
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     */
    public Portal(double x, double y) {
        this.px = x;
        this.py = y;
    }

    /** 获取浮点 X 坐标 */
    public double getPx() { return px; }

    /** 获取浮点 Y 坐标 */
    public double getPy() { return py; }

    /**
     * 生长动画：每渲染一帧推进一帧，满后保持全尺寸。
     * 由视图层在绘制传送门前调用。
     */
    public void advanceAge() {
        if (age < GROW_TICKS) {
            age++;
        }
    }

    /**
     * 生长进度：0~1 缓出曲线，由小到大显现。
     * 采用 1-(1-t)^2 缓出公式，使传送门前期生长快、后期趋缓。
     *
     * @return 当前生长进度（0~1）
     */
    public double getGrowProgress() {
        double t = Math.min(1.0, age / (double) GROW_TICKS);
        return 1 - (1 - t) * (1 - t);
    }
}
