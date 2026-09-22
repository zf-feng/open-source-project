package com.example.smallgame.model.entity;

/**
 * 掉落物类型枚举，定义战斗中可掉落的四类奖励。
 * <p>
 * 被 Reward 实体与 RewardController（掉落生成与拾取逻辑）引用，
 * 不同类型的掉落物在拾取时产生不同效果：生命包回血、能量恢复能量、武器替换装备、金币累加货币。
 */
public enum RewardType {
    /** 生命包：恢复生命值 */
    HEALTH_PACK,
    /** 能量：恢复能量值 */
    ENERGY,
    /** 武器：更换当前武器 */
    WEAPON,
    /** 金币：累加金币数量 */
    GOLD
}
