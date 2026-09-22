package com.example.smallgame.model.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 地牢地图实体：维护墙体、障碍物、装饰与地板纹理的列表，
 * 提供碰撞检测、墙体布局校验与随机生成逻辑（装饰、障碍物）。
 * 内置 Wall（可斜墙）、Obstacle（多形状障碍物）、Decoration（纯视觉装饰）三类内部结构。
 * 绘制由视图层（EntityRenderer、MapSceneryRenderer）完成。
 * <p>
 * 被 LevelController（关卡生成）、Player / Monster（移动碰撞）、
 * MapSceneryRenderer（背景与墙体渲染）引用。
 */
public class DungeonMap {
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 760;
    public static final int WORLD_TOP = 64;
    public static final int PATH_CELL_SIZE = 24;
    public static final int PATH_COLUMNS = WIDTH / PATH_CELL_SIZE;
    public static final int PATH_ROWS = (HEIGHT - WORLD_TOP) / PATH_CELL_SIZE;

    private final int width;
    private final int height;
    private final List<Wall> walls = new ArrayList<>();
    /** 场景装饰与地板纹理（纯视觉、无碰撞），生成关卡时按主题确定。 */
    private final List<Decoration> decorations = new ArrayList<>();
    private final List<Decoration> floorTextures = new ArrayList<>();
    /** 可碰撞障碍物（树、土坡、冰块、城堡残骸等），形状多样，按主题生成。 */
    private final List<Obstacle> obstacles = new ArrayList<>();

    /**
     * 构造地图，仅保存尺寸，墙体等由生成流程逐步填充。
     *
     * @param width  地图宽度
     * @param height 地图高度
     */
    public DungeonMap(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /** 判断坐标是否越出地图边界 */
    public boolean isWall(int x, int y) { return x < 0 || y < 0 || x >= width || y >= height; }

    /** 获取地图宽度 */
    public int getWidth() { return width; }

    /** 获取地图高度 */
    public int getHeight() { return height; }

    /** 获取墙体列表 */
    public List<Wall> getWalls() { return walls; }

    /** 清空墙体列表 */
    public void clearWalls() { walls.clear(); }
    /** 场景装饰列表（由视图层按主题绘制）。 */
    public List<Decoration> getDecorations() { return decorations; }
    /** 地板纹理列表（散布的斑点与短线）。 */
    public List<Decoration> getFloorTextures() { return floorTextures; }
    /** 障碍物列表（有碰撞，按主题绘制）。 */
    public List<Obstacle> getObstacles() { return obstacles; }

    /** 按当前层主题生成地板纹理与场景装饰（纯视觉、无碰撞、避开墙体），
     *  在 buildRoom 生成墙体后调用，确定性随机数保证同一关卡布局一致。 */
    public void generateDecorations(MapTheme theme, Random random) {
        decorations.clear();
        floorTextures.clear();
        if (theme == MapTheme.DEFAULT) {
            return;
        }
        // 地板纹理：随机散布的斑点与短线，墙体会盖住它们，无需避墙
        int textureCount = 150 + random.nextInt(80);
        for (int i = 0; i < textureCount; i++) {
            double x = 44 + random.nextDouble() * (WIDTH - 88);
            double y = WORLD_TOP + 22 + random.nextDouble() * (HEIGHT - WORLD_TOP - 46);
            floorTextures.add(new Decoration(x, y, random.nextInt(3)));
        }
        // 场景装饰：避开墙体且相互保持间距
        for (int attempt = 0; attempt < 60 && decorations.size() < 16; attempt++) {
            double x = 90 + random.nextDouble() * (WIDTH - 180);
            double y = WORLD_TOP + 90 + random.nextDouble() * (HEIGHT - WORLD_TOP - 200);
            if (collidesWithWall(x, y, 16)) {
                continue;
            }
            boolean tooClose = false;
            for (Decoration existing : decorations) {
                if (distance(existing.x, existing.y, x, y) < 110) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) {
                decorations.add(new Decoration(x, y, random.nextInt(2)));
            }
        }
    }

    /**
     * 判断某圆区域是否与墙体、障碍物或地图边界碰撞。
     * 依次检测墙体与障碍物的圆形相交，最后校验外围留边边界。
     *
     * @param x      圆心 X
     * @param y      圆心 Y
     * @param radius 圆半径
     * @return true 表示碰撞
     */
    public boolean collidesWithWall(double x, double y, double radius) {
        for (Wall wall : walls) {
            if (wall.intersectsCircle(x, y, radius)) {
                return true;
            }
        }
        for (Obstacle obstacle : obstacles) {
            if (obstacle.intersectsCircle(x, y, radius)) {
                return true;
            }
        }
        return x - radius < 34 || x + radius > WIDTH - 34
                || y - radius < WORLD_TOP + 12 || y + radius > HEIGHT - 24;
    }

    /** 按主题生成可碰撞障碍物（树、土坡、冰块、城堡残骸等）：
     *  30% 概率本关无障碍物，否则普通关生成 1~3 个、Boss 关 0~1 个（障碍物随机不对称摆放）；
     *  避开墙体、出生角点与 Boss 竞技场中心，在生成装饰前调用（装饰会自动避开障碍物）。 */
    public void generateObstacles(MapTheme theme, Random random, boolean bossStage) {
        obstacles.clear();
        if (theme == MapTheme.DEFAULT) {
            return;
        }
        if (random.nextInt(10) < 3) {
            return;
        }
        int target = bossStage ? random.nextInt(2) : 1 + random.nextInt(3);
        for (int attempt = 0; attempt < 90 && obstacles.size() < target; attempt++) {
            double size = 56 + random.nextDouble() * 52;
            int shape = random.nextInt(4);
            double x = 120 + random.nextDouble() * (WIDTH - 240);
            double y = WORLD_TOP + 100 + random.nextDouble() * (HEIGHT - WORLD_TOP - 200);
            if (collidesWithWall(x, y, size * 0.55 + 16)) {
                continue;
            }
            // 避开四个出生角点，避免挤压出生位置
            if (distance(x, y, 90, WORLD_TOP + 70) < 100
                    || distance(x, y, WIDTH - 90, WORLD_TOP + 70) < 100
                    || distance(x, y, 90, HEIGHT - 80) < 100
                    || distance(x, y, WIDTH - 90, HEIGHT - 80) < 100) {
                continue;
            }
            // Boss 关避开竞技场中心与玩家出生点（Boss/传送门/宝箱区域）
            if (bossStage && (distance(x, y, WIDTH / 2.0, HEIGHT / 2.0) < 130
                    || distance(x, y, WIDTH / 2.0, HEIGHT / 2.0 - 150) < 100)) {
                continue;
            }
            boolean tooClose = false;
            for (Obstacle existing : obstacles) {
                if (distance(existing.x, existing.y, x, y)
                        < existing.size * 0.55 + size * 0.55 + 70) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) {
                obstacles.add(new Obstacle(x, y, size, shape, random.nextInt(3)));
            }
        }
    }

    /**
     * 判断墙体是否与 Boss 竞技场中心区域相交（用于阻止墙体生成在竞技场内）。
     *
     * @param wall 待检查墙体
     * @return true 表示阻挡竞技场
     */
    public boolean blocksBossArena(Wall wall) {
        return wall.intersectsCircle(WIDTH / 2.0, HEIGHT / 2.0, 120);
    }

    /**
     * 判断候选墙体是否与既有墙体（含安全间距）重叠。
     * 将候选区域外扩 padding 后做矩形相交检测。
     *
     * @param candidate 候选墙体
     * @return true 表示可添加（不重叠）
     */
    public boolean canAddWall(Wall candidate) {
        int padding = 30;
        for (Wall existing : walls) {
            if (rectanglesOverlap(candidate.x - padding, candidate.y - padding,
                    candidate.width + padding * 2, candidate.height + padding * 2,
                    existing.x, existing.y, existing.width, existing.height)) {
                return false;
            }
        }
        return true;
    }

    /** 若候选墙体不与其他墙体重叠则加入列表 */
    public void addWallIfClear(Wall candidate) {
        if (canAddWall(candidate)) {
            walls.add(candidate);
        }
    }

    /**
     * 判断两个轴对齐矩形是否重叠（边重叠也算重叠）。
     * 用于墙体生成时的间距检查。
     */
    public static boolean rectanglesOverlap(int firstX, int firstY, int firstWidth, int firstHeight,
                                            int secondX, int secondY, int secondWidth, int secondHeight) {
        return firstX < secondX + secondWidth
                && firstX + firstWidth > secondX
                && firstY < secondY + secondHeight
                && firstY + firstHeight > secondY;
    }

    /** 将数值钳制到 [min, max] 区间 */
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /** 计算两点间欧几里得距离 */
    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    /** 地图装饰元素：位置与变体编号（0 起），外观由当前层主题决定，纯视觉无碰撞。 */
    public static final class Decoration {
        public final double x;
        public final double y;
        public final int kind;

        /**
         * 构造装饰元素。
         *
         * @param x    坐标 X
         * @param y    坐标 Y
         * @param kind 变体编号
         */
        public Decoration(double x, double y, int kind) {
            this.x = x;
            this.y = y;
            this.kind = kind;
        }
    }

    /** 地图障碍物：可碰撞的地形物件，形状多样（圆形/三角形/矩形/菱形），
     *  碰撞与绘制几何一致，外观由当前层主题决定。 */
    public static final class Obstacle {
        public final double x;
        public final double y;
        public final double size;
        /** 形状：0 圆形、1 三角形（顶点朝上）、2 矩形、3 菱形。 */
        public final int shape;
        /** 样式变体（0 起），同一形状下绘制细节不同。 */
        public final int variant;

        /**
         * 构造障碍物。
         *
         * @param x      坐标 X
         * @param y      坐标 Y
         * @param size   尺寸
         * @param shape  形状编号
         * @param variant 样式变体
         */
        public Obstacle(double x, double y, double size, int shape, int variant) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.shape = shape;
            this.variant = variant;
        }

        /**
         * 障碍物与圆相交检测，按形状分派：
         * 圆形/菱形用等效圆，三角形用中心下移的等效圆，矩形用包围盒最近点距离。
         *
         * @param cx     圆心 X
         * @param cy     圆心 Y
         * @param radius 圆半径
         * @return true 表示相交
         */
        boolean intersectsCircle(double cx, double cy, double radius) {
            switch (shape) {
                case 0:
                    // 圆形：真圆碰撞
                    return distance(cx, cy, x, y) < size * 0.5 + radius;
                case 1:
                    // 三角形：等效圆（中心略下移，覆盖底宽顶尖的轮廓）
                    return distance(cx, cy, x, y + size * 0.15) < size * 0.52 + radius;
                case 2: {
                    // 矩形：轴对齐包围盒最近点
                    double halfWidth = size * 0.5;
                    double halfHeight = size * 0.35;
                    double nearestX = clamp(cx, x - halfWidth, x + halfWidth);
                    double nearestY = clamp(cy, y - halfHeight, y + halfHeight);
                    return distance(cx, cy, nearestX, nearestY) < radius;
                }
                default:
                    // 菱形：等效圆
                    return distance(cx, cy, x, y) < size * 0.5 + radius;
            }
        }
    }

    public static final class Wall {
        public final int x;
        public final int y;
        public final int width;
        public final int height;
        /** 形状变体：0 经典砖块、1 圆角石台、2 尖顶塔、3 垛口墙（仅影响视觉，碰撞仍为矩形）。 */
        public final int kind;
        /** 斜墙参数：倾角（弧度，0 为普通矩形）、中心、半长、半厚与轴向单位向量。 */
        private double angle;
        private double centerX;
        private double centerY;
        private double halfLength;
        private double halfThickness;
        private double dirX;
        private double dirY;

        /**
         * 构造矩形墙体。
         *
         * @param x      左上角 X
         * @param y      左上角 Y
         * @param width  宽
         * @param height 高
         * @param kind   形状变体
         */
        public Wall(int x, int y, int width, int height, int kind) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.kind = kind;
        }

        /** 是否为斜墙（碰撞按带宽度线段处理，表面装饰跳过斜墙）。 */
        public boolean isDiagonal() {
            return angle != 0;
        }

        /** 获取斜墙倾角（弧度，普通墙为 0）。 */
        public double getAngle() { return angle; }

        /** 获取斜墙中心 X 坐标。 */
        public double getCenterX() { return centerX; }

        /** 获取斜墙中心 Y 坐标。 */
        public double getCenterY() { return centerY; }

        /** 获取斜墙半长（沿轴向）。 */
        public double getHalfLength() { return halfLength; }

        /** 获取斜墙半厚（垂直轴向）。 */
        public double getHalfThickness() { return halfThickness; }

        /** 获取斜墙轴向单位向量 X 分量。 */
        public double getDirX() { return dirX; }

        /** 获取斜墙轴向单位向量 Y 分量。 */
        public double getDirY() { return dirY; }

        /** 斜墙工厂：以 (cx, cy) 为中心、按倾角旋转的长条墙体（碰撞按带宽度线段处理，
         *  x/y/width/height 存轴对齐包围盒，用于布局间距检查）。 */
        public static Wall rotated(double cx, double cy, int length, int thickness, double angle) {
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            int bboxWidth = (int) Math.ceil(Math.abs(cos) * length + Math.abs(sin) * thickness);
            int bboxHeight = (int) Math.ceil(Math.abs(sin) * length + Math.abs(cos) * thickness);
            Wall wall = new Wall((int) (cx - bboxWidth / 2.0), (int) (cy - bboxHeight / 2.0),
                    bboxWidth, bboxHeight, 0);
            wall.angle = angle;
            wall.centerX = cx;
            wall.centerY = cy;
            wall.halfLength = length / 2.0;
            wall.halfThickness = thickness / 2.0;
            wall.dirX = cos;
            wall.dirY = sin;
            return wall;
        }

        /**
         * 墙体与圆相交检测：
         * 斜墙将圆心投影到墙轴线，取投影点与圆心的距离与半径+半厚比较；
         * 矩形墙用包围盒最近点距离判定。
         *
         * @param cx     圆心 X
         * @param cy     圆心 Y
         * @param radius 圆半径
         * @return true 表示相交
         */
        boolean intersectsCircle(double cx, double cy, double radius) {
            if (angle != 0) {
                // 斜墙：圆心到墙轴线段的距离小于 半径+半厚
                double along = (cx - centerX) * dirX + (cy - centerY) * dirY;
                double clamped = Math.max(-halfLength, Math.min(halfLength, along));
                double px = centerX + dirX * clamped;
                double py = centerY + dirY * clamped;
                return distance(cx, cy, px, py) < radius + halfThickness;
            }
            double nearestX = clamp(cx, x, x + width);
            double nearestY = clamp(cy, y, y + height);
            return distance(cx, cy, nearestX, nearestY) < radius;
        }

    }
}