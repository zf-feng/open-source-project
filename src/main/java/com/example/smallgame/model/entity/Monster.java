package com.example.smallgame.model.entity;

/**
 * 怪物实体，继承 Character，承载怪物的类型、体积、移速、AI 状态（路径点、攻击冷却、
 * 召唤冷却、冰冻、瞄准）与动画相位。绘制由视图层（MonsterAppearance）完成。
 * <p>
 * 引用文件：MapTheme（外观主题）。
 * 被 LevelController（生成与波次控制）、CombatController / BossController（战斗 AI）、
 * SkillController（冰冻等技能效果）、MonsterAppearance（绘制）使用。
 */
public class Monster extends Character {
    /** 生成入场红光闪光持续帧数（比人物蓝色闪光更短更小，颜色醒目区分）。 */
    public static final int SPAWN_FLASH_TICKS = 8;
    /** 攻击挥击/射击闪光持续帧数（近战挥击弧与远程发射时的枪口光）。 */
    public static final int ATTACK_FLASH_TICKS = 10;
    /** 怪物类型：普通 / 精英 / Boss */
    private MonsterType type;
    /** Boss 半血能量点是否已掉落（半血时一次性触发，避免每帧重复掉落）。 */
    private boolean halfHpLootDropped;
    /** 精确坐标（移动为浮点步进） */
    private double px;
    private double py;
    /** 碰撞半径 */
    private final double radius;
    /** 移动速度（像素/帧） */
    private final double speed;
    /** 动画相位偏移（让同类怪浮动节奏错开） */
    private final double animationOffset;
    /** 当前寻路目标点 */
    private double waypointX;
    private double waypointY;
    /** 换路点冷却帧数（耗尽后重新随机目标点） */
    private int pathCooldown;
    /** 攻击冷却帧数 */
    private int attackCooldown;
    /** Boss 召唤小怪冷却帧数（非 Boss 为 -1 禁用） */
    private int summonCooldown;
    /** 冰冻剩余帧数（>0 时无法移动与攻击）。 */
    private int frozenTicks;
    /** 连续撞墙帧数（超过阈值视为被墙体卡住，触发绕行脱困）。 */
    private int stuckTicks;
    /** 生成入场红光闪光剩余帧数（>0 时绘制红色闪烁特效）。 */
    private int spawnFlashTicks;
    /** 攻击风格：0=近战（追击靠近后挥击），1=远程（随机游走+直线射击）。 */
    private final int attackStyle;
    /** 远程怪弹幕模式：0 单发 / 1 一排 / 2 环形扩散 / 3 旋转扫射 / 4 散弹 / 5 十字 / 6 三连齐射。
     *  Boss 非 final：每次发射后轮换模式（Boss 会全部七种弹幕）。 */
    private int attackPattern;
    /** 旋转扫射模式当前发射角度（弧度），每次发射后旋转递增。 */
    private double bulletAngle;
    /** 攻击闪光剩余帧数（>0 时绘制挥击弧/发射光）。 */
    private int attackFlashTicks;
    /** 攻击朝向（弧度），闪光特效沿此方向绘制。 */
    private double attackAngle;
    /** 远程怪发射前瞄准静止剩余帧数（0.5 秒预警，>0 时站定不动）。 */
    private int aimTicks;
    /** 外观主题：与出生关卡地图主题一致，决定身体造型风格。 */
    private final MapTheme appearanceTheme;

    /**
     * 波次构造：按波次与类型决定生命值、体积、攻击风格、弹幕模式与移速。
     * Boss 为远程攻击且弹幕模式每发轮换；精英五五开、普通怪四成概率为远程；
     * 远程怪移速快于近战怪；草原主题的 Boss 为巨人形态（碰撞半径与命中半径更大）。
     *
     * @param x     初始 X 坐标
     * @param y     初始 Y 坐标
     * @param wave  所属波次（影响血量成长）
     * @param kind  怪物类型
     * @param theme 所属地图主题（决定外观与 Boss 体积）
     */
    public Monster(double x, double y, int wave, MonsterType kind, MapTheme theme) {
        super(maxHealthFor(kind, wave));
        this.type = kind;
        this.px = x;
        this.py = y;
        if (kind == MonsterType.BOSS) {
            // 巨人 Boss（草原）碰撞更大，便于远程武器命中庞大身躯；其余 Boss 标准体积
            this.radius = theme == MapTheme.GRASSLAND ? 55 : 27;
            setAttack(2);
        } else if (kind == MonsterType.ELITE) {
            this.radius = 19;
            setAttack(1);
        } else {
            this.radius = 14;
            setAttack(1);
        }
        // 攻击风格：Boss 只远程攻击（弹幕模式轮换，会全部七种）；精英五五开、普通怪四成远程
        if (kind == MonsterType.BOSS) {
            this.attackStyle = 1;
            this.attackPattern = (int) (Math.random() * 7);
        } else if (kind == MonsterType.ELITE) {
            this.attackStyle = Math.random() < 0.5 ? 1 : 0;
            this.attackPattern = (int) (Math.random() * 7);
        } else {
            this.attackStyle = Math.random() < 0.4 ? 1 : 0;
            this.attackPattern = (int) (Math.random() * 7);
        }
        // 移速：远程怪更快（便于拉开距离），近战怪更慢（追击但追不上）；Boss 移速全游戏最大
        if (kind == MonsterType.BOSS) {
            this.speed = theme == MapTheme.DESERT ? 3.3 : 2.7;
        } else if (attackStyle == 1) {
            this.speed = 2.35 + Math.random() * 0.4;
        } else {
            this.speed = 1.55 + Math.random() * 0.35;
        }
        this.animationOffset = Math.random() * Math.PI * 2;
        this.waypointX = x;
        this.waypointY = y;
        this.pathCooldown = 0;
        this.attackCooldown = 0;
        this.summonCooldown = kind == MonsterType.BOSS ? 300 : -1;
        this.stuckTicks = 0;
        this.spawnFlashTicks = 0;
        this.bulletAngle = Math.random() * Math.PI * 2;
        this.attackFlashTicks = 0;
        this.attackAngle = 0;
        this.aimTicks = 0;
        this.appearanceTheme = theme;
    }

    /**
     * 按类型与波次计算最大生命值。
     * Boss 血量大幅高于普通怪，并随波次线性成长，普通怪与精英按各自系数成长。
     *
     * @param kind 怪物类型
     * @param wave 所属波次
     * @return 最大生命值
     */
    private static int maxHealthFor(MonsterType kind, int wave) {
        if (kind == MonsterType.BOSS) {
            // Boss 血量大幅提升：单发普通枪很难磨死，需要巨弓/咖喱棒等重型武器
            return 1400 + wave * 60;
        }
        if (kind == MonsterType.ELITE) {
            return 105 + wave * 12;
        }
        return 24 + wave * 10;
    }

    /** 获取怪物类型 */
    public MonsterType getType() { return type; }

    /** Boss 半血能量点是否已掉落。 */
    public boolean isHalfHpLootDropped() { return halfHpLootDropped; }

    /** 标记 Boss 半血能量点已掉落。 */
    public void setHalfHpLootDropped(boolean dropped) { this.halfHpLootDropped = dropped; }

    /** 获取浮点 X 坐标 */
    public double getPx() { return px; }

    /** 获取浮点 Y 坐标 */
    public double getPy() { return py; }

    /** 设置浮点 X 坐标 */
    public void setPx(double px) { this.px = px; }

    /** 设置浮点 Y 坐标 */
    public void setPy(double py) { this.py = py; }

    /** 获取碰撞半径 */
    public double getRadius() { return radius; }

    /**
     * 远程武器命中判定半径：巨人 Boss 身躯庞大，判定半径远大于碰撞半径。
     *
     * @return 命中判定半径
     */
    public double getHitRadius() {
        return type == MonsterType.BOSS && appearanceTheme == MapTheme.GRASSLAND ? 175 : radius;
    }

    /** 获取移动速度 */
    public double getSpeed() { return speed; }

    /** 获取寻路目标点 X */
    public double getWaypointX() { return waypointX; }

    /** 设置寻路目标点 X */
    public void setWaypointX(double waypointX) { this.waypointX = waypointX; }

    /** 获取寻路目标点 Y */
    public double getWaypointY() { return waypointY; }

    /** 设置寻路目标点 Y */
    public void setWaypointY(double waypointY) { this.waypointY = waypointY; }

    /** 获取换路点冷却帧数 */
    public int getPathCooldown() { return pathCooldown; }

    /** 设置换路点冷却帧数 */
    public void setPathCooldown(int pathCooldown) { this.pathCooldown = pathCooldown; }

    /** 获取攻击冷却帧数 */
    public int getAttackCooldown() { return attackCooldown; }

    /** 设置攻击冷却帧数 */
    public void setAttackCooldown(int attackCooldown) { this.attackCooldown = attackCooldown; }

    /** 获取召唤冷却帧数 */
    public int getSummonCooldown() { return summonCooldown; }

    /** 设置召唤冷却帧数 */
    public void setSummonCooldown(int summonCooldown) { this.summonCooldown = summonCooldown; }

    /** 获取冰冻剩余帧数 */
    public int getFrozenTicks() { return frozenTicks; }

    /** 设置冰冻剩余帧数 */
    public void setFrozenTicks(int frozenTicks) { this.frozenTicks = frozenTicks; }

    /** 获取撞墙累计帧数 */
    public int getStuckTicks() { return stuckTicks; }

    /** 设置撞墙累计帧数 */
    public void setStuckTicks(int stuckTicks) { this.stuckTicks = stuckTicks; }

    /** 获取入场闪光剩余帧数 */
    public int getSpawnFlashTicks() { return spawnFlashTicks; }

    /** 设置入场闪光剩余帧数 */
    public void setSpawnFlashTicks(int spawnFlashTicks) { this.spawnFlashTicks = spawnFlashTicks; }

    /** 获取攻击风格（0 近战 / 1 远程） */
    public int getAttackStyle() { return attackStyle; }

    /** 获取弹幕模式 */
    public int getAttackPattern() { return attackPattern; }

    /** 设置弹幕模式 */
    public void setAttackPattern(int attackPattern) { this.attackPattern = attackPattern; }

    /** 获取当前发射角度（旋转扫射用） */
    public double getBulletAngle() { return bulletAngle; }

    /** 设置当前发射角度 */
    public void setBulletAngle(double bulletAngle) { this.bulletAngle = bulletAngle; }

    /** 获取瞄准静止剩余帧数 */
    public int getAimTicks() { return aimTicks; }

    /** 设置瞄准静止剩余帧数 */
    public void setAimTicks(int aimTicks) { this.aimTicks = aimTicks; }

    /** 获取攻击闪光剩余帧数 */
    public int getAttackFlashTicks() { return attackFlashTicks; }

    /** 设置攻击闪光剩余帧数 */
    public void setAttackFlashTicks(int attackFlashTicks) { this.attackFlashTicks = attackFlashTicks; }

    /** 获取攻击朝向（弧度） */
    public double getAttackAngle() { return attackAngle; }

    /** 设置攻击朝向（弧度） */
    public void setAttackAngle(double attackAngle) { this.attackAngle = attackAngle; }

    /** 获取外观主题 */
    public MapTheme getAppearanceTheme() { return appearanceTheme; }

    /** 获取动画相位偏移 */
    public double getAnimationOffset() { return animationOffset; }
}
