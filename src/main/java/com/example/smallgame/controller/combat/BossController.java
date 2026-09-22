package com.example.smallgame.controller.combat;

import com.example.smallgame.model.entity.DungeonMap;
import com.example.smallgame.model.entity.Monster;
import com.example.smallgame.model.entity.MonsterType;
import com.example.smallgame.model.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Boss 专属特殊攻击控制器：为每个主题的 Boss 实现专属机制。
 * 森林=圣诞树人持续冲刺；冰原=大雪怪跳跃砸地三层环形弹；草原=巨人眼部巨型激光（不可移动）；
 * 沙漠=黄金毒蝎无特殊攻击（高频高移速由 LevelController 处理）；
 * 城堡=大骑士三连斩逐步逼近；黑森林=大巫师三陨石预警坠落；
 * 沼泽=大史莱姆王连续跳砸环形弹；太空=虚空领主召唤虚空裂缝；
 * 火山=赤焰飞龙五连大火球（碰墙分裂小火球）；海岛=钢铁海盗钩锁拉人斩击。
 * 所有 Boss 都会全部小怪弹幕模式（LevelController 轮换），本类只负责专属特殊攻击。
 * <p>
 * 引用文件：model.entity（Monster、MonsterType、Player、DungeonMap、MapTheme）、
 * controller.combat.CombatController（特殊攻击发射子弹时复用其敌弹接口）。
 * 被 LevelController（每帧更新 Boss 特殊攻击）、BossEffectRenderer（特效绘制）调用。
 */
public final class BossController {

    /** 圣诞树人冲刺：冷却、前摇帧数、冲刺速度与时长。 */
    private static final int TREE_CHARGE_COOLDOWN = 240;
    public static final int TREE_CHARGE_WINDUP_TICKS = 46;
    private static final double TREE_CHARGE_SPEED = 4.6;
    private static final int TREE_CHARGE_TICKS = 55;
    /** 大雪怪跳跃砸地：冷却与跳跃总帧数（跳得高、距离远）。 */
    private static final int YETI_JUMP_COOLDOWN = 220;
    private static final int YETI_JUMP_TICKS = 82;
    /** 巨人眼部激光：冷却、前摇帧数与持续帧数（伤害每 25 帧结算一次）。 */
    private static final int GIANT_LASER_COOLDOWN = 260;
    public static final int GIANT_LASER_WINDUP_TICKS = 60;
    private static final int GIANT_LASER_TICKS = 100;
    private static final int GIANT_LASER_HIT_INTERVAL = 25;
    /** 巨人眼部双激光出发点：相对 Boss 中心的横向偏移与纵向高度。 */
    public static final double GIANT_EYE_OFFSET_X = 43;
    public static final double GIANT_EYE_OFFSET_Y = -120;
    /** 大骑士三连斩：冷却、每刀间隔帧数与每刀前进距离（方向第一刀锁定后不再改变）。 */
    private static final int KNIGHT_SLASH_COOLDOWN = 200;
    private static final int KNIGHT_SLASH_STEP = 80;
    /** 大巫师陨石：冷却、每次数量、预警帧数、施法站定帧数与下落帧数。 */
    private static final int WIZARD_METEOR_COOLDOWN = 300;
    private static final int METEOR_WARN_TICKS = 60;
    private static final int WIZARD_CAST_TICKS = METEOR_WARN_TICKS + 10;
    private static final int METEOR_FALL_TICKS = 30;
    public static final int METEOR_BOOM_TICKS = 12;
    /** 陨石落地爆炸的伤害判定半径。 */
    private static final double METEOR_HIT_RADIUS = 75;
    /** 大史莱姆王连续跳砸：冷却、跳跃帧数与连跳次数（跳得高、砸得远）。 */
    private static final int SLIME_SLAM_COOLDOWN = 260;
    private static final int SLIME_JUMP_TICKS = 64;
    private static final int SLIME_SLAM_COUNT = 3;
    /** 虚空领主裂缝：召唤间隔、场上上限、存活帧数与伤害间隔。 */
    private static final int VOID_RIFT_INTERVAL = 180;
    private static final int VOID_MAX_RIFTS = 4;
    private static final int VOID_RIFT_LIFE = 420;
    private static final int VOID_RIFT_HIT_INTERVAL = 30;
    /** 赤焰飞龙大火球：冷却、连发数、发射间隔、火球速度/伤害与分裂小火球。 */
    private static final int DRAGON_FIREBALL_COOLDOWN = 260;
    private static final int DRAGON_FIREBALL_COUNT = 5;
    private static final int DRAGON_SHOT_INTERVAL = 12;
    private static final double FIREBALL_SPEED = 5.0;
    private static final double SMALL_FIREBALL_SPEED = 3.2;
    /** 钢铁海盗钩锁：冷却、瞄准帧数、射出/收回速度、最大射程。 */
    private static final int PIRATE_HOOK_COOLDOWN = 300;
    public static final int PIRATE_HOOK_AIM_TICKS = 68;
    private static final double PIRATE_HOOK_SPEED = 7.0;
    private static final double PIRATE_HOOK_RETRACT_SPEED = 3.0;
    public static final double PIRATE_HOOK_MAX_RANGE = 700;
    private static final double PIRATE_PULL_SPEED = 5.5;

    private final Random random = new Random();
    private final List<Meteor> meteors = new ArrayList<>();
    private final List<Rift> rifts = new ArrayList<>();
    private final List<Fireball> fireballs = new ArrayList<>();
    private final List<SmallFireball> smallFireballs = new ArrayList<>();

    private Monster boss;
    private int specialCooldown;
    /** 圣诞树人冲刺：前摇帧数与冲刺剩余帧数、锁定方向。 */
    private int chargeWindupTicks;
    private int chargeTicks;
    private double chargeAngle;
    /** 大雪怪/大史莱姆跳跃状态。 */
    private int jumpTicks;
    private int jumpProgress;
    private double jumpStartX;
    private double jumpStartY;
    private double jumpTargetX;
    private double jumpTargetY;
    /** 巨人激光状态（巨人不可移动）：前摇聚能 → 锁定发射。 */
    private int laserWindupTicks;
    private int laserTicks;
    private int laserHitTicks;
    private double laserAngle;
    /** 大骑士三连斩状态（-1 空闲，0~2 第几刀）。 */
    private int slashPhase = -1;
    private int slashPhaseTicks;
    private double slashAngle;
    /** 大巫师施法站定帧数（召唤陨石时自身不动）。 */
    private int wizardCastTicks;
    /** 大史莱姆王连跳计数与落地停顿。 */
    private int slimeSlamLeft;
    private int slimeRestTicks;
    /** 赤焰飞龙连发计数。 */
    private int fireballLeft;
    private int fireballShotTicks;
    /** 钢铁海盗钩锁状态：0 空闲 / 1 瞄准 / 2 射出 / 3 收回 / 4 拉回 / 5 斩击。 */
    private int hookPhase;
    private int hookAimTicks;
    private double hookAngle;
    private double hookX;
    private double hookY;
    private double hookTravel;
    /** 拉回被墙体挡住的连续帧数：累计 30 帧仍未推进则提前结束钩锁，防止人物贴墙被永久锁定。 */
    private int pullStuckTicks;
    private int slashTicks;
    /** 巨人激光当前射线长度（update 期间探测，draw 直接使用）。 */
    private double laserRayLength = 900;

    /** 重置所有 Boss 特殊攻击状态（新关卡开始时调用）。 */
    public void reset() {
        boss = null;
        specialCooldown = 0;
        chargeWindupTicks = 0;
        chargeTicks = 0;
        jumpTicks = 0;
        laserWindupTicks = 0;
        laserTicks = 0;
        slashPhase = -1;
        slashPhaseTicks = 0;
        slimeSlamLeft = 0;
        slimeRestTicks = 0;
        fireballLeft = 0;
        hookPhase = 0;
        pullStuckTicks = 0;
        slashTicks = 0;
        wizardCastTicks = 0;
        meteors.clear();
        rifts.clear();
        fireballs.clear();
        smallFireballs.clear();
    }

    /** 巨人 Boss（草原）不可移动：LevelController 跳过其移动逻辑。 */
    public boolean isImmobile() {
        return boss != null && boss.getAppearanceTheme() == com.example.smallgame.model.entity.MapTheme.GRASSLAND;
    }

    /**
     * 更新 Boss 专属特殊攻击；返回 true 表示特殊攻击正在接管 Boss（此帧 Boss 不移动、不弹幕）。
     */
    public boolean update(Monster bossRef, Player player, DungeonMap map,
                          CombatController combat, List<Monster> monsters) {
        if (bossRef == null || bossRef.getType() != MonsterType.BOSS) {
            return false;
        }
        this.boss = bossRef;
        // 特殊实体独立于 Boss 状态持续更新
        updateMeteors(player);
        updateRifts(player);
        updateFireballs(player, map);
        updateSmallFireballs(player, map);
        if (specialCooldown > 0) {
            specialCooldown--;
        }
        switch (boss.getAppearanceTheme()) {
            case FOREST: return updateTreeSanta(player, map);
            case TUNDRA: return updateSnowYeti(player, map, combat);
            case GRASSLAND: return updateGrassGiant(player, map);
            case DESERT: return false; // 黄金毒蝎：无特殊攻击（高频高移速由 LevelController 加速）
            case CASTLE: return updateKnight(player, map);
            case DARK_FOREST: return updateWizard(player);
            case SWAMP: return updateSlimeKing(player, map, combat);
            case SPACE: return updateVoidLord(player, map);
            case VOLCANO: return updateFireDragon(player);
            case ISLAND: return updateIronPirate(player, map);
            default: return false;
        }
    }

    /** 圣诞树人：冷却结束后先前摇蓄力（站定不动），随后锁定方向朝玩家持续冲刺，撞到玩家造成伤害。 */
    private boolean updateTreeSanta(Player player, DungeonMap map) {
        if (chargeWindupTicks > 0) {
            chargeWindupTicks--;
            boss.setAttackFlashTicks(Monster.ATTACK_FLASH_TICKS);
            if (chargeWindupTicks <= 0) {
                chargeAngle = Math.atan2(player.getPy() - boss.getPy(), player.getPx() - boss.getPx());
                chargeTicks = TREE_CHARGE_TICKS;
            }
            return true;
        }
        if (chargeTicks > 0) {
            double stepX = Math.cos(chargeAngle) * TREE_CHARGE_SPEED;
            double stepY = Math.sin(chargeAngle) * TREE_CHARGE_SPEED;
            if (!map.collidesWithWall(boss.getPx() + stepX, boss.getPy() + stepY, boss.getRadius())) {
                boss.setPx(boss.getPx() + stepX);
                boss.setPy(boss.getPy() + stepY);
            }
            boss.setAttackFlashTicks(Monster.ATTACK_FLASH_TICKS);
            boss.setAttackAngle(chargeAngle);
            if (player.isAlive()
                    && distance(boss.getPx(), boss.getPy(), player.getPx(), player.getPy())
                    < boss.getRadius() + Player.RADIUS + 8) {
                player.takeDamage(8);
            }
            chargeTicks--;
            if (chargeTicks <= 0) {
                specialCooldown = TREE_CHARGE_COOLDOWN;
                return false;
            }
            return true;
        }
        if (specialCooldown <= 0 && player.isAlive()) {
            chargeWindupTicks = TREE_CHARGE_WINDUP_TICKS;
            return true;
        }
        return false;
    }

    /** 大雪怪：抛物线跳跃砸向玩家附近（跳得高、距离远），落地瞬间砸击伤害 + 三层环形子弹。 */
    private boolean updateSnowYeti(Player player, DungeonMap map, CombatController combat) {
        if (jumpTicks > 0) {
            jumpProgress++;
            double t = jumpProgress / (double) YETI_JUMP_TICKS;
            if (t < 1) {
                double x = jumpStartX + (jumpTargetX - jumpStartX) * t;
                double y = jumpStartY + (jumpTargetY - jumpStartY) * t;
                double height = Math.sin(t * Math.PI) * 120;
                boss.setPx(x);
                boss.setPy(y - height);
                return true;
            }
            boss.setPx(jumpTargetX);
            boss.setPy(jumpTargetY);
            if (player.isAlive()
                    && distance(boss.getPx(), boss.getPy(), player.getPx(), player.getPy()) < 115) {
                player.takeDamage(9);
            }
            // 落地瞬间：三层错开角度的环形子弹
            for (int layer = 0; layer < 3; layer++) {
                for (int i = 0; i < 14; i++) {
                    double angle = layer * Math.PI / 21 + Math.PI * 2 * i / 14;
                    combat.spawnEnemyBulletAt(boss, angle);
                }
            }
            jumpTicks = 0;
            specialCooldown = YETI_JUMP_COOLDOWN;
            return false;
        }
        if (specialCooldown <= 0 && player.isAlive()) {
            jumpStartX = boss.getPx();
            jumpStartY = boss.getPy();
            jumpTargetX = player.getPx() + (random.nextDouble() - 0.5) * 40;
            jumpTargetY = player.getPy() + (random.nextDouble() - 0.5) * 40;
            if (map.collidesWithWall(jumpTargetX, jumpTargetY, 30)) {
                jumpTargetX = (boss.getPx() + player.getPx()) / 2;
                jumpTargetY = (boss.getPy() + player.getPy()) / 2;
            }
            jumpTicks = YETI_JUMP_TICKS;
            jumpProgress = 0;
            return true;
        }
        return false;
    }

    /** 巨人：不可移动，冷却结束后先眼部聚能前摇，随后锁定玩家方向发射两道持续巨型激光。 */
    private boolean updateGrassGiant(Player player, DungeonMap map) {
        if (laserWindupTicks > 0) {
            laserWindupTicks--;
            if (laserWindupTicks <= 0) {
                laserAngle = Math.atan2(player.getPy() - boss.getPy() - GIANT_EYE_OFFSET_Y,
                        player.getPx() - boss.getPx());
                laserTicks = GIANT_LASER_TICKS;
                laserHitTicks = 0;
            }
            return true;
        }
        if (laserTicks > 0) {
            laserTicks--;
            laserRayLength = laserLength(map, boss.getPx(), boss.getPy() + GIANT_EYE_OFFSET_Y);
            if (laserHitTicks > 0) {
                laserHitTicks--;
            }
            if (player.isAlive() && laserHitTicks <= 0) {
                laserHitTicks = GIANT_LASER_HIT_INTERVAL;
                if (hitByGiantLaser(player, map, -GIANT_EYE_OFFSET_X)
                        || hitByGiantLaser(player, map, GIANT_EYE_OFFSET_X)) {
                    player.takeDamage(5);
                }
            }
            if (laserTicks <= 0) {
                specialCooldown = GIANT_LASER_COOLDOWN;
                return false;
            }
            return true;
        }
        if (specialCooldown <= 0 && player.isAlive()) {
            laserWindupTicks = GIANT_LASER_WINDUP_TICKS;
            return true;
        }
        return false;
    }

    /** 巨人激光命中判定：玩家到激光射线（眼睛出发、撞墙截断）的距离小于阈值即命中。 */
    private boolean hitByGiantLaser(Player player, DungeonMap map, double eyeOffset) {
        double eyeX = boss.getPx() + eyeOffset;
        double eyeY = boss.getPy() + GIANT_EYE_OFFSET_Y;
        double dx = Math.cos(laserAngle);
        double dy = Math.sin(laserAngle);
        double len = laserLength(map, eyeX, eyeY);
        // 点到线段距离（玩家投影在激光区间内）
        double toPlayerX = player.getPx() - eyeX;
        double toPlayerY = player.getPy() - eyeY;
        double projection = toPlayerX * dx + toPlayerY * dy;
        if (projection < 0 || projection > len) {
            return false;
        }
        double perpendicular = Math.abs(toPlayerX * dy - toPlayerY * dx);
        return perpendicular < 13;
    }

    /** 激光射线长度：沿方向逐步探测，碰到墙体截断，最远 900px。 */
    private double laserLength(DungeonMap map, double originX, double originY) {
        double dx = Math.cos(laserAngle);
        double dy = Math.sin(laserAngle);
        double dist = 0;
        while (dist < 900) {
            dist += 12;
            if (map.collidesWithWall(originX + dx * dist, originY + dy * dist, 4)) {
                return dist;
            }
        }
        return 900;
    }

    /** 大骑士：三连斩，第一刀锁定方向后三刀均朝该固定方向挥砍，每刀向前跨一大步。 */
    private boolean updateKnight(Player player, DungeonMap map) {
        if (slashPhase >= 0) {
            slashPhaseTicks++;
            if (slashPhaseTicks == 1) {
                // 仅第一刀锁方向，后续两刀沿用（朝固定方向连续挥砍）
                if (slashPhase == 0) {
                    slashAngle = Math.atan2(player.getPy() - boss.getPy(), player.getPx() - boss.getPx());
                }
                double stepX = Math.cos(slashAngle) * KNIGHT_SLASH_STEP;
                double stepY = Math.sin(slashAngle) * KNIGHT_SLASH_STEP;
                if (!map.collidesWithWall(boss.getPx() + stepX, boss.getPy() + stepY, boss.getRadius())) {
                    boss.setPx(boss.getPx() + stepX);
                    boss.setPy(boss.getPy() + stepY);
                }
            }
            if (slashPhaseTicks == 6) {
                boss.setAttackFlashTicks(Monster.ATTACK_FLASH_TICKS);
                boss.setAttackAngle(slashAngle);
                double toPlayer = Math.atan2(player.getPy() - boss.getPy(), player.getPx() - boss.getPx());
                if (player.isAlive()
                        && distance(boss.getPx(), boss.getPy(), player.getPx(), player.getPy())
                        < boss.getRadius() + Player.RADIUS + 70
                        && Math.cos(toPlayer - slashAngle) > 0.2) {
                    player.takeDamage(9);
                }
            }
            if (slashPhaseTicks >= 20) {
                slashPhaseTicks = 0;
                slashPhase++;
                if (slashPhase >= 3) {
                    slashPhase = -1;
                    specialCooldown = KNIGHT_SLASH_COOLDOWN;
                    return false;
                }
            }
            return true;
        }
        if (specialCooldown <= 0 && player.isAlive()) {
            slashPhase = 0;
            slashPhaseTicks = 0;
            return true;
        }
        return false;
    }

    /** 大巫师：一次召唤三个陨石（一个正砸人物、两个随机偏移），红色预警后坠落爆炸；
     *  施法站定期间自身不动（不移动、不弹幕）。 */
    private boolean updateWizard(Player player) {
        if (wizardCastTicks > 0) {
            wizardCastTicks--;
            return true;
        }
        if (specialCooldown <= 0 && player.isAlive()) {
            for (int i = 0; i < 3; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double offset = i == 0 ? 0 : 50 + random.nextDouble() * 40;
                double targetX = player.getPx() + Math.cos(angle) * offset;
                double targetY = player.getPy() + Math.sin(angle) * offset;
                meteors.add(new Meteor(targetX, targetY));
            }
            wizardCastTicks = WIZARD_CAST_TICKS;
            specialCooldown = WIZARD_METEOR_COOLDOWN;
        }
        return false;
    }

    /** 陨石更新：预警倒计时 → 高空坠落 → 落地爆炸伤害。 */
    private void updateMeteors(Player player) {
        Iterator<Meteor> meteorIterator = meteors.iterator();
        while (meteorIterator.hasNext()) {
            Meteor meteor = meteorIterator.next();
            if (meteor.warnTicks > 0) {
                meteor.warnTicks--;
                continue;
            }
            if (meteor.boomTicks > 0) {
                meteor.boomTicks--;
                if (meteor.boomTicks <= 0) {
                    meteorIterator.remove();
                }
                continue;
            }
            meteor.fallTicks++;
            if (meteor.fallTicks >= METEOR_FALL_TICKS) {
                meteor.boomTicks = METEOR_BOOM_TICKS;
                if (player.isAlive()
                        && distance(meteor.x, meteor.y, player.getPx(), player.getPy())
                        < METEOR_HIT_RADIUS) {
                    player.takeDamage(10);
                }
            }
        }
    }

    /** 大史莱姆王：连续三次跳跃砸向人物（跳得高、砸得远），每次落地砸击伤害 + 一圈环形子弹。 */
    private boolean updateSlimeKing(Player player, DungeonMap map, CombatController combat) {
        if (jumpTicks > 0) {
            jumpProgress++;
            double t = jumpProgress / (double) SLIME_JUMP_TICKS;
            if (t < 1) {
                double x = jumpStartX + (jumpTargetX - jumpStartX) * t;
                double y = jumpStartY + (jumpTargetY - jumpStartY) * t;
                double height = Math.sin(t * Math.PI) * 105;
                boss.setPx(x);
                boss.setPy(y - height);
                return true;
            }
            boss.setPx(jumpTargetX);
            boss.setPy(jumpTargetY);
            if (player.isAlive()
                    && distance(boss.getPx(), boss.getPy(), player.getPx(), player.getPy()) < 95) {
                player.takeDamage(8);
            }
            for (int i = 0; i < 14; i++) {
                combat.spawnEnemyBulletAt(boss, Math.PI * 2 * i / 14);
            }
            jumpTicks = 0;
            slimeSlamLeft--;
            if (slimeSlamLeft > 0) {
                slimeRestTicks = 40; // 落地停顿后继续下一跳
            } else {
                specialCooldown = SLIME_SLAM_COOLDOWN;
                return false;
            }
            return true;
        }
        if (slimeRestTicks > 0) {
            slimeRestTicks--;
            if (slimeRestTicks <= 0) {
                jumpStartX = boss.getPx();
                jumpStartY = boss.getPy();
                jumpTargetX = player.getPx();
                jumpTargetY = player.getPy();
                if (map.collidesWithWall(jumpTargetX, jumpTargetY, 26)) {
                    jumpTargetX = (boss.getPx() + player.getPx()) / 2;
                    jumpTargetY = (boss.getPy() + player.getPy()) / 2;
                }
                jumpTicks = SLIME_JUMP_TICKS;
                jumpProgress = 0;
            }
            return true;
        }
        if (specialCooldown <= 0 && player.isAlive()) {
            slimeSlamLeft = SLIME_SLAM_COUNT;
            slimeRestTicks = 30;
            return true;
        }
        return false;
    }

    /** 虚空领主：持续在人物附近召唤虚空裂缝（场上最多 4 个），靠近持续受伤。 */
    private boolean updateVoidLord(Player player, DungeonMap map) {
        if (specialCooldown <= 0) {
            if (rifts.size() < VOID_MAX_RIFTS) {
                double angle = random.nextDouble() * Math.PI * 2;
                double dist = 90 + random.nextDouble() * 120;
                double x = player.getPx() + Math.cos(angle) * dist;
                double y = player.getPy() + Math.sin(angle) * dist;
                if (!map.collidesWithWall(x, y, 24)) {
                    rifts.add(new Rift(x, y));
                }
            }
            specialCooldown = VOID_RIFT_INTERVAL;
        }
        return false;
    }

    /** 裂缝更新：寿命倒计时；玩家靠近时按固定间隔持续受伤。 */
    private void updateRifts(Player player) {
        Iterator<Rift> riftIterator = rifts.iterator();
        while (riftIterator.hasNext()) {
            Rift rift = riftIterator.next();
            rift.life--;
            if (rift.life <= 0) {
                riftIterator.remove();
                continue;
            }
            if (rift.hitTicks > 0) {
                rift.hitTicks--;
            }
            if (player.isAlive() && rift.hitTicks <= 0
                    && distance(rift.x, rift.y, player.getPx(), player.getPy()) < 30) {
                player.takeDamage(4);
                rift.hitTicks = VOID_RIFT_HIT_INTERVAL;
            }
        }
    }

    /** 赤焰飞龙：连续发射五个大火球（高伤害）；碰墙后分裂成一圈小火球（低伤害）。 */
    private boolean updateFireDragon(Player player) {
        if (fireballLeft > 0) {
            fireballShotTicks++;
            if (fireballShotTicks >= DRAGON_SHOT_INTERVAL) {
                fireballShotTicks = 0;
                fireballLeft--;
                double angle = Math.atan2(player.getPy() - boss.getPy(), player.getPx() - boss.getPx());
                fireballs.add(new Fireball(boss.getPx(), boss.getPy(),
                        Math.cos(angle) * FIREBALL_SPEED, Math.sin(angle) * FIREBALL_SPEED, 10));
                boss.setAttackFlashTicks(Monster.ATTACK_FLASH_TICKS);
                boss.setAttackAngle(angle);
                if (fireballLeft <= 0) {
                    specialCooldown = DRAGON_FIREBALL_COOLDOWN;
                }
            }
        } else if (specialCooldown <= 0 && player.isAlive()) {
            fireballLeft = DRAGON_FIREBALL_COUNT;
            fireballShotTicks = DRAGON_SHOT_INTERVAL; // 立即发射第一颗
        }
        return false;
    }

    /** 大火球更新：碰墙分裂成 8 颗小火球；命中人物造成高伤害。 */
    private void updateFireballs(Player player, DungeonMap map) {
        Iterator<Fireball> fireballIterator = fireballs.iterator();
        while (fireballIterator.hasNext()) {
            Fireball fireball = fireballIterator.next();
            fireball.x += fireball.dx;
            fireball.y += fireball.dy;
            if (fireball.x < 0 || fireball.x > DungeonMap.WIDTH
                    || fireball.y < DungeonMap.WORLD_TOP || fireball.y > DungeonMap.HEIGHT) {
                fireballIterator.remove();
                continue;
            }
            if (map.collidesWithWall(fireball.x, fireball.y, 6)) {
                for (int i = 0; i < 8; i++) {
                    double angle = Math.PI * 2 * i / 8;
                    smallFireballs.add(new SmallFireball(fireball.x, fireball.y,
                            Math.cos(angle) * SMALL_FIREBALL_SPEED,
                            Math.sin(angle) * SMALL_FIREBALL_SPEED, 3));
                }
                fireballIterator.remove();
                continue;
            }
            if (player.isAlive()
                    && distance(fireball.x, fireball.y, player.getPx(), player.getPy())
                    < Player.RADIUS + 10) {
                player.takeDamage(fireball.damage);
                fireballIterator.remove();
            }
        }
    }

    /** 小火球更新：无限飞行，直到命中人物、碰撞墙体或飞出地图边界才消失。 */
    private void updateSmallFireballs(Player player, DungeonMap map) {
        Iterator<SmallFireball> fireballIterator = smallFireballs.iterator();
        while (fireballIterator.hasNext()) {
            SmallFireball fireball = fireballIterator.next();
            fireball.x += fireball.dx;
            fireball.y += fireball.dy;
            if (fireball.x < 0 || fireball.x > DungeonMap.WIDTH
                    || fireball.y < DungeonMap.WORLD_TOP || fireball.y > DungeonMap.HEIGHT
                    || map.collidesWithWall(fireball.x, fireball.y, 3)) {
                fireballIterator.remove();
                continue;
            }
            if (player.isAlive()
                    && distance(fireball.x, fireball.y, player.getPx(), player.getPy())
                    < Player.RADIUS + 6) {
                player.takeDamage(fireball.damage);
                fireballIterator.remove();
            }
        }
    }

    /** 钢铁海盗：钩锁瞄准人物直线射出，未命中缓慢收回；钩中后拉至身前强力斩击。 */
    private boolean updateIronPirate(Player player, DungeonMap map) {
        switch (hookPhase) {
            case 1: { // 瞄准：站定 0.5 秒，锁定人物方向
                boss.setAttackFlashTicks(Monster.ATTACK_FLASH_TICKS);
                hookAngle = Math.atan2(player.getPy() - boss.getPy(), player.getPx() - boss.getPx());
                boss.setAttackAngle(hookAngle);
                hookAimTicks--;
                if (hookAimTicks <= 0) {
                    hookPhase = 2;
                    hookX = boss.getPx();
                    hookY = boss.getPy();
                    hookTravel = 0;
                }
                return true;
            }
            case 2: { // 射出：直线飞行，钩中人物进入拉回，超距或碰墙则收回
                hookTravel += PIRATE_HOOK_SPEED;
                hookX = boss.getPx() + Math.cos(hookAngle) * hookTravel;
                hookY = boss.getPy() + Math.sin(hookAngle) * hookTravel;
                if (player.isAlive()
                        && distance(hookX, hookY, player.getPx(), player.getPy()) < Player.RADIUS + 6) {
                    hookPhase = 4;
                    pullStuckTicks = 0;
                    return true;
                }
                if (hookTravel > PIRATE_HOOK_MAX_RANGE || map.collidesWithWall(hookX, hookY, 4)) {
                    hookPhase = 3;
                }
                return false;
            }
            case 3: { // 未命中：缓慢收回
                hookTravel -= PIRATE_HOOK_RETRACT_SPEED;
                if (hookTravel <= 0) {
                    hookPhase = 0;
                    specialCooldown = PIRATE_HOOK_COOLDOWN;
                } else {
                    hookX = boss.getPx() + Math.cos(hookAngle) * hookTravel;
                    hookY = boss.getPy() + Math.sin(hookAngle) * hookTravel;
                }
                return false;
            }
            case 4: { // 钩中：把人物拉向 Boss（期间锁定人物移动，仍可攻击）
                if (!player.isAlive()) {
                    hookPhase = 0;
                    player.setHookedTicks(0);
                    specialCooldown = PIRATE_HOOK_COOLDOWN;
                    return false;
                }
                player.setHookedTicks(1);
                double pullAngle = Math.atan2(boss.getPy() - player.getPy(), boss.getPx() - player.getPx());
                double stepX = Math.cos(pullAngle) * PIRATE_PULL_SPEED;
                double stepY = Math.sin(pullAngle) * PIRATE_PULL_SPEED;
                if (!map.collidesWithWall(player.getPx() + stepX, player.getPy() + stepY, Player.RADIUS)) {
                    player.setPx(player.getPx() + stepX);
                    player.setPy(player.getPy() + stepY);
                    pullStuckTicks = 0;
                } else if (++pullStuckTicks >= 30) {
                    // 拉回被墙连续挡住 0.5 秒（人物贴墙无法抵达 Boss 身前）：提前结束钩锁，解锁人物移动
                    hookPhase = 0;
                    player.setHookedTicks(0);
                    specialCooldown = PIRATE_HOOK_COOLDOWN;
                    return false;
                }
                if (distance(boss.getPx(), boss.getPy(), player.getPx(), player.getPy()) < 55) {
                    hookPhase = 5;
                    slashAngle = Math.atan2(player.getPy() - boss.getPy(), player.getPx() - boss.getPx());
                    player.takeDamage(12); // 强力斩击巨额伤害
                    boss.setAttackFlashTicks(Monster.ATTACK_FLASH_TICKS);
                    boss.setAttackAngle(slashAngle);
                    slashTicks = 18;
                }
                return true;
            }
            case 5: { // 斩击硬直后复位
                slashTicks--;
                player.setHookedTicks(1);
                if (slashTicks <= 0) {
                    hookPhase = 0;
                    player.setHookedTicks(0);
                    specialCooldown = PIRATE_HOOK_COOLDOWN;
                }
                return true;
            }
            default:
                if (specialCooldown <= 0 && player.isAlive()) {
                    hookPhase = 1;
                    hookAimTicks = PIRATE_HOOK_AIM_TICKS;
                }
                return false;
        }
    }

    /** 获取当前 Boss 实体（无 Boss 时为 null）。 */
    public Monster getBoss() { return boss; }

    /** 获取圣诞树人冲刺前摇剩余帧数。 */
    public int getChargeWindupTicks() { return chargeWindupTicks; }

    /** 获取巨人眼部激光前摇剩余帧数。 */
    public int getLaserWindupTicks() { return laserWindupTicks; }

    /** 获取巨人眼部激光发射剩余帧数。 */
    public int getLaserTicks() { return laserTicks; }

    /** 获取巨人眼部激光发射角度（弧度）。 */
    public double getLaserAngle() { return laserAngle; }

    /** 获取巨人眼部激光射线长度（发射瞬间计算，延伸至墙体）。 */
    public double getLaserRayLength() { return laserRayLength; }

    /** 获取海盗钩锁当前阶段（0 空闲 / 1 瞄准 / 2 射出 / 3 收回 / 4 拉回 / 5 斩击）。 */
    public int getHookPhase() { return hookPhase; }

    /** 获取海盗钩锁瞄准剩余帧数。 */
    public int getHookAimTicks() { return hookAimTicks; }

    /** 获取海盗钩锁瞄准角度（弧度）。 */
    public double getHookAngle() { return hookAngle; }

    /** 获取海盗钩锁当前 X 坐标。 */
    public double getHookX() { return hookX; }

    /** 获取海盗钩锁当前 Y 坐标。 */
    public double getHookY() { return hookY; }

    /** 获取场上陨石列表（视图层读取状态绘制）。 */
    public List<Meteor> getMeteors() { return meteors; }

    /** 获取场上虚空裂缝列表。 */
    public List<Rift> getRifts() { return rifts; }

    /** 获取场上大火球列表。 */
    public List<Fireball> getFireballs() { return fireballs; }

    /** 获取场上小火球列表。 */
    public List<SmallFireball> getSmallFireballs() { return smallFireballs; }

    /** 两点间距离（各特殊攻击命中判定的通用工具）。 */
    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    /** 大巫师陨石：预警 → 坠落 → 爆炸（渲染由 BossEffectRenderer 读取状态完成）。 */
    public static final class Meteor {
        /** 坠落目标点坐标。 */
        public final double x;
        public final double y;
        /** 预警、坠落、爆炸剩余帧数。 */
        public int warnTicks = METEOR_WARN_TICKS;
        public int fallTicks;
        public int boomTicks;

        /** 构造陨石：记录坠落目标点，预警帧数取自常量。 */
        public Meteor(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    /** 虚空裂缝：位置、寿命与伤害节拍（渲染由 BossEffectRenderer 读取状态完成）。 */
    public static final class Rift {
        public final double x;
        public final double y;
        public int life = VOID_RIFT_LIFE;
        public int hitTicks;

        /** 构造裂缝：记录生成位置，寿命取自常量。 */
        public Rift(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    /** 赤焰飞龙大火球：高速飞行、碰墙分裂（渲染由 BossEffectRenderer 读取状态完成）。 */
    public static final class Fireball {
        public double x;
        public double y;
        public final double dx;
        public final double dy;
        public final int damage;

        /** 构造大火球：记录起点、逐帧位移向量与伤害值。 */
        public Fireball(double x, double y, double dx, double dy, int damage) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.damage = damage;
        }
    }

    /** 火球分裂的小火球：无限飞行、低伤害，直到命中人物或碰撞墙体才消失（渲染由 BossEffectRenderer 读取状态完成）。 */
    public static final class SmallFireball {
        public double x;
        public double y;
        public final double dx;
        public final double dy;
        public final int damage;

        /** 构造小火球：记录起点、逐帧位移向量与伤害值。 */
        public SmallFireball(double x, double y, double dx, double dy, int damage) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.damage = damage;
        }
    }
}
