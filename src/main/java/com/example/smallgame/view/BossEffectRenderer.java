package com.example.smallgame.view;

import com.example.smallgame.controller.combat.BossController;
import com.example.smallgame.model.entity.Monster;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcType;

/**
 * Boss 专属特殊攻击特效渲染器：负责 BossController 特殊攻击的视觉表现
 * （圣诞树人冲刺前摇、巨人眼部激光、大巫师陨石、虚空裂缝、飞龙火球与海盗钩锁）。
 * BossController 只保存特效状态与结算逻辑，绘制全部集中在本类。
 * <p>
 * 引用文件：controller.combat.BossController（特效状态数据）、model.entity.Monster（Boss 坐标）。
 * 被 GameMainView（敌人之上的特效图层）调用。
 */
public final class BossEffectRenderer {

    /** 工具类，禁止实例化。 */
    private BossEffectRenderer() {
    }

    /** 绘制 Boss 专属特殊攻击特效（前摇、激光、陨石、裂缝、火球、钩锁与瞄准线）。 */
    public static void draw(GraphicsContext g, BossController controller, int animationTick) {
        drawTreeWindup(g, controller, animationTick);
        drawGiantLaser(g, controller, animationTick);
        drawMeteors(g, controller);
        drawRifts(g, controller, animationTick);
        drawFireballs(g, controller);
        drawHook(g, controller);
    }

    /** 圣诞树人冲刺前摇：脚下尘圈扩散 + 身体蓄力金色光点。 */
    private static void drawTreeWindup(GraphicsContext g, BossController controller, int tick) {
        Monster boss = controller.getBoss();
        if (boss == null || controller.getChargeWindupTicks() <= 0) {
            return;
        }
        double progress = 1 - controller.getChargeWindupTicks()
                / (double) BossController.TREE_CHARGE_WINDUP_TICKS;
        // 脚下尘土扩散圈
        double dustRadius = 34 + progress * 46;
        setColor(g, Color.rgb(160, 130, 90, 120 * progress / 255.0));
        g.setLineWidth(3);
        g.strokeOval(boss.getPx() - dustRadius, boss.getPy() + 36 - dustRadius * 0.4,
                dustRadius * 2, dustRadius * 0.8);
        // 蓄力金色光点（闪烁渐亮）
        double glow = (0.5 + Math.sin(tick * 0.6) * 0.3) * progress;
        setColor(g, Color.rgb(255, 220, 120, glow * 0.55));
        g.fillOval(boss.getPx() - 20, boss.getPy() - 34, 40, 40);
        setColor(g, Color.rgb(255, 246, 200, glow));
        g.fillOval(boss.getPx() - 10, boss.getPy() - 24, 20, 20);
    }

    /** 巨人眼部双激光：前摇聚能 → 外光束 + 亮芯，沿锁定方向延伸至墙体。 */
    private static void drawGiantLaser(GraphicsContext g, BossController controller, int tick) {
        Monster boss = controller.getBoss();
        if (boss == null) {
            return;
        }
        // 前摇：双眼聚能渐亮，蓄力完成前不发射光束
        if (controller.getLaserWindupTicks() > 0) {
            double intensity = 1 - controller.getLaserWindupTicks()
                    / (double) BossController.GIANT_LASER_WINDUP_TICKS;
            for (double offset : new double[]{-BossController.GIANT_EYE_OFFSET_X,
                    BossController.GIANT_EYE_OFFSET_X}) {
                double eyeX = boss.getPx() + offset;
                double eyeY = boss.getPy() + BossController.GIANT_EYE_OFFSET_Y;
                double pulse = 0.5 + Math.sin(tick * 0.8) * 0.2;
                setColor(g, Color.rgb(140, 255, 120, (0.35 + 0.5 * intensity) * pulse));
                g.fillOval(eyeX - 14, eyeY - 14, 28, 28);
                setColor(g, Color.rgb(240, 255, 230, (0.5 + 0.5 * intensity) * pulse));
                g.fillOval(eyeX - 7, eyeY - 7, 14, 14);
                // 外扩聚能光环
                double ring = 14 + (1 - intensity) * 16;
                setColor(g, Color.rgb(140, 255, 120, (0.25 + 0.4 * intensity) * pulse));
                g.setLineWidth(3);
                g.strokeOval(eyeX - ring, eyeY - ring, ring * 2, ring * 2);
            }
            return;
        }
        if (controller.getLaserTicks() <= 0) {
            return;
        }
        for (double offset : new double[]{-BossController.GIANT_EYE_OFFSET_X,
                BossController.GIANT_EYE_OFFSET_X}) {
            double eyeX = boss.getPx() + offset;
            double eyeY = boss.getPy() + BossController.GIANT_EYE_OFFSET_Y;
            g.save();
            g.translate(eyeX, eyeY);
            g.rotate(Math.toDegrees(controller.getLaserAngle()));
            setColor(g, Color.rgb(120, 255, 110, 90 / 255.0));
            g.fillRect(0, -8, controller.getLaserRayLength(), 16);
            setColor(g, Color.rgb(190, 255, 160, 200 / 255.0));
            g.fillRect(0, -4, controller.getLaserRayLength(), 8);
            setColor(g, Color.rgb(255, 255, 240, 220 / 255.0));
            g.fillRect(0, -1, controller.getLaserRayLength(), 2);
            g.restore();
            // 眼源光斑
            setColor(g, Color.rgb(220, 255, 190, 200 / 255.0));
            g.fillOval(eyeX - 8, eyeY - 8, 16, 16);
        }
    }

    /** 大巫师陨石：红色预警圈闪烁 → 高空坠落 → 落地爆炸。 */
    private static void drawMeteors(GraphicsContext g, BossController controller) {
        for (BossController.Meteor meteor : controller.getMeteors()) {
            if (meteor.warnTicks > 0) {
                double intensity = 0.5 + Math.sin(meteor.warnTicks * 0.4) * 0.3;
                setColor(g, Color.rgb(255, 70, 60, Math.max(0.2, intensity)));
                g.setLineWidth(3);
                g.strokeOval(meteor.x - 40, meteor.y - 40, 80, 80);
                g.setLineWidth(2);
                g.strokeOval(meteor.x - 26, meteor.y - 26, 52, 52);
                setColor(g, Color.rgb(255, 150, 120, 0.5));
                g.fillOval(meteor.x - 10, meteor.y - 10, 20, 20);
            } else if (meteor.boomTicks > 0) {
                double t = meteor.boomTicks / (double) BossController.METEOR_BOOM_TICKS;
                setColor(g, Color.rgb(255, 160, 60, 0.7 * t));
                g.fillOval(meteor.x - 50 * t, meteor.y - 50 * t, 100 * t, 100 * t);
                setColor(g, Color.rgb(255, 220, 120, t));
                g.fillOval(meteor.x - 20 * t, meteor.y - 20 * t, 40 * t, 40 * t);
            } else {
                double fallY = meteor.y - 420 + meteor.fallTicks * 14;
                setColor(g, Color.rgb(96, 68, 46));
                g.fillOval(meteor.x - 16, fallY - 16, 32, 32);
                setColor(g, Color.rgb(134, 96, 60));
                g.fillOval(meteor.x - 12, fallY - 12, 24, 24);
                setColor(g, Color.rgb(255, 170, 60));
                g.fillOval(meteor.x - 7, fallY - 26, 11, 11);
                setColor(g, Color.rgb(255, 224, 130));
                g.fillOval(meteor.x - 4, fallY - 23, 5, 5);
            }
        }
    }

    /** 虚空裂缝：旋转紫色裂片 + 中央黑洞。 */
    private static void drawRifts(GraphicsContext g, BossController controller, int tick) {
        for (BossController.Rift rift : controller.getRifts()) {
            double fade = Math.min(1, rift.life / 60.0);
            setColor(g, Color.rgb(150, 70, 230, 150 * fade / 255.0));
            g.setLineWidth(2);
            g.strokeOval(rift.x - 26, rift.y - 26, 52, 52);
            setColor(g, Color.rgb(120, 50, 200, 190 * fade / 255.0));
            for (int i = 0; i < 3; i++) {
                double angle = tick * 0.12 + Math.PI * 2 * i / 3;
                double innerX = rift.x + Math.cos(angle) * 4;
                double innerY = rift.y + Math.sin(angle) * 4;
                double outerX = rift.x + Math.cos(angle) * 18;
                double outerY = rift.y + Math.sin(angle) * 18;
                double sideX = Math.cos(angle + Math.PI / 2) * 7;
                double sideY = Math.sin(angle + Math.PI / 2) * 7;
                g.fillPolygon(new double[]{innerX + sideX, innerX - sideX, outerX},
                        new double[]{innerY + sideY, innerY - sideY, outerY}, 3);
            }
            setColor(g, Color.rgb(24, 10, 48, 220 * fade / 255.0));
            g.fillOval(rift.x - 10, rift.y - 10, 20, 20);
            setColor(g, Color.rgb(170, 120, 255, 160 * fade / 255.0));
            g.fillOval(rift.x - 4, rift.y - 4, 8, 8);
        }
    }

    /** 大火球（橙红光晕+亮核+尾焰）与小火球。 */
    private static void drawFireballs(GraphicsContext g, BossController controller) {
        for (BossController.Fireball fireball : controller.getFireballs()) {
            setColor(g, Color.rgb(255, 120, 40, 90 / 255.0));
            g.fillOval(fireball.x - 15, fireball.y - 15, 30, 30);
            setColor(g, Color.rgb(255, 160, 50));
            g.fillOval(fireball.x - 10, fireball.y - 10, 20, 20);
            setColor(g, Color.rgb(255, 236, 150));
            g.fillOval(fireball.x - 5, fireball.y - 5, 10, 10);
            double tailX = fireball.x - fireball.dx * 3;
            double tailY = fireball.y - fireball.dy * 3;
            setColor(g, Color.rgb(255, 110, 40, 120 / 255.0));
            g.fillOval(tailX - 6, tailY - 6, 12, 12);
        }
        for (BossController.SmallFireball fireball : controller.getSmallFireballs()) {
            setColor(g, Color.rgb(255, 170, 60, 200 / 255.0));
            g.fillOval(fireball.x - 4, fireball.y - 4, 8, 8);
            setColor(g, Color.rgb(255, 230, 150));
            g.fillOval(fireball.x - 2, fireball.y - 2, 4, 4);
        }
    }

    /** 钢铁海盗钩锁：瞄准阶段旋转蓄力钩 + 红线渐伸；射出后为锁链与铁钩。 */
    private static void drawHook(GraphicsContext g, BossController controller) {
        Monster boss = controller.getBoss();
        if (boss == null || controller.getHookPhase() == 0) {
            return;
        }
        if (controller.getHookPhase() == 1) {
            double progress = 1 - controller.getHookAimTicks()
                    / (double) BossController.PIRATE_HOOK_AIM_TICKS;
            // 瞄准红线随蓄力进度延伸
            double aimLength = 60 + (BossController.PIRATE_HOOK_MAX_RANGE - 60) * progress;
            setColor(g, Color.rgb(255, 80, 70, (90 + 90 * progress) / 255.0));
            g.setLineWidth(2);
            g.strokeLine(boss.getPx(), boss.getPy(),
                    boss.getPx() + Math.cos(controller.getHookAngle()) * aimLength,
                    boss.getPy() + Math.sin(controller.getHookAngle()) * aimLength);
            // 手部旋转蓄力钩子（前摇动作）
            double spin = controller.getHookAimTicks() * 0.55;
            double handX = boss.getPx() + Math.cos(controller.getHookAngle()) * 46;
            double handY = boss.getPy() + Math.sin(controller.getHookAngle()) * 46;
            double hookTipX = handX + Math.cos(spin) * 18;
            double hookTipY = handY + Math.sin(spin) * 18;
            setColor(g, Color.rgb(204, 208, 218));
            g.setLineWidth(4);
            g.strokeLine(handX, handY, hookTipX, hookTipY);
            setColor(g, Color.rgb(180, 186, 200));
            g.setLineWidth(4);
            g.strokeArc(hookTipX - 8, hookTipY - 8, 16, 16, Math.toDegrees(spin), 190, ArcType.OPEN);
            return;
        }
        // 锁链：分段虚线感
        setColor(g, Color.rgb(122, 126, 136));
        g.setLineWidth(3);
        g.strokeLine(boss.getPx(), boss.getPy(), controller.getHookX(), controller.getHookY());
        // 铁钩
        setColor(g, Color.rgb(204, 208, 218));
        g.fillOval(controller.getHookX() - 5, controller.getHookY() - 5, 10, 10);
        setColor(g, Color.rgb(150, 156, 168));
        g.setLineWidth(4);
        g.strokeArc(controller.getHookX() - 9, controller.getHookY() - 11, 13, 13, -40, 200, ArcType.OPEN);
    }

    /** 同时设置填充色与描边色（特效绘制时免去两次调用）。 */
    private static void setColor(GraphicsContext graphics, Paint paint) {
        graphics.setFill(paint);
        graphics.setStroke(paint);
    }
}
