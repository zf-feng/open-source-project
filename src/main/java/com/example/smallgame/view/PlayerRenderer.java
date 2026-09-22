package com.example.smallgame.view;

import com.example.smallgame.model.entity.HeroType;
import com.example.smallgame.model.entity.Player;
import com.example.smallgame.model.entity.WeaponType;
import com.example.smallgame.util.SpriteSheet;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import java.util.EnumMap;
import java.util.Map;

/**
 * 玩家渲染器：负责玩家立绘、武器与蓄力特效的绘制。
 * 全部立绘资源在本类静态缓存并按职业/武器类型取用，预加载由 preload 在
 * 游戏画布构造时触发；玩家实体本身不持有任何图像资源。
 * <p>
 * 引用文件：model.entity（Player、HeroType、WeaponType）、util.SpriteSheet（立绘加载与旋转绘制）。
 * 被 GameMainView（玩家与武器绘制）调用。
 */
public final class PlayerRenderer {

    /** 角色立绘显示尺寸（正方形边长）。 */
    private static final double DISPLAY_SIZE = 72;
    /** 手刀立绘尺寸：人物建模（DISPLAY_SIZE）的 1.25 倍，宽高按 GIF 素材画布比例换算。 */
    private static final double HAND_KNIFE_DISPLAY_HEIGHT = DISPLAY_SIZE * 1.25;
    private static final double HAND_KNIFE_DISPLAY_WIDTH = HAND_KNIFE_DISPLAY_HEIGHT * 150 / 192.0;
    /** 手刀立绘沿朝向的前移量（贴近角色身前）。 */
    private static final double HAND_KNIFE_FORWARD_OFFSET = 44;
    /** 角色立绘帧序列缓存（按职业）。 */
    private static final Map<HeroType, SpriteSheet> HERO_SPRITES = new EnumMap<>(HeroType.class);
    /** 武器立绘帧缓存（按武器类型）。 */
    private static final Map<WeaponType, Image> WEAPON_FRAMES = new EnumMap<>(WeaponType.class);
    /** 箭矢立绘帧。 */
    private static Image arrowFrame;
    /** 双刀第二把刀刃立绘帧。 */
    private static Image blade1Frame;
    /** 手刀挥击动画帧序列（挥砍-正.gif 逐帧提取，共 3 帧）。 */
    private static final Image[] HAND_KNIFE_FRAMES = new Image[3];

    /** 工具类，禁止实例化。 */
    private PlayerRenderer() {
    }

    /**
     * 预加载全部角色立绘、武器立绘与手刀挥砍帧序列，避免战斗中途加载造成卡顿。
     * 在游戏画布构造时调用一次，后续绘制直接命中静态缓存。
     */
    public static void preload() {
        for (HeroType hero : HeroType.values()) {
            HERO_SPRITES.put(hero, SpriteSheet.load(hero.getSpriteFolder()));
        }
        for (WeaponType type : WeaponType.values()) {
            WEAPON_FRAMES.put(type, SpriteSheet.load(type.getSpriteFolder()).frame(0));
        }
        arrowFrame = SpriteSheet.load("/sprites/weapons/arrow").frame(0);
        blade1Frame = SpriteSheet.load("/sprites/weapons/blade1").frame(0);
        SpriteSheet handKnifeSheet = SpriteSheet.load("/sprites/weapons/hand_knife");
        for (int i = 0; i < HAND_KNIFE_FRAMES.length; i++) {
            HAND_KNIFE_FRAMES[i] = handKnifeSheet.frame(i);
        }
    }

    /**
     * 绘制玩家：角色立绘朝向鼠标（cos 值为负时水平镜像），
     * 再按当前武器类型分发枪/弓/剑/双刀绘制，最后叠加咖喱棒蓄力进度条与手刀挥击动画。
     *
     * @param g      画布上下文
     * @param player 玩家实体（提供坐标、朝向与武器状态）
     * @param mouseX 鼠标 X 坐标（决定朝向）
     * @param mouseY 鼠标 Y 坐标（决定朝向）
     */
    public static void draw(GraphicsContext g, Player player, double mouseX, double mouseY) {
        double px = player.getPx();
        double py = player.getPy();
        double angle = Math.atan2(mouseY - py, mouseX - px);
        boolean flip = Math.cos(angle) < 0;
        int frameIndex = player.isMoving() ? (int) (player.getWalkCycle() / 3.0) : 0;
        setColor(g, Color.rgb(0, 0, 0, 80 / 255.0));
        g.fillRect((int) px - 18, (int) py + 24, 36, 5);
        HERO_SPRITES.get(player.getHeroType()).draw(g, frameIndex, px, py + 28, DISPLAY_SIZE, flip);

        WeaponType weapon = player.getWeapon();
        double drawY = py + Player.WEAPON_DRAW_OFFSET_Y;
        if (weapon.isGun()) {
            drawGun(g, player, weapon, angle, flip, px, drawY);
        } else if (weapon.isBow()) {
            drawBow(g, player, weapon, angle, flip, px, drawY, py);
        } else if (weapon.isSword()) {
            drawSword(g, player, weapon, angle, flip, px, drawY);
        } else {
            // 双刀（小明/小红交替）：80×40 保持素材 2:1 比例，前移量随尺寸等比缩小
            SpriteSheet.drawRotated(g, player.getNextBladeIndex() == 0
                            ? WEAPON_FRAMES.get(WeaponType.DUAL_BLADES) : blade1Frame,
                    px, drawY, 80, 40, 30, angle, 0, flip);
        }
        // 咖喱棒蓄力：头顶金色进度条（与弓类蓄力一致的视觉反馈）
        if (weapon == WeaponType.EXCALIBUR && player.getBowChargeTicks() > 0) {
            double ratio = Math.min(1.0, player.getBowChargeTicks() / (double) player.getChargeMax());
            int barWidth = 34;
            int barX = (int) px - barWidth / 2;
            int barY = (int) py - 56;
            setColor(g, Color.rgb(0, 0, 0, 160 / 255.0));
            g.fillRect(barX - 2, barY - 2, barWidth + 4, 9);
            setColor(g, Color.rgb(255, 224, 123));
            g.fillRect(barX, barY, (int) (barWidth * ratio), 5);
        }
        // 手刀挥击（近身自动触发）：按挥砍-正.gif 帧序列播放，立绘为人物建模 1.25 倍大小；
        // -45 度矫正使刀身位于角色身前（刀尖朝前），动画为高举→劈下→收尾的正面挥砍
        if (player.getHandKnifeTimer() > 0) {
            double progress = 1 - player.getHandKnifeTimer() / (double) Player.HAND_KNIFE_SWING_TICKS;
            int frame = Math.min(HAND_KNIFE_FRAMES.length - 1,
                    (int) (progress * HAND_KNIFE_FRAMES.length));
            SpriteSheet.drawRotated(g, HAND_KNIFE_FRAMES[frame], px, drawY, HAND_KNIFE_DISPLAY_WIDTH,
                    HAND_KNIFE_DISPLAY_HEIGHT, HAND_KNIFE_FORWARD_OFFSET, angle, -45, flip);
        }
    }

    /**
     * 枪类：按立绘比例绘制，开火后显示枪口火光。
     * 各枪型使用独立宽高还原素材比例，火光出现在冷却剩余较多（刚开火）的帧内。
     */
    private static void drawGun(GraphicsContext g, Player player, WeaponType weapon,
                                double angle, boolean flip, double px, double drawY) {
        double width;
        double height;
        switch (weapon) {
            case GOLDEN_DEAGLE:
                width = 36;
                height = 22;
                break;
            case REVOLVER:
                width = 42;
                height = 21;
                break;
            case SNOWMAN_EAGLE:
                width = 40;
                height = 21;
                break;
            default:
                width = 36;
                height = 25;
        }
        SpriteSheet.drawRotated(g, WEAPON_FRAMES.get(weapon), px, drawY, width, height,
                9, angle, 0, flip);
        if (player.getFireCooldown() > 11) {
            double muzzleX = px + Math.cos(angle) * 28;
            double muzzleY = drawY + Math.sin(angle) * 28;
            setColor(g, Color.rgb(255, 243, 143, 220 / 255.0));
            g.fillRect((int) muzzleX - 3, (int) muzzleY - 3, 6, 6);
        }
    }

    /**
     * 弓类：立绘带 45° 倾斜矫正；蓄力中显示搭箭与头顶进度条。
     * 巨弓使用更大的弓与箭尺寸，进度条宽度也相应加大。
     */
    private static void drawBow(GraphicsContext g, Player player, WeaponType weapon,
                                double angle, boolean flip, double px, double drawY, double py) {
        boolean giant = weapon == WeaponType.GIANT_BOW;
        double size = giant ? 52 : 40;
        double arrowSize = giant ? 34 : 28;
        SpriteSheet.drawRotated(g, WEAPON_FRAMES.get(weapon), px, drawY, size, size,
                2, angle, -45, flip);
        if (player.isBowCharging() || player.getBowChargeTicks() > 0) {
            SpriteSheet.drawRotated(g, arrowFrame, px, drawY, arrowSize, arrowSize,
                    8, angle, 0, flip);
        }
        if (player.getBowChargeTicks() > 0) {
            double ratio = Math.min(1.0, player.getBowChargeTicks() / (double) player.getChargeMax());
            int barWidth = giant ? 44 : 34;
            int barX = (int) px - barWidth / 2;
            int barY = (int) py - 56;
            setColor(g, Color.rgb(0, 0, 0, 160 / 255.0));
            g.fillRect(barX - 2, barY - 2, barWidth + 4, 9);
            setColor(g, Color.rgb(255, 224, 123));
            g.fillRect(barX, barY, (int) (barWidth * ratio), 5);
        }
    }

    /**
     * 剑类：静止时指向鼠标；挥砍时沿鼠标方向做半圆摆动；血刀更细长。
     * 摆动角按 sin 曲线随挥砍进度变化，形成起手-劈下-收势的弧线轨迹。
     */
    private static void drawSword(GraphicsContext g, Player player, WeaponType weapon,
                                  double angle, boolean flip, double px, double drawY) {
        double swing = 0;
        if (player.getSlashTimer() > 0) {
            double progress = 1 - player.getSlashTimer() / 12.0;
            swing = Math.sin(progress * Math.PI) * 1.15;
        }
        double width = weapon == WeaponType.BLOOD_BLADE ? 52 : 46;
        double height = weapon == WeaponType.BLOOD_BLADE ? 12 : 14;
        SpriteSheet.drawRotated(g, WEAPON_FRAMES.get(weapon), px, drawY, width, height,
                20, angle + swing, 0, flip);
    }

    /** 同时设置填充色与描边色，供统一调用。 */
    private static void setColor(GraphicsContext graphics, Paint paint) {
        graphics.setFill(paint);
        graphics.setStroke(paint);
    }
}
