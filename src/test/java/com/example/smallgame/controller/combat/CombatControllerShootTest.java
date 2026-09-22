package com.example.smallgame.controller.combat;

import com.example.smallgame.model.entity.HeroType;
import com.example.smallgame.model.entity.Player;
import java.util.ArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 射击结算单元测试：验证枪类武器的能量消耗、射击冷却与子弹生成规则，
 * 以及能量不足、冷却中、火力全开三种特殊状态的结算分支。
 * <p>
 * 引用文件：controller.combat.CombatController（被测控制器）、model.entity.Player。
 */
class CombatControllerShootTest {

    /** 构建位于地图中央的骑士（手枪 2 能量/发、冷却 15 帧、伤害 11）。 */
    private static Player newKnight() {
        return new Player(640, 400, HeroType.KNIGHT);
    }

    /**
     * 正常射击结算：消耗 2 点能量、生成一颗子弹并进入 15 帧射击冷却。
     */
    @Test
    @DisplayName("射击消耗能量、生成子弹并进入冷却")
    void pistolShotConsumesEnergySetsCooldown() {
        CombatController combat = new CombatController();
        Player player = newKnight();
        combat.shoot(player, 800, 400, new ArrayList<>(), enemy -> { });
        assertEquals(198, player.getEnergy());
        assertEquals(15, player.getFireCooldown());
        assertEquals(1, combat.getBullets().size());
    }

    /**
     * 冷却结算规则：射击冷却未结束时再次射击被忽略，不生成子弹也不扣能量。
     */
    @Test
    @DisplayName("冷却期间射击被忽略")
    void shotBlockedDuringCooldown() {
        CombatController combat = new CombatController();
        Player player = newKnight();
        combat.shoot(player, 800, 400, new ArrayList<>(), enemy -> { });
        combat.shoot(player, 800, 400, new ArrayList<>(), enemy -> { });
        assertEquals(1, combat.getBullets().size());
        assertEquals(198, player.getEnergy());
    }

    /**
     * 资源不足结算规则：能量低于单发消耗（2 点）时射击被拒绝。
     */
    @Test
    @DisplayName("能量不足时射击被拒绝")
    void shotBlockedWhenEnergyInsufficient() {
        CombatController combat = new CombatController();
        Player player = newKnight();
        player.setEnergy(1);
        combat.shoot(player, 800, 400, new ArrayList<>(), enemy -> { });
        assertTrue(combat.getBullets().isEmpty());
        assertEquals(1, player.getEnergy());
    }

    /**
     * 火力全开结算规则：增益期间射击不消耗能量，冷却缩短为一半。
     */
    @Test
    @DisplayName("火力全开射击免费且冷却减半")
    void firepowerShotFreeAndHalfCooldown() {
        CombatController combat = new CombatController();
        Player player = newKnight();
        player.setFirepowerTicks(60);
        combat.shoot(player, 800, 400, new ArrayList<>(), enemy -> { });
        assertEquals(200, player.getEnergy());
        assertEquals(7, player.getFireCooldown());
        assertEquals(1, combat.getBullets().size());
    }

    /**
     * 火力全开边界规则：能量不足以支付单发消耗时，
     * 增益期间仍可免费射击（能量封锁判定被火力全开绕过）。
     */
    @Test
    @DisplayName("火力全开期间能量不足也可免费射击")
    void firepowerShotWorksEvenWhenEnergyInsufficient() {
        CombatController combat = new CombatController();
        Player player = newKnight();
        player.setEnergy(1);
        player.setFirepowerTicks(60);
        combat.shoot(player, 800, 400, new ArrayList<>(), enemy -> { });
        assertEquals(1, combat.getBullets().size());
        assertEquals(1, player.getEnergy());
        assertEquals(7, player.getFireCooldown());
    }
}
