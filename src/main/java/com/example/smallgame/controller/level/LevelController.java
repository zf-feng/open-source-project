package com.example.smallgame.controller.level;

import com.example.smallgame.controller.combat.BossController;
import com.example.smallgame.controller.combat.CombatController;
import com.example.smallgame.model.entity.DungeonMap;
import com.example.smallgame.model.entity.MapTheme;
import com.example.smallgame.model.entity.Monster;
import com.example.smallgame.model.entity.MonsterType;
import com.example.smallgame.model.entity.Player;
import com.example.smallgame.model.entity.Portal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 关卡控制器：负责关卡推进、地图生成、敌人波次与 AI 更新。
 * 地图按五种布局模板（自由散布/十字形/空心环/对角 X/H 形廊道）对称生成墙体，
 * 再按主题生成障碍物与装饰；敌人按波次预警生成，近战怪 BFS 寻路追击、
 * 远程怪保持距离射击、Boss 走专属 AI；每世界第 5 关为 Boss 房，通关后生成传送门。
 * <p>
 * 引用文件：model.entity（DungeonMap、Monster、MonsterType、Player、Portal、MapTheme）、
 * controller.combat（CombatController、BossController）。
 * 被 GameMainView（关卡循环）、RewardController（宝箱与传送门）、
 * BossEffectRenderer（读取 Boss 特效状态）调用。
 */
public final class LevelController {
    public static final int DEFAULT_WIDTH = DungeonMap.WIDTH;
    public static final int DEFAULT_HEIGHT = DungeonMap.HEIGHT;
    private static final int WIDTH = DungeonMap.WIDTH;
    private static final int HEIGHT = DungeonMap.HEIGHT;
    private static final int WORLD_TOP = DungeonMap.WORLD_TOP;
    private static final int PATH_CELL_SIZE = DungeonMap.PATH_CELL_SIZE;
    private static final int PATH_COLUMNS = DungeonMap.PATH_COLUMNS;
    private static final int PATH_ROWS = DungeonMap.PATH_ROWS;
    /** 敌人出生预警持续帧数（0.8 秒）：预警期间在出生点显示红色准心，结束后敌人才生成。 */
    private static final int SPAWN_WARNING_TICKS = 48;
    /** 关卡切换后的刷怪冷却：与人物入场锁定（0.8 秒出现 + 0.1 秒锁定 = 54 帧）一致，
     *  保证第一波预警在人物可操作后立即显示。 */
    private static final int LEVEL_START_SPAWN_DELAY = 54;
    /** 随机出生点与玩家的最小距离，避免贴脸生成。 */
    private static final double MIN_SPAWN_DISTANCE = 120;
    /** 出生点检测半径。 */
    private static final double SPAWN_CLEARANCE = 24;
    /** Boss 场上同时存在的小怪数量上限（不含 Boss）。 */
    private static final int MAX_BOSS_MINIONS = 5;
    /** Boss 两次召唤之间的间隔（逻辑帧，约 12 秒）。 */
    private static final int BOSS_SUMMON_INTERVAL_TICKS = 720;
    /** 精英怪基础生成概率与每世界加成。 */
    private static final double ELITE_CHANCE_BASE = 0.05;
    private static final double ELITE_CHANCE_PER_WORLD = 0.02;
    /** 整波全精英怪的概率（5%）。 */
    private static final double ELITE_WAVE_CHANCE = 0.05;
    /** Boss 召唤的小怪为精英怪的概率。 */
    private static final double BOSS_MINION_ELITE_CHANCE = 0.10;

    private final Random random = new Random();
    /** Boss 专属特殊攻击控制器（十种主题专属机制），buildRoom 时重置。 */
    private final BossController bossController = new BossController();
    /** 近战怪挥击距离：贴近角色（未重合）即可攻击，为挥击半径额外值。 */
    private static final double MELEE_ATTACK_RANGE = 14;
    /** 远程怪理想攻击距离环：保持与玩家不远不近（过近后退、过远寻路靠近），区间较宽便于自由移动。 */
    private static final double RANGED_PREFERRED_MIN = 110;
    private static final double RANGED_PREFERRED_MAX = 320;
    /** 远程怪射击冷却（60Hz 下约 1.4 秒一枪），射程无限（视线无遮挡即攻击）。 */
    private static final int RANGED_ATTACK_COOLDOWN = 85;
    /** 远程怪发射前瞄准静止帧数（0.5 秒预警，期间站定不动）。 */
    private static final int RANGED_AIM_TICKS = 30;
    private int currentLevel = 1;
    private DungeonMap currentMap = new DungeonMap(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    private final List<Monster> monsters = new ArrayList<>();
    private Portal portal;
    private long mapSeed;
    private int world = 1;
    private int stage = 1;
    private int wave = 1;
    private int spawnCooldown;
    private boolean bossSpawned;
    private int wavesTotal;
    private int wavesRemaining;
    /** 正在预警的出生点及其对应怪种（预警结束后按此正式生成）。 */
    private final List<double[]> pendingSpawnPoints = new ArrayList<>();
    private final List<MonsterType> pendingSpawnKinds = new ArrayList<>();
    /** 出生预警剩余帧数（>0 表示预警进行中，敌人尚未生成）。 */
    private int spawnWarningTicks;
    /** 当前层的主题风格（进入新世界时随机选定，整层沿用）。 */
    private MapTheme theme = MapTheme.DEFAULT;
    /** 空心环布局的两个开口矩形（对角朝向随机），补充墙禁止占用以保持环内外连通。 */
    private int[] ringGapA;
    private int[] ringGapB;

    /** 关卡完成：所有波次已刷完且场上怪物全部被清空。 */
    public boolean checkLevelComplete() {
        return wavesRemaining <= 0 && monsters.stream().noneMatch(Monster::isAlive);
    }

    /** 获取当前世界（1~3）。 */
    public int getWorld() { return world; }

    /** 获取当前层的主题风格。 */
    public MapTheme getTheme() { return theme; }

    /** 获取当前地图。 */
    public DungeonMap getCurrentMap() { return currentMap; }

    /** 获取场上怪物列表。 */
    public List<Monster> getMonsters() { return monsters; }
    /** 正在预警的出生点（视图层用于绘制红色准心预警）。 */
    public List<double[]> getPendingSpawnPoints() { return pendingSpawnPoints; }
    /** 获取通关传送门（尚未生成时为 null）。 */
    public Portal getPortal() { return portal; }

    /** 刷怪冷却倒计时（由 GameMainView 每帧调用）。 */
    public void decrementSpawnCooldown() {
        if (spawnCooldown > 0) {
            spawnCooldown--;
        }
    }

    /** 重置整个游戏流程到 1-1：重新随机地图种子与主题、重建地图、规划波次并进入开局刷怪冷却。 */
    public void reset() {
        world = 1;
        stage = 1;
        wave = 1;
        mapSeed = random.nextLong();
        theme = MapTheme.randomForWorld(world, random);
        monsters.clear();
        portal = null;
        currentMap = new DungeonMap(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        buildRoom(currentMap);
        planWaves();
        spawnCooldown = LEVEL_START_SPAWN_DELAY;
        bossSpawned = false;
    }

    /** 进入下一关：关卡序号递增，跨世界时世界 +1 并重选主题，随后重建地图、规划波次；返回 true 表示已通关最终关 3-5。 */
    public boolean advanceLevel() {
        if (world == 3 && stage == 5) {
            return true;
        }
        stage++;
        if (stage > 5) {
            world++;
            stage = 1;
            theme = MapTheme.randomForWorld(world, random);
        }
        wave = (world - 1) * 5 + stage;
        monsters.clear();
        portal = null;
        buildRoom(currentMap);
        planWaves();
        spawnCooldown = LEVEL_START_SPAWN_DELAY;
        bossSpawned = false;
        return false;
    }

    /** 本关敌人总数：Boss 关为 1（仅 Boss 本身），普通关随世界与关卡递增（4 + 2*世界 + 关卡）。 */
    public int enemiesForCurrentLevel() {
        if (stage == 5) {
            return 1;
        }
        return 4 + world * 2 + stage;
    }

    /** 关卡名称（世界-关卡，如 2-3）。 */
    public String levelName() {
        return world + "-" + stage;
    }

    /** Boss 房入场展示的 Boss 名称（按本层主题返回专属 Boss 名）。 */
    public String bossName() {
        switch (theme) {
            case FOREST: return "圣诞树人";
            case TUNDRA: return "大雪怪";
            case GRASSLAND: return "巨人";
            case DESERT: return "黄金毒蝎";
            case CASTLE: return "大骑士";
            case DARK_FOREST: return "大巫师";
            case SWAMP: return "大史莱姆王";
            case SPACE: return "虚空领主";
            case VOLCANO: return "赤焰飞龙";
            case ISLAND: return "钢铁海盗";
            default: return "";
        }
    }

    /** 当前是否为 Boss 房（每世界第 5 关）。 */
    public boolean isBossStage() {
        return stage == 5;
    }

    /** 是否为最终关（3-5，通关后直接结算、不再生成宝箱）。 */
    public boolean isFinalStage() {
        return world >= 3 && stage == 5;
    }

    public void buildRoom(DungeonMap map) {
        map.clearWalls();
        ringGapA = null;
        ringGapB = null;
        int levelIndex = (world - 1) * 5 + stage - 1;
        Random layoutRandom = new Random(mapSeed + levelIndex * 7919L);
        // 墙体统一对称摆放（镜像成对），障碍物保持随机不对称；Boss 关墙体更少
        boolean symmetricLayout = true;
        // 地图整体布局模板：0 自由散布、1 十字形、2 空心环、3 对角 X、4 H 形廊道（Boss 关仅自由）
        int layoutPattern = stage == 5 ? 0 : layoutRandom.nextInt(5);
        int targetWallCount = stage == 5
                ? 2
                : layoutPattern == 0
                        ? 4 + layoutRandom.nextInt(2) * 2
                        : 2 + layoutRandom.nextInt(2) * 2;

        // 先放置布局模板主体结构（大墙，决定地图整体形状）
        switch (layoutPattern) {
            case 1:
                addCrossLayout(map, layoutRandom);
                break;
            case 2:
                addRingLayout(map, layoutRandom);
                break;
            case 3:
                addDiagonalCrossLayout(map, layoutRandom);
                break;
            case 4:
                addCorridorLayout(map, layoutRandom);
                break;
            default:
                break;
        }

        // 非对称布局分支：先放置地标大墙，再在其周围安置小墙（布局开关恒为对称布局，本分支不执行）
        if (!symmetricLayout && stage != 5 && layoutPattern == 0) {
            if (layoutRandom.nextInt(3) == 0) {
                map.addWallIfClear(new DungeonMap.Wall(465, 330, 350, 28, wallKindFor(layoutRandom, 350, 28)));
            } else if (layoutRandom.nextInt(2) == 0) {
                map.addWallIfClear(new DungeonMap.Wall(625, 180, 28, 390, wallKindFor(layoutRandom, 28, 390)));
            } else {
                map.addWallIfClear(new DungeonMap.Wall(360, 270, 28, 250, wallKindFor(layoutRandom, 28, 250)));
                map.addWallIfClear(new DungeonMap.Wall(890, 180, 28, 250, wallKindFor(layoutRandom, 28, 250)));
            }
        }

        int attempts = 0;
        while (map.getWalls().size() < targetWallCount && attempts++ < 400) {
            double roll = layoutRandom.nextDouble();
            if (!symmetricLayout && layoutPattern == 0 && roll < 0.12) {
                // 十字形墙：横竖两段矩形交叉
                addCrossWall(map, layoutRandom);
            } else if (!symmetricLayout && layoutPattern == 0 && roll < 0.24) {
                // L 形拐角墙：水平段与竖直段相接
                addLWall(map, layoutRandom);
            } else if (roll < 0.36) {
                // 方形实心墙体（小/中/大三档尺寸，对称布局时成对镜像）
                addBlockWall(map, layoutRandom, symmetricLayout);
            } else {
                // 普通矩形墙（长度短/中/长/超长四档、厚度 24/32/40 三档，对称布局时镜像成对）
                boolean horizontal = layoutRandom.nextBoolean();
                int length = pickWallLength(layoutRandom);
                int thickness = 24 + layoutRandom.nextInt(3) * 8;
                int width = horizontal ? length : thickness;
                int height = horizontal ? thickness : length;
                int x = 90 + layoutRandom.nextInt(Math.max(1, WIDTH - width - 180));
                int y = WORLD_TOP + 70
                        + layoutRandom.nextInt(Math.max(1, HEIGHT - WORLD_TOP - height - 130));
                DungeonMap.Wall candidate = new DungeonMap.Wall(x, y, width, height,
                        wallKindFor(layoutRandom, width, height));
                if (blocksRingGap(candidate.x, candidate.y, candidate.width, candidate.height)) {
                    continue;
                }
                if (symmetricLayout) {
                    // 镜像墙复用同一种形状变体，保持对称美观
                    DungeonMap.Wall mirror = new DungeonMap.Wall(WIDTH - x - width, y, width, height,
                            candidate.kind);
                    if (blocksRingGap(mirror.x, mirror.y, mirror.width, mirror.height)) {
                        continue;
                    }
                    boolean mirrorsOverlap = DungeonMap.rectanglesOverlap(
                            candidate.x - 30, candidate.y - 30,
                            candidate.width + 60, candidate.height + 60,
                            mirror.x, mirror.y, mirror.width, mirror.height);
                    if (!mirrorsOverlap
                            && (stage != 5 || (!map.blocksBossArena(candidate)
                            && !map.blocksBossArena(mirror)))
                            && map.canAddWall(candidate) && map.canAddWall(mirror)) {
                        map.getWalls().add(candidate);
                        map.getWalls().add(mirror);
                    }
                } else if (stage != 5 || !map.blocksBossArena(candidate)) {
                    map.addWallIfClear(candidate);
                }
            }
        }
        // 斜向墙点缀：自由布局 2~3 面、模板布局 1~2 面（对角 X 模板本身已由斜墙构成）
        if (!symmetricLayout && layoutPattern != 3) {
            int diagonalCount = layoutPattern == 0 ? 2 + layoutRandom.nextInt(2)
                    : 1 + layoutRandom.nextInt(2);
            for (int i = 0; i < diagonalCount; i++) {
                addDiagonalWall(map, layoutRandom);
            }
        }
        // 地图边缘墙体段：沿四边放置墙段形成不规则边界
        addBorderWalls(map, layoutRandom, symmetricLayout);
        // 墙体生成完毕后先生成可碰撞障碍物（主题地形物件），再生成纯视觉装饰（装饰自动避开障碍物）
        map.generateObstacles(theme, layoutRandom, stage == 5);
        map.generateDecorations(theme, layoutRandom);
        // 新关卡开始：重置 Boss 专属特殊攻击状态
        bossController.reset();
    }

    /** 十字形地图模板：四个角落各放一个 L 形拐角墙，围出中央十字形开放通道。 */
    private void addCrossLayout(DungeonMap map, Random random) {
        int leg = 170 + random.nextInt(60);
        int thick = 28;
        int marginX = 80;
        int marginY = WORLD_TOP + 64;
        addCornerL(map, marginX, marginY, leg, thick, 1, 1, random);
        addCornerL(map, WIDTH - marginX, marginY, leg, thick, -1, 1, random);
        addCornerL(map, marginX, HEIGHT - 40, leg, thick, 1, -1, random);
        addCornerL(map, WIDTH - marginX, HEIGHT - 40, leg, thick, -1, -1, random);
    }

    /** 在指定角落放置 L 形拐角墙：dirX/dirY 为墙体向地图内侧的延伸方向。 */
    private void addCornerL(DungeonMap map, int cornerX, int cornerY, int leg, int thick,
                            int dirX, int dirY, Random random) {
        DungeonMap.Wall horizontal = new DungeonMap.Wall(
                dirX > 0 ? cornerX : cornerX - leg + thick,
                dirY > 0 ? cornerY : cornerY - thick, leg, thick, 3);
        DungeonMap.Wall vertical = new DungeonMap.Wall(
                dirX > 0 ? cornerX : cornerX - thick,
                dirY > 0 ? cornerY : cornerY - leg + thick, thick, leg, 2);
        if (map.canAddWall(horizontal) && map.canAddWall(vertical)) {
            map.getWalls().add(horizontal);
            map.getWalls().add(vertical);
        }
    }

    /** 空心环矩形地图模板：地图中央一圈墙体围成空心矩形，
     *  两条相对边上各留一个对角开口（四种朝向随机）：顶左+底右 / 顶右+底左 / 左上+右下 / 左下+右上，
     *  开口记录于 ringGapA/ringGapB，补充墙/斜墙/方块墙禁止占用，方便人物与敌人进出绕行。 */
    private void addRingLayout(DungeonMap map, Random random) {
        int ringWidth = 460 + random.nextInt(140);
        int ringHeight = 300 + random.nextInt(140);
        int thick = 26 + random.nextInt(10);
        int gap = 88;
        int x = WIDTH / 2 - ringWidth / 2;
        int y = HEIGHT / 2 - ringHeight / 2;
        DungeonMap.Wall top;
        DungeonMap.Wall bottom;
        DungeonMap.Wall left;
        DungeonMap.Wall right;
        int[] gapA;
        int[] gapB;
        switch (random.nextInt(4)) {
            case 1: {
                // 顶右 + 底左开口
                top = new DungeonMap.Wall(x, y, ringWidth - gap, thick, 3);
                bottom = new DungeonMap.Wall(x + gap, y + ringHeight - thick,
                        ringWidth - gap, thick, 3);
                left = new DungeonMap.Wall(x, y, thick, ringHeight, 2);
                right = new DungeonMap.Wall(x + ringWidth - thick, y, thick, ringHeight, 2);
                gapA = new int[]{x + ringWidth - gap, y, gap - thick, thick};
                gapB = new int[]{x + thick, y + ringHeight - thick, gap - thick, thick};
                break;
            }
            case 2: {
                // 左上 + 右下开口
                top = new DungeonMap.Wall(x, y, ringWidth, thick, 3);
                bottom = new DungeonMap.Wall(x, y + ringHeight - thick, ringWidth, thick, 3);
                left = new DungeonMap.Wall(x, y + gap, thick, ringHeight - gap, 2);
                right = new DungeonMap.Wall(x + ringWidth - thick, y, thick, ringHeight - gap, 2);
                gapA = new int[]{x, y + thick, thick, gap - thick};
                gapB = new int[]{x + ringWidth - thick, y + ringHeight - gap, thick, gap - thick};
                break;
            }
            case 3: {
                // 左下 + 右上开口
                top = new DungeonMap.Wall(x, y, ringWidth, thick, 3);
                bottom = new DungeonMap.Wall(x, y + ringHeight - thick, ringWidth, thick, 3);
                left = new DungeonMap.Wall(x, y, thick, ringHeight - gap, 2);
                right = new DungeonMap.Wall(x + ringWidth - thick, y + gap, thick, ringHeight - gap, 2);
                gapA = new int[]{x, y + ringHeight - gap, thick, gap - thick};
                gapB = new int[]{x + ringWidth - thick, y + thick, thick, gap - thick};
                break;
            }
            default: {
                // 顶左 + 底右开口
                top = new DungeonMap.Wall(x + gap, y, ringWidth - gap, thick, 3);
                bottom = new DungeonMap.Wall(x, y + ringHeight - thick, ringWidth - gap, thick, 3);
                left = new DungeonMap.Wall(x, y, thick, ringHeight, 2);
                right = new DungeonMap.Wall(x + ringWidth - thick, y, thick, ringHeight, 2);
                gapA = new int[]{x + thick, y, gap - thick, thick};
                gapB = new int[]{x + ringWidth - gap, y + ringHeight - thick, gap - thick, thick};
                break;
            }
        }
        if (map.canAddWall(top) && map.canAddWall(bottom)
                && map.canAddWall(left) && map.canAddWall(right)) {
            map.getWalls().add(top);
            map.getWalls().add(bottom);
            map.getWalls().add(left);
            map.getWalls().add(right);
            ringGapA = gapA;
            ringGapB = gapB;
        }
    }

    /** 是否与空心环布局的开口矩形重叠（补充墙禁止占用开口）。 */
    private boolean blocksRingGap(int x, int y, int width, int height) {
        if (ringGapA == null || ringGapB == null) {
            return false;
        }
        return DungeonMap.rectanglesOverlap(x, y, width, height,
                ringGapA[0], ringGapA[1], ringGapA[2], ringGapA[3])
                || DungeonMap.rectanglesOverlap(x, y, width, height,
                ringGapB[0], ringGapB[1], ringGapB[2], ringGapB[3]);
    }

    /** 对角 X 形地图模板：两条对角线方向各放两段斜墙，中央留出菱形开放区，四角形成三角隔间。 */
    private void addDiagonalCrossLayout(DungeonMap map, Random random) {
        int thick = 26;
        int length = 200 + random.nextInt(100);
        double centerGap = 150;
        double centerX = WIDTH / 2.0;
        double centerY = HEIGHT / 2.0;
        for (double angle : new double[]{Math.PI / 4, -Math.PI / 4}) {
            double dx = Math.cos(angle);
            double dy = Math.sin(angle);
            double offset = length / 2.0 + centerGap / 2.0;
            for (int sign = -1; sign <= 1; sign += 2) {
                DungeonMap.Wall wall = DungeonMap.Wall.rotated(
                        centerX + dx * offset * sign, centerY + dy * offset * sign,
                        length, thick, angle);
                if (map.canAddWall(wall)) {
                    map.getWalls().add(wall);
                }
            }
        }
    }

    /** H 形廊道地图模板：左右两条长竖墙由中央横墙连接，形成环绕的 H 形结构。 */
    private void addCorridorLayout(DungeonMap map, Random random) {
        int thick = 28;
        int leg = 240 + random.nextInt(120);
        int leftX = 260 + random.nextInt(60);
        int topY = HEIGHT / 2 - leg / 2;
        DungeonMap.Wall left = new DungeonMap.Wall(leftX, topY, thick, leg, 2);
        DungeonMap.Wall right = new DungeonMap.Wall(WIDTH - leftX - thick, topY, thick, leg, 2);
        DungeonMap.Wall bar = new DungeonMap.Wall(leftX + thick, HEIGHT / 2 - thick / 2,
                WIDTH - 2 * (leftX + thick), thick, 3);
        if (map.canAddWall(left) && map.canAddWall(right) && map.canAddWall(bar)) {
            map.getWalls().add(left);
            map.getWalls().add(right);
            map.getWalls().add(bar);
        }
    }

    /** 十字形墙：横竖两段矩形交叉，作为一个整体避开其它墙体放置。 */
    private void addCrossWall(DungeonMap map, Random random) {
        int arm = 90 + random.nextInt(70);
        int thick = 28;
        int cx = 160 + random.nextInt(WIDTH - 320);
        int cy = WORLD_TOP + 160 + random.nextInt(HEIGHT - WORLD_TOP - 320);
        DungeonMap.Wall horizontal = new DungeonMap.Wall(cx - arm / 2, cy - thick / 2, arm, thick,
                wallKindFor(random, arm, thick));
        DungeonMap.Wall vertical = new DungeonMap.Wall(cx - thick / 2, cy - arm / 2, thick, arm,
                wallKindFor(random, thick, arm));
        // 两段矩形均在加入前完成与现有墙的间距检查（组内重叠属正常）
        if (map.canAddWall(horizontal) && map.canAddWall(vertical)) {
            map.getWalls().add(horizontal);
            map.getWalls().add(vertical);
        }
    }

    /** L 形拐角墙：水平段与竖直段在拐角相接（共享厚度重叠区），作为一个整体放置。 */
    private void addLWall(DungeonMap map, Random random) {
        int leg = 110 + random.nextInt(80);
        int thick = 28;
        int cx = 160 + random.nextInt(WIDTH - 320);
        int cy = WORLD_TOP + 160 + random.nextInt(HEIGHT - WORLD_TOP - 320);
        boolean up = random.nextBoolean();
        DungeonMap.Wall horizontal = new DungeonMap.Wall(cx - leg / 2, cy - thick / 2, leg, thick,
                wallKindFor(random, leg, thick));
        // 竖直段从水平段右端向上或向下延伸，拐角处与水平段重叠 thick 宽
        DungeonMap.Wall vertical = new DungeonMap.Wall(cx + leg / 2 - thick,
                up ? cy - leg + thick / 2 : cy - thick / 2, thick, leg, wallKindFor(random, thick, leg));
        if (map.canAddWall(horizontal) && map.canAddWall(vertical)) {
            map.getWalls().add(horizontal);
            map.getWalls().add(vertical);
        }
    }

    /** 方形实心墙体：尺寸小/中/大三档，对称布局时成对镜像放置。 */
    private void addBlockWall(DungeonMap map, Random random, boolean symmetric) {
        int tier = random.nextInt(3);
        int size = tier == 0 ? 70 + random.nextInt(20)
                : tier == 1 ? 110 + random.nextInt(30)
                : 150 + random.nextInt(30);
        int x = 130 + random.nextInt(WIDTH - size * 2 - 260);
        int y = WORLD_TOP + 110 + random.nextInt(HEIGHT - WORLD_TOP - size - 200);
        DungeonMap.Wall candidate = new DungeonMap.Wall(x, y, size, size,
                wallKindFor(random, size, size));
        if (blocksRingGap(candidate.x, candidate.y, candidate.width, candidate.height)) {
            return;
        }
        if (symmetric) {
            DungeonMap.Wall mirror = new DungeonMap.Wall(WIDTH - x - size, y, size, size,
                    candidate.kind);
            if (blocksRingGap(mirror.x, mirror.y, mirror.width, mirror.height)) {
                return;
            }
            boolean mirrorsOverlap = DungeonMap.rectanglesOverlap(
                    candidate.x - 30, candidate.y - 30,
                    candidate.width + 60, candidate.height + 60,
                    mirror.x, mirror.y, mirror.width, mirror.height);
            if (!mirrorsOverlap
                    && (stage != 5 || (!map.blocksBossArena(candidate)
                    && !map.blocksBossArena(mirror)))
                    && map.canAddWall(candidate) && map.canAddWall(mirror)) {
                map.getWalls().add(candidate);
                map.getWalls().add(mirror);
            }
        } else if (stage != 5 || !map.blocksBossArena(candidate)) {
            map.addWallIfClear(candidate);
        }
    }

    /** 斜向墙：绕中心旋转 ±45° 的长条墙体，碰撞按带宽度线段处理。 */
    private void addDiagonalWall(DungeonMap map, Random random) {
        int length = 130 + random.nextInt(90);
        int thickness = 26;
        double angle = random.nextBoolean() ? Math.PI / 4 : -Math.PI / 4;
        for (int attempt = 0; attempt < 20; attempt++) {
            double cx = 160 + random.nextDouble() * (WIDTH - 320);
            double cy = WORLD_TOP + 160 + random.nextDouble() * (HEIGHT - WORLD_TOP - 320);
            DungeonMap.Wall wall = DungeonMap.Wall.rotated(cx, cy, length, thickness, angle);
            if (blocksRingGap(wall.x, wall.y, wall.width, wall.height)) {
                continue;
            }
            if (map.canAddWall(wall)) {
                map.getWalls().add(wall);
                return;
            }
        }
    }

    /** 地图边缘墙体段：沿四边放置墙段，使活动区域边界不再呈现标准矩形。
     *  墙体对称镜像成对；Boss 关仅上下各放 1 段，左右边不放置，保持竞技场空旷。 */
    private void addBorderWalls(DungeonMap map, Random random, boolean symmetric) {
        int borderThickness = 22;
        // 上边：普通关 1~2 段随机横墙，Boss 关仅 1 段
        int topSegments = stage == 5 ? 1 : 1 + random.nextInt(2);
        for (int i = 0; i < topSegments; i++) {
            int width = 90 + random.nextInt(150);
            int x = 50 + random.nextInt(WIDTH - width - 100);
            map.addWallIfClear(new DungeonMap.Wall(x, WORLD_TOP + 14, width, borderThickness,
                    wallKindFor(random, width, borderThickness)));
        }
        // 下边：普通关 1~2 段随机横墙，Boss 关仅 1 段
        int bottomSegments = stage == 5 ? 1 : 1 + random.nextInt(2);
        for (int i = 0; i < bottomSegments; i++) {
            int width = 90 + random.nextInt(150);
            int x = 50 + random.nextInt(WIDTH - width - 100);
            map.addWallIfClear(new DungeonMap.Wall(x, HEIGHT - 24 - borderThickness, width,
                    borderThickness, wallKindFor(random, width, borderThickness)));
        }
        // 左右边：Boss 关不放置，普通关成对镜像 1 段
        int sideSegments = stage == 5 ? 0 : 1;
        for (int i = 0; i < sideSegments; i++) {
            int height = 80 + random.nextInt(140);
            int y = WORLD_TOP + 60 + random.nextInt(HEIGHT - WORLD_TOP - height - 120);
            DungeonMap.Wall left = new DungeonMap.Wall(36, y, borderThickness, height,
                    wallKindFor(random, borderThickness, height));
            if (symmetric) {
                DungeonMap.Wall right = new DungeonMap.Wall(WIDTH - 36 - borderThickness, y,
                        borderThickness, height, left.kind);
                if (map.canAddWall(left) && map.canAddWall(right)) {
                    map.getWalls().add(left);
                    map.getWalls().add(right);
                }
            } else {
                map.addWallIfClear(left);
                map.addWallIfClear(new DungeonMap.Wall(WIDTH - 36 - borderThickness, y,
                        borderThickness, height, wallKindFor(random, borderThickness, height)));
            }
        }
    }

    /** 普通墙长度档位：短 70~110、中 130~200、长 220~320、超长 340~450。 */
    private static int pickWallLength(Random random) {
        double roll = random.nextDouble();
        if (roll < 0.25) {
            return 70 + random.nextInt(40);
        }
        if (roll < 0.60) {
            return 130 + random.nextInt(70);
        }
        if (roll < 0.85) {
            return 220 + random.nextInt(100);
        }
        return 340 + random.nextInt(110);
    }

    /** 按墙体方向随机分配形状变体：竖墙适合经典砖块/圆角石台/尖顶塔，
     *  横墙适合经典砖块/圆角石台/垛口墙（仅视觉，碰撞仍为矩形）。 */
    private static int wallKindFor(Random random, int width, int height) {
        if (width < height) {
            return random.nextInt(3);
        }
        return random.nextBoolean() ? 3 : random.nextInt(2);
    }

    /** 放置玩家出生点：Boss 关固定在地图中心正上方，普通关按预设出生点轮换选择首个不与墙体重叠的位置。 */
    public void placePlayerForLevel(Player player, DungeonMap map) {
        // Boss 关：人物固定出生在地图中心正上方（Boss 出生点正上方 150 处）
        if (stage == 5) {
            double spawnX = WIDTH / 2.0;
            double spawnY = HEIGHT / 2.0 - 150;
            if (!map.collidesWithWall((int) spawnX, (int) spawnY, Player.RADIUS)) {
                player.setPx(spawnX);
                player.setPy(spawnY);
                return;
            }
        }
        int[][] spawnPoints = {
                {90, WORLD_TOP + 70}, {WIDTH - 90, WORLD_TOP + 70},
                {90, HEIGHT - 80}, {WIDTH - 90, HEIGHT - 80},
                {WIDTH / 2, WORLD_TOP + 70}, {WIDTH / 2, HEIGHT - 80},
                {230, HEIGHT / 2}, {WIDTH - 230, HEIGHT / 2},
                {WIDTH / 2 - 170, HEIGHT / 2}, {WIDTH / 2 + 170, HEIGHT / 2}
        };
        int start = (wave - 1) % spawnPoints.length;
        for (int i = 0; i < spawnPoints.length; i++) {
            int[] point = spawnPoints[(start + i) % spawnPoints.length];
            if (!map.collidesWithWall(point[0], point[1], Player.RADIUS)) {
                player.setPx(point[0]);
                player.setPy(point[1]);
                return;
            }
        }
        player.setPx(WIDTH / 2.0);
        player.setPy(HEIGHT / 2.0);
    }

    /** 每帧推进刷怪流程：出生预警倒计时结束即正式生成；冷却结束且场上已清空时，为 Boss 关或下一波启动红色准心预警。 */
    public void spawnEnemies(Player player, DungeonMap map) {
        if (spawnWarningTicks > 0) {
            // 预警倒计时结束后，敌人在准心位置伴随红光正式生成
            spawnWarningTicks--;
            if (spawnWarningTicks == 0) {
                doSpawnWave();
            }
            return;
        }
        if (spawnCooldown > 0) {
            return;
        }
        if (stage == 5) {
            startBossWarning();
            return;
        }
        if (wavesRemaining <= 0 || !monsters.isEmpty()) {
            return;
        }
        // 新关卡首波或上一波刚清空：立即为下一波启动红色准心预警
        startWaveWarning(player, map);
    }

    /** 规划本关波次：Boss 关固定 1 波（仅 Boss），普通关随机 1~3 波（1 波 20%、2 波 60%、3 波 20%）。 */
    private void planWaves() {
        wavesTotal = stage == 5 ? 1 : rollWaveCount();
        wavesRemaining = wavesTotal;
    }

    /** 随机波次数：1 波 20%、2 波 60%、3 波 20%。 */
    private int rollWaveCount() {
        double roll = random.nextDouble();
        if (roll < 0.20) {
            return 1;
        }
        if (roll < 0.80) {
            return 2;
        }
        return 3;
    }

    /** 第 waveIndex 波（从 0 起）的怪物数：总数按波数均分，每波固定 4~7 个。 */
    private int waveSizeFor(int waveIndex) {
        int total = enemiesForCurrentLevel();
        int base = total / wavesTotal;
        int count = base + (waveIndex < total % wavesTotal ? 1 : 0);
        return Math.max(4, Math.min(count, 7));
    }

    /** Boss 关：Boss 未出场时启动出生预警（准心固定在地图中心），预警结束后 Boss 带红光生成。 */
    private void startBossWarning() {
        if (bossSpawned) {
            return;
        }
        bossSpawned = true;
        pendingSpawnPoints.clear();
        pendingSpawnKinds.clear();
        pendingSpawnPoints.add(new double[]{WIDTH / 2.0, HEIGHT / 2.0});
        pendingSpawnKinds.add(MonsterType.BOSS);
        spawnWarningTicks = SPAWN_WARNING_TICKS;
    }

    /** 确定下一波敌人的出生点并启动红色准心预警；预警结束后敌人才正式生成。
     *  每波有 5% 概率整波全是精英怪。 */
    private void startWaveWarning(Player player, DungeonMap map) {
        pendingSpawnPoints.clear();
        pendingSpawnKinds.clear();
        int count = waveSizeFor(wavesTotal - wavesRemaining);
        boolean allEliteWave = random.nextDouble() < ELITE_WAVE_CHANCE;
        for (int i = 0; i < count; i++) {
            double[] point = randomOpenPoint(player, map);
            if (point == null) {
                continue;
            }
            MonsterType kind = allEliteWave
                    || random.nextDouble() < ELITE_CHANCE_BASE + world * ELITE_CHANCE_PER_WORLD
                    ? MonsterType.ELITE : MonsterType.NORMAL;
            pendingSpawnPoints.add(point);
            pendingSpawnKinds.add(kind);
        }
        spawnWarningTicks = SPAWN_WARNING_TICKS;
    }

    /** 预警结束：按准心位置正式生成敌人，每只伴随红光入场闪光。 */
    private void doSpawnWave() {
        for (int i = 0; i < pendingSpawnPoints.size(); i++) {
            double[] point = pendingSpawnPoints.get(i);
            Monster enemy = new Monster(point[0], point[1], wave, pendingSpawnKinds.get(i), theme);
            enemy.setSpawnFlashTicks(Monster.SPAWN_FLASH_TICKS);
            monsters.add(enemy);
        }
        pendingSpawnPoints.clear();
        pendingSpawnKinds.clear();
        wavesRemaining--;
    }

    /** 随机取一个不在墙体内、且与玩家保持距离的出生点。 */
    private double[] randomOpenPoint(Player player, DungeonMap map) {
        for (int attempt = 0; attempt < 150; attempt++) {
            double x = 70 + random.nextDouble() * (WIDTH - 140);
            double y = WORLD_TOP + 60 + random.nextDouble() * (HEIGHT - WORLD_TOP - 120);
            if (map.collidesWithWall(x, y, SPAWN_CLEARANCE)) {
                continue;
            }
            if (distance(x, y, player.getPx(), player.getPy()) < MIN_SPAWN_DISTANCE) {
                continue;
            }
            return new double[]{x, y};
        }
        return null;
    }

    /** Boss 召唤小怪：Boss 召唤冷却结束且小怪未达上限时，在 Boss 周围随机位置生成 1~2 只小怪。 */
    public void summonBossMinions(Player player, DungeonMap map) {
        if (stage != 5) {
            return;
        }
        Monster boss = null;
        int minionCount = 0;
        for (Monster enemy : monsters) {
            if (enemy.getType() == MonsterType.BOSS) {
                boss = enemy;
            } else {
                minionCount++;
            }
        }
        if (boss == null || boss.getSummonCooldown() > 0) {
            return;
        }
        int openSlots = MAX_BOSS_MINIONS - minionCount;
        if (openSlots <= 0) {
            // 小怪已满：等有怪死亡后再召唤
            return;
        }
        int summonCount = Math.min(1 + random.nextInt(2), openSlots);
        for (int i = 0; i < summonCount; i++) {
            for (int attempt = 0; attempt < 30; attempt++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double distance = 75 + random.nextDouble() * 45;
                double x = boss.getPx() + Math.cos(angle) * distance;
                double y = boss.getPy() + Math.sin(angle) * distance;
                if (!map.collidesWithWall(x, y, 20)
                        && distance(x, y, player.getPx(), player.getPy()) > 45) {
                    MonsterType kind = random.nextDouble() < BOSS_MINION_ELITE_CHANCE
                            ? MonsterType.ELITE : MonsterType.NORMAL;
                    Monster minion = new Monster(x, y, wave, kind, theme);
                    minion.setSpawnFlashTicks(Monster.SPAWN_FLASH_TICKS);
                    monsters.add(minion);
                    break;
                }
            }
        }
        boss.setSummonCooldown(BOSS_SUMMON_INTERVAL_TICKS);
    }

    /** 更新全部敌人的 AI 与状态：出生闪光与冰冻倒计时；Boss 走专属 AI，远程怪保持距离射击，近战怪寻路追击并近身挥击；Boss 死亡后清空其专属攻击特效。 */
    public void updateEnemies(Player player, DungeonMap map, CombatController combat) {
        // Boss 已死亡：清空其专属特殊攻击残留特效（裂缝/陨石等）
        if (stage == 5 && bossSpawned) {
            boolean bossAlive = false;
            for (Monster monster : monsters) {
                if (monster.getType() == MonsterType.BOSS) {
                    bossAlive = true;
                    break;
                }
            }
            if (!bossAlive) {
                bossController.reset();
            }
        }
        for (Monster enemy : monsters) {
            if (enemy.getSpawnFlashTicks() > 0) {
                enemy.setSpawnFlashTicks(enemy.getSpawnFlashTicks() - 1);
            }
            if (enemy.getFrozenTicks() > 0) {
                // 冰冻：无法移动与攻击，只倒计时
                enemy.setFrozenTicks(enemy.getFrozenTicks() - 1);
                if (enemy.getSummonCooldown() > 0) {
                    enemy.setSummonCooldown(enemy.getSummonCooldown() - 1);
                }
                continue;
            }
            if (enemy.getAttackFlashTicks() > 0) {
                enemy.setAttackFlashTicks(enemy.getAttackFlashTicks() - 1);
            }
            // Boss：专属 AI（特殊攻击接管 + 弹幕模式轮换 + 高速游走），不会近战
            if (enemy.getType() == MonsterType.BOSS) {
                updateBossEnemy(enemy, player, map, combat);
                continue;
            }
            // 远程怪：随机游走移动 + 直线射击，不追击玩家
            if (enemy.getAttackStyle() == 1) {
                updateRangedEnemy(enemy, player, map, combat);
                continue;
            }
            if (enemy.getPathCooldown() <= 0
                    || distance(enemy.getPx(), enemy.getPy(),
                    enemy.getWaypointX(), enemy.getWaypointY()) < 10) {
                updateEnemyPath(enemy, player, map);
                enemy.setPathCooldown(8);
            }
            enemy.setPathCooldown(enemy.getPathCooldown() - 1);
            if (enemy.getSummonCooldown() > 0) {
                enemy.setSummonCooldown(enemy.getSummonCooldown() - 1);
            }

            double dx = enemy.getWaypointX() - enemy.getPx();
            double dy = enemy.getWaypointY() - enemy.getPy();
            double length = Math.max(0.001, Math.hypot(dx, dy));
            double stepX = dx / length * enemy.getSpeed();
            double stepY = dy / length * enemy.getSpeed();
            if (!map.collidesWithWall(enemy.getPx() + stepX, enemy.getPy() + stepY,
                    enemy.getRadius())) {
                enemy.setPx(enemy.getPx() + stepX);
                enemy.setPy(enemy.getPy() + stepY);
                enemy.setStuckTicks(0);
            } else {
                // 撞墙时沿墙滑动：分别尝试仅横移/仅纵移，绕开墙体边缘
                boolean slid = false;
                if (!map.collidesWithWall(enemy.getPx() + stepX, enemy.getPy(), enemy.getRadius())) {
                    enemy.setPx(enemy.getPx() + stepX);
                    slid = true;
                } else if (!map.collidesWithWall(enemy.getPx(), enemy.getPy() + stepY,
                        enemy.getRadius())) {
                    enemy.setPy(enemy.getPy() + stepY);
                    slid = true;
                }
                if (slid) {
                    // 滑动中立即重算路径，避免持续顶着墙走
                    enemy.setStuckTicks(0);
                    enemy.setPathCooldown(0);
                } else {
                    // 完全动弹不得：累计卡住时长，超时后随机绕行脱困
                    enemy.setStuckTicks(enemy.getStuckTicks() + 1);
                    if (enemy.getStuckTicks() > 30) {
                        pickDetourWaypoint(enemy, player, map);
                        enemy.setStuckTicks(0);
                        enemy.setPathCooldown(10);
                    }
                }
            }
            // 近战攻击：贴近角色（未重合）即可挥击，挥击时触发闪光弧
            if (distance(enemy.getPx(), enemy.getPy(), player.getPx(), player.getPy())
                    < enemy.getRadius() + Player.RADIUS + MELEE_ATTACK_RANGE) {
                if (enemy.getAttackCooldown() <= 0) {
                    combat.handleMonsterAttack(enemy, player);
                    enemy.setAttackCooldown(42);
                    enemy.setAttackFlashTicks(Monster.ATTACK_FLASH_TICKS);
                    enemy.setAttackAngle(Math.atan2(
                            player.getPy() - enemy.getPy(), player.getPx() - enemy.getPx()));
                }
                if (enemy.getAttackCooldown() > 0) {
                    enemy.setAttackCooldown(enemy.getAttackCooldown() - 1);
                }
            } else if (enemy.getAttackCooldown() > 0) {
                enemy.setAttackCooldown(enemy.getAttackCooldown() - 1);
            }
        }
    }

    /** Boss AI：专属特殊攻击由 BossController 接管（期间不弹幕）；空闲时高速游走 + 弹幕模式轮换射击。
     *  黄金毒蝎攻击频率更快前摇更短；巨人 Boss 固定不动只攻击。 */
    private void updateBossEnemy(Monster enemy, Player player, DungeonMap map,
                                 CombatController combat) {
        // 召唤小怪冷却（Boss 通用计时，特殊攻击期间也持续递减）
        if (enemy.getSummonCooldown() > 0) {
            enemy.setSummonCooldown(enemy.getSummonCooldown() - 1);
        }
        // 特殊攻击进行中（冲刺/跳跃/激光/三连斩/钩锁等）：接管 Boss，本帧不弹幕不移动
        if (bossController.update(enemy, player, map, combat, monsters)) {
            return;
        }
        boolean immobile = bossController.isImmobile();
        boolean rapid = theme == MapTheme.DESERT; // 黄金毒蝎：高频弹幕
        int aimTicks = rapid ? 10 : 15;
        if (enemy.getAttackCooldown() > 0) {
            enemy.setAttackCooldown(enemy.getAttackCooldown() - 1);
        }
        boolean canShoot = hasClearPath(enemy.getPx(), enemy.getPy(),
                player.getPx(), player.getPy(), 3, map);
        // 瞄准中：静止不动（红色弧光预警），倒计时结束发射并轮换弹幕模式
        if (enemy.getAimTicks() > 0) {
            if (!canShoot) {
                enemy.setAimTicks(0);
                return;
            }
            enemy.setAttackFlashTicks(Monster.ATTACK_FLASH_TICKS);
            enemy.setAttackAngle(Math.atan2(
                    player.getPy() - enemy.getPy(), player.getPx() - enemy.getPx()));
            enemy.setAimTicks(enemy.getAimTicks() - 1);
            if (enemy.getAimTicks() <= 0) {
                fireRangedBullet(enemy, player, combat);
                enemy.setAttackCooldown(bossCooldownFor(enemy, rapid));
                enemy.setAttackPattern((enemy.getAttackPattern() + 1) % 7); // 会全部七种弹幕
            }
            return;
        }
        // 视线无遮挡且冷却结束：进入瞄准静止
        if (canShoot && enemy.getAttackCooldown() <= 0) {
            enemy.setAimTicks(aimTicks);
            enemy.setAttackAngle(Math.atan2(
                    player.getPy() - enemy.getPy(), player.getPx() - enemy.getPx()));
            return;
        }
        if (immobile) {
            return; // 巨人固定不动，只攻击不移动
        }
        double distToPlayer = distance(enemy.getPx(), enemy.getPy(),
                player.getPx(), player.getPy());
        if (distToPlayer > RANGED_PREFERRED_MAX || !canShoot) {
            // 离得太远或视线被挡：BFS 寻路靠近玩家
            if (enemy.getPathCooldown() <= 0
                    || distance(enemy.getPx(), enemy.getPy(),
                    enemy.getWaypointX(), enemy.getWaypointY()) < 10) {
                updateEnemyPath(enemy, player, map);
                enemy.setPathCooldown(8);
            }
            enemy.setPathCooldown(enemy.getPathCooldown() - 1);
            moveRangedEnemy(enemy, map, true, player);
        } else if (distToPlayer < RANGED_PREFERRED_MIN) {
            // 离得太近：后退拉开距离
            double awayX = enemy.getPx() - player.getPx();
            double awayY = enemy.getPy() - player.getPy();
            double len = Math.max(0.001, Math.hypot(awayX, awayY));
            enemy.setWaypointX(clamp(enemy.getPx() + awayX / len * 120,
                    60, DungeonMap.WIDTH - 60));
            enemy.setWaypointY(clamp(enemy.getPy() + awayY / len * 120,
                    DungeonMap.WORLD_TOP + 30, DungeonMap.HEIGHT - 40));
            moveRangedEnemy(enemy, map, false, player);
        } else {
            // 不远不近：自由随机游走（Boss 移速全游戏最大）
            if (enemy.getPathCooldown() <= 0
                    || distance(enemy.getPx(), enemy.getPy(),
                    enemy.getWaypointX(), enemy.getWaypointY()) < 10
                    || enemy.getStuckTicks() > 30) {
                pickRangedWaypoint(enemy, map);
                enemy.setPathCooldown(30 + random.nextInt(40));
            }
            enemy.setPathCooldown(enemy.getPathCooldown() - 1);
            moveRangedEnemy(enemy, map, false, player);
        }
    }

    /** Boss 射击冷却：毒蝎高频（三分之一）、其余为小怪弹幕冷却的 65%。 */
    private int bossCooldownFor(Monster enemy, boolean rapid) {
        int base = rapid ? 40 : 30; // 单发模式基准
        switch (enemy.getAttackPattern()) {
            case 1: return rapid ? 35 : 68;
            case 2: return rapid ? 53 : 104;
            case 3: return rapid ? 3 : 7;
            case 4: return rapid ? 42 : 81;
            case 5: return rapid ? 37 : 72;
            case 6: return rapid ? 37 : 72;
            default: return base;
        }
    }

    /** 获取 Boss 专属特殊攻击控制器（视图层读取特效状态绘制）。 */
    public BossController getBossController() { return bossController; }

    /** 远程怪 AI：离玩家太远或视线被挡时寻路靠近，保持不远不近的距离自由移动，
     *  视线无遮挡即持续射击（射程无限）。 */
    private void updateRangedEnemy(Monster enemy, Player player, DungeonMap map,
                                   CombatController combat) {
        double distToPlayer = distance(enemy.getPx(), enemy.getPy(),
                player.getPx(), player.getPy());
        boolean canShoot = hasClearPath(enemy.getPx(), enemy.getPy(),
                player.getPx(), player.getPy(), 3, map);

        // 冷却递减
        if (enemy.getAttackCooldown() > 0) {
            enemy.setAttackCooldown(enemy.getAttackCooldown() - 1);
        }

        // 瞄准中：静止不动 0.5 秒（红色弧光预警），倒计时结束发射；视线被挡则取消瞄准
        if (enemy.getAimTicks() > 0) {
            if (!canShoot) {
                enemy.setAimTicks(0);
            } else {
                enemy.setAttackFlashTicks(Monster.ATTACK_FLASH_TICKS);
                enemy.setAttackAngle(Math.atan2(
                        player.getPy() - enemy.getPy(), player.getPx() - enemy.getPx()));
                enemy.setAimTicks(enemy.getAimTicks() - 1);
                if (enemy.getAimTicks() <= 0) {
                    fireRangedBullet(enemy, player, combat);
                    enemy.setAttackCooldown(rangedCooldownFor(enemy));
                    enemy.setAttackFlashTicks(Monster.ATTACK_FLASH_TICKS);
                    enemy.setAttackAngle(Math.atan2(
                            player.getPy() - enemy.getPy(), player.getPx() - enemy.getPx()));
                }
            }
            return;
        }

        // 视线无遮挡且冷却结束：进入 0.5 秒瞄准静止，本帧开始站定不动
        if (canShoot && enemy.getAttackCooldown() <= 0) {
            enemy.setAimTicks(RANGED_AIM_TICKS);
            enemy.setAttackAngle(Math.atan2(
                    player.getPy() - enemy.getPy(), player.getPx() - enemy.getPx()));
            return;
        }

        if (distToPlayer > RANGED_PREFERRED_MAX || !canShoot) {
                // 离得太远或视线被墙体挡住：BFS 寻路靠近玩家
                if (enemy.getPathCooldown() <= 0
                        || distance(enemy.getPx(), enemy.getPy(),
                        enemy.getWaypointX(), enemy.getWaypointY()) < 10) {
                    updateEnemyPath(enemy, player, map);
                    enemy.setPathCooldown(8);
                }
                enemy.setPathCooldown(enemy.getPathCooldown() - 1);
                moveRangedEnemy(enemy, map, true, player);
            } else if (distToPlayer < RANGED_PREFERRED_MIN) {
                // 离得太近：朝远离玩家的方向后退，拉开距离
                double awayX = enemy.getPx() - player.getPx();
                double awayY = enemy.getPy() - player.getPy();
                double len = Math.max(0.001, Math.hypot(awayX, awayY));
                enemy.setWaypointX(clamp(enemy.getPx() + awayX / len * 120,
                        60, DungeonMap.WIDTH - 60));
                enemy.setWaypointY(clamp(enemy.getPy() + awayY / len * 120,
                        DungeonMap.WORLD_TOP + 30, DungeonMap.HEIGHT - 40));
                moveRangedEnemy(enemy, map, false, player);
            } else {
                // 不远不近：自由随机游走（走出区间后自然回到上述分支）
                if (enemy.getPathCooldown() <= 0
                        || distance(enemy.getPx(), enemy.getPy(),
                        enemy.getWaypointX(), enemy.getWaypointY()) < 10
                        || enemy.getStuckTicks() > 30) {
                    pickRangedWaypoint(enemy, map);
                    enemy.setPathCooldown(30 + random.nextInt(40));
                }
                enemy.setPathCooldown(enemy.getPathCooldown() - 1);
                moveRangedEnemy(enemy, map, false, player);
            }
    }

    /** 远程怪射击冷却帧数：弹幕越密集冷却越久（旋转扫射高频旋转发射）。 */
    private int rangedCooldownFor(Monster enemy) {
        switch (enemy.getAttackPattern()) {
            case 1: return 105;  // 一排并排弹
            case 2: return 160;  // 环形扩散弹幕
            case 3: return 10;   // 旋转扫射：每 10 帧一发持续转圈
            case 4: return 125;  // 散弹扇形喷射
            case 5: return 110;  // 十字交叉弹
            case 6: return 110;  // 三连齐射
            default: return RANGED_ATTACK_COOLDOWN; // 0 单发
        }
    }

    /** 按远程怪的攻击模式发射对应弹幕。 */
    private void fireRangedBullet(Monster enemy, Player player, CombatController combat) {
        double baseAngle = Math.atan2(player.getPy() - enemy.getPy(),
                player.getPx() - enemy.getPx());
        switch (enemy.getAttackPattern()) {
            case 1: { // 一排并排子弹：沿射击方向垂直排开 3~5 颗（间隔拉大）
                int count = 3 + random.nextInt(3);
                for (int i = 0; i < count; i++) {
                    double offset = (i - (count - 1) / 2.0) * 0.18;
                    combat.spawnEnemyBulletAt(enemy, baseAngle + offset);
                }
                break;
            }
            case 2: { // 环形扩散：以自身为中心向四周均匀发射一圈（弹幕更稀疏）
                int count = 9 + random.nextInt(5);
                for (int i = 0; i < count; i++) {
                    combat.spawnEnemyBulletAt(enemy, Math.PI * 2 * i / count);
                }
                break;
            }
            case 3: // 旋转扫射：每次发射后旋转角度，持续转圈扫射
                combat.spawnEnemyBulletAt(enemy, enemy.getBulletAngle());
                enemy.setBulletAngle(enemy.getBulletAngle() + 0.5);
                break;
            case 4: { // 散弹枪：一次性扇形喷出大量弹丸（散布更开）
                int count = 6 + random.nextInt(4);
                for (int i = 0; i < count; i++) {
                    double spread = (random.nextDouble() - 0.5) * 1.5;
                    combat.spawnShotgunPellet(enemy, baseAngle + spread);
                }
                break;
            }
            case 5: { // 十字交叉弹：以玩家方向为基准的前后左右四向
                for (int i = 0; i < 4; i++) {
                    combat.spawnEnemyBulletAt(enemy, baseAngle + Math.PI / 2 * i);
                }
                break;
            }
            case 6: { // 三连齐射：三颗夹角更开
                for (int i = -1; i <= 1; i++) {
                    combat.spawnEnemyBulletAt(enemy, baseAngle + i * 0.2);
                }
                break;
            }
            default: // 0 单发：朝玩家一颗
                combat.spawnEnemyBullet(enemy, player);
                break;
        }
    }

    /** 远程怪沿 waypoint 移动一步：碰撞检测 + 沿墙滑动；完全卡住超时后按模式重新选路。 */
    private void moveRangedEnemy(Monster enemy, DungeonMap map, boolean pursuing, Player player) {
        double dx = enemy.getWaypointX() - enemy.getPx();
        double dy = enemy.getWaypointY() - enemy.getPy();
        double length = Math.max(0.001, Math.hypot(dx, dy));
        double stepX = dx / length * enemy.getSpeed();
        double stepY = dy / length * enemy.getSpeed();
        if (!map.collidesWithWall(enemy.getPx() + stepX, enemy.getPy() + stepY,
                enemy.getRadius())) {
            enemy.setPx(enemy.getPx() + stepX);
            enemy.setPy(enemy.getPy() + stepY);
            enemy.setStuckTicks(0);
            return;
        }
        // 撞墙时沿墙滑动：分别尝试仅横移/仅纵移
        boolean slid = false;
        if (!map.collidesWithWall(enemy.getPx() + stepX, enemy.getPy(), enemy.getRadius())) {
            enemy.setPx(enemy.getPx() + stepX);
            slid = true;
        } else if (!map.collidesWithWall(enemy.getPx(), enemy.getPy() + stepY,
                enemy.getRadius())) {
            enemy.setPy(enemy.getPy() + stepY);
            slid = true;
        }
        if (slid) {
            return;
        }
        // 完全动弹不得：累计卡住时长，超时后重新选路
        enemy.setStuckTicks(enemy.getStuckTicks() + 1);
        if (enemy.getStuckTicks() > 30) {
            if (pursuing) {
                updateEnemyPath(enemy, player, map);
                enemy.setPathCooldown(8);
            } else {
                pickRangedWaypoint(enemy, map);
                enemy.setPathCooldown(20 + random.nextInt(30));
            }
            enemy.setStuckTicks(0);
        }
    }

    /** 远程怪随机游走：随机角度与距离的新目标点（避开墙体，限制在地图范围内）。 */
    private void pickRangedWaypoint(Monster enemy, DungeonMap map) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = 90 + random.nextDouble() * 150;
            double wx = clamp(enemy.getPx() + Math.cos(angle) * dist,
                    60, DungeonMap.WIDTH - 60);
            double wy = clamp(enemy.getPy() + Math.sin(angle) * dist,
                    DungeonMap.WORLD_TOP + 30, DungeonMap.HEIGHT - 40);
            if (!map.collidesWithWall(wx, wy, enemy.getRadius())) {
                enemy.setWaypointX(wx);
                enemy.setWaypointY(wy);
                enemy.setStuckTicks(0);
                return;
            }
        }
        // 多次尝试失败（周围都是墙）：原地待命，下一轮重新尝试
        enemy.setWaypointX(enemy.getPx());
        enemy.setWaypointY(enemy.getPy());
    }

    /** 近战怪寻路：与玩家之间无墙体遮挡时直取玩家位置，否则在路径网格上做 BFS 搜索，取路径上的下一格作为航点；
     *  目标格不可达时，改取已探索格中离玩家最近的一格。 */
    private void updateEnemyPath(Monster enemy, Player player, DungeonMap map) {
        if (hasClearPath(enemy.getPx(), enemy.getPy(), player.getPx(), player.getPy(),
                enemy.getRadius(), map)) {
            enemy.setWaypointX(player.getPx());
            enemy.setWaypointY(player.getPy());
            return;
        }
        int startColumn = pathColumn(enemy.getPx());
        int startRow = pathRow(enemy.getPy());
        int goalColumn = pathColumn(player.getPx());
        int goalRow = pathRow(player.getPy());
        int start = startRow * PATH_COLUMNS + startColumn;
        int goal = goalRow * PATH_COLUMNS + goalColumn;
        int[] parents = new int[PATH_COLUMNS * PATH_ROWS];
        java.util.Arrays.fill(parents, -1);
        boolean[] visited = new boolean[parents.length];
        int[] queue = new int[parents.length];
        int head = 0;
        int tail = 0;
        queue[tail++] = start;
        visited[start] = true;
        int[] columnOffsets = {1, -1, 0, 0};
        int[] rowOffsets = {0, 0, 1, -1};

        while (head < tail && !visited[goal]) {
            int current = queue[head++];
            int currentColumn = current % PATH_COLUMNS;
            int currentRow = current / PATH_COLUMNS;
            for (int i = 0; i < columnOffsets.length; i++) {
                int nextColumn = currentColumn + columnOffsets[i];
                int nextRow = currentRow + rowOffsets[i];
                if (nextColumn < 0 || nextColumn >= PATH_COLUMNS
                        || nextRow < 0 || nextRow >= PATH_ROWS) {
                    continue;
                }
                int next = nextRow * PATH_COLUMNS + nextColumn;
                if (visited[next] || (next != goal
                        && pathCellBlocked(nextColumn, nextRow, enemy.getRadius(), map))) {
                    continue;
                }
                visited[next] = true;
                parents[next] = current;
                queue[tail++] = next;
            }
        }

        if (!visited[goal]) {
            // 目标格被墙阻隔：改取 BFS 已探索格中离玩家最近的一格，避免直冲墙体
            int target = start;
            double bestDistance = Double.MAX_VALUE;
            for (int cell = 0; cell < visited.length; cell++) {
                if (!visited[cell]) {
                    continue;
                }
                double cellX = cell % PATH_COLUMNS * PATH_CELL_SIZE + PATH_CELL_SIZE / 2.0;
                double cellY = WORLD_TOP + cell / PATH_COLUMNS * PATH_CELL_SIZE
                        + PATH_CELL_SIZE / 2.0;
                double d = distance(cellX, cellY, player.getPx(), player.getPy());
                if (d < bestDistance) {
                    bestDistance = d;
                    target = cell;
                }
            }
            if (target == start) {
                // 完全被困：随机绕行脱困
                pickDetourWaypoint(enemy, player, map);
                return;
            }
            int step = target;
            while (parents[step] != -1 && parents[step] != start) {
                step = parents[step];
            }
            enemy.setWaypointX(step % PATH_COLUMNS * PATH_CELL_SIZE + PATH_CELL_SIZE / 2.0);
            enemy.setWaypointY(WORLD_TOP + step / PATH_COLUMNS * PATH_CELL_SIZE
                    + PATH_CELL_SIZE / 2.0);
            return;
        }
        int step = goal;
        while (parents[step] != -1 && parents[step] != start) {
            step = parents[step];
        }
        enemy.setWaypointX(step % PATH_COLUMNS * PATH_CELL_SIZE + PATH_CELL_SIZE / 2.0);
        enemy.setWaypointY(WORLD_TOP + step / PATH_COLUMNS * PATH_CELL_SIZE + PATH_CELL_SIZE / 2.0);
    }

    /** 敌人被墙卡住时，在周围随机找一个可达点作为临时绕行目标。 */
    private void pickDetourWaypoint(Monster enemy, Player player, DungeonMap map) {
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double range = 70 + random.nextDouble() * 60;
            double x = enemy.getPx() + Math.cos(angle) * range;
            double y = enemy.getPy() + Math.sin(angle) * range;
            if (!map.collidesWithWall(x, y, enemy.getRadius())
                    && hasClearPath(enemy.getPx(), enemy.getPy(), x, y,
                    enemy.getRadius(), map)) {
                enemy.setWaypointX(x);
                enemy.setWaypointY(y);
                return;
            }
        }
        // 找不到绕行点时朝玩家方向移动，交给滑动逻辑沿墙绕行
        enemy.setWaypointX(player.getPx());
        enemy.setWaypointY(player.getPy());
    }

    /** 判断路径网格某格是否可通行：以格子中心做墙体碰撞检测。 */
    private boolean pathCellBlocked(int column, int row, double radius, DungeonMap map) {
        double centerX = column * PATH_CELL_SIZE + PATH_CELL_SIZE / 2.0;
        double centerY = WORLD_TOP + row * PATH_CELL_SIZE + PATH_CELL_SIZE / 2.0;
        return map.collidesWithWall(centerX, centerY, radius);
    }

    /** 两点间视线检测：沿连线每 10px 采样一次墙体碰撞，全部畅通返回 true。 */
    private boolean hasClearPath(double startX, double startY, double endX, double endY,
                                 double radius, DungeonMap map) {
        double length = distance(startX, startY, endX, endY);
        int samples = Math.max(1, (int) Math.ceil(length / 10));
        for (int i = 1; i <= samples; i++) {
            double progress = i / (double) samples;
            double x = startX + (endX - startX) * progress;
            double y = startY + (endY - startY) * progress;
            if (map.collidesWithWall(x, y, radius)) {
                return false;
            }
        }
        return true;
    }

    /** 世界坐标转路径网格列索引（越界时钳制）。 */
    private int pathColumn(double x) {
        return (int) clamp(x / PATH_CELL_SIZE, 0, PATH_COLUMNS - 1);
    }

    /** 世界坐标转路径网格行索引（越界时钳制）。 */
    private int pathRow(double y) {
        return (int) clamp((y - WORLD_TOP) / PATH_CELL_SIZE, 0, PATH_ROWS - 1);
    }

    /** 生成通关传送门：Boss 关固定在地图中心，普通关随机取一个远离玩家且不与墙体碰撞的位置。 */
    public void spawnPortal(Player player, DungeonMap map) {
        if (isBossStage()) {
            // Boss 关：传送门固定生成在地图中心（宝箱由 RewardController 放在其正下方）
            portal = new Portal(WIDTH / 2.0, HEIGHT / 2.0);
            return;
        }
        for (int attempt = 0; attempt < 150; attempt++) {
            double x = 80 + random.nextDouble() * (WIDTH - 160);
            double y = WORLD_TOP + 70 + random.nextDouble() * (HEIGHT - WORLD_TOP - 150);
            if (!map.collidesWithWall(x, y, 30)
                    && distance(x, y, player.getPx(), player.getPy()) > 150) {
                portal = new Portal(x, y);
                return;
            }
        }
        portal = new Portal(WIDTH / 2.0, HEIGHT / 2.0);
    }

    /** 数值钳制到 [min, max] 区间。 */
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /** 两点间距离。 */
    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }
}