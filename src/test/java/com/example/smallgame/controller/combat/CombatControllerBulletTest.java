package com.example.smallgame.controller.combat;

import com.example.smallgame.model.entity.DungeonMap;
import com.example.smallgame.model.entity.HeroType;
import com.example.smallgame.model.entity.MapTheme;
import com.example.smallgame.model.entity.Monster;
import com.example.smallgame.model.entity.MonsterType;
import com.example.smallgame.model.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 飞行物与受击结算单元测试：验证子弹命中怪物的碰撞结算、
 * 贯穿箭伤害衰减、出界与插墙规则、怪物攻击结算与伤害过滤。
 * <p>
 * 引用文件：controller.combat.CombatController（被测控制器，含 Bullet 内部类）、
 * model.entity.Monster、model.entity.Player。
 */
class CombatControllerBulletTest {

    /** 构建位于地图中央的骑士（护盾 6）。 */
    private static Player newKnight() {
        return new Player(640, 400, HeroType.KNIGHT);
    }

    /** 空地图（无墙无障碍），用于飞行物推进。 */
    private static DungeonMap emptyMap() {
        return new DungeonMap(DungeonMap.WIDTH, DungeonMap.HEIGHT);
    }

    /**
     * 子弹碰撞结算：子弹飞行一帧后与怪物距离小于命中半径，
     * 扣除伤害后子弹消失（非贯穿弹只命中一个目标）。
     */
    @Test
    @DisplayName("子弹命中怪物扣除伤害并消失")
    void bulletHitsMonsterAndIsConsumed() {
        CombatController combat = new CombatController();
        Monster enemy = new Monster(115, 100, 1, MonsterType.NORMAL, MapTheme.DEFAULT);
        List<Monster> enemies = new ArrayList<>();
        enemies.add(enemy);
        combat.getBullets().add(new CombatController.Bullet(100, 100, 10, 0, 30, 0));
        combat.updateBullets(enemies, emptyMap(), e -> { });
        // 第 1 波普通怪 34 血：30 点伤害后剩 4
        assertEquals(4, enemy.getHp());
        assertTrue(combat.getBullets().isEmpty());
    }

    /**
     * 贯穿箭结算：满蓄力箭命中首个目标全额伤害、继续飞行，
     * 后续目标伤害降为 75%（60 → 45），两怪先后被击杀移除。
     */
    @Test
    @DisplayName("贯穿箭对后续目标伤害降为 75%")
    void pierceArrowDeals75PercentToSecondTarget() {
        CombatController combat = new CombatController();
        Monster first = new Monster(115, 100, 1, MonsterType.NORMAL, MapTheme.DEFAULT);
        Monster second = new Monster(135, 100, 1, MonsterType.NORMAL, MapTheme.DEFAULT);
        List<Monster> enemies = new ArrayList<>();
        enemies.add(first);
        enemies.add(second);
        AtomicInteger killed = new AtomicInteger();
        combat.getBullets().add(new CombatController.Bullet(100, 100, 10, 0, 60, 1, true));
        combat.updateBullets(enemies, emptyMap(), e -> killed.incrementAndGet());
        combat.updateBullets(enemies, emptyMap(), e -> killed.incrementAndGet());
        assertEquals(2, killed.get());
        assertTrue(enemies.isEmpty());
    }

    /**
     * 出界规则：子弹飞出地图边界后立即移除。
     */
    @Test
    @DisplayName("子弹飞出边界后移除")
    void bulletRemovedWhenOutOfBounds() {
        CombatController combat = new CombatController();
        combat.getBullets().add(new CombatController.Bullet(50, 100, -100, 0, 30, 0));
        combat.updateBullets(new ArrayList<>(), emptyMap(), e -> { });
        assertTrue(combat.getBullets().isEmpty());
    }

    /**
     * 箭插墙规则：箭撞墙后停止飞行并保留（插墙倒计时 600 帧），
     * 倒计时期间位置不变，归零后才消失。
     */
    @Test
    @DisplayName("箭撞墙后插墙保留并倒计时")
    void arrowSticksToWallWithCountdown() {
        CombatController combat = new CombatController();
        combat.getBullets().add(new CombatController.Bullet(40, 400, -10, 0, 30, 1));
        // 第一帧：箭飞入地图左侧留边区域（x=30），撞边界插墙
        combat.updateBullets(new ArrayList<>(), emptyMap(), e -> { });
        assertEquals(1, combat.getBullets().size());
        CombatController.Bullet arrow = combat.getBullets().get(0);
        assertEquals(600, arrow.stuckTicks);
        double stuckX = arrow.x;
        // 下一帧：停在原地倒计时
        combat.updateBullets(new ArrayList<>(), emptyMap(), e -> { });
        assertEquals(599, arrow.stuckTicks);
        assertEquals(stuckX, arrow.x);
    }

    /**
     * 受击结算规则：已死亡的怪物不再对玩家造成伤害。
     */
    @Test
    @DisplayName("死亡怪物的攻击不结算")
    void monsterAttackIgnoresDeadTargets() {
        CombatController combat = new CombatController();
        Player player = newKnight();
        Monster monster = new Monster(200, 200, 1, MonsterType.NORMAL, MapTheme.DEFAULT);
        monster.setHp(0);
        combat.handleMonsterAttack(monster, player);
        assertEquals(6, player.getShield());
    }

    /**
     * 伤害下限规则：怪物攻击力为 0 时结算伤害至少为 1。
     */
    @Test
    @DisplayName("怪物攻击伤害至少为 1")
    void monsterAttackMinimumDamageOne() {
        CombatController combat = new CombatController();
        Player player = newKnight();
        Monster monster = new Monster(200, 200, 1, MonsterType.NORMAL, MapTheme.DEFAULT);
        monster.setAttack(0);
        combat.handleMonsterAttack(monster, player);
        assertEquals(5, player.getShield());
    }

    /**
     * 参数化验证：applyDamage 将负伤害过滤为 0，目标状态不发生变化。
     */
    @ParameterizedTest(name = "applyDamage 伤害 {0} 被过滤为 0")
    @ValueSource(ints = {-10, -1, 0})
    @DisplayName("applyDamage 过滤负伤害")
    void applyDamageClampsNegativeToZero(int damage) {
        CombatController combat = new CombatController();
        Player player = newKnight();
        combat.applyDamage(player, damage);
        assertEquals(7, player.getHp());
        assertEquals(6, player.getShield());
    }
}
