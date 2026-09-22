package com.example.smallgame.view;

import com.example.smallgame.controller.skill.SkillController;
import com.example.smallgame.model.entity.DungeonMap;
import com.example.smallgame.model.entity.HeroType;
import com.example.smallgame.util.SpriteSheet;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * 角色选择面板：渲染骑士/游侠/精灵三张官方立绘卡片，
 * 处理卡片命中检测与选中状态。选角海报与局内建模同源（同一套立绘帧）。
 * <p>
 * 引用文件：controller.skill.SkillController（技能名与描述）、
 * model.entity（HeroType、DungeonMap）、util.SpriteSheet（英雄立绘帧）。
 * 被 GameMainView（角色选择界面的绘制与交互）调用。
 */
public final class CharacterSelectView {
    /** 卡片区域（世界坐标）。 */
    public static final int CARD_Y = 270;
    public static final int CARD_WIDTH = 180;
    public static final int CARD_HEIGHT = 250;
    public static final int CARD_GAP = 20;
    public static final int CARD_START_X =
            (DungeonMap.WIDTH - (CARD_WIDTH * 3 + CARD_GAP * 2)) / 2;

    private static final double SPRITE_SIZE = 128;

    private final HeroType[] heroes = HeroType.values();
    private int selectedIndex;

    /** 构造面板：预加载三套英雄立绘帧，避免进入游戏时才加载。 */
    public CharacterSelectView() {
        // 预加载三套立绘帧，避免进入游戏时才加载。
        for (HeroType hero : heroes) {
            SpriteSheet.load(hero.getSpriteFolder());
        }
    }

    /** 获取当前选中的英雄索引。 */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /** 设置选中英雄索引（取模环绕，支持左右循环切换）。 */
    public void setSelectedIndex(int index) {
        selectedIndex = Math.floorMod(index, heroes.length);
    }

    /** 获取当前选中的英雄类型。 */
    public HeroType getSelectedHero() {
        return heroes[selectedIndex];
    }

    /** 卡片命中检测，返回卡片索引；未命中返回 -1。 */
    public int hitTestCard(double x, double y) {
        if (y < CARD_Y || y > CARD_Y + CARD_HEIGHT) {
            return -1;
        }
        for (int i = 0; i < heroes.length; i++) {
            double cardX = CARD_START_X + i * (CARD_WIDTH + CARD_GAP);
            if (x >= cardX && x <= cardX + CARD_WIDTH) {
                return i;
            }
        }
        return -1;
    }

    /** 渲染选择面板。elapsedSeconds 为真实累计时间（秒），驱动立绘循环动画。
     *  选中角色的技能描述显示在屏幕左侧空白处。 */
    public void draw(GraphicsContext g, double elapsedSeconds) {
        int animFrame = (int) (elapsedSeconds * 8) % SpriteSheet.FRAME_COUNT;
        for (int i = 0; i < heroes.length; i++) {
            HeroType hero = heroes[i];
            double cardX = CARD_START_X + i * (CARD_WIDTH + CARD_GAP);
            boolean selected = i == selectedIndex;
            setColor(g, Color.rgb(0, 0, 0, 90 / 255.0));
            g.fillRect(cardX + 4, CARD_Y + 5, CARD_WIDTH, CARD_HEIGHT);
            setColor(g, selected ? Color.rgb(52, 88, 112) : Color.rgb(38, 48, 78));
            g.fillRect(cardX, CARD_Y, CARD_WIDTH, CARD_HEIGHT);
            setColor(g, selected ? Color.rgb(148, 239, 226) : Color.rgb(101, 114, 165));
            g.setLineWidth(selected ? 4 : 2);
            g.strokeRect(cardX, CARD_Y, CARD_WIDTH, CARD_HEIGHT);
            SpriteSheet sprite = SpriteSheet.load(hero.getSpriteFolder());
            sprite.draw(g, selected ? animFrame : 0, cardX + CARD_WIDTH / 2.0,
                    CARD_Y + 180, SPRITE_SIZE, false);
            setColor(g, selected ? Color.rgb(255, 224, 123) : Color.rgb(193, 206, 239));
            Font nameFont = Font.font("Monospaced", FontWeight.BOLD, 24);
            g.setFont(nameFont);
            String name = hero.getDisplayName();
            g.fillText(name, cardX + (CARD_WIDTH - textWidth(name, nameFont)) / 2, CARD_Y + 215);
            if (selected) {
                setColor(g, Color.rgb(255, 224, 123));
                Font tagFont = Font.font("Monospaced", FontWeight.BOLD, 15);
                g.setFont(tagFont);
                String tag = "已选择";
                g.fillText(tag, cardX + (CARD_WIDTH - textWidth(tag, tagFont)) / 2, CARD_Y + 240);
            }
        }
        // 选中角色的技能描述面板：屏幕左侧空白处
        drawSkillPanel(g, heroes[selectedIndex]);
    }

    /** 选中角色的技能说明面板（左侧空白处）：技能名 + 描述行。 */
    private void drawSkillPanel(GraphicsContext g, HeroType hero) {
        String skillName = SkillController.skillNameFor(hero);
        String[] description = SkillController.skillDescriptionFor(hero);
        int panelX = 40;
        int panelY = 320;
        int panelWidth = 270;
        int lineHeight = 22;
        int panelHeight = lineHeight * description.length + 60;
        setColor(g, Color.rgb(0, 0, 0, 90 / 255.0));
        g.fillRect(panelX + 4, panelY + 5, panelWidth, panelHeight);
        setColor(g, Color.rgb(38, 48, 78));
        g.fillRect(panelX, panelY, panelWidth, panelHeight);
        setColor(g, Color.rgb(101, 114, 165));
        g.setLineWidth(2);
        g.strokeRect(panelX, panelY, panelWidth, panelHeight);
        // 技能名
        setColor(g, Color.rgb(255, 224, 123));
        Font nameFont = Font.font("Monospaced", FontWeight.BOLD, 20);
        g.setFont(nameFont);
        g.fillText(skillName, panelX + 16, panelY + 30);
        // 描述行
        setColor(g, Color.rgb(213, 224, 245));
        Font descFont = Font.font("Monospaced", 13);
        g.setFont(descFont);
        for (int i = 0; i < description.length; i++) {
            g.fillText(description[i], panelX + 16, panelY + 58 + i * lineHeight);
        }
    }

    /** 同时设置填充色与描边色。 */
    private static void setColor(GraphicsContext graphics, Paint paint) {
        graphics.setFill(paint);
        graphics.setStroke(paint);
    }

    /** 按指定字体测量文本渲染宽度（用于居中定位）。 */
    private static double textWidth(String text, Font font) {
        Text textNode = new Text(text);
        textNode.setFont(font);
        return textNode.getLayoutBounds().getWidth();
    }
}
