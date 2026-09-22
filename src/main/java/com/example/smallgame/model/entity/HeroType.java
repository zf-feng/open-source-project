package com.example.smallgame.model.entity;

/**
 * 可选英雄职业枚举：骑士 / 游侠 / 精灵。
 * 每个职业对应一套官方立绘帧资源目录与初始数值（生命值 / 护盾 / 能量）。
 * <p>
 * 被 CharacterSelectView（角色选择展示）、Player（初始属性构建）、
 * SkillController（英雄技能分派）、PlayerRenderer（立绘帧加载）引用，立绘目录由 SpriteSheet 加载。
 */
public enum HeroType {
    KNIGHT("骑士", "/sprites/knight", WeaponType.PISTOL, 7, 6, 200),
    RANGER("游侠", "/sprites/ranger", WeaponType.DUAL_BLADES, 6, 4, 300),
    ELF("精灵", "/sprites/elf", WeaponType.BOW, 6, 5, 250);

    /** 界面显示名 */
    private final String displayName;
    /** 立绘帧资源目录（classpath 相对路径） */
    private final String spriteFolder;
    /** 初始武器 */
    private final WeaponType initialWeapon;
    /** 初始数值：生命值、护盾（护甲）、能量。 */
    private final int maxHp;
    private final int maxShield;
    private final int maxEnergy;

    /**
     * 枚举常量构造。
     *
     * @param displayName   界面显示名
     * @param spriteFolder  立绘帧资源目录
     * @param initialWeapon 初始武器类型
     * @param maxHp         初始最大生命值
     * @param maxShield     初始最大护盾
     * @param maxEnergy     初始最大能量
     */
    HeroType(String displayName, String spriteFolder, WeaponType initialWeapon,
             int maxHp, int maxShield, int maxEnergy) {
        this.displayName = displayName;
        this.spriteFolder = spriteFolder;
        this.initialWeapon = initialWeapon;
        this.maxHp = maxHp;
        this.maxShield = maxShield;
        this.maxEnergy = maxEnergy;
    }

    /** 获取界面显示名 */
    public String getDisplayName() {
        return displayName;
    }

    /** 获取立绘帧资源目录 */
    public String getSpriteFolder() {
        return spriteFolder;
    }

    /** 获取初始武器类型 */
    public WeaponType getInitialWeapon() {
        return initialWeapon;
    }

    /** 获取初始最大生命值 */
    public int getMaxHp() {
        return maxHp;
    }

    /** 获取初始最大护盾 */
    public int getMaxShield() {
        return maxShield;
    }

    /** 获取初始最大能量 */
    public int getMaxEnergy() {
        return maxEnergy;
    }
}
