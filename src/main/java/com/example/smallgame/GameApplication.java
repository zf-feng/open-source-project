package com.example.smallgame;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import com.example.smallgame.config.AppConfig;
import com.example.smallgame.view.AppView;

/**
 * 程序入口，负责 JavaFX 应用的启动与主窗口的初始化。
 * <p>
 * 启动流程：创建 AppView（主界面视图），依据 AppConfig 中的常量配置窗口标题与尺寸，
 * 加载窗口图标后显示舞台。
 * <p>
 * 引用文件：config.AppConfig（窗口参数）、view.AppView（主界面视图）、
 * resources/sprites/ui/game_icon.png（窗口图标）。
 */
public final class GameApplication extends Application {
    /**
     * JavaFX 应用启动入口。
     * 创建控制器与视图，完成窗口标题、图标、尺寸的设置，并将主界面挂载到场景中展示。
     *
     * @param stage JavaFX 主舞台
     */
    @Override
    public void start(Stage stage) {
        AppView view = new AppView();
        // 使用 AppConfig 中的统一常量配置窗口，保证全局尺寸一致
        stage.setTitle(AppConfig.TITLE);
        stage.getIcons().add(new Image(
                GameApplication.class.getResourceAsStream("/sprites/ui/game_icon.png")));
        stage.setScene(new Scene(view.createContent(), AppConfig.WIDTH, AppConfig.HEIGHT));
        stage.setMinWidth(AppConfig.WIDTH);
        stage.setMinHeight(AppConfig.HEIGHT);
        stage.show();
    }

    /**
     * main 方法，委托给 JavaFX 的 launch 启动图形界面。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        launch(args);
    }
}
