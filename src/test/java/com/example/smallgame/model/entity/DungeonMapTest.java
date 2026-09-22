package com.example.smallgame.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DungeonMap 地图单元测试：验证圆与墙体/边界碰撞判定、
 * 矩形重叠判定与墙体命中检测。
 * <p>
 * 引用文件：model.entity.DungeonMap（含内部类 Wall）。
 */
class DungeonMapTest {

    /**
     * 参数化验证：空地图中圆形区域越过外围留边边界即判定碰撞，
     * 位于地图中央安全区域则不碰撞。
     */
    @ParameterizedTest(name = "圆 ({0}, {1}) 半径 {2} 边界碰撞 = {3}")
    @CsvSource({
            "20, 400, 10, true",
            "40, 400, 10, true",
            "1260, 400, 10, true",
            "640, 68, 10, true",
            "640, 745, 10, true",
            "640, 400, 10, false"
    })
    @DisplayName("圆与地图边界的碰撞判定")
    void collidesWithWallAtBorders(double x, double y, double radius, boolean expected) {
        DungeonMap map = new DungeonMap(DungeonMap.WIDTH, DungeonMap.HEIGHT);
        boolean actual = map.collidesWithWall(x, y, radius);
        String message = "圆 (" + x + ", " + y + ") 边界判定不符";
        if (expected) {
            assertTrue(actual, message);
        } else {
            assertFalse(actual, message);
        }
    }

    /**
     * 参数化验证：两个轴对齐矩形的重叠判定——
     * 分离不重叠、相交重叠、边恰好相接不重叠、完全包含重叠。
     */
    @ParameterizedTest(name = "{0},{1},{2},{3} 与 {4},{5},{6},{7} 重叠 = {8}")
    @CsvSource({
            "0, 0, 10, 10, 20, 20, 10, 10, false",
            "0, 0, 10, 10, 9, 9, 10, 10, true",
            "0, 0, 10, 10, 10, 0, 10, 10, false",
            "0, 0, 100, 100, 10, 10, 5, 5, true"
    })
    @DisplayName("轴对齐矩形重叠判定")
    void rectanglesOverlapRules(int ax, int ay, int aw, int ah,
                                int bx, int by, int bw, int bh, boolean expected) {
        boolean actual = DungeonMap.rectanglesOverlap(ax, ay, aw, ah, bx, by, bw, bh);
        if (expected) {
            assertTrue(actual);
        } else {
            assertFalse(actual);
        }
    }

    /**
     * 墙体命中检测：圆心落入墙体内（或距墙小于半径）判定碰撞，远离墙体则安全。
     */
    @Test
    @DisplayName("圆形区域与墙体的碰撞判定")
    void wallBlocksCircle() {
        DungeonMap map = new DungeonMap(DungeonMap.WIDTH, DungeonMap.HEIGHT);
        map.getWalls().add(new DungeonMap.Wall(400, 400, 100, 100, 0));
        // 圆心在墙体内：距离最近点 0 < 半径 3
        assertTrue(map.collidesWithWall(450, 450, 3));
        // 圆心贴近墙边缘：距离最近点 5 > 半径 3
        assertFalse(map.collidesWithWall(450, 395, 3));
        // 远离墙体的安全区域
        assertFalse(map.collidesWithWall(200, 200, 10));
    }
}
