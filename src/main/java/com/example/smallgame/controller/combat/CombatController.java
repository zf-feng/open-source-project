package com.example.smallgame.controller.combat;

import com.example.smallgame.model.entity.Character;
import com.example.smallgame.model.entity.DungeonMap;
import com.example.smallgame.model.entity.Monster;
import com.example.smallgame.model.entity.Player;
import com.example.smallgame.model.entity.WeaponType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 战斗控制器：按武器类型分流攻击逻辑。
 * 枪类：点按/按住连发；弓类：长按蓄力、松手射箭（蓄力越久伤害越高）；
 * 剑类：半圆挥砍近战；双刀：按住连发（冷却限速）交替掷出两把飞刀；
 * 手刀：非剑类武器近身自动切换——敌人进入手刀范围后左键攻击变为手刀，
 * 原武器暂时禁用，脱离范围后恢复原武器攻击。
 * <p>
 * 引用文件：model.entity（Player、Monster、WeaponType、DungeonMap、Character）。
 * 被 GameMainView（攻击输入）、BulletRenderer（飞行物绘制）、LevelController（怪物 AI 攻击）调用。
 */
public final class CombatController {
    public static final int INVINCIBLE_FRAMES = 30;
    public static final int DEFAULT_MONSTER_ATTACK = 8;

    /** 手枪：每发能量消耗与射击冷却（60Hz 下约 4 发/秒）。 */
    private static final int PISTOL_ENERGY_COST = 2;
    private static final int PISTOL_COOLDOWN = 15;
    private static final int PISTOL_DAMAGE = 11;
    /** 精灵之弓：伤害区间（满蓄力 1 秒）与能量消耗区间（随蓄力增长）。 */
    private static final int BOW_MIN_DAMAGE = 10;
    private static final int BOW_MAX_DAMAGE = 40;
    private static final int BOW_MIN_ENERGY_COST = 3;
    private static final int BOW_MAX_ENERGY_COST = 7;
    /** 双刀：掷刀冷却、单刀伤害与能量消耗。 */
    private static final int BLADE_COOLDOWN = 30;
    private static final int BLADE_DAMAGE = 28;
    private static final int BLADE_ENERGY_COST = 2;

    /** 冰霜剑：近战伤害（一刀击杀 2-1 小怪）、挥砍冷却与冰冻概率。 */
    private static final int FROST_SWORD_DAMAGE = 85;
    private static final int FROST_SWORD_COOLDOWN = 30;
    private static final double FROST_FREEZE_CHANCE = 0.20;

    /** 复合弓：蓄满一箭秒杀 2-1 小怪，机制与精灵之弓一致（满蓄力贯穿）。 */
    private static final int COMPOUND_MIN_DAMAGE = 20;
    private static final int COMPOUND_MAX_DAMAGE = 90;
    private static final int COMPOUND_MIN_COST = 4;
    private static final int COMPOUND_MAX_COST = 10;

    /** 猎人弓：机制与复合弓一致，一次朝三个方向射三支箭，消耗为复合弓四倍。 */
    private static final int HUNTER_ARROW_COUNT = 3;
    private static final double HUNTER_SPREAD = 0.21;
    private static final int HUNTER_COST_MULTIPLIER = 4;

    /** 黄金沙漠之鹰：一枪造成 2-1 小怪一半血量伤害。 */
    private static final int GOLDEN_DEAGLE_DAMAGE = 42;
    private static final int GOLDEN_DEAGLE_COOLDOWN = 20;
    private static final int GOLDEN_DEAGLE_COST = 6;

    /** 漫游左轮：射速为普通枪 1.5 倍，一枪造成 2-1 小怪三分之一血量伤害。 */
    private static final int REVOLVER_DAMAGE = 28;
    private static final int REVOLVER_COOLDOWN = 10;
    private static final int REVOLVER_COST = 3;

    /** 巨弓：蓄力 3 秒，蓄满一箭造成 3-5 Boss 五分之一血量伤害，无贯穿。 */
    private static final int GIANT_MIN_DAMAGE = 40;
    private static final int GIANT_MAX_DAMAGE = 210;
    private static final int GIANT_MIN_COST = 8;
    private static final int GIANT_MAX_COST = 25;

    /** 咖喱棒：近战伤害一剑造成 3-1 小怪 75% 血量；
     *  长按蓄力（1.5 秒），蓄满后松开释放大范围剑气（穿透墙体与敌人），消耗 40 能量。 */
    private static final int EXCALIBUR_DAMAGE = 100;
    private static final int EXCALIBUR_COOLDOWN = 35;
    private static final int EXCALIBUR_BEAM_DAMAGE = 150;
    private static final int EXCALIBUR_BEAM_COST = 40;

    /** 雪人之鹰：一枪造成 3-1 小怪四分之一血量伤害，概率冰冻。 */
    private static final int SNOWMAN_EAGLE_DAMAGE = 34;
    private static final int SNOWMAN_EAGLE_COOLDOWN = 15;
    private static final int SNOWMAN_EAGLE_COST = 4;
    private static final double SNOWMAN_FREEZE_CHANCE = 0.25;

    /** 血刀：一刀秒杀 3-5 小怪；血量不满时挥砍消耗大量能量回 1 血；持有时受伤翻倍。 */
    private static final int BLOOD_BLADE_DAMAGE = 180;
    private static final int BLOOD_BLADE_COOLDOWN = 40;
    private static final int BLOOD_BLADE_HEAL_COST = 50;

    /** 手刀（近身自动切换）：不消耗能量、无攻击间隔，伤害为第一关小怪血量的三分之一。 */
    private static final int HAND_KNIFE_DAMAGE = 11;
    /** 手刀切换范围：敌人进入此范围后左键攻击自动切换为手刀（比伤害范围更宽松，便于起手）。 */
    private static final double HAND_KNIFE_RANGE = 72;
    /** 手刀伤害范围：挥击命中判定半径（小于切换范围，切换后敌人必在挥击范围内）。 */
    private static final double HAND_KNIFE_DAMAGE_RANGE = 60;

    /** 剑类挥砍半径（前方半圆）与冰冻时长（3 秒）。 */
    private static final double SWORD_RANGE = 90;
    private static final int FREEZE_TICKS = 180;
    /** 咖喱棒满蓄剑气的命中判定额外半径（大范围）与存活帧数（约 640px 射程）。 */
    private static final double BEAM_HIT_RADIUS = 22;
    private static final int BEAM_LIFE = 80;

    private static final double BULLET_SPEED = 10;
    private static final double ARROW_SPEED = 9;
    private static final double BLADE_SPEED = 9;
    private static final double BEAM_SPEED = 8;
    /** 飞行物存活帧数（刀与剑气的射程上限；枪弹与箭无限远，不随时间消失）。 */
    private static final int PROJECTILE_LIFE = 55;
    /** 箭插在墙上的保留时长（60Hz 下 600 帧 = 10 秒）。 */
    private static final int ARROW_STUCK_TICKS = 600;
    /** 敌人子弹：飞行速度（慢于玩家枪弹）；射程无限远，仅碰墙或命中人物才消失。 */
    private static final double ENEMY_BULLET_SPEED = 3.3;
    /** 散弹枪弹丸：初速（刚发射时较快）与每帧线性减速量，速度归零瞬间消失，射程约六个身位（≈168px）。 */
    private static final double SHOTGUN_PELLET_SPEED = 8.4;
    private static final double SHOTGUN_PELLET_FRICTION = 0.21;

    private final List<Bullet> bullets = new ArrayList<>();
    /** 敌人发射的子弹（远程怪直线射击，命中玩家造成伤害）。 */
    private final List<EnemyBullet> enemyBullets = new ArrayList<>();
    private final Random random = new Random();

    /** 获取玩家飞行物列表 */
    public List<Bullet> getBullets() { return bullets; }

    /** 获取敌人子弹列表 */
    public List<EnemyBullet> getEnemyBullets() { return enemyBullets; }

    /** 清空玩家与敌人的全部子弹 */
    public void clearBullets() { bullets.clear(); enemyBullets.clear(); }

    /** 攻击键按下：非剑类武器近身有敌人时挥出手刀（点按一次挥一次），否则按原武器分流。 */
    public void attackPressed(Player player, double mouseX, double mouseY,
                              List<Monster> enemies, Consumer<Monster> onEnemyKilled) {
        WeaponType weapon = player.getWeapon();
        if (weapon.isBow()) {
            if (!swingHandKnife(player, enemies, onEnemyKilled)) {
                player.setBowCharging(true);
            }
        } else if (weapon == WeaponType.EXCALIBUR) {
            // 咖喱棒：按下开始蓄力（长按蓄满松开发剑气；点按即松则挥砍）
            player.setBowCharging(true);
            player.setBowChargeTicks(0);
        } else if (weapon.isSword()) {
            swingSword(player, mouseX, mouseY, enemies, onEnemyKilled);
        } else if (weapon == WeaponType.DUAL_BLADES) {
            if (!swingHandKnife(player, enemies, onEnemyKilled)) {
                throwBlade(player, mouseX, mouseY, enemies, onEnemyKilled);
            }
        } else {
            if (!swingHandKnife(player, enemies, onEnemyKilled)) {
                shoot(player, mouseX, mouseY, enemies, onEnemyKilled);
            }
        }
    }

    /** 攻击键持续按住：枪弹处于射击状态时保持连发、弓蓄力中不因敌人近身而中断
     *  （手刀仅点按或射箭后触发，长按不连续挥击）。 */
    public void attackHeld(Player player, double mouseX, double mouseY,
                           List<Monster> enemies, Consumer<Monster> onEnemyKilled) {
        WeaponType weapon = player.getWeapon();
        if (weapon.isBow()) {
            // 蓄力中即使敌人进入手刀范围也继续蓄力；箭射出后才判定手刀
            if (player.isBowCharging()) {
                double ratio = player.getBowChargeTicks() / (double) player.getChargeMax();
                int need = bowCostFor(weapon, Math.min(1.0, ratio));
                if (player.getEnergy() < need) {
                    // 能量不足：取消蓄力
                    player.setBowCharging(false);
                    player.setBowChargeTicks(0);
                } else {
                    // 火力全开：蓄力速度变为 2 倍
                    int chargeStep = player.getFirepowerTicks() > 0 ? 2 : 1;
                    player.setBowChargeTicks(Math.min(player.getChargeMax(),
                            player.getBowChargeTicks() + chargeStep));
                }
            }
        } else if (weapon == WeaponType.EXCALIBUR) {
            // 咖喱棒蓄力：按住持续充能（火力全开蓄力速度为 2 倍）
            if (player.isBowCharging()) {
                int chargeStep = player.getFirepowerTicks() > 0 ? 2 : 1;
                player.setBowChargeTicks(Math.min(player.getChargeMax(),
                        player.getBowChargeTicks() + chargeStep));
            }
        } else if (weapon.isGun()) {
            // 处于射击状态（长按连发）时保持持续攻击，敌人进入手刀范围也不中断
            shoot(player, mouseX, mouseY, enemies, onEnemyKilled);
        } else if (weapon == WeaponType.DUAL_BLADES) {
            if (!hasHandKnifeTarget(player, enemies)) {
                throwBlade(player, mouseX, mouseY, enemies, onEnemyKilled);
            }
        } else if (weapon.isSword()) {
            swingSword(player, mouseX, mouseY, enemies, onEnemyKilled);
        }
    }

    /** 攻击键松开：弓松弦射箭（伤害随蓄力时间），并复位蓄力状态；
     *  蓄力期间不受手刀范围影响，箭射出后若敌人在手刀释放范围内则补一刀手刀。
     *  咖喱棒：蓄满且能量足够时释放大范围穿透剑气，否则退化为近战挥砍。 */
    public void attackReleased(Player player, double mouseX, double mouseY,
                               List<Monster> enemies, Consumer<Monster> onEnemyKilled) {
        if (player.getWeapon() == WeaponType.EXCALIBUR) {
            releaseExcalibur(player, mouseX, mouseY, enemies, onEnemyKilled);
            return;
        }
        if (!player.getWeapon().isBow()) {
            return;
        }
        if (player.isBowCharging() || player.getBowChargeTicks() > 0) {
            // 先把箭射出去，箭成功射出后才判定手刀
            if (releaseBow(player, mouseX, mouseY, enemies, onEnemyKilled)) {
                swingHandKnife(player, enemies, onEnemyKilled);
            }
        }
        player.setBowCharging(false);
        player.setBowChargeTicks(0);
    }

    /** 咖喱棒松键：蓄满且能量足够时释放大范围剑气（穿透墙体与敌人）；
     *  未蓄满（点按）或能量不足时退化为近战挥砍。 */
    private void releaseExcalibur(Player player, double mouseX, double mouseY,
                                  List<Monster> enemies, Consumer<Monster> onEnemyKilled) {
        boolean full = player.getBowChargeTicks() >= player.getChargeMax();
        player.setBowCharging(false);
        player.setBowChargeTicks(0);
        boolean firepower = player.getFirepowerTicks() > 0;
        if (full && (player.getEnergy() >= EXCALIBUR_BEAM_COST || firepower)) {
            double angle = Math.atan2(mouseY - player.getPy(), mouseX - player.getPx());
            double beamY = player.getPy() + Player.WEAPON_DRAW_OFFSET_Y;
            bullets.add(new Bullet(player.getPx() + Math.cos(angle) * 26,
                    beamY + Math.sin(angle) * 26,
                    Math.cos(angle) * BEAM_SPEED, Math.sin(angle) * BEAM_SPEED,
                    EXCALIBUR_BEAM_DAMAGE, 4, true, 0, true, BEAM_LIFE));
            if (!firepower) {
                player.setEnergy(player.getEnergy() - EXCALIBUR_BEAM_COST);
            }
            player.setSlashTimer(12);
            return;
        }
        swingSword(player, mouseX, mouseY, enemies, onEnemyKilled);
    }

    /** 枪类射击：按武器类型决定伤害、冷却与能量消耗。 */
    public void shoot(Player player, double mouseX, double mouseY,
                      List<Monster> enemies, Consumer<Monster> onEnemyKilled) {
        int damage;
        int cooldown;
        int cost;
        int kind = 0;
        double freezeChance = 0;
        switch (player.getWeapon()) {
            case GOLDEN_DEAGLE:
                damage = GOLDEN_DEAGLE_DAMAGE;
                cooldown = GOLDEN_DEAGLE_COOLDOWN;
                cost = GOLDEN_DEAGLE_COST;
                kind = 5;
                break;
            case REVOLVER:
                damage = REVOLVER_DAMAGE;
                cooldown = REVOLVER_COOLDOWN;
                cost = REVOLVER_COST;
                break;
            case SNOWMAN_EAGLE:
                damage = SNOWMAN_EAGLE_DAMAGE;
                cooldown = SNOWMAN_EAGLE_COOLDOWN;
                cost = SNOWMAN_EAGLE_COST;
                kind = 6;
                freezeChance = SNOWMAN_FREEZE_CHANCE;
                break;
            default:
                damage = PISTOL_DAMAGE;
                cooldown = PISTOL_COOLDOWN;
                cost = PISTOL_ENERGY_COST;
        }
        if (player.getFireCooldown() > 0) {
            return;
        }
        boolean firepower = player.getFirepowerTicks() > 0;
        if (player.getEnergy() < cost && !firepower) {
            return;
        }
        double dx = mouseX - player.getPx();
        double dy = mouseY - (player.getPy() + Player.WEAPON_DRAW_OFFSET_Y);
        double length = Math.max(0.001, Math.sqrt(dx * dx + dy * dy));
        bullets.add(new Bullet(player.getPx() + dx / length * 20,
                player.getPy() + Player.WEAPON_DRAW_OFFSET_Y + dy / length * 20,
                dx / length * BULLET_SPEED, dy / length * BULLET_SPEED, damage, kind,
                false, freezeChance));
        player.setEnergy(player.getEnergy() - (firepower ? 0 : cost));
        // 火力全开：攻速提升为原来的 200%（冷却缩短为二分之一）
        player.setFireCooldown(firepower ? Math.max(1, cooldown / 2) : cooldown);
    }

    /** 弓松弦：伤害与消耗随蓄力增长；普通弓/复合弓满蓄力可贯穿；猎人弓三箭齐射。
     *  返回 true 表示箭已射出（能量不足时返回 false 不射箭）。 */
    private boolean releaseBow(Player player, double mouseX, double mouseY,
                               List<Monster> enemies, Consumer<Monster> onEnemyKilled) {
        int chargeMax = player.getChargeMax();
        double ratio = player.getBowChargeTicks() / (double) chargeMax;
        boolean full = player.getBowChargeTicks() >= chargeMax;
        WeaponType weapon = player.getWeapon();

        int damage;
        int cost;
        boolean pierce;
        int arrowCount = 1;
        boolean giant = false;
        switch (weapon) {
            case COMPOUND_BOW:
                damage = (int) Math.round(COMPOUND_MIN_DAMAGE
                        + (COMPOUND_MAX_DAMAGE - COMPOUND_MIN_DAMAGE) * ratio);
                cost = bowCostFor(weapon, ratio);
                pierce = full;
                break;
            case HUNTER_BOW:
                damage = (int) Math.round(COMPOUND_MIN_DAMAGE
                        + (COMPOUND_MAX_DAMAGE - COMPOUND_MIN_DAMAGE) * ratio);
                cost = bowCostFor(weapon, ratio);
                pierce = full;
                arrowCount = HUNTER_ARROW_COUNT;
                break;
            case GIANT_BOW:
                damage = (int) Math.round(GIANT_MIN_DAMAGE
                        + (GIANT_MAX_DAMAGE - GIANT_MIN_DAMAGE) * ratio);
                cost = bowCostFor(weapon, ratio);
                pierce = false;
                giant = true;
                break;
            default:
                damage = (int) Math.round(BOW_MIN_DAMAGE
                        + (BOW_MAX_DAMAGE - BOW_MIN_DAMAGE) * ratio);
                cost = bowCostFor(weapon, ratio);
                pierce = full;
        }
        boolean firepower = player.getFirepowerTicks() > 0;
        if (player.getEnergy() < cost && !firepower) {
            return false;
        }
        double dx = mouseX - player.getPx();
        double dy = mouseY - (player.getPy() + Player.WEAPON_DRAW_OFFSET_Y);
        double length = Math.max(0.001, Math.sqrt(dx * dx + dy * dy));
        double baseAngle = Math.atan2(dy, dx);
        double startX = player.getPx() + dx / length * 20;
        double startY = player.getPy() + Player.WEAPON_DRAW_OFFSET_Y + dy / length * 20;
        for (int i = 0; i < arrowCount; i++) {
            double offset = arrowCount > 1 ? (i - (arrowCount - 1) / 2.0) * HUNTER_SPREAD : 0;
            double arrowAngle = baseAngle + offset;
            bullets.add(new Bullet(startX, startY,
                    Math.cos(arrowAngle) * ARROW_SPEED, Math.sin(arrowAngle) * ARROW_SPEED,
                    damage, giant ? 8 : 1, pierce));
        }
        player.setEnergy(player.getEnergy() - (firepower ? 0 : cost));
        return true;
    }

    /** 弓类按蓄力比例的消耗（猎人弓为复合弓四倍）。 */
    private int bowCostFor(WeaponType weapon, double ratio) {
        switch (weapon) {
            case COMPOUND_BOW:
                return (int) Math.round(COMPOUND_MIN_COST
                        + (COMPOUND_MAX_COST - COMPOUND_MIN_COST) * ratio);
            case HUNTER_BOW:
                return (int) Math.round((COMPOUND_MIN_COST
                        + (COMPOUND_MAX_COST - COMPOUND_MIN_COST) * ratio)
                        * HUNTER_COST_MULTIPLIER);
            case GIANT_BOW:
                return (int) Math.round(GIANT_MIN_COST
                        + (GIANT_MAX_COST - GIANT_MIN_COST) * ratio);
            default:
                return (int) Math.round(BOW_MIN_ENERGY_COST
                        + (BOW_MAX_ENERGY_COST - BOW_MIN_ENERGY_COST) * ratio);
        }
    }

    /**
     * 手刀点按挥击：手刀切换范围内有敌人时挥出一刀，对伤害范围内所有怪物造成伤害，
     * 不消耗能量、无前后攻击间隔（每次点按立即挥击并重置动画，不等待上一次动画结束）。
     * 返回 true 表示本次点按由手刀接管（原武器不攻击）。
     */
    private boolean swingHandKnife(Player player, List<Monster> enemies,
                                   Consumer<Monster> onEnemyKilled) {
        if (!hasHandKnifeTarget(player, enemies)) {
            return false;
        }
        Iterator<Monster> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Monster enemy = enemyIterator.next();
            if (distance(player.getPx(), player.getPy(), enemy.getPx(), enemy.getPy())
                    < HAND_KNIFE_DAMAGE_RANGE) {
                enemy.setHp(enemy.getHp() - HAND_KNIFE_DAMAGE);
                if (enemy.getHp() <= 0) {
                    onEnemyKilled.accept(enemy);
                    enemyIterator.remove();
                }
            }
        }
        player.setHandKnifeTimer(Player.HAND_KNIFE_SWING_TICKS);
        return true;
    }

    /** 近身是否有敌人（手刀切换模式判定，使用切换范围）。 */
    private boolean hasHandKnifeTarget(Player player, List<Monster> enemies) {
        for (Monster enemy : enemies) {
            if (distance(player.getPx(), player.getPy(), enemy.getPx(), enemy.getPy())
                    < HAND_KNIFE_RANGE) {
                return true;
            }
        }
        return false;
    }

    /** 剑类挥砍：前方半圆范围近战，冰霜剑低概率冰冻；血刀附带吸血。 */
    public void swingSword(Player player, double mouseX, double mouseY,
                           List<Monster> enemies, Consumer<Monster> onEnemyKilled) {
        if (player.getMeleeCooldown() > 0) {
            return;
        }
        WeaponType weapon = player.getWeapon();
        boolean frost = weapon == WeaponType.FROST_SWORD;
        boolean blood = weapon == WeaponType.BLOOD_BLADE;
        boolean firepower = player.getFirepowerTicks() > 0;
        int damage = blood ? BLOOD_BLADE_DAMAGE
                : frost ? FROST_SWORD_DAMAGE : EXCALIBUR_DAMAGE;
        int cooldown = blood ? BLOOD_BLADE_COOLDOWN
                : frost ? FROST_SWORD_COOLDOWN : EXCALIBUR_COOLDOWN;
        double angle = Math.atan2(mouseY - player.getPy(), mouseX - player.getPx());

        Iterator<Monster> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Monster enemy = enemyIterator.next();
            double enemyAngle = Math.atan2(enemy.getPy() - player.getPy(),
                    enemy.getPx() - player.getPx());
            if (distance(player.getPx(), player.getPy(), enemy.getPx(), enemy.getPy())
                    < SWORD_RANGE && Math.cos(enemyAngle - angle) > 0) {
                enemy.setHp(enemy.getHp() - damage);
                if (frost && random.nextDouble() < FROST_FREEZE_CHANCE) {
                    enemy.setFrozenTicks(FREEZE_TICKS);
                }
                if (enemy.getHp() <= 0) {
                    onEnemyKilled.accept(enemy);
                    enemyIterator.remove();
                }
            }
        }
        player.setSlashTimer(12);
        // 火力全开：攻速提升为原来的 200%
        player.setMeleeCooldown(firepower ? Math.max(1, cooldown / 2) : cooldown);

        if (blood && player.getHp() < player.getMaxHp()
                && (player.getEnergy() >= BLOOD_BLADE_HEAL_COST || firepower)) {
            player.setHp(player.getHp() + 1);
            if (!firepower) {
                player.setEnergy(player.getEnergy() - BLOOD_BLADE_HEAL_COST);
            }
        }
    }

    /** 双刀投掷：按住连发（冷却限速）交替掷出两把刀，伤害一致，消耗能量；能量不足时手刀。 */
    public void throwBlade(Player player, double mouseX, double mouseY,
                           List<Monster> enemies, Consumer<Monster> onEnemyKilled) {
        if (player.getMeleeCooldown() > 0) {
            return;
        }
        boolean firepower = player.getFirepowerTicks() > 0;
        if (player.getEnergy() < BLADE_ENERGY_COST && !firepower) {
            return;
        }
        double dx = mouseX - player.getPx();
        double dy = mouseY - (player.getPy() + Player.WEAPON_DRAW_OFFSET_Y);
        double length = Math.max(0.001, Math.sqrt(dx * dx + dy * dy));
        int bladeIndex = player.getNextBladeIndex();
        bullets.add(new Bullet(player.getPx() + dx / length * 20,
                player.getPy() + Player.WEAPON_DRAW_OFFSET_Y + dy / length * 20,
                dx / length * BLADE_SPEED, dy / length * BLADE_SPEED, BLADE_DAMAGE, 2 + bladeIndex));
        player.setNextBladeIndex(1 - bladeIndex);
        // 火力全开：攻速提升为原来的 200%
        player.setMeleeCooldown(firepower ? Math.max(1, BLADE_COOLDOWN / 2) : BLADE_COOLDOWN);
        player.setEnergy(player.getEnergy() - (firepower ? 0 : BLADE_ENERGY_COST));
    }

    /**
     * 每逻辑帧推进玩家飞行物：插墙箭倒计时、位移、边界/墙体碰撞、命中结算。
     * 箭碰墙后插墙保留；贯穿箭命中后继续飞行且后续伤害降为 75%；
     * 枪弹与弓箭无限远飞行，刀与剑气受存活帧数限制。
     *
     * @param enemies       怪物列表
     * @param map           地图（墙体碰撞）
     * @param onEnemyKilled 怪物死亡回调
     */
    public void updateBullets(List<Monster> enemies, DungeonMap map,
                              Consumer<Monster> onEnemyKilled) {
        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            // 插在墙上的箭：停在原地保留建模，倒计时结束后消失
            if (bullet.stuckTicks > 0) {
                bullet.stuckTicks--;
                if (bullet.stuckTicks <= 0) {
                    bulletIterator.remove();
                }
                continue;
            }
            bullet.x += bullet.dx;
            bullet.y += bullet.dy;
            // 枪弹与弓箭无限远飞行，不随时间消失；刀与剑气保持原有射程
            if (!fliesForever(bullet)) {
                bullet.life--;
            }
            if (bullet.life <= 0 || bullet.x < 0 || bullet.x > DungeonMap.WIDTH
                    || bullet.y < DungeonMap.WORLD_TOP || bullet.y > DungeonMap.HEIGHT) {
                bulletIterator.remove();
                continue;
            }
            if (!bullet.pierceWalls && map.collidesWithWall(bullet.x, bullet.y, 3)) {
                if (bullet.kind == 1 || bullet.kind == 8) {
                    // 箭碰墙：停止飞行，建模插在墙上保留 10 秒后消失
                    bullet.stuckTicks = ARROW_STUCK_TICKS;
                    continue;
                }
                bulletIterator.remove();
                continue;
            }
            boolean hit = false;
            Iterator<Monster> enemyIterator = enemies.iterator();
            while (enemyIterator.hasNext()) {
                Monster enemy = enemyIterator.next();
                if (bullet.pierce && bullet.hitMonsters.contains(enemy)) {
                    continue;
                }
                if (distance(bullet.x, bullet.y, enemy.getPx(), enemy.getPy())
                        < enemy.getHitRadius() + (bullet.kind == 4 ? BEAM_HIT_RADIUS : 5)) {
                    int dealt = bullet.pierce && !bullet.hitMonsters.isEmpty()
                            ? bullet.damage * 3 / 4 : bullet.damage;
                    enemy.setHp(enemy.getHp() - dealt);
                    if (bullet.freezeChance > 0 && random.nextDouble() < bullet.freezeChance) {
                        enemy.setFrozenTicks(FREEZE_TICKS);
                    }
                    hit = true;
                    if (bullet.pierce) {
                        bullet.hitMonsters.add(enemy);
                    }
                    if (enemy.getHp() <= 0) {
                        onEnemyKilled.accept(enemy);
                        enemyIterator.remove();
                    }
                    if (!bullet.pierce) {
                        break;
                    }
                }
            }
            if (hit && !bullet.pierce) {
                bulletIterator.remove();
            }
        }
    }

    /**
     * 怪物对玩家发起一次近战接触攻击（双方存活时结算，伤害至少为 1）。
     *
     * @param monster 怪物实体
     * @param player  目标玩家
     */
    public void handleMonsterAttack(Monster monster, Player player) {
        if (monster.isAlive() && player.isAlive()) applyDamage(player, Math.max(1, monster.getAttack()));
    }

    /** 远程怪发射一颗朝玩家飞行的子弹（带轻微散布），伤害与接触攻击一致。 */
    public void spawnEnemyBullet(Monster monster, Player player) {
        double dx = player.getPx() - monster.getPx();
        double dy = player.getPy() - monster.getPy();
        double length = Math.max(0.001, Math.hypot(dx, dy));
        double spread = (random.nextDouble() - 0.5) * 0.18;
        spawnEnemyBulletAt(monster, Math.atan2(dy, dx) + spread);
    }

    /** 远程怪朝指定角度发射一颗敌人子弹（各弹幕模式共用），伤害与接触攻击一致。 */
    public void spawnEnemyBulletAt(Monster monster, double angle) {
        enemyBullets.add(new EnemyBullet(monster.getPx(), monster.getPy(),
                Math.cos(angle) * ENEMY_BULLET_SPEED, Math.sin(angle) * ENEMY_BULLET_SPEED,
                Math.max(1, monster.getAttack())));
    }

    /** 散弹枪弹丸：初速快、逐帧线性减速，速度归零瞬间消失（射程约六个身位）。 */
    public void spawnShotgunPellet(Monster monster, double angle) {
        enemyBullets.add(new EnemyBullet(monster.getPx(), monster.getPy(),
                Math.cos(angle) * SHOTGUN_PELLET_SPEED, Math.sin(angle) * SHOTGUN_PELLET_SPEED,
                Math.max(1, monster.getAttack()), SHOTGUN_PELLET_FRICTION));
    }

    /** 更新敌人子弹：移动、碰墙消失、命中玩家造成伤害（玩家无敌帧由 takeDamage 内部处理）。 */
    public void updateEnemyBullets(Player player, DungeonMap map) {
        Iterator<EnemyBullet> bulletIterator = enemyBullets.iterator();
        while (bulletIterator.hasNext()) {
            EnemyBullet bullet = bulletIterator.next();
            // 带减速的弹丸（散弹）：速度逐渐变慢，归零瞬间消失
            if (bullet.friction > 0) {
                double speed = Math.hypot(bullet.dx, bullet.dy);
                double newSpeed = speed - bullet.friction;
                if (newSpeed <= 0) {
                    bulletIterator.remove();
                    continue;
                }
                bullet.dx *= newSpeed / speed;
                bullet.dy *= newSpeed / speed;
            }
            bullet.x += bullet.dx;
            bullet.y += bullet.dy;
            // 射程无限：仅出界（地图边界）、碰墙或命中玩家时消失
            if (bullet.x < 0 || bullet.x > DungeonMap.WIDTH
                    || bullet.y < DungeonMap.WORLD_TOP || bullet.y > DungeonMap.HEIGHT
                    || map.collidesWithWall(bullet.x, bullet.y, 3)) {
                bulletIterator.remove();
                continue;
            }
            if (player.isAlive()
                    && distance(bullet.x, bullet.y, player.getPx(), player.getPy())
                    < Player.RADIUS + 5) {
                player.takeDamage(bullet.damage);
                bulletIterator.remove();
            }
        }
    }

    /** 对目标实体结算一次伤害（负伤害置 0） */
    public void applyDamage(Character target, int damage) { target.takeDamage(Math.max(0, damage)); }

    /** 计算两点间欧几里得距离 */
    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    /** 无限远飞行类型：枪弹类（0/5/6）与弓类箭（1/8），碰到墙体或敌人后才消失。 */
    private static boolean fliesForever(Bullet bullet) {
        return bullet.kind == 0 || bullet.kind == 1 || bullet.kind == 5
                || bullet.kind == 6 || bullet.kind == 8;
    }

    /** 飞行物：子弹（光弹）、箭（立绘）、刀（立绘）、剑气（立绘）。
     *  渲染由 BulletRenderer 读取字段完成，本类只维护飞行数据。 */
    public static final class Bullet {
        public double x;
        public double y;
        public final double dx;
        public final double dy;
        public final int damage;
        public int life = PROJECTILE_LIFE;
        /** 类型：0=普通子弹，1=箭，2=刀（小明），3=刀（小红），4=剑气，
         *  5=金色子弹（沙漠之鹰），6=冰弹（雪人之鹰），8=巨箭。 */
        public final int kind;
        public final double angle;
        /** 是否贯穿（满蓄力箭）：命中后继续飞行，后续目标伤害降为 75%。 */
        public final boolean pierce;
        /** 是否穿透墙体（咖喱棒满蓄剑气）。 */
        public final boolean pierceWalls;
        /** 贯穿箭已命中的敌人（避免同一敌人重复结算）。 */
        public final Set<Monster> hitMonsters;
        /** 命中时冰冻敌人的概率（0 表示无冰冻效果）。 */
        public final double freezeChance;
        /** 箭插在墙上的剩余帧数（-1 表示飞行中，>0 为插墙倒计时，归零后移除）。 */
        public int stuckTicks = -1;

        /**
         * 基础构造（无贯穿、无冰冻）。
         */
        Bullet(double x, double y, double dx, double dy, int damage, int kind) {
            this(x, y, dx, dy, damage, kind, false);
        }

        /**
         * 带贯穿参数的构造。
         */
        Bullet(double x, double y, double dx, double dy, int damage, int kind,
               boolean pierce) {
            this(x, y, dx, dy, damage, kind, pierce, 0);
        }

        /**
         * 带贯穿与冰冻概率的构造。
         */
        Bullet(double x, double y, double dx, double dy, int damage, int kind,
               boolean pierce, double freezeChance) {
            this(x, y, dx, dy, damage, kind, pierce, freezeChance, false, PROJECTILE_LIFE);
        }

        /**
         * 完整构造：含穿透墙体与自定义存活帧数。
         */
        Bullet(double x, double y, double dx, double dy, int damage, int kind,
               boolean pierce, double freezeChance, boolean pierceWalls, int life) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.damage = damage;
            this.kind = kind;
            this.angle = Math.atan2(dy, dx);
            this.pierce = pierce;
            this.pierceWalls = pierceWalls;
            this.hitMonsters = pierce ? new HashSet<>() : null;
            this.freezeChance = freezeChance;
            this.life = life;
        }
    }

    /** 敌人子弹：远程怪发射的紫红能量弹（比玩家子弹慢、有明显外圈光晕）。
     *  渲染由 BulletRenderer 读取字段完成，本类只维护飞行数据。 */
    public static final class EnemyBullet {
        public double x;
        public double y;
        public double dx;
        public double dy;
        public final int damage;
        /** 每帧速度衰减量（散弹弹丸），0=匀速飞行。 */
        public final double friction;

        /**
         * 匀速飞行构造。
         */
        EnemyBullet(double x, double y, double dx, double dy, int damage) {
            this(x, y, dx, dy, damage, 0);
        }

        /**
         * 带速度衰减的构造（散弹弹丸用）。
         */
        EnemyBullet(double x, double y, double dx, double dy, int damage, double friction) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.damage = damage;
            this.friction = friction;
        }
    }
}
