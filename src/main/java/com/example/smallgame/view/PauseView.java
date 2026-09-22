package com.example.smallgame.view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * 暂停界面（Canvas 绘制）：半透明遮罩 + 标题 + 三个按钮。
 * 暂停 UI 的绘制与按钮命中判定集中在此，由 GameMainView 在 Screen.PAUSED 状态下调用。
 * <p>
 * 引用文件：无（仅使用 JavaFX 图形与文本 API）。
 * 被 GameMainView（暂停状态的绘制与按钮命中交互）调用。
 */
public final class PauseView {
    /** 三个按钮的统一布局（绘制与命中判定共用）。 */
    public static final int BUTTON_X = 490;
    public static final int BUTTON_WIDTH = 300;
    public static final int BUTTON_HEIGHT = 56;
    public static final int RESUME_Y = 315;
    public static final int TO_MENU_Y = 390;
    public static final int EXIT_Y = 465;

    /** 暂停菜单按钮动作。 */
    public enum Action { NONE, RESUME, BACK_TO_MENU, EXIT_GAME }

    /** 工具类，禁止实例化。 */
    private PauseView() {
    }

    /** 暂停界面按钮命中判定（世界坐标，未命中返回 NONE）。 */
    public static Action actionAt(int x, int y) {
        if (isInside(x, y, BUTTON_X, RESUME_Y)) {
            return Action.RESUME;
        }
        if (isInside(x, y, BUTTON_X, TO_MENU_Y)) {
            return Action.BACK_TO_MENU;
        }
        if (isInside(x, y, BUTTON_X, EXIT_Y)) {
            return Action.EXIT_GAME;
        }
        return Action.NONE;
    }

    /** 绘制暂停界面：半透明遮罩 + 标题 + 三个按钮。 */
    public static void draw(GraphicsContext g, int width, int height) {
        setColor(g, Color.rgb(5, 8, 18, 205 / 255.0));
        g.fillRect(0, 0, width, height);
        drawPanelTitle(g, width, "游戏暂停", "冒险暂时停在这里");
        drawButton(g, BUTTON_X, RESUME_Y, "继续游戏", true);
        drawButton(g, BUTTON_X, TO_MENU_Y, "返回主界面", false);
        drawButton(g, BUTTON_X, EXIT_Y, "退出游戏", false);
    }

    /** 绘制面板标题与副标题（水平居中，逐行测量文本宽度定位）。 */
    private static void drawPanelTitle(GraphicsContext g, int width,
                                       String title, String subtitle) {
        setColor(g, Color.rgb(255, 224, 123));
        Font titleFont = Font.font("Monospaced", FontWeight.BOLD, 54);
        g.setFont(titleFont);
        g.fillText(title, width / 2 - textWidth(title, titleFont) / 2, 220);
        setColor(g, Color.rgb(193, 206, 239));
        Font subtitleFont = Font.font("Monospaced", 20);
        g.setFont(subtitleFont);
        g.fillText(subtitle, width / 2 - textWidth(subtitle, subtitleFont) / 2, 265);
    }

    /** 绘制按钮：主按钮用高亮配色，先画阴影偏移矩形再画主体，标签水平居中。 */
    private static void drawButton(GraphicsContext g, int x, int y,
                                   String label, boolean primary) {
        Color fill = primary ? Color.rgb(66, 143, 161) : Color.rgb(60, 69, 104);
        setColor(g, Color.rgb(0, 0, 0, 90 / 255.0));
        g.fillRect(x + 4, y + 5, BUTTON_WIDTH, BUTTON_HEIGHT);
        setColor(g, fill);
        g.fillRect(x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
        setColor(g, primary ? Color.rgb(148, 239, 226) : Color.rgb(137, 151, 198));
        g.setLineWidth(2);
        g.strokeRect(x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
        setColor(g, Color.WHITE);
        Font buttonFont = Font.font("Monospaced", FontWeight.BOLD, 21);
        g.setFont(buttonFont);
        g.fillText(label, x + (BUTTON_WIDTH - textWidth(label, buttonFont)) / 2,
                y + BUTTON_HEIGHT / 2 + 8);
    }

    /** 判断坐标点是否落在以 buttonX/buttonY 为左上角的按钮矩形内。 */
    private static boolean isInside(int x, int y, int buttonX, int buttonY) {
        return x >= buttonX && x <= buttonX + BUTTON_WIDTH
                && y >= buttonY && y <= buttonY + BUTTON_HEIGHT;
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
