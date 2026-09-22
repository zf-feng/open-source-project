package com.example.smallgame.view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import java.util.Random;

/**
 * 结算界面绘制中心：负责死亡/通关界面的遮罩、标题与按钮绘制、
 * 按钮命中检测，并提供死亡信息文案池的随机抽取。
 * <p>
 * 引用文件：无（仅使用 JavaFX 图形与文本 API）。
 * 被 GameMainView（对局结束时的界面绘制与按钮点击处理）调用。
 */
public final class GameEndView {

    /** 工具类，禁止实例化。 */
    private GameEndView() {
    }

    /** 死亡信息文案池：死亡界面随机抽取一条，以红色醒目字体显示在正上方。 */
    private static final String[] DEATH_MESSAGES = {
            "你已战至绝境！",
            "你因邪王的黑暗太过强大而不知所措",
            "你的战斗力不如空气",
            "你被敌人发现，叮咚一声把你送走了",
            "你与敌人的对手戏太过逼真，导致提前退场",
            "敌人长相过于可怕，你被吓死了",
            "你的思路不太理智",
            "敌人计算好物理路径，一击带走了你",
            "你在战斗中落败，从历史变成了传说",
            "你被敌人的神之箭矢所贯穿",
            "邪王看中了你的身体，你被当场吞噬",
            "你为什么要和敌人拥抱？",
            "你在最后一秒脑海里闪过走马灯",
            "你在敌人的刀尖上跳舞",
            "敌人的眼里没有你",
            "敌人唤起古老的岩灵，终结了你的冒险之旅",
            "敌人释放了焰核溢能，你当场被蒸发",
            "敌人朝你袭来致命一击，你连闪避的机会都没有",
            "你试图进行反抗，却被敌人碾成粉末",
            "你未被胜利女神所眷顾",
            "你的失败早已定下了结局"
    };
    /** 结算界面（死亡/通关）布局：标题纵坐标与两个按钮的位置尺寸。 */
    private static final int END_TITLE_Y = 200;
    private static final int END_BUTTON_Y = 520;
    private static final int END_BUTTON_WIDTH = 300;
    private static final int END_BUTTON_HEIGHT = 56;
    private static final int MENU_BUTTON_X = 320;
    private static final int EXIT_BUTTON_X = 660;
    /** 文案随机源。 */
    private static final Random RANDOM = new Random();

    /** 结算界面按钮动作（死亡与通关共用）。 */
    public enum EndAction {
        NONE, BACK_TO_MENU, EXIT_GAME
    }

    /** 从死亡信息文案池中随机抽取一条。 */
    public static String randomDeathMessage() {
        return DEATH_MESSAGES[RANDOM.nextInt(DEATH_MESSAGES.length)];
    }

    /** 死亡后界面：灰色遮罩覆盖静止的战场画面，正上方显示红色死亡信息，
     *  下方提供“返回主界面”与“退出游戏”两个互动按钮。 */
    public static void drawDeathScreen(GraphicsContext g, double width, double height,
                                       String deathMessage) {
        // 灰色半透明遮罩：局内人物、敌人、弹幕均停止更新，保持最后一帧静止画面
        g.setFill(Color.rgb(52, 52, 58, 220 / 255.0));
        g.fillRect(0, 0, width, height);
        // 正上方红色醒目死亡信息
        Font deathFont = Font.font("Monospaced", FontWeight.BOLD, 40);
        g.setFont(deathFont);
        double titleX = (width - textWidth(deathMessage, deathFont)) / 2;
        g.setFill(Color.rgb(0, 0, 0, 190 / 255.0));
        g.fillText(deathMessage, titleX + 3, END_TITLE_Y + 3);
        g.setFill(Color.rgb(255, 60, 60));
        g.fillText(deathMessage, titleX, END_TITLE_Y);
        // 两个互动按钮
        drawEndButton(g, MENU_BUTTON_X, END_BUTTON_Y, END_BUTTON_WIDTH,
                END_BUTTON_HEIGHT, "返回主界面", true);
        drawEndButton(g, EXIT_BUTTON_X, END_BUTTON_Y, END_BUTTON_WIDTH,
                END_BUTTON_HEIGHT, "退出游戏", false);
    }

    /** 通关后界面：灰色遮罩覆盖战场，正上方显示绿色“恭喜通关”，
     *  下方提供“返回主界面”与“退出游戏”两个互动按钮。 */
    public static void drawVictoryScreen(GraphicsContext g, double width, double height) {
        // 灰色半透明遮罩：战场画面保持静止
        g.setFill(Color.rgb(52, 52, 58, 220 / 255.0));
        g.fillRect(0, 0, width, height);
        // 正上方绿色“恭喜通关”
        Font titleFont = Font.font("Monospaced", FontWeight.BOLD, 46);
        g.setFont(titleFont);
        String title = "恭喜通关";
        double titleX = (width - textWidth(title, titleFont)) / 2;
        g.setFill(Color.rgb(0, 0, 0, 190 / 255.0));
        g.fillText(title, titleX + 3, END_TITLE_Y + 3);
        g.setFill(Color.rgb(80, 230, 120));
        g.fillText(title, titleX, END_TITLE_Y);
        // 两个互动按钮
        drawEndButton(g, MENU_BUTTON_X, END_BUTTON_Y, END_BUTTON_WIDTH,
                END_BUTTON_HEIGHT, "返回主界面", true);
        drawEndButton(g, EXIT_BUTTON_X, END_BUTTON_Y, END_BUTTON_WIDTH,
                END_BUTTON_HEIGHT, "退出游戏", false);
    }

    /** 结算界面按钮命中检测：返回点击命中的动作。 */
    public static EndAction endScreenActionAt(double x, double y) {
        if (isInside(x, y, MENU_BUTTON_X, END_BUTTON_Y, END_BUTTON_WIDTH, END_BUTTON_HEIGHT)) {
            return EndAction.BACK_TO_MENU;
        }
        if (isInside(x, y, EXIT_BUTTON_X, END_BUTTON_Y, END_BUTTON_WIDTH, END_BUTTON_HEIGHT)) {
            return EndAction.EXIT_GAME;
        }
        return EndAction.NONE;
    }

    /** 结算界面按钮绘制（与主界面按钮风格一致）。 */
    private static void drawEndButton(GraphicsContext g, int x, int y, int width, int height,
                                      String label, boolean primary) {
        g.setFill(Color.rgb(0, 0, 0, 90 / 255.0));
        g.fillRect(x + 4, y + 5, width, height);
        g.setFill(primary ? Color.rgb(66, 143, 161) : Color.rgb(60, 69, 104));
        g.fillRect(x, y, width, height);
        g.setStroke(primary ? Color.rgb(148, 239, 226) : Color.rgb(137, 151, 198));
        g.setLineWidth(2);
        g.strokeRect(x, y, width, height);
        g.setFill(Color.WHITE);
        Font buttonFont = Font.font("Monospaced", FontWeight.BOLD, 21);
        g.setFont(buttonFont);
        g.fillText(label, x + (width - textWidth(label, buttonFont)) / 2, y + height / 2 + 8);
    }

    /** 判断坐标是否落在指定按钮矩形内。 */
    private static boolean isInside(double x, double y, int buttonX, int buttonY,
                                    int buttonWidth, int buttonHeight) {
        return x >= buttonX && x <= buttonX + buttonWidth
                && y >= buttonY && y <= buttonY + buttonHeight;
    }

    /** 测量文本在指定字体下的渲染宽度（用于标题与按钮文字居中）。 */
    private static double textWidth(String text, Font font) {
        Text textNode = new Text(text);
        textNode.setFont(font);
        return textNode.getLayoutBounds().getWidth();
    }
}
