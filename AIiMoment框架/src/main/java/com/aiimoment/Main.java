package com.aiimoment;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Windows 上 {@link StageStyle#UNDECORATED} 窗口在最大化时，Glass 客户区与 JavaFX {@link Scene}
 * 偶发不同步，Scene 未铺满 Stage，底部会露出原生窗口底色（白色）。通过最大化后微移窗口尺寸并
 * 触发 layout，强制同步客户区与 Scene。
 */
public class Main extends Application {

    /** 供控制器在需要时手动触发同步（例如某些环境下仅监听器不够时）。 */
    public static void syncSceneToStage(Stage stage) {
        if (stage == null) {
            return;
        }
        Scene scene = stage.getScene();
        if (scene == null) {
            return;
        }
        double w = stage.getWidth();
        double h = stage.getHeight();
        if (w <= 1 || h <= 1) {
            return;
        }
        stage.setWidth(w - 1);
        stage.setWidth(w);
        stage.setHeight(h - 1);
        stage.setHeight(h);
        Parent root = scene.getRoot();
        root.applyCss();
        root.requestLayout();
    }

    private static void scheduleSync(Stage stage) {
        Platform.runLater(() -> syncSceneToStage(stage));
        PauseTransition delay = new PauseTransition(Duration.millis(120));
        delay.setOnFinished(e -> syncSceneToStage(stage));
        delay.play();
    }

    private static void installStageSceneSync(Stage stage) {
        stage.maximizedProperty().addListener((obs, was, now) -> scheduleSync(stage));
        stage.fullScreenProperty().addListener((obs, was, now) -> scheduleSync(stage));
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent loaded = FXMLLoader.load(getClass().getResource("/MainWindow.fxml"));
        if (loaded instanceof Region) {
            Region r = (Region) loaded;
            r.setMaxWidth(Double.MAX_VALUE);
            r.setMaxHeight(Double.MAX_VALUE);
        }
        StackPane shell = new StackPane(loaded);
        shell.setAlignment(Pos.TOP_LEFT);
        shell.setStyle("-fx-background-color: #1a1a2e;");

        Scene scene = new Scene(shell);
        scene.setFill(Color.web("#1a1a2e"));

        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("AIiMoment");
        primaryStage.setScene(scene);

        Screen screen = Screen.getPrimary();
        Rectangle2D vb = screen.getVisualBounds();
        double winW = Math.min(1280, vb.getWidth() - 48);
        double winH = Math.min(720, vb.getHeight() - 48);
        primaryStage.setWidth(winW);
        primaryStage.setHeight(winH);
        primaryStage.setX(vb.getMinX() + (vb.getWidth() - winW) / 2);
        primaryStage.setY(vb.getMinY() + (vb.getHeight() - winH) / 2);

        installStageSceneSync(primaryStage);

        primaryStage.show();

        Platform.runLater(() -> syncSceneToStage(primaryStage));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
