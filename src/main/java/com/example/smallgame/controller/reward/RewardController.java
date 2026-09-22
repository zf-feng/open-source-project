package com.example.smallgame.controller.reward;

import com.example.smallgame.model.entity.DungeonMap;
import com.example.smallgame.model.entity.Monster;
import com.example.smallgame.model.entity.MonsterType;
import com.example.smallgame.model.entity.Reward;
import com.example.smallgame.model.entity.RewardType;
import com.example.smallgame.model.entity.Player;
import com.example.smallgame.model.entity.WeaponDrop;
import com.example.smallgame.model.entity.WeaponType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 掉落物控制器：负责怪物击杀掉落、宝箱生成与开启掉落、
 * 能量吸取与拾取结算、地面武器的拾取与替换等奖励系统逻辑。
 * <p>
 * 引用文件：model.entity（Reward、WeaponDrop、WeaponType、Monster、Player、DungeonMap）、
 * TreasureBox（宝箱实体）。
 * 被 LevelController（掉落触发）与 GameMainView（拾取输入与渲染数据源）调用。
 */
public final class RewardController {
    /** 能量点吸附范围：玩家进入此距离后能量点飞向玩家。 */
    private static final double ENERGY_PULL_RANGE = 135;
    private static final double PICKUP_RANGE = 25;
    /** 武器掉落物的拾取距离（按 E 键）。 */
    private static final double WEAPON_PICKUP_RANGE = 45;
    /** 普通关宝箱掉武器的概率。 */
    private static final double NORMAL_CHEST_WEAPON_CHANCE = 0.01;
    /** 宝箱开出血包的概率（与能量点互斥，能量点概率为其余部分）。 */
    private static final double CHEST_HEALTH_PACK_CHANCE = 0.10;
    /** 普通关宝箱开出隐藏武器血刀的独立概率（Boss 箱不会开出）。 */
    private static final double BLOOD_BLADE_CHANCE = 0.005;
    /** Boss 关宝箱相对传送门中心的垂直偏移（宝箱固定在传送门正下方）。 */
    private static final double CHEST_PORTAL_OFFSET_Y = 84;

    /** 1-5 Boss 可获取武器池。 */
    private static final WeaponType[] WORLD1_WEAPONS = {
            WeaponType.FROST_SWORD, WeaponType.COMPOUND_BOW, WeaponType.GOLDEN_DEAGLE,
            WeaponType.HUNTER_BOW, WeaponType.REVOLVER
    };
    /** 2-5 Boss 可获取武器池。 */
    private static final WeaponType[] WORLD2_WEAPONS = {
            WeaponType.GIANT_BOW, WeaponType.EXCALIBUR, WeaponType.SNOWMAN_EAGLE
    };
    /** 3-5 Boss 可获取任意武器。 */
    private static final WeaponType[] WORLD3_WEAPONS = {
            WeaponType.FROST_SWORD, WeaponType.COMPOUND_BOW, WeaponType.GOLDEN_DEAGLE,
            WeaponType.HUNTER_BOW, WeaponType.REVOLVER, WeaponType.GIANT_BOW,
            WeaponType.EXCALIBUR, WeaponType.SNOWMAN_EAGLE
    };

    private final Random random = new Random();
    private final List<Reward> pickups = new ArrayList<>();
    private final List<WeaponDrop> weaponDrops = new ArrayList<>();
    private TreasureBox treasureBox;
    private int world = 1;

    /** 获取掉落物列表 */
    public List<Reward> getPickups() { return pickups; }

    /** 获取当前宝箱 */
    public TreasureBox getTreasureBox() { return treasureBox; }

    /** 获取地面武器列表 */
    public List<WeaponDrop> getWeaponDrops() { return weaponDrops; }

    /** 重置掉落物、地面武器与宝箱（新游戏/新关卡时调用）。 */
    public void reset() {
        pickups.clear();
        weaponDrops.clear();
        treasureBox = null;
    }

    /**
     * 怪物死亡掉落：按类型决定能量点数量（Boss 8~12、精英 3~5、普通 1~2），
     * 能量点环绕死亡点随机散布，另有 12% 概率额外掉落一个生命包。
     *
     * @param enemy 被击杀的怪物
     */
    public void dropLoot(Monster enemy) {
        int energyCount;
        if (enemy.getType() == MonsterType.BOSS) {
            energyCount = 8 + random.nextInt(5);
        } else if (enemy.getType() == MonsterType.ELITE) {
            energyCount = 3 + random.nextInt(3);
        } else {
            energyCount = 1 + random.nextInt(2);
        }
        dropEnergyPoints(enemy, energyCount);
        if (random.nextDouble() < 0.12) {
            pickups.add(new Reward(RewardType.HEALTH_PACK, enemy.getPx(), enemy.getPy()));
        }
    }

    /**
     * Boss 半血掉落：血量首次降至一半时一次性触发，
     * 能量点数量与击杀 Boss 时一致（8~12），不含生命包。
     *
     * @param boss 半血的 Boss
     */
    public void dropHalfHpLoot(Monster boss) {
        dropEnergyPoints(boss, 8 + random.nextInt(5));
    }

    /** 按给定数量在目标周围随机散落能量点（击杀掉落与 Boss 半血掉落共用）。 */
    private void dropEnergyPoints(Monster enemy, int count) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 8 + random.nextDouble() * 18;
            pickups.add(new Reward(RewardType.ENERGY,
                    enemy.getPx() + Math.cos(angle) * distance,
                    enemy.getPy() + Math.sin(angle) * distance));
        }
    }

    /**
     * 每逻辑帧推进掉落物：拾取延迟与散落动画倒计时，
     * 能量点在吸附范围内朝玩家飞行（速度 8.5 快于玩家移速，可追上移动中的玩家），
     * 进入拾取范围后结算效果：生命包回血 2 点、能量点恢复 8 点能量。
     *
     * @param player 玩家实体
     */
    public void updatePickups(Player player) {
        for (Reward pickup : pickups) {
            if (pickup.getPickupDelayTicks() > 0) {
                pickup.setPickupDelayTicks(pickup.getPickupDelayTicks() - 1);
            }
            if (pickup.getScatterTicks() > 0) {
                pickup.setPx(pickup.getPx() + pickup.getVx());
                pickup.setPy(pickup.getPy() + pickup.getVy());
                pickup.setVx(pickup.getVx() * 0.82);
                pickup.setVy(pickup.getVy() * 0.82);
                pickup.setScatterTicks(pickup.getScatterTicks() - 1);
            }
        }
        Iterator<Reward> iterator = pickups.iterator();
        while (iterator.hasNext()) {
            Reward pickup = iterator.next();
            if (pickup.getPickupDelayTicks() > 0) {
                continue;
            }
            double pickupDistance = distance(pickup.getPx(), pickup.getPy(),
                    player.getPx(), player.getPy());
            if (pickup.getType() == RewardType.ENERGY && pickupDistance < ENERGY_PULL_RANGE
                    && pickupDistance > PICKUP_RANGE) {
                // 吸取速度需明显快于人物移速（4.5），能量点可追上移动中的玩家
                double pullSpeed = 8.5;
                pickup.setPx(pickup.getPx()
                        + (player.getPx() - pickup.getPx()) / pickupDistance * pullSpeed);
                pickup.setPy(pickup.getPy()
                        + (player.getPy() - pickup.getPy()) / pickupDistance * pullSpeed);
            }
            if (distance(pickup.getPx(), pickup.getPy(), player.getPx(), player.getPy()) < PICKUP_RANGE) {
                if (pickup.getType() == RewardType.HEALTH_PACK) {
                    player.setHp(Math.min(player.getMaxHp(), player.getHp() + 2));
                } else {
                    player.setEnergy(Math.min(player.getMaxEnergy(), player.getEnergy() + 8));
                }
                iterator.remove();
            }
        }
    }

    /** 生成宝箱：Boss 关固定在传送门正下方；普通关在随机空地生成（与传送门一同出现）。 */
    public void spawnTreasureBox(Player player, DungeonMap map, boolean golden, int world,
                                 double portalX, double portalY) {
        this.world = world;
        if (golden) {
            treasureBox = new TreasureBox(portalX, portalY + CHEST_PORTAL_OFFSET_Y, true);
            return;
        }
        for (int attempt = 0; attempt < 150; attempt++) {
            double x = 80 + random.nextDouble() * (DungeonMap.WIDTH - 160);
            double y = DungeonMap.WORLD_TOP + 70 + random.nextDouble()
                    * (DungeonMap.HEIGHT - DungeonMap.WORLD_TOP - 140);
            if (!map.collidesWithWall(x, y, 20)
                    && distance(x, y, player.getPx(), player.getPy()) > 160) {
                treasureBox = new TreasureBox(x, y, golden);
                return;
            }
        }
        // 兜底：在玩家周围找空地生成，保证玩家一定能发现宝箱
        for (int attempt = 0; attempt < 80; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double x = player.getPx() + Math.cos(angle) * 230;
            double y = player.getPy() + Math.sin(angle) * 230;
            if (!map.collidesWithWall(x, y, 20)) {
                treasureBox = new TreasureBox(x, y, golden);
                return;
            }
        }
        treasureBox = new TreasureBox(player.getPx() + 220, player.getPy(), golden);
    }

    /** 推进宝箱状态：玩家靠近即开启消失，开启瞬间散落掉落物。 */
    public void updateTreasureBox(Player player) {
        if (treasureBox == null) {
            return;
        }
        if (treasureBox.update(player.getPx(), player.getPy())) {
            scatterLoot(treasureBox);
        }
    }

    /** 宝箱掉落：能量点与血包互斥——10% 概率掉落 1 个血包，否则散落能量点；
     *  Boss 箱必掉武器，普通箱 1% 概率掉武器；
     *  普通箱另有极低概率开出隐藏武器血刀（Boss 箱不会开出）。 */
    private void scatterLoot(TreasureBox box) {
        if (random.nextDouble() < CHEST_HEALTH_PACK_CHANCE) {
            double angle = random.nextDouble() * Math.PI * 2;
            Reward health = new Reward(RewardType.HEALTH_PACK, box.getPx(), box.getPy() - 12);
            health.setScatter(Math.cos(angle) * 1.8, Math.sin(angle) * 1.8 - 0.6, 30);
            health.setPickupDelayTicks(30);
            pickups.add(health);
        } else {
            int energyCount = 14 + random.nextInt(6);
            for (int i = 0; i < energyCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = 1.6 + random.nextDouble() * 2.6;
                Reward pickup = new Reward(RewardType.ENERGY, box.getPx(), box.getPy() - 12);
                pickup.setScatter(Math.cos(angle) * speed, Math.sin(angle) * speed, 26);
                pickup.setPickupDelayTicks(30);
                pickups.add(pickup);
            }
        }
        if (box.isGolden() || random.nextDouble() < NORMAL_CHEST_WEAPON_CHANCE) {
            WeaponType[] pool = weaponPoolForWorld();
            WeaponType type = pool[random.nextInt(pool.length)];
            WeaponDrop drop = new WeaponDrop(type, box.getPx(), box.getPy() - 12);
            double angle = random.nextDouble() * Math.PI * 2;
            drop.setScatter(Math.cos(angle) * 1.6, Math.sin(angle) * 1.6 - 0.4, 24);
            weaponDrops.add(drop);
        }
        if (!box.isGolden() && random.nextDouble() < BLOOD_BLADE_CHANCE) {
            WeaponDrop drop = new WeaponDrop(WeaponType.BLOOD_BLADE,
                    box.getPx(), box.getPy() - 12);
            double angle = random.nextDouble() * Math.PI * 2;
            drop.setScatter(Math.cos(angle) * 1.6, Math.sin(angle) * 1.6 - 0.4, 24);
            weaponDrops.add(drop);
        }
    }

    /** 当前世界的武器池：世界 1 → 1-5 池，世界 2 → 2-5 池，世界 3 → 任意。 */
    private WeaponType[] weaponPoolForWorld() {
        if (world >= 3) {
            return WORLD3_WEAPONS;
        }
        if (world == 2) {
            return WORLD2_WEAPONS;
        }
        return WORLD1_WEAPONS;
    }

    /** 推进地面武器的散落动画（每逻辑帧调用）。 */
    public void updateWeaponDrops() {
        for (WeaponDrop drop : weaponDrops) {
            drop.update();
        }
    }

    /**
     * E 键拾取最近的地面武器；两槽已满时替换当前槽，被替换的武器丢到玩家脚下。
     * 返回拾取到的武器类型（无武器可拾取时返回 null）。
     */
    public WeaponType tryPickupWeapon(Player player) {
        WeaponDrop nearest = null;
        double best = WEAPON_PICKUP_RANGE;
        for (WeaponDrop drop : weaponDrops) {
            double d = distance(drop.getPx(), drop.getPy(), player.getPx(), player.getPy());
            if (d < best) {
                best = d;
                nearest = drop;
            }
        }
        if (nearest == null) {
            return null;
        }
        WeaponType picked = nearest.getType();
        WeaponType dropped = player.pickupWeapon(picked);
        weaponDrops.remove(nearest);
        if (dropped != null) {
            weaponDrops.add(new WeaponDrop(dropped, player.getPx(), player.getPy()));
        }
        return picked;
    }

    /** 计算两点间欧几里得距离 */
    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }
}