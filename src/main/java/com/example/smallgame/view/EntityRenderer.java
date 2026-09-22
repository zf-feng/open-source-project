package com.example.smallgame.view;

import com.example.smallgame.controller.reward.TreasureBox;
import com.example.smallgame.model.entity.DungeonMap;
import com.example.smallgame.model.entity.MapTheme;
import com.example.smallgame.model.entity.Portal;
import com.example.smallgame.model.entity.Reward;
import com.example.smallgame.model.entity.RewardType;
import com.example.smallgame.model.entity.WeaponDrop;
import com.example.smallgame.model.entity.WeaponType;
import com.example.smallgame.util.SpriteSheet;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import java.util.EnumMap;
import java.util.Map;

/**
 * 实体渲染器：负责地图墙体、掉落物、传送门、地面武器掉落与宝箱的绘制。
 * 这些实体在模型/控制层只保存数据，全部视觉表现集中在本类实现；
 * 武器掉落立绘帧按武器类型静态缓存，与 PlayerRenderer 互不干扰。
 * <p>
 * 引用文件：model.entity（DungeonMap、MapTheme、Reward、RewardType、Portal、
 * WeaponDrop、WeaponType）、controller.reward.TreasureBox（宝箱状态）、
 * util.SpriteSheet（武器与宝箱立绘加载、旋转绘制）。
 * 被 GameMainView（游戏世界的墙体、掉落物、传送门、武器与宝箱绘制）调用。
 */
public final class EntityRenderer {

    /** 武器掉落立绘帧缓存（按武器类型）。 */
    private static final Map<WeaponType, Image> WEAPON_FRAMES = new EnumMap<>(WeaponType.class);

    /** 工具类，禁止实例化。 */
    private EntityRenderer() {
    }

    /**
     * 预加载全部武器掉落立绘帧，避免战斗中首次掉落时加载造成卡顿。
     * 在游戏画布构造时调用一次。
     */
    public static void preload() {
        for (WeaponType type : WeaponType.values()) {
            WEAPON_FRAMES.put(type, SpriteSheet.load(type.getSpriteFolder()).frame(0));
        }
    }

    /** 按当前层主题配色绘制墙体（阴影、主体、描边、顶面高光），
     *  形状随 kind 变化，碰撞区域保持矩形不变。 */
    public static void drawWall(GraphicsContext g, DungeonMap.Wall wall, MapTheme theme) {
        if (wall.getAngle() != 0) {
            drawDiagonal(g, wall, theme);
            return;
        }
        int x = wall.x;
        int y = wall.y;
        int width = wall.width;
        int height = wall.height;
        int kind = wall.kind;
        setColor(g, Palette.color(theme.wallShadow()));
        g.fillRect(x + 5, y + 6, width, height);
        Color mortar = Palette.color(theme.wallBase()).deriveColor(0, 1, 0.62, 1);
        Color roof = Palette.color(theme.wallBase()).deriveColor(0, 0.9, 1.28, 1);
        switch (kind) {
            case 1:
                // 圆角石台：四角圆角的主体，砖缝收在圆角内
                setColor(g, Palette.color(theme.wallBase()));
                g.fillRoundRect(x, y, width, height, 14, 14);
                drawBrickLines(g, mortar, x + 8, y + 8, width - 16, height - 16);
                setColor(g, Palette.color(theme.wallEdge()));
                g.strokeRoundRect(x, y, width, height, 14, 14);
                break;
            case 2:
                // 尖顶塔：矩形主体 + 顶部三角尖顶（适合竖墙）
                double spire = Math.min(38, width * 1.5);
                setColor(g, Palette.color(theme.wallBase()));
                g.fillRect(x, y, width, height);
                drawBrickLines(g, mortar, x, y, width, height);
                setColor(g, roof);
                g.beginPath();
                g.moveTo(x, y);
                g.lineTo(x + width, y);
                g.lineTo(x + width / 2.0, y - spire);
                g.closePath();
                g.fill();
                setColor(g, Palette.color(theme.wallEdge()));
                g.strokeRect(x, y, width, height);
                g.beginPath();
                g.moveTo(x, y);
                g.lineTo(x + width, y);
                g.lineTo(x + width / 2.0, y - spire);
                g.closePath();
                g.stroke();
                break;
            case 3:
                // 垛口墙：矩形主体 + 顶部锯齿垛口（适合横墙）
                setColor(g, Palette.color(theme.wallBase()));
                g.fillRect(x, y, width, height);
                drawBrickLines(g, mortar, x, y, width, height);
                setColor(g, Palette.color(theme.wallEdge()));
                g.strokeRect(x, y, width, height);
                setColor(g, Palette.color(theme.wallBase()));
                for (double mx = x + 2; mx < x + width - 8; mx += 15) {
                    g.fillRect(mx, y - 10, 8, 10);
                }
                setColor(g, Palette.color(theme.wallEdge()));
                for (double mx = x + 2; mx < x + width - 8; mx += 15) {
                    g.strokeRect(mx, y - 10, 8, 10);
                }
                break;
            default:
                // 经典砖块
                setColor(g, Palette.color(theme.wallBase()));
                g.fillRect(x, y, width, height);
                drawBrickLines(g, mortar, x, y, width, height);
                setColor(g, Palette.color(theme.wallEdge()));
                g.strokeRect(x, y, width, height);
                break;
        }
        drawWearSpots(g, x, y, width, height);
        setColor(g, Palette.color(theme.wallHighlight()));
        g.strokeLine(x + 8, y + 7, x + width - 8, y + 7);
    }

    /** 墙体砖缝纹理：横墙画水平缝 + 错缝竖线，竖墙画竖缝 + 错缝横线。 */
    private static void drawBrickLines(GraphicsContext g, Color mortar, int bx, int by,
                                       int bw, int bh) {
        setColor(g, mortar);
        if (bw >= bh) {
            g.strokeLine(bx + 4, by + bh / 2.0, bx + bw - 4, by + bh / 2.0);
            for (int sx = bx + 18; sx < bx + bw - 8; sx += 36) {
                g.strokeLine(sx, by + 4, sx, by + bh / 2.0 - 1);
            }
            for (int sx = bx + 36; sx < bx + bw - 8; sx += 36) {
                g.strokeLine(sx, by + bh / 2.0 + 1, sx, by + bh - 4);
            }
        } else {
            g.strokeLine(bx + bw / 2.0, by + 4, bx + bw / 2.0, by + bh - 4);
            for (int sy = by + 18; sy < by + bh - 8; sy += 36) {
                g.strokeLine(bx + 4, sy, bx + bw / 2.0 - 1, sy);
            }
            for (int sy = by + 36; sy < by + bh - 8; sy += 36) {
                g.strokeLine(bx + bw / 2.0 + 1, sy, bx + bw - 4, sy);
            }
        }
    }

    /** 墙体磨损斑：以墙体坐标为种子的确定性暗斑，模拟石材风化。 */
    private static void drawWearSpots(GraphicsContext g, int wx, int wy, int ww, int wh) {
        int spotCount = ww * wh / 2600 + 1;
        setColor(g, Color.color(0, 0, 0, 0.16));
        for (int i = 0; i < spotCount; i++) {
            int sx = wx + 8 + (wx * 31 + wy * 17 + i * 97) % Math.max(1, ww - 16);
            int sy = wy + 8 + (wx * 13 + wy * 53 + i * 41) % Math.max(1, wh - 16);
            g.fillRect(sx, sy, 4, 3);
        }
    }

    /** 斜墙绘制：绕中心旋转的长条砖块（四个角点），含阴影、描边与沿轴向砖缝。 */
    private static void drawDiagonal(GraphicsContext g, DungeonMap.Wall wall, MapTheme theme) {
        double dirX = wall.getDirX();
        double dirY = wall.getDirY();
        double halfLength = wall.getHalfLength();
        double halfThickness = wall.getHalfThickness();
        double centerX = wall.getCenterX();
        double centerY = wall.getCenterY();
        double perpX = -dirY;
        double perpY = dirX;
        double[][] corners = {
                {centerX + dirX * halfLength + perpX * halfThickness,
                        centerY + dirY * halfLength + perpY * halfThickness},
                {centerX + dirX * halfLength - perpX * halfThickness,
                        centerY + dirY * halfLength - perpY * halfThickness},
                {centerX - dirX * halfLength - perpX * halfThickness,
                        centerY - dirY * halfLength - perpY * halfThickness},
                {centerX - dirX * halfLength + perpX * halfThickness,
                        centerY - dirY * halfLength + perpY * halfThickness}
        };
        // 阴影（右下偏移）
        setColor(g, Palette.color(theme.wallShadow()));
        fillPolygon(g, corners, 4, 4);
        // 主体
        setColor(g, Palette.color(theme.wallBase()));
        fillPolygon(g, corners, 0, 0);
        // 砖缝：沿长轴的中线 + 垂直短缝
        setColor(g, Palette.color(theme.wallBase()).deriveColor(0, 1, 0.62, 1));
        g.strokeLine(centerX - dirX * halfLength * 0.8, centerY - dirY * halfLength * 0.8,
                centerX + dirX * halfLength * 0.8, centerY + dirY * halfLength * 0.8);
        for (double t = -halfLength + 20; t < halfLength - 12; t += 36) {
            double bx = centerX + dirX * t;
            double by = centerY + dirY * t;
            g.strokeLine(bx - perpX * (halfThickness - 4), by - perpY * (halfThickness - 4),
                    bx + perpX * (halfThickness - 4), by + perpY * (halfThickness - 4));
        }
        // 描边
        setColor(g, Palette.color(theme.wallEdge()));
        strokePolygon(g, corners);
        // 顶面高光（沿上侧长边）
        setColor(g, Palette.color(theme.wallHighlight()));
        g.strokeLine(centerX - dirX * halfLength * 0.85 - perpX * (halfThickness - 2),
                centerY - dirY * halfLength * 0.85 - perpY * (halfThickness - 2),
                centerX + dirX * halfLength * 0.85 - perpX * (halfThickness - 2),
                centerY + dirY * halfLength * 0.85 - perpY * (halfThickness - 2));
    }

    /** 按角点数组绘制填充多边形，可整体偏移（斜墙阴影用）。 */
    private static void fillPolygon(GraphicsContext g, double[][] corners, double offsetX, double offsetY) {
        g.beginPath();
        g.moveTo(corners[0][0] + offsetX, corners[0][1] + offsetY);
        for (int i = 1; i < corners.length; i++) {
            g.lineTo(corners[i][0] + offsetX, corners[i][1] + offsetY);
        }
        g.closePath();
        g.fill();
    }

    /** 按角点数组描边多边形（斜墙描边用）。 */
    private static void strokePolygon(GraphicsContext g, double[][] corners) {
        g.beginPath();
        g.moveTo(corners[0][0], corners[0][1]);
        for (int i = 1; i < corners.length; i++) {
            g.lineTo(corners[i][0], corners[i][1]);
        }
        g.closePath();
        g.stroke();
    }

    /**
     * 绘制掉落物。
     * 生命包绘制为红色外框 + 白色十字；能量绘制为随动画帧脉动的蓝色方块，
     * 脉动幅度由 sin 函数按时间相位计算。
     */
    public static void drawReward(GraphicsContext g, Reward reward, int animationTick) {
        double px = reward.getPx();
        double py = reward.getPy();
        boolean health = reward.getType() == RewardType.HEALTH_PACK;
        Color color = health
                ? Color.rgb(244, 104, 113) : Color.rgb(84, 191, 255);
        if (health) {
            // 外层半透明底、中层主色块、内层白色十字
            setColor(g, color.deriveColor(0, 1, 1, 70 / 255.0));
            g.fillRect((int) px - 15, (int) py - 15, 30, 30);
            setColor(g, color);
            g.fillRect((int) px - 8, (int) py - 8, 16, 16);
            setColor(g, Color.WHITE);
            g.fillRect((int) px - 2, (int) py - 6, 4, 12);
            g.fillRect((int) px - 6, (int) py - 2, 12, 4);
        } else {
            // 能量：光晕与内核均按正弦脉动缩放
            int pulse = (int) (Math.sin(animationTick * 0.15 + px) * 1.5);
            setColor(g, Color.rgb(84, 191, 255, 85 / 255.0));
            g.fillRect((int) px - 4 - pulse, (int) py - 4 - pulse,
                    8 + pulse * 2, 8 + pulse * 2);
            setColor(g, Color.rgb(151, 235, 255));
            g.fillRect((int) px - 2, (int) py - 2, 4, 4);
        }
    }

    /**
     * 绘制传送门：外圈光晕、椭圆主体、内部漩涡与中心亮点，
     * 全部尺寸乘以生长进度实现渐变显现；
     * 漩涡由三层旋转弧线构成，旋转角随动画帧递增形成旋涡动效。
     */
    public static void drawPortal(GraphicsContext g, Portal portal, int animationTick) {
        portal.advanceAge();
        double grow = portal.getGrowProgress();
        double centerX = portal.getPx();
        double centerY = portal.getPy();
        double rx = Portal.RADIUS_X * grow;
        double ry = Portal.RADIUS_Y * grow;
        double pulse = Math.sin(animationTick * 0.12) * 3 * grow;

        // 外圈光晕（半透明椭圆）
        setColor(g, Color.rgb(87, 190, 255, 45 / 255.0));
        g.fillOval(centerX - rx - 8 - pulse, centerY - ry - 8 - pulse,
                (rx + 8 + pulse) * 2, (ry + 8 + pulse) * 2);

        // 椭圆主体
        setColor(g, Color.rgb(61, 129, 207, 200 / 255.0));
        g.fillOval(centerX - rx, centerY - ry, rx * 2, ry * 2);

        // 内部漩涡底色（多层椭圆叠加出纵深）
        setColor(g, Color.rgb(120, 196, 255, 90 / 255.0));
        g.fillOval(centerX - rx * 0.8, centerY - ry * 0.8, rx * 1.6, ry * 1.6);

        // 气流漩涡：三层旋转弧线随帧数转动，形成旋涡感
        setColor(g, Color.rgb(222, 251, 255, 210 / 255.0));
        g.setLineWidth(2);
        double spin = animationTick * 4;
        for (int layer = 0; layer < 3; layer++) {
            double ratio = 0.72 - layer * 0.22;
            double arcRx = rx * ratio;
            double arcRy = ry * ratio;
            g.strokeArc(centerX - arcRx, centerY - arcRy, arcRx * 2, arcRy * 2,
                    spin + layer * 80, 140 + layer * 30, ArcType.OPEN);
        }

        // 漩涡中心亮点
        setColor(g, Color.rgb(231, 250, 255, 200 / 255.0));
        g.fillOval(centerX - rx * 0.2, centerY - ry * 0.2, rx * 0.4, ry * 0.4);

        // 椭圆描边
        setColor(g, Color.rgb(185, 244, 255));
        g.setLineWidth(3);
        g.strokeOval(centerX - rx, centerY - ry, rx * 2, ry * 2);
    }

    /**
     * 绘制地面武器掉落：影子 + 立绘 + 微光；玩家靠近时显示武器名称。
     * 立绘按武器类型决定旋转姿态（弓类整体偏转 -45 度），
     * 微光以 sin 脉动制造呼吸效果，名称标签宽度先经 Text 布局测量再居中绘制。
     */
    public static void drawWeaponDrop(GraphicsContext g, WeaponDrop drop, int logicTick,
                                      double playerX, double playerY) {
        double px = drop.getPx();
        double py = drop.getPy();
        WeaponType type = drop.getType();
        // 底部阴影
        setColor(g, Color.rgb(0, 0, 0, 80 / 255.0));
        g.fillRect((int) px - 12, (int) py + 16, 24, 4);
        // 呼吸微光与描边框
        double pulse = Math.sin(logicTick * 0.08 + px * 0.05) * 2;
        setColor(g, Color.rgb(255, 224, 123, 90 / 255.0));
        g.fillRect((int) px - 18 - pulse / 2, (int) py - 18 - pulse / 2, 36 + pulse, 36 + pulse);
        setColor(g, Color.rgb(255, 255, 255, 230 / 255.0));
        g.setLineWidth(2);
        g.strokeRect((int) px - 16, (int) py - 16, 32, 32);
        SpriteSheet.drawRotated(g, WEAPON_FRAMES.get(type), px, py, 30, 30, 0, -Math.PI / 4,
                type.isBow() ? -45 : 0, false);

        // 玩家靠近时显示 "名称 [E]" 提示标签
        double dist = distance(px, py, playerX, playerY);
        if (dist < WeaponDrop.NAME_SHOW_DISTANCE) {
            String label = type.getLabel() + " [E]";
            Font font = Font.font("Monospaced", FontWeight.BOLD, 13);
            double width = textWidth(label, font);
            setColor(g, Color.rgb(0, 0, 0, 170 / 255.0));
            g.fillRect((int) (px - width / 2 - 5), (int) py - 44, (int) width + 10, 18);
            setColor(g, Color.rgb(255, 235, 160));
            g.setFont(font);
            g.fillText(label, px - width / 2, (int) py - 30);
        }
    }

    /**
     * 绘制宝箱：阴影、逐帧立绘动画（每 6 逻辑帧切一帧）与正弦脉动微光。
     * 立绘按品质从 TreasureBox 资源目录常量加载（SpriteSheet 自带缓存）。
     *
     * @param g         画布上下文
     * @param box       宝箱数据
     * @param logicTick 逻辑帧计数
     */
    public static void drawTreasureBox(GraphicsContext g, TreasureBox box, int logicTick) {
        if (box.isOpened()) {
            return;
        }
        // 地面阴影
        setColor(g, Color.rgb(0, 0, 0, 80 / 255.0));
        g.fillRect((int) box.getPx() - 18, (int) box.getPy() + 24, 36, 5);
        // 逐帧立绘动画（每 6 逻辑帧切一帧）
        SpriteSheet sprite = SpriteSheet.load(box.isGolden()
                ? TreasureBox.GOLD_FOLDER : TreasureBox.WHITE_FOLDER);
        int frame = (logicTick / 6) % sprite.frameCount();
        sprite.draw(g, frame, box.getPx(), box.getPy() + 28, 64, false);
        // 正弦脉动微光
        double glow = 0.14 + Math.sin(logicTick * 0.06) * 0.05;
        setColor(g, Color.rgb(255, 214, 107, glow));
        g.fillRect((int) box.getPx() - 20, (int) box.getPy() - 4, 40, 24);
    }

    /** 计算两点间欧几里得距离。 */
    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    /** 测量文本在指定字体下的渲染宽度（用于标签居中）。 */
    private static double textWidth(String text, Font font) {
        Text node = new Text(text);
        node.setFont(font);
        return node.getLayoutBounds().getWidth();
    }

    /** 同时设置填充与描边颜色，供统一调用。 */
    private static void setColor(GraphicsContext graphics, Paint paint) {
        graphics.setFill(paint);
        graphics.setStroke(paint);
    }
}
