package com.example.smallgame.model.entity;

/**
 * 玩家实体，继承 Character，承载英雄职业属性（护盾、能量）、双武器槽、
 * 各武器攻击状态（射击/近战冷却、挥砍计时、弓与咖喱棒蓄力）、
 * 技能增益状态（火力全开、瞬身闪避、钩锁拉拽）与行走动画相位。
 * 绘制（角色/武器立绘、蓄力条、手刀挥击）由视图层（PlayerRenderer）完成。
 * <p>
 * 引用文件：HeroType（职业初始属性）、WeaponType（武器类型）、
 * DungeonMap（墙体碰撞判定）。
 * 被 CombatController（武器攻击）、SkillController（技能增益）、
 * BossController（钩锁控制）、LevelController（关卡移动）等控制器操作。
 */
public class Player extends Character {
    /** 玩家碰撞半径 */
    public static final double RADIUS = 16;
    /** 武器绘制锚点相对玩家中心的垂直偏移（枪口/弓弦/刀柄高度），飞行物生成位置共用。 */
    public static final double WEAPON_DRAW_OFFSET_Y = 14;
    /** 常规弓蓄力满所需帧数（60Hz 下 60 帧 = 1 秒）。 */
    public static final int BOW_CHARGE_MAX = 60;
    /** 巨弓蓄力满所需帧数（60Hz 下 180 帧 = 3 秒）。 */
    public static final int GIANT_BOW_CHARGE_MAX = 180;
    /** 咖喱棒蓄力满所需帧数（60Hz 下 90 帧 = 1.5 秒，长于常规弓）。 */
    public static final int EXCALIBUR_CHARGE_MAX = 90;
    /** 基础移动速度（像素/帧） */
    private static final double MOVE_SPEED = 4.5;
    /** 巨弓蓄力时的移速倍率（大幅降低移速）。 */
    private static final double GIANT_BOW_CHARGE_SPEED = 0.25;
    /** 武器槽数量上限 */
    private static final int MAX_WEAPON_SLOTS = 2;
    /** 手刀挥击动画帧数（挥砍-正.gif 共 3 帧，每帧约 80ms 对应 5 逻辑帧）。 */
    public static final int HAND_KNIFE_SWING_TICKS = 15;

    /** 当前护盾值 */
    private int shield;
    /** 当前能量值 */
    private int energy;
    /** 最大能量值 */
    private int maxEnergy;

    /** 精确坐标（移动为浮点步进） */
    private double px;
    private double py;
    /** 最大护盾值 */
    private int maxShield;
    /** 枪类射击冷却帧数 */
    private int fireCooldown;
    /** 近战攻击冷却帧数 */
    private int meleeCooldown;
    /** 剑类挥砍动画剩余帧数 */
    private int slashTimer;
    /** 手刀挥击动画剩余帧数（近身自动切换为手刀时触发）。 */
    private int handKnifeTimer;
    /** 是否处于移动状态（决定行走动画帧） */
    private boolean moving;
    /** 行走动画循环计数 */
    private double walkCycle;
    /** 护盾回复累计帧数（每 120 帧回复 1 点，无论是否脱战） */
    private int shieldRegenCounter;
    /** 受击无敌帧数 */
    private int damageInvulnerability;
    /** 火力全开增益剩余帧数（>0 时攻速×2、移速×1.5、攻击不耗能量、蓄力速度×2），由 SkillController 维护。 */
    private int firepowerTicks;
    /** 瞬身闪避剩余帧数（>0 时免疫一切伤害），由 SkillController 维护。 */
    private int dodgeTicks;
    /** 钩锁拉拽锁定（>0 时无法主动移动但仍可攻击），由 BossController（钢铁海盗）维护。 */
    private int hookedTicks;
    /** 当前英雄职业 */
    private HeroType heroType;
    /** 弓类/咖喱棒当前蓄力帧数 */
    private int bowChargeTicks;
    /** 弓类/咖喱棒是否正在蓄力 */
    private boolean bowCharging;
    /** 双刀交替挥舞索引 */
    private int nextBladeIndex;
    /** 武器槽（最多两把），slotIndex 为当前使用槽。 */
    private final WeaponType[] slots = new WeaponType[MAX_WEAPON_SLOTS];
    private int slotIndex;

    /**
     * 职业构造：按所选英雄的初始属性（生命/护盾/能量）构建玩家并装配初始武器。
     *
     * @param x        初始 X 坐标
     * @param y        初始 Y 坐标
     * @param heroType 英雄职业
     */
    public Player(double x, double y, HeroType heroType) {
        super(heroType.getMaxHp());
        this.px = x;
        this.py = y;
        this.heroType = heroType;
        this.maxShield = heroType.getMaxShield();
        this.shield = heroType.getMaxShield();
        this.maxEnergy = heroType.getMaxEnergy();
        this.energy = heroType.getMaxEnergy();
        equipInitialWeapon();
    }

    /** 初始武器放入槽 0，槽 1 留空。 */
    private void equipInitialWeapon() {
        slots[0] = heroType.getInitialWeapon();
        slots[1] = null;
        slotIndex = 0;
    }

    /** 获取英雄职业 */
    public HeroType getHeroType() { return heroType; }

    /** 获取当前护盾值 */
    public int getShield() { return shield; }

    /** 设置当前护盾值 */
    public void setShield(int shield) { this.shield = shield; }

    /** 获取最大护盾值 */
    public int getMaxShield() { return maxShield; }

    /** 获取当前能量值 */
    public int getEnergy() { return energy; }

    /** 设置当前能量值 */
    public void setEnergy(int energy) { this.energy = energy; }

    /** 获取最大能量值 */
    public int getMaxEnergy() { return maxEnergy; }

    /** 获取浮点 X 坐标 */
    public double getPx() { return px; }

    /** 获取浮点 Y 坐标 */
    public double getPy() { return py; }

    /** 设置浮点 X 坐标 */
    public void setPx(double px) { this.px = px; }

    /** 设置浮点 Y 坐标 */
    public void setPy(double py) { this.py = py; }

    /** 获取枪类射击冷却帧数 */
    public int getFireCooldown() { return fireCooldown; }

    /** 设置枪类射击冷却帧数 */
    public void setFireCooldown(int fireCooldown) { this.fireCooldown = fireCooldown; }

    /** 获取近战攻击冷却帧数 */
    public int getMeleeCooldown() { return meleeCooldown; }

    /** 设置近战攻击冷却帧数 */
    public void setMeleeCooldown(int meleeCooldown) { this.meleeCooldown = meleeCooldown; }

    /** 获取剑类挥砍动画剩余帧数 */
    public int getSlashTimer() { return slashTimer; }

    /** 设置剑类挥砍动画剩余帧数 */
    public void setSlashTimer(int slashTimer) { this.slashTimer = slashTimer; }

    /** 获取手刀挥击动画剩余帧数 */
    public int getHandKnifeTimer() { return handKnifeTimer; }

    /** 设置手刀挥击动画剩余帧数 */
    public void setHandKnifeTimer(int handKnifeTimer) { this.handKnifeTimer = handKnifeTimer; }

    /** 获取当前蓄力帧数 */
    public int getBowChargeTicks() { return bowChargeTicks; }

    /** 设置当前蓄力帧数 */
    public void setBowChargeTicks(int bowChargeTicks) { this.bowChargeTicks = bowChargeTicks; }

    /** 获取火力全开剩余帧数 */
    public int getFirepowerTicks() { return firepowerTicks; }

    /** 设置火力全开剩余帧数 */
    public void setFirepowerTicks(int firepowerTicks) { this.firepowerTicks = firepowerTicks; }

    /** 获取瞬身闪避剩余帧数 */
    public int getDodgeTicks() { return dodgeTicks; }

    /** 设置瞬身闪避剩余帧数 */
    public void setDodgeTicks(int dodgeTicks) { this.dodgeTicks = dodgeTicks; }

    /** 获取钩锁拉拽剩余帧数 */
    public int getHookedTicks() { return hookedTicks; }

    /** 设置钩锁拉拽剩余帧数 */
    public void setHookedTicks(int hookedTicks) { this.hookedTicks = hookedTicks; }

    /** 查询当前是否处于移动状态 */
    public boolean isMoving() { return moving; }

    /** 获取行走动画循环计数 */
    public double getWalkCycle() { return walkCycle; }

    /** 查询是否正在蓄力 */
    public boolean isBowCharging() { return bowCharging; }

    /** 设置蓄力状态 */
    public void setBowCharging(boolean bowCharging) { this.bowCharging = bowCharging; }

    /** 获取双刀交替挥舞索引 */
    public int getNextBladeIndex() { return nextBladeIndex; }

    /** 设置双刀交替挥舞索引 */
    public void setNextBladeIndex(int nextBladeIndex) { this.nextBladeIndex = nextBladeIndex; }

    /** 当前使用的武器。 */
    public WeaponType getWeapon() { return slots[slotIndex]; }

    /** 另一把持有的武器（没有则为 null）。 */
    public WeaponType getOffhandWeapon() { return slots[1 - slotIndex]; }

    /**
     * Q 键：在两把武器间切换（第二把为空时不切换）。
     * 切换后重置蓄力等武器状态，避免旧武器状态残留。
     */
    public void cycleWeapon() {
        if (slots[1] != null) {
            slotIndex = 1 - slotIndex;
            resetWeaponState();
        }
    }

    /**
     * 拾取武器：当前槽为空则放入；仅另一槽为空则放入另一槽并切换过去；
     * 两槽均满则替换当前槽，返回被丢下的武器（由调用方生成地面掉落物）。
     *
     * @param type 拾取的武器类型
     * @return 被替换丢下的武器（未发生替换时为 null）
     */
    public WeaponType pickupWeapon(WeaponType type) {
        if (slots[slotIndex] == null) {
            slots[slotIndex] = type;
            resetWeaponState();
            return null;
        }
        if (slots[1 - slotIndex] == null) {
            slots[1 - slotIndex] = type;
            slotIndex = 1 - slotIndex;
            resetWeaponState();
            return null;
        }
        WeaponType dropped = slots[slotIndex];
        slots[slotIndex] = type;
        resetWeaponState();
        return dropped;
    }

    /**
     * 当前武器蓄力满所需帧数（弓类与咖喱棒外返回 0）。
     * 巨弓、咖喱棒、常规弓分别对应各自蓄力时长。
     *
     * @return 蓄力满帧数
     */
    public int getChargeMax() {
        if (getWeapon() == WeaponType.GIANT_BOW) {
            return GIANT_BOW_CHARGE_MAX;
        }
        if (getWeapon() == WeaponType.EXCALIBUR) {
            return EXCALIBUR_CHARGE_MAX;
        }
        if (getWeapon().isBow()) {
            return BOW_CHARGE_MAX;
        }
        return 0;
    }

    /** 重置武器相关状态（切换武器/进入下一关时调用）。 */
    public void resetWeaponState() {
        bowCharging = false;
        bowChargeTicks = 0;
        nextBladeIndex = 0;
    }

    /**
     * 每逻辑帧更新：先检测是否陷入墙内并自动解卡，再处理方向键移动、
     * 速度修正与墙体碰撞，并推进各类计时器。
     * 移动向量归一化后乘速度，X/Y 轴分别做墙体碰撞检测；
     * 火力全开期间移速提升 50%，巨弓蓄力期间移速降至 25%；
     * 护盾无论是否脱战，每 120 帧回复 1 点。
     *
     * @param up    上方向键按下
     * @param down  下方向键按下
     * @param left  左方向键按下
     * @param right 右方向键按下
     * @param map   当前地图（碰撞判定）
     */
    public void update(boolean up, boolean down, boolean left, boolean right, DungeonMap map) {
        // 卡墙解卡：任何原因（钩锁拉扯、技能位移、出生点兜底等）使人物陷入墙内时，
        // 自动向最近空旷方向弹出，避免永久卡死
        if (map.collidesWithWall(px, py, RADIUS)) {
            resolveStuckInWall(map);
        }
        double dx = 0;
        double dy = 0;
        if (up) dy--;
        if (down) dy++;
        if (left) dx--;
        if (right) dx++;
        moving = dx != 0 || dy != 0;
        if (dx != 0 || dy != 0) {
            double speed = MOVE_SPEED;
            if (firepowerTicks > 0) {
                speed *= 1.5; // 火力全开：移速提升 50%
            }
            if (bowCharging && getWeapon() == WeaponType.GIANT_BOW) {
                speed *= GIANT_BOW_CHARGE_SPEED;
            }
            double length = Math.sqrt(dx * dx + dy * dy);
            dx = dx / length * speed;
            dy = dy / length * speed;
            if (!map.collidesWithWall(px + dx, py, RADIUS)) px += dx;
            if (!map.collidesWithWall(px, py + dy, RADIUS)) py += dy;
            walkCycle += 0.42;
        }
        if (fireCooldown > 0) fireCooldown--;
        if (meleeCooldown > 0) meleeCooldown--;
        if (slashTimer > 0) slashTimer--;
        if (handKnifeTimer > 0) handKnifeTimer--;
        if (damageInvulnerability > 0) {
            damageInvulnerability--;
        }
        // 护盾无论是否脱战，每 120 帧（2 秒）回复 1 点
        if (shield < maxShield && ++shieldRegenCounter >= 120) {
            shield++;
            shieldRegenCounter = 0;
        }
    }

    /**
     * 卡墙解卡：依次沿四个正方向小步向外探测，移入第一个不与墙体/边界碰撞的位置。
     * 作为兜底手段覆盖所有外力位移造成的陷入墙内情况（钩锁拉扯、出生点兜底等）。
     *
     * @param map 当前地图（碰撞判定）
     */
    private void resolveStuckInWall(DungeonMap map) {
        double[][] directions = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};
        for (double[] direction : directions) {
            double nx = px;
            double ny = py;
            for (int i = 0; i < 60; i++) {
                nx += direction[0];
                ny += direction[1];
                if (!map.collidesWithWall(nx, ny, RADIUS)) {
                    px = nx;
                    py = ny;
                    return;
                }
            }
        }
    }

    /**
     * 承受伤害：无敌帧或瞬身闪避期间直接免疫；
     * 手持血刀时伤害翻倍；护盾未耗尽时伤害只扣护盾（溢出部分作废，下一击才扣血），
     * 护盾耗尽后伤害直接扣除生命；受击后进入 18 帧无敌。
     *
     * @param damage 原始伤害值
     * @return 结算后的剩余生命值
     */
    @Override
    public int takeDamage(int damage) {
        if (damageInvulnerability > 0 || dodgeTicks > 0) {
            return getHp();
        }
        // 血刀诅咒：手持血刀时受到伤害翻倍
        if (getWeapon() == WeaponType.BLOOD_BLADE) {
            damage *= 2;
        }
        damageInvulnerability = 18;
        if (shield > 0) {
            // 护盾防溢出：单次伤害最多清空全部护盾，多余伤害作废，下一击才开始扣血
            shield = Math.max(0, shield - damage);
        } else {
            setHp(getHp() - damage);
        }
        return getHp();
    }
}
