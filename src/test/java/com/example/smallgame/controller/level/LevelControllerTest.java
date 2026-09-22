package com.example.smallgame.controller.level;

import com.example.smallgame.model.entity.HeroType;
import com.example.smallgame.model.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 关卡回合推进单元测试：验证开局重置状态、敌人数量成长公式、
 * 关卡/世界递进规则、最终关通关判定与波次刷怪全流程。
 * <p>
 * 引用文件：controller.level.LevelController（被测控制器）、model.entity.Player。
 */
class LevelControllerTest {

    /**
     * 开局重置规则：reset 后回到 1-1，普通关敌人数量为
     * 4 + 2*世界 + 关卡 = 7，且关未完成（波次尚未刷完）。
     */
    @Test
    @DisplayName("reset 后回到 1-1 且关卡未完成")
    void resetStartsAtStageOneOne() {
        LevelController level = new LevelController();
        level.reset();
        assertEquals("1-1", level.levelName());
        assertEquals(7, level.enemiesForCurrentLevel());
        assertFalse(level.isBossStage());
        assertFalse(level.checkLevelComplete());
    }

    /**
     * 敌人数量成长公式：普通关按 4 + 2*世界 + 关卡 递增（1-1=7、2-1=9），
     * Boss 关固定为 1（仅 Boss 本身）。
     */
    @Test
    @DisplayName("敌人数量随关卡与 Boss 房规则变化")
    void enemyCountFormulaAcrossStages() {
        LevelController level = new LevelController();
        level.reset();
        assertEquals(7, level.enemiesForCurrentLevel());
        // 推进 4 关到达 1-5（Boss 房）
        for (int i = 0; i < 4; i++) {
            level.advanceLevel();
        }
        assertEquals("1-5", level.levelName());
        assertTrue(level.isBossStage());
        assertEquals(1, level.enemiesForCurrentLevel());
        // 跨世界进入 2-1：敌人数量按公式成长
        level.advanceLevel();
        assertEquals("2-1", level.levelName());
        assertFalse(level.isBossStage());
        assertEquals(9, level.enemiesForCurrentLevel());
    }

    /**
     * 回合推进规则：从 1-1 连续推进 14 次到达最终关 3-5，
     * 此时通关最终关返回 true（游戏结束）。
     */
    @Test
    @DisplayName("推进到 3-5 后通关返回 true")
    void finalStageEndsGame() {
        LevelController level = new LevelController();
        level.reset();
        for (int i = 0; i < 14; i++) {
            assertFalse(level.advanceLevel(), "第 " + (i + 1) + " 次推进不应通关");
        }
        assertEquals("3-5", level.levelName());
        assertTrue(level.isFinalStage());
        // 从最终关通关：返回 true
        assertTrue(level.advanceLevel());
    }

    /**
     * 波次刷怪全流程：开局冷却结束 → 出生预警 → 敌人生成，
     * 击杀清场后依次推进全部波次，最终判定关卡完成。
     */
    @Test
    @DisplayName("波次刷怪推进至关卡完成")
    void spawnEnemiesProgressesWaves() {
        LevelController level = new LevelController();
        level.reset();
        Player player = new Player(640, 400, HeroType.KNIGHT);
        boolean spawnedOnce = false;
        // 模拟每逻辑帧：推进刷怪冷却、执行刷怪流程、击杀全部怪物并移除
        for (int i = 0; i < 400; i++) {
            level.decrementSpawnCooldown();
            level.spawnEnemies(player, level.getCurrentMap());
            spawnedOnce |= !level.getMonsters().isEmpty();
            // 模拟战斗结算：击杀全部怪物（血量归零）后从列表移除
            level.getMonsters().forEach(monster -> monster.setHp(0));
            level.getMonsters().removeIf(monster -> !monster.isAlive());
            if (level.checkLevelComplete()) {
                break;
            }
        }
        assertTrue(spawnedOnce, "流程中应至少生成过一波敌人");
        assertTrue(level.checkLevelComplete());
    }
}
