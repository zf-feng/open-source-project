package com.example.smallgame.view;

import com.example.smallgame.model.entity.MapTheme;
import com.example.smallgame.model.entity.Monster;
import com.example.smallgame.model.entity.MonsterType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcType;

/**
 * 敌人外观绘制中心：为每个地图主题提供专属的敌人造型。
 * drawMonster 负责完整敌人绘制（地面阴影、身体造型、冰冻蒙版、
 * 入场红光、攻击闪光与血条），model.entity.Monster 仅保存数据。
 * <p>
 * 引用文件：model.entity（MapTheme、Monster、MonsterType）。
 * 被 GameMainView（渲染敌人）调用。
 */
public final class MonsterAppearance {

    /** 巨人 Boss（草原）整体放大倍率：280/220。 */
    private static final double GIANT_BOSS_SCALE = 280.0 / 220.0;

    /** 工具类，禁止实例化。 */
    private MonsterAppearance() {
    }

    /** 按主题分发绘制敌人身体造型，并叠加攻击方式特征部件。 */
    public static void draw(GraphicsContext g, MapTheme theme, MonsterType type,
                            int attackStyle, int attackPattern,
                            int px, int drawY, int size, int animationTick) {
        if (type == MonsterType.BOSS) {
            // Boss 专属建模：十套独立造型（自带武器，不再叠加攻击特征部件）
            drawBoss(g, theme, px, drawY, size, animationTick);
            return;
        }
        switch (theme) {
            case FOREST: drawForestWisp(g, type, px, drawY, size, animationTick); break;
            case GRASSLAND: drawGrassImp(g, type, px, drawY, size, animationTick); break;
            case TUNDRA: drawFrostYeti(g, type, px, drawY, size, animationTick); break;
            case DESERT: drawSandScorpion(g, type, px, drawY, size, animationTick); break;
            case CASTLE: drawStoneGargoyle(g, type, px, drawY, size, animationTick); break;
            case DARK_FOREST: drawShadowFiend(g, type, px, drawY, size, animationTick); break;
            case SWAMP: drawToxicToad(g, type, px, drawY, size, animationTick); break;
            case SPACE: drawCosmicWatcher(g, type, px, drawY, size, animationTick); break;
            case VOLCANO: drawLavaGolem(g, type, px, drawY, size, animationTick); break;
            case ISLAND: drawCoconutCrab(g, type, px, drawY, size, animationTick); break;
            default: drawSlime(g, type, px, drawY, size, animationTick); break;
        }
        // 攻击方式特征部件：近战利爪 / 各弹幕模式专属武器（同一主题内小怪以此区分）
        drawAttackFeature(g, theme, attackStyle, attackPattern, px, drawY, size, animationTick);
    }

    /**
     * 绘制完整怪物：地面阴影 → 按主题造型的敌人身体 → 冰冻蒙版 →
     * 生成入场红光 → 攻击闪光 → 头顶血条（原 Monster.draw 的全部职责迁入视图层）。
     * 巨人 Boss（草原）只露上半身，整体建模放大至 280 像素。
     *
     * @param g             画布画笔
     * @param monster       怪物实体（提供坐标、状态与外观参数）
     * @param animationTick 逻辑帧计数（驱动浮动与特效动画）
     */
    public static void drawMonster(GraphicsContext g, Monster monster, int animationTick) {
        MapTheme theme = monster.getAppearanceTheme();
        MonsterType type = monster.getType();
        double px = monster.getPx();
        double py = monster.getPy();
        // 上下浮动：同类型怪物以各自相位错开节奏
        double bob = Math.sin(animationTick * 0.1 + monster.getAnimationOffset()) * 3.5;
        int drawY = (int) (py + bob);
        int size = type == MonsterType.BOSS ? 96 : type == MonsterType.ELITE ? 40 : 30;
        if (type == MonsterType.BOSS && theme == MapTheme.GRASSLAND) {
            size = 280; // 巨人 Boss：只露上半身，占据约四分之一地图（建模整体再放大）
        }
        int shadowWidth = (int) (size + 2 - bob * 1.5);
        int half = size / 2;
        setColor(g, Color.rgb(0, 0, 0, 75 / 255.0));
        g.fillRect((int) px - shadowWidth / 2, (int) py + size / 2 - 3, shadowWidth, 6);

        // 身体造型：按主题风格分发（十套专属敌人外观），叠加攻击方式特征部件
        MonsterAppearance.draw(g, theme, type, monster.getAttackStyle(),
                monster.getAttackPattern(), (int) px, drawY, size, animationTick);

        // 冰冻：蓝色透明蒙版覆盖怪物立绘
        if (monster.getFrozenTicks() > 0) {
            setColor(g, Color.rgb(90, 175, 255, 110 / 255.0));
            g.fillRect((int) px - half - 8, drawY - half - 10, size + 16, size + 20);
            setColor(g, Color.rgb(200, 240, 255, 200 / 255.0));
            g.setLineWidth(2);
            g.strokeRect((int) px - half - 8, drawY - half - 10, size + 16, size + 20);
        }

        // 生成入场红光闪烁：小型红色光晕 + 四向短射线，比人物蓝色闪光更小且颜色醒目区分
        if (monster.getSpawnFlashTicks() > 0) {
            double intensity = monster.getSpawnFlashTicks() / (double) Monster.SPAWN_FLASH_TICKS;
            // 闪烁感：奇数帧亮度降低
            if (monster.getSpawnFlashTicks() % 2 == 1) {
                intensity *= 0.7;
            }
            double fx = px;
            double fy = drawY - 4;
            for (int layer = 0; layer < 2; layer++) {
                double radius = 12 + layer * 10 + (1 - intensity) * 12;
                double alpha = intensity * (0.55 - layer * 0.2);
                setColor(g, Color.rgb(255, 70, 60, Math.max(0, alpha)));
                g.fillOval(fx - radius, fy - radius, radius * 2, radius * 2);
            }
            // 中心亮核
            setColor(g, Color.rgb(255, 255, 230, intensity * 0.95));
            g.fillOval(fx - 5, fy - 5, 10, 10);
            // 四向短红光射线（十字形，区别于人物十条长射线）
            double inner = 10;
            double outer = 22 + 14 * intensity;
            double halfWidth = 2 + 2 * intensity;
            for (int i = 0; i < 4; i++) {
                double angle = i * Math.PI / 2;
                double cosA = Math.cos(angle);
                double sinA = Math.sin(angle);
                setColor(g, Color.rgb(255, 110, 90, intensity * 0.9));
                g.beginPath();
                g.moveTo(fx + cosA * inner - sinA * halfWidth,
                        fy + sinA * inner + cosA * halfWidth);
                g.lineTo(fx + cosA * outer, fy + sinA * outer);
                g.lineTo(fx + cosA * inner + sinA * halfWidth,
                        fy + sinA * inner - cosA * halfWidth);
                g.closePath();
                g.fill();
            }
        }

        // 攻击闪光：近战挥击弧 / 远程发射光（沿攻击朝向的半圆弧）
        if (monster.getAttackFlashTicks() > 0) {
            double intensity = monster.getAttackFlashTicks() / (double) Monster.ATTACK_FLASH_TICKS;
            double range = monster.getRadius() + 16;
            double centerAngle = Math.toDegrees(monster.getAttackAngle());
            setColor(g, Color.rgb(255, 96, 70, 0.5 * intensity));
            g.setLineWidth(4);
            g.strokeArc(px - range, drawY - range, range * 2, range * 2,
                    centerAngle - 55, 110, ArcType.OPEN);
            setColor(g, Color.rgb(255, 214, 160, 0.75 * intensity));
            g.setLineWidth(2);
            g.strokeArc(px - range + 7, drawY - range + 7, (range - 7) * 2, (range - 7) * 2,
                    centerAngle - 38, 76, ArcType.OPEN);
        }

        // 血条：按类型区分宽度与颜色（Boss 紫色、精英橙色、普通红色）
        int barWidth = type == MonsterType.BOSS ? 84 : type == MonsterType.ELITE ? 44 : 30;
        setColor(g, Color.rgb(0, 0, 0, 130 / 255.0));
        g.fillRect((int) px - barWidth / 2, drawY - half - 14, barWidth, 5);
        setColor(g, type == MonsterType.BOSS ? Color.rgb(198, 110, 255)
                : type == MonsterType.ELITE ? Color.rgb(255, 174, 81)
                : Color.rgb(247, 104, 108));
        g.fillRect((int) px - barWidth / 2, drawY - half - 14,
                barWidth * monster.getHp() / monster.getMaxHp(), 5);
    }

    /** 各主题武器/能量强调色。 */
    private static Color accentFor(MapTheme theme) {
        switch (theme) {
            case FOREST: return Color.rgb(198, 228, 112);
            case GRASSLAND: return Color.rgb(224, 242, 132);
            case TUNDRA: return Color.rgb(184, 226, 255);
            case DESERT: return Color.rgb(242, 202, 122);
            case CASTLE: return Color.rgb(204, 204, 222);
            case DARK_FOREST: return Color.rgb(150, 255, 160);
            case SWAMP: return Color.rgb(192, 242, 112);
            case SPACE: return Color.rgb(140, 232, 255);
            case VOLCANO: return Color.rgb(255, 152, 72);
            case ISLAND: return Color.rgb(255, 222, 192);
            default: return Color.rgb(232, 192, 255);
        }
    }

    /**
     * 攻击方式特征部件：同一主题内的小怪按攻击方式拥有不同武器/姿态，
     * 近战怪展示双爪，远程怪按弹幕模式展示专属发射装置。
     */
    private static void drawAttackFeature(GraphicsContext g, MapTheme theme,
                                          int attackStyle, int attackPattern,
                                          int cx, int cy, int size, int tick) {
        int h = size / 2;
        if (attackStyle == 0) {
            // 近战：两侧三道利爪（骨白色）
            setColor(g, Color.rgb(238, 234, 216));
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 3; i++) {
                    g.setLineWidth(2);
                    g.strokeLine(cx + side * (h + 2), cy - 4 + i * 4,
                            cx + side * (h + 7 + i * 1.5), cy - 8 + i * 5);
                }
            }
            return;
        }
        Color accent = accentFor(theme);
        switch (attackPattern) {
            case 0: { // 单发：细长狙击管 + 瞄准红点
                setColor(g, accent);
                g.fillRect(cx + h - 6, cy - 5, 14, 5);
                g.fillRect(cx + h + 6, cy - 7, 4, 9);
                setColor(g, Color.rgb(255, 64, 64));
                g.fillOval(cx + h + 8, cy - 2, 4, 4);
                break;
            }
            case 1: { // 一排：横排三短管 + 三点红光
                setColor(g, accent);
                g.fillRect(cx - h + 2, cy - 4, 10, 4);
                g.fillRect(cx - h + 2, cy - 1, 10, 4);
                g.fillRect(cx - h + 2, cy + 2, 10, 4);
                setColor(g, Color.rgb(255, 92, 72));
                g.fillOval(cx - h - 2, cy - 4, 3, 3);
                g.fillOval(cx - h - 2, cy - 1, 3, 3);
                g.fillOval(cx - h - 2, cy + 2, 3, 3);
                break;
            }
            case 2: { // 环形弹幕：环绕旋转能量珠 + 头顶光环
                setColor(g, accent);
                for (int i = 0; i < 4; i++) {
                    double a = tick * 0.12 + Math.PI / 2 * i;
                    int bx = (int) (cx + Math.cos(a) * (h + 6));
                    int by = (int) (cy + Math.sin(a) * (h / 2.0 + 6));
                    g.fillOval(bx - 3, by - 3, 6, 6);
                }
                setColor(g, Color.rgb(255, 255, 255, 150 / 255.0));
                g.setLineWidth(1);
                g.strokeArc(cx - h - 6, cy - h - 8, size + 12, size + 12, 0, 360, ArcType.OPEN);
                break;
            }
            case 3: { // 旋转扫射：头顶旋转旋翼 + 中轴
                double spin = tick * 0.3;
                double wing = h + 9;
                setColor(g, accent);
                g.setLineWidth(3);
                g.strokeLine(cx - Math.cos(spin) * wing, cy - h - 8 - Math.sin(spin) * wing * 0.35,
                        cx + Math.cos(spin) * wing, cy - h - 8 + Math.sin(spin) * wing * 0.35);
                setColor(g, Color.rgb(96, 96, 116));
                g.fillOval(cx - 4, cy - h - 12, 8, 8);
                break;
            }
            case 4: { // 散弹：粗短炮管（喇叭口）
                setColor(g, accent);
                g.fillRect(cx + h - 10, cy - 7, 12, 14);
                g.fillPolygon(new double[]{cx + h + 2, cx + h + 8, cx + h + 2},
                        new double[]{cy - 9, cy, cy + 9}, 3);
                setColor(g, Color.rgb(52, 42, 36));
                g.fillRect(cx + h - 8, cy - 5, 8, 10);
                break;
            }
            case 5: { // 十字：前后左右四向晶棱
                setColor(g, accent);
                g.fillPolygon(new double[]{cx - h - 8, cx - h, cx - h - 8},
                        new double[]{cy - 4, cy, cy + 4}, 3);
                g.fillPolygon(new double[]{cx + h + 8, cx + h, cx + h + 8},
                        new double[]{cy - 4, cy, cy + 4}, 3);
                g.fillPolygon(new double[]{cx - 4, cx, cx + 4},
                        new double[]{cy - h - 8, cy - h, cy - h - 8}, 3);
                g.fillPolygon(new double[]{cx - 4, cx, cx + 4},
                        new double[]{cy + h + 8, cy + h, cy + h + 8}, 3);
                break;
            }
            case 6: { // 三连：并排三细管
                setColor(g, accent);
                g.fillRect(cx + h - 6, cy - 6, 12, 3);
                g.fillRect(cx + h - 6, cy - 1, 12, 3);
                g.fillRect(cx + h - 6, cy + 4, 12, 3);
                setColor(g, Color.rgb(255, 92, 72));
                g.fillOval(cx + h + 6, cy - 6, 3, 3);
                g.fillOval(cx + h + 6, cy - 1, 3, 3);
                g.fillOval(cx + h + 6, cy + 4, 3, 3);
                break;
            }
            default:
                break;
        }
    }

    /** 同时设置填充色与描边色。 */
    private static void setColor(GraphicsContext graphics, Paint paint) {
        graphics.setFill(paint);
        graphics.setStroke(paint);
    }

    /** 是否为 Boss 怪。 */
    private static boolean isBoss(MonsterType type) { return type == MonsterType.BOSS; }

    /** 是否为精英怪。 */
    private static boolean isElite(MonsterType type) { return type == MonsterType.ELITE; }

    // ------------------------------------------------------------------
    // 默认：黏液史莱姆（菜单界面）
    // ------------------------------------------------------------------
    /** 默认主题小怪：黏液史莱姆（菜单界面展示），按普通/精英/首领三档配色，含尖刺轮廓、脸与首领王冠。 */
    private static void drawSlime(GraphicsContext g, MonsterType type, int cx, int cy, int size, int tick) {
        int h = size / 2;
        Color outerColor = isBoss(type) ? Color.rgb(89, 32, 107)
                : isElite(type) ? Color.rgb(157, 53, 78)
                : Color.rgb(126, 35, 91);
        Color bodyColor = isBoss(type) ? Color.rgb(125, 65, 177)
                : isElite(type) ? Color.rgb(224, 116, 63)
                : Color.rgb(213, 83, 115);
        // 尖刺轮廓
        setColor(g, outerColor);
        double[] spikesX = {cx - h, cx - h / 2.0, cx - 7, cx, cx + 8, cx + h / 2.0, cx + h};
        double[] spikesY = {cy + h / 3.0, cy - h, cy - h / 2.0, cy - h - 8, cy - h / 2.0, cy - h, cy + h / 3.0};
        g.fillPolygon(spikesX, spikesY, spikesX.length);
        setColor(g, isBoss(type) ? Color.rgb(239, 177, 255)
                : isElite(type) ? Color.rgb(255, 170, 111)
                : Color.rgb(255, 131, 164));
        g.setLineWidth(2);
        g.strokePolygon(spikesX, spikesY, spikesX.length);
        // 身体
        setColor(g, bodyColor);
        g.fillRect(cx - h, cy - h, size, size);
        setColor(g, Color.rgb(255, 255, 255, 45 / 255.0));
        g.fillRect(cx - h / 2, cy - h / 2, size / 3, size / 4);
        setColor(g, isBoss(type) ? Color.rgb(197, 137, 255)
                : isElite(type) ? Color.rgb(255, 170, 92)
                : Color.rgb(244, 116, 147));
        g.fillRect(cx - h / 2, cy - h / 2, size / 2, size / 3);
        // 脸
        setColor(g, Color.rgb(255, 170, 151));
        int eyeSize = isBoss(type) ? 12 : 8;
        g.fillRect(cx - eyeSize / 2, cy - eyeSize / 2, eyeSize, eyeSize);
        setColor(g, Color.rgb(62, 20, 57));
        g.fillRect(cx + size / 5, cy - size / 5, Math.max(5, eyeSize / 2), Math.max(5, eyeSize / 2));
        setColor(g, Color.rgb(255, 239, 176, 180 / 255.0));
        g.setLineWidth(2);
        g.strokeLine(cx - eyeSize / 2, cy + eyeSize / 2 + 3, cx + eyeSize / 2, cy + eyeSize / 2 + 3);
        setColor(g, Color.rgb(255, 220, 116));
        g.fillRect(cx - 2, cy + size / 5, isBoss(type) ? 9 : 5, isBoss(type) ? 9 : 5);
        setColor(g, Color.rgb(244, 97, 127));
        g.fillRect(cx - h - 7, cy - 2, 8, 14);
        g.fillRect(cx + h - 1, cy - 2, 8, 14);
        if (isBoss(type)) {
            setColor(g, Color.rgb(255, 218, 102));
            g.fillPolygon(new double[]{cx - 18, cx - 9, cx, cx + 9, cx + 18},
                    new double[]{cy - h - 7, cy - h + 4, cy - h - 8, cy - h + 4, cy - h - 7}, 5);
        }
    }

    // ------------------------------------------------------------------
    // 森林：荆棘树妖
    // ------------------------------------------------------------------
    /** 森林主题小怪：荆棘树妖，藤蔓手臂摆动、树桩身体带年轮纹理，精英/首领有金色枝刺。 */
    private static void drawForestWisp(GraphicsContext g, MonsterType type, int cx, int cy, int size, int tick) {
        int h = size / 2;
        double sway = Math.sin(tick * 0.12) * 2;
        // 藤蔓手臂
        setColor(g, Color.rgb(66, 46, 30));
        g.setLineWidth(3);
        g.strokeLine(cx - h, cy + h / 4, cx - h - 7 + sway, cy - h / 4);
        g.strokeLine(cx + h, cy + h / 4, cx + h + 7 - sway, cy - h / 4);
        setColor(g, Color.rgb(70, 130, 60));
        g.fillOval((int) (cx - h - 11 + sway), cy - h / 4 - 4, 8, 8);
        g.fillOval((int) (cx + h + 4 - sway), cy - h / 4 - 4, 8, 8);
        // 树桩身体
        setColor(g, Color.rgb(94, 66, 40));
        g.fillRoundRect(cx - h, cy - h / 2, size, size, 8, 8);
        setColor(g, Color.rgb(56, 38, 24));
        g.setLineWidth(2);
        g.strokeRoundRect(cx - h, cy - h / 2, size, size, 8, 8);
        // 年轮纹理
        setColor(g, Color.rgb(72, 50, 30));
        g.strokeArc(cx - h + 5, cy - h / 2 + 5, size - 10, h - 4, 180, 180, ArcType.OPEN);
        g.strokeArc(cx - h + 5, cy + h / 4 - 2, size - 10, h - 4, 180, 180, ArcType.OPEN);
        // 树冠
        setColor(g, Color.rgb(28, 66, 40));
        g.fillPolygon(new double[]{cx - h - 5, cx, cx + h + 5},
                new double[]{cy - h / 2 + 6, cy - h - 10, cy - h / 2 + 6}, 3);
        setColor(g, Color.rgb(44, 100, 54));
        g.fillPolygon(new double[]{cx - h / 2, cx, cx + h / 2},
                new double[]{cy - h / 2 + 2, cy - h - 16, cy - h / 2 + 2}, 3);
        setColor(g, Color.rgb(120, 200, 96));
        g.setLineWidth(1);
        g.strokeLine(cx, cy - h - 14, cx, cy - h - 6);
        if (isElite(type) || isBoss(type)) {
            // 精英/首领：金色枝刺
            setColor(g, Color.rgb(214, 178, 96));
            g.fillPolygon(new double[]{cx - h - 4, cx - h + 4, cx - h - 8},
                    new double[]{cy - h / 2, cy - h / 2 - 6, cy - h / 2 + 2}, 3);
            g.fillPolygon(new double[]{cx + h + 4, cx + h - 4, cx + h + 8},
                    new double[]{cy - h / 2, cy - h / 2 - 6, cy - h / 2 + 2}, 3);
        }
        if (isBoss(type)) {
            setColor(g, Color.rgb(255, 214, 96));
            g.fillPolygon(new double[]{cx - 12, cx, cx + 12},
                    new double[]{cy - h - 15, cy - h - 26, cy - h - 15}, 3);
        }
        // 发光眼
        boolean blink = tick % 90 < 3;
        if (blink) {
            setColor(g, Color.rgb(36, 66, 30));
            g.fillRect(cx - h / 2, cy - h / 6 - 2, h, 4);
        } else {
            setColor(g, Color.rgb(220, 255, 130));
            g.fillRect(cx - h / 2, cy - h / 6 - 3, h - 2, 5);
            setColor(g, Color.rgb(110, 160, 40));
            g.fillRect(cx + h / 6, cy - h / 6 - 3, h / 5, 5);
        }
        // 嘴巴
        setColor(g, Color.rgb(44, 32, 22));
        g.setLineWidth(2);
        g.strokeArc(cx - 7, cy + 4, 14, 8, 0, 180, ArcType.OPEN);
    }

    // ------------------------------------------------------------------
    // 草原：草叶精怪
    // ------------------------------------------------------------------
    /** 草原主题小怪：草叶精怪，头顶草叶摇曳，精英/首领有额上草芽角。 */
    private static void drawGrassImp(GraphicsContext g, MonsterType type, int cx, int cy, int size, int tick) {
        int h = size / 2;
        double sway = Math.sin(tick * 0.14) * 2;
        // 头顶草叶
        setColor(g, Color.rgb(74, 150, 56));
        g.fillPolygon(new double[]{cx - h / 2, cx - h / 3 + sway, cx - h / 2 - 3},
                new double[]{cy - h + 2, cy - h - 12, cy - h + 2}, 3);
        g.fillPolygon(new double[]{cx, cx + sway, cx - 3},
                new double[]{cy - h + 2, cy - h - 16, cy - h + 2}, 3);
        g.fillPolygon(new double[]{cx + h / 2, cx + h / 3 - sway, cx + h / 2 + 3},
                new double[]{cy - h + 2, cy - h - 12, cy - h + 2}, 3);
        // 身体
        setColor(g, Color.rgb(96, 186, 78));
        g.fillOval(cx - h, cy - h / 2, size, size - 4);
        setColor(g, Color.rgb(58, 124, 50));
        g.setLineWidth(2);
        g.strokeOval(cx - h, cy - h / 2, size, size - 4);
        // 肚皮
        setColor(g, Color.rgb(178, 232, 148));
        g.fillOval(cx - h / 2, cy - 4, h, h + 6);
        // 腮红
        setColor(g, Color.rgb(255, 150, 150, 150 / 255.0));
        g.fillOval(cx - h + 2, cy + 2, h / 3, h / 4);
        g.fillOval(cx + h / 2, cy + 2, h / 3, h / 4);
        // 眼睛
        boolean blink = tick % 80 < 3;
        int eyeSize = isBoss(type) ? 8 : 6;
        setColor(g, Color.rgb(30, 60, 26));
        if (blink) {
            g.fillRect(cx - h / 2, cy - h / 4, h / 2, 3);
            g.fillRect(cx, cy - h / 4, h / 2, 3);
        } else {
            g.fillOval(cx - h / 2, cy - h / 4, eyeSize, eyeSize);
            g.fillOval(cx + h / 2 - eyeSize, cy - h / 4, eyeSize, eyeSize);
            setColor(g, Color.WHITE);
            g.fillOval(cx - h / 2 + 2, cy - h / 4 + 1, 3, 3);
            g.fillOval(cx + h / 2 - eyeSize + 2, cy - h / 4 + 1, 3, 3);
        }
        if (isElite(type) || isBoss(type)) {
            // 精英/首领：额上草芽角
            setColor(g, Color.rgb(150, 220, 110));
            g.fillPolygon(new double[]{cx - 5, cx - 2, cx - 7},
                    new double[]{cy - h, cy - h - 8, cy - h}, 3);
            g.fillPolygon(new double[]{cx + 5, cx + 2, cx + 7},
                    new double[]{cy - h, cy - h - 8, cy - h}, 3);
        }
        if (isBoss(type)) {
            // 首领：头顶红花
            setColor(g, Color.rgb(244, 92, 108));
            for (int i = 0; i < 5; i++) {
                double a = Math.PI * 2 * i / 5;
                g.fillOval((int) (cx + Math.cos(a) * 5) - 3, (int) (cy - h - 16 + Math.sin(a) * 5) - 3, 6, 6);
            }
            setColor(g, Color.rgb(255, 216, 96));
            g.fillOval(cx - 3, cy - h - 19, 6, 6);
        }
    }

    // ------------------------------------------------------------------
    // 冰原：冰晶雪怪
    // ------------------------------------------------------------------
    /** 冰原主题小怪：冰晶雪怪，六边形冰晶身体带反光，精英/首领有额头冰冠。 */
    private static void drawFrostYeti(GraphicsContext g, MonsterType type, int cx, int cy, int size, int tick) {
        int h = size / 2;
        // 头顶与两侧冰晶刺
        setColor(g, Color.rgb(176, 224, 250));
        g.fillPolygon(new double[]{cx - 4, cx, cx + 4}, new double[]{cy - h + 4, cy - h - 12, cy - h + 4}, 3);
        g.fillPolygon(new double[]{cx - h + 2, cx - h - 4, cx - h + 6}, new double[]{cy - 2, cy - h / 3, cy - 4}, 3);
        g.fillPolygon(new double[]{cx + h - 2, cx + h + 4, cx + h - 6}, new double[]{cy - 2, cy - h / 3, cy - 4}, 3);
        // 冰晶身体（六边形）
        setColor(g, Color.rgb(140, 194, 240));
        g.fillPolygon(new double[]{cx - h, cx - h / 2, cx + h / 2, cx + h, cx + h / 2, cx - h / 2},
                new double[]{cy + 2, cy - h / 2, cy - h / 2, cy + 2, cy + h, cy + h}, 6);
        setColor(g, Color.rgb(74, 116, 166));
        g.setLineWidth(2);
        g.strokePolygon(new double[]{cx - h, cx - h / 2, cx + h / 2, cx + h, cx + h / 2, cx - h / 2},
                new double[]{cy + 2, cy - h / 2, cy - h / 2, cy + 2, cy + h, cy + h}, 6);
        // 冰面反光
        setColor(g, Color.rgb(255, 255, 255, 90 / 255.0));
        g.fillPolygon(new double[]{cx - h / 2, cx, cx + h / 2}, new double[]{cy - h / 2 + 2, cy - h / 2 - 4, cy - h / 2 + 2}, 3);
        // 雪白肚皮
        setColor(g, Color.rgb(232, 246, 255));
        g.fillOval(cx - h / 2, cy + 2, h, h - 2);
        setColor(g, Color.rgb(196, 226, 248));
        g.strokeArc(cx - h / 2 + 2, cy + 6, h - 4, h / 2, 180, 180, ArcType.OPEN);
        // 眼睛
        setColor(g, Color.rgb(38, 84, 140));
        int eyeSize = isBoss(type) ? 9 : 7;
        g.fillOval(cx - h / 2 + 2, cy - h / 4, eyeSize, eyeSize);
        g.fillOval(cx + h / 2 - eyeSize - 2, cy - h / 4, eyeSize, eyeSize);
        setColor(g, Color.rgb(220, 244, 255));
        g.fillOval(cx - h / 2 + 4, cy - h / 4 + 2, 3, 3);
        g.fillOval(cx + h / 2 - eyeSize, cy - h / 4 + 2, 3, 3);
        if (isElite(type) || isBoss(type)) {
            // 精英/首领：额头冰冠
            setColor(g, Color.rgb(210, 240, 255));
            g.fillPolygon(new double[]{cx - 10, cx - 5, cx, cx + 5, cx + 10},
                    new double[]{cy - h / 2, cy - h / 2 - 7, cy - h / 2 - 3, cy - h / 2 - 7, cy - h / 2}, 5);
        }
        if (isBoss(type)) {
            // 首领：大冰晶角
            setColor(g, Color.rgb(214, 244, 255));
            g.fillPolygon(new double[]{cx - 8, cx - 2, cx - 12}, new double[]{cy - h - 10, cy - h - 24, cy - h - 10}, 3);
            g.fillPolygon(new double[]{cx + 8, cx + 2, cx + 12}, new double[]{cy - h - 10, cy - h - 24, cy - h - 10}, 3);
        }
    }

    // ------------------------------------------------------------------
    // 沙漠：沙蝎卫士
    // ------------------------------------------------------------------
    /** 沙漠主题小怪：沙蝎卫士，毒尾摆动、双钳与三段甲壳，精英/首领有金色甲斑。 */
    private static void drawSandScorpion(GraphicsContext g, MonsterType type, int cx, int cy, int size, int tick) {
        int h = size / 2;
        double wag = Math.sin(tick * 0.16) * 3;
        // 蝎尾（身后上翘，毒针摆动）
        setColor(g, Color.rgb(176, 128, 66));
        g.setLineWidth(4);
        g.strokeLine(cx - h / 2, cy - h / 4, cx - h - 4, cy - h / 2 - 6 + wag);
        setColor(g, Color.rgb(150, 44, 40));
        g.fillOval((int) (cx - h - 10), (int) (cy - h / 2 - 10 + wag), 9, 9);
        setColor(g, Color.rgb(236, 190, 96));
        g.setLineWidth(1);
        g.strokeLine(cx - h - 6, cy - h / 2 - 6 + wag, cx - h - 9, cy - h / 2 - 9 + wag);
        // 双钳
        setColor(g, Color.rgb(190, 140, 78));
        g.fillOval(cx - h - 8, cy - 4, 10, 12);
        g.fillOval(cx + h - 2, cy - 4, 10, 12);
        g.setLineWidth(2);
        g.strokeOval(cx - h - 8, cy - 4, 10, 12);
        g.strokeOval(cx + h - 2, cy - 4, 10, 12);
        // 甲壳身体（三段）
        setColor(g, Color.rgb(202, 156, 90));
        g.fillRoundRect(cx - h, cy - h / 2, size, size - 2, 10, 10);
        setColor(g, Color.rgb(128, 96, 54));
        g.strokeRoundRect(cx - h, cy - h / 2, size, size - 2, 10, 10);
        setColor(g, Color.rgb(158, 118, 66));
        g.strokeArc(cx - h + 2, cy - 2, size - 4, 10, 180, 180, ArcType.OPEN);
        g.strokeArc(cx - h + 2, cy + 8, size - 4, 10, 180, 180, ArcType.OPEN);
        // 甲壳高光
        setColor(g, Color.rgb(236, 196, 130, 150 / 255.0));
        g.fillOval(cx - h / 3, cy - h / 2 + 2, h / 2, h / 3);
        // 眼睛（红）
        setColor(g, Color.rgb(230, 60, 40));
        int eyeSize = isBoss(type) ? 8 : 6;
        g.fillOval(cx - h / 2, cy - h / 4, eyeSize, eyeSize);
        g.fillOval(cx + h / 2 - eyeSize, cy - h / 4, eyeSize, eyeSize);
        setColor(g, Color.rgb(255, 220, 160));
        g.fillOval(cx - h / 2 + 2, cy - h / 4 + 1, 2, 2);
        g.fillOval(cx + h / 2 - eyeSize + 2, cy - h / 4 + 1, 2, 2);
        if (isElite(type) || isBoss(type)) {
            // 精英/首领：金色甲斑
            setColor(g, Color.rgb(246, 214, 128));
            g.fillOval(cx - 3, cy - h / 2 - 2, 6, 6);
        }
        if (isBoss(type)) {
            // 首领：双尾刺
            setColor(g, Color.rgb(150, 44, 40));
            g.fillOval((int) (cx - h - 14), (int) (cy - h / 2 - 16 - wag), 9, 9);
        }
    }

    // ------------------------------------------------------------------
    // 城堡：石像鬼
    // ------------------------------------------------------------------
    /** 城堡主题小怪：石像鬼，石翼扇动、石砖纹身体与紫眼，精英/首领有胸口符文。 */
    private static void drawStoneGargoyle(GraphicsContext g, MonsterType type, int cx, int cy, int size, int tick) {
        int h = size / 2;
        double flap = Math.sin(tick * 0.18) * 2;
        // 石翼
        setColor(g, Color.rgb(96, 98, 114));
        g.fillPolygon(new double[]{cx - h + 4, cx - h - 12, cx - h + 2, cx - h - 8},
                new double[]{cy - h / 2, cy - h + 2 + flap, cy + 2, cy + h / 2}, 4);
        g.fillPolygon(new double[]{cx + h - 4, cx + h + 12, cx + h - 2, cx + h + 8},
                new double[]{cy - h / 2, cy - h + 2 - flap, cy + 2, cy + h / 2}, 4);
        setColor(g, Color.rgb(70, 72, 86));
        g.setLineWidth(2);
        g.strokeLine(cx - h + 2, cy - h / 2, cx - h - 8, cy - h + 4);
        g.strokeLine(cx + h - 2, cy - h / 2, cx + h + 8, cy - h + 4);
        // 石身
        setColor(g, Color.rgb(112, 114, 130));
        g.fillRoundRect(cx - h, cy - h / 2, size, size - 2, 8, 8);
        setColor(g, Color.rgb(62, 64, 78));
        g.setLineWidth(2);
        g.strokeRoundRect(cx - h, cy - h / 2, size, size - 2, 8, 8);
        // 石砖纹
        setColor(g, Color.rgb(92, 94, 108));
        g.strokeLine(cx - h + 3, cy, cx + h - 3, cy);
        g.strokeLine(cx, cy, cx, cy + h);
        // 裂缝
        setColor(g, Color.rgb(52, 54, 66));
        g.strokeLine(cx - h / 3, cy - h / 2 + 2, cx - h / 4, cy - h / 6);
        // 额上尖角
        setColor(g, Color.rgb(86, 88, 102));
        g.fillPolygon(new double[]{cx - 6, cx, cx + 6}, new double[]{cy - h / 2 + 2, cy - h / 2 - 10, cy - h / 2 + 2}, 3);
        // 紫眼
        setColor(g, Color.rgb(190, 110, 255));
        int eyeSize = isBoss(type) ? 9 : 7;
        g.fillOval(cx - h / 2 + 2, cy - h / 6, eyeSize, eyeSize);
        g.fillOval(cx + h / 2 - eyeSize - 2, cy - h / 6, eyeSize, eyeSize);
        setColor(g, Color.rgb(255, 240, 255));
        g.fillOval(cx - h / 2 + 4, cy - h / 6 + 2, 2, 2);
        g.fillOval(cx + h / 2 - eyeSize, cy - h / 6 + 2, 2, 2);
        if (isElite(type) || isBoss(type)) {
            // 精英/首领：胸口符文
            setColor(g, Color.rgb(190, 110, 255));
            g.fillOval(cx - 3, cy + h / 2 - 4, 6, 6);
        }
        if (isBoss(type)) {
            // 首领：双角+胸甲
            setColor(g, Color.rgb(80, 82, 96));
            g.fillPolygon(new double[]{cx - 14, cx - 6, cx - 16}, new double[]{cy - h / 2 + 2, cy - h / 2 - 14, cy - h / 2 + 2}, 3);
            g.fillPolygon(new double[]{cx + 14, cx + 6, cx + 16}, new double[]{cy - h / 2 + 2, cy - h / 2 - 14, cy - h / 2 + 2}, 3);
            setColor(g, Color.rgb(140, 142, 158));
            g.fillRect(cx - h / 2, cy + 2, h, h - 6);
        }
    }

    // ------------------------------------------------------------------
    // 黑森林：暗影魔物
    // ------------------------------------------------------------------
    /** 黑森林主题小怪：暗影魔物，底部触须飘动、荧绿发光眼，精英/首领有额部第三只小眼。 */
    private static void drawShadowFiend(GraphicsContext g, MonsterType type, int cx, int cy, int size, int tick) {
        int h = size / 2;
        double wave = Math.sin(tick * 0.2) * 3;
        // 底部飘动触须
        setColor(g, Color.rgb(52, 32, 74));
        g.setLineWidth(3);
        g.strokeLine(cx - h / 2, cy + h, cx - h / 2 - 4 + wave, cy + h + 8);
        g.strokeLine(cx, cy + h, cx + wave, cy + h + 8);
        g.strokeLine(cx + h / 2, cy + h, cx + h / 2 + 4 - wave, cy + h + 8);
        // 身体（暗紫）
        setColor(g, Color.rgb(58, 36, 82));
        g.fillOval(cx - h, cy - h / 2, size, size - 4);
        setColor(g, Color.rgb(120, 72, 150));
        g.setLineWidth(2);
        g.strokeOval(cx - h, cy - h / 2, size, size - 4);
        // 紫光斑纹
        setColor(g, Color.rgb(150, 96, 190, 110 / 255.0));
        g.fillOval(cx - h / 3, cy - h / 2 + 4, h / 2, h / 3);
        setColor(g, Color.rgb(96, 54, 130));
        g.strokeArc(cx - h + 3, cy + 2, size - 6, h - 4, 200, 140, ArcType.OPEN);
        // 双弯角
        setColor(g, Color.rgb(150, 94, 180));
        g.fillPolygon(new double[]{cx - h + 4, cx - h - 4, cx - h + 8},
                new double[]{cy - h / 2, cy - h - 6, cy - h / 2 + 4}, 3);
        g.fillPolygon(new double[]{cx + h - 4, cx + h + 4, cx + h - 8},
                new double[]{cy - h / 2, cy - h - 6, cy - h / 2 + 4}, 3);
        // 荧绿发光眼
        boolean blink = tick % 100 < 3;
        setColor(g, Color.rgb(96, 255, 130));
        if (blink) {
            g.fillRect(cx - h / 2, cy - h / 6, h / 2, 3);
            g.fillRect(cx, cy - h / 6, h / 2, 3);
        } else {
            int eyeSize = isBoss(type) ? 9 : 7;
            g.fillOval(cx - h / 2, cy - h / 6, eyeSize, eyeSize);
            g.fillOval(cx + h / 2 - eyeSize, cy - h / 6, eyeSize, eyeSize);
            // 眼周光晕
            setColor(g, Color.rgb(96, 255, 130, 60 / 255.0));
            g.fillOval(cx - h / 2 - 3, cy - h / 6 - 3, eyeSize + 6, eyeSize + 6);
            g.fillOval(cx + h / 2 - eyeSize - 3, cy - h / 6 - 3, eyeSize + 6, eyeSize + 6);
        }
        if (isElite(type) || isBoss(type)) {
            // 精英/首领：额部第三只小眼
            setColor(g, Color.rgb(96, 255, 130));
            g.fillOval(cx - 2, cy - h / 2 + 2, 4, 4);
        }
        if (isBoss(type)) {
            // 首领：紫色光环
            setColor(g, Color.rgb(170, 110, 220, 110 / 255.0));
            g.setLineWidth(2);
            g.strokeOval(cx - h - 6, cy - h - 10, size + 12, size + 8);
        }
    }

    // ------------------------------------------------------------------
    // 沼泽：剧毒蟾王
    // ------------------------------------------------------------------
    /** 沼泽主题小怪：剧毒蟾王，毒气泡上升、黄眼竖瞳与背部毒疣，精英/首领有头冠。 */
    private static void drawToxicToad(GraphicsContext g, MonsterType type, int cx, int cy, int size, int tick) {
        int h = size / 2;
        double bubble = Math.sin(tick * 0.15 + 1.2) * 3;
        // 毒气泡
        setColor(g, Color.rgb(150, 210, 90, 110 / 255.0));
        g.fillOval(cx - h - 10, cy - h + 2 + bubble, 8, 8);
        g.fillOval(cx + h + 3, cy - h + 6 - bubble, 6, 6);
        setColor(g, Color.rgb(190, 240, 120, 150 / 255.0));
        g.setLineWidth(1);
        g.strokeOval(cx - h - 10, cy - h + 2 + bubble, 8, 8);
        g.strokeOval(cx + h + 3, cy - h + 6 - bubble, 6, 6);
        // 泥绿身体
        setColor(g, Color.rgb(102, 156, 72));
        g.fillOval(cx - h, cy - h / 2, size, size - 4);
        setColor(g, Color.rgb(54, 92, 46));
        g.setLineWidth(2);
        g.strokeOval(cx - h, cy - h / 2, size, size - 4);
        // 背疣
        setColor(g, Color.rgb(74, 122, 56));
        g.fillOval(cx - h / 3, cy - h / 2 + 3, 5, 5);
        g.fillOval(cx + h / 4, cy - h / 2 + 5, 4, 4);
        setColor(g, Color.rgb(128, 188, 88));
        g.fillOval(cx - h / 2, cy - h / 2 + 4, 4, 4);
        // 肚皮
        setColor(g, Color.rgb(168, 214, 110));
        g.fillOval(cx - h / 2, cy + 2, h, h - 4);
        // 大嘴
        setColor(g, Color.rgb(40, 66, 34));
        g.setLineWidth(2);
        g.strokeArc(cx - h / 2, cy - 2, h, h / 2, 200, 140, ArcType.OPEN);
        // 黄眼竖瞳
        setColor(g, Color.rgb(244, 226, 96));
        int eyeSize = isBoss(type) ? 10 : 8;
        g.fillOval(cx - h / 2, cy - h / 3, eyeSize, eyeSize);
        g.fillOval(cx + h / 2 - eyeSize, cy - h / 3, eyeSize, eyeSize);
        setColor(g, Color.rgb(30, 40, 26));
        g.fillRect(cx - h / 2 + eyeSize / 2 - 1, cy - h / 3 + 1, 2, eyeSize - 2);
        g.fillRect(cx + h / 2 - eyeSize / 2 - 1, cy - h / 3 + 1, 2, eyeSize - 2);
        if (isElite(type) || isBoss(type)) {
            // 精英/首领：头冠
            setColor(g, Color.rgb(196, 108, 168));
            g.fillPolygon(new double[]{cx - 8, cx - 4, cx, cx + 4, cx + 8},
                    new double[]{cy - h / 2, cy - h / 2 - 8, cy - h / 2 - 4, cy - h / 2 - 8, cy - h / 2}, 5);
        }
        if (isBoss(type)) {
            // 首领：背部毒泡群
            setColor(g, Color.rgb(170, 226, 100, 140 / 255.0));
            g.fillOval(cx - 10, cy - h / 2 - 4, 10, 10);
            g.fillOval(cx + 2, cy - h / 2 - 6, 8, 8);
        }
    }

    // ------------------------------------------------------------------
    // 太空：星际观察者
    // ------------------------------------------------------------------
    /** 太空主题小怪：星际观察者，金属球体带环绕光环与独眼。 */
    private static void drawCosmicWatcher(GraphicsContext g, MonsterType type, int cx, int cy, int size, int tick) {
        int h = size / 2;
        double rot = tick * 0.05;
        // 天线
        setColor(g, Color.rgb(120, 132, 180));
        g.setLineWidth(2);
        g.strokeLine(cx - h / 3, cy - h / 2, cx - h / 3, cy - h / 2 - 8);
        g.strokeLine(cx + h / 3, cy - h / 2, cx + h / 3, cy - h / 2 - 6);
        setColor(g, Color.rgb(150, 240, 255));
        g.fillOval(cx - h / 3 - 3, cy - h / 2 - 13, 6, 6);
        g.fillOval(cx + h / 3 - 3, cy - h / 2 - 11, 6, 6);
        // 金属球体
        setColor(g, Color.rgb(108, 128, 190));
        g.fillOval(cx - h, cy - h / 2, size, size - 4);
        setColor(g, Color.rgb(58, 72, 122));
        g.setLineWidth(2);
        g.strokeOval(cx - h, cy - h / 2, size, size - 4);
        // 金属缝线
        setColor(g, Color.rgb(84, 100, 160));
        g.strokeArc(cx - h + 3, cy - h / 2 + 3, size - 6, size - 10, 190, 130, ArcType.OPEN);
        g.strokeArc(cx - h + 3, cy - h / 2 + 3, size - 6, size - 10, 10, 130, ArcType.OPEN);
        // 高光
        setColor(g, Color.rgb(200, 220, 255, 130 / 255.0));
        g.fillOval(cx - h / 2, cy - h / 2 + 3, h / 2, h / 3);
        // 环绕光环
        setColor(g, Color.rgb(150, 220, 255, 120 / 255.0));
        g.setLineWidth(3);
        double ringRx = h + 10;
        double ringRy = h / 2 + 4;
        double ringX = cx + Math.cos(rot) * ringRx;
        double ringY = cy + Math.sin(rot) * ringRy * 0.4;
        g.strokeOval(ringX - ringRx, ringY - ringRy, ringRx * 2, ringRy * 2);
        if (isBoss(type)) {
            // 首领：第二道光环
            setColor(g, Color.rgb(255, 200, 120, 100 / 255.0));
            g.strokeOval(cx - ringRx - 6, cy - ringRy - 6, (ringRx + 6) * 2, (ringRy + 6) * 2);
        }
        // 独眼
        setColor(g, Color.rgb(20, 26, 48));
        int eyeSize = isBoss(type) ? 16 : isElite(type) ? 13 : 11;
        g.fillOval(cx - eyeSize / 2, cy - eyeSize / 2, eyeSize, eyeSize);
        setColor(g, Color.rgb(140, 245, 255));
        g.fillOval(cx - eyeSize / 2 + 2, cy - eyeSize / 2 + 2, eyeSize - 4, eyeSize - 4);
        setColor(g, Color.rgb(240, 255, 255));
        g.fillOval(cx - eyeSize / 4, cy - eyeSize / 4, eyeSize / 4, eyeSize / 4);
    }

    // ------------------------------------------------------------------
    // 火山：熔岩魔像
    // ------------------------------------------------------------------
    /** 火山主题小怪：熔岩魔像，黑岩身体带发光熔岩裂缝，精英/首领有胸口熔岩核。 */
    private static void drawLavaGolem(GraphicsContext g, MonsterType type, int cx, int cy, int size, int tick) {
        int h = size / 2;
        double flame = Math.sin(tick * 0.25) * 3;
        // 头顶火焰
        setColor(g, Color.rgb(255, 96, 40));
        g.fillPolygon(new double[]{cx - 6, cx, cx + 6}, new double[]{cy - h + 2, cy - h - 10 + flame, cy - h + 2}, 3);
        setColor(g, Color.rgb(255, 200, 60));
        g.fillPolygon(new double[]{cx - 3, cx, cx + 3}, new double[]{cy - h + 2, cy - h - 6 + flame / 2, cy - h + 2}, 3);
        // 黑岩身体
        setColor(g, Color.rgb(54, 38, 32));
        g.fillRoundRect(cx - h, cy - h / 2, size, size - 2, 8, 8);
        setColor(g, Color.rgb(30, 20, 16));
        g.setLineWidth(2);
        g.strokeRoundRect(cx - h, cy - h / 2, size, size - 2, 8, 8);
        // 肩部岩突
        setColor(g, Color.rgb(66, 46, 38));
        g.fillPolygon(new double[]{cx - h, cx - h - 5, cx - h + 3}, new double[]{cy - h / 2 + 2, cy - h / 4, cy - h / 4}, 3);
        g.fillPolygon(new double[]{cx + h, cx + h + 5, cx + h - 3}, new double[]{cy - h / 2 + 2, cy - h / 4, cy - h / 4}, 3);
        // 熔岩裂缝（发光折线）
        setColor(g, Color.rgb(255, 150, 60));
        g.setLineWidth(3);
        g.strokeLine(cx - h / 3, cy - h / 2 + 4, cx - h / 4, cy - h / 6);
        g.strokeLine(cx - h / 4, cy - h / 6, cx - h / 3, cy + 2);
        g.strokeLine(cx + h / 4, cy - h / 6, cx + h / 5, cy + 2);
        g.strokeLine(cx, cy + h / 4, cx + 2, cy + h - 4);
        // 裂缝亮核
        setColor(g, Color.rgb(255, 224, 120));
        g.setLineWidth(1);
        g.strokeLine(cx - h / 4, cy - h / 6, cx - h / 3, cy + 2);
        g.strokeLine(cx + h / 4, cy - h / 6, cx + h / 5, cy + 2);
        // 亮橙眼
        setColor(g, Color.rgb(255, 190, 60));
        int eyeSize = isBoss(type) ? 9 : 7;
        g.fillOval(cx - h / 2 + 2, cy - h / 6, eyeSize, eyeSize);
        g.fillOval(cx + h / 2 - eyeSize - 2, cy - h / 6, eyeSize, eyeSize);
        setColor(g, Color.rgb(255, 244, 200));
        g.fillOval(cx - h / 2 + 4, cy - h / 6 + 2, 2, 2);
        g.fillOval(cx + h / 2 - eyeSize, cy - h / 6 + 2, 2, 2);
        if (isElite(type) || isBoss(type)) {
            // 精英/首领：胸口熔岩核
            setColor(g, Color.rgb(255, 120, 40));
            g.fillOval(cx - 4, cy + h / 4, 8, 8);
            setColor(g, Color.rgb(255, 224, 120));
            g.fillOval(cx - 2, cy + h / 4 + 2, 4, 4);
        }
        if (isBoss(type)) {
            // 首领：全身更多裂缝
            setColor(g, Color.rgb(255, 150, 60));
            g.setLineWidth(2);
            g.strokeLine(cx - h / 2, cy + 2, cx - h / 3, cy + h - 4);
            g.strokeLine(cx + h / 2, cy + 2, cx + h / 3, cy + h - 4);
        }
    }

    // ------------------------------------------------------------------
    // 海岛：椰子蟹
    // ------------------------------------------------------------------
    /** 海岛主题小怪：椰子蟹，大钳开合、红壳带椰纹，精英/首领有钳口尖刺。 */
    private static void drawCoconutCrab(GraphicsContext g, MonsterType type, int cx, int cy, int size, int tick) {
        int h = size / 2;
        double snap = Math.sin(tick * 0.2) * 2;
        // 底部短腿
        setColor(g, Color.rgb(150, 60, 48));
        g.setLineWidth(2);
        g.strokeLine(cx - h / 2, cy + h - 4, cx - h / 2 - 4, cy + h + 2);
        g.strokeLine(cx - h / 6, cy + h - 4, cx - h / 6 - 2, cy + h + 2);
        g.strokeLine(cx + h / 6, cy + h - 4, cx + h / 6 + 2, cy + h + 2);
        g.strokeLine(cx + h / 2, cy + h - 4, cx + h / 2 + 4, cy + h + 2);
        // 大钳
        setColor(g, Color.rgb(224, 104, 80));
        g.fillOval(cx - h - 10, cy - h / 4, 12, 16);
        g.fillOval(cx + h - 2, cy - h / 4, 12, 16);
        setColor(g, Color.rgb(160, 62, 50));
        g.setLineWidth(2);
        g.strokeOval(cx - h - 10, cy - h / 4, 12, 16);
        g.strokeOval(cx + h - 2, cy - h / 4, 12, 16);
        setColor(g, Color.rgb(255, 230, 200));
        g.fillOval(cx - h - 10 + 3, cy - h / 4 + 4 + snap, 4, 4);
        g.fillOval(cx + h + 1, cy - h / 4 + 4 - snap, 4, 4);
        // 红壳身体
        setColor(g, Color.rgb(214, 98, 76));
        g.fillOval(cx - h, cy - h / 2, size, size - 4);
        setColor(g, Color.rgb(142, 54, 44));
        g.setLineWidth(2);
        g.strokeOval(cx - h, cy - h / 2, size, size - 4);
        // 椰纹弧线
        setColor(g, Color.rgb(176, 78, 60));
        g.strokeArc(cx - h + 4, cy - h / 2 + 4, size - 8, h - 4, 180, 180, ArcType.OPEN);
        g.strokeArc(cx - h + 4, cy + 2, size - 8, h - 4, 180, 180, ArcType.OPEN);
        // 壳高光
        setColor(g, Color.rgb(255, 170, 150, 110 / 255.0));
        g.fillOval(cx - h / 2, cy - h / 2 + 2, h / 2, h / 3);
        // 眼柄小眼
        setColor(g, Color.rgb(150, 60, 48));
        g.setLineWidth(2);
        g.strokeLine(cx - h / 4, cy - h / 3, cx - h / 4, cy - h / 3 - 5);
        g.strokeLine(cx + h / 4, cy - h / 3, cx + h / 4, cy - h / 3 - 5);
        setColor(g, Color.WHITE);
        g.fillOval(cx - h / 4 - 4, cy - h / 3 - 11, 8, 8);
        g.fillOval(cx + h / 4 - 4, cy - h / 3 - 11, 8, 8);
        setColor(g, Color.rgb(30, 26, 24));
        g.fillOval(cx - h / 4 - 1, cy - h / 3 - 8, 3, 3);
        g.fillOval(cx + h / 4 - 2, cy - h / 3 - 8, 3, 3);
        if (isElite(type) || isBoss(type)) {
            // 精英/首领：钳口尖刺
            setColor(g, Color.rgb(255, 240, 220));
            g.fillPolygon(new double[]{cx - h - 10, cx - h - 4, cx - h - 10},
                    new double[]{cy - h / 4 - 3, cy - h / 4 - 5, cy - h / 4 + 3}, 3);
            g.fillPolygon(new double[]{cx + h - 2, cx + h + 4, cx + h - 2},
                    new double[]{cy - h / 4 - 3, cy - h / 4 - 5, cy - h / 4 + 3}, 3);
        }
        if (isBoss(type)) {
            // 首领：头顶皇冠叶
            setColor(g, Color.rgb(84, 178, 96));
            g.fillPolygon(new double[]{cx - 8, cx, cx + 8}, new double[]{cy - h - 2, cy - h - 12, cy - h - 2}, 3);
        }
    }

    /** Boss 专属建模分发：每个主题一套独立 Boss 造型；巨人（草原）再整体放大。 */
    private static void drawBoss(GraphicsContext g, MapTheme theme,
                                 int cx, int cy, int size, int tick) {
        switch (theme) {
            case FOREST: drawTreeSanta(g, cx, cy, size, tick); break;
            case GRASSLAND:
                g.save();
                g.translate(cx, cy);
                g.scale(GIANT_BOSS_SCALE, GIANT_BOSS_SCALE);
                g.translate(-cx, -cy);
                drawGrassGiant(g, cx, cy, size, tick);
                g.restore();
                break;
            case TUNDRA: drawSnowYeti(g, cx, cy, size, tick); break;
            case DESERT: drawGoldenScorpion(g, cx, cy, size, tick); break;
            case CASTLE: drawKnight(g, cx, cy, size, tick); break;
            case DARK_FOREST: drawWizard(g, cx, cy, size, tick); break;
            case SWAMP: drawSlimeKing(g, cx, cy, size, tick); break;
            case SPACE: drawVoidLord(g, cx, cy, size, tick); break;
            case VOLCANO: drawFireDragon(g, cx, cy, size, tick); break;
            case ISLAND: drawIronPirate(g, cx, cy, size, tick); break;
            default: drawSlime(g, MonsterType.BOSS, cx, cy, size, tick); break;
        }
    }

    /** 圣诞树人（森林 Boss）：三层雪松树冠 + 彩灯 + 星星 + 树干笑脸，持续冲刺。 */
    private static void drawTreeSanta(GraphicsContext g, int cx, int cy, int size, int tick) {
        int h = size / 2;
        // 树干
        setColor(g, Color.rgb(122, 84, 48));
        g.fillRect(cx - 11, cy + 6, 22, 40);
        setColor(g, Color.rgb(90, 60, 34));
        g.fillRect(cx - 11, cy + 6, 5, 40);
        g.fillRect(cx - 11, cy + 22, 22, 3);
        g.fillRect(cx - 11, cy + 36, 22, 3);
        // 三层雪松（底→顶）
        setColor(g, Color.rgb(28, 118, 62));
        g.fillPolygon(new double[]{cx - 46, cx, cx + 46}, new double[]{cy + 16, cy - 24, cy + 16}, 3);
        setColor(g, Color.rgb(36, 142, 76));
        g.fillPolygon(new double[]{cx - 38, cx, cx + 38}, new double[]{cy - 4, cy - 40, cy - 4}, 3);
        setColor(g, Color.rgb(44, 164, 88));
        g.fillPolygon(new double[]{cx - 30, cx, cx + 30}, new double[]{cy - 24, cy - 56, cy - 24}, 3);
        // 雪边
        setColor(g, Color.rgb(238, 248, 252));
        g.fillRect(cx - 46, cy + 12, 92, 4);
        g.fillRect(cx - 38, cy - 8, 76, 4);
        g.fillRect(cx - 30, cy - 28, 60, 4);
        // 彩灯（红黄蓝循环，隔帧闪烁）
        int glow = tick / 14 % 2;
        setColor(g, Color.rgb(255, 84, 84, 160 / 255.0));
        g.fillOval(cx - 40 + glow * 0, cy - 14, 6, 6);
        setColor(g, Color.rgb(255, 214, 74, 160 / 255.0));
        g.fillOval(cx + 2, cy - 36, 6, 6);
        setColor(g, Color.rgb(96, 180, 255, 160 / 255.0));
        g.fillOval(cx + 22, cy - 18, 6, 6);
        setColor(g, Color.rgb(255, 240, 160));
        g.fillOval(cx + 6, cy + 2, 5, 5);
        // 金星顶
        setColor(g, Color.rgb(255, 214, 74));
        g.fillPolygon(new double[]{cx, cx + 5, cx + 16, cx + 8, cx + 11, cx, cx - 11, cx - 8, cx - 16, cx - 5},
                new double[]{cy - 68, cy - 58, cy - 58, cy - 50, cy - 40, cy - 44, cy - 40, cy - 50, cy - 58, cy - 58}, 10);
        // 树干笑脸
        setColor(g, Color.rgb(34, 22, 14));
        g.fillOval(cx - 7, cy + 12, 5, 6);
        g.fillOval(cx + 2, cy + 12, 5, 6);
        setColor(g, Color.rgb(255, 96, 72));
        g.fillOval(cx - 3, cy + 20, 6, 5);
        setColor(g, Color.rgb(34, 22, 14));
        g.strokeArc(cx - 7, cy + 24, 14, 10, 200, 140, ArcType.OPEN);
    }

    /** 大雪怪（冰原 Boss）：雪白长毛 + 大肚 + 獠牙 + 红眼，跳跃砸地释放三层环形弹。 */
    private static void drawSnowYeti(GraphicsContext g, int cx, int cy, int size, int tick) {
        int h = size / 2;
        // 大脚
        setColor(g, Color.rgb(200, 214, 226));
        g.fillOval(cx - 32, cy + 40, 26, 12);
        g.fillOval(cx + 6, cy + 40, 26, 12);
        // 身体
        setColor(g, Color.rgb(244, 248, 252));
        g.fillOval(cx - 44, cy - 30, 88, 84);
        // 长毛刺（身周）
        setColor(g, Color.rgb(226, 234, 244));
        for (int i = 0; i < 10; i++) {
            double a = tick * 0.06 + Math.PI * 2 * i / 10;
            int sx = (int) (cx + Math.cos(a) * (h + 2));
            int sy = (int) (cy + 8 + Math.sin(a) * (h - 6));
            g.fillPolygon(new double[]{sx - 4, sx + 4, sx + Math.cos(a) * 10},
                    new double[]{sy, sy, sy + Math.sin(a) * 10}, 3);
        }
        // 白肚
        setColor(g, Color.rgb(236, 242, 250));
        g.fillOval(cx - 26, cy + 8, 52, 42);
        // 脸
        setColor(g, Color.rgb(216, 226, 238));
        g.fillOval(cx - 26, cy - 34, 52, 44);
        // 红眼
        setColor(g, Color.rgb(224, 64, 64));
        g.fillOval(cx - 13, cy - 26, 8, 6);
        g.fillOval(cx + 5, cy - 26, 8, 6);
        setColor(g, Color.rgb(40, 20, 20));
        g.fillOval(cx - 11, cy - 25, 4, 4);
        g.fillOval(cx + 7, cy - 25, 4, 4);
        // 大嘴 + 獠牙
        setColor(g, Color.rgb(96, 74, 82));
        g.fillOval(cx - 12, cy - 12, 24, 14);
        setColor(g, Color.rgb(255, 250, 236));
        g.fillPolygon(new double[]{cx - 14, cx - 8, cx - 10}, new double[]{cy - 12, cy - 12, cy - 22}, 3);
        g.fillPolygon(new double[]{cx + 14, cx + 8, cx + 10}, new double[]{cy - 12, cy - 12, cy - 22}, 3);
        // 冰蓝眉脊
        setColor(g, Color.rgb(150, 200, 235));
        g.fillRect(cx - 18, cy - 34, 14, 4);
        g.fillRect(cx + 4, cy - 34, 14, 4);
    }

    /** 巨人（草原 Boss）：岩石巨人身躯，下半身入土只露上半身，占据四分之一地图，眼部发射巨型激光。 */
    private static void drawGrassGiant(GraphicsContext g, int cx, int cy, int size, int tick) {
        int h = size / 2;
        double breathe = Math.sin(tick * 0.05) * 3;
        // 手臂（撑地巨石臂）
        setColor(g, Color.rgb(96, 112, 88));
        g.fillOval(cx - 96, cy - 26, 46, 110);
        g.fillOval(cx + 50, cy - 26, 46, 110);
        // 手掌（撑地爪）
        setColor(g, Color.rgb(78, 92, 70));
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 3; i++) {
                g.fillOval(cx + side * 96 + i * 12, cy + 66, 14, 20);
            }
        }
        // 身躯（胸腹）
        setColor(g, Color.rgb(118, 134, 104));
        g.fillOval(cx - 56, cy - 34 + breathe, 112, 112);
        // 肩部巨石
        setColor(g, Color.rgb(104, 120, 92));
        g.fillOval(cx - 72, cy - 46 + breathe, 54, 48);
        g.fillOval(cx + 18, cy - 46 + breathe, 54, 48);
        // 苔藓斑
        setColor(g, Color.rgb(86, 152, 82));
        g.fillOval(cx - 42, cy + 8 + breathe, 20, 12);
        g.fillOval(cx + 24, cy - 20 + breathe, 14, 9);
        g.fillOval(cx - 8, cy + 40 + breathe, 16, 10);
        // 裂纹
        setColor(g, Color.rgb(70, 84, 62));
        g.setLineWidth(2);
        g.strokeLine(cx - 30, cy - 28 + breathe, cx - 18, cy - 8 + breathe);
        g.strokeLine(cx - 18, cy - 8 + breathe, cx - 28, cy + 8 + breathe);
        g.strokeLine(cx + 24, cy + 20 + breathe, cx + 34, cy + 38 + breathe);
        // 头部
        setColor(g, Color.rgb(124, 140, 108));
        g.fillOval(cx - 54, cy - 118 + breathe, 108, 92);
        // 发光巨眼（激光眼）
        double eyeGlow = 0.75 + Math.sin(tick * 0.2) * 0.25;
        setColor(g, Color.rgb(140, 255, 120, eyeGlow));
        g.fillOval(cx - 36, cy - 100 + breathe, 30, 18);
        g.fillOval(cx + 6, cy - 100 + breathe, 30, 18);
        setColor(g, Color.rgb(240, 255, 230, eyeGlow));
        g.fillOval(cx - 30, cy - 96 + breathe, 18, 10);
        g.fillOval(cx + 12, cy - 96 + breathe, 18, 10);
        // 大嘴 + 石牙
        setColor(g, Color.rgb(70, 84, 62));
        g.fillRect(cx - 30, cy - 66 + breathe, 60, 16);
        setColor(g, Color.rgb(214, 224, 200));
        for (int i = 0; i < 5; i++) {
            g.fillPolygon(new double[]{cx - 26 + i * 13, cx - 21 + i * 13, cx - 26 + i * 13},
                    new double[]{cy - 66 + breathe, cy - 66 + breathe, cy - 56 + breathe}, 3);
        }
        // 鼻孔
        setColor(g, Color.rgb(80, 94, 70));
        g.fillOval(cx - 12, cy - 48 + breathe, 8, 5);
        g.fillOval(cx + 4, cy - 48 + breathe, 8, 5);
        // 腰部土堆过渡（下半身入土）
        setColor(g, Color.rgb(120, 96, 62));
        g.fillArc(cx - 84, cy + 58, 168, 44, 0, 180, ArcType.CHORD);
        setColor(g, Color.rgb(146, 118, 76));
        g.fillArc(cx - 60, cy + 66, 120, 30, 0, 180, ArcType.CHORD);
    }

    /** 巨型黄金毒蝎（沙漠 Boss）：金色甲壳 + 双巨钳 + 宝石 + 毒尾，高频弹幕快速移动。 */
    private static void drawGoldenScorpion(GraphicsContext g, int cx, int cy, int size, int tick) {
        int h = size / 2;
        double pinch = Math.sin(tick * 0.18) * 8;
        // 侧足
        setColor(g, Color.rgb(196, 152, 34));
        for (int i = 0; i < 3; i++) {
            g.setLineWidth(3);
            g.strokeLine(cx - 16 + i * 12, cy + 16, cx - 30 + i * 14, cy + 34);
            g.strokeLine(cx + 6 + i * 10, cy + 16, cx - 2 + i * 12, cy + 34);
        }
        // 甲壳三段（后→前）
        setColor(g, Color.rgb(232, 178, 44));
        g.fillOval(cx - 42, cy - 14, 40, 28);
        setColor(g, Color.rgb(244, 196, 58));
        g.fillOval(cx - 24, cy - 20, 44, 34);
        setColor(g, Color.rgb(252, 212, 76));
        g.fillOval(cx - 2, cy - 22, 42, 38);
        // 甲壳纹
        setColor(g, Color.rgb(196, 152, 34));
        g.setLineWidth(2);
        g.strokeArc(cx - 20, cy - 12, 34, 22, 200, 140, ArcType.OPEN);
        g.strokeArc(cx + 2, cy - 16, 32, 24, 200, 140, ArcType.OPEN);
        // 红宝石
        setColor(g, Color.rgb(232, 60, 74));
        g.fillPolygon(new double[]{cx - 8, cx - 2, cx + 4, cx + 2, cx - 4},
                new double[]{cy - 22, cy - 28, cy - 22, cy - 18, cy - 18}, 5);
        setColor(g, Color.rgb(255, 150, 150));
        g.fillOval(cx - 5, cy - 24, 3, 3);
        // 双巨钳（开合）
        setColor(g, Color.rgb(244, 196, 58));
        g.fillOval(cx + 18, cy - 18, 20, 20);
        g.fillOval(cx + 30, cy + 2 - pinch / 2, 14, 14 + pinch);
        g.fillPolygon(new double[]{cx + 42, cx + 52, cx + 42}, new double[]{cy - 6 - pinch, cy - 2, cy + 2 + pinch}, 3);
        g.fillOval(cx + 12, cy + 12, 18, 18);
        g.fillPolygon(new double[]{cx + 20, cx + 32, cx + 20}, new double[]{cy + 22, cy + 26, cy + 30}, 3);
        // 毒尾（上翘弯尾 + 毒针）
        setColor(g, Color.rgb(232, 178, 44));
        for (int i = 0; i < 5; i++) {
            g.fillOval(cx - 52 - i * 8, cy - 4 - i * 7, 14, 12);
        }
        setColor(g, Color.rgb(170, 60, 190));
        g.fillPolygon(new double[]{cx - 84, cx - 74, cx - 88}, new double[]{cy - 36, cy - 40, cy - 40}, 3);
        // 金眼
        setColor(g, Color.rgb(60, 34, 10));
        g.fillOval(cx + 18, cy - 12, 6, 6);
        g.fillOval(cx + 28, cy - 12, 6, 6);
        setColor(g, Color.rgb(255, 236, 160));
        g.fillOval(cx + 19, cy - 11, 3, 3);
        g.fillOval(cx + 29, cy - 11, 3, 3);
    }

    /** 大骑士（城堡 Boss）：银铠板甲 + 红缨头盔 + 披风 + 巨剑，三连斩逐步逼近。 */
    private static void drawKnight(GraphicsContext g, int cx, int cy, int size, int tick) {
        int h = size / 2;
        // 披风
        setColor(g, Color.rgb(156, 40, 44));
        g.fillPolygon(new double[]{cx - 8, cx + 26, cx + 10}, new double[]{cy - 24, cy - 26, cy + 34}, 3);
        // 腿甲
        setColor(g, Color.rgb(160, 168, 182));
        g.fillRect(cx - 24, cy + 22, 20, 24);
        g.fillRect(cx + 4, cy + 22, 20, 24);
        // 身体板甲
        setColor(g, Color.rgb(206, 214, 228));
        g.fillRoundRect(cx - 30, cy - 16, 60, 42, 10, 10);
        setColor(g, Color.rgb(150, 160, 176));
        g.setLineWidth(2);
        g.strokeLine(cx - 30, cy - 2, cx + 30, cy - 2);
        g.strokeLine(cx, cy - 16, cx, cy + 26);
        // 肩甲
        setColor(g, Color.rgb(178, 188, 204));
        g.fillOval(cx - 36, cy - 22, 18, 16);
        g.fillOval(cx + 18, cy - 22, 18, 16);
        // 头盔
        setColor(g, Color.rgb(196, 204, 220));
        g.fillOval(cx - 26, cy - 48, 52, 44);
        // T 形面缝
        setColor(g, Color.rgb(40, 44, 54));
        g.fillRect(cx - 2, cy - 40, 4, 12);
        g.fillRect(cx - 12, cy - 34, 24, 4);
        // 红缨
        setColor(g, Color.rgb(220, 60, 60));
        double plume = Math.sin(tick * 0.12) * 4;
        g.fillPolygon(new double[]{cx - 4, cx + 4, cx + plume}, new double[]{cy - 52, cy - 52, cy - 66}, 3);
        // 巨剑（斜持）
        setColor(g, Color.rgb(120, 126, 138));
        g.fillPolygon(new double[]{cx + 34, cx + 40, cx + 60}, new double[]{cy + 8, cy - 18, cy + 34}, 3);
        setColor(g, Color.rgb(224, 232, 242));
        g.fillPolygon(new double[]{cx + 38, cx + 42, cx + 58}, new double[]{cy - 16, cy - 26, cy + 6}, 3);
        setColor(g, Color.rgb(216, 168, 66));
        g.fillRect(cx + 32, cy - 4, 10, 4);
        // 盾（左臂）
        setColor(g, Color.rgb(150, 158, 172));
        g.fillRoundRect(cx - 44, cy - 8, 14, 26, 6, 6);
        setColor(g, Color.rgb(216, 168, 66));
        g.fillOval(cx - 40, cy + 0, 6, 6);
    }

    /** 大巫师（黑森林 Boss）：深紫法袍 + 宽檐帽 + 白胡 + 骷髅法杖，召唤三陨石。 */
    private static void drawWizard(GraphicsContext g, int cx, int cy, int size, int tick) {
        int h = size / 2;
        // 法袍
        setColor(g, Color.rgb(84, 48, 118));
        g.fillPolygon(new double[]{cx - 38, cx + 38, cx + 20, cx - 20}, new double[]{cy - 16, cy - 16, cy + 48, cy + 48}, 4);
        setColor(g, Color.rgb(196, 160, 72));
        g.setLineWidth(2);
        g.strokeLine(cx - 36, cy - 14, cx - 18, cy + 46);
        g.strokeLine(cx + 36, cy - 14, cx + 18, cy + 46);
        // 帽檐 + 尖帽
        setColor(g, Color.rgb(102, 60, 140));
        g.fillOval(cx - 32, cy - 34, 64, 14);
        double bend = Math.sin(tick * 0.08) * 4;
        g.fillPolygon(new double[]{cx - 20, cx + 20, cx + 6 + bend}, new double[]{cy - 34, cy - 34, cy - 70}, 3);
        setColor(g, Color.rgb(196, 160, 72));
        g.fillRect(cx - 20, cy - 36, 40, 3);
        setColor(g, Color.rgb(240, 226, 170));
        g.fillOval(cx + 2 + bend, cy - 68, 5, 5);
        // 脸（暗绿）
        setColor(g, Color.rgb(110, 138, 92));
        g.fillOval(cx - 22, cy - 26, 44, 34);
        // 发光绿眼
        setColor(g, Color.rgb(150, 255, 120, 0.8 + Math.sin(tick * 0.15) * 0.2));
        g.fillOval(cx - 14, cy - 18, 7, 6);
        g.fillOval(cx + 7, cy - 18, 7, 6);
        // 白胡
        setColor(g, Color.rgb(232, 232, 224));
        g.fillPolygon(new double[]{cx - 16, cx + 16, cx + 8, cx - 8}, new double[]{cy - 10, cy - 10, cy + 10, cy + 10}, 4);
        // 骷髅法杖
        setColor(g, Color.rgb(126, 88, 46));
        g.setLineWidth(5);
        g.strokeLine(cx + 30, cy + 30, cx + 40, cy - 28);
        setColor(g, Color.rgb(236, 230, 214));
        g.fillOval(cx + 40, cy - 40, 14, 12);
        setColor(g, Color.rgb(60, 50, 60));
        g.fillOval(cx + 43, cy - 37, 4, 4);
        g.fillOval(cx + 48, cy - 37, 4, 4);
        // 法杖紫宝石
        setColor(g, Color.rgb(180, 110, 255, 0.75 + Math.sin(tick * 0.2) * 0.25));
        g.fillOval(cx + 40, cy - 50, 10, 10);
    }

    /** 大史莱姆王（沼泽 Boss）：巨型毒果冻 + 金王冠 + 上升气泡，连续跳砸释放环形弹。 */
    private static void drawSlimeKing(GraphicsContext g, int cx, int cy, int size, int tick) {
        int h = size / 2;
        double wobble = Math.sin(tick * 0.12) * 4;
        // 主体
        setColor(g, Color.rgb(96, 196, 104));
        g.fillOval(cx - 46, cy - 16 + wobble, 92 + wobble / 2, 74);
        setColor(g, Color.rgb(120, 220, 122));
        g.fillOval(cx - 34, cy - 26 + wobble, 64, 40);
        // 高光
        setColor(g, Color.rgb(230, 255, 226, 200 / 255.0));
        g.fillOval(cx - 30, cy - 18 + wobble, 16, 10);
        // 内部消化物（小骨头）
        setColor(g, Color.rgb(228, 232, 220, 180 / 255.0));
        g.fillRect(cx - 14, cy + 18, 12, 4);
        g.fillRect(cx - 10, cy + 14, 4, 12);
        g.fillRect(cx + 4, cy + 26, 10, 3);
        // 眯眼 + 大嘴
        setColor(g, Color.rgb(28, 74, 40));
        g.strokeArc(cx - 20, cy - 14 + wobble, 12, 8, 20, 140, ArcType.OPEN);
        g.strokeArc(cx + 8, cy - 14 + wobble, 12, 8, 20, 140, ArcType.OPEN);
        g.setLineWidth(3);
        g.strokeArc(cx - 20, cy - 4 + wobble, 40, 18, 200, 140, ArcType.OPEN);
        // 毒斑
        setColor(g, Color.rgb(190, 236, 88));
        g.fillOval(cx + 26, cy + 10, 8, 6);
        g.fillOval(cx - 38, cy + 14, 6, 5);
        // 金王冠
        setColor(g, Color.rgb(244, 198, 66));
        g.fillPolygon(new double[]{cx - 20, cx - 20, cx - 12, cx - 6, cx, cx + 6, cx + 12, cx + 20, cx + 20},
                new double[]{cy - 30 + wobble, cy - 42 + wobble, cy - 34 + wobble, cy - 44 + wobble, cy - 34 + wobble,
                        cy - 44 + wobble, cy - 34 + wobble, cy - 42 + wobble, cy - 30 + wobble}, 9);
        setColor(g, Color.rgb(232, 60, 74));
        g.fillOval(cx - 3, cy - 38 + wobble, 6, 6);
        // 上升气泡
        for (int i = 0; i < 3; i++) {
            double rise = (tick * 0.7 + i * 37) % 90;
            setColor(g, Color.rgb(190, 236, 160, (1 - rise / 90) * 180 / 255.0));
            g.fillOval(cx + 24 + i * 10, cy + 8 - rise, 5 + i * 2, 5 + i * 2);
        }
    }

    /** 虚空领主（太空 Boss）：紫黑斗篷 + 旋转虚空核心 + 空洞白瞳，召唤虚空裂缝。 */
    private static void drawVoidLord(GraphicsContext g, int cx, int cy, int size, int tick) {
        int h = size / 2;
        // 残破斗篷
        setColor(g, Color.rgb(46, 30, 74));
        g.fillPolygon(new double[]{cx - 40, cx + 40, cx + 30, cx + 16, cx - 16, cx - 30},
                new double[]{cy - 18, cy - 18, cy + 44, cy + 32, cy + 32, cy + 44}, 6);
        // 身体
        setColor(g, Color.rgb(70, 48, 104));
        g.fillOval(cx - 30, cy - 24, 60, 60);
        // 虚空核心（黑洞 + 旋转吸积环）
        setColor(g, Color.rgb(24, 14, 40));
        g.fillOval(cx - 16, cy - 8, 32, 32);
        setColor(g, Color.rgb(160, 110, 255, 180 / 255.0));
        for (int i = 0; i < 3; i++) {
            double a = tick * 0.14 + Math.PI * 2 * i / 3;
            double ringX = cx + Math.cos(a) * 22;
            double ringY = cy + 8 + Math.sin(a) * 22;
            g.setLineWidth(3);
            g.strokeOval(ringX - 9, ringY - 9, 18, 18);
        }
        setColor(g, Color.rgb(220, 190, 255));
        g.fillOval(cx - 5, cy + 3, 10, 10);
        // 空洞白瞳
        setColor(g, Color.rgb(246, 244, 250));
        g.fillOval(cx - 18, cy - 20, 11, 11);
        g.fillOval(cx + 7, cy - 20, 11, 11);
        setColor(g, Color.rgb(30, 20, 50));
        g.fillOval(cx - 15, cy - 17, 5, 5);
        g.fillOval(cx + 10, cy - 17, 5, 5);
        // 白爪
        setColor(g, Color.rgb(226, 222, 236));
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 3; i++) {
                g.setLineWidth(3);
                g.strokeLine(cx + side * 28, cy - 4 + i * 5, cx + side * 40, cy + 2 + i * 7);
            }
        }
    }

    /** 赤焰飞龙（火山 Boss）：深红龙身 + 扇动双翼 + 金角金瞳 + 喷火口，五连大火球。 */
    private static void drawFireDragon(GraphicsContext g, int cx, int cy, int size, int tick) {
        int h = size / 2;
        double flap = Math.sin(tick * 0.15) * 10;
        // 双翼（膜 + 骨）
        setColor(g, Color.rgb(158, 44, 40));
        g.fillPolygon(new double[]{cx - 30, cx - 84, cx - 46}, new double[]{cy - 18, cy - 46 - flap, cy + 6}, 3);
        g.fillPolygon(new double[]{cx + 30, cx + 84, cx + 46}, new double[]{cy - 18, cy - 46 - flap, cy + 6}, 3);
        setColor(g, Color.rgb(120, 32, 30));
        g.setLineWidth(2);
        g.strokeLine(cx - 30, cy - 18, cx - 62, cy - 34 - flap);
        g.strokeLine(cx + 30, cy - 18, cx + 62, cy - 34 - flap);
        // 身体
        setColor(g, Color.rgb(196, 52, 46));
        g.fillOval(cx - 38, cy - 16, 76, 58);
        // 熔岩纹
        setColor(g, Color.rgb(255, 128, 40, 0.65 + Math.sin(tick * 0.2) * 0.25));
        g.setLineWidth(3);
        g.strokeArc(cx - 24, cy - 4, 48, 26, 20, 140, ArcType.OPEN);
        g.strokeArc(cx - 16, cy + 4, 32, 18, 200, 140, ArcType.OPEN);
        // 龙角（金）
        setColor(g, Color.rgb(244, 198, 66));
        g.fillPolygon(new double[]{cx - 22, cx - 30, cx - 16}, new double[]{cy - 30, cy - 52, cy - 30}, 3);
        g.fillPolygon(new double[]{cx + 22, cx + 30, cx + 16}, new double[]{cy - 30, cy - 52, cy - 30}, 3);
        // 头
        setColor(g, Color.rgb(210, 60, 52));
        g.fillOval(cx - 22, cy - 34, 44, 32);
        // 金色竖瞳
        setColor(g, Color.rgb(255, 226, 110));
        g.fillOval(cx - 13, cy - 26, 8, 6);
        g.fillOval(cx + 5, cy - 26, 8, 6);
        setColor(g, Color.rgb(40, 20, 10));
        g.fillRect(cx - 11, cy - 25, 2, 5);
        g.fillRect(cx + 7, cy - 25, 2, 5);
        // 喷火口（下颚火光）
        setColor(g, Color.rgb(255, 170, 40, 0.7 + Math.sin(tick * 0.3) * 0.3));
        g.fillPolygon(new double[]{cx - 10, cx + 10, cx}, new double[]{cy - 8, cy - 8, cy + 2}, 3);
        // 尾
        setColor(g, Color.rgb(196, 52, 46));
        g.fillPolygon(new double[]{cx + 34, cx + 60, cx + 36}, new double[]{cy + 8, cy - 6, cy + 22}, 3);
        setColor(g, Color.rgb(255, 150, 50));
        g.fillPolygon(new double[]{cx + 56, cx + 64, cx + 58}, new double[]{cy - 6, cy - 4, cy + 4}, 3);
    }

    /** 钢铁海盗（海岛 Boss）：船长帽 + 骷髅徽 + 独眼罩 + 铁钩手 + 铁甲，钩锁拉人斩击。 */
    private static void drawIronPirate(GraphicsContext g, int cx, int cy, int size, int tick) {
        int h = size / 2;
        // 腿（铁腿）
        setColor(g, Color.rgb(96, 104, 116));
        g.fillRect(cx - 22, cy + 20, 18, 26);
        g.fillRect(cx + 4, cy + 20, 18, 26);
        setColor(g, Color.rgb(60, 64, 72));
        g.fillRect(cx - 24, cy + 42, 22, 6);
        g.fillRect(cx + 2, cy + 42, 22, 6);
        // 身体铁甲
        setColor(g, Color.rgb(120, 128, 142));
        g.fillRoundRect(cx - 28, cy - 14, 56, 40, 8, 8);
        // 铆钉
        setColor(g, Color.rgb(60, 64, 72));
        g.fillOval(cx - 22, cy - 10, 4, 4);
        g.fillOval(cx + 18, cy - 10, 4, 4);
        g.fillOval(cx - 22, cy + 10, 4, 4);
        g.fillOval(cx + 18, cy + 10, 4, 4);
        // 红巾腰带
        setColor(g, Color.rgb(196, 44, 48));
        g.fillRect(cx - 28, cy + 18, 56, 7);
        // 铁钩手（左）
        setColor(g, Color.rgb(150, 158, 172));
        g.fillRect(cx - 44, cy - 6, 16, 6);
        double hookSwing = Math.sin(tick * 0.1) * 3;
        g.setLineWidth(5);
        g.strokeArc(cx - 54, cy - 10, 20, 18 + hookSwing, 90, 200, ArcType.OPEN);
        // 铁拳（右）
        setColor(g, Color.rgb(150, 158, 172));
        g.fillRect(cx + 28, cy - 8, 14, 8);
        g.fillOval(cx + 38, cy - 10, 12, 12);
        // 头
        setColor(g, Color.rgb(228, 186, 146));
        g.fillOval(cx - 20, cy - 42, 40, 36);
        // 独眼罩（右眼）
        setColor(g, Color.rgb(30, 28, 34));
        g.fillRect(cx + 4, cy - 40, 14, 5);
        g.fillRect(cx + 4, cy - 30, 10, 10);
        // 左眼 + 疤
        setColor(g, Color.rgb(40, 30, 26));
        g.fillOval(cx - 12, cy - 34, 5, 6);
        g.setLineWidth(2);
        g.strokeLine(cx - 18, cy - 38, cx - 12, cy - 31);
        // 嘴（奸笑）
        g.strokeArc(cx - 8, cy - 22, 16, 10, 200, 140, ArcType.OPEN);
        // 船长帽
        setColor(g, Color.rgb(40, 38, 48));
        g.fillPolygon(new double[]{cx - 24, cx + 24, cx + 12, cx - 12}, new double[]{cy - 40, cy - 40, cy - 54, cy - 54}, 4);
        g.fillRect(cx - 32, cy - 42, 64, 5);
        // 骷髅徽
        setColor(g, Color.rgb(240, 238, 230));
        g.fillOval(cx - 5, cy - 50, 10, 8);
        g.fillRect(cx - 4, cy - 46, 8, 3);
        setColor(g, Color.rgb(40, 38, 48));
        g.fillOval(cx - 3, cy - 48, 2, 2);
        g.fillOval(cx + 1, cy - 48, 2, 2);
        // 帽羽
        setColor(g, Color.rgb(220, 60, 60));
        g.fillPolygon(new double[]{cx + 10, cx + 16, cx + 14}, new double[]{cy - 44, cy - 60, cy - 44}, 3);
    }
}
