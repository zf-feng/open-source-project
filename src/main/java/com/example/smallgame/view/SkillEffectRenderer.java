package com.example.smallgame.view;

import com.example.smallgame.controller.skill.SkillController;
import com.example.smallgame.util.SpriteSheet;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.List;

/**
 * 技能特效渲染器：负责灵箭之息技能的地面领域圈与从天而降的箭矢装饰。
 * SkillController 只保存技能状态与伤害结算逻辑，绘制全部集中在本类。
 * <p>
 * 引用文件：controller.skill.SkillController（箭雨领域数据）、util.SpriteSheet（箭矢立绘）。
 * 被 GameMainView（技能特效图层）调用。
 */
public final class SkillEffectRenderer {

    /** 箭雨装饰性坠落箭的立绘。 */
    private static final Image ARROW_SPRITE = SpriteSheet.load("/sprites/weapons/arrow").frame(0);

    /** 工具类，禁止实例化。 */
    private SkillEffectRenderer() {
    }

    /** 预热箭矢立绘缓存（进入游戏时调用，避免首帧卡顿）。 */
    public static void preload() {
        SpriteSheet.load("/sprites/weapons/arrow");
    }

    /**
     * 绘制灵箭之息特效：地面领域圈（固定位置，不随敌人移动）与从天而降的箭矢装饰。
     *
     * @param g              画布上下文
     * @param tick           逻辑帧计数
     * @param rainX          领域中心 X（生成瞬间确定）
     * @param rainY          领域中心 Y
     * @param fallingArrows  下坠箭矢装饰（x, y, 倾摆角度）
     */
    public static void draw(GraphicsContext g, int tick, double rainX, double rainY,
                            List<double[]> fallingArrows) {
        double pulse = (Math.sin(tick * 0.15) + 1) / 2;
        // 地面领域圈
        setColor(g, Color.rgb(80, 160, 255, 55 / 255.0));
        g.fillOval(rainX - SkillController.ARROW_RAIN_RADIUS, rainY - SkillController.ARROW_RAIN_RADIUS,
                SkillController.ARROW_RAIN_RADIUS * 2, SkillController.ARROW_RAIN_RADIUS * 2);
        setColor(g, Color.rgb(150, 215, 255, 0.45 + 0.3 * pulse));
        g.setLineWidth(1.5);
        g.strokeOval(rainX - SkillController.ARROW_RAIN_RADIUS, rainY - SkillController.ARROW_RAIN_RADIUS,
                SkillController.ARROW_RAIN_RADIUS * 2, SkillController.ARROW_RAIN_RADIUS * 2);
        // 从天而降的箭矢
        for (double[] arrow : fallingArrows) {
            SpriteSheet.drawRotated(g, ARROW_SPRITE, arrow[0], arrow[1], 22, 22,
                    0, Math.PI / 2, arrow[2], false);
        }
    }

    /** 同时设置填充与描边颜色，供统一调用 */
    private static void setColor(GraphicsContext graphics, Paint paint) {
        graphics.setFill(paint);
        graphics.setStroke(paint);
    }
}
