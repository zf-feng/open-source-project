package com.example.smallgame.model.entity;

/**
 * 全部武器类型枚举：
 * 初始武器（英雄自带）：手枪（骑士）、弓（精灵）、双刀（游侠）；
 * 1-5 Boss 可获取：冰霜剑、复合弓、黄金沙漠之鹰、猎人弓、漫游左轮；
 * 2-5 Boss 可获取：巨弓、咖喱棒、雪人之鹰；
 * 3-5 Boss 可获取任意武器；
 * 隐藏武器：血刀（仅普通关宝箱极低概率掉落，Boss 箱不掉）。
 * <p>
 * 被 Player（武器持有）、CombatController（武器攻击逻辑）、WeaponDrop（地面武器展示）、
 * RewardController / LevelController（宝箱掉落配置）引用，
 * 立绘资源位于 resources/sprites/weapons 下对应目录，由 SpriteSheet 加载。
 */
public enum WeaponType {
    PISTOL("破旧的手枪", "pistol"),
    BOW("精灵之弓", "bow"),
    DUAL_BLADES("小明小红", "blade0"),
    FROST_SWORD("冰霜剑", "frost_sword"),
    COMPOUND_BOW("复合弓", "compound_bow"),
    GOLDEN_DEAGLE("黄金沙漠之鹰", "golden_deagle"),
    HUNTER_BOW("猎人弓", "hunter_bow"),
    REVOLVER("漫游左轮", "revolver"),
    GIANT_BOW("巨弓", "giant_bow"),
    EXCALIBUR("咖喱棒", "excalibur"),
    SNOWMAN_EAGLE("雪人之鹰", "snowman_eagle"),
    BLOOD_BLADE("血刀", "blood_blade");

    /** 界面显示名 */
    private final String label;
    /** 立绘资源目录名（不含 /sprites/weapons 前缀） */
    private final String folderName;

    /**
     * 枚举常量构造。
     *
     * @param label      界面显示名
     * @param folderName 立绘资源目录名
     */
    WeaponType(String label, String folderName) {
        this.label = label;
        this.folderName = folderName;
    }

    /** 获取界面显示名 */
    public String getLabel() {
        return label;
    }

    /** 立绘资源目录（classpath 相对路径，如 /sprites/weapons/frost_sword）。 */
    public String getSpriteFolder() {
        return "/sprites/weapons/" + folderName;
    }

    /** 是否为弓类（长按蓄力、松手射箭）。 */
    public boolean isBow() {
        return this == BOW || this == COMPOUND_BOW || this == HUNTER_BOW || this == GIANT_BOW;
    }

    /** 是否为枪类（点按/按住连发）。 */
    public boolean isGun() {
        return this == PISTOL || this == GOLDEN_DEAGLE || this == REVOLVER || this == SNOWMAN_EAGLE;
    }

    /** 是否为近战剑类（半圆挥砍）。 */
    public boolean isSword() {
        return this == FROST_SWORD || this == EXCALIBUR || this == BLOOD_BLADE;
    }
}
