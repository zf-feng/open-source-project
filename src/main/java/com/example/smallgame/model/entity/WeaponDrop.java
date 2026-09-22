package com.example.smallgame.model.entity;

/**
 * 地面上的武器掉落物：宝箱开出或玩家丢弃，靠近后按 E 拾取。
 * 负责自身的散落动画推进，绘制由视图层（EntityRenderer）完成。
 * <p>
 * 引用文件：WeaponType（武器类型）。
 * 被 RewardController（掉落生成）、CombatController（换枪丢弃）、
 * GameMainView（地面渲染）使用。
 */
public class WeaponDrop {
    /** 玩家靠近多远显示名称（拾取距离由控制器判定）。 */
    public static final double NAME_SHOW_DISTANCE = 70;

    /** 武器类型 */
    private final WeaponType type;
    /** 地面坐标（浮点，用于散落动画） */
    private double px;
    private double py;
    /** 散落速度 */
    private double vx;
    private double vy;
    /** 散落剩余帧数 */
    private int scatterTicks;

    /**
     * 构造武器掉落物。
     *
     * @param type 武器类型
     * @param x    初始 X 坐标
     * @param y    初始 Y 坐标
     */
    public WeaponDrop(WeaponType type, double x, double y) {
        this.type = type;
        this.px = x;
        this.py = y;
    }

    /** 获取武器类型 */
    public WeaponType getType() { return type; }

    /** 获取浮点 X 坐标 */
    public double getPx() { return px; }

    /** 获取浮点 Y 坐标 */
    public double getPy() { return py; }

    /** 设置浮点 X 坐标 */
    public void setPx(double px) { this.px = px; }

    /** 设置浮点 Y 坐标 */
    public void setPy(double py) { this.py = py; }

    /**
     * 设置散落动画：初始速度与持续帧数（宝箱喷出时使用）。
     *
     * @param vx    初始速度 X 分量
     * @param vy    初始速度 Y 分量
     * @param ticks 持续帧数
     */
    public void setScatter(double vx, double vy, int ticks) {
        this.vx = vx;
        this.vy = vy;
        this.scatterTicks = ticks;
    }

    /**
     * 推进散落动画（每逻辑帧调用一次）。
     * 速度按 0.82 系数逐帧衰减，模拟阻尼减速，帧数耗尽后静止。
     */
    public void update() {
        if (scatterTicks > 0) {
            px += vx;
            py += vy;
            vx *= 0.82;
            vy *= 0.82;
            scatterTicks--;
        }
    }
}
