package com.example.smallgame.controller.combat;

import com.example.smallgame.model.entity.HeroType;
import com.example.smallgame.model.entity.MapTheme;
import com.example.smallgame.model.entity.Monster;
import com.example.smallgame.model.entity.MonsterType;
import com.example.smallgame.model.entity.Player;
import com.example.smallgame.model.entity.WeaponType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 近战结算单元测试：验证剑类挥砍的命中判定与击杀移除、
 * 血刀吸血结算、以及近身自动切换手刀的规则。
 * <p>
 * 引用文件：controller.combat.CombatController（被测控制器）、
 * model.entity.Player、model.entity.Monster。
 */
class CombatControllerMeleeTest {

    /** 构建位于地图中央的骑士。 */
    private static Player newKnight() {
        return new Player(640, 400, HeroType.KNIGHT);
    }

    /**
     * 剑类挥砍结算：前方半圆范围内敌人受到 85 点冰霜剑伤害，
     * 生命归零触发击杀回调并从敌人列表移除；挥砍进入冷却并触发动画计时。
     */
    @Test
    @DisplayName("挥砍命中并击杀范围内的怪物")
    void swordSwingDamagesAndRemovesKilledMonster() {
        CombatController combat = new CombatController();
        Player player = newKnight();
        player.pickupWeapon(WeaponType.FROST_SWORD);
        // 敌人在玩家正右方 50 处（挥砍范围 90 内）
        Monster enemy = new Monster(player.getPx() + 50, player.getPy(),
                1, MonsterType.NORMAL, MapTheme.DEFAULT);
        enemy.setHp(50);
        List<Monster> enemies = new ArrayList<>();
        enemies.add(enemy);
        AtomicInteger killed = new AtomicInteger();
        combat.swingSword(player, player.getPx() + 100, player.getPy(), enemies,
                e -> killed.incrementAndGet());
        assertEquals(1, killed.get());
        assertTrue(enemies.isEmpty());
        assertEquals(12, player.getSlashTimer());
        assertEquals(30, player.getMeleeCooldown());
    }

    /**
     * 血刀吸血结算：血量不满时挥砍消耗 50 能量回复 1 点生命，
     * 且回复后不超过最大生命值。
     */
    @Test
    @DisplayName("血刀挥砍消耗能量回血")
    void bloodBladeHealsAtEnergyCost() {
        CombatController combat = new CombatController();
        Player player = newKnight();
        player.pickupWeapon(WeaponType.BLOOD_BLADE);
        player.setHp(3);
        combat.swingSword(player, 800, 400, new ArrayList<>(), enemy -> { });
        assertEquals(4, player.getHp());
        assertEquals(150, player.getEnergy());
    }

    /**
     * 手刀切换规则：敌人进入手刀范围（72）后，枪类武器的点按攻击
     * 被手刀接管（伤害 11、不消耗能量、不生成子弹）。
     */
    @Test
    @DisplayName("敌人近身时点按攻击切换为手刀")
    void handKnifeOverridesGunWhenEnemyClose() {
        CombatController combat = new CombatController();
        Player player = newKnight();
        // 敌人在玩家正右方 50 处（手刀伤害范围 60 内）
        Monster enemy = new Monster(player.getPx() + 50, player.getPy(),
                1, MonsterType.NORMAL, MapTheme.DEFAULT);
        List<Monster> enemies = new ArrayList<>();
        enemies.add(enemy);
        combat.attackPressed(player, 800, 400, enemies, e -> { });
        // 第 1 波普通怪 34 血：手刀一刀 11 伤害
        assertEquals(23, enemy.getHp());
        assertEquals(Player.HAND_KNIFE_SWING_TICKS, player.getHandKnifeTimer());
        // 原武器未射击：无子弹生成、能量不变
        assertTrue(combat.getBullets().isEmpty());
        assertEquals(200, player.getEnergy());
    }
}
