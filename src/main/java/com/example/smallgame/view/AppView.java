package com.example.smallgame.view;

import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

/**
 * 主界面视图：构建根容器并把游戏画布（GameMainView.GamePanel）挂载其中。
 * <p>
 * 引用文件：GameMainView.GamePanel（游戏画布）。
 * 被 GameApplication（程序入口创建场景）使用。
 */
public final class AppView {

    /**
     * 创建界面内容：实例化游戏画布并放入 BorderPane 根容器。
     *
     * @return 根容器节点
     */
    public Parent createContent() {
        GameMainView.GamePanel game = new GameMainView.GamePanel();
        return new BorderPane(game);
    }
}
