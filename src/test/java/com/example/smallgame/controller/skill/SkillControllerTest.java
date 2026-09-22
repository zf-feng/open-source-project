package com.example.smallgame.controller.skill;

import com.example.smallgame.model.entity.DungeonMap;
import com.example.smallgame.model.entity.HeroType;
import com.example.smallgame.model.entity.Player;
import java.util.ArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 技能释放规则单元测试：验证骑士火力全开的增益结算、
 * 技能持续期间的释放封锁与结束后的冷却封锁、游侠闪避的方向要求。
 * <p>
 * 引用文件：controller.skill.SkillController（被测控制器）、model.entity.Player。
 */
class SkillControllerTest {

    /** 构建位于地图中央的骑士（火力全开：持续 360 帧、冷却 600 帧）。 */
    private static Player newKnight() {
        return new Player(640, 400, HeroType.KNIGHT);
    }

    /** 空地图（无墙无障碍），用于技能每帧更新。 */
    private static DungeonMap emptyMap() {
        return new DungeonMap(DungeonMap.WIDTH, DungeonMap.HEIGHT);
    }

    /**
     * 骑士技能释放结算：空格键触发后进入 360 帧持续状态，
     * 同时向玩家写入火力全开增益（360 帧）。
     */
    @Test
    @DisplayName("骑士技能释放写入火力全开增益")
    void knightSkillGrantsFirepower() {
        SkillController skill = new SkillController();
        Player player = newKnight();
        boolean cast = skill.tryCast(player, 0, 0, 800, 400,
                new ArrayList<>(), emptyMap(), enemy -> { });
        assertTrue(cast);
        assertEquals(360, player.getFirepowerTicks());
        assertEquals(360, skill.getActiveTicks());
    }

    /**
     * 冷却封锁规则：技能持续期间不能重复释放；
     * 360 帧持续结束后进入 600 帧冷却，冷却期间仍不能释放。
     */
    @Test
    @DisplayName("技能持续与冷却期间不能重复释放")
    void skillBlockedDuringCooldown() {
        SkillController skill = new SkillController();
        Player player = newKnight();
        skill.tryCast(player, 0, 0, 800, 400, new ArrayList<>(), emptyMap(), enemy -> { });
        // 持续期间：释放被拒绝
        assertFalse(skill.tryCast(player, 0, 0, 800, 400,
                new ArrayList<>(), emptyMap(), enemy -> { }));
        // 推进 360 帧：技能结束并进入冷却
        for (int i = 0; i < 360; i++) {
            skill.update(player, new ArrayList<>(), enemy -> { });
        }
        assertEquals(600, skill.getCooldownTicks());
        assertEquals(0, player.getFirepowerTicks());
        // 冷却期间：释放被拒绝
        assertFalse(skill.tryCast(player, 0, 0, 800, 400,
                new ArrayList<>(), emptyMap(), enemy -> { }));
    }

    /**
     * 游侠闪避方向规则：静止且鼠标指向自身时（方向向量为 0），
     * 闪避无法确定方向，技能释放被拒绝。
     */
    @Test
    @DisplayName("游侠静止且无方向时闪避释放失败")
    void rangerSkillRequiresDirection() {
        SkillController skill = new SkillController();
        Player player = new Player(640, 400, HeroType.RANGER);
        boolean cast = skill.tryCast(player, 0, 0,
                player.getPx(), player.getPy(), new ArrayList<>(), emptyMap(), enemy -> { });
        assertFalse(cast);
    }
}
