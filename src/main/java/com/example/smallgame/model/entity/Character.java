package com.example.smallgame.model.entity;

/**
 * 角色实体基类，承载战斗单位共有的基础属性：生命值与攻击力，
 * 并提供伤害结算方法。
 * <p>
 * 被 Player 继承扩展（坐标、能量、护盾、武器等），Monster 继承扩展（类型、AI 状态等），
 * 供 controller.combat（战斗）、controller.level（关卡）等控制器操作。
 */
public class Character {
    private int hp;
    private int maxHp;
    private int attack;

    /**
     * 构造角色，初始生命值置为满血。
     *
     * @param maxHp 最大生命值
     */
    public Character(int maxHp) {
        this.maxHp = maxHp;
        this.hp = maxHp;
    }

    /** 获取攻击力 */
    public int getAttack() { return attack; }

    /** 设置攻击力 */
    public void setAttack(int attack) { this.attack = attack; }

    /** 获取当前生命值 */
    public int getHp() { return hp; }

    /**
     * 设置当前生命值，并钳制在 [0, maxHp] 区间内，防止越界。
     */
    public void setHp(int hp) { this.hp = Math.max(0, Math.min(maxHp, hp)); }

    /** 获取最大生命值 */
    public int getMaxHp() { return maxHp; }

    /**
     * 承受伤害：先用 Math.max(0, damage) 过滤负伤害，再扣减并钳制到 0。
     *
     * @return 结算后的剩余生命值
     */
    public int takeDamage(int damage) { setHp(hp - Math.max(0, damage)); return hp; }

    /** 生命值大于 0 即视为存活 */
    public boolean isAlive() { return hp > 0; }
}
