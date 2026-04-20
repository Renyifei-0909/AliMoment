package com.aiimoment;

import javafx.animation.PauseTransition;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.Objects;

import com.aiimoment.ui.WindowResizeHelper;
import com.aiimoment.controller.LoginController;

/**
 * Windows 上 {@link StageStyle#UNDECORATED} 窗口在最大化时，Glass 客户区与 JavaFX {@link Scene}
 * 偶发不同步，Scene 未铺满 Stage，底部会露出原生窗口底色（白色）。通过最大化后微移窗口尺寸并
 * 触发 layout，强制同步客户区与 Scene。
 */
public class Main extends Application {

    /** 设计稿逻辑宽度（与 FXML 页面布局基准一致）。 */
    public static final double DESIGN_WIDTH = 1280;
    /** 整窗设计高度（顶栏 + 主内容区）。 */
    public static final double DESIGN_HEIGHT = 720;
    /** 与 MainWindow.fxml 中 topChrome 的 minHeight 保持一致。 */
    public static final double TOP_CHROME_DESIGN_HEIGHT = 56;
    /** 仅主内容区（center）的设计高度，用于等比缩放，顶栏不参与变换以避免与内容叠层错位。 */
    public static final double DESIGN_CONTENT_HEIGHT = DESIGN_HEIGHT - TOP_CHROME_DESIGN_HEIGHT;

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

    private static void installGlobalExitShortcut(Scene scene, Stage stage) {
        final boolean[] dialogOpen = {false};
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() != KeyCode.ESCAPE || dialogOpen[0]) {
                return;
            }
            dialogOpen[0] = true;

            ButtonType cancel = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType confirm = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.initOwner(stage);
            alert.setTitle("退出应用");
            alert.setHeaderText("退出应用");
            alert.setContentText("确定要退出应用吗？");
            alert.getButtonTypes().setAll(cancel, confirm);

            alert.showAndWait().ifPresent(type -> {
                if (type == confirm) {
                    Platform.exit();
                }
            });
            dialogOpen[0] = false;
            e.consume();
        });
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent rootParent = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/MainWindow.fxml")));
        if (!(rootParent instanceof BorderPane)) {
            throw new IllegalStateException("MainWindow.fxml 根节点应为 BorderPane");
        }
        BorderPane loaded = (BorderPane) rootParent;
        loaded.setMinSize(0, 0);
        loaded.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Node centerNode = loaded.getCenter();
        if (!(centerNode instanceof AnchorPane)) {
            throw new IllegalStateException("MainWindow center 应为 contentArea（AnchorPane）");
        }
        AnchorPane contentArea = (AnchorPane) centerNode;
        loaded.setCenter(null);

        final double contentDesignW = DESIGN_WIDTH;
        final double contentDesignH = DESIGN_CONTENT_HEIGHT;

        contentArea.setMinSize(contentDesignW, contentDesignH);
        contentArea.setPrefSize(contentDesignW, contentDesignH);
        contentArea.setMaxSize(contentDesignW, contentDesignH);

        /*
         * 只对主内容区缩放：顶栏留在 BorderPane.top，由布局管理，不与 center 共用同一组 Transform，
         * 避免「下部界面盖住导航栏」的叠层/裁切问题。
         * 缩放支点为内容区左上角 (0,0)。
         */
        Scale contentScale = new Scale(1, 1, 0, 0);
        contentArea.getTransforms().setAll(contentScale);

        Pane scaleViewport = new Pane(contentArea);
        scaleViewport.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        /* 与主内容底色一致，缩放留白（letterbox）不显突兀白块，见 app.css / theme-light.css */
        scaleViewport.getStyleClass().add("app-scale-viewport");
        Rectangle clip = new Rectangle();
        scaleViewport.setClip(clip);

        AnchorPane centerHost = new AnchorPane();
        centerHost.getStyleClass().add("app-scale-viewport");
        AnchorPane.setTopAnchor(scaleViewport, 0.0);
        AnchorPane.setRightAnchor(scaleViewport, 0.0);
        AnchorPane.setBottomAnchor(scaleViewport, 0.0);
        AnchorPane.setLeftAnchor(scaleViewport, 0.0);
        centerHost.getChildren().add(scaleViewport);
        loaded.setCenter(centerHost);

        Runnable updateScale = () -> {
            double w = scaleViewport.getWidth();
            double h = scaleViewport.getHeight();
            clip.setWidth(Math.max(0, w));
            clip.setHeight(Math.max(0, h));
            if (w <= 1 || h <= 1) {
                return;
            }
            double s = Math.min(w / contentDesignW, h / contentDesignH);
            contentScale.setX(s);
            contentScale.setY(s);
            contentArea.setLayoutX(Math.round((w - contentDesignW * s) / 2.0));
            contentArea.setLayoutY(Math.round((h - contentDesignH * s) / 2.0));
        };
        scaleViewport.widthProperty().addListener((o, a, b) -> updateScale.run());
        scaleViewport.heightProperty().addListener((o, a, b) -> updateScale.run());

        StackPane shell = new StackPane(loaded);
        shell.getStyleClass().add("app-shell");

        FXMLLoader loginLoader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/LoginView.fxml")));
        Parent loginRoot = loginLoader.load();
        LoginController loginController = loginLoader.getController();

        StackPane sceneRoot = new StackPane(shell, loginRoot);
        Scene scene = new Scene(sceneRoot);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/app.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/theme-light.css")).toExternalForm());
        AppTheme.applySaved(shell, scene);
        installGlobalExitShortcut(scene, primaryStage);

        loginController.setOnLoginSuccess(() -> {
            FadeTransition fade = new FadeTransition(Duration.millis(320), loginRoot);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(evt -> sceneRoot.getChildren().remove(loginRoot));
            fade.play();
        });

        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("AIiMoment");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(640);
        primaryStage.setMinHeight(400);

        Screen screen = Screen.getPrimary();
        Rectangle2D vb = screen.getVisualBounds();
        double winW = Math.min(DESIGN_WIDTH, vb.getWidth() - 48);
        double winH = Math.min(DESIGN_HEIGHT, vb.getHeight() - 48);
        primaryStage.setWidth(winW);
        primaryStage.setHeight(winH);
        primaryStage.setX(vb.getMinX() + (vb.getWidth() - winW) / 2);
        primaryStage.setY(vb.getMinY() + (vb.getHeight() - winH) / 2);

        installStageSceneSync(primaryStage);

        primaryStage.show();

        WindowResizeHelper.install(primaryStage, scene, 8);

        Platform.runLater(() -> {
            updateScale.run();
            syncSceneToStage(primaryStage);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
