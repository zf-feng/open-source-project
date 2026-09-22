package com.example.smallgame.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Character 基类单元测试：验证伤害结算核心规则——
 * 负伤害过滤、生命值钳制与存活判定。
 * <p>
 * 引用文件：model.entity.Character（被测基类）。
 */
class CharacterTest {

    /**
     * 参数化验证：负伤害与零伤害均不改变生命值（takeDamage 内部先做 Math.max(0, damage) 过滤）。
     *
     * @param damage 传入的伤害值（-10、-1、0 均应被过滤为 0）
     */
    @ParameterizedTest(name = "伤害 {0} 被过滤为 0，不扣血")
    @ValueSource(ints = {-10, -1, 0})
    @DisplayName("负伤害与零伤害被过滤，不改变生命值")
    void takeDamageNegativeDamageIgnored(int damage) {
        Character character = new Character(10);
        int hp = character.takeDamage(damage);
        assertEquals(10, hp);
    }

    /**
     * 正常伤害按数值扣减生命值，且结算结果向下钳制到 0（不会出现负血）。
     */
    @Test
    @DisplayName("正常伤害扣血，过量伤害钳制到 0")
    void takeDamageReducesHpAndClampsToZero() {
        Character character = new Character(10);
        assertEquals(6, character.takeDamage(4));
        assertEquals(0, character.takeDamage(100));
    }

    /**
     * 参数化验证：setHp 将生命值钳制在 [0, maxHp] 区间。
     */
    @ParameterizedTest(name = "setHp({0}) 钳制为 {1}")
    @CsvSource({"15, 10", "-3, 0", "7, 7"})
    @DisplayName("setHp 钳制到 [0, maxHp] 区间")
    void setHpClampedToRange(int input, int expected) {
        Character character = new Character(10);
        character.setHp(input);
        assertEquals(expected, character.getHp());
    }

    /**
     * 生命值大于 0 即存活，归零后判定死亡。
     */
    @Test
    @DisplayName("存活判定随生命值变化")
    void isAliveReflectsHpState() {
        Character character = new Character(5);
        assertTrue(character.isAlive());
        character.takeDamage(4);
        assertTrue(character.isAlive());
        character.takeDamage(1);
        assertFalse(character.isAlive());
    }
}
