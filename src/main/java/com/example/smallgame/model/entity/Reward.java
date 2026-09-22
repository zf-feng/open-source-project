package com.example.smallgame.model.entity;

/**
 * 掉落物实体（生命包 / 能量），由怪物击杀或宝箱开启时产生，散落到地面供玩家拾取。
 * 绘制由视图层（EntityRenderer）完成。
 * <p>
 * 被 RewardController（掉落生成、散落与吸取动画）、CombatController（击杀掉落）、
 * GameMainView（掉落物渲染）引用。
 */
public class Reward {
    private RewardType type;
    /** 精确坐标（散落动画使用浮点） */
    private double px;
    private double py;
    /** 散落速度 */
    private double vx;
    private double vy;
    /** 散落剩余帧数 */
    private int scatterTicks;
    /** 拾取延迟帧数（掉落瞬间不可立即拾取） */
    private int pickupDelayTicks;

    /**
     * 构造掉落物，用于战斗中即时生成的掉落物。
     *
     * @param type 掉落物类型
     * @param x    初始 X 坐标
     * @param y    初始 Y 坐标
     */
    public Reward(RewardType type, double x, double y) {
        this.type = type;
        this.px = x;
        this.py = y;
    }

    /** 获取类型 */
    public RewardType getType() { return type; }

    /** 获取浮点 X 坐标 */
    public double getPx() { return px; }

    /** 获取浮点 Y 坐标 */
    public double getPy() { return py; }

    /** 设置浮点 X 坐标 */
    public void setPx(double px) { this.px = px; }

    /** 设置浮点 Y 坐标 */
    public void setPy(double py) { this.py = py; }

    /** 获取散落速度 X 分量 */
    public double getVx() { return vx; }

    /** 设置散落速度 X 分量 */
    public void setVx(double vx) { this.vx = vx; }

    /** 获取散落速度 Y 分量 */
    public double getVy() { return vy; }

    /** 设置散落速度 Y 分量 */
    public void setVy(double vy) { this.vy = vy; }

    /** 获取散落剩余帧数 */
    public int getScatterTicks() { return scatterTicks; }

    /** 设置散落剩余帧数 */
    public void setScatterTicks(int scatterTicks) { this.scatterTicks = scatterTicks; }

    /** 获取拾取延迟帧数 */
    public int getPickupDelayTicks() { return pickupDelayTicks; }

    /** 设置拾取延迟帧数 */
    public void setPickupDelayTicks(int pickupDelayTicks) { this.pickupDelayTicks = pickupDelayTicks; }

    /**
     * 设置散落动画：给定初始速度与持续帧数（用于宝箱喷出的掉落物）。
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
}
