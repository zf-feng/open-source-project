package com.example.smallgame.controller.reward;

import com.example.smallgame.model.entity.HeroType;
import com.example.smallgame.model.entity.MapTheme;
import com.example.smallgame.model.entity.Monster;
import com.example.smallgame.model.entity.MonsterType;
import com.example.smallgame.model.entity.Player;
import com.example.smallgame.model.entity.Reward;
import com.example.smallgame.model.entity.RewardType;
import com.example.smallgame.model.entity.WeaponDrop;
import com.example.smallgame.model.entity.WeaponType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 掉落与拾取结算单元测试：验证怪物击杀掉落的能量点数量区间、
 * 能量点/生命包的拾取效果与上限、拾取延迟与地面武器拾取规则。
 * <p>
 * 引用文件：controller.reward.RewardController（被测控制器）、
 * model.entity.Monster、model.entity.Player、model.entity.Reward。
 */
class RewardControllerTest {

    /** 构建位于地图中央的骑士（能量 200、生命 7）。 */
    private static Player newKnight() {
        return new Player(640, 400, HeroType.KNIGHT);
    }

    /**
     * 参数化验证：击杀掉落能量点数量按怪物类型决定——
     * 普通怪 1~2、精英 3~5、Boss 8~12（12% 概率的额外生命包不计入能量点）。
     */
    @ParameterizedTest(name = "{0} 掉落能量点 {1}~{2} 个")
    @CsvSource({"NORMAL, 1, 2", "ELITE, 3, 5", "BOSS, 8, 12"})
    @DisplayName("击杀掉落能量点数量按类型取区间")
    void dropLootCountByMonsterType(MonsterType kind, int minCount, int maxCount) {
        RewardController controller = new RewardController();
        Monster enemy = new Monster(500, 400, 1, kind, MapTheme.DEFAULT);
        controller.dropLoot(enemy);
        int energyCount = (int) controller.getPickups().stream()
                .filter(reward -> reward.getType() == RewardType.ENERGY)
                .count();
        assertTrue(energyCount >= minCount && energyCount <= maxCount,
                "能量点数量 " + energyCount + " 不在 [" + minCount + ", " + maxCount + "]");
    }

    /**
     * 能量点拾取结算：进入拾取范围后恢复 8 点能量并从场上移除，且不超过能量上限。
     */
    @Test
    @DisplayName("拾取能量点恢复 8 点能量且不超过上限")
    void energyPickupRestoresCappedEnergy() {
        RewardController controller = new RewardController();
        Player player = newKnight();
        player.setEnergy(100);
        controller.getPickups().add(
                new Reward(RewardType.ENERGY, player.getPx() + 10, player.getPy()));
        controller.updatePickups(player);
        assertEquals(108, player.getEnergy());
        assertTrue(controller.getPickups().isEmpty());
        // 接近上限时钳制到最大能量 200
        player.setEnergy(195);
        controller.getPickups().add(
                new Reward(RewardType.ENERGY, player.getPx() + 10, player.getPy()));
        controller.updatePickups(player);
        assertEquals(200, player.getEnergy());
    }

    /**
     * 生命包拾取结算：恢复 2 点生命且不超过生命上限。
     */
    @Test
    @DisplayName("拾取生命包恢复 2 点生命且不超过上限")
    void healthPackRestoresCappedHp() {
        RewardController controller = new RewardController();
        Player player = newKnight();
        player.setHp(3);
        controller.getPickups().add(
                new Reward(RewardType.HEALTH_PACK, player.getPx() + 10, player.getPy()));
        controller.updatePickups(player);
        assertEquals(5, player.getHp());
        assertTrue(controller.getPickups().isEmpty());
        // 接近上限时钳制到最大生命 7
        player.setHp(6);
        controller.getPickups().add(
                new Reward(RewardType.HEALTH_PACK, player.getPx() + 10, player.getPy()));
        controller.updatePickups(player);
        assertEquals(7, player.getHp());
    }

    /**
     * 拾取延迟规则：掉落瞬间的拾取延迟倒计时归零前不可拾取。
     * updatePickups 先递减延迟再判定拾取，故 delay=2 时第 1 帧不可拾取、第 2 帧拾取。
     */
    @Test
    @DisplayName("拾取延迟期间掉落物不可拾取")
    void pickupDelayBlocksImmediatePickup() {
        RewardController controller = new RewardController();
        Player player = newKnight();
        player.setEnergy(100);
        Reward pickup = new Reward(RewardType.ENERGY, player.getPx() + 10, player.getPy());
        pickup.setPickupDelayTicks(2);
        controller.getPickups().add(pickup);
        // 第 1 帧：延迟 2→1，仍不可拾取
        controller.updatePickups(player);
        assertEquals(100, player.getEnergy());
        assertEquals(1, controller.getPickups().size());
        // 第 2 帧：延迟 1→0，恢复可拾取
        controller.updatePickups(player);
        assertEquals(108, player.getEnergy());
        assertTrue(controller.getPickups().isEmpty());
    }

    /**
     * 地面武器拾取规则：E 键拾取最近的地面武器放入空槽；
     * 超过拾取距离（45）时不拾取。
     */
    @Test
    @DisplayName("按距离拾取地面武器")
    void tryPickupWeaponWithinRange() {
        RewardController controller = new RewardController();
        Player player = newKnight();
        controller.getWeaponDrops().add(
                new WeaponDrop(WeaponType.FROST_SWORD, player.getPx() + 10, player.getPy()));
        assertSame(WeaponType.FROST_SWORD, controller.tryPickupWeapon(player));
        assertSame(WeaponType.FROST_SWORD, player.getWeapon());
        assertTrue(controller.getWeaponDrops().isEmpty());
        // 距离 100 超出拾取范围：无法拾取
        controller.getWeaponDrops().add(
                new WeaponDrop(WeaponType.BOW, player.getPx() + 100, player.getPy()));
        assertNull(controller.tryPickupWeapon(player));
    }
}
