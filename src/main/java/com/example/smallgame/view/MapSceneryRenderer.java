package com.example.smallgame.view;

import com.example.smallgame.model.entity.DungeonMap;
import com.example.smallgame.model.entity.MapTheme;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import java.util.List;

/**
 * 地图场景渲染器：按当前层主题绘制地板纹理与场景装饰（纯视觉、无碰撞）。
 * 装饰数据由 DungeonMap 在生成关卡时以确定性随机数产生，本类只负责按主题样式绘制，
 * 部分装饰带轻微环境动画（火焰摇曳、星光闪烁、熔岩脉动等），由逻辑帧 tick 驱动。
 * <p>
 * 引用文件：model.entity（DungeonMap、MapTheme）。
 * 被 GameMainView（背景、装饰与障碍物绘制）调用。
 */
public final class MapSceneryRenderer {

    /** 工具类，禁止实例化。 */
    private MapSceneryRenderer() {
    }

    /** 地板纹理：随机散布的斑点、短线与亮斑，颜色从主题地板色派生（墙体在其上，会被盖住）。 */
    public static void drawFloorTextures(GraphicsContext g, MapTheme theme,
                                         List<DungeonMap.Decoration> textures) {
        if (textures.isEmpty() || theme == MapTheme.DEFAULT) {
            return;
        }
        Color dot = Palette.color(theme.floorColor()).deriveColor(0, 1, 0.55, 1);
        Color line = Palette.color(theme.floorColor()).deriveColor(0, 1, 0.82, 1);
        Color bright = Palette.color(theme.floorColor()).deriveColor(0, 0.85, 1.4, 1);
        g.setLineWidth(1);
        for (DungeonMap.Decoration texture : textures) {
            switch (texture.kind) {
                case 0:
                    setColor(g, dot);
                    g.fillRect(texture.x, texture.y, 3, 3);
                    break;
                case 1:
                    setColor(g, line);
                    g.fillRect(texture.x, texture.y, 7, 2);
                    break;
                default:
                    setColor(g, bright);
                    g.fillRect(texture.x, texture.y, 2, 2);
                    break;
            }
        }
    }

    /** 场景装饰：按主题绘制专属物件，tick 驱动轻微环境动画。 */
    public static void drawDecorations(GraphicsContext g, MapTheme theme,
                                       List<DungeonMap.Decoration> decorations, int tick) {
        g.setLineWidth(1);
        for (DungeonMap.Decoration decoration : decorations) {
            switch (theme) {
                case FOREST:
                    drawForest(g, decoration);
                    break;
                case GRASSLAND:
                    drawGrassland(g, decoration);
                    break;
                case TUNDRA:
                    drawTundra(g, decoration, tick);
                    break;
                case DESERT:
                    drawDesert(g, decoration);
                    break;
                case CASTLE:
                    drawCastle(g, decoration, tick);
                    break;
                case DARK_FOREST:
                    drawDarkForest(g, decoration, tick);
                    break;
                case SWAMP:
                    drawSwamp(g, decoration, tick);
                    break;
                case SPACE:
                    drawSpace(g, decoration, tick);
                    break;
                case VOLCANO:
                    drawVolcano(g, decoration, tick);
                    break;
                case ISLAND:
                    drawIsland(g, decoration);
                    break;
                default:
                    break;
            }
        }
    }

    /** 墙体装饰：叠加在墙体本体之上的主题专属装饰（纯视觉、无碰撞），
     *  装饰位置由墙体坐标确定性伪随机产生，同一面墙每次进入外观一致；斜墙不装饰。 */
    public static void drawWallDecorations(GraphicsContext g, MapTheme theme,
                                           List<DungeonMap.Wall> walls, int tick) {
        if (theme == MapTheme.DEFAULT) {
            return;
        }
        g.setLineWidth(1);
        for (DungeonMap.Wall wall : walls) {
            if (wall.isDiagonal()) {
                continue;
            }
            switch (theme) {
                case FOREST:
                    drawForestWall(g, wall);
                    break;
                case GRASSLAND:
                    drawGrasslandWall(g, wall);
                    break;
                case TUNDRA:
                    drawTundraWall(g, wall, tick);
                    break;
                case DESERT:
                    drawDesertWall(g, wall);
                    break;
                case CASTLE:
                    drawCastleWall(g, wall, tick);
                    break;
                case DARK_FOREST:
                    drawDarkForestWall(g, wall, tick);
                    break;
                case SWAMP:
                    drawSwampWall(g, wall);
                    break;
                case SPACE:
                    drawSpaceWall(g, wall, tick);
                    break;
                case VOLCANO:
                    drawVolcanoWall(g, wall, tick);
                    break;
                case ISLAND:
                    drawIslandWall(g, wall);
                    break;
                default:
                    break;
            }
        }
    }

    /** 森林墙体装饰：墙顶垂挂藤蔓与墙脚苔藓斑。 */
    private static void drawForestWall(GraphicsContext g, DungeonMap.Wall w) {
        boolean horizontal = w.width >= w.height;
        int vines = horizontal ? 2 + (w.x * 7 + w.y * 13) % 3 : 1 + (w.x * 7 + w.y * 13) % 2;
        for (int i = 0; i < vines; i++) {
            int vx = w.x + 10 + (w.x * 31 + w.y * 17 + i * 97) % Math.max(1, w.width - 20);
            int len = 9 + (w.x * 11 + w.y * 7 + i * 53) % 12;
            setColor(g, Color.rgb(52, 104, 56));
            g.strokeLine(vx, w.y + 2, vx, w.y + len);
            g.strokeLine(vx, w.y + len - 4, vx - 3, w.y + len - 8);
            g.strokeLine(vx, w.y + len - 4, vx + 3, w.y + len - 8);
        }
        int moss = 1 + (w.x * 5 + w.y * 3) % 3;
        for (int i = 0; i < moss; i++) {
            int mx = w.x + 6 + (w.x * 17 + w.y * 31 + i * 41) % Math.max(1, w.width - 12);
            int my = w.y + w.height - 5 - (w.x * 7 + w.y * 11 + i * 29) % 6;
            setColor(g, Color.rgb(52, 96, 50, 0.55));
            g.fillOval(mx, my - 2, 9, 5);
        }
    }

    /** 草原墙体装饰：墙头生长的草叶与墙脚小花。 */
    private static void drawGrasslandWall(GraphicsContext g, DungeonMap.Wall w) {
        boolean horizontal = w.width >= w.height;
        int tufts = horizontal ? 2 + (w.x * 7 + w.y * 13) % 3 : 1;
        for (int i = 0; i < tufts; i++) {
            int gx = w.x + 10 + (w.x * 29 + w.y * 19 + i * 83) % Math.max(1, w.width - 20);
            setColor(g, Color.rgb(96, 138, 66));
            g.strokeLine(gx, w.y - 1, gx - 3, w.y - 8);
            g.strokeLine(gx, w.y - 1, gx, w.y - 11);
            g.strokeLine(gx, w.y - 1, gx + 3, w.y - 7);
        }
        int flowers = 1 + (w.x * 5 + w.y * 3) % 2;
        for (int i = 0; i < flowers; i++) {
            int fx = w.x + 8 + (w.x * 13 + w.y * 23 + i * 61) % Math.max(1, w.width - 16);
            int fy = w.y + w.height - 3;
            setColor(g, Color.rgb(235, 240, 220));
            g.fillRect(fx - 2, fy - 1, 1, 2);
            g.fillRect(fx + 1, fy - 1, 1, 2);
            setColor(g, Color.rgb(240, 200, 70));
            g.fillRect(fx - 1, fy - 1, 2, 2);
        }
    }

    /** 冰原墙体装饰：墙顶垂挂冰柱与墙脚雪堆，冰柱带呼吸反光。 */
    private static void drawTundraWall(GraphicsContext g, DungeonMap.Wall w, int tick) {
        boolean horizontal = w.width >= w.height;
        int icicles = horizontal ? 2 + (w.x * 7 + w.y * 13) % 3 : 1 + (w.x * 7 + w.y * 13) % 2;
        for (int i = 0; i < icicles; i++) {
            int ix = w.x + 10 + (w.x * 31 + w.y * 17 + i * 97) % Math.max(1, w.width - 20);
            int len = 7 + (w.x * 11 + w.y * 7 + i * 53) % 9;
            double shine = 0.6 + 0.4 * Math.abs(Math.sin(tick * 0.06 + ix * 0.02));
            setColor(g, Color.rgb(190, 224, 248, shine));
            g.beginPath();
            g.moveTo(ix - 3, w.y + 1);
            g.lineTo(ix + 3, w.y + 1);
            g.lineTo(ix, w.y + len);
            g.closePath();
            g.fill();
        }
        int drifts = 1 + (w.x * 5 + w.y * 3) % 2;
        for (int i = 0; i < drifts; i++) {
            int sx = w.x + 8 + (w.x * 17 + w.y * 31 + i * 41) % Math.max(1, w.width - 16);
            setColor(g, Color.rgb(205, 230, 248, 0.8));
            g.fillOval(sx, w.y + w.height - 4, 12, 5);
        }
    }

    /** 沙漠墙体装饰：墙脚沙堆与墙身风蚀沙纹。 */
    private static void drawDesertWall(GraphicsContext g, DungeonMap.Wall w) {
        int dunes = 1 + (w.x * 5 + w.y * 3) % 3;
        for (int i = 0; i < dunes; i++) {
            int dx = w.x + 8 + (w.x * 17 + w.y * 31 + i * 41) % Math.max(1, w.width - 16);
            setColor(g, Color.rgb(196, 164, 108, 0.85));
            g.fillOval(dx, w.y + w.height - 5, 14, 6);
        }
        int streaks = 1 + (w.x * 7 + w.y * 11) % 2;
        for (int i = 0; i < streaks; i++) {
            int sx = w.x + 10 + (w.x * 23 + w.y * 13 + i * 71) % Math.max(1, w.width - 20);
            int sy = w.y + 12 + (w.x * 3 + w.y * 29 + i * 47) % Math.max(1, w.height - 26);
            setColor(g, Color.rgb(210, 178, 118, 0.45));
            g.fillRect(sx, sy, 9, 2);
        }
    }

    /** 城堡墙体装饰：墙顶挂旗（随风微摆）与墙身铆钉亮点。 */
    private static void drawCastleWall(GraphicsContext g, DungeonMap.Wall w, int tick) {
        boolean horizontal = w.width >= w.height;
        int flags = horizontal ? 1 + (w.x * 7 + w.y * 13) % 3 : 1;
        for (int i = 0; i < flags; i++) {
            int fx = w.x + 12 + (w.x * 31 + w.y * 17 + i * 97) % Math.max(1, w.width - 24);
            setColor(g, Color.rgb(64, 58, 52));
            g.strokeLine(fx, w.y, fx, w.y - 9);
            double sway = Math.sin(tick * 0.12 + fx * 0.05) * 1.5;
            setColor(g, Color.rgb(180, 62, 54));
            g.beginPath();
            g.moveTo(fx, w.y - 9);
            g.lineTo(fx + 8 + sway, w.y - 6);
            g.lineTo(fx, w.y - 3);
            g.closePath();
            g.fill();
        }
        int rivets = 2 + (w.x * 5 + w.y * 3) % 3;
        for (int i = 0; i < rivets; i++) {
            int rx = w.x + 6 + (w.x * 17 + w.y * 31 + i * 41) % Math.max(1, w.width - 12);
            int ry = w.y + 8 + (w.x * 13 + w.y * 7 + i * 29) % Math.max(1, w.height - 16);
            setColor(g, Color.rgb(206, 202, 216, 0.5));
            g.fillRect(rx, ry, 2, 2);
        }
    }

    /** 黑森林墙体装饰：墙顶垂挂枯藤与墙身荧光苔点。 */
    private static void drawDarkForestWall(GraphicsContext g, DungeonMap.Wall w, int tick) {
        boolean horizontal = w.width >= w.height;
        int vines = horizontal ? 1 + (w.x * 7 + w.y * 13) % 3 : 1;
        for (int i = 0; i < vines; i++) {
            int vx = w.x + 10 + (w.x * 31 + w.y * 17 + i * 97) % Math.max(1, w.width - 20);
            int len = 10 + (w.x * 11 + w.y * 7 + i * 53) % 13;
            setColor(g, Color.rgb(52, 44, 36));
            g.strokeLine(vx, w.y + 2, vx + 2, w.y + len / 2);
            g.strokeLine(vx + 2, w.y + len / 2, vx - 1, w.y + len);
        }
        int glows = 1 + (w.x * 5 + w.y * 3) % 3;
        for (int i = 0; i < glows; i++) {
            int gx = w.x + 6 + (w.x * 17 + w.y * 31 + i * 41) % Math.max(1, w.width - 12);
            int gy = w.y + 8 + (w.x * 13 + w.y * 7 + i * 29) % Math.max(1, w.height - 16);
            // 发光强度按正弦绝对值脉动
            double glow = 0.3 + 0.35 * Math.abs(Math.sin(tick * 0.07 + gx * 0.05));
            setColor(g, Color.rgb(96, 150, 88, glow));
            g.fillRect(gx, gy, 3, 3);
        }
    }

    /** 沼泽墙体装饰：墙身苔藓大斑与墙顶垂下的水草藤须。 */
    private static void drawSwampWall(GraphicsContext g, DungeonMap.Wall w) {
        int moss = 1 + (w.x * 5 + w.y * 3) % 3;
        for (int i = 0; i < moss; i++) {
            int mx = w.x + 6 + (w.x * 17 + w.y * 31 + i * 41) % Math.max(1, w.width - 12);
            int my = w.y + 8 + (w.x * 13 + w.y * 7 + i * 29) % Math.max(1, w.height - 16);
            setColor(g, Color.rgb(64, 96, 52, 0.5));
            g.fillOval(mx, my, 10, 6);
            g.fillOval(mx + 5, my + 3, 8, 5);
        }
        boolean horizontal = w.width >= w.height;
        int fronds = horizontal ? 1 + (w.x * 7 + w.y * 13) % 2 : 1;
        for (int i = 0; i < fronds; i++) {
            int fx = w.x + 10 + (w.x * 31 + w.y * 17 + i * 97) % Math.max(1, w.width - 20);
            int len = 10 + (w.x * 11 + w.y * 7 + i * 53) % 10;
            setColor(g, Color.rgb(84, 100, 56));
            g.strokeLine(fx, w.y + 2, fx + 3, w.y + len / 2);
            g.strokeLine(fx + 3, w.y + len / 2, fx - 1, w.y + len);
        }
    }

    /** 太空墙体装饰：墙身发光灯条（闪烁）与四角铆钉亮点。 */
    private static void drawSpaceWall(GraphicsContext g, DungeonMap.Wall w, int tick) {
        boolean horizontal = w.width >= w.height;
        double flicker = 0.55 + 0.45 * Math.abs(Math.sin(tick * 0.09 + w.x * 0.03));
        if (horizontal) {
            int ly = w.y + w.height / 2 - 2;
            setColor(g, Color.rgb(120, 220, 255, flicker));
            g.fillRect(w.x + 6, ly, w.width - 12, 3);
            setColor(g, Color.rgb(220, 250, 255, flicker * 0.8));
            g.fillRect(w.x + 6, ly, w.width - 12, 1);
        } else {
            int lx = w.x + w.width / 2 - 2;
            setColor(g, Color.rgb(120, 220, 255, flicker));
            g.fillRect(lx, w.y + 6, 3, w.height - 12);
            setColor(g, Color.rgb(220, 250, 255, flicker * 0.8));
            g.fillRect(lx, w.y + 6, 1, w.height - 12);
        }
        setColor(g, Color.rgb(190, 210, 255, 0.7));
        g.fillRect(w.x + 3, w.y + 3, 2, 2);
        g.fillRect(w.x + w.width - 5, w.y + 3, 2, 2);
        g.fillRect(w.x + 3, w.y + w.height - 5, 2, 2);
        g.fillRect(w.x + w.width - 5, w.y + w.height - 5, 2, 2);
    }

    /** 火山墙体装饰：墙身脉动熔岩裂纹与墙脚余烬光点。 */
    private static void drawVolcanoWall(GraphicsContext g, DungeonMap.Wall w, int tick) {
        int cracks = 1 + (w.x * 5 + w.y * 3) % 2;
        for (int i = 0; i < cracks; i++) {
            int cx = w.x + 8 + (w.x * 17 + w.y * 31 + i * 41) % Math.max(1, w.width - 16);
            int cy = w.y + 10 + (w.x * 13 + w.y * 7 + i * 29) % Math.max(1, w.height - 22);
            double pulse = 0.5 + 0.5 * Math.sin(tick * 0.08 + cx * 0.05);
            setColor(g, Color.rgb(232, 96, 40, pulse));
            g.strokeLine(cx - 6, cy + 4, cx - 2, cy);
            g.strokeLine(cx - 2, cy, cx + 2, cy + 3);
            g.strokeLine(cx + 2, cy + 3, cx + 7, cy - 1);
            setColor(g, Color.rgb(255, 176, 70, pulse));
            g.fillRect(cx - 1, cy + 2, 3, 2);
        }
        int embers = 2 + (w.x * 7 + w.y * 11) % 3;
        for (int i = 0; i < embers; i++) {
            int ex = w.x + 6 + (w.x * 23 + w.y * 13 + i * 71) % Math.max(1, w.width - 12);
            double flicker = 0.35 + 0.5 * Math.abs(Math.sin(tick * 0.1 + ex * 0.07 + i));
            setColor(g, Color.rgb(255, 150, 60, flicker));
            g.fillRect(ex, w.y + w.height - 4, 2, 2);
        }
    }

    /** 海岛墙体装饰：墙顶垂挂海藻与墙脚扇贝。 */
    private static void drawIslandWall(GraphicsContext g, DungeonMap.Wall w) {
        boolean horizontal = w.width >= w.height;
        int fronds = horizontal ? 2 + (w.x * 7 + w.y * 13) % 3 : 1;
        for (int i = 0; i < fronds; i++) {
            int fx = w.x + 10 + (w.x * 31 + w.y * 17 + i * 97) % Math.max(1, w.width - 20);
            int len = 10 + (w.x * 11 + w.y * 7 + i * 53) % 11;
            setColor(g, Color.rgb(40, 96, 80));
            g.strokeLine(fx, w.y + 2, fx - 3, w.y + len / 2);
            g.strokeLine(fx - 3, w.y + len / 2, fx, w.y + len);
        }
        int shells = 1 + (w.x * 5 + w.y * 3) % 2;
        for (int i = 0; i < shells; i++) {
            int sx = w.x + 8 + (w.x * 17 + w.y * 31 + i * 41) % Math.max(1, w.width - 16);
            setColor(g, Color.rgb(226, 192, 158));
            g.fillOval(sx, w.y + w.height - 4, 8, 5);
            setColor(g, Color.rgb(160, 120, 96));
            g.strokeLine(sx + 2, w.y + w.height - 3, sx + 2, w.y + w.height - 5);
            g.strokeLine(sx + 4, w.y + w.height - 3, sx + 4, w.y + w.height - 5);
            g.strokeLine(sx + 6, w.y + w.height - 3, sx + 6, w.y + w.height - 5);
        }
    }

    /** 森林：带年轮的树桩与三叶草丛。 */
    private static void drawForest(GraphicsContext g, DungeonMap.Decoration d) {
        double x = d.x;
        double y = d.y;
        if (d.kind == 0) {
            setColor(g, Color.rgb(96, 72, 44));
            g.fillOval(x - 7, y - 7, 14, 14);
            setColor(g, Color.rgb(146, 112, 68));
            g.fillOval(x - 4, y - 4, 8, 8);
            setColor(g, Color.rgb(96, 72, 44));
            g.fillOval(x - 2, y - 2, 4, 4);
        } else {
            setColor(g, Color.rgb(46, 92, 52));
            g.strokeLine(x, y, x - 5, y - 8);
            g.strokeLine(x, y, x, y - 12);
            g.strokeLine(x, y, x + 5, y - 8);
        }
    }

    /** 草原：草丛与白瓣黄花。 */
    private static void drawGrassland(GraphicsContext g, DungeonMap.Decoration d) {
        double x = d.x;
        double y = d.y;
        if (d.kind == 0) {
            setColor(g, Color.rgb(72, 122, 56));
            g.strokeLine(x, y, x - 5, y - 8);
            g.strokeLine(x, y, x, y - 12);
            g.strokeLine(x, y, x + 5, y - 8);
        } else {
            setColor(g, Color.rgb(235, 240, 220));
            g.fillRect(x - 6, y - 1, 2, 3);
            g.fillRect(x + 4, y - 1, 2, 3);
            g.fillRect(x - 1, y - 6, 3, 2);
            g.fillRect(x - 1, y + 4, 3, 2);
            setColor(g, Color.rgb(240, 200, 70));
            g.fillRect(x - 1, y - 1, 3, 3);
        }
    }

    /** 冰原：呼吸闪烁的冰晶与雪堆。 */
    private static void drawTundra(GraphicsContext g, DungeonMap.Decoration d, int tick) {
        double x = d.x;
        double y = d.y;
        if (d.kind == 0) {
            double pulse = 0.55 + 0.45 * Math.abs(Math.sin(tick * 0.05 + x * 0.01));
            setColor(g, Color.rgb(150, 200, 235, pulse));
            g.beginPath();
            g.moveTo(x, y - 9);
            g.lineTo(x + 5, y);
            g.lineTo(x, y + 9);
            g.lineTo(x - 5, y);
            g.closePath();
            g.fill();
            setColor(g, Color.rgb(230, 245, 255, pulse));
            g.fillRect(x - 1, y - 4, 2, 2);
        } else {
            setColor(g, Color.rgb(200, 225, 245));
            g.fillOval(x - 9, y - 4, 16, 8);
            g.fillOval(x - 3, y - 7, 10, 8);
        }
    }

    /** 沙漠：仙人掌与沙石块。 */
    private static void drawDesert(GraphicsContext g, DungeonMap.Decoration d) {
        double x = d.x;
        double y = d.y;
        if (d.kind == 0) {
            setColor(g, Color.rgb(56, 110, 60));
            g.fillRect(x - 3, y - 15, 6, 15);
            g.fillRect(x - 10, y - 11, 7, 4);
            g.fillRect(x - 10, y - 7, 4, 7);
            g.fillRect(x + 3, y - 14, 7, 4);
            g.fillRect(x + 6, y - 10, 4, 7);
        } else {
            setColor(g, Color.rgb(150, 122, 84));
            g.beginPath();
            g.moveTo(x - 7, y);
            g.lineTo(x - 2, y - 7);
            g.lineTo(x + 4, y - 3);
            g.lineTo(x + 8, y);
            g.closePath();
            g.fill();
            setColor(g, Color.rgb(196, 164, 108));
            g.fillRect(x - 4, y - 5, 5, 2);
        }
    }

    /** 城堡：摇曳火把与碎石板。 */
    private static void drawCastle(GraphicsContext g, DungeonMap.Decoration d, int tick) {
        double x = d.x;
        double y = d.y;
        if (d.kind == 0) {
            setColor(g, Color.rgb(58, 52, 46));
            g.fillRect(x - 2, y - 13, 4, 13);
            setColor(g, Color.rgb(96, 78, 52));
            g.fillRect(x - 5, y - 13, 10, 2);
            // 火焰随 tick 摇曳：外焰与内焰左右摆动
            double sway = Math.sin(tick * 0.2 + x * 0.05) * 2;
            setColor(g, Color.rgb(232, 118, 40));
            g.fillOval(x - 4 + sway, y - 23, 8, 11);
            setColor(g, Color.rgb(255, 210, 100));
            g.fillOval(x - 2 + sway * 0.6, y - 20, 4, 7);
        } else {
            setColor(g, Color.rgb(84, 82, 96));
            g.fillRect(x - 7, y - 3, 14, 6);
            setColor(g, Color.rgb(46, 44, 56));
            g.strokeLine(x - 3, y - 2, x + 2, y + 1);
            g.strokeLine(x + 3, y + 1, x + 6, y - 1);
        }
    }

    /** 黑森林：枯树与带荧光的毒蘑菇。 */
    private static void drawDarkForest(GraphicsContext g, DungeonMap.Decoration d, int tick) {
        double x = d.x;
        double y = d.y;
        if (d.kind == 0) {
            setColor(g, Color.rgb(66, 58, 48));
            g.strokeLine(x, y, x - 3, y - 14);
            g.strokeLine(x, y, x + 4, y - 16);
            g.strokeLine(x - 3, y - 14, x - 7, y - 19);
            g.strokeLine(x + 4, y - 16, x + 8, y - 22);
            g.strokeLine(x - 1, y - 9, x + 3, y - 12);
        } else {
            setColor(g, Color.rgb(96, 84, 74));
            g.fillRect(x - 2, y - 7, 4, 7);
            setColor(g, Color.rgb(146, 46, 74));
            g.fillOval(x - 7, y - 12, 14, 7);
            setColor(g, Color.rgb(222, 200, 210, 0.85));
            g.fillRect(x - 3, y - 11, 2, 2);
            g.fillRect(x + 2, y - 10, 2, 2);
            // 蘑菇荧光随 tick 呼吸
            double glow = 0.35 + 0.3 * Math.sin(tick * 0.08 + x * 0.04);
            setColor(g, Color.rgb(230, 120, 150, glow));
            g.fillRect(x, y - 9, 2, 2);
        }
    }

    /** 沼泽：水坑与芦苇，水光随 tick 波动。 */
    private static void drawSwamp(GraphicsContext g, DungeonMap.Decoration d, int tick) {
        double x = d.x;
        double y = d.y;
        if (d.kind == 0) {
            setColor(g, Color.rgb(24, 40, 28));
            g.fillOval(x - 11, y - 6, 22, 11);
            double glint = 0.35 + 0.3 * Math.sin(tick * 0.08 + x * 0.03);
            setColor(g, Color.rgb(110, 160, 130, glint));
            g.fillRect(x - 5, y - 4, 8, 2);
            g.fillRect(x + 2, y - 2, 4, 1);
        } else {
            setColor(g, Color.rgb(84, 100, 56));
            g.strokeLine(x - 4, y, x - 5, y - 13);
            g.strokeLine(x, y, x + 1, y - 16);
            g.strokeLine(x + 4, y, x + 6, y - 12);
            setColor(g, Color.rgb(140, 150, 96));
            g.fillOval(x - 6, y - 16, 4, 3);
            g.fillOval(x, y - 19, 4, 3);
            g.fillOval(x + 5, y - 15, 4, 3);
        }
    }

    /** 太空：呼吸闪烁的十字星与陨石坑。 */
    private static void drawSpace(GraphicsContext g, DungeonMap.Decoration d, int tick) {
        double x = d.x;
        double y = d.y;
        if (d.kind == 0) {
            double twinkle = 0.3 + 0.7 * Math.abs(Math.sin(tick * 0.06 + x * 0.07));
            setColor(g, Color.rgb(190, 210, 255, twinkle));
            g.strokeLine(x - 6, y, x + 6, y);
            g.strokeLine(x, y - 6, x, y + 6);
            setColor(g, Color.rgb(255, 255, 255, twinkle));
            g.fillRect(x - 1, y - 1, 3, 3);
        } else {
            setColor(g, Color.rgb(10, 13, 30));
            g.fillOval(x - 9, y - 6, 18, 12);
            setColor(g, Color.rgb(96, 104, 168, 0.5));
            g.strokeArc(x - 9, y - 6, 18, 12, 200, 140, ArcType.OPEN);
        }
    }

    /** 火山：脉动熔岩裂缝与火山岩。 */
    private static void drawVolcano(GraphicsContext g, DungeonMap.Decoration d, int tick) {
        double x = d.x;
        double y = d.y;
        if (d.kind == 0) {
            double pulse = 0.55 + 0.45 * Math.sin(tick * 0.09 + x * 0.05);
            setColor(g, Color.rgb(190, 70, 30, pulse));
            g.strokeLine(x - 7, y + 3, x - 2, y - 2);
            g.strokeLine(x - 2, y - 2, x + 3, y + 1);
            g.strokeLine(x + 3, y + 1, x + 8, y - 2);
            setColor(g, Color.rgb(255, 170, 60, pulse));
            g.fillRect(x - 1, y - 3, 3, 2);
            g.fillRect(x + 5, y - 2, 2, 2);
        } else {
            setColor(g, Color.rgb(52, 30, 28));
            g.beginPath();
            g.moveTo(x - 8, y);
            g.lineTo(x - 4, y - 8);
            g.lineTo(x + 2, y - 4);
            g.lineTo(x + 8, y - 6);
            g.lineTo(x + 9, y);
            g.closePath();
            g.fill();
            setColor(g, Color.rgb(96, 48, 40, 0.7));
            g.strokeLine(x - 4, y - 8, x + 2, y - 4);
        }
    }

    /** 海岛：扇贝与海星。 */
    private static void drawIsland(GraphicsContext g, DungeonMap.Decoration d) {
        double x = d.x;
        double y = d.y;
        if (d.kind == 0) {
            setColor(g, Color.rgb(226, 192, 158));
            g.fillOval(x - 8, y - 5, 16, 10);
            setColor(g, Color.rgb(160, 120, 96));
            g.strokeLine(x - 6, y, x - 8, y - 4);
            g.strokeLine(x - 2, y, x - 2, y - 5);
            g.strokeLine(x + 2, y, x + 2, y - 5);
            g.strokeLine(x + 6, y, x + 8, y - 4);
        } else {
            setColor(g, Color.rgb(240, 150, 90));
            drawStar(g, x, y, 8, 4);
        }
    }

    /** 五角星（用于海星等装饰）。 */
    private static void drawStar(GraphicsContext g, double cx, double cy,
                                 double outer, double inner) {
        g.beginPath();
        for (int i = 0; i < 10; i++) {
            double radius = i % 2 == 0 ? outer : inner;
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            double x = cx + Math.cos(angle) * radius;
            double y = cy + Math.sin(angle) * radius;
            if (i == 0) {
                g.moveTo(x, y);
            } else {
                g.lineTo(x, y);
            }
        }
        g.closePath();
        g.fill();
    }

    /** 同时设置填充色与描边色。 */
    private static void setColor(GraphicsContext g, Color color) {
        g.setFill(color);
        g.setStroke(color);
    }

    /** 障碍物：可碰撞的主题地形物件（树、土坡、冰块、城堡残骸等），
     *  形状多样（圆/三角/矩形/菱形），细节随主题与变体变化，tick 驱动少量环境动画。 */
    public static void drawObstacles(GraphicsContext g, MapTheme theme,
                                     List<DungeonMap.Obstacle> obstacles, int tick) {
        if (theme == MapTheme.DEFAULT) {
            return;
        }
        g.setLineWidth(1);
        for (DungeonMap.Obstacle obstacle : obstacles) {
            drawObstacleShadow(g, obstacle);
            switch (theme) {
                case FOREST:
                    drawForestObstacle(g, obstacle);
                    break;
                case GRASSLAND:
                    drawGrasslandObstacle(g, obstacle);
                    break;
                case TUNDRA:
                    drawTundraObstacle(g, obstacle, tick);
                    break;
                case DESERT:
                    drawDesertObstacle(g, obstacle);
                    break;
                case CASTLE:
                    drawCastleObstacle(g, obstacle);
                    break;
                case DARK_FOREST:
                    drawDarkForestObstacle(g, obstacle, tick);
                    break;
                case SWAMP:
                    drawSwampObstacle(g, obstacle, tick);
                    break;
                case SPACE:
                    drawSpaceObstacle(g, obstacle, tick);
                    break;
                case VOLCANO:
                    drawVolcanoObstacle(g, obstacle, tick);
                    break;
                case ISLAND:
                    drawIslandObstacle(g, obstacle, tick);
                    break;
                default:
                    break;
            }
        }
    }

    /** 障碍物底部阴影：所有主题共用。 */
    private static void drawObstacleShadow(GraphicsContext g, DungeonMap.Obstacle o) {
        setColor(g, Color.rgb(0, 0, 0, 55 / 255.0));
        double halfWidth = o.size * (o.shape == 2 ? 0.58 : 0.5);
        g.fillOval(o.x - halfWidth, o.y + o.size * 0.4, halfWidth * 2, o.size * 0.24);
    }

    /** 森林障碍物：繁茂绿色大树（圆冠/杉树/老树桩/灌木丛）。 */
    private static void drawForestObstacle(GraphicsContext g, DungeonMap.Obstacle o) {
        double s = o.size;
        g.setLineWidth(2);
        switch (o.shape) {
            case 1: {
                // 杉树：三层三角树冠 + 树干 + 层间描边
                setColor(g, Color.rgb(74, 48, 30));
                g.fillRect(o.x - s * 0.08, o.y + s * 0.14, s * 0.16, s * 0.3);
                setColor(g, Color.rgb(26, 74, 44));
                triangle(g, o.x, o.y - s * 0.5, s * 0.78, true);
                setColor(g, Color.rgb(40, 104, 58));
                triangle(g, o.x, o.y - s * 0.16, s * 0.6, true);
                setColor(g, Color.rgb(52, 120, 66));
                triangle(g, o.x, o.y + s * 0.18, s * 0.42, true);
                setColor(g, Color.rgb(12, 40, 26));
                triangleStroke(g, o.x, o.y - s * 0.5, s * 0.78, true);
                triangleStroke(g, o.x, o.y - s * 0.16, s * 0.6, true);
                triangleStroke(g, o.x, o.y + s * 0.18, s * 0.42, true);
                setColor(g, Color.rgb(56, 36, 24));
                g.strokeLine(o.x - s * 0.02, o.y + s * 0.24, o.x - s * 0.02, o.y + s * 0.4);
                // 树冠亮光
                setColor(g, Color.rgb(84, 152, 92, 0.8));
                g.strokeLine(o.x - s * 0.08, o.y - s * 0.42, o.x - s * 0.16, o.y - s * 0.2);
                break;
            }
            case 2: {
                // 老树桩：矩形树桩 + 顶面年轮 + 树根
                setColor(g, Color.rgb(92, 62, 38));
                g.fillRoundRect(o.x - s * 0.5, o.y - s * 0.15, s, s * 0.55, 6, 6);
                setColor(g, Color.rgb(128, 88, 52));
                g.fillOval(o.x - s * 0.5, o.y - s * 0.28, s, s * 0.34);
                setColor(g, Color.rgb(96, 64, 40));
                g.strokeOval(o.x - s * 0.28, o.y - s * 0.2, s * 0.56, s * 0.18);
                g.strokeOval(o.x - s * 0.16, o.y - s * 0.16, s * 0.32, s * 0.1);
                setColor(g, Color.rgb(58, 38, 24));
                g.strokeRoundRect(o.x - s * 0.5, o.y - s * 0.15, s, s * 0.55, 6, 6);
                g.strokeOval(o.x - s * 0.5, o.y - s * 0.28, s, s * 0.34);
                // 树根
                g.strokeLine(o.x - s * 0.3, o.y + s * 0.36, o.x - s * 0.44, o.y + s * 0.46);
                g.strokeLine(o.x + s * 0.28, o.y + s * 0.36, o.x + s * 0.42, o.y + s * 0.44);
                setColor(g, Color.rgb(104, 72, 44));
                g.strokeLine(o.x - s * 0.18, o.y - s * 0.26, o.x - s * 0.06, o.y - s * 0.16);
                break;
            }
            case 3: {
                // 灌木丛：两个菱形叶簇 + 描边
                setColor(g, Color.rgb(30, 84, 48));
                diamond(g, o.x, o.y, s * 0.6, s * 0.42);
                setColor(g, Color.rgb(46, 110, 62));
                diamond(g, o.x - s * 0.2, o.y + s * 0.08, s * 0.42, s * 0.3);
                diamond(g, o.x + s * 0.22, o.y + s * 0.06, s * 0.4, s * 0.28);
                setColor(g, Color.rgb(14, 46, 28));
                diamondStroke(g, o.x, o.y, s * 0.6, s * 0.42);
                diamondStroke(g, o.x - s * 0.2, o.y + s * 0.08, s * 0.42, s * 0.3);
                diamondStroke(g, o.x + s * 0.22, o.y + s * 0.06, s * 0.4, s * 0.28);
                setColor(g, Color.rgb(72, 132, 80, 0.85));
                diamond(g, o.x - s * 0.06, o.y - s * 0.08, s * 0.18, s * 0.12);
                break;
            }
            default: {
                // 大树：树干 + 多层圆树冠 + 高光斑 + 描边
                setColor(g, Color.rgb(74, 48, 30));
                g.fillRect(o.x - s * 0.09, o.y - s * 0.1, s * 0.18, s * 0.5);
                setColor(g, Color.rgb(56, 36, 24));
                g.strokeRect(o.x - s * 0.09, o.y - s * 0.1, s * 0.18, s * 0.5);
                setColor(g, Color.rgb(24, 66, 40));
                g.fillOval(o.x - s * 0.52, o.y - s * 0.5, s * 1.04, s * 0.72);
                setColor(g, Color.rgb(36, 92, 52));
                g.fillOval(o.x - s * 0.38, o.y - s * 0.44, s * 0.76, s * 0.56);
                setColor(g, Color.rgb(52, 116, 66));
                g.fillOval(o.x - s * 0.22, o.y - s * 0.36, s * 0.44, s * 0.34);
                setColor(g, Color.rgb(14, 40, 24));
                g.strokeOval(o.x - s * 0.52, o.y - s * 0.5, s * 1.04, s * 0.72);
                // 树冠高光斑块
                setColor(g, Color.rgb(88, 156, 98, 0.8));
                g.fillOval(o.x - s * 0.3, o.y - s * 0.48, s * 0.24, s * 0.14);
                g.fillOval(o.x + s * 0.06, o.y - s * 0.4, s * 0.2, s * 0.12);
                setColor(g, Color.rgb(112, 176, 118, 0.7));
                g.fillOval(o.x - s * 0.16, o.y - s * 0.34, s * 0.12, s * 0.08);
                // 树根
                setColor(g, Color.rgb(58, 38, 24));
                g.strokeLine(o.x - s * 0.06, o.y + s * 0.38, o.x - s * 0.2, o.y + s * 0.48);
                g.strokeLine(o.x + s * 0.06, o.y + s * 0.38, o.x + s * 0.2, o.y + s * 0.46);
                break;
            }
        }
        g.setLineWidth(1);
    }

    /** 草原障碍物：小土坡（拱丘/尖坡/土台/草丘）。 */
    private static void drawGrasslandObstacle(GraphicsContext g, DungeonMap.Obstacle o) {
        double s = o.size;
        g.setLineWidth(2);
        switch (o.shape) {
            case 1: {
                // 尖土坡：绿色三角 + 草叶 + 描边 + 小花
                setColor(g, Color.rgb(66, 96, 44));
                triangle(g, o.x, o.y + s * 0.32, s * 0.72, true);
                setColor(g, Color.rgb(88, 124, 56));
                triangle(g, o.x, o.y + s * 0.16, s * 0.44, true);
                setColor(g, Color.rgb(32, 52, 28));
                triangleStroke(g, o.x, o.y + s * 0.32, s * 0.72, true);
                grassTuft(g, o.x - s * 0.12, o.y - s * 0.18, 3);
                grassTuft(g, o.x + s * 0.14, o.y - s * 0.06, 3);
                flower(g, o.x + s * 0.02, o.y - s * 0.26, 0);
                break;
            }
            case 2: {
                // 土台：圆角矩形 + 顶部草叶 + 描边
                setColor(g, Color.rgb(60, 88, 40));
                g.fillRoundRect(o.x - s * 0.5, o.y - s * 0.2, s, s * 0.6, 10, 10);
                setColor(g, Color.rgb(88, 122, 58));
                g.fillRoundRect(o.x - s * 0.38, o.y - s * 0.3, s * 0.76, s * 0.26, 8, 8);
                setColor(g, Color.rgb(30, 48, 26));
                g.strokeRoundRect(o.x - s * 0.5, o.y - s * 0.2, s, s * 0.6, 10, 10);
                setColor(g, Color.rgb(52, 80, 36));
                g.strokeLine(o.x - s * 0.4, o.y + s * 0.06, o.x + s * 0.4, o.y + s * 0.06);
                grassTuft(g, o.x - s * 0.16, o.y - s * 0.34, 4);
                grassTuft(g, o.x + s * 0.18, o.y - s * 0.3, 3);
                flower(g, o.x - s * 0.34, o.y - s * 0.24, 1);
                break;
            }
            case 3: {
                // 菱形草丘 + 描边
                setColor(g, Color.rgb(58, 88, 40));
                diamond(g, o.x, o.y, s * 0.6, s * 0.4);
                setColor(g, Color.rgb(90, 126, 58));
                diamond(g, o.x, o.y - s * 0.12, s * 0.36, s * 0.24);
                setColor(g, Color.rgb(30, 48, 26));
                diamondStroke(g, o.x, o.y, s * 0.6, s * 0.4);
                grassTuft(g, o.x, o.y - s * 0.32, 3);
                flower(g, o.x + s * 0.14, o.y + s * 0.06, 0);
                break;
            }
            default: {
                // 拱形土坡：椭圆丘 + 草叶 + 描边 + 小花
                setColor(g, Color.rgb(62, 92, 42));
                g.fillOval(o.x - s * 0.5, o.y - s * 0.1, s, s * 0.52);
                setColor(g, Color.rgb(92, 128, 60));
                g.fillOval(o.x - s * 0.34, o.y - s * 0.2, s * 0.68, s * 0.34);
                setColor(g, Color.rgb(30, 48, 26));
                g.strokeOval(o.x - s * 0.5, o.y - s * 0.1, s, s * 0.52);
                grassTuft(g, o.x - s * 0.24, o.y - s * 0.22, 4);
                grassTuft(g, o.x + s * 0.2, o.y - s * 0.1, 3);
                flower(g, o.x - s * 0.02, o.y - s * 0.28, 2);
                flower(g, o.x + s * 0.36, o.y - s * 0.02, 0);
                break;
            }
        }
        g.setLineWidth(1);
    }

    /** 冰原障碍物：冰块或冰石（冰石/冰锥/冰块/冰晶），描边与高光增强立体感。 */
    private static void drawTundraObstacle(GraphicsContext g, DungeonMap.Obstacle o, int tick) {
        double s = o.size;
        g.setLineWidth(2);
        switch (o.shape) {
            case 1: {
                // 冰锥：蓝色三角 + 白色亮边 + 描边 + 裂纹
                setColor(g, Color.rgb(108, 150, 190));
                triangle(g, o.x, o.y + s * 0.34, s * 0.7, true);
                setColor(g, Color.rgb(140, 182, 216));
                triangle(g, o.x - s * 0.1, o.y + s * 0.18, s * 0.4, true);
                setColor(g, Color.rgb(54, 92, 136));
                triangleStroke(g, o.x, o.y + s * 0.34, s * 0.7, true);
                setColor(g, Color.rgb(178, 214, 240));
                g.strokeLine(o.x, o.y - s * 0.28, o.x, o.y + s * 0.24);
                setColor(g, Color.rgb(216, 240, 252, 0.85));
                g.strokeLine(o.x - s * 0.07, o.y - s * 0.2, o.x - s * 0.04, o.y);
                setColor(g, Color.rgb(70, 110, 152, 0.8));
                g.strokeLine(o.x + s * 0.12, o.y + s * 0.12, o.x + s * 0.24, o.y + s * 0.24);
                break;
            }
            case 2: {
                // 冰块：蓝色矩形 + 高光面 + 描边 + 冰面反光
                setColor(g, Color.rgb(96, 138, 180));
                g.fillRoundRect(o.x - s * 0.5, o.y - s * 0.28, s, s * 0.68, 8, 8);
                setColor(g, Color.rgb(150, 190, 220));
                g.fillRoundRect(o.x - s * 0.42, o.y - s * 0.22, s * 0.4, s * 0.2, 4, 4);
                setColor(g, Color.rgb(50, 86, 130));
                g.strokeRoundRect(o.x - s * 0.5, o.y - s * 0.28, s, s * 0.68, 8, 8);
                setColor(g, Color.rgb(204, 230, 248, 0.85));
                g.strokeLine(o.x - s * 0.34, o.y - s * 0.02, o.x + s * 0.1, o.y - s * 0.02);
                setColor(g, Color.rgb(232, 246, 254, 0.5));
                g.strokeLine(o.x - s * 0.3, o.y + s * 0.16, o.x + s * 0.14, o.y + s * 0.16);
                // 底部冰面反光
                setColor(g, Color.rgb(220, 238, 250, 0.35));
                g.fillOval(o.x - s * 0.42, o.y + s * 0.32, s * 0.84, s * 0.14);
                break;
            }
            case 3: {
                // 冰晶：菱形 + 白色高光边 + 描边
                setColor(g, Color.rgb(110, 158, 198));
                diamond(g, o.x, o.y, s * 0.56, s * 0.46);
                setColor(g, Color.rgb(190, 222, 244, 0.9));
                diamond(g, o.x - s * 0.08, o.y - s * 0.1, s * 0.24, s * 0.2);
                setColor(g, Color.rgb(54, 92, 136));
                diamondStroke(g, o.x, o.y, s * 0.56, s * 0.46);
                setColor(g, Color.rgb(228, 244, 254, 0.9));
                g.strokeLine(o.x - s * 0.2, o.y - s * 0.16, o.x + s * 0.16, o.y - s * 0.16);
                break;
            }
            default: {
                // 冰石：圆润冰块 + 高光与裂缝 + 描边 + 冰面反光
                double shimmer = 0.5 + 0.5 * Math.sin(tick * 0.05 + o.x * 0.01);
                setColor(g, Color.rgb(96, 138, 180));
                g.fillOval(o.x - s * 0.5, o.y - s * 0.34, s, s * 0.72);
                setColor(g, Color.rgb(158, 196, 226));
                g.fillOval(o.x - s * 0.32, o.y - s * 0.28, s * 0.34, s * 0.22);
                setColor(g, Color.rgb(214, 236, 250, 0.75 + 0.25 * shimmer));
                g.fillOval(o.x - s * 0.2, o.y - s * 0.24, s * 0.14, s * 0.1);
                setColor(g, Color.rgb(50, 86, 130));
                g.strokeOval(o.x - s * 0.5, o.y - s * 0.34, s, s * 0.72);
                setColor(g, Color.rgb(210, 234, 250, 0.7));
                g.strokeLine(o.x - s * 0.1, o.y - s * 0.14, o.x + s * 0.16, o.y - s * 0.06);
                setColor(g, Color.rgb(230, 244, 252, 0.4));
                g.fillOval(o.x - s * 0.4, o.y + s * 0.3, s * 0.8, s * 0.14);
                break;
            }
        }
        g.setLineWidth(1);
    }

    /** 沙漠障碍物：沙堆或沙石（沙丘/沙堆/沙岩块/菱形沙岩），波纹纹理与描边增强层次。 */
    private static void drawDesertObstacle(GraphicsContext g, DungeonMap.Obstacle o) {
        double s = o.size;
        g.setLineWidth(2);
        switch (o.shape) {
            case 1: {
                // 沙堆：沙色三角 + 波纹 + 描边 + 顶光
                setColor(g, Color.rgb(168, 130, 78));
                triangle(g, o.x, o.y + s * 0.34, s * 0.72, true);
                setColor(g, Color.rgb(196, 156, 98));
                triangle(g, o.x, o.y + s * 0.14, s * 0.4, true);
                setColor(g, Color.rgb(110, 82, 46));
                triangleStroke(g, o.x, o.y + s * 0.34, s * 0.72, true);
                setColor(g, Color.rgb(220, 182, 118, 0.85));
                g.strokeLine(o.x - s * 0.06, o.y - s * 0.3, o.x - s * 0.02, o.y - s * 0.1);
                setColor(g, Color.rgb(150, 114, 64, 0.9));
                g.strokeArc(o.x - s * 0.16, o.y - s * 0.14, s * 0.32, s * 0.14, 20, 140, ArcType.OPEN);
                g.strokeArc(o.x - s * 0.26, o.y + s * 0.06, s * 0.52, s * 0.16, 30, 120, ArcType.OPEN);
                break;
            }
            case 2: {
                // 沙岩块：矩形 + 裂纹 + 描边 + 顶面亮层
                setColor(g, Color.rgb(158, 120, 70));
                g.fillRoundRect(o.x - s * 0.5, o.y - s * 0.26, s, s * 0.64, 6, 6);
                setColor(g, Color.rgb(190, 150, 92));
                g.fillRoundRect(o.x - s * 0.42, o.y - s * 0.2, s * 0.84, s * 0.2, 4, 4);
                setColor(g, Color.rgb(226, 188, 122, 0.9));
                g.fillRoundRect(o.x - s * 0.36, o.y - s * 0.18, s * 0.2, s * 0.08, 3, 3);
                setColor(g, Color.rgb(106, 78, 44));
                g.strokeRoundRect(o.x - s * 0.5, o.y - s * 0.26, s, s * 0.64, 6, 6);
                setColor(g, Color.rgb(120, 88, 50));
                g.strokeLine(o.x - s * 0.16, o.y - s * 0.12, o.x - s * 0.04, o.y + s * 0.18);
                g.strokeLine(o.x + s * 0.12, o.y - s * 0.14, o.x + s * 0.22, o.y + s * 0.08);
                g.strokeLine(o.x - s * 0.34, o.y - s * 0.04, o.x - s * 0.26, o.y + s * 0.14);
                break;
            }
            case 3: {
                // 菱形沙岩 + 描边 + 纹理线
                setColor(g, Color.rgb(164, 126, 74));
                diamond(g, o.x, o.y, s * 0.56, s * 0.44);
                setColor(g, Color.rgb(198, 158, 100));
                diamond(g, o.x, o.y - s * 0.1, s * 0.3, s * 0.22);
                setColor(g, Color.rgb(108, 80, 46));
                diamondStroke(g, o.x, o.y, s * 0.56, s * 0.44);
                setColor(g, Color.rgb(140, 106, 60, 0.8));
                g.strokeLine(o.x - s * 0.14, o.y + s * 0.1, o.x + s * 0.12, o.y - s * 0.02);
                break;
            }
            default: {
                // 沙丘：圆丘 + 波纹弧线 + 描边 + 顶光
                setColor(g, Color.rgb(162, 124, 72));
                g.fillOval(o.x - s * 0.5, o.y - s * 0.16, s, s * 0.54);
                setColor(g, Color.rgb(198, 158, 96));
                g.fillOval(o.x - s * 0.3, o.y - s * 0.2, s * 0.6, s * 0.28);
                setColor(g, Color.rgb(104, 76, 42));
                g.strokeOval(o.x - s * 0.5, o.y - s * 0.16, s, s * 0.54);
                setColor(g, Color.rgb(220, 182, 118, 0.9));
                g.fillOval(o.x - s * 0.16, o.y - s * 0.18, s * 0.12, s * 0.08);
                setColor(g, Color.rgb(150, 114, 64, 0.9));
                g.strokeArc(o.x - s * 0.3, o.y - s * 0.08, s * 0.6, s * 0.22, 15, 150, ArcType.OPEN);
                g.strokeArc(o.x - s * 0.2, o.y + s * 0.06, s * 0.4, s * 0.16, 20, 140, ArcType.OPEN);
                g.strokeArc(o.x - s * 0.36, o.y + s * 0.14, s * 0.72, s * 0.2, 25, 130, ArcType.OPEN);
                break;
            }
        }
        g.setLineWidth(1);
    }

    /** 城堡障碍物：小型残骸或完整小城堡（碎石堆/尖塔/残骸与小城堡/菱形石），描边与细节增强。 */
    private static void drawCastleObstacle(GraphicsContext g, DungeonMap.Obstacle o) {
        double s = o.size;
        g.setLineWidth(2);
        switch (o.shape) {
            case 1: {
                // 尖塔：石质三角 + 小窗 + 描边
                setColor(g, Color.rgb(76, 74, 90));
                triangle(g, o.x, o.y + s * 0.34, s * 0.68, true);
                setColor(g, Color.rgb(102, 100, 118));
                triangle(g, o.x, o.y + s * 0.18, s * 0.4, true);
                setColor(g, Color.rgb(38, 38, 52));
                g.fillOval(o.x - s * 0.06, o.y - s * 0.02, s * 0.12, s * 0.14);
                setColor(g, Color.rgb(148, 144, 166, 0.8));
                g.strokeLine(o.x - s * 0.04, o.y - s * 0.28, o.x - s * 0.02, o.y - s * 0.06);
                setColor(g, Color.rgb(40, 38, 54));
                triangleStroke(g, o.x, o.y + s * 0.34, s * 0.68, true);
                break;
            }
            case 2: {
                if (o.variant == 1) {
                    // 完整小城堡：塔楼 + 垛口 + 拱门 + 窗户 + 描边 + 旗帜
                    setColor(g, Color.rgb(70, 68, 84));
                    g.fillRoundRect(o.x - s * 0.5, o.y - s * 0.14, s, s * 0.54, 4, 4);
                    setColor(g, Color.rgb(96, 94, 112));
                    g.fillRoundRect(o.x - s * 0.2, o.y - s * 0.52, s * 0.4, s * 0.4, 3, 3);
                    setColor(g, Color.rgb(132, 128, 148));
                    for (double mx = o.x - s * 0.5; mx < o.x + s * 0.5 - 4; mx += s * 0.16) {
                        g.fillRect(mx, o.y - s * 0.22, s * 0.1, s * 0.1);
                    }
                    setColor(g, Color.rgb(30, 30, 44));
                    g.fillRoundRect(o.x - s * 0.12, o.y + s * 0.08, s * 0.24, s * 0.32, 6, 6);
                    setColor(g, Color.rgb(208, 200, 190, 0.85));
                    g.fillRect(o.x - s * 0.3, o.y - s * 0.1, s * 0.08, s * 0.1);
                    g.fillRect(o.x + s * 0.24, o.y - s * 0.1, s * 0.08, s * 0.1);
                    setColor(g, Color.rgb(44, 42, 58));
                    g.strokeRoundRect(o.x - s * 0.5, o.y - s * 0.14, s, s * 0.54, 4, 4);
                    g.strokeRoundRect(o.x - s * 0.2, o.y - s * 0.52, s * 0.4, s * 0.4, 3, 3);
                    // 塔楼旗帜
                    g.strokeLine(o.x, o.y - s * 0.52, o.x, o.y - s * 0.66);
                    setColor(g, Color.rgb(170, 60, 66));
                    triangle(g, o.x + s * 0.03, o.y - s * 0.64, s * 0.16, false);
                } else if (o.variant == 2) {
                    // 石台：矩形 + 砖线 + 描边
                    setColor(g, Color.rgb(80, 78, 94));
                    g.fillRoundRect(o.x - s * 0.5, o.y - s * 0.24, s, s * 0.6, 4, 4);
                    setColor(g, Color.rgb(56, 54, 70));
                    g.strokeLine(o.x - s * 0.4, o.y - s * 0.06, o.x + s * 0.4, o.y - s * 0.06);
                    for (double sx = o.x - s * 0.3; sx < o.x + s * 0.4; sx += s * 0.24) {
                        g.strokeLine(sx, o.y - s * 0.24, sx, o.y - s * 0.06);
                    }
                    for (double sx = o.x - s * 0.42; sx < o.x + s * 0.4; sx += s * 0.24) {
                        g.strokeLine(sx, o.y - s * 0.06, sx, o.y + s * 0.3);
                    }
                    setColor(g, Color.rgb(40, 38, 52));
                    g.strokeRoundRect(o.x - s * 0.5, o.y - s * 0.24, s, s * 0.6, 4, 4);
                    setColor(g, Color.rgb(110, 108, 126, 0.8));
                    g.fillRoundRect(o.x - s * 0.42, o.y - s * 0.2, s * 0.16, s * 0.08, 3, 3);
                } else {
                    // 断壁残骸：矩形 + 顶部缺口 + 裂纹 + 描边 + 碎石
                    setColor(g, Color.rgb(74, 72, 88));
                    g.fillRoundRect(o.x - s * 0.5, o.y - s * 0.16, s, s * 0.56, 3, 3);
                    setColor(g, Color.rgb(100, 98, 116));
                    g.fillRect(o.x - s * 0.28, o.y - s * 0.34, s * 0.3, s * 0.2);
                    setColor(g, Color.rgb(48, 46, 62));
                    g.strokeLine(o.x - s * 0.3, o.y - s * 0.08, o.x - s * 0.16, o.y + s * 0.24);
                    g.strokeLine(o.x + s * 0.12, o.y - s * 0.1, o.x + s * 0.26, o.y + s * 0.16);
                    g.strokeRoundRect(o.x - s * 0.5, o.y - s * 0.16, s, s * 0.56, 3, 3);
                    setColor(g, Color.rgb(64, 62, 78));
                    g.fillOval(o.x - s * 0.58, o.y + s * 0.3, s * 0.2, s * 0.12);
                    g.fillOval(o.x + s * 0.4, o.y + s * 0.32, s * 0.16, s * 0.1);
                }
                break;
            }
            case 3: {
                // 菱形石 + 描边
                setColor(g, Color.rgb(78, 76, 92));
                diamond(g, o.x, o.y, s * 0.54, s * 0.42);
                setColor(g, Color.rgb(104, 102, 120));
                diamond(g, o.x, o.y - s * 0.08, s * 0.28, s * 0.2);
                setColor(g, Color.rgb(42, 40, 56));
                diamondStroke(g, o.x, o.y, s * 0.54, s * 0.42);
                break;
            }
            default: {
                // 碎石堆：大圆 + 两个小圆 + 描边 + 高光
                setColor(g, Color.rgb(72, 70, 86));
                g.fillOval(o.x - s * 0.5, o.y - s * 0.24, s, s * 0.6);
                setColor(g, Color.rgb(94, 92, 110));
                g.fillOval(o.x - s * 0.18, o.y - s * 0.42, s * 0.4, s * 0.36);
                g.fillOval(o.x + s * 0.18, o.y - s * 0.14, s * 0.32, s * 0.3);
                setColor(g, Color.rgb(40, 38, 54));
                g.strokeOval(o.x - s * 0.5, o.y - s * 0.24, s, s * 0.6);
                g.strokeOval(o.x - s * 0.18, o.y - s * 0.42, s * 0.4, s * 0.36);
                g.strokeOval(o.x + s * 0.18, o.y - s * 0.14, s * 0.32, s * 0.3);
                setColor(g, Color.rgb(126, 124, 142, 0.8));
                g.fillOval(o.x - s * 0.4, o.y - s * 0.16, s * 0.14, s * 0.1);
                break;
            }
        }
        g.setLineWidth(1);
    }

    /** 黑森林障碍物：南瓜头或黑色枯木（南瓜/枯木桩/毒蘑菇/黑枯木），描边与细节增强。 */
    private static void drawDarkForestObstacle(GraphicsContext g, DungeonMap.Obstacle o, int tick) {
        double s = o.size;
        g.setLineWidth(2);
        switch (o.shape) {
            case 1: {
                // 黑色枯木：枝干三角 + 分叉线 + 描边 + 亮纹
                setColor(g, Color.rgb(24, 28, 22));
                triangle(g, o.x, o.y + s * 0.34, s * 0.6, true);
                setColor(g, Color.rgb(10, 12, 9));
                triangleStroke(g, o.x, o.y + s * 0.34, s * 0.6, true);
                g.setLineWidth(Math.max(1, s * 0.06));
                g.strokeLine(o.x, o.y - s * 0.2, o.x - s * 0.22, o.y - s * 0.32);
                g.strokeLine(o.x, o.y - s * 0.08, o.x + s * 0.2, o.y - s * 0.18);
                g.setLineWidth(2);
                setColor(g, Color.rgb(48, 54, 44, 0.7));
                g.strokeLine(o.x - s * 0.04, o.y - s * 0.24, o.x - s * 0.02, o.y - s * 0.02);
                break;
            }
            case 2: {
                // 黑色枯木桩：暗矩形 + 裂纹 + 描边
                setColor(g, Color.rgb(22, 26, 20));
                g.fillRoundRect(o.x - s * 0.5, o.y - s * 0.22, s, s * 0.6, 4, 4);
                setColor(g, Color.rgb(10, 12, 9));
                g.strokeLine(o.x - s * 0.2, o.y - s * 0.14, o.x - s * 0.06, o.y + s * 0.22);
                g.strokeLine(o.x + s * 0.18, o.y - s * 0.18, o.x + s * 0.28, o.y + s * 0.1);
                g.strokeRoundRect(o.x - s * 0.5, o.y - s * 0.22, s, s * 0.6, 4, 4);
                setColor(g, Color.rgb(42, 48, 38, 0.7));
                g.strokeLine(o.x - s * 0.36, o.y + s * 0.08, o.x + s * 0.36, o.y + s * 0.08);
                break;
            }
            case 3: {
                // 菱形南瓜 + 描边 + 瓜纹
                setColor(g, Color.rgb(196, 108, 34));
                diamond(g, o.x, o.y, s * 0.52, s * 0.4);
                setColor(g, Color.rgb(236, 150, 48));
                diamond(g, o.x - s * 0.06, o.y - s * 0.06, s * 0.3, s * 0.22);
                setColor(g, Color.rgb(52, 88, 40));
                g.fillRect(o.x - s * 0.04, o.y - s * 0.3, s * 0.08, s * 0.14);
                setColor(g, Color.rgb(120, 64, 20));
                diamondStroke(g, o.x, o.y, s * 0.52, s * 0.4);
                g.strokeLine(o.x, o.y - s * 0.26, o.x, o.y + s * 0.14);
                break;
            }
            default: {
                if (o.variant == 1) {
                    // 黑色枯木桩：暗圆 + 年轮 + 描边
                    setColor(g, Color.rgb(24, 28, 22));
                    g.fillOval(o.x - s * 0.5, o.y - s * 0.26, s, s * 0.64);
                    setColor(g, Color.rgb(44, 50, 40));
                    g.fillOval(o.x - s * 0.5, o.y - s * 0.38, s, s * 0.3);
                    setColor(g, Color.rgb(12, 14, 11));
                    g.strokeOval(o.x - s * 0.24, o.y - s * 0.32, s * 0.48, s * 0.16);
                    g.strokeOval(o.x - s * 0.12, o.y - s * 0.3, s * 0.24, s * 0.08);
                    g.strokeOval(o.x - s * 0.5, o.y - s * 0.26, s, s * 0.64);
                } else if (o.variant == 2) {
                    // 毒蘑菇：暗紫圆伞 + 白点 + 柄 + 描边
                    setColor(g, Color.rgb(92, 70, 110));
                    g.fillOval(o.x - s * 0.5, o.y - s * 0.36, s, s * 0.5);
                    setColor(g, Color.rgb(128, 98, 148));
                    g.fillOval(o.x - s * 0.34, o.y - s * 0.34, s * 0.68, s * 0.3);
                    setColor(g, Color.rgb(226, 210, 238, 0.9));
                    g.fillOval(o.x - s * 0.2, o.y - s * 0.28, s * 0.12, s * 0.1);
                    g.fillOval(o.x + s * 0.12, o.y - s * 0.2, s * 0.1, s * 0.08);
                    setColor(g, Color.rgb(178, 162, 148));
                    g.fillRect(o.x - s * 0.09, o.y - s * 0.04, s * 0.18, s * 0.34);
                    setColor(g, Color.rgb(56, 40, 70));
                    g.strokeOval(o.x - s * 0.5, o.y - s * 0.36, s, s * 0.5);
                } else {
                    // 南瓜头：橙色圆 + 三角眼 + 锯齿嘴 + 瓜蒂 + 瓜棱纹
                    double pulse = 0.5 + 0.5 * Math.sin(tick * 0.06 + o.x * 0.02);
                    setColor(g, Color.rgb(172, 92, 28));
                    g.fillOval(o.x - s * 0.5, o.y - s * 0.28, s, s * 0.66);
                    setColor(g, Color.rgb(214, 126, 40));
                    g.fillOval(o.x - s * 0.36, o.y - s * 0.24, s * 0.72, s * 0.46);
                    setColor(g, Color.rgb(30, 60, 34));
                    g.fillRect(o.x - s * 0.05, o.y - s * 0.44, s * 0.1, s * 0.16);
                    setColor(g, Color.rgb(24, 16, 8));
                    triangle(g, o.x - s * 0.14, o.y - s * 0.1, s * 0.16, true);
                    triangle(g, o.x + s * 0.14, o.y - s * 0.1, s * 0.16, true);
                    g.strokeLine(o.x - s * 0.2, o.y + s * 0.12, o.x - s * 0.06, o.y + s * 0.06);
                    g.strokeLine(o.x - s * 0.06, o.y + s * 0.06, o.x + s * 0.06, o.y + s * 0.14);
                    g.strokeLine(o.x + s * 0.06, o.y + s * 0.14, o.x + s * 0.2, o.y + s * 0.08);
                    // 眼睛孔透出的幽光
                    setColor(g, Color.rgb(255, 168, 48, 0.35 + 0.3 * pulse));
                    triangle(g, o.x - s * 0.14, o.y - s * 0.12, s * 0.1, true);
                    triangle(g, o.x + s * 0.14, o.y - s * 0.12, s * 0.1, true);
                    // 瓜棱纹与描边
                    setColor(g, Color.rgb(120, 60, 16, 0.8));
                    g.strokeOval(o.x - s * 0.5, o.y - s * 0.28, s, s * 0.66);
                    g.strokeArc(o.x - s * 0.3, o.y - s * 0.26, s * 0.12, s * 0.58, 90, 180, ArcType.OPEN);
                    g.strokeArc(o.x - s * 0.08, o.y - s * 0.26, s * 0.12, s * 0.58, 90, 180, ArcType.OPEN);
                    g.strokeArc(o.x + s * 0.14, o.y - s * 0.26, s * 0.12, s * 0.58, 90, 180, ArcType.OPEN);
                }
                break;
            }
        }
        g.setLineWidth(1);
    }

    /** 沼泽障碍物：泥潭（泥潭/泥丘/泥台/菱形泥坑），描边与泥面高光增强。 */
    private static void drawSwampObstacle(GraphicsContext g, DungeonMap.Obstacle o, int tick) {
        double s = o.size;
        g.setLineWidth(2);
        switch (o.shape) {
            case 1: {
                // 泥丘：泥色三角 + 芦苇 + 描边 + 泥面反光
                setColor(g, Color.rgb(48, 56, 34));
                triangle(g, o.x, o.y + s * 0.32, s * 0.66, true);
                setColor(g, Color.rgb(62, 72, 44));
                triangle(g, o.x, o.y + s * 0.12, s * 0.34, true);
                setColor(g, Color.rgb(22, 28, 18));
                triangleStroke(g, o.x, o.y + s * 0.32, s * 0.66, true);
                setColor(g, Color.rgb(86, 100, 58, 0.6));
                g.strokeLine(o.x - s * 0.14, o.y - s * 0.18, o.x - s * 0.04, o.y - s * 0.08);
                reeds(g, o.x - s * 0.14, o.y - s * 0.2);
                reeds(g, o.x + s * 0.16, o.y - s * 0.06);
                break;
            }
            case 2: {
                // 泥台：泥矩形 + 气泡 + 描边 + 分层
                setColor(g, Color.rgb(44, 52, 32));
                g.fillRoundRect(o.x - s * 0.5, o.y - s * 0.22, s, s * 0.58, 8, 8);
                setColor(g, Color.rgb(58, 68, 42));
                g.fillRoundRect(o.x - s * 0.4, o.y - s * 0.3, s * 0.8, s * 0.2, 6, 6);
                setColor(g, Color.rgb(22, 28, 18));
                g.strokeRoundRect(o.x - s * 0.5, o.y - s * 0.22, s, s * 0.58, 8, 8);
                swampBubbles(g, o, tick, 3);
                reeds(g, o.x + s * 0.3, o.y - s * 0.3);
                break;
            }
            case 3: {
                // 菱形泥坑 + 描边 + 内沿
                setColor(g, Color.rgb(46, 54, 34));
                diamond(g, o.x, o.y, s * 0.54, s * 0.42);
                setColor(g, Color.rgb(30, 36, 24));
                diamond(g, o.x, o.y + s * 0.06, s * 0.3, s * 0.24);
                setColor(g, Color.rgb(20, 26, 16));
                diamondStroke(g, o.x, o.y, s * 0.54, s * 0.42);
                break;
            }
            default: {
                // 泥潭：深泥椭圆 + 气泡 + 芦苇 + 描边 + 高光
                setColor(g, Color.rgb(40, 48, 30));
                g.fillOval(o.x - s * 0.5, o.y - s * 0.18, s, s * 0.52);
                setColor(g, Color.rgb(26, 32, 20));
                g.fillOval(o.x - s * 0.36, o.y - s * 0.08, s * 0.72, s * 0.32);
                setColor(g, Color.rgb(20, 26, 16));
                g.strokeOval(o.x - s * 0.5, o.y - s * 0.18, s, s * 0.52);
                setColor(g, Color.rgb(70, 84, 48, 0.6));
                g.strokeArc(o.x - s * 0.28, o.y - s * 0.12, s * 0.56, s * 0.18, 200, 140, ArcType.OPEN);
                swampBubbles(g, o, tick, 4);
                reeds(g, o.x - s * 0.32, o.y - s * 0.24);
                reeds(g, o.x + s * 0.34, o.y - s * 0.18);
                break;
            }
        }
        g.setLineWidth(1);
    }

    /** 太空障碍物：行星或恒星（行星/陨石/恒星/气态行星），描边与细节增强。 */
    private static void drawSpaceObstacle(GraphicsContext g, DungeonMap.Obstacle o, int tick) {
        double s = o.size;
        g.setLineWidth(2);
        switch (o.shape) {
            case 1: {
                // 陨石：岩石三角 + 坑点 + 描边 + 高光
                setColor(g, Color.rgb(84, 78, 96));
                triangle(g, o.x, o.y + s * 0.3, s * 0.64, true);
                setColor(g, Color.rgb(58, 54, 70));
                g.fillOval(o.x - s * 0.14, o.y - s * 0.02, s * 0.12, s * 0.1);
                g.fillOval(o.x + s * 0.1, o.y + s * 0.14, s * 0.1, s * 0.08);
                setColor(g, Color.rgb(40, 36, 52));
                triangleStroke(g, o.x, o.y + s * 0.3, s * 0.64, true);
                setColor(g, Color.rgb(128, 122, 146, 0.8));
                g.strokeLine(o.x - s * 0.06, o.y - s * 0.26, o.x - s * 0.02, o.y - s * 0.08);
                break;
            }
            case 2: {
                // 陨石块 + 描边 + 裂纹
                setColor(g, Color.rgb(88, 82, 100));
                g.fillRoundRect(o.x - s * 0.5, o.y - s * 0.22, s, s * 0.56, 6, 6);
                setColor(g, Color.rgb(60, 56, 72));
                g.fillOval(o.x - s * 0.2, o.y - s * 0.06, s * 0.14, s * 0.1);
                g.fillOval(o.x + s * 0.14, o.y + s * 0.1, s * 0.12, s * 0.08);
                setColor(g, Color.rgb(42, 38, 54));
                g.strokeRoundRect(o.x - s * 0.5, o.y - s * 0.22, s, s * 0.56, 6, 6);
                setColor(g, Color.rgb(70, 66, 84, 0.9));
                g.strokeLine(o.x - s * 0.34, o.y - s * 0.14, o.x - s * 0.2, o.y + s * 0.1);
                g.strokeLine(o.x + s * 0.3, o.y - s * 0.16, o.x + s * 0.36, o.y + s * 0.06);
                break;
            }
            case 3: {
                // 小行星：菱形 + 描边
                setColor(g, Color.rgb(92, 86, 104));
                diamond(g, o.x, o.y, s * 0.52, s * 0.42);
                setColor(g, Color.rgb(64, 60, 76));
                diamond(g, o.x + s * 0.04, o.y + s * 0.06, s * 0.26, s * 0.2);
                setColor(g, Color.rgb(44, 40, 56));
                diamondStroke(g, o.x, o.y, s * 0.52, s * 0.42);
                break;
            }
            default: {
                if (o.variant == 1) {
                    // 恒星：发光圆 + 脉动光晕 + 光芒射线
                    double pulse = 0.5 + 0.5 * Math.sin(tick * 0.08 + o.x * 0.02);
                    setColor(g, Color.rgb(255, 196, 84, 0.22 + 0.2 * pulse));
                    g.fillOval(o.x - s * 0.62, o.y - s * 0.62, s * 1.24, s * 1.24);
                    setColor(g, Color.rgb(255, 220, 120, 0.55 + 0.35 * pulse));
                    g.fillOval(o.x - s * 0.52, o.y - s * 0.52, s * 1.04, s * 1.04);
                    setColor(g, Color.rgb(255, 238, 160));
                    g.fillOval(o.x - s * 0.4, o.y - s * 0.4, s * 0.8, s * 0.8);
                    setColor(g, Color.rgb(255, 252, 224));
                    g.fillOval(o.x - s * 0.26, o.y - s * 0.26, s * 0.52, s * 0.52);
                    // 光芒射线
                    setColor(g, Color.rgb(255, 244, 190, 0.5 + 0.4 * pulse));
                    g.strokeLine(o.x - s * 0.64, o.y, o.x - s * 0.42, o.y);
                    g.strokeLine(o.x + s * 0.42, o.y, o.x + s * 0.64, o.y);
                    g.strokeLine(o.x, o.y - s * 0.64, o.x, o.y - s * 0.42);
                    g.strokeLine(o.x, o.y + s * 0.42, o.x, o.y + s * 0.64);
                } else if (o.variant == 2) {
                    // 气态行星：圆 + 横向条纹 + 描边 + 极光
                    setColor(g, Color.rgb(118, 150, 190));
                    g.fillOval(o.x - s * 0.5, o.y - s * 0.5, s, s);
                    setColor(g, Color.rgb(160, 188, 218, 0.7));
                    g.fillOval(o.x - s * 0.5, o.y - s * 0.3, s, s * 0.24);
                    setColor(g, Color.rgb(92, 124, 168, 0.7));
                    g.fillOval(o.x - s * 0.5, o.y + s * 0.02, s, s * 0.22);
                    setColor(g, Color.rgb(56, 86, 130));
                    g.strokeOval(o.x - s * 0.5, o.y - s * 0.5, s, s);
                    setColor(g, Color.rgb(220, 240, 252, 0.6));
                    g.fillOval(o.x - s * 0.36, o.y - s * 0.42, s * 0.2, s * 0.12);
                } else {
                    // 行星：彩色圆 + 倾斜行星环 + 高光 + 描边
                    setColor(g, Color.rgb(96, 128, 172));
                    g.fillOval(o.x - s * 0.48, o.y - s * 0.48, s * 0.96, s * 0.96);
                    setColor(g, Color.rgb(148, 178, 210));
                    g.fillOval(o.x - s * 0.3, o.y - s * 0.38, s * 0.28, s * 0.22);
                    setColor(g, Color.rgb(206, 226, 242, 0.75));
                    g.strokeOval(o.x - s * 0.64, o.y - s * 0.2, s * 1.28, s * 0.4);
                    setColor(g, Color.rgb(240, 244, 250, 0.55));
                    g.strokeOval(o.x - s * 0.56, o.y - s * 0.16, s * 1.12, s * 0.32);
                    setColor(g, Color.rgb(60, 92, 136));
                    g.strokeOval(o.x - s * 0.48, o.y - s * 0.48, s * 0.96, s * 0.96);
                }
                break;
            }
        }
        g.setLineWidth(1);
    }

    /** 火山障碍物：小型火山口（火山锥/熔岩块/熔岩球），描边与纹理增强。 */
    private static void drawVolcanoObstacle(GraphicsContext g, DungeonMap.Obstacle o, int tick) {
        double s = o.size;
        double pulse = 0.5 + 0.5 * Math.sin(tick * 0.07 + o.x * 0.03);
        g.setLineWidth(2);
        switch (o.shape) {
            case 1: {
                // 火山锥：棕黑三角 + 顶部红橙发光口 + 熔岩流 + 描边
                setColor(g, Color.rgb(58, 32, 26));
                triangle(g, o.x, o.y + s * 0.34, s * 0.7, true);
                setColor(g, Color.rgb(84, 48, 34));
                triangle(g, o.x, o.y + s * 0.2, s * 0.44, true);
                setColor(g, Color.rgb(30, 16, 14));
                triangleStroke(g, o.x, o.y + s * 0.34, s * 0.7, true);
                setColor(g, Color.rgb(255, 120, 44, 0.75 + 0.25 * pulse));
                g.fillOval(o.x - s * 0.1, o.y - s * 0.36, s * 0.2, s * 0.14);
                setColor(g, Color.rgb(255, 176, 60, 0.8 + 0.2 * pulse));
                g.fillOval(o.x - s * 0.06, o.y - s * 0.33, s * 0.12, s * 0.08);
                setColor(g, Color.rgb(240, 96, 36, 0.8));
                g.strokeLine(o.x, o.y - s * 0.24, o.x + s * 0.14, o.y - s * 0.02);
                g.strokeLine(o.x + s * 0.14, o.y - s * 0.02, o.x + s * 0.2, o.y + s * 0.16);
                // 锥体纹理线
                setColor(g, Color.rgb(40, 22, 18, 0.8));
                g.strokeLine(o.x - s * 0.16, o.y + s * 0.08, o.x - s * 0.1, o.y + s * 0.26);
                g.strokeLine(o.x + s * 0.2, o.y + s * 0.06, o.x + s * 0.14, o.y + s * 0.26);
                break;
            }
            case 2: {
                // 熔岩块：暗红矩形 + 橙红裂缝 + 描边
                setColor(g, Color.rgb(52, 28, 24));
                g.fillRoundRect(o.x - s * 0.5, o.y - s * 0.24, s, s * 0.62, 6, 6);
                setColor(g, Color.rgb(255, 110, 40, 0.5 + 0.3 * pulse));
                g.strokeLine(o.x - s * 0.2, o.y - s * 0.2, o.x - s * 0.08, o.y + s * 0.02);
                g.strokeLine(o.x - s * 0.08, o.y + s * 0.02, o.x + s * 0.06, o.y + s * 0.22);
                g.strokeLine(o.x + s * 0.2, o.y - s * 0.18, o.x + s * 0.28, o.y + s * 0.08);
                setColor(g, Color.rgb(26, 14, 12));
                g.strokeRoundRect(o.x - s * 0.5, o.y - s * 0.24, s, s * 0.62, 6, 6);
                setColor(g, Color.rgb(96, 54, 40, 0.8));
                g.fillRoundRect(o.x - s * 0.4, o.y - s * 0.18, s * 0.16, s * 0.08, 3, 3);
                break;
            }
            case 3: {
                // 菱形熔岩 + 描边
                setColor(g, Color.rgb(56, 30, 26));
                diamond(g, o.x, o.y, s * 0.52, s * 0.42);
                setColor(g, Color.rgb(255, 116, 44, 0.55 + 0.3 * pulse));
                diamond(g, o.x, o.y + s * 0.06, s * 0.24, s * 0.2);
                setColor(g, Color.rgb(28, 14, 12));
                diamondStroke(g, o.x, o.y, s * 0.52, s * 0.42);
                break;
            }
            default: {
                // 熔岩球：暗圆 + 脉动红裂纹 + 描边
                setColor(g, Color.rgb(50, 26, 22));
                g.fillOval(o.x - s * 0.5, o.y - s * 0.3, s, s * 0.68);
                setColor(g, Color.rgb(255, 112, 42, 0.45 + 0.35 * pulse));
                g.strokeArc(o.x - s * 0.24, o.y - s * 0.2, s * 0.48, s * 0.4, 200, 100, ArcType.OPEN);
                g.strokeArc(o.x - s * 0.06, o.y - s * 0.1, s * 0.3, s * 0.3, 20, 90, ArcType.OPEN);
                setColor(g, Color.rgb(120, 70, 44));
                g.fillOval(o.x - s * 0.16, o.y - s * 0.24, s * 0.12, s * 0.1);
                setColor(g, Color.rgb(26, 14, 12));
                g.strokeOval(o.x - s * 0.5, o.y - s * 0.3, s, s * 0.68);
                break;
            }
        }
        g.setLineWidth(1);
    }

    /** 海岛障碍物：清澈水坑（水坑/礁石/矩形水坑/菱形水坑），沙滩边沿与描边增强。 */
    private static void drawIslandObstacle(GraphicsContext g, DungeonMap.Obstacle o, int tick) {
        double s = o.size;
        g.setLineWidth(2);
        switch (o.shape) {
            case 1: {
                // 礁石：青灰三角 + 海藻 + 描边 + 高光
                setColor(g, Color.rgb(96, 112, 96));
                triangle(g, o.x, o.y + s * 0.3, s * 0.6, true);
                setColor(g, Color.rgb(136, 154, 130));
                triangle(g, o.x, o.y + s * 0.12, s * 0.32, true);
                setColor(g, Color.rgb(54, 66, 56));
                triangleStroke(g, o.x, o.y + s * 0.3, s * 0.6, true);
                setColor(g, Color.rgb(176, 194, 168, 0.8));
                g.strokeLine(o.x - s * 0.04, o.y - s * 0.22, o.x - s * 0.02, o.y - s * 0.06);
                seaweed(g, o.x - s * 0.2, o.y - s * 0.16);
                seaweed(g, o.x + s * 0.18, o.y - s * 0.04);
                break;
            }
            case 2: {
                // 矩形水坑 + 沙岸边沿 + 描边
                setColor(g, Color.rgb(70, 150, 168));
                g.fillRoundRect(o.x - s * 0.5, o.y - s * 0.22, s, s * 0.54, 12, 12);
                setColor(g, Color.rgb(120, 206, 218, 0.8));
                g.fillRoundRect(o.x - s * 0.36, o.y - s * 0.14, s * 0.72, s * 0.16, 8, 8);
                setColor(g, Color.rgb(236, 214, 166));
                g.strokeRoundRect(o.x - s * 0.5, o.y - s * 0.22, s, s * 0.54, 12, 12);
                setColor(g, Color.rgb(40, 112, 130));
                g.strokeRoundRect(o.x - s * 0.5, o.y - s * 0.22, s, s * 0.54, 12, 12);
                waterSparkle(g, o.x - s * 0.16, o.y - s * 0.1, tick);
                break;
            }
            case 3: {
                // 菱形水坑 + 沙岸边沿
                setColor(g, Color.rgb(72, 152, 170));
                diamond(g, o.x, o.y, s * 0.54, s * 0.42);
                setColor(g, Color.rgb(126, 210, 220, 0.8));
                diamond(g, o.x - s * 0.06, o.y - s * 0.06, s * 0.28, s * 0.2);
                setColor(g, Color.rgb(42, 116, 134));
                diamondStroke(g, o.x, o.y, s * 0.54, s * 0.42);
                waterSparkle(g, o.x + s * 0.08, o.y - s * 0.08, tick);
                break;
            }
            default: {
                // 清澈水坑：浅蓝圆 + 波纹 + 反光 + 沙岸边沿
                setColor(g, Color.rgb(64, 146, 166));
                g.fillOval(o.x - s * 0.5, o.y - s * 0.2, s, s * 0.54);
                setColor(g, Color.rgb(96, 190, 204, 0.85));
                g.fillOval(o.x - s * 0.34, o.y - s * 0.14, s * 0.68, s * 0.3);
                setColor(g, Color.rgb(206, 242, 246, 0.55));
                g.strokeArc(o.x - s * 0.28, o.y - s * 0.08, s * 0.56, s * 0.2, 200, 140, ArcType.OPEN);
                setColor(g, Color.rgb(40, 112, 130));
                g.strokeOval(o.x - s * 0.5, o.y - s * 0.2, s, s * 0.54);
                waterSparkle(g, o.x - s * 0.1, o.y - s * 0.12, tick);
                waterSparkle(g, o.x + s * 0.18, o.y - s * 0.02, tick + 9);
                break;
            }
        }
        g.setLineWidth(1);
    }

    /** 草叶簇：土坡顶部的短草。 */
    private static void grassTuft(GraphicsContext g, double x, double y, int blades) {
        setColor(g, Color.rgb(108, 148, 66));
        for (int i = 0; i < blades; i++) {
            double lean = (i - (blades - 1) / 2.0) * 0.35;
            g.strokeLine(x + i * 5 - blades * 2.5, y + 6, x + i * 5 - blades * 2.5 + lean, y - 6);
        }
    }

    /** 小花：草原土坡上的点缀（kind 0 黄芯白瓣、1 粉瓣、2 蓝瓣）。 */
    private static void flower(GraphicsContext g, double x, double y, int kind) {
        Color petal = kind == 1 ? Color.rgb(224, 148, 168)
                : kind == 2 ? Color.rgb(150, 178, 226) : Color.rgb(238, 232, 214);
        setColor(g, petal);
        g.fillOval(x - 4, y - 6, 4, 4);
        g.fillOval(x + 1, y - 6, 4, 4);
        g.fillOval(x - 4, y - 1, 4, 4);
        g.fillOval(x + 1, y - 1, 4, 4);
        setColor(g, Color.rgb(244, 204, 96));
        g.fillOval(x - 1, y - 3, 3, 3);
    }

    /** 沼泽气泡：泥潭表面定时冒出的气泡。 */
    private static void swampBubbles(GraphicsContext g, DungeonMap.Obstacle o, int tick, int count) {
        for (int i = 0; i < count; i++) {
            double phase = (tick * 0.05 + i * 0.37 + o.x * 0.013) % 1.0;
            if (phase < 0.55) {
                double rise = phase / 0.55;
                double bx = o.x + (i % 2 == 0 ? -1 : 1) * o.size * (0.1 + 0.14 * (i % 3));
                double by = o.y - rise * o.size * 0.2;
                setColor(g, Color.rgb(140, 156, 96, 0.75 * (1 - rise * 0.5)));
                g.fillOval(bx - 3, by - 3, 6, 6);
            }
        }
    }

    /** 芦苇：沼泽泥丘旁的草杆。 */
    private static void reeds(GraphicsContext g, double x, double y) {
        setColor(g, Color.rgb(96, 110, 60));
        g.strokeLine(x, y + 8, x - 3, y - 12);
        g.strokeLine(x + 6, y + 8, x + 4, y - 10);
        setColor(g, Color.rgb(130, 142, 86));
        g.fillOval(x - 5, y - 15, 5, 8);
    }

    /** 水坑反光：随 tick 闪烁的高光点。 */
    private static void waterSparkle(GraphicsContext g, double x, double y, int tick) {
        double sparkle = 0.5 + 0.5 * Math.sin(tick * 0.1 + x * 0.05);
        setColor(g, Color.rgb(240, 252, 252, 0.3 + 0.55 * sparkle));
        g.fillOval(x - 3, y - 2, 6, 3);
    }

    /** 海藻：礁石旁的绿色水草。 */
    private static void seaweed(GraphicsContext g, double x, double y) {
        setColor(g, Color.rgb(70, 130, 100));
        g.strokeLine(x, y + 6, x - 4, y - 10);
        g.strokeLine(x + 5, y + 6, x + 7, y - 8);
    }

    /** 等腰三角形：底边位于 baseY（宽 width），顶点朝上（upward）时高为 width*0.9。 */
    private static void triangle(GraphicsContext g, double centerX, double baseY,
                                 double width, boolean upward) {
        g.beginPath();
        g.moveTo(centerX - width / 2, baseY);
        g.lineTo(centerX + width / 2, baseY);
        g.lineTo(centerX, baseY - (upward ? width * 0.9 : -width * 0.9));
        g.closePath();
        g.fill();
    }

    /** 菱形（中心模式）。 */
    private static void diamond(GraphicsContext g, double cx, double cy,
                                double width, double height) {
        g.beginPath();
        g.moveTo(cx, cy - height / 2);
        g.lineTo(cx + width / 2, cy);
        g.lineTo(cx, cy + height / 2);
        g.lineTo(cx - width / 2, cy);
        g.closePath();
        g.fill();
    }

    /** 等腰三角形描边（与 triangle 同几何，仅描边）。 */
    private static void triangleStroke(GraphicsContext g, double centerX, double baseY,
                                       double width, boolean upward) {
        g.beginPath();
        g.moveTo(centerX - width / 2, baseY);
        g.lineTo(centerX + width / 2, baseY);
        g.lineTo(centerX, baseY - (upward ? width * 0.9 : -width * 0.9));
        g.closePath();
        g.stroke();
    }

    /** 菱形描边（与 diamond 同几何，仅描边）。 */
    private static void diamondStroke(GraphicsContext g, double cx, double cy,
                                      double width, double height) {
        g.beginPath();
        g.moveTo(cx, cy - height / 2);
        g.lineTo(cx + width / 2, cy);
        g.lineTo(cx, cy + height / 2);
        g.lineTo(cx - width / 2, cy);
        g.closePath();
        g.stroke();
    }
}
