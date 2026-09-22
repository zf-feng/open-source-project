package com.example.smallgame.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Player 实体单元测试：验证受击结算规则（无敌帧、护盾防溢出、血刀诅咒）、
 * 护盾资源回复与武器槽规则（拾取替换、切换）。
 * <p>
 * 引用文件：model.entity.Player、model.entity.HeroType、model.entity.WeaponType。
 */
class PlayerTest {

    /** 构建位于空地图中心、手持初始武器的骑士（生命 7 / 护盾 6 / 能量 200）。 */
    private static Player newKnight() {
        return new Player(640, 400, HeroType.KNIGHT);
    }

    /** 空地图（无墙无障碍），用于推进玩家每帧更新。 */
    private static DungeonMap emptyMap() {
        return new DungeonMap(DungeonMap.WIDTH, DungeonMap.HEIGHT);
    }

    /** 空转 N 帧：推进玩家无敌帧、冷却与护盾回复计时器（不移动）。 */
    private static void tickPlayer(Player player, DungeonMap map, int frames) {
        for (int i = 0; i < frames; i++) {
            player.update(false, false, false, false, map);
        }
    }

    /**
     * 受击后进入 18 帧无敌：无敌期间再次受击直接免疫，护盾不再扣减；
     * 无敌结束后恢复可受伤。
     */
    @Test
    @DisplayName("无敌帧期间伤害被免疫")
    void invincibilityFramesBlockRepeatedDamage() {
        Player player = newKnight();
        DungeonMap map = emptyMap();
        player.takeDamage(1);
        assertEquals(5, player.getShield());
        // 无敌帧内：伤害免疫，护盾不变
        player.takeDamage(1);
        assertEquals(5, player.getShield());
        // 空转 18 帧度过无敌期后再受击：护盾恢复扣减
        tickPlayer(player, map, 18);
        player.takeDamage(1);
        assertEquals(4, player.getShield());
    }

    /**
     * 护盾防溢出规则：单次伤害最多清空全部护盾，溢出部分作废，
     * 本击不扣生命值（下一击才开始扣血）。
     */
    @Test
    @DisplayName("护盾吸收过量伤害且溢出作废")
    void shieldAbsorbsOverkillDamageWithoutHpLoss() {
        Player player = newKnight();
        player.takeDamage(100);
        assertEquals(0, player.getShield());
        assertEquals(7, player.getHp());
    }

    /**
     * 护盾耗尽后伤害直接扣除生命值，且无敌结束后才会结算下一击。
     */
    @Test
    @DisplayName("护盾耗尽后伤害扣除生命值")
    void damageHitsHpOnlyWhenShieldEmpty() {
        Player player = newKnight();
        DungeonMap map = emptyMap();
        player.takeDamage(100);
        tickPlayer(player, map, 18);
        player.takeDamage(3);
        assertEquals(4, player.getHp());
    }

    /**
     * 血刀诅咒规则：手持血刀时受到的伤害翻倍。
     */
    @Test
    @DisplayName("手持血刀时伤害翻倍")
    void bloodBladeDoublesIncomingDamage() {
        Player player = newKnight();
        player.pickupWeapon(WeaponType.BLOOD_BLADE);
        assertEquals(WeaponType.BLOOD_BLADE, player.getWeapon());
        // 原始伤害 2 翻倍为 4：护盾 6 → 2
        player.takeDamage(2);
        assertEquals(2, player.getShield());
    }

    /**
     * 护盾资源回复规则：无论是否脱战，每 120 帧回复 1 点护盾，且不超过上限。
     */
    @Test
    @DisplayName("护盾每 120 帧回复 1 点且不超过上限")
    void shieldRegensOnePointPer120Frames() {
        Player player = newKnight();
        DungeonMap map = emptyMap();
        player.setShield(0);
        tickPlayer(player, map, 120);
        assertEquals(1, player.getShield());
        // 继续空转：回复累计但不会越过最大护盾 6
        tickPlayer(player, map, 6 * 120);
        assertEquals(6, player.getShield());
    }

    /**
     * 拾取武器槽位规则：当前槽为空则放入；仅另一槽为空则放入并切换过去；
     * 两槽均满则替换当前槽并返回被丢下的武器。
     */
    @Test
    @DisplayName("拾取武器按槽位规则放入/切换/替换")
    void pickupWeaponSlotRules() {
        Player player = newKnight();
        // 初始只有槽 0 的手枪
        assertSame(WeaponType.PISTOL, player.getWeapon());
        // 槽 1 为空：放入并自动切换过去，无武器被替换
        assertNull(player.pickupWeapon(WeaponType.FROST_SWORD));
        assertSame(WeaponType.FROST_SWORD, player.getWeapon());
        assertSame(WeaponType.PISTOL, player.getOffhandWeapon());
        // 两槽均满：替换当前槽（冰霜剑），返回被丢下的武器
        assertSame(WeaponType.FROST_SWORD, player.pickupWeapon(WeaponType.BOW));
        assertSame(WeaponType.BOW, player.getWeapon());
        // 切换回另一把武器
        player.cycleWeapon();
        assertSame(WeaponType.PISTOL, player.getWeapon());
    }

    /**
     * 副手槽为空时按 Q 切换武器不生效（保持当前武器不变）。
     */
    @Test
    @DisplayName("副手槽为空时切换武器无效")
    void cycleWeaponWithEmptyOffhandDoesNothing() {
        Player player = newKnight();
        player.cycleWeapon();
        assertSame(WeaponType.PISTOL, player.getWeapon());
    }

    /**
     * 瞬身闪避免疫规则：闪避期间（dodgeTicks > 0）受到任何伤害均直接免疫。
     */
    @Test
    @DisplayName("瞬身闪避期间伤害被免疫")
    void dodgeTicksGrantDamageImmunity() {
        Player player = newKnight();
        player.setDodgeTicks(24);
        player.takeDamage(5);
        assertEquals(6, player.getShield());
        assertEquals(7, player.getHp());
    }

    /**
     * 血刀诅咒在护盾耗尽后的结算：伤害翻倍后直接扣除生命值。
     */
    @Test
    @DisplayName("血刀诅咒翻倍伤害在护盾耗尽后直接扣血")
    void bloodBladeDoubledDamageHitsHpWhenShieldEmpty() {
        Player player = newKnight();
        player.pickupWeapon(WeaponType.BLOOD_BLADE);
        player.setShield(0);
        // 原始伤害 3 翻倍为 6：生命 7 → 1
        player.takeDamage(3);
        assertEquals(1, player.getHp());
    }
}
