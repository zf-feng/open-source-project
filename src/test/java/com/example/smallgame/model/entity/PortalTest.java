package com.example.smallgame.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Portal 传送门单元测试：验证生长动画进度推进与缓出曲线
 * （1-(1-t)^2：前期生长快、后期趋缓，满后保持全尺寸）。
 * <p>
 * 引用文件：model.entity.Portal（被测实体）。
 */
class PortalTest {

    /**
     * 生长进度推进规则：初始为 0；推进一半帧数时进度超过一半（缓出）；
     * 推满 36 帧后到达 1.0，之后保持全尺寸不再增长。
     */
    @Test
    @DisplayName("传送门生长动画缓出且满后保持")
    void growProgressEasesOutToFull() {
        Portal portal = new Portal(640, 400);
        assertEquals(0.0, portal.getGrowProgress());
        // 半程（18/36）：缓出曲线使进度 0.75，快于线性进度 0.5
        for (int i = 0; i < 18; i++) {
            portal.advanceAge();
        }
        assertEquals(0.75, portal.getGrowProgress(), 1e-9);
        // 全程（36 帧）：进度到达 1.0
        for (int i = 0; i < 18; i++) {
            portal.advanceAge();
        }
        assertEquals(1.0, portal.getGrowProgress(), 1e-9);
        // 继续推进：保持全尺寸
        portal.advanceAge();
        assertEquals(1.0, portal.getGrowProgress(), 1e-9);
    }
}
