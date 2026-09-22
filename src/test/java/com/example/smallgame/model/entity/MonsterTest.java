package com.example.smallgame.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Monster 实体单元测试：验证按类型与波次的数值成长公式
 * （生命值、攻击力、召唤冷却）与巨人 Boss 的命中半径规则。
 * <p>
 * 引用文件：model.entity.Monster、model.entity.MonsterType、model.entity.MapTheme。
 */
class MonsterTest {

    /**
     * 参数化验证：怪物最大生命值按类型与波次线性成长。
     * 普通怪 24 + 10*wave，精英 105 + 12*wave，Boss 1400 + 60*wave。
     */
    @ParameterizedTest(name = "{0} 第 {1} 波生命值 = {2}")
    @CsvSource({
            "NORMAL, 1, 34",
            "NORMAL, 3, 54",
            "ELITE, 1, 117",
            "ELITE, 2, 129",
            "BOSS, 1, 1460",
            "BOSS, 5, 1700"
    })
    @DisplayName("怪物生命值按类型与波次成长")
    void healthScalesByKindAndWave(MonsterType kind, int wave, int expectedHp) {
        Monster monster = new Monster(100, 100, wave, kind, MapTheme.DEFAULT);
        assertEquals(expectedHp, monster.getMaxHp());
    }

    /**
     * 攻击力与召唤冷却规则：Boss 攻击力 2 且可召唤小怪（冷却 300 帧），
     * 普通怪攻击力 1 且无召唤能力（冷却 -1 禁用）。
     */
    @Test
    @DisplayName("Boss 攻击力与召唤冷却区别于普通怪")
    void bossAttackAndSummonCooldownRules() {
        Monster boss = new Monster(100, 100, 1, MonsterType.BOSS, MapTheme.DEFAULT);
        assertEquals(2, boss.getAttack());
        assertEquals(300, boss.getSummonCooldown());
        Monster normal = new Monster(100, 100, 1, MonsterType.NORMAL, MapTheme.DEFAULT);
        assertEquals(1, normal.getAttack());
        assertEquals(-1, normal.getSummonCooldown());
    }

    /**
     * 命中半径规则：草原主题的巨人 Boss 身躯庞大，远程命中半径 175，
     * 远大于其余主题 Boss 的碰撞半径。
     */
    @Test
    @DisplayName("草原巨人 Boss 拥有超大命中半径")
    void grasslandBossHasLargeHitRadius() {
        Monster giant = new Monster(100, 100, 1, MonsterType.BOSS, MapTheme.GRASSLAND);
        assertEquals(175, giant.getHitRadius());
        Monster desertBoss = new Monster(100, 100, 1, MonsterType.BOSS, MapTheme.DESERT);
        assertEquals(desertBoss.getRadius(), desertBoss.getHitRadius());
    }
}
