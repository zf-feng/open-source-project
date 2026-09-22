package com.example.smallgame.controller.skill;

import com.example.smallgame.model.entity.DungeonMap;
import com.example.smallgame.model.entity.HeroType;
import com.example.smallgame.model.entity.Monster;
import com.example.smallgame.model.entity.Player;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * 技能控制器：空格键释放，按英雄类型分流三种技能。
 * 骑士-火力全开：武器攻速提升 200%、移速提升 50%、攻击不消耗能量、蓄力速度变为 2 倍，
 *   持续 6 秒后进入 10 秒冷却（增益状态写入 player.firepowerTicks，由战斗控制器查询生效）。
 * 精灵-灵箭之息：以释放瞬间最近敌人为中心生成固定圆形箭雨领域（不随敌人移动，
 *   场上无敌人时以自身为中心），从天而降大量箭矢，范围内敌人每 0.5 秒受到一次伤害，
 *   持续 4 秒后进入 8 秒冷却。
 * 游侠-瞬身闪避：朝移动方向（静止时朝鼠标方向）滑动闪避 2.8 个身位，无法穿墙（遇墙停在墙前），
 *   0.4 秒内从起点线性滑向终点形成过渡动画，期间无敌、无法移动与攻击并伴随明显的淡出淡入，
 *   结束后进入 0.5 秒冷却。
 * <p>
 * 引用文件：model.entity（Player、Monster、DungeonMap、HeroType）。
 * 被 GameMainView（技能释放与状态展示）、SkillEffectRenderer（特效绘制）调用。
 */
public final class SkillController {
    /** 火力全开：持续 6 秒（360 帧），结束后冷却 10 秒（600 帧）。 */
    private static final int FIRE_AT_WILL_DURATION = 360;
    private static final int FIRE_AT_WILL_COOLDOWN = 600;
    /** 灵箭之息：持续 4 秒（240 帧），每 0.5 秒（30 帧）结算一次伤害，结束后冷却 8 秒。 */
    private static final int ARROW_RAIN_DURATION = 240;
    private static final int ARROW_RAIN_TICK_INTERVAL = 30;
    /** 箭雨每跳伤害比例（按目标最大生命值的 5% 结算，至少 1 点）。 */
    private static final double ARROW_RAIN_PERCENT = 0.05;
    public static final double ARROW_RAIN_RADIUS = 110;
    private static final int ARROW_RAIN_COOLDOWN = 480;
    /** 瞬身闪避：持续 0.4 秒（24 帧，前 8 帧淡出、中 8 帧全透明、后 8 帧淡入），
     *  位移在全程内线性过渡，闪避距离 2.8 个身位（140），冷却 0.4 秒（24 帧）。 */
    private static final int DODGE_DURATION = 24;
    private static final double DODGE_DISTANCE = 140;
    private static final int DODGE_COOLDOWN = 24;

    private final Random random = new Random();
    /** 箭雨装饰性坠落箭（x, y, 倾摆角度）。 */
    private final List<double[]> fallingArrows = new ArrayList<>();
    /** 当前正在生效的技能（无技能时为 null）。 */
    private HeroType activeSkill;
    /** 技能剩余持续帧数。 */
    private int activeTicks;
    /** 技能冷却剩余帧数。 */
    private int cooldownTicks;
    /** 灵箭之息领域中心（生成瞬间确定，不随敌人移动）。 */
    private double rainX;
    private double rainY;
    /** 灵箭之息伤害结算倒计时。 */
    private int rainTickTimer;
    /** 瞬身闪避过渡动画的起点与终点（0.4 秒内线性滑动）。 */
    private double dashStartX;
    private double dashStartY;
    private double dashEndX;
    private double dashEndY;

    /** 技能名称（角色选择界面与 HUD 状态提示共用）。 */
    public static String skillNameFor(HeroType hero) {
        switch (hero) {
            case KNIGHT:
                return "火力全开";
            case ELF:
                return "灵箭之息";
            case RANGER:
                return "瞬身闪避";
            default:
                return "";
        }
    }

    /** 技能描述（角色选择界面面板按行显示）。 */
    public static String[] skillDescriptionFor(HeroType hero) {
        switch (hero) {
            case KNIGHT:
                return new String[]{
                        "释放后6秒内武器攻速",
                        "提升为200%，移速提",
                        "升50%，攻击不再消",
                        "耗能量，结束后冷却10秒"};
            case ELF:
                return new String[]{
                        "以最近敌人为中心降下",
                        "箭雨，持续4秒，范围",
                        "内敌人每0.5秒损失5%",
                        "最大生命值，结束冷却8秒"};
            case RANGER:
                return new String[]{
                        "朝移动方向（静止时朝",
                        "鼠标方向）瞬身闪避两",
                        "个身位，期间免疫伤害",
                        "且无法穿墙，持续0.4秒",
                        "结束后冷却0.4秒"};
            default:
                return new String[0];
        }
    }

    /** 清空技能状态（重新开始游戏 / 进入下一关时调用）。 */
    public void reset() {
        activeSkill = null;
        activeTicks = 0;
        cooldownTicks = 0;
        rainTickTimer = 0;
        fallingArrows.clear();
    }

    /** 进入下一关时调用：结束激活中的技能与增益，但冷却剩余时间继承上一关状态（不清零）。 */
    public void onLevelTransition(Player player) {
        activeSkill = null;
        activeTicks = 0;
        rainTickTimer = 0;
        fallingArrows.clear();
        player.setFirepowerTicks(0);
        player.setDodgeTicks(0);
    }

    /** 技能剩余持续帧数。 */
    public int getActiveTicks() {
        return activeTicks;
    }

    /** 技能冷却剩余帧数。 */
    public int getCooldownTicks() {
        return cooldownTicks;
    }

    /** 释放技能（空格键触发）。moveDx/moveDy 为当前移动方向（静止时均为 0，游侠改朝鼠标闪避）。 */
    public boolean tryCast(Player player, double moveDx, double moveDy,
                           double mouseX, double mouseY, List<Monster> monsters,
                           DungeonMap map, Consumer<Monster> onEnemyKilled) {
        if (activeTicks > 0 || cooldownTicks > 0) {
            return false;
        }
        switch (player.getHeroType()) {
            case KNIGHT:
                activeSkill = HeroType.KNIGHT;
                activeTicks = FIRE_AT_WILL_DURATION;
                player.setFirepowerTicks(FIRE_AT_WILL_DURATION);
                return true;
            case ELF:
                Monster target = nearestMonster(player, monsters);
                // 场上没有敌人时以自己为中心释放箭雨
                rainX = target == null ? player.getPx() : target.getPx();
                rainY = target == null ? player.getPy() : target.getPy();
                activeSkill = HeroType.ELF;
                activeTicks = ARROW_RAIN_DURATION;
                rainTickTimer = ARROW_RAIN_TICK_INTERVAL;
                fallingArrows.clear();
                return true;
            case RANGER:
                double dirX = moveDx;
                double dirY = moveDy;
                if (dirX == 0 && dirY == 0) {
                    // 静止状态：朝鼠标方向闪避
                    dirX = mouseX - player.getPx();
                    dirY = mouseY - player.getPy();
                }
                double length = Math.hypot(dirX, dirY);
                if (length < 0.001) {
                    return false;
                }
                // 确定滑动终点（遇墙停在墙前），位移在 0.4 秒内线性过渡
                double[] dashEnd = dashTarget(player, dirX / length, dirY / length, map);
                dashStartX = player.getPx();
                dashStartY = player.getPy();
                dashEndX = dashEnd[0];
                dashEndY = dashEnd[1];
                player.setDodgeTicks(DODGE_DURATION);
                activeSkill = HeroType.RANGER;
                activeTicks = DODGE_DURATION;
                return true;
            default:
                return false;
        }
    }

    /** 游侠是否正在闪避（闪避期间玩家无法移动与攻击，由视图层查询锁定）。 */
    public boolean isDodging() {
        return activeSkill == HeroType.RANGER && activeTicks > 0;
    }

    /** 每帧更新：递减冷却与持续计时，执行箭雨领域伤害、增益/无敌倒计时等持续效果。 */
    public void update(Player player, List<Monster> monsters, Consumer<Monster> onEnemyKilled) {
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
        if (activeTicks > 0) {
            activeTicks--;
            if (activeTicks == 0) {
                // 技能结束：进入对应英雄的冷却时间并清理增益状态
                cooldownTicks = cooldownFor(activeSkill);
                endSkill(player);
                activeSkill = null;
            }
        }
        switch (player.getHeroType()) {
            case KNIGHT:
                if (player.getFirepowerTicks() > 0) {
                    player.setFirepowerTicks(player.getFirepowerTicks() - 1);
                }
                break;
            case ELF:
                updateArrowRain(monsters, onEnemyKilled);
                break;
            case RANGER:
                if (player.getDodgeTicks() > 0) {
                    player.setDodgeTicks(player.getDodgeTicks() - 1);
                    updateDashMovement(player);
                }
                break;
            default:
                break;
        }
    }

    /** 玩家绘制透明度：瞬身闪避期间明显的三段式淡出淡入
     *  （前 1/3 淡出至全透明、中 1/3 保持全透明、后 1/3 淡入恢复）。 */
    public double playerAlpha() {
        if (activeSkill != HeroType.RANGER) {
            return 1.0;
        }
        int elapsed = DODGE_DURATION - activeTicks;
        int third = DODGE_DURATION / 3;
        if (elapsed < third) {
            // 淡出：逐渐透明至完全消失
            return 1.0 - elapsed / (double) third;
        }
        if (elapsed < third * 2) {
            // 中段：保持完全透明
            return 0.0;
        }
        // 淡入：从完全透明恢复可见
        return (elapsed - third * 2) / (double) (DODGE_DURATION - third * 2);
    }

    /** 查询灵箭之息是否正在生效（决定是否渲染领域特效）。 */
    public boolean isArrowRainActive() { return activeSkill == HeroType.ELF && activeTicks > 0; }

    /** 获取箭雨领域中心 X（生成瞬间确定，不随敌人移动）。 */
    public double getRainX() { return rainX; }

    /** 获取箭雨领域中心 Y。 */
    public double getRainY() { return rainY; }

    /** 获取箭雨装饰性坠落箭列表（x, y, 倾摆角度），由视图层绘制。 */
    public List<double[]> getFallingArrows() { return fallingArrows; }

    /** 箭雨领域：每 0.5 秒对范围内所有敌人结算一次伤害；期间持续生成下坠箭矢装饰。 */
    private void updateArrowRain(List<Monster> monsters, Consumer<Monster> onEnemyKilled) {
        if (activeSkill != HeroType.ELF || activeTicks <= 0) {
            return;
        }
        rainTickTimer--;
        if (rainTickTimer <= 0) {
            rainTickTimer = ARROW_RAIN_TICK_INTERVAL;
            damageRainZone(monsters, onEnemyKilled);
        }
        // 领域内随机位置生成 1~2 支下坠箭矢（纯视觉效果）
        int spawnCount = random.nextDouble() < 0.55 ? 2 : 1;
        for (int i = 0; i < spawnCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = random.nextDouble() * ARROW_RAIN_RADIUS;
            double tilt = (random.nextDouble() - 0.5) * 14;
            fallingArrows.add(new double[]{
                    rainX + Math.cos(angle) * radius,
                    DungeonMap.WORLD_TOP - 26 - random.nextDouble() * 30,
                    tilt});
        }
        Iterator<double[]> arrowIterator = fallingArrows.iterator();
        while (arrowIterator.hasNext()) {
            double[] arrow = arrowIterator.next();
            arrow[1] += 9 + random.nextDouble() * 3;
            if (arrow[1] >= rainY + 30) {
                arrowIterator.remove();
            }
        }
    }

    /** 对箭雨领域内所有敌人造成一次百分比伤害（最大生命值的 5%，至少 1 点），死亡的敌人由 onEnemyKilled 结算掉落并移除。 */
    private void damageRainZone(List<Monster> monsters, Consumer<Monster> onEnemyKilled) {
        Iterator<Monster> monsterIterator = monsters.iterator();
        while (monsterIterator.hasNext()) {
            Monster enemy = monsterIterator.next();
            if (distance(enemy.getPx(), enemy.getPy(), rainX, rainY)
                    <= ARROW_RAIN_RADIUS + enemy.getRadius()) {
                int percentDamage = Math.max(1, (int) (enemy.getMaxHp() * ARROW_RAIN_PERCENT));
                enemy.setHp(enemy.getHp() - percentDamage);
                if (enemy.getHp() <= 0) {
                    onEnemyKilled.accept(enemy);
                    monsterIterator.remove();
                }
            }
        }
    }

    /** 瞬身闪避终点：沿方向逐段推进至墙前（无法穿墙），返回终点坐标。
     *  每次推进 5，共推进 2.8 个身位的距离。 */
    private double[] dashTarget(Player player, double dirX, double dirY, DungeonMap map) {
        double x = player.getPx();
        double y = player.getPy();
        double step = 5;
        int steps = (int) Math.ceil(DODGE_DISTANCE / step);
        for (int i = 0; i < steps; i++) {
            double nextX = x + dirX * step;
            double nextY = y + dirY * step;
            if (map.collidesWithWall(nextX, nextY, Player.RADIUS)) {
                break;
            }
            x = nextX;
            y = nextY;
        }
        return new double[]{x, y};
    }

    /** 瞬身闪避过渡动画：0.4 秒内从起点线性滑向终点（配合淡出淡入，非瞬间瞬移）。 */
    private void updateDashMovement(Player player) {
        int elapsed = DODGE_DURATION - player.getDodgeTicks();
        double progress = Math.min(1.0, elapsed / (double) DODGE_DURATION);
        player.setPx(dashStartX + (dashEndX - dashStartX) * progress);
        player.setPy(dashStartY + (dashEndY - dashStartY) * progress);
    }

    /** 距玩家最近的敌人（灵箭之息领域中心）。 */
    private static Monster nearestMonster(Player player, List<Monster> monsters) {
        Monster nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (Monster enemy : monsters) {
            double distance = distance(player.getPx(), player.getPy(), enemy.getPx(), enemy.getPy());
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = enemy;
            }
        }
        return nearest;
    }

    /** 技能结束：清理写入玩家的增益状态（游侠精确落到滑动终点）。 */
    private void endSkill(Player player) {
        switch (player.getHeroType()) {
            case KNIGHT:
                player.setFirepowerTicks(0);
                break;
            case RANGER:
                player.setDodgeTicks(0);
                player.setPx(dashEndX);
                player.setPy(dashEndY);
                break;
            default:
                break;
        }
    }

    /** 各英雄技能的冷却时长。 */
    private static int cooldownFor(HeroType hero) {
        switch (hero) {
            case KNIGHT:
                return FIRE_AT_WILL_COOLDOWN;
            case ELF:
                return ARROW_RAIN_COOLDOWN;
            case RANGER:
                return DODGE_COOLDOWN;
            default:
                return 0;
        }
    }

    /** 计算两点间欧几里得距离 */
    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }
}
