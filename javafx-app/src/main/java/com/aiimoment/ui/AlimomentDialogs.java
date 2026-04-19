package com.aiimoment.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Window;

import com.aiimoment.AppTheme;

import java.net.URL;

/**
 * 统一的应用内对话框：深色圆角、品牌色按钮与左侧图标。
 */
public final class AlimomentDialogs {

    private static final String DIALOG_CSS = "/styles/dialog.css";
    private static final String DIALOG_LIGHT_CSS = "/styles/dialog-light.css";
    private static final int ICON_BOX = 56;
    private static final int LOGO_IN_ICON = 32;

    private AlimomentDialogs() {
    }

    public static void showError(Window owner, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.setTitle("Alimoment");
        alert.setHeaderText(header != null && !header.isEmpty() ? header : "出错了");
        alert.setContentText(message);
        alert.getDialogPane().setGraphic(buildErrorGraphic());
        applyStyle(alert.getDialogPane());
        alert.showAndWait();
    }

    private static void applyStyle(DialogPane pane) {
        pane.getStylesheets().removeIf(s ->
                s.contains("dialog.css") || s.contains("dialog-light.css"));
        URL dark = AlimomentDialogs.class.getResource(DIALOG_CSS);
        URL light = AlimomentDialogs.class.getResource(DIALOG_LIGHT_CSS);
        if (AppTheme.isLight()) {
            if (light != null) {
                pane.getStylesheets().add(light.toExternalForm());
            }
        } else if (dark != null) {
            pane.getStylesheets().add(dark.toExternalForm());
        }
        pane.getStyleClass().add("alimoment-dialog");
        pane.setMinWidth(380);
    }

    /**
     * 左侧图标：品牌 logo + 右下角红色错误角标，比默认 JavaFX 错误图标更贴合产品风格。
     */
    private static Node buildErrorGraphic() {
        StackPane root = new StackPane();
        root.setMinSize(ICON_BOX + 8, ICON_BOX + 8);
        root.setPrefSize(ICON_BOX + 8, ICON_BOX + 8);

        StackPane logoWrap = new StackPane();
        logoWrap.getStyleClass().add("dialog-brand-icon-wrap");
        logoWrap.setMinSize(ICON_BOX, ICON_BOX);
        logoWrap.setPrefSize(ICON_BOX, ICON_BOX);
        logoWrap.setMaxSize(ICON_BOX, ICON_BOX);

        URL logoUrl = resolveLogoUrl();
        if (logoUrl != null) {
            ImageView iv = new ImageView(new Image(logoUrl.toExternalForm(), LOGO_IN_ICON, LOGO_IN_ICON, true, true, true));
            logoWrap.getChildren().add(iv);
            StackPane.setAlignment(iv, Pos.CENTER);
        } else {
            Label fallback = new Label("A");
            fallback.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
            logoWrap.getChildren().add(fallback);
        }

        StackPane badge = new StackPane();
        badge.getStyleClass().add("dialog-error-badge");
        Label ex = new Label("!");
        ex.getStyleClass().add("dialog-error-badge-text");
        badge.getChildren().add(ex);

        root.getChildren().addAll(logoWrap, badge);
        StackPane.setAlignment(logoWrap, Pos.CENTER);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(4, 4, 0, 0));
        badge.setTranslateX(10);
        badge.setTranslateY(6);
        return root;
    }

    private static URL resolveLogoUrl() {
        URL u = AlimomentDialogs.class.getResource("/images/ali_logo.png");
        if (u == null) {
            u = AlimomentDialogs.class.getResource("/images/ali-logo.png");
        }
        return u;
    }
}
