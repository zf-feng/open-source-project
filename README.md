# 星尘地牢

一款使用 JavaFX 开发的俯视角地牢冒险小游戏。

## 一键开始（Windows 10/11 64 位）

1. 下载本项目（约 1MB），解压到任意文件夹（路径可以包含中文、空格）；
2. 双击根目录的「星尘地牢.exe」。

首次运行时，启动器会自动准备运行环境：

- 如果电脑没有 Java 17：启动器会弹出一个窗口，引导你下载并安装 JDK 17（免费软件，约 190MB，只需安装一次）；安装完成后点击窗口里的“我已安装，重试”即可，也可以直接重新双击 exe；
- 自动下载 JavaFX 依赖（约 10MB，存放在 `.libs` 文件夹，需要联网）；
- 自动编译游戏源码（约 10 秒）；
- 随后游戏窗口自动打开。

之后每次游玩只需双击「星尘地牢.exe」，几秒内即可进入游戏，无需联网。

修改了源码后无需任何“打包”操作，再次双击 exe 会自动重新编译最新代码。

### 常见问题

- 双击后提示“Windows 已保护你的电脑”：因 exe 未购买签名证书所致。点击“更多信息”，再点击“仍要运行”即可。
- 提示“需要先安装 Java 17”：本游戏基于 Java 开发，请按弹窗提示下载安装 JDK 17（约 190MB，一次性）；推荐用弹窗里的“国内镜像”按钮，下载更快。装好后点“我已安装，重试”。
- 运行时生成的内容（`.libs`、`out`）可以随时删除，下次双击 exe 会按需重建。
- 注意：exe 不能脱离源码单独拿走使用，需与项目文件夹一起复制/分享。

## 玩法指南

### 基础操作

| 按键 | 功能 |
| --- | --- |
| W A S D / 方向键 | 移动 |
| 鼠标移动 | 瞄准方向 |
| 鼠标左键 | 攻击 |
| 鼠标右键 | 进入传送门 |
| Q | 切换主副武器（最多持有两把） |
| E | 拾取地面上的武器 |
| 空格 | 释放英雄技能 |
| Esc | 暂停 / 继续 |
| Enter | 菜单中确认角色选择 |

### 通关目标

游戏共 3 个世界，每个世界 5 关（1-1 至 3-5）：

1. 每关需要消灭所有波次的怪物，清空敌人后战场中央会升起传送门；
2. 靠近传送门后按鼠标右键进入下一关，生命、能量与技能冷却会继承到下一关；
3. 每个世界的第 5 关是 Boss 关：Boss 拥有独特的特殊攻击且血量极高，击败它才能开启传送门；
4. 通关 3-5（最终 Boss 关）即获得胜利。

### 武器获取

初始武器随所选英雄自带：骑士·手枪、游侠·双刀（小明小红）、精灵·弓。

- **Boss 关宝箱（金箱）**：击败 Boss 后出现在传送门旁，走近自动开启，必出武器：
  - 1-5 Boss：冰霜剑、复合弓、黄金沙漠之鹰、猎人弓、漫游左轮
  - 2-5 Boss：巨弓、咖喱棒、雪人之鹰
  - 3-5 为最终关，通关后直接结算胜利，不再生成宝箱；
- **普通关宝箱（白箱）**：通关后随机出现在地图某处，1% 概率开出当前世界的武器（世界 3 为全部武器）；
- **隐藏武器·血刀**：仅普通关宝箱有 0.5% 概率开出（Boss 箱不会出）。伤害极高，血量不满时挥砍消耗能量回复 1 点生命，但持有期间受到的伤害翻倍。

### 资源与技巧

- 能量：枪、弓、双刀攻击会消耗能量，击杀怪物会掉落能量点，走近自动吸取；
- Boss 血量降至一半时会掉落一批能量点，击杀后再掉落一批，善用时机补充资源；
- 护盾：每 2 秒回复 1 点；护盾未耗尽时受到攻击只扣护盾、不扣生命，护盾被打空后下一击才会伤到生命；
- 手刀：耗能武器能量不足时，敌人近身会自动切换为不耗能的空手攻击；
- 宝箱走近即可自动开启；靠近传送门后按鼠标右键进入下一关。

## 开发

环境要求：

- JDK 17+
- Maven 3.8+

在项目根目录执行：

```bash
mvn javafx:run
```

也可以在 IntelliJ IDEA 中导入 `pom.xml`，运行 `com.example.smallgame.GameApplication`。

源码按 `config`、`controller.combat`、`controller.level`、`controller.reward`、`controller.skill`、`model.entity`、`util`、`view` 分包，项目及 Maven 编译统一使用 UTF-8。

## 启动器维护

- 重建「星尘地牢.exe」：双击 `launcher\build-launcher.bat`（使用 Windows 自带的 .NET 编译器，无需安装任何额外工具）。
- 生成发布包：双击 `launcher\打包发布.bat`，会在项目根目录生成「星尘地牢-源码版.zip」（约 1.5MB，含源码与 exe，可直接发给朋友）。
- 更换游戏图标：把新图标放在项目根目录（命名为「星尘地牢.png」，建议正方形），运行 `launcher\make-icon.ps1` 重新生成 `app.ico`，再执行上面的重建脚本；窗口图标对应替换 `src/main/resources/sprites/ui/game_icon.png`。
- 重要同步点：升级 JavaFX 版本时，需同时修改 `pom.xml` 的 `javafx.version` 与 `launcher\StardustLauncher.cs` 中的 `JavafxVersion` 常量，保持一致。
- 启动器逻辑说明：双击后检测 Java 17（项目 `.runtime\jdk` → `JAVA_HOME` → `PATH` → 常见安装目录，装完 JDK 后点“重试”即可识别），缺失时弹窗引导用户自行下载安装；随后校验/下载 JavaFX 依赖、按源码时间戳增量编译，最后用 `javaw` 启动游戏。日志见 `out\launch-log.txt`。

## 目录说明

- `src/main/java`：游戏源码（model / controller / view 分层）
- `src/main/resources/sprites`：美术资源（角色、怪物、武器、宝箱等立绘）
- `launcher/`：一键启动器源码与维护脚本（`StardustLauncher.cs`、`build-launcher.bat`、`make-icon.ps1`、`package-release.ps1`、`打包发布.bat`、`app.ico`）
- `星尘地牢.exe`：一键启动器（约 180KB，必须与源码放在一起）
- `.libs`、`out`：启动器运行时自动生成（依赖 jar、编译结果、日志），不参与版本管理，可随时删除，下次双击 exe 会自动重建

## AI 使用与核对说明

本项目按规定使用 AI 辅助编程，AI 生成的核心算法实现如下（类 · 方法）：

- 战斗（controller/combat）：
  - `CombatController.attackPressed / attackHeld / attackReleased`（三类武器攻击输入分发）
  - `CombatController.shoot / releaseBow`（枪械连发与弓蓄力射击）
  - `CombatController.swingSword / throwBlade / swingHandKnife`（剑、双刀、手刀近战判定）
  - `CombatController.updateBullets`（子弹与墙体、敌人碰撞检测）
  - `BossController.updateTreeSanta / updateSnowYeti / updateGrassGiant / updateKnight / updateWizard / updateSlimeKing / updateVoidLord / updateFireDragon / updateIronPirate`（9 种 Boss 特殊技能状态机）
- 技能（controller/skill）：`SkillController.tryCast`（技能调度）、`updateArrowRain / damageRainZone`（箭雨百分比伤害）
- 关卡（controller/level）：`LevelController.addCrossLayout / addCornerL / addRingLayout / addDiagonalCrossLayout / addCorridorLayout / addCrossWall / addLWall / addBlockWall / addDiagonalWall / addBorderWalls`（墙体布局生成）、`planWaves / rollWaveCount`（波次规划）、`spawnPortal`（传送门生成）
- 奖励（controller/reward）：`RewardController.dropLoot / dropHalfHpLoot / dropEnergyPoints`（掉落）、`scatterLoot / weaponPoolForWorld`（宝箱掉落概率与武器池）、`tryPickupWeapon`（拾取与换枪）
- 实体（model/entity）：`Player.takeDamage`（护盾防溢出伤害结算）、`Player.update`（护盾回复计时）、`DungeonMap.collidesWithWall`（墙体碰撞）
- 界面与渲染（view）：AI 生成框架代码，人工调整布局与动画细节

人工核对方式：

1. 逐行核对边界与异常处理：护盾溢出保护（单次伤害清空护盾后多余伤害作废）、Boss 半血掉落的一次性标记防重复触发、小火球飞出地图边界的移除、箭雨伤害下限 `Math.max(1, (int) (enemy.getMaxHp() * ARROW_RAIN_PERCENT))` 防零伤害、咖喱棒剑气立绘角度矫正（素材为竖向长条，移除误加的 90° 矫正，修正剑气横置问题）；
2. 测试用例由 AI 生成草稿，人工逐项核对源码逻辑后定稿，共 12 个测试类 70 个用例，`mvn test` 全部通过；
3. Maven 编译验证通过，实际试玩调整数值与手感。

AI 仅作为编码与查错辅助工具，项目整体设计、数值平衡与最终代码质量由开发者负责。
