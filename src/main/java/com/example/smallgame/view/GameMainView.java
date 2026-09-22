package com.example.smallgame.view;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import com.example.smallgame.controller.combat.CombatController;
import com.example.smallgame.controller.level.LevelController;
import com.example.smallgame.controller.reward.RewardController;
import com.example.smallgame.controller.reward.TreasureBox;
import com.example.smallgame.controller.skill.SkillController;
import com.example.smallgame.model.entity.DungeonMap;
import com.example.smallgame.model.entity.MapTheme;
import com.example.smallgame.model.entity.Monster;
import com.example.smallgame.model.entity.MonsterType;
import com.example.smallgame.model.entity.Player;
import com.example.smallgame.model.entity.Portal;
import com.example.smallgame.model.entity.Reward;
import com.example.smallgame.model.entity.WeaponDrop;
import com.example.smallgame.model.entity.WeaponType;
import java.io.InputStream;
import java.util.List;

/**
 * 游戏主视图：持有 GamePanel 游戏画布（主循环、渲染与输入处理均在其中）。
 * GamePanel 内置固定 60Hz 逻辑帧循环，整合关卡、战斗、奖励与技能控制器，
 * 以低分辨率像素画布放大渲染，并负责菜单/暂停/结算界面的绘制与交互；
 * 实体绘制全部委托给静态渲染器（EntityRenderer、PlayerRenderer、MonsterAppearance、
 * MapSceneryRenderer、BossEffectRenderer、BulletRenderer、SkillEffectRenderer），
 * 界面配色经 Palette 解码，结算界面由 GameEndView 绘制。
 * <p>
 * 引用文件：controller（LevelController、CombatController、RewardController、
 * SkillController、TreasureBox）、model.entity（Player、Monster、
 * DungeonMap、MapTheme、Portal、Reward、WeaponDrop、WeaponType）、
 * view（CharacterSelectView、PauseView、MapSceneryRenderer、EntityRenderer、
 * PlayerRenderer、MonsterAppearance、GameEndView、Palette、BossEffectRenderer、
 * BulletRenderer、SkillEffectRenderer）。
 * 被 GameApplication（经由 AppView 挂载游戏画布）使用。
 */
public class GameMainView {

    public static final class GamePanel extends Canvas {
        private static final int WIDTH = DungeonMap.WIDTH;
        private static final int HEIGHT = DungeonMap.HEIGHT;
        private static final int WORLD_TOP = DungeonMap.WORLD_TOP;
        /** 逻辑帧时长：60Hz 固定步长（纳秒）。 */
        private static final long TICK_NANOS = 1_000_000_000L / 60;
        /** 像素风缩放：画布先以 1/2 分辨率渲染，再放大 2 倍输出。 */
        private static final int PIXEL_SCALE = 2;
        /** 关卡名淡入淡出总帧数（约 2.5 秒）。 */
        private static final int LEVEL_INTRO_TICKS = 150;
        /** 关卡名淡入/淡出各占的帧数。 */
        private static final int LEVEL_INTRO_FADE = 30;
        /** 人物入场延迟：关卡名淡入动画开始后 0.8 秒（48 帧）人物瞬间出现，出现前不可见。 */
        private static final int SPAWN_DELAY_TICKS = 48;
        /** 人物出现后 0.1 秒（6 帧）内锁定操作，无法移动与攻击。 */
        private static final int SPAWN_LOCK_TICKS = 6;
        /** 人物出现时的蓝色闪光特效持续帧数（快速闪烁）。 */
        private static final int SPAWN_FLASH_TICKS = 10;
        /** 银白色盾牌图标（护盾状态栏）。 */
        private static final Image SHIELD_ICON = loadShieldIcon();

        /** 加载护盾状态栏图标。 */
        private static Image loadShieldIcon() {
            InputStream stream = GameMainView.class.getResourceAsStream(
                    "/sprites/ui/shield_icon.png");
            return new Image(stream);
        }

        /** 关卡控制器（地图/波次/敌人 AI）。 */
        private final LevelController levelController = new LevelController();
        /** 战斗控制器（武器攻击与弹幕）。 */
        private final CombatController combatController = new CombatController();
        /** 奖励控制器（能量点/武器掉落/宝箱）。 */
        private final RewardController rewardController = new RewardController();
        /** 技能控制器（英雄技能与特效）。 */
        private final SkillController skillController = new SkillController();
        /** 角色选择面板。 */
        private final CharacterSelectView characterSelectView = new CharacterSelectView();

        /** 低分辨率像素画布与快照图像（渲染缓冲，最终放大输出到主画布）。 */
        private final Canvas pixelCanvas = new Canvas(WIDTH / PIXEL_SCALE, HEIGHT / PIXEL_SCALE);
        private final WritableImage pixelImage =
                new WritableImage(WIDTH / PIXEL_SCALE, HEIGHT / PIXEL_SCALE);
        /** 键盘按键按下状态表。 */
        private final boolean[] keys = new boolean[256];
        /** 鼠标世界坐标。 */
        private double mouseX = WIDTH / 2.0;
        private double mouseY = HEIGHT / 2.0;
        /** 主循环定时器。 */
        private final AnimationTimer timer;
        /** 玩家实体。 */
        private Player player;
        /** 关卡入场动画剩余帧数。 */
        private int roomFlash;
        /** 逻辑帧计数。 */
        private int logicTick;
        /** 真实累计秒数（驱动立绘动画）。 */
        private double elapsedSeconds;
        /** 左键是否按住。 */
        private boolean mouseDown;
        /** 死亡结算标志。 */
        private boolean gameOver;
        /** 通关结算标志。 */
        private boolean victory;
        /** 上一帧是否处于闪避中（用于闪避结束后接续按住左键的操作）。 */
        private boolean wasDodging;
        /** 本次死亡随机抽取的死亡信息（死亡界面正上方红色显示）。 */
        private String deathMessage = "";
        private Screen screen = Screen.MENU;

        /** 界面状态：主菜单 / 对局中 / 暂停。 */
        private enum Screen {
            MENU, PLAYING, PAUSED
        }

        /** 构造画布：绑定键鼠事件，启动固定步长主循环并进入初始菜单。 */
        public GamePanel() {
            setWidth(WIDTH);
            setHeight(HEIGHT);
            setFocusTraversable(true);
            setOnKeyPressed(this::handleKeyPressed);
            setOnKeyReleased(this::handleKeyReleased);
            setOnMouseMoved(this::handleMouseMoved);
            setOnMouseDragged(this::handleMouseMoved);
            setOnMousePressed(this::handleMousePressed);
            setOnMouseReleased(this::handleMouseReleased);
            timer = new AnimationTimer() {
                private long lastUpdate;
                private long accumulated;
                @Override
                public void handle(long now) {
                    if (lastUpdate == 0) {
                        lastUpdate = now;
                        return;
                    }
                    long delta = now - lastUpdate;
                    lastUpdate = now;
                    accumulated += delta;
                    while (accumulated >= TICK_NANOS) {
                        updateGame();
                        accumulated -= TICK_NANOS;
                    }
                    elapsedSeconds += delta / 1_000_000_000.0;
                    render();
                }
            };
            // 预热英雄与武器立绘静态缓存，避免战斗中首次加载造成卡顿
            PlayerRenderer.preload();
            EntityRenderer.preload();
            BulletRenderer.preload();
            SkillEffectRenderer.preload();
            resetGame();
            timer.start();
        }

        /** 重置对局：重建玩家与各控制器状态、按关卡放置出生点并回到主菜单。 */
        private void resetGame() {
            player = new Player(WIDTH / 2.0, 360, characterSelectView.getSelectedHero());
            combatController.clearBullets();
            rewardController.reset();
            skillController.reset();
            levelController.reset();
            levelController.placePlayerForLevel(player, levelController.getCurrentMap());
            roomFlash = LEVEL_INTRO_TICKS;
            mouseDown = false;
            wasDodging = false;
            gameOver = false;
            victory = false;
            deathMessage = "";
            screen = Screen.MENU;
        }

        /** 开始游戏：重置对局后切换到 PLAYING 状态。 */
        private void startGame() {
            resetGame();
            screen = Screen.PLAYING;
            requestFocus();
        }

        /** 退出程序：停止主循环、关闭窗口并结束 JavaFX 进程。 */
        private void exitGame() {
            timer.stop();
            if (getScene() != null && getScene().getWindow() != null) {
                getScene().getWindow().hide();
            }
            Platform.exit();
        }

        /** 进入下一关：推进关卡（最终关直接胜利），清理战场并继承生命/能量/技能冷却状态。 */
        private void enterNextLevel() {
            if (levelController.advanceLevel()) {
                victory = true;
                return;
            }
            combatController.clearBullets();
            rewardController.reset();
            // 技能冷却继承上一关状态（仅结束激活中的技能与增益）；护盾直接刷新满，生命值与能量值继承
            skillController.onLevelTransition(player);
            player.setShield(player.getMaxShield());
            player.setFireCooldown(0);
            player.setMeleeCooldown(0);
            player.setSlashTimer(0);
            player.setHandKnifeTimer(0);
            player.resetWeaponState();
            levelController.placePlayerForLevel(player, levelController.getCurrentMap());
            roomFlash = LEVEL_INTRO_TICKS;
        }

        /** 单帧逻辑更新：输入 → 移动 → 攻击 → 敌人 → 拾取 → 关卡完成检测的完整时序。
         *  非对局状态下逻辑帧计数不推进，战场完全静止（含动画相位）。 */
        private void updateGame() {
            if (screen != Screen.PLAYING || gameOver || victory) {
                return;
            }
            logicTick++;
            if (roomFlash > 0) {
                roomFlash--;
            }
            DungeonMap map = levelController.getCurrentMap();
            List<Monster> monsters = levelController.getMonsters();
            // 入场锁定：人物出现前及出现后 0.1 秒内不可操作
            boolean inputLocked = spawnLockActive();
            // 闪避期间：无法移动与攻击（位移完全由闪避插值控制）；钩锁拉拽期间：无法移动但仍可攻击
            boolean dodging = skillController.isDodging();
            boolean hooked = player.getHookedTicks() > 0;
            player.update(!inputLocked && !dodging && !hooked && (keys[KeyCode.W.getCode()] || keys[KeyCode.UP.getCode()]),
                    !inputLocked && !dodging && !hooked && (keys[KeyCode.S.getCode()] || keys[KeyCode.DOWN.getCode()]),
                    !inputLocked && !dodging && !hooked && (keys[KeyCode.A.getCode()] || keys[KeyCode.LEFT.getCode()]),
                    !inputLocked && !dodging && !hooked && (keys[KeyCode.D.getCode()] || keys[KeyCode.RIGHT.getCode()]), map);
            if (!inputLocked && !dodging && mouseDown) {
                combatController.attackHeld(player, mouseX, mouseY, monsters, rewardController::dropLoot);
            }
            // 敌人波次生成：入场锁定期间不启动（红色准心预警需在人物可操作后显示）
            if (!inputLocked) {
                levelController.spawnEnemies(player, map);
            }
            combatController.updateBullets(monsters, map, rewardController::dropLoot);
            combatController.updateEnemyBullets(player, map);
            levelController.updateEnemies(player, map, combatController);
            // Boss 半血奖励：血量首次降至一半时掉落与击杀同数量的能量点（一次性触发）
            for (Monster monster : monsters) {
                if (monster.getType() == MonsterType.BOSS && monster.isAlive()
                        && !monster.isHalfHpLootDropped()
                        && monster.getHp() <= monster.getMaxHp() / 2) {
                    monster.setHalfHpLootDropped(true);
                    rewardController.dropHalfHpLoot(monster);
                }
            }
            levelController.summonBossMinions(player, map);
            skillController.update(player, monsters, rewardController::dropLoot);
            // 闪避刚结束：接续闪避前/闪避中按住的左键操作（枪连发、弓续蓄力、剑再挥砍）；
            // 若闪避期间松开左键，则补一次松弦结算（非弓无副作用）
            if (wasDodging && !skillController.isDodging()) {
                if (mouseDown) {
                    combatController.attackPressed(player, mouseX, mouseY,
                            levelController.getMonsters(), rewardController::dropLoot);
                } else {
                    combatController.attackReleased(player, mouseX, mouseY,
                            levelController.getMonsters(), rewardController::dropLoot);
                }
            }
            wasDodging = skillController.isDodging();
            rewardController.updatePickups(player);
            rewardController.updateWeaponDrops();
            if (player.getHp() <= 0) {
                gameOver = true;
                deathMessage = GameEndView.randomDeathMessage();
                // 死亡瞬间立即冻结战场：本帧起人物、敌人、弹幕全部静止
                return;
            }
            if (levelController.checkLevelComplete()
                    && levelController.getPortal() == null) {
                levelController.spawnPortal(player, map);
                Portal portal = levelController.getPortal();
                // 3-5 通关直接进入结算，不再生成宝箱
                if (!levelController.isFinalStage()) {
                    rewardController.spawnTreasureBox(player, map, levelController.isBossStage(),
                            levelController.getWorld(), portal.getPx(), portal.getPy());
                }
            }
            rewardController.updateTreasureBox(player);
            levelController.decrementSpawnCooldown();
        }

        /** 渲染一帧：像素画布绘制游戏世界后放大输出，再叠加 HUD/菜单/暂停/结算界面。 */
        private void render() {
            GraphicsContext output = getGraphicsContext2D();
            GraphicsContext g = pixelCanvas.getGraphicsContext2D();
            g.setTransform(1, 0, 0, 1, 0, 0);
            g.clearRect(0, 0, pixelCanvas.getWidth(), pixelCanvas.getHeight());
            g.scale(1.0 / PIXEL_SCALE, 1.0 / PIXEL_SCALE);
            g.setLineCap(StrokeLineCap.SQUARE);
            g.setLineJoin(StrokeLineJoin.MITER);
            drawBackground(g);
            if (screen != Screen.MENU) {
                drawGameWorld(g);
            }
            pixelCanvas.snapshot(null, pixelImage);
            output.setTransform(1, 0, 0, 1, 0, 0);
            output.clearRect(0, 0, getWidth(), getHeight());
            output.setImageSmoothing(false);
            output.drawImage(pixelImage, 0, 0, getWidth(), getHeight());
            output.setTransform(getWidth() / (double) WIDTH, 0, 0,
                    getHeight() / (double) HEIGHT, 0, 0);
            output.setLineCap(StrokeLineCap.SQUARE);
            output.setLineJoin(StrokeLineJoin.MITER);
            if (screen == Screen.MENU) {
                drawMenuScreen(output);
            } else {
                drawHud(output);
                drawLevelIntro(output);
                drawGameHints(output);
                if (screen == Screen.PAUSED) {
                    PauseView.draw(output, WIDTH, HEIGHT);
                }
                if (gameOver) {
                    GameEndView.drawDeathScreen(output, WIDTH, HEIGHT, deathMessage);
                } else if (victory) {
                    GameEndView.drawVictoryScreen(output, WIDTH, HEIGHT);
                }
            }
        }

        /** 绘制游戏世界：按装饰 → 墙体 → 障碍物 → 掉落物 → 弹幕 → 敌人 → 特效的层级顺序。 */
        private void drawGameWorld(GraphicsContext g) {
            MapTheme theme = levelController.getTheme();
            // 场景装饰：主题专属物件（纯视觉、无碰撞），画在墙体之下
            MapSceneryRenderer.drawDecorations(g, theme,
                    levelController.getCurrentMap().getDecorations(), logicTick);
            for (DungeonMap.Wall wall : levelController.getCurrentMap().getWalls()) {
                EntityRenderer.drawWall(g, wall, theme);
            }
            // 障碍物：可碰撞的主题地形物件（树、土坡、冰块等），画在墙体之上
            MapSceneryRenderer.drawObstacles(g, theme,
                    levelController.getCurrentMap().getObstacles(), logicTick);
            // 墙体装饰：主题专属装饰叠加在墙体表面（纯视觉、无碰撞）
            MapSceneryRenderer.drawWallDecorations(g, theme,
                    levelController.getCurrentMap().getWalls(), logicTick);
            for (Reward pickup : rewardController.getPickups()) {
                EntityRenderer.drawReward(g, pickup, logicTick);
            }
            for (WeaponDrop drop : rewardController.getWeaponDrops()) {
                EntityRenderer.drawWeaponDrop(g, drop, logicTick, player.getPx(), player.getPy());
            }
            TreasureBox treasureBox = rewardController.getTreasureBox();
            if (treasureBox != null) {
                EntityRenderer.drawTreasureBox(g, treasureBox, logicTick);
            }
            Portal portal = levelController.getPortal();
            if (portal != null) {
                EntityRenderer.drawPortal(g, portal, logicTick);
            }
            for (CombatController.Bullet bullet : combatController.getBullets()) {
                BulletRenderer.drawBullet(g, bullet);
            }
            // 敌人子弹（远程怪发射，命中玩家造成伤害）
            BulletRenderer.drawEnemyBullets(g, combatController.getEnemyBullets());
            for (Monster enemy : levelController.getMonsters()) {
                MonsterAppearance.drawMonster(g, enemy, logicTick);
            }
            // Boss 专属特殊攻击特效（巨人激光/陨石/裂缝/火球/钩锁），画在敌人之上
            BossEffectRenderer.draw(g, levelController.getBossController(), logicTick);
            drawSpawnWarnings(g);
            // 技能特效：灵箭之息地面领域圈与下坠箭矢
            if (skillController.isArrowRainActive()) {
                SkillEffectRenderer.draw(g, logicTick, skillController.getRainX(),
                        skillController.getRainY(), skillController.getFallingArrows());
            }
            // 入场动画：人物出现前不可见，出现瞬间伴随蓝色闪光特效；闪避期间淡出淡入
            if (playerSpawned()) {
                g.setGlobalAlpha(skillController.playerAlpha());
                PlayerRenderer.draw(g, player, mouseX, mouseY);
                g.setGlobalAlpha(1.0);
                drawSpawnFlash(g);
            }
        }

        /** 入场进度：关卡名淡入动画开始后经过的帧数。 */
        private int spawnElapsedTicks() {
            return LEVEL_INTRO_TICKS - roomFlash;
        }

        /** 人物是否已入场出现（淡入开始 0.8 秒后瞬间出现）。 */
        private boolean playerSpawned() {
            return spawnElapsedTicks() >= SPAWN_DELAY_TICKS;
        }

        /** 入场操作锁是否生效（人物出现前及出现后 0.1 秒内）。 */
        private boolean spawnLockActive() {
            return spawnElapsedTicks() < SPAWN_DELAY_TICKS + SPAWN_LOCK_TICKS;
        }

        /** 人物出现瞬间的蓝色闪光特效：中心亮核 + 光晕 + 放射状光线，快速衰减。 */
        private void drawSpawnFlash(GraphicsContext g) {
            int elapsed = spawnElapsedTicks() - SPAWN_DELAY_TICKS;
            if (elapsed < 0 || elapsed >= SPAWN_FLASH_TICKS) {
                return;
            }
            double intensity = 1 - elapsed / (double) SPAWN_FLASH_TICKS;
            double px = player.getPx();
            double py = player.getPy() - 4;
            // 蓝色光晕：多层圆由内向外扩散
            for (int layer = 0; layer < 3; layer++) {
                double radius = 24 + layer * 16 + (1 - intensity) * 20;
                double alpha = intensity * (0.5 - layer * 0.13);
                setColor(g, Color.rgb(110, 185, 255, alpha));
                g.fillOval(px - radius, py - radius, radius * 2, radius * 2);
            }
            // 中心亮核
            setColor(g, Color.rgb(255, 255, 255, intensity * 0.95));
            g.fillOval(px - 10, py - 10, 20, 20);
            setColor(g, Color.rgb(180, 220, 255, intensity));
            g.fillOval(px - 14, py - 14, 28, 28);
            // 放射状光线：从中心向外延伸的细长三角闪光（随强度收窄变短）
            int rays = 10;
            for (int i = 0; i < rays; i++) {
                double angle = i * Math.PI * 2 / rays;
                double cosA = Math.cos(angle);
                double sinA = Math.sin(angle);
                double inner = 22;
                double outer = 50 + 36 * intensity;
                double halfWidth = 3.5 + 3 * intensity;
                setColor(g, Color.rgb(150, 210, 255, intensity * 0.9));
                g.beginPath();
                g.moveTo(px + cosA * inner - sinA * halfWidth,
                        py + sinA * inner + cosA * halfWidth);
                g.lineTo(px + cosA * outer, py + sinA * outer);
                g.lineTo(px + cosA * inner + sinA * halfWidth,
                        py + sinA * inner - cosA * halfWidth);
                g.closePath();
                g.fill();
            }
        }

        /** 敌人出生前的红色准心预警：在出生点绘制脉冲圆环与旋转十字线（区别于人物蓝色闪光）。 */
        private void drawSpawnWarnings(GraphicsContext g) {
            for (double[] point : levelController.getPendingSpawnPoints()) {
                double px = point[0];
                double py = point[1];
                double pulse = (Math.sin(logicTick * 0.3) + 1) / 2;
                double radius = 10 + pulse * 3;
                setColor(g, Color.rgb(255, 70, 60, 220 / 255.0));
                g.setLineWidth(1.5);
                g.strokeOval(px - radius, py - radius, radius * 2, radius * 2);
                // 旋转十字准星线，向四周辐射
                double angle = logicTick * 0.09;
                for (int i = 0; i < 4; i++) {
                    double a = angle + i * Math.PI / 2;
                    double cosA = Math.cos(a);
                    double sinA = Math.sin(a);
                    g.strokeLine(px + cosA * (radius + 2), py + sinA * (radius + 2),
                            px + cosA * (radius + 6 + pulse * 2),
                            py + sinA * (radius + 6 + pulse * 2));
                }
                // 中心红点
                setColor(g, Color.rgb(255, 90, 70, 220 / 255.0));
                g.fillOval(px - 2, py - 2, 4, 4);
            }
        }

        /** 进入关卡时在屏幕中心淡入关卡名（主题名 + 关卡号），随后淡出；
         *  Boss 房同时以更大字号在下方展示 Boss 名（白色），一起淡入淡出。 */
        private void drawLevelIntro(GraphicsContext g) {
            if (roomFlash <= 0 || gameOver || victory) {
                return;
            }
            int elapsed = LEVEL_INTRO_TICKS - roomFlash;
            double alpha = Math.min(1.0,
                    Math.min((double) elapsed / LEVEL_INTRO_FADE,
                            (double) roomFlash / LEVEL_INTRO_FADE));
            if (alpha <= 0) {
                return;
            }
            MapTheme theme = levelController.getTheme();
            Font introFont = Font.font("Monospaced", FontWeight.BOLD, 42);
            g.setFont(introFont);
            String themeName = theme.getDisplayName();
            String levelNumber = levelController.levelName();
            double themeWidth = textWidth(themeName, introFont);
            double gapWidth = textWidth(" ", introFont);
            double x = WIDTH / 2.0
                    - (themeWidth + gapWidth + textWidth(levelNumber, introFont)) / 2;
            double y = HEIGHT / 2.0;
            // 投影增强可读性
            setColor(g, Color.rgb(0, 0, 0, 0.6 * alpha));
            g.fillText(themeName, x + 3, y + 3);
            g.fillText(levelNumber, x + themeWidth + gapWidth + 3, y + 3);
            // 地图名用本层主题配色，数字默认白色
            Color themeColor = Palette.color(theme.wallEdge());
            setColor(g, Color.color(themeColor.getRed(), themeColor.getGreen(),
                    themeColor.getBlue(), alpha));
            g.fillText(themeName, x, y);
            setColor(g, Color.rgb(255, 255, 255, alpha));
            g.fillText(levelNumber, x + themeWidth + gapWidth, y);
            // Boss 房：Boss 名以更大的白色字体显示在关卡名下方，随关卡名一起淡入淡出
            if (levelController.isBossStage() && !levelController.bossName().isEmpty()) {
                String bossName = levelController.bossName();
                Font bossFont = Font.font("Monospaced", FontWeight.BOLD, 60);
                g.setFont(bossFont);
                double bossX = WIDTH / 2.0 - textWidth(bossName, bossFont) / 2;
                double bossY = y + 76;
                // 投影增强可读性
                setColor(g, Color.rgb(0, 0, 0, 0.6 * alpha));
                g.fillText(bossName, bossX + 3, bossY + 3);
                setColor(g, Color.rgb(255, 255, 255, alpha));
                g.fillText(bossName, bossX, bossY);
            }
        }

        /** 传送门交互提示：玩家靠近传送门时居中显示"进入下一关"。 */
        private void drawGameHints(GraphicsContext g) {
            Portal portal = levelController.getPortal();
            if (portal != null
                    && distance(portal.getPx(), portal.getPy(), player.getPx(), player.getPy()) <= 90) {
                setColor(g, Color.rgb(216, 244, 255));
                Font hintFont = Font.font("Monospaced", FontWeight.BOLD, 17);
                g.setFont(hintFont);
                String hint = "进入下一关";
                g.fillText(hint, WIDTH / 2 - textWidth(hint, hintFont) / 2, 145);
            }
        }

        /** 主菜单：标题 + 角色选择面板 + 开始/退出按钮。 */
        private void drawMenuScreen(GraphicsContext g) {
            drawPanelTitle(g, "星尘地牢", "选择你的角色");
            characterSelectView.draw(g, elapsedSeconds);
            drawButton(g, 490, 560, 300, 56, "开始游戏", true);
            drawButton(g, 490, 630, 300, 56, "退出游戏", false);
        }

        /** 绘制面板标题与副标题（水平居中，逐行测量文本宽度定位）。 */
        private void drawPanelTitle(GraphicsContext g, String title, String subtitle) {
            setColor(g, Color.rgb(255, 224, 123));
            Font titleFont = Font.font("Monospaced", FontWeight.BOLD, 54);
            g.setFont(titleFont);
            g.fillText(title, WIDTH / 2 - textWidth(title, titleFont) / 2, 220);
            setColor(g, Color.rgb(193, 206, 239));
            Font subtitleFont = Font.font("Monospaced", 20);
            g.setFont(subtitleFont);
            g.fillText(subtitle, WIDTH / 2 - textWidth(subtitle, subtitleFont) / 2, 265);
        }

        /** 绘制按钮：主按钮高亮配色，阴影矩形 + 主体 + 居中标签。 */
        private void drawButton(GraphicsContext g, int x, int y, int width, int height,
                                String label, boolean primary) {
            Color fill = primary ? Color.rgb(66, 143, 161) : Color.rgb(60, 69, 104);
            setColor(g, Color.rgb(0, 0, 0, 90 / 255.0));
            g.fillRect(x + 4, y + 5, width, height);
            setColor(g, fill);
            g.fillRect(x, y, width, height);
            setColor(g, primary ? Color.rgb(148, 239, 226) : Color.rgb(137, 151, 198));
            g.setLineWidth(2);
            g.strokeRect(x, y, width, height);
            setColor(g, Color.WHITE);
            Font buttonFont = Font.font("Monospaced", FontWeight.BOLD, 21);
            g.setFont(buttonFont);
            g.fillText(label, x + (width - textWidth(label, buttonFont)) / 2,
                    y + height / 2 + 8);
        }

        /** 绘制背景：主题配色地面、网格、顶栏与地板纹理（菜单界面用默认主题）。 */
        private void drawBackground(GraphicsContext g) {
            // 菜单界面保持默认配色，游戏中按当前层主题配色。
            MapTheme theme = screen == Screen.MENU ? MapTheme.DEFAULT : levelController.getTheme();
            setColor(g, Palette.color(theme.bgColor()));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            setColor(g, Palette.color(theme.gridColor()));
            for (int x = 40; x < WIDTH; x += 32) {
                g.strokeLine(x, WORLD_TOP, x, HEIGHT);
            }
            for (int y = WORLD_TOP + 16; y < HEIGHT; y += 32) {
                g.strokeLine(34, y, WIDTH - 34, y);
            }
            setColor(g, Palette.color(theme.topBarColor()));
            g.fillRect(0, 0, WIDTH, WORLD_TOP);
            setColor(g, Palette.color(theme.topBarEdgeColor()));
            g.fillRect(0, WORLD_TOP - 2, WIDTH, 2);
            setColor(g, Palette.color(theme.floorColor()));
            g.fillRect(34, WORLD_TOP + 12, WIDTH - 68, HEIGHT - WORLD_TOP - 36);
            setColor(g, Palette.color(theme.floorBorderColor()));
            g.setLineWidth(3);
            g.strokeRect(34, WORLD_TOP + 12, WIDTH - 68, HEIGHT - WORLD_TOP - 36);
            // 地板纹理：散布的斑点与短线，颜色从主题地板色派生
            MapSceneryRenderer.drawFloorTextures(g, theme,
                    levelController.getCurrentMap().getFloorTextures());
        }

        /** 绘制 HUD：生命/护盾/能量状态栏、关卡名、武器槽、暂停按钮与技能状态。 */
        private void drawHud(GraphicsContext g) {
            setColor(g, Color.WHITE);
            Font hudFont = Font.font("Monospaced", FontWeight.BOLD, 16);
            g.setFont(hudFont);
            g.fillText("星尘地牢", 18, 31);
            // 状态栏一排：从左到右依次为生命值、护盾、能量值（图标 + 条 + 数值）
            drawBar(g, 145, 17, 145, 14, player.getHp(), player.getMaxHp(),
                    Color.rgb(239, 94, 104));
            drawHeartIcon(g, 145 - 21, 24);
            drawBar(g, 315, 17, 145, 14, player.getShield(), player.getMaxShield(),
                    Color.rgb(192, 208, 228));
            g.drawImage(SHIELD_ICON, 315 - 24, 18, 16, 16);
            drawBar(g, 485, 17, 145, 14, player.getEnergy(), player.getMaxEnergy(),
                    Color.rgb(77, 181, 255));
            drawEnergyIcon(g, 485 - 21, 24);
            String levelText = "关卡 " + levelController.levelName();
            setColor(g, Color.rgb(255, 240, 177));
            g.fillText(levelText,
                    WIDTH - 145 - 18 - textWidth(levelText, hudFont), 31);
            drawWeaponSlots(g);
            drawButton(g, WIDTH - 145, 10, 112, 34, "暂停", false);
            drawSkillStatus(g);
        }

        /** 技能状态提示（底部中央）：技能名 + 剩余持续/冷却秒数。 */
        private void drawSkillStatus(GraphicsContext g) {
            String name = SkillController.skillNameFor(player.getHeroType());
            Font font = Font.font("Monospaced", FontWeight.BOLD, 14);
            g.setFont(font);
            String text;
            Color color;
            if (skillController.getActiveTicks() > 0) {
                text = String.format("%s %.1fs", name, skillController.getActiveTicks() / 60.0);
                color = Color.rgb(255, 224, 123);
            } else if (skillController.getCooldownTicks() > 0) {
                text = String.format("%s 冷却 %.1fs", name,
                        skillController.getCooldownTicks() / 60.0);
                color = Color.rgb(150, 160, 190);
            } else {
                text = "空格 释放技能：" + name;
                color = Color.rgb(180, 230, 255);
            }
            double x = WIDTH / 2 - textWidth(text, font) / 2;
            setColor(g, Color.rgb(0, 0, 0, 150 / 255.0));
            g.fillText(text, x + 2, HEIGHT - 10 + 2);
            setColor(g, color);
            g.fillText(text, x, HEIGHT - 10);
        }

        /** 双武器槽位显示：顶部中央第二行，当前武器高亮。 */
        private void drawWeaponSlots(GraphicsContext g) {
            Font slotFont = Font.font("Monospaced", FontWeight.BOLD, 12);
            g.setFont(slotFont);
            for (int i = 0; i < 2; i++) {
                WeaponType type = i == 0 ? player.getWeapon() : player.getOffhandWeapon();
                boolean active = i == 0;
                int x = 480 + i * 158;
                setColor(g, active ? Color.rgb(66, 143, 161) : Color.rgb(60, 69, 104));
                g.fillRect(x, 36, 148, 24);
                setColor(g, active ? Color.rgb(148, 239, 226) : Color.rgb(137, 151, 198));
                g.setLineWidth(2);
                g.strokeRect(x, 36, 148, 24);
                String label;
                if (type == null) {
                    label = "（空）";
                } else if (i == 0) {
                    label = "1 " + type.getLabel()
                            + (player.getOffhandWeapon() != null ? " [Q]" : "");
                } else {
                    label = "2 " + type.getLabel();
                }
                setColor(g, Color.WHITE);
                g.fillText(label, x + 8, 53);
            }
        }

        /** 绘制状态条：暗底 + 按比例填充的色条 + 数值文本。 */
        private void drawBar(GraphicsContext g, int x, int y, int width, int height,
                             int value, int max, Color color) {
            setColor(g, Color.rgb(0, 0, 0, 150 / 255.0));
            g.fillRect(x, y, width, height);
            setColor(g, color);
            g.fillRect(x, y, width * value / max, height);
            setColor(g, Color.WHITE);
            g.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
            g.fillText(value + "/" + max, x + 6, y + 11);
        }

        /** 红色心形图标（生命值状态）。 */
        private void drawHeartIcon(GraphicsContext g, double cx, double cy) {
            setColor(g, Color.rgb(255, 76, 96));
            g.beginPath();
            g.moveTo(cx, cy + 6);
            g.bezierCurveTo(cx - 9, cy - 2, cx - 7, cy - 9, cx, cy - 4);
            g.bezierCurveTo(cx + 7, cy - 9, cx + 9, cy - 2, cx, cy + 6);
            g.closePath();
            g.fill();
        }

        /** 能量点图标（能量点建模的静态一帧：光晕 + 亮核，不脉动）。 */
        private void drawEnergyIcon(GraphicsContext g, double cx, double cy) {
            setColor(g, Color.rgb(84, 191, 255, 85 / 255.0));
            g.fillRect((int) cx - 4, (int) cy - 4, 8, 8);
            setColor(g, Color.rgb(151, 235, 255));
            g.fillRect((int) cx - 2, (int) cy - 2, 4, 4);
        }

        /** 同时设置填充色与描边色。 */
        private void setColor(GraphicsContext graphics, Paint paint) {
            graphics.setFill(paint);
            graphics.setStroke(paint);
        }

        /** 按指定字体测量文本渲染宽度（用于居中定位）。 */
        private double textWidth(String text, Font font) {
            Text textNode = new Text(text);
            textNode.setFont(font);
            return textNode.getLayoutBounds().getWidth();
        }

        /** 数值钳制到 [min, max] 区间。 */
        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }

        /** 两点间距离。 */
        private static double distance(double x1, double y1, double x2, double y2) {
            return Math.hypot(x1 - x2, y1 - y2);
        }

        /** 键盘按下：ESC 暂停切换、菜单选角、空格技能、Q 换枪、E 拾取武器。 */
        private void handleKeyPressed(KeyEvent event) {
            KeyCode code = event.getCode();
            if (code == KeyCode.ESCAPE && screen != Screen.MENU && !gameOver && !victory) {
                screen = screen == Screen.PLAYING ? Screen.PAUSED : Screen.PLAYING;
                return;
            }
            if (screen == Screen.MENU) {
                if (code == KeyCode.LEFT || code == KeyCode.A) {
                    characterSelectView.setSelectedIndex(characterSelectView.getSelectedIndex() - 1);
                } else if (code == KeyCode.RIGHT || code == KeyCode.D) {
                    characterSelectView.setSelectedIndex(characterSelectView.getSelectedIndex() + 1);
                } else if (code == KeyCode.ENTER) {
                    startGame();
                }
                return;
            }
            if (code == KeyCode.SPACE && screen == Screen.PLAYING && !gameOver && !victory
                    && !keys[code.getCode()] && !spawnLockActive()) {
                // 空格释放技能：游侠按当前移动方向（静止时按鼠标方向）闪避
                double moveDx = 0;
                double moveDy = 0;
                if (keys[KeyCode.W.getCode()] || keys[KeyCode.UP.getCode()]) {
                    moveDy--;
                }
                if (keys[KeyCode.S.getCode()] || keys[KeyCode.DOWN.getCode()]) {
                    moveDy++;
                }
                if (keys[KeyCode.A.getCode()] || keys[KeyCode.LEFT.getCode()]) {
                    moveDx--;
                }
                if (keys[KeyCode.D.getCode()] || keys[KeyCode.RIGHT.getCode()]) {
                    moveDx++;
                }
                skillController.tryCast(player, moveDx, moveDy, mouseX, mouseY,
                        levelController.getMonsters(), levelController.getCurrentMap(),
                        rewardController::dropLoot);
            }
            if (code == KeyCode.Q && screen == Screen.PLAYING && !gameOver && !victory
                    && !keys[code.getCode()] && !spawnLockActive()) {
                player.cycleWeapon();
            }
            if (code == KeyCode.E && screen == Screen.PLAYING && !gameOver && !victory
                    && !keys[code.getCode()] && !spawnLockActive()) {
                rewardController.tryPickupWeapon(player);
            }
            if (code.getCode() < keys.length) {
                keys[code.getCode()] = true;
            }
        }

        /** 键盘松开：清除按键按下状态。 */
        private void handleKeyReleased(KeyEvent event) {
            int code = event.getCode().getCode();
            if (code < keys.length) {
                keys[code] = false;
            }
        }

        /** 鼠标移动：换算为世界坐标；暂停/结算后战场冻结，不再更新。 */
        private void handleMouseMoved(MouseEvent event) {
            // 死亡/胜利/暂停时战场静止：冻结鼠标坐标，人物不再随鼠标转向
            if (gameOver || victory || screen == Screen.PAUSED) {
                return;
            }
            mouseX = toWorldX(event.getX());
            mouseY = toWorldY(event.getY());
        }

        /** 鼠标按下：结算按钮、传送门进入、暂停按钮、菜单交互与攻击输入的分流处理。 */
        private void handleMousePressed(MouseEvent event) {
            int x = toWorldX(event.getX());
            int y = toWorldY(event.getY());
            // 结算界面（死亡/通关）按钮：返回主界面 / 退出游戏，其余点击一律拦截
            if (gameOver || victory) {
                GameEndView.EndAction action = GameEndView.endScreenActionAt(x, y);
                if (action == GameEndView.EndAction.BACK_TO_MENU) {
                    resetGame();
                    return;
                }
                if (action == GameEndView.EndAction.EXIT_GAME) {
                    exitGame();
                    return;
                }
                return;
            }
            Portal portal = levelController.getPortal();
            if (event.getButton() == MouseButton.SECONDARY && screen == Screen.PLAYING
                    && portal != null && !spawnLockActive()
                    && distance(portal.getPx(), portal.getPy(),
                    player.getPx(), player.getPy()) <= 90) {
                enterNextLevel();
                return;
            }
            if (event.getButton() == MouseButton.PRIMARY && handleMenuClick(x, y)) {
                return;
            }
            if (event.getButton() == MouseButton.PRIMARY && screen == Screen.PLAYING
                    && !gameOver && !victory && isInside(x, y, WIDTH - 145, 10, 112, 34)) {
                screen = Screen.PAUSED;
                mouseDown = false;
                return;
            }
            if (event.getButton() == MouseButton.PRIMARY && screen == Screen.PLAYING
                    && !gameOver && !victory && !spawnLockActive()) {
                mouseDown = true;
                // 闪避期间停止任何操作：左键按下仅记录按住状态，闪避结束后由 updateGame 接续
                if (!skillController.isDodging()) {
                    combatController.attackPressed(player, mouseX, mouseY,
                            levelController.getMonsters(), rewardController::dropLoot);
                }
            }
        }

        /** 鼠标松开：结束按住状态并结算弓类松弦。 */
        private void handleMouseReleased(MouseEvent event) {
            if (event.getButton() == MouseButton.PRIMARY) {
                mouseDown = false;
                // 闪避期间松开左键不射箭，闪避结束后由 updateGame 补松弦结算
                if (screen == Screen.PLAYING && !gameOver && !victory && !spawnLockActive()
                        && !skillController.isDodging()) {
                    combatController.attackReleased(player, mouseX, mouseY,
                            levelController.getMonsters(), rewardController::dropLoot);
                }
            }
        }

        /** 屏幕坐标转世界坐标（按画布缩放比例换算并钳制）。 */
        private int toWorldX(double screenX) {
            return (int) clamp(screenX * WIDTH / Math.max(1, getWidth()), 0, WIDTH);
        }

        /** 屏幕坐标转世界坐标（按画布缩放比例换算并钳制）。 */
        private int toWorldY(double screenY) {
            return (int) clamp(screenY * HEIGHT / Math.max(1, getHeight()), 0, HEIGHT);
        }

        /** 菜单/暂停界面点击分流：角色卡片选择、开始/退出、恢复/回菜单。 */
        private boolean handleMenuClick(int x, int y) {
            if (screen == Screen.MENU) {
                int card = characterSelectView.hitTestCard(x, y);
                if (card >= 0) {
                    characterSelectView.setSelectedIndex(card);
                    return true;
                }
                if (isInside(x, y, 490, 560, 300, 56)) {
                    startGame();
                    return true;
                }
                if (isInside(x, y, 490, 630, 300, 56)) {
                    exitGame();
                    return true;
                }
            } else if (screen == Screen.PAUSED) {
                PauseView.Action action = PauseView.actionAt(x, y);
                if (action == PauseView.Action.RESUME) {
                    screen = Screen.PLAYING;
                    requestFocus();
                    return true;
                }
                if (action == PauseView.Action.BACK_TO_MENU) {
                    resetGame();
                    return true;
                }
                if (action == PauseView.Action.EXIT_GAME) {
                    exitGame();
                    return true;
                }
            }
            return screen == Screen.MENU;
        }

        /** 判断坐标点是否落在按钮矩形内。 */
        private boolean isInside(int x, int y, int buttonX, int buttonY,
                                 int buttonWidth, int buttonHeight) {
            return x >= buttonX && x <= buttonX + buttonWidth
                    && y >= buttonY && y <= buttonY + buttonHeight;
        }
    }
}
