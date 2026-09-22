package com.example.smallgame.view;

import com.example.smallgame.controller.combat.CombatController;
import com.example.smallgame.util.SpriteSheet;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.List;

/**
 * 飞行物渲染器：负责战斗飞行物的视觉表现——玩家子弹/箭/刀/剑气与敌人紫红能量弹。
 * CombatController 只保存飞行物数据与命中结算逻辑，绘制全部集中在本类。
 * <p>
 * 引用文件：controller.combat.CombatController（子弹与敌弹数据）、util.SpriteSheet（立绘加载）。
 * 被 GameMainView（弹道图层）调用。
 */
public final class BulletRenderer {

    /** 枪弹类使用的立绘缓存（箭、刀、剑气）。 */
    private static final Image ARROW_SPRITE = SpriteSheet.load("/sprites/weapons/arrow").frame(0);
    private static final Image BLADE_0_SPRITE = SpriteSheet.load("/sprites/weapons/blade0").frame(0);
    private static final Image BLADE_1_SPRITE = SpriteSheet.load("/sprites/weapons/blade1").frame(0);
    private static final Image BEAM_SPRITE = SpriteSheet.load("/sprites/weapons/sword_beam").frame(0);

    /** 工具类，禁止实例化。 */
    private BulletRenderer() {
    }

    /** 预热立绘缓存（进入游戏时调用，避免首帧卡顿）。 */
    public static void preload() {
        SpriteSheet.load("/sprites/weapons/arrow");
        SpriteSheet.load("/sprites/weapons/blade0");
        SpriteSheet.load("/sprites/weapons/blade1");
        SpriteSheet.load("/sprites/weapons/sword_beam");
    }

    /** 绘制全部敌人子弹。 */
    public static void drawEnemyBullets(GraphicsContext g, List<CombatController.EnemyBullet> bullets) {
        for (CombatController.EnemyBullet bullet : bullets) {
            drawEnemyBullet(g, bullet);
        }
    }

    /**
     * 绘制玩家飞行物：枪弹类绘制双层光弹（按 kind 配色），
     * 箭/刀/剑气分别用对应立绘按飞行角度旋转绘制。
     *
     * @param g      画布上下文
     * @param bullet 飞行物数据
     */
    public static void drawBullet(GraphicsContext g, CombatController.Bullet bullet) {
        if (bullet.kind == 0 || bullet.kind == 5 || bullet.kind == 6) {
            Color outer = bullet.kind == 5
                    ? Color.rgb(255, 208, 84, 80 / 255.0)
                    : bullet.kind == 6 ? Color.rgb(140, 210, 255, 80 / 255.0)
                    : Color.rgb(255, 240, 148, 80 / 255.0);
            Color inner = bullet.kind == 5
                    ? Color.rgb(255, 230, 130)
                    : bullet.kind == 6 ? Color.rgb(205, 240, 255)
                    : Color.rgb(255, 246, 190);
            setColor(g, outer);
            g.fillRect((int) bullet.x - 7, (int) bullet.y - 3, 14, 6);
            setColor(g, inner);
            g.fillRect((int) bullet.x - 3, (int) bullet.y - 2, 6, 4);
        } else if (bullet.kind == 1 || bullet.kind == 8) {
            double size = bullet.kind == 8 ? 36 : 28;
            SpriteSheet.drawRotated(g, ARROW_SPRITE, bullet.x, bullet.y, size, size,
                    0, bullet.angle, 0, false);
        } else if (bullet.kind == 4) {
            // 剑气素材（sword_beam 700×2400）本身是竖向长条，无需角度矫正，
            // 直接按飞行朝向角旋转即可
            SpriteSheet.drawRotated(g, BEAM_SPRITE, bullet.x, bullet.y, 22, 80,
                    0, bullet.angle, 0, false);
        } else {
            // 掷出飞刀（小明/小红）：80×40 保持素材 2:1 比例
            SpriteSheet.drawRotated(g, bullet.kind == 2 ? BLADE_0_SPRITE : BLADE_1_SPRITE,
                    bullet.x, bullet.y, 80, 40, 0, bullet.angle, 0, false);
        }
    }

    /**
     * 绘制敌人子弹：紫红外圈光晕 + 橙色内核 + 白色高光的三层圆。
     *
     * @param g      画布上下文
     * @param bullet 敌弹数据
     */
    public static void drawEnemyBullet(GraphicsContext g, CombatController.EnemyBullet bullet) {
        setColor(g, Color.rgb(255, 92, 150, 70 / 255.0));
        g.fillOval((int) bullet.x - 8, (int) bullet.y - 8, 16, 16);
        setColor(g, Color.rgb(255, 158, 92));
        g.fillOval((int) bullet.x - 4, (int) bullet.y - 4, 8, 8);
        setColor(g, Color.rgb(255, 238, 200));
        g.fillOval((int) bullet.x - 2, (int) bullet.y - 2, 4, 4);
    }

    /** 同时设置填充与描边颜色，供统一调用 */
    private static void setColor(GraphicsContext graphics, Paint paint) {
        graphics.setFill(paint);
        graphics.setStroke(paint);
    }
}
