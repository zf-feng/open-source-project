package com.example.smallgame.model.entity;

/**
 * 怪物类型枚举，将怪物划分为普通怪、精英怪与 Boss 三个等级。
 * <p>
 * 被 Monster 实体与 LevelController（刷怪配置）、BossController（Boss 战斗）引用，
 * 用于区分不同等级怪物的属性倍率与生成规则。
 */
public enum MonsterType {
    /** 普通怪 */
    NORMAL,
    /** 精英怪 */
    ELITE,
    /** Boss */
    BOSS
}
