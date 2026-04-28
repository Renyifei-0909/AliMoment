package com.aiimoment.controller;

import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import com.aiimoment.ui.AliBrandLogo;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

public class MainController {

    private static final String NAV_STYLE = "nav-tab-btn";
    private static final String NAV_COMPACT_STYLE = "nav-tab-btn-compact";
    /** 设为负值：保持导航「图标 + 文字」常显，不进入仅图标折叠模式 */
    private static final double NAV_COMPACT_WIDTH_THRESHOLD = -1;

    @FXML
    private BorderPane topChrome;
    @FXML
    private StackPane brandLogoContainer;
    @FXML
    private Button navMenuBtn;
    @FXML
    private Button minimizeBtn;
    @FXML
    private Button maximizeBtn;
    @FXML
    private Button closeBtn;
    @FXML
    private Button navMaterial;
    @FXML
    private Button navDraft;
    @FXML
    private Button navSearch;
    @FXML
    private Button navSmartEdit;
    @FXML
    private Button navSettings;
    @FXML
    private HBox navItemsContainer;
    @FXML
    private AnchorPane pageMaterial;
    @FXML
    private AnchorPane pageDraft;
    @FXML
    private AnchorPane pageSearch;
    @FXML
    private AnchorPane pageSmartEdit;
    @FXML
    private AnchorPane pageSettings;

    private Map<Button, AnchorPane> navMap = new HashMap<>();
    private final Map<Button, String> navFullLabels = new HashMap<>();
    private boolean navCompact;
    private Button activeNavButton;

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        navMap.put(navMaterial, pageMaterial);
        navMap.put(navDraft, pageDraft);
        navMap.put(navSearch, pageSearch);
        navMap.put(navSmartEdit, pageSmartEdit);
        navMap.put(navSettings, pageSettings);

        initializePageContent();
        setupNavButtons();
        setupWindowControls();
        setupTopChromeDrag();

        if (navMenuBtn != null) {
            navMenuBtn.setOnAction(e -> { /* 菜单占位 */ });
        }

        loadBrandLogo();
        loadNavIcons();
        captureNavLabelsAndSetupAdaptiveNav();
        setActiveNavButton(navSearch);
        Platform.runLater(this::syncNavCompactToCurrentWidth);
    }

    private void captureNavLabelsAndSetupAdaptiveNav() {
        for (Button b : navMap.keySet()) {
            navFullLabels.put(b, b.getText());
        }
        if (navItemsContainer == null) {
            return;
        }
        navItemsContainer.widthProperty().addListener((obs, ignored, w) -> syncNavCompactToCurrentWidth());
    }

    private void syncNavCompactToCurrentWidth() {
        if (navItemsContainer == null) {
            return;
        }
        double width = navItemsContainer.getWidth();
        if (width <= 0) {
            return;
        }
        boolean wantCompact = width < NAV_COMPACT_WIDTH_THRESHOLD;
        if (wantCompact == navCompact) {
            return;
        }
        navCompact = wantCompact;
        refreshAllNavPresentation();
    }

    private void refreshAllNavPresentation() {
        for (Button b : navMap.keySet()) {
            applyNavCompactState(b);
            applyNavStyle(b, b == activeNavButton);
        }
    }

    private void applyNavCompactState(Button b) {
        String full = navFullLabels.get(b);
        if (full == null) {
            return;
        }
        if (navCompact) {
            b.setText("");
            b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            b.setTooltip(new Tooltip(full));
            if (!b.getStyleClass().contains(NAV_COMPACT_STYLE)) {
                b.getStyleClass().add(NAV_COMPACT_STYLE);
            }
        } else {
            b.setText(full);
            b.setContentDisplay(ContentDisplay.TOP);
            b.setTooltip(null);
            b.getStyleClass().remove(NAV_COMPACT_STYLE);
        }
    }

    private void loadBrandLogo() {
        if (brandLogoContainer == null) {
            return;
        }
        var url = getClass().getResource("/images/ali_logo.png");
        if (url == null) {
            url = getClass().getResource("/images/ali-logo.png");
        }
        if (url != null) {
            ImageView iv = new ImageView(new Image(url.toExternalForm(), 40, 40, true, true, true));
            brandLogoContainer.getChildren().setAll(iv);
        } else {
            brandLogoContainer.getChildren().setAll(AliBrandLogo.build(40));
        }
    }

    private void loadNavIcons() {
        setNavButtonIcon(navMaterial, "/images/p0.1.png");
        setNavButtonIcon(navDraft, "/images/p0.2.png");
        setNavButtonIcon(navSearch, "/images/p0.3.png");
        setNavButtonIcon(navSmartEdit, "/images/p0.4.png");
        setNavButtonIcon(navSettings, "/images/p0.5.png");
    }

    private void setNavButtonIcon(Button button, String resourcePath) {
        if (button == null || resourcePath == null || resourcePath.isBlank()) {
            return;
        }
        var url = getClass().getResource(resourcePath);
        if (url == null) {
            return;
        }
        ImageView iv = new ImageView(new Image(url.toExternalForm(), 18, 18, true, true, true));
        iv.getStyleClass().add("nav-tab-image");
        iv.setSmooth(true);
        iv.setPreserveRatio(true);
        button.setGraphic(iv);
        button.setContentDisplay(ContentDisplay.TOP);
    }

    private void initializePageContent() {
        loadPageFXML(pageMaterial, "/pages/MaterialPage.fxml");
        loadPageFXML(pageDraft, "/pages/DraftPage.fxml");
        loadPageFXML(pageSearch, "/pages/SearchPage.fxml");
        loadPageFXML(pageSmartEdit, "/pages/SmartEditPage.fxml");
        loadPageFXML(pageSettings, "/pages/SettingsPage.fxml");
    }

    private void loadPageFXML(AnchorPane container, String fxmlPath) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Node page = loader.load();
            container.getChildren().add(page);
            AnchorPane.setTopAnchor(page, 0.0);
            AnchorPane.setLeftAnchor(page, 0.0);
            AnchorPane.setRightAnchor(page, 0.0);
            AnchorPane.setBottomAnchor(page, 0.0);
        } catch (Exception e) {
            System.err.println("[FXML] 加载失败: " + fxmlPath + " — " + e.getMessage());
            e.printStackTrace();
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            String detail = root.getMessage();
            if (detail == null || detail.isBlank()) {
                detail = e.getMessage();
            }
            if (detail == null || detail.isBlank()) {
                detail = root.getClass().getSimpleName();
            }
            Label err = new Label(
                    "页面加载失败（" + fxmlPath + "）\n"
                            + e.getClass().getSimpleName() + ": " + e.getMessage()
                            + "\n根因: " + detail
            );
            err.setWrapText(true);
            err.setStyle("-fx-text-fill: #ff6666; -fx-padding: 16; -fx-font-size: 12px;");
            container.getChildren().add(err);
            AnchorPane.setTopAnchor(err, 8.0);
            AnchorPane.setLeftAnchor(err, 8.0);
            AnchorPane.setRightAnchor(err, 8.0);
        }
    }

    private void setupNavButtons() {
        navMap.keySet().forEach(button -> {
            button.setOnAction(event -> {
                setActiveNavButton(button);
                ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(150), button);
                scaleTransition.setToX(0.97);
                scaleTransition.setToY(0.97);
                scaleTransition.setAutoReverse(true);
                scaleTransition.setCycleCount(2);
                scaleTransition.play();
            });
        });
    }

    private void applyNavStyle(Button btn, boolean active) {
        btn.getStyleClass().removeAll(NAV_STYLE, "nav-tab-btn-active");
        btn.getStyleClass().add(NAV_STYLE);
        if (active) {
            btn.getStyleClass().add("nav-tab-btn-active");
        }
    }

    private void setActiveNavButton(Button button) {
        navMap.forEach((btn, page) -> {
            applyNavStyle(btn, false);
            page.setVisible(false);
        });
        activeNavButton = button;
        applyNavStyle(button, true);
        navMap.get(button).setVisible(true);
    }

    private void setupWindowControls() {
        if (minimizeBtn == null || maximizeBtn == null || closeBtn == null) {
            return;
        }

        minimizeBtn.setDefaultButton(false);
        maximizeBtn.setDefaultButton(false);
        closeBtn.setDefaultButton(false);
        minimizeBtn.setCancelButton(false);
        maximizeBtn.setCancelButton(false);
        closeBtn.setCancelButton(false);
        minimizeBtn.setFocusTraversable(false);
        maximizeBtn.setFocusTraversable(false);
        closeBtn.setFocusTraversable(false);

        minimizeBtn.setOnAction(event -> {
            Stage stage = (Stage) minimizeBtn.getScene().getWindow();
            stage.setIconified(true);
        });

        maximizeBtn.setOnAction(event -> {
            Stage stage = (Stage) maximizeBtn.getScene().getWindow();
            if (stage.isMaximized()) {
                stage.setMaximized(false);
            } else {
                stage.setMaximized(true);
            }
        });

        closeBtn.setOnAction(event -> {
            Stage stage = (Stage) closeBtn.getScene().getWindow();
            stage.close();
        });
    }

    private static boolean isUnderButton(Node n) {
        while (n != null) {
            if (n instanceof Button) {
                return true;
            }
            n = n.getParent();
        }
        return false;
    }

    private void setupTopChromeDrag() {
        topChrome.setOnMousePressed(event -> {
            if (isUnderButton(event.getTarget() instanceof Node ? (Node) event.getTarget() : null)) {
                return;
            }
            Stage stage = (Stage) topChrome.getScene().getWindow();
            xOffset = stage.getX() - event.getScreenX();
            yOffset = stage.getY() - event.getScreenY();
        });

        topChrome.setOnMouseDragged(event -> {
            if (isUnderButton(event.getTarget() instanceof Node ? (Node) event.getTarget() : null)) {
                return;
            }
            Stage stage = (Stage) topChrome.getScene().getWindow();
            stage.setX(event.getScreenX() + xOffset);
            stage.setY(event.getScreenY() + yOffset);
        });
    }
}
