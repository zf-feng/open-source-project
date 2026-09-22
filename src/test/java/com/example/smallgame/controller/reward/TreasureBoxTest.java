package com.example.smallgame.controller.reward;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 宝箱开启规则单元测试：验证生成保护帧数内不响应靠近、
 * 保护结束后靠近即开启、开启后不再重复触发。
 * <p>
 * 引用文件：controller.reward.TreasureBox（被测实体）。
 */
class TreasureBoxTest {

    /**
     * 生成保护规则：宝箱生成后 40 帧内即使玩家站在宝箱位置也不开启。
     */
    @Test
    @DisplayName("生成保护期内不响应靠近")
    void spawnProtectionDelaysOpening() {
        TreasureBox box = new TreasureBox(640, 400, false);
        // 玩家站在宝箱位置（距离 0）：保护期内 40 帧均不开启
        for (int i = 0; i < 40; i++) {
            assertFalse(box.update(640, 400));
        }
        assertFalse(box.isOpened());
        // 保护结束后第一帧靠近立即开启
        assertTrue(box.update(640, 400));
        assertTrue(box.isOpened());
    }

    /**
     * 开启距离规则：保护期结束后，玩家进入开启距离（45）内即开启；
     * 开启后宝箱消失，不再重复触发。
     */
    @Test
    @DisplayName("保护期后靠近开启且不重复触发")
    void opensWhenPlayerWithinDistance() {
        TreasureBox box = new TreasureBox(640, 400, true);
        // 度过保护期：40 帧
        for (int i = 0; i < 40; i++) {
            box.update(640, 400);
        }
        // 距离 100 超出开启范围：不开启
        assertFalse(box.update(740, 400));
        assertFalse(box.isOpened());
        // 距离 44 在开启范围内：开启
        assertTrue(box.update(684, 400));
        assertTrue(box.isOpened());
        // 已开启：后续帧不再返回 true
        assertFalse(box.update(640, 400));
    }
}
